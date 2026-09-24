package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryType;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * How likely the random character generator is to add another rank in a category right now,
 * replicating the legacy {@code CategoryProbability} heuristics on top of the id-based NG model
 * ({@link Category#getId()} replaces the legacy Spanish category/skill names, so the suggested-ranks
 * map is keyed by raw ids instead of display names).
 *
 * <p>The returned value is a 0..90 percentage (the legacy generator caps it at 90), plus two special
 * outcomes the caller must interpret before feeding it to a
 * {@link RandomSelector}: a veto also returns {@code -100} when the category is flagged
 * {@link Category#isNotUsedInRandom()}, and the firearms/armour sanity checks of the smart bonus make
 * whole categories effectively unselected by returning large negative bonuses. Positive results never
 * exceed 90, so selecting among the positive-probability candidates by weight reproduces the legacy
 * behaviour.</p>
 *
 * <p>Two configurable knobs are hardcoded to the legacy defaults because there is no NG equivalent of
 * the legacy single-user persisted {@code Config}: {@link #CATEGORY_MAX_COST} mirrors the default of
 * {@code Config.getCategoryMaxCost()} and the armour-tag threshold values (10/20/30) remain fixed as
 * in the legacy source.</p>
 */
public class CategoryProbability {

    /** Result returned when the category is vetoed for random generation (see {@link Category#isNotUsedInRandom()}). */
    public static final int VETO_PROBABILITY = -100;

    /** A very negative smart bonus that makes a firearms category unselectable when firearms are disallowed. */
    private static final int FIREARMS_DISALLOWED_BONUS = -10000;

    /** A very negative smart bonus that makes an armour category unselectable once its total value exceeds its threshold. */
    private static final int ARMOUR_EXCEEDED_BONUS = -1000;

    /** The smart bonus awarded for a category the character is already expected to master. */
    private static final int COMMON_OR_PROFESSIONAL_SKILL_BONUS = 50;

    /** Development cost ceiling below which the probability calculation applies (legacy {@code Config} default). */
    private static final int CATEGORY_MAX_COST = 50;

    /** Legacy cap for the final probability. */
    private static final int MAX_PROBABILITY = 90;

    private static final int PREFERRED_CATEGORY_CAP = 30;
    private static final int COST_PROBABILITY_BASE = 50;
    private static final int ZERO_RANK_BASE = 30;
    private static final int SKILL_WITH_RANKS_BONUS = 20;
    private static final int COMMON_SKILL_BONUS = 20;

    private static final String WEAPONS_FIREARM_ONE_HANDED = "weaponsFirearmOneHanded";
    private static final String WEAPONS_FIREARM_TWO_HANDED = "weaponsFirearmTwoHanded";
    private static final String ARMOR_LIGHT = "armorLight";
    private static final String ARMOR_MIDDLE = "armorMiddle";
    private static final String ARMOR_HEAVY = "armorHeavy";

    private final CharacterPlayer characterPlayer;
    private final Category category;
    private final Map<String, Integer> suggestedSkillsRanks;
    private final int specializationLevel;
    private final int finalLevel;

    /**
     * @param suggestedSkillsRanks desired total ranks per category/skill id (lifted from a legacy
     *                             pre-generated character, e.g. one archetype), {@code null} when no
     *                             target profile applies. Both the category itself and its skills are
     *                             keyed by raw id.
     * @param specializationLevel   how much the character should invest in the categories it already
     *                              has ranks in (the legacy {@code specializationLevel}, a "rank this
     *                              far" multiplier).
     * @param finalLevel            the level the character is being generated up to.
     */
    public CategoryProbability(CharacterPlayer characterPlayer, Category category, Map<String, Integer> suggestedSkillsRanks,
            int specializationLevel, int finalLevel) {
        this.characterPlayer = characterPlayer;
        this.category = category;
        this.suggestedSkillsRanks = suggestedSkillsRanks;
        this.specializationLevel = specializationLevel;
        this.finalLevel = finalLevel;
    }

    /**
     * Probability of adding another rank to {@link #category}, matching the legacy {@code
     * CategoryProbability#rankProbability()} exactly (veto, affordability and suggested-ranks short
     * circuits first, then the characteristic/preferred/cost/common/smart bonus accumulation capped
     * at 90).
     */
    public int rankProbability() throws InvalidXmlElementException {
        if (category.isNotUsedInRandom()) {
            return VETO_PROBABILITY;
        }

        final Integer cost = getNewRankCost();

        if (cost == null || cost > characterPlayer.getRemainingDevelopmentPoints()) {
            return 0;
        }

        // Suggested ranks: buy straight to the target profile, skipping the bonus heuristics.
        if (suggestedSkillsRanks != null && suggestedSkillsRanks.get(category.getId()) != null) {
            final Integer suggested = suggestedSkillsRanks.get(category.getId());
            if (characterPlayer.getCategoryTotalRanks(category.getId()) < suggested) {
                if (characterPlayer.getCurrentLevelRanks(category) == 0) {
                    return 100;
                } else if (characterPlayer.getCategoryTotalRanks(category.getId()) < suggested - finalLevel
                        && cost < CharacterPlayer.MAX_REASONABLE_COST) {
                    return 100;
                }
            }
        }

        if (cost <= CATEGORY_MAX_COST
                && characterPlayer.getRemainingDevelopmentPoints() >= cost
                && category.getType() == CategoryType.STANDARD) {
            int probability = characterPlayer.getCategoryCharacteristicBonus(category);
            probability += preferredCategory();
            probability += categoryCostProbability();
            probability += commonSkillBonus();
            probability += smartBonus();
            return Math.min(probability, MAX_PROBABILITY);
        }
        return 0;
    }

    /** The development point cost of the next rank in this category at the current level (the legacy {@code getNewRankCost(Category)}). */
    private Integer getNewRankCost() throws InvalidXmlElementException {
        return characterPlayer.getCategoryDevelopmentCost(category.getId(), characterPlayer.getCurrentLevelRanks(category));
    }

    /**
     * How much the next rank costs (bounded bonus: the cheaper the better, never going below 1),
     * matching the legacy {@code categoryCostProbability()}.
     */
    private int categoryCostProbability() throws InvalidXmlElementException {
        final int cost = getNewRankCost();
        return Math.max(1, COST_PROBABILITY_BASE - cost * cost * 2);
    }

    /** Prefers categories the character already specializes in, capped at 30. */
    private int preferredCategory() {
        final int probability = characterPlayer.getCategoryTotalRanks(category.getId()) * (specializationLevel + 4);
        return Math.min(probability, PREFERRED_CATEGORY_CAP);
    }

    /** +20 per common skill of the category, matching the legacy {@code bonusCommonSkills()}. */
    private int commonSkillBonus() throws InvalidXmlElementException {
        int bonus = 0;
        for (final String skillId : category.getSkills()) {
            final Skill skill;
            try {
                skill = RulesCatalog.getInstance().getSkill(skillId);
            } catch (final InvalidXmlElementException e) {
                continue;
            }
            if (characterPlayer.isSkillCommon(skill)) {
                bonus += COMMON_SKILL_BONUS;
            }
        }
        if (characterPlayer.getCategoryTotalRanks(category.getId()) >= 0) {
            return bonus;
        }
        return bonus / 10;
    }

    /**
     * Heuristic tweaks matching the legacy {@code smartBonus()}: veto firearms categories when
     * firearms are disallowed, veto armour categories past their total-value threshold, push empty
     * categories with skilled/suggested skills or common/professional skills, and cool down heavily
     * invested categories.
     */
    private int smartBonus() throws InvalidXmlElementException {
        if (!characterPlayer.isFirearmsAllowed() && isFirearmCategory()) {
            return FIREARMS_DISALLOWED_BONUS;
        }
        if (armourTotalExceeds(ARMOR_LIGHT, 10) || armourTotalExceeds(ARMOR_MIDDLE, 20) || armourTotalExceeds(ARMOR_HEAVY, 30)) {
            return ARMOUR_EXCEEDED_BONUS;
        }

        int bonus = 0;
        if (characterPlayer.getCategoryTotalRanks(category.getId()) == 0) {
            bonus += ZERO_RANK_BASE + characterPlayer.getCategorySkillsWithRanks(category).size() * SKILL_WITH_RANKS_BONUS;
            bonus += ZERO_RANK_BASE + getSkillsWithSuggestedRanks().size() * SKILL_WITH_RANKS_BONUS;
            if (characterPlayer.hasCommonOrProfessionalSkills(category)) {
                bonus += COMMON_OR_PROFESSIONAL_SKILL_BONUS;
            }
        }
        final int totalRanks = characterPlayer.getCategoryTotalRanks(category.getId());
        if (totalRanks > 10) {
            bonus -= (8 - totalRanks) * 10;
        }
        return bonus;
    }

    private boolean isFirearmCategory() {
        return WEAPONS_FIREARM_ONE_HANDED.equals(category.getId()) || WEAPONS_FIREARM_TWO_HANDED.equals(category.getId());
    }

    private boolean armourTotalExceeds(String armourCategoryId, int threshold) throws InvalidXmlElementException {
        if (!armourCategoryId.equals(category.getId())) {
            return false;
        }
        final Category armourCategory = RulesCatalog.getInstance().getCategory(armourCategoryId);
        return characterPlayer.getCategoryTotalBonus(armourCategory) > threshold;
    }

    /** Every skill of the category with a positive suggested rank, matching the legacy {@code getSkillsWithSuggestedRanks()}. */
    private List<Skill> getSkillsWithSuggestedRanks() {
        final List<Skill> skills = new ArrayList<>();
        if (suggestedSkillsRanks == null) {
            return skills;
        }
        for (final String skillId : category.getSkills()) {
            if (getSuggestedSkillRanks(skillId) > 0) {
                try {
                    skills.add(RulesCatalog.getInstance().getSkill(skillId));
                } catch (final InvalidXmlElementException e) {
                    // Dangling skill id referenced by a dynamic category: ignore it.
                }
            }
        }
        return skills;
    }

    private Integer getSuggestedSkillRanks(String skillId) {
        final Integer suggested = suggestedSkillsRanks.get(skillId);
        if (suggested == null || suggested < 0) {
            return 0;
        }
        return suggested;
    }
}