package com.softwaremagico.librodeesher.pdf.legacy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryFactory;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.magic.MagicListType;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.ProfessionCategoryCost;
import com.softwaremagico.librodeesher.profession.ProfessionMagicCost;
import com.softwaremagico.librodeesher.profession.ProfessionWeaponCostTier;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.race.RaceSpecial;
import com.softwaremagico.librodeesher.resistance.ResistanceType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.skill.SkillFactory;
import com.softwaremagico.librodeesher.skill.SkillType;

/**
 * The handful of legacy values the character sheet paints that no single NG accessor returns.
 *
 * <p>NG folded the legacy {@code CharacterPlayer} bookkeeping into fewer, larger methods, each one
 * documented against the legacy method it replaces. The printed sheet, however, showed each piece in
 * its own column, so the legacy decomposition has to be reconstructed here rather than read off one
 * call. Each method below documents the legacy formula it reproduces.</p>
 */
final class LegacyValues {

    private static List<Category> availableCategories;

    private LegacyValues() {
        // Utility class.
    }

    /**
     * The legacy {@code CharacterPlayer#getRanksValue(Skill)}: the value of the skill's "real" ranks
     * (see {@link CharacterPlayer#getSkillRealRanks(Skill)}), which is the only part of
     * {@link CharacterPlayer#getSkillDevelopmentBonus(Category, String)} that comes from ranks.
     */
    static int skillRanksValue(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        return characterPlayer.getSkillRankValue(categoryOf(skill), characterPlayer.getSkillRealRanks(skill));
    }

    /**
     * The legacy {@code CharacterPlayer#getSpecializedTotalValue(Skill)}: the specialization's own
     * ranks value plus the flat bonuses plus the value of its skill's category.
     */
    static int specializedSkillTotalValue(CharacterPlayer characterPlayer, Skill skill)
            throws InvalidXmlElementException {
        final Category category = categoryOf(skill);
        return characterPlayer.getSpecializedSkillTotalBonus(category, skill.getId());
    }

    /**
     * The legacy {@code CategoryCost#getCostTag()}: the whole rank-cost progression of a category,
     * joined with slashes ("3/2/2/1"). It is printed in the category table so the player can see what
     * each rank will cost. A weapon category has no per-category cost table: the legacy sheet showed
     * the cost progression of the {@code WEAPONx} tier the player assigned to it, which is what NG
     * re-exposes through {@code CharacterPlayer#getAssignedWeaponCategoryCostTier}, so that tier is
     * used here too.
     */
    static String categoryCostTag(CharacterPlayer characterPlayer, Category category)
            throws InvalidXmlElementException {
        ProfessionCategoryCost cost = characterPlayer.getProfessionCategoryCost(category.getId());
        if (cost == null) {
            final ProfessionWeaponCostTier weaponTier = characterPlayer.getAssignedWeaponCategoryCostTier(category.getId());
            if (weaponTier == null) {
                // The legacy getCategoryCost of a synthetic "Listas ... de Hechizos" category was the
                // profession's magic cost for that list type (one bracket per rank range), so its cost
                // tag is the whole rank-cost progression of the same bracket the list ranks start in.
                final MagicListType listType = MagicListType.fromCategoryId(category.getId());
                if (listType != null) {
                    final Profession profession = characterPlayer.getProfession();
                    final ProfessionMagicCost magicCost = profession == null ? null : profession.getMagicCost(listType, 0);
                    if (magicCost != null) {
                        return costTag(magicCost.getRankCosts());
                    }
                }
                // No profession, or that profession has no cost table for this category: the legacy
                // sheet printed nothing in the cost column rather than failing.
                return "";
            }
            return costTag(weaponTier.getRankCosts());
        }
        return costTag(cost.getRankCosts());
    }

    private static String costTag(List<Integer> rankCosts) {
        final StringBuilder tag = new StringBuilder();
        for (int i = 0; i < rankCosts.size(); i++) {
            tag.append(rankCosts.get(i));
            if (i < rankCosts.size() - 1) {
                tag.append("/");
            }
        }
        return tag.toString();
    }

