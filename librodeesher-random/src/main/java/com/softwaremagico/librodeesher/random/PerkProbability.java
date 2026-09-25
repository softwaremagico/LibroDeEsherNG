package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkBonus;
import com.softwaremagico.librodeesher.perk.PerkBonusKind;
import com.softwaremagico.librodeesher.perk.PerkType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;

import java.util.List;

/**
 * How likely the random character generator is to pick {@code perk} right now, replicating the
 * legacy {@code PerkProbability} heuristics on top of the id-based NG model ({@link Perk#getId()}
 * replaces the legacy perks keyed by Spanish name, so the suggested list is keyed by raw ids instead
 * of display names).
 *
 * <p>The returned value is a percentage (larger is better; {@link #SUGGESTED_PROBABILITY} short
 * circuits a suggested perk), plus two special outcomes the caller must interpret before feeding it
 * to a {@link RandomSelector}: {@link #VETO_PROBABILITY} (never take this perk) and {@code 0}
 * (magical perk while magic is disabled, reported by the legacy code without applying any other
 * gate). A perk that costs more background points than the race assigns
 * ({@link CharacterPlayer#getRacePerksPoints()}) is also vetoed, as is the sceptic perk for any
 * wizard (see {@code smartRandomness()}).</p>
 *
 * <p>The NG race catalog only ships one combined {@code Race#getBackgroundPoints()} pool where the
 * legacy code used a separately computed {@code Race#getPerksPoints()}, so the budget terms mirror
 * the legacy ones with that pool standing in (documented on
 * {@link CharacterPlayer#getRacePerksPoints()}); the grade-based background costing of {@code
 * CharacterPlayer#canAffordPerk} is a separate accounting model and remains untouched.</p>
 *
 * <p>{@code selectOptions()} and {@code shufflePerks()} of the legacy class are not ported here: NG
 * resolves a perk's "choose N" grants inside {@code CharacterPlayer#applyPerkChoiceGrant} (see
 * {@code PerkBonus}/{@code PerkChoiceGrant}), and the shuffle step belongs to the character
 * generator that consumes this class, which will order suggested perks first in {@code
 * RandomCharacterPlayer}.</p>
 */
public class PerkProbability {

    /** Result returned when the perk is vetoed for random generation (unaffordable, or the sceptic perk for wizards). */
    public static final int VETO_PROBABILITY = Integer.MIN_VALUE;

    /** A suggested perk is always picked ({@code suggestedPerks} short circuit). */
    public static final int SUGGESTED_PROBABILITY = 100;

    private static final String SCEPTIC_PERK_ID = "skeptic";

    /** Cap of the accumulated skill/category bonus term, matching the legacy {@code getProbabilityBySkillsBonus()}. */
    private static final int SKILL_PROBABILITY_CAP = 15;

    private final CharacterPlayer characterPlayer;
    private final Perk perk;
    private final int specializationLevel;
    private final List<String> suggestedPerks;

    /**
     * @param suggestedPerks perk ids the generator is instructed to pick this level (legacy
     *                       suggested-perks names, lifted from a pre-generated character), {@code
     *                       null} when no target profile applies.
     * @param specializationLevel how much the character should invest in perks, matching the legacy
     *                       {@code specializationLevel} (used as a slight positive/negative push in
     *                       {@code getProbabilityByNumberOfPerks()} and the "standard perk" fallback).
     */
    public PerkProbability(CharacterPlayer characterPlayer, Perk perk, int specializationLevel, List<String> suggestedPerks) {
        this.characterPlayer = characterPlayer;
        this.perk = perk;
        this.specializationLevel = specializationLevel;
        this.suggestedPerks = suggestedPerks;
    }

