package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkBonus;
import com.softwaremagico.librodeesher.perk.PerkBonusKind;
import com.softwaremagico.librodeesher.perk.PerkChoiceGrant;
import com.softwaremagico.librodeesher.perk.PerkChoiceScope;
import com.softwaremagico.librodeesher.perk.PerkType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Random-character-generation scoring for the legacy {@code PerkProbability}: how likely the
 * generator is to pick each perk, which (for a picked perk) options its "choose N of..." grants
 * resolve to, and the order in which every perk is evaluated.
 *
 * <p>The NG {@link Perk} model reshapes the legacy bonus columns into {@link PerkBonus}/{@link
 * PerkChoiceGrant} entries, so each legacy probability term is computed from those fields instead
 * of the legacy name-keyed maps. The {@code OTHER} perk type is excluded entirely (the legacy data
 * leaves house-rule perks out of random selection too), and an {@link PerkType#MAGICAL} perk is
 * never picked when the character has no assigned realm of magic.</p>
 */
public final class PerkProbability {

    /** The probability a perk must score to be vetoed outright (see {@link #smartRandomness()}). */
    public static final int VETO_PROBABILITY = Integer.MIN_VALUE;

    /** A suggested perk is always picked ({@code suggestedPerks} short circuit). */
    public static final int SUGGESTED_PROBABILITY = 100;

    private static final String SCEPTIC_PERK_ID = "skeptic";
    private static final int SKILL_PROBABILITY_CAP = 15;

    private final CharacterPlayer characterPlayer;
    private final Perk perk;
    private final int specializationLevel;
    private final List<String> suggestedPerkIds;

    public PerkProbability(CharacterPlayer characterPlayer, Perk perk, int specializationLevel,
            List<String> suggestedPerkIds) {
        this.characterPlayer = characterPlayer;
        this.perk = perk;
        this.specializationLevel = specializationLevel;
        this.suggestedPerkIds = suggestedPerkIds;
    }

    /**
     * The suggested perk ids first, then every other perk in random order, mirroring the legacy
     * {@code shufflePerks} (its suggested list is preserved as given, the rest shuffled). Perks of
     * {@link PerkType#OTHER} are left out. The input list is left untouched.
     */
    public static List<String> shufflePerks(CharacterPlayer characterPlayer, List<String> suggestedPerkIds)
            throws InvalidXmlElementException {
        final List<String> suggested = suggestedPerkIds == null ? new ArrayList<>() : new ArrayList<>(suggestedPerkIds);
        final List<String> rest = new ArrayList<>();
        for (final Perk perk : RulesCatalog.getInstance().getPerks()) {
            if (perk.getType() != PerkType.OTHER && !suggested.contains(perk.getId())) {
                rest.add(perk.getId());
            }
        }
        RandomValues.shuffle(rest);
        final List<String> ordered = new ArrayList<>(suggested);
        ordered.addAll(rest);
        return ordered;
    }

    /**
     * Probability (in percent, tiny for most perks) of a random generator picking {@link #perk},
     * mirroring the legacy {@code PerkProbability#getProbability()}: a suggested perk scores 100,
     * a magical perk on a character without magic 0, and otherwise the fixed-bonus terms (cost,
     * number of perks, grade, characteristics, resistances, skill/category bonuses, common/
     * restricted markers, armor class, movement) accumulate on top of the base cost check, with the
     * legacy "standard perk" floor and the wizard-veto of the sceptic perk.
     */
    public int getProbability() throws InvalidXmlElementException {
        if (perk.getType() == PerkType.MAGICAL && !characterPlayer.isMagicAllowed()) {
            return 0;
        }
        if (suggestedPerkIds != null && suggestedPerkIds.contains(perk.getId())) {
            return SUGGESTED_PROBABILITY;
        }
        int probability = 0;
        int probabilityBySkills = 0;
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
            // No bonus, no malus. A standard perk. Add it if it has not perk chosen.
            if (probabilityBySkills == 0 && probability < 0 && probability != VETO_PROBABILITY) {
                probability = 2 - characterPlayer.getSelectedPerks().size() + specializationLevel;
            }
            final int smartRandomness = smartRandomness();
            probability = smartRandomness == VETO_PROBABILITY ? VETO_PROBABILITY : probability + smartRandomness;
            // A single-veto outcome (unaffordable, sceptic for a wizard) collapses to the exact
            // veto sentinel, as the legacy code intended.
            probability = probability <= VETO_PROBABILITY / 2 ? VETO_PROBABILITY : probability;
        }
        return probability;
    }

    /**
     * Resolves the "choose N of..." grants of the already-selected {@link #perk} to random targets,
     * mirroring the legacy {@code PerkProbability#selectOptions()}: an explicit single category/
     * skill is kept as-is; a whole scope is shuffled and targets the character is prepared for (an
     * enabled category with ranks, or an interesting skill) are preferred. Every resolution lands
     * in the decision layer, so it is reused on subsequent calls.
     */
    public void selectOptions() throws InvalidXmlElementException {
        final List<PerkChoiceGrant> grants = perk.getChoiceGrants();
        for (int i = 0; i < grants.size(); i++) {
            final PerkChoiceGrant grant = grants.get(i);
            final List<String> targets;
            if (grant.getCategoryId() != null) {
                targets = List.of(grant.getCategoryId());
            } else if (grant.getSkillId() != null) {
                targets = List.of(grant.getSkillId());
            } else if (isCategoryScope(grant.getScope())) {
                targets = selectTargets(getScopeOptions(grant.getScope()), choiceCount(grant),
                        this::isInterestingCategory);
            } else {
                targets = selectTargets(getScopeOptions(grant.getScope()), choiceCount(grant),
                        this::isInterestingSkill);
            }
            characterPlayer.applyPerkChoiceGrant(perk.getId(), i, grant, targets);
        }
    }

    private static int choiceCount(PerkChoiceGrant grant) {
        final Integer optionsToChoose = grant.getOptionsToChoose();
        return optionsToChoose == null || optionsToChoose == -1 ? 1 : optionsToChoose;
    }

    /** A perk that would overflow the race-derived perk point budget scores the lowest possible value. */
    private int getProbabilityByCost() {
        try {
            final Integer racePoints = characterPlayer.getRacePerksPoints();
            final Integer cost = perk.getCost();
            if (racePoints == null || cost == null
                    || characterPlayer.getSpentPerksPoints() + cost <= racePoints) {
                return 0;
            }
        } catch (final InvalidXmlElementException e) {
            return 0;
        }
        return VETO_PROBABILITY;
    }

    private int getProbabilityByNumberOfPerks() {
        return -1 * characterPlayer.getSelectedPerks().size() * (2 - specializationLevel) * 5;
    }

    private int getProbabilityByGrade() {
        return -perk.getGrade().getLevel() * 3;
    }

    private int getProbabilityByCharacteristics() {
        int bonus = 0;
        for (final PerkBonus bonusEntry : perk.getBonuses()) {
            if (bonusEntry.getCharacteristic() != null && bonusEntry.getKind() == PerkBonusKind.FLAT
                    && bonusEntry.getValue() != null) {
                bonus += bonusEntry.getValue();
            }
        }
        return bonus;
    }

    private int getProbabilityByResistances() {
        int bonus = 0;
        for (final PerkBonus bonusEntry : perk.getBonuses()) {
            if (bonusEntry.getResistanceType() != null && bonusEntry.getKind() == PerkBonusKind.FLAT
                    && bonusEntry.getValue() != null) {
                bonus += bonusEntry.getValue() / 5;
            }
        }
        return bonus;
    }

    private int getProbabilityBySkillsBonus() throws InvalidXmlElementException {
        int bonus = 0;
        for (final PerkBonus bonusEntry : perk.getBonuses()) {
            if (bonusEntry.getCategoryId() != null) {
                bonus += interestingCategoryBonus(bonusEntry);
            } else if (bonusEntry.getSkillId() != null) {
                bonus += interestingSkillBonus(bonusEntry);
            }
        }
        return bonus;
    }

    private int interestingCategoryBonus(PerkBonus bonusEntry) throws InvalidXmlElementException {
        if (!isInterestingCategory(bonusEntry.getCategoryId()) || bonusEntry.getValue() == null) {
            return 0;
        }
        return bonusEntry.getKind() == PerkBonusKind.CONDITIONAL ? bonusEntry.getValue() / 2 : bonusEntry.getValue();
    }

    private int interestingSkillBonus(PerkBonus bonusEntry) throws InvalidXmlElementException {
        if (!isInterestingSkill(bonusEntry.getSkillId()) || bonusEntry.getValue() == null) {
            return 0;
        }
        return switch (bonusEntry.getKind()) {
            case CONDITIONAL -> bonusEntry.getValue() / 3;
            case PER_RANK -> bonusEntry.getValue();
            default -> bonusEntry.getValue() / 2;
        };
    }

    private int getProbabilityByCommonsOrRestricted() throws InvalidXmlElementException {
        int bonus = 0;
        for (final PerkBonus bonusEntry : perk.getBonuses()) {
            if (bonusEntry.getKind() != PerkBonusKind.MAKES_COMMON
                    && bonusEntry.getKind() != PerkBonusKind.MAKES_RESTRICTED) {
                continue;
            }
            final boolean applies;
            if (bonusEntry.getCategoryId() != null) {
                applies = isInterestingCategory(bonusEntry.getCategoryId());
            } else if (bonusEntry.getSkillId() != null) {
                applies = isInterestingSkill(bonusEntry.getSkillId());
            } else {
                continue;
            }
            if (applies) {
                final int weight = bonusEntry.getCategoryId() != null ? 3 : 1;
                bonus += bonusEntry.getKind() == PerkBonusKind.MAKES_COMMON ? weight : -weight;
            }
        }
        return bonus;
    }

    private int getProbabilityByArmourClass() {
        for (final PerkBonus bonusEntry : perk.getBonuses()) {
            if (bonusEntry.isArmor()) {
                return bonusEntry.getValue() == null ? 0 : bonusEntry.getValue() - 1;
            }
        }
        return 0;
    }

    private int getProbabilityByMovement() {
        for (final PerkBonus bonusEntry : perk.getBonuses()) {
            if (bonusEntry.isMovement()) {
                return bonusEntry.getValue() == null ? 0 : bonusEntry.getValue() / 2;
            }
        }
        return 0;
    }

    private int smartRandomness() throws InvalidXmlElementException {
        if (characterPlayer.isSpellCaster() && SCEPTIC_PERK_ID.equals(perk.getId())) {
            return VETO_PROBABILITY;
        }
        return 0;
    }

    private boolean isInterestingCategory(String categoryId) {
        try {
            if (!characterPlayer.isCategoryEnabledByOptions(RulesCatalog.getInstance().getCategory(categoryId))) {
                return false;
            }
            return characterPlayer.getCategoryTotalRanks(categoryId) > 0;
        } catch (final InvalidXmlElementException e) {
            return false;
        }
    }

    private boolean isInterestingSkill(String skillId) {
        try {
            return characterPlayer.isSkillInteresting(RulesCatalog.getInstance().getSkill(skillId));
        } catch (final InvalidXmlElementException e) {
            return false;
        }
    }

    private static List<String> selectTargets(List<String> options, int count,
            Function<String, Boolean> isInteresting) {
        RandomValues.shuffle(options);
        final List<String> selected = new ArrayList<>();
        for (final String option : options) {
            if (selected.size() < count && isInteresting.apply(option)) {
                selected.add(option);
            }
        }
        for (final String option : options) {
            if (selected.size() < count && !selected.contains(option)) {
                selected.add(option);
            }
        }
        return selected;
    }

    private static boolean isCategoryScope(PerkChoiceScope scope) {
        return scope == PerkChoiceScope.ANY_CATEGORY || scope == PerkChoiceScope.ANY_WEAPON_CATEGORY;
    }

    private static List<String> getScopeOptions(PerkChoiceScope scope) throws InvalidXmlElementException {
        final List<String> ids = new ArrayList<>();
        switch (scope) {
            case ANY_CATEGORY -> {
                for (final Category category : RulesCatalog.getInstance().getCategories()) {
                    ids.add(category.getId());
                }
            }
            case ANY_WEAPON_CATEGORY -> {
                for (final Category category : RulesCatalog.getInstance().getCategories()) {
                    if (category.getId().startsWith("weapons")) {
                        ids.add(category.getId());
                    }
                }
            }
            case ANY_SKILL -> {
                for (final Skill skill : RulesCatalog.getInstance().getSkills()) {
                    ids.add(skill.getId());
                }
            }
            case ANY_WEAPON_SKILL -> {
                for (final Skill skill : RulesCatalog.getInstance().getSkills()) {
                    if (skill.getCategoryId() != null && skill.getCategoryId().startsWith("weapons")) {
                        ids.add(skill.getId());
                    }
                }
            }
            default -> {
            }
        }
        return ids;
    }
}