    /**
     * The legacy {@code CharacterPlayer#getSelectedTrainings()}: every training the character picked,
     * across all levels.
     */
    static List<String> selectedTrainings(CharacterPlayer characterPlayer) {
        final List<String> trainings = new ArrayList<>();
        for (final LevelUp levelUp : characterPlayer.getLevels()) {
            trainings.addAll(levelUp.getTrainings());
        }
        return trainings;
    }

    /**
     * The legacy {@code Race#getProgressionRankValuesAsString(List)}: a list of progression values
     * joined with slashes, rounding each one.
     */
    static String progressionValues(List<Float> values) {
        final StringBuilder tag = new StringBuilder();
        if (values != null) {
            for (final Float value : values) {
                if (tag.length() > 0) {
                    tag.append("/");
                }
                tag.append(Math.round(value));
            }
        }
        return tag.toString();
    }

    /**
     * Every category, ordered by name: the legacy {@code CategoryFactory} finished by sorting its
     * {@code availableCategoriesByName} list, and the sheet walked that order to place each category
     * on its row (the XML files are stored roughly alphabetically, but the twelve "Listas ... de
     * Hechizos" categories are not in that order there, hence the explicit sort).
     */
    static List<Category> availableCategories() throws InvalidXmlElementException {
        if (availableCategories == null) {
            availableCategories = new ArrayList<>(CategoryFactory.getInstance().getElements());
            availableCategories.sort(Comparator.comparing(Category::getName));
        }
        return availableCategories;
    }

    /**
     * The skills a category develops, in the category's own order.
     *
     * <p>{@code categories.xml} references about 200 skill ids that {@code skills.xml} does not
     * define, so resolving each id can fail. Those skills have no name, type or costs to print, and
     * the sheet has always been driven by the skills the catalog actually knows, so they are skipped
     * rather than failing the whole export.</p>
     *
     * <p>The nine weapon categories keep the legacy "skillsRaw noimporta" marker instead of a
     * skill list, but every weapon skill in {@code skills.xml} records the category it belongs to,
     * so those skills are listed by that cross-reference, exactly as the legacy sheet listed the
     * weapon skills of its own categories.</p>
     */
    static List<Skill> skillsOf(Category category) throws InvalidXmlElementException {
        final List<Skill> skills = new ArrayList<>();
        for (final String skillId : category.getSkills()) {
            try {
                final Skill skill = RulesCatalog.getInstance().getSkill(skillId);
                if (skill != null) {
                    skills.add(skill);
                }
            } catch (InvalidXmlElementException e) {
                // Referenced by the category but not defined in the rules: nothing to print.
            }
        }
        if (skills.isEmpty()) {
            for (final Skill skill : SkillFactory.getInstance().getElements()) {
                if (category.getId().equals(skill.getCategoryId())) {
                    skills.add(skill);
                }
            }
        }
        return skills;
    }

    /**
     * The legacy {@code Spanish.PHISICAL_DEVELOPMENT_SKILL} skill, which the sheet printed the total
     * value of in two places.
     */
    static Skill physicalDevelopmentSkill() throws InvalidXmlElementException {
        return RulesCatalog.getInstance().getSkill("physicalDevelopment");
    }

    /**
     * The legacy {@code Spanish.POWER_POINTS_DEVELOPMENT_SKILL} skill, whose total value the sheet
     * printed as the character's power points.
     */
    static Skill powerPointDevelopmentSkill() throws InvalidXmlElementException {
        return RulesCatalog.getInstance().getSkill("powerPointDevelopment");
    }

    /**
     * The legacy {@code CharacterPlayer#getSkillNameWithSufix(Skill)}: the skill's name followed by
     * the suffix of its rank-cost variant, so a hand-filled sheet still says which skills were
     * restricted, professional, common or generalized.
     */
    static String skillNameWithSufix(CharacterPlayer characterPlayer, Skill skill) {
        String skillName = LegacyTextBlocks.name(skill.getName());
        final SkillType skillType = skill.getSkillType();
        if (skillType == SkillType.GENERALIZED) {
            skillName += " " + SkillType.GENERALIZED.getTag();
        }
        if (skillType == SkillType.PROFESSIONAL) {
            skillName += " " + SkillType.PROFESSIONAL.getTag();
        } else if (skillType == SkillType.COMMON) {
            skillName += " " + SkillType.COMMON.getTag();
        }
        if (skillType == SkillType.RESTRICTED) {
            skillName += " " + SkillType.RESTRICTED.getTag();
        }
        return skillName;
    }

    /**
     * The legacy {@code CharacterPlayer#getSimpleBonus(Skill)}: every flat bonus the skill gets
     * (profession, background, perks including their per-rank part, conditional perks and race),
     * without its ranks value and without item bonuses. The legacy sheet painted this in the
     * "bonificación" column next to the ranks value.
     */
    static int skillSimpleBonus(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        return characterPlayer.getSkillDevelopmentBonus(categoryOf(skill), skill.getId()) - skillRanksValue(characterPlayer, skill);
    }

    /**
     * The legacy {@code CharacterPlayer#getTotalValue(Category)}, which the skill row painted next to
     * the skill's ranks value: the total value of the skill's own category, the same number on
     * every row of that category and without the skill's own bonus.
     */
    static int categoryTotalValue(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        return characterPlayer.getCategoryTotalBonus(categoryOf(skill));
    }

    /**
     * The legacy {@code CharacterPlayer#getPreviousRanks(Skill)}: the ranks the character already had
     * before the current level. The legacy total was {@code previous + current - specialities}, and
     * NG's {@link CharacterPlayer#getSkillTotalRanks(String)} already subtracts those specialities, so
     * the previous ranks are the total plus that cost, minus the current level's ranks.
     */
    static int previousSkillRanks(CharacterPlayer characterPlayer, Skill skill) {
        return characterPlayer.getSkillTotalRanks(skill.getId())
                + characterPlayer.getSkillSpecializationsRankCost(skill.getId())
                - characterPlayer.getCurrentLevel().getSkillRanks(skill.getId());
    }

    /**
     * The legacy {@code CharacterPlayer#getPreviousRanks(Category)}: the ranks bought before the
     * current level. Unlike skills, the legacy category total was simply {@code previous + current}.
     */
    static int previousCategoryRanks(CharacterPlayer characterPlayer, Category category) {
        return characterPlayer.getCategoryTotalRanks(category.getId())
                - characterPlayer.getCurrentLevel().getCategoryRanks(category.getId());
    }

    /**
     * The legacy {@code CharacterPlayer#getRanksValue(Category)}: the value of the ranks bought in
     * the category.
     */
    static int categoryRanksValue(CharacterPlayer characterPlayer, Category category) {
        return category.getCategoryRankBonus(characterPlayer.getCategoryTotalRanks(category.getId()));
    }

	/**
     * The legacy {@code CharacterPlayer#getTotalValue(Category)}: the category's ranks value plus
     * every bonus it grants (its flat bonuses, its item bonus and the characteristic bonuses of its
     * characteristics), which is what {@code getCategoryTotalBonus} already adds up, so the ranks
     * value and the characteristic bonuses must not be counted a second time here. The skill rows of
     * the sheet showed it in their own column.
     */
    static int categoryTotalValue(CharacterPlayer characterPlayer, Category category)
            throws InvalidXmlElementException {
        return characterPlayer.getCategoryTotalBonus(category);
    }

    /**
     * The category table's own "bonificación" column: the legacy {@code
     * CharacterPlayer#getSimpleBonus(Category)}, i.e. the category's fixed bonus plus the flat
     * profession and race bonuses, without ranks, items, perks or characteristics.
     */
    static int categoryBonus(Category category, CharacterPlayer characterPlayer)
            throws InvalidXmlElementException {
        int raceBonus = 0;
        if (characterPlayer.getRace() != null) {
            final Integer bonus = characterPlayer.getRace().getCategoryBonus(category.getId());
            raceBonus = bonus == null ? 0 : bonus;
        }
        return category.getFixedBonus() + professionCategoryBonus(characterPlayer, category) + raceBonus;
    }

    /**
     * The race's own bonus for a resistance (the legacy {@code Race#getResistancesBonus}), keyed by
     * the resistance's Spanish tag as the race XML stores it.
     */
    static int raceResistanceBonus(Race race, ResistanceType resistance) {
        if (race == null) {
            return 0;
        }
        final Integer bonus = race.getResistanceBonuses().get(resistance.name());
        return bonus == null ? 0 : bonus;
    }

    /**
     * The legacy {@code Race#getProgressionRankValuesAsString(ProgressionCostType)}: the race's own
     * development cost progression for one of its tables, joined with slashes. NG stores those
     * already joined, keyed by the XML element name ({@code physicalDevelopment}, {@code ppEssence},
     * ...).
     */
    static String progressionRankValues(Race race, String progressionKey) {
        if (race == null) {
            return "";
        }
        final String values = race.getProgressionRankValues().get(progressionKey);
        return values == null ? "" : values;
    }

    /**
     * A character under construction may not have a race, profession, culture or background picked
     * yet. The legacy sheet was only ever asked to print finished characters, so it dereferenced
     * these blindly; here they resolve to the empty value so a partially-built character still gets
     * a sheet rather than an exception.
     */
    static String raceName(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        return characterPlayer.getRace() == null ? ""
                : LegacyTextBlocks.name(characterPlayer.getRace().getName());
    }

    static String professionName(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        return characterPlayer.getProfession() == null ? ""
                : LegacyTextBlocks.name(characterPlayer.getProfession().getName());
    }

    static String cultureName(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        return characterPlayer.getCulture() == null ? ""
                : LegacyTextBlocks.name(characterPlayer.getCulture().getName());
    }

    /** The race's racial specials, empty while no race is selected. */
    static List<String> raceSpecials(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        if (characterPlayer.getRace() == null) {
            return List.of();
        }
        final List<String> specials = new ArrayList<>();
        for (final RaceSpecial special : characterPlayer.getRace().getSpecials()) {
            specials.add(LegacyTextBlocks.name(special.getText()));
        }
        return specials;
    }

    static Integer raceExpectedLifeYears(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        return characterPlayer.getRace() == null ? 0 : characterPlayer.getRace().getExpectedLifeYears();
    }

    static Integer raceSoulDepartTime(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        return characterPlayer.getRace() == null ? 0 : characterPlayer.getRace().getSoulDepartTime();
    }

    static Double raceRestorationTime(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        if (characterPlayer.getRace() == null || characterPlayer.getRace().getRestorationTime() == null) {
            return 0d;
        }
        return characterPlayer.getRace().getRestorationTime();
    }

    static Integer raceNaturalArmorType(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        return characterPlayer.getRace() == null ? 0 : characterPlayer.getRace().getNaturalArmorType();
    }

    /** The selected profession's flat bonus on a skill, 0 while no profession is selected. */
    static int professionSkillBonus(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        return characterPlayer.getProfessionBonus(skill.getId());
    }

    static int professionCategoryBonus(CharacterPlayer characterPlayer, Category category) throws InvalidXmlElementException {
        return characterPlayer.getProfessionBonus(category.getId());
    }

    static int backgroundSkillBonus(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        if (characterPlayer.getBackground() == null) {
            return 0;
        }
        final Integer bonus = characterPlayer.getBackground().getSkillBonus(skill.getId());
        return bonus == null ? 0 : bonus;
    }

    static int backgroundCategoryBonus(CharacterPlayer characterPlayer, Category category) throws InvalidXmlElementException {
        if (characterPlayer.getBackground() == null) {
            return 0;
        }
        final Integer bonus = characterPlayer.getBackground().getCategoryBonus(category.getId());
        return bonus == null ? 0 : bonus;
    }

    private static Category categoryOf(Skill skill) throws InvalidXmlElementException {
        return RulesCatalog.getInstance().getCategory(skill.getCategoryId());
    }
}