    /**
     * Probability of a random generator picking {@link #perk}, matching the legacy {@code
     * PerkProbability#getProbability()} exactly: magical-perks-while-disabled and suggested-perks
     * short circuits first, then the allowed/similar-perk gate, the budget veto and the
     * number/grade/characteristic/resistance/skill/common/armour/movement bonus accumulation, the
     * "no bonus at all" fallback, and finally the sceptic veto for wizards.
     */
    public int getProbability() throws InvalidXmlElementException {
        long probability = 0;
        int probabilityBySkills = 0;

        // Avoid magical perks if the magic option is disabled.
        if (perk.getType() == PerkType.MAGICAL && !characterPlayer.isMagicAllowed()) {
            return 0;
        }
        // Suggested perks are always picked.
        if (suggestedPerks != null && suggestedPerks.contains(perk.getId())) {
            return SUGGESTED_PROBABILITY;
        }
        if (characterPlayer.isPerkAllowedForCharacter(perk.getId()) && !characterPlayer.hasSimilarPerk(perk)) {
            probability += getProbabilityByCost();
            if (probability >= 0) {
                probability += getProbabilityByNumberOfPerks();
                probability += getProbabilityByGrade();
                probability += getProbabilityByCharacteristics();
                probability += getProbabilityByResistances();
                probabilityBySkills += getProbabilityBySkillsBonus();
                if (probabilityBySkills > SKILL_PROBABILITY_CAP) {
                    probabilityBySkills = SKILL_PROBABILITY_CAP;
                }
                probability += probabilityBySkills;
                probability += getProbabilityByCommonsOrRestricted();
                probability += getProbabilityByArmourClass();
                probability += getProbabilityByMovement();
            }
            // No bonus, no malus: a standard perk, add it if it has not been chosen.
            if (probabilityBySkills == 0 && probability < 0) {
                probability = 2L - characterPlayer.getSelectedPerks().size() + specializationLevel;
            }
            probability += smartRandomness();
        }
        // The legacy code accumulated in plain int space, which could silently overflow when adding
        // the veto (-MIN_VALUE) to an already negative fallback value, producing non-deterministic
        // negative results. Accumulating in long space and clamping every heavy veto down to the
        // exact VETO_PROBABILITY keeps every single-veto outcome as the legacy code intended (all
        // real, non-vetoed results are small integers, far above the clamp threshold).
        return probability <= VETO_PROBABILITY / 2 ? VETO_PROBABILITY : (int) probability;
    }

    private long getProbabilityByNumberOfPerks() {
        return -1L * characterPlayer.getSelectedPerks().size() * (2 - specializationLevel) * 5;
    }

    /**
     * Wizards never take the sceptic perk (it would cancel their magic), matching the legacy {@code
     * smartRandomness()}.
     */
    private long smartRandomness() throws InvalidXmlElementException {
        if (characterPlayer.isWizard() && SCEPTIC_PERK_ID.equals(perk.getId())) {
            return VETO_PROBABILITY;
        }
        return 0;
    }

    private long getProbabilityByMovement() {
        long movementBonus = 0;
        for (final PerkBonus perkBonus : perk.getBonuses()) {
            if (perkBonus.isMovement() && perkBonus.getValue() != null) {
                movementBonus += perkBonus.getValue();
            }
        }
        return movementBonus / 2;
    }

    private long getProbabilityByArmourClass() {
        long armourClass = 0;
        for (final PerkBonus perkBonus : perk.getBonuses()) {
            if (perkBonus.isArmor() && perkBonus.getValue() != null) {
                armourClass += perkBonus.getValue();
            }
        }
        return armourClass == 0 ? 0 : armourClass - 1;
    }

    private long getProbabilityByCost() throws InvalidXmlElementException {
        final Integer racePerksPoints = characterPlayer.getRacePerksPoints();
        if (racePerksPoints != null) {
            final int cost = perk.getCost() == null ? 0 : perk.getCost();
            if (characterPlayer.getSpentPerksPoints() + cost <= racePerksPoints) {
                return 0;
            }
        }
        return VETO_PROBABILITY;
    }

    private long getProbabilityByGrade() {
        return -gradeLevel(perk) * 3;
    }

    private static int gradeLevel(Perk perk) {
        return perk.getGrade() == null ? 0 : perk.getGrade().getLevel();
    }

    private long getProbabilityByResistances() {
        long bonus = 0;
        for (final PerkBonus perkBonus : perk.getBonuses()) {
            if (perkBonus.getResistanceType() != null && perkBonus.getValue() != null) {
                bonus += perkBonus.getValue() / 5;
            }
        }
        return bonus;
    }

    private long getProbabilityByCharacteristics() {
        long bonus = 0;
        for (final PerkBonus perkBonus : perk.getBonuses()) {
            if (perkBonus.getCharacteristic() != null && perkBonus.getValue() != null) {
                bonus += perkBonus.getValue();
            }
        }
        return bonus;
    }

    /**
     * Fixed category/skill bonuses of {@link #perk}, added only when the character already finds the
     * target interesting (see {@link CharacterPlayer#isCategoryInteresting(Category)}/{@link
     * CharacterPlayer#isSkillInteresting(Skill)}), weighted by bonus kind exactly as the legacy
     * {@code getProbabilityBySkillsBonus()}.
     */
    private int getProbabilityBySkillsBonus() throws InvalidXmlElementException {
        int bonus = 0;
        for (final PerkBonus perkBonus : perk.getBonuses()) {
            if (perkBonus.getCategoryId() != null) {
                if (isCategoryInteresting(perkBonus.getCategoryId())) {
                    bonus += categoryBonus(perkBonus);
                }
            } else if (perkBonus.getSkillId() != null && isSkillInteresting(perkBonus.getSkillId())) {
                bonus += skillBonus(perkBonus);
            }
        }
        return bonus;
    }

    private long getProbabilityByCommonsOrRestricted() throws InvalidXmlElementException {
        long bonus = 0;
        for (final PerkBonus perkBonus : perk.getBonuses()) {
            if (perkBonus.getKind() == PerkBonusKind.MAKES_COMMON) {
                if (perkBonus.getCategoryId() != null) {
                    bonus += isCategoryInteresting(perkBonus.getCategoryId()) ? 3 : 0;
                } else if (perkBonus.getSkillId() != null && isSkillInteresting(perkBonus.getSkillId())) {
                    bonus += 1;
                }
            } else if (perkBonus.getKind() == PerkBonusKind.MAKES_RESTRICTED) {
                if (perkBonus.getCategoryId() != null) {
                    bonus -= isCategoryInteresting(perkBonus.getCategoryId()) ? 3 : 0;
                } else if (perkBonus.getSkillId() != null && isSkillInteresting(perkBonus.getSkillId())) {
                    bonus -= 1;
                }
            }
        }
        return bonus;
    }

    private boolean isCategoryInteresting(String categoryId) throws InvalidXmlElementException {
        final Category category;
        try {
            category = RulesCatalog.getInstance().getCategory(categoryId);
        } catch (final InvalidXmlElementException e) {
            return false;
        }
        return characterPlayer.isCategoryInteresting(category);
    }

    private boolean isSkillInteresting(String skillId) throws InvalidXmlElementException {
        final Skill skill;
        try {
            skill = RulesCatalog.getInstance().getSkill(skillId);
        } catch (final InvalidXmlElementException e) {
            return false;
        }
        return characterPlayer.isSkillInteresting(skill);
    }

    /**
     * Weight of a category-targeted bonus by kind: full for a fixed bonus or extra ranks, half for a
     * conditional one, nothing for the makes-common/-restricted markers (handled by {@link
     * #getProbabilityByCommonsOrRestricted()}).
     */
    private static int categoryBonus(PerkBonus perkBonus) {
        final int value = perkBonus.getValue() == null ? 0 : perkBonus.getValue();
        return switch (perkBonus.getKind()) {
            case CONDITIONAL -> value / 2;
            case FLAT, PER_RANK -> value;
            default -> 0;
        };
    }

    /**
     * Weight of a skill-targeted bonus by kind, matching the legacy {@code getProbabilityBySkillsBonus()}
     * dividers: half for a fixed bonus, full for per-rank, a third for a conditional one.
     */
    private static int skillBonus(PerkBonus perkBonus) {
        final int value = perkBonus.getValue() == null ? 0 : perkBonus.getValue();
        return switch (perkBonus.getKind()) {
            case FLAT -> value / 2;
            case PER_RANK -> value;
            case CONDITIONAL -> value / 3;
            default -> 0;
        };
    }
}