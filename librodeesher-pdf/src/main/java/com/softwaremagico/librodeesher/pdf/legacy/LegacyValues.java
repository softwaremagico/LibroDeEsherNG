package com.softwaremagico.librodeesher.pdf.legacy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryFactory;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.language.LanguageSlot;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.magic.MagicListType;
import com.softwaremagico.librodeesher.magic.MagicSpellList;
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

    /** The Core "Comunicación" category, where the legacy app added every race language. */
    private static final String COMMUNICATION_CATEGORY_ID = "communication";

    private static List<Category> availableCategories;

    static {
        ModuleManager.registerResettable(() -> availableCategories = null);
    }

    private LegacyValues() {
        // Utility class.
    }

    /**
     * The legacy {@code CharacterPlayer#getRanksValue(Skill)}: the value of the skill's "real" ranks
     * (see {@link CharacterPlayer#getSkillRealRanks(Skill)}), which is the only part of
     * {@link CharacterPlayer#getSkillDevelopmentBonus(Category, String)} that comes from ranks.
     */
    static int skillRanksValue(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        return skillRanksValue(characterPlayer, skill, null);
    }
    static int skillRanksValue(CharacterPlayer characterPlayer, Skill skill, Category categoryContext) throws InvalidXmlElementException {
        Category cat = categoryContext != null ? categoryContext : categoryOf(skill);
        return characterPlayer.getSkillRankValue(cat, characterPlayer.getSkillRealRanks(skill));
    }

    /**
     * The legacy {@code CharacterPlayer#getSpecializedTotalValue(Skill)}: the specialization's own
     * ranks value plus the flat bonuses plus the value of its skill's category.
     */
    static int specializedSkillTotalValue(CharacterPlayer characterPlayer, Skill skill)
            throws InvalidXmlElementException {
        return specializedSkillTotalValue(characterPlayer, skill, null);
    }
    static int specializedSkillTotalValue(CharacterPlayer characterPlayer, Skill skill, Category categoryContext)
            throws InvalidXmlElementException {
        final Category category = categoryContext != null ? categoryContext : categoryOf(skill);
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

    /**
     * Whether the legacy {@code CharacterPlayer#getCategoryCost(Category, 0)} found a cost for the
     * category: its own table in the selected profession, the {@code WEAPONx} tier the player
     * assigned to a weapon category, or the profession's magic cost for the list type of a spell
     * list. The combined sheet used it to leave out the categories the character could not develop,
     * which is why a spell list of another realm was still printed: the profession defines a cost
     * for every list type, not only for the realms it may use.
     */
    static boolean hasCategoryCost(CharacterPlayer characterPlayer, Category category)
            throws InvalidXmlElementException {
        if (characterPlayer.getProfessionCategoryCost(category.getId()) != null
                || characterPlayer.getAssignedWeaponCategoryCostTier(category.getId()) != null) {
            return true;
        }
        final MagicListType listType = MagicListType.fromCategoryId(category.getId());
        if (listType == null) {
            return false;
        }
        final Profession profession = characterPlayer.getProfession();
        return profession != null && profession.getMagicCost(listType, 0) != null;
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
     * across all levels, printed with its display name. The legacy {@code LevelUp} stored the
     * training names themselves, while the NG level-up bookkeeps their ids, so each id is resolved
     * against the {@link RulesCatalog} here (an unknown id is printed as-is rather than failing the
     * whole sheet).
     */
    static List<String> selectedTrainings(CharacterPlayer characterPlayer) {
        final List<String> trainings = new ArrayList<>();
        for (final LevelUp levelUp : characterPlayer.getLevels()) {
            for (final String trainingId : levelUp.getTrainings()) {
                trainings.add(trainingName(trainingId));
            }
        }
        return trainings;
    }

    private static String trainingName(String trainingId) {
        try {
            return LegacyTextBlocks.name(RulesCatalog.getInstance().getTraining(trainingId).getName());
        } catch (InvalidXmlElementException e) {
            return trainingId;
        }
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
     * The skills a category develops, in the alphabetical order the legacy sheet painted them.
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
     *
     * <p>The legacy catalog was sorted by the Spanish skill name, so both pages of the legacy sheet
     * rendered each category's skills alphabetically, and the NG catalog order (which only matches
     * in most categories) is normalized here to the same alphabetical order.</p>
     */
    static List<Skill> skillsOf(Category category) throws InvalidXmlElementException {
        return skillsOf(null, category);
    }

    static List<Skill> skillsOf(CharacterPlayer characterPlayer, Category category) throws InvalidXmlElementException {
        final MagicListType listType = MagicListType.fromCategoryId(category.getId());
        if (listType != null && characterPlayer != null) {
            final List<Skill> spellListSkills = new ArrayList<>();
            for (final MagicSpellList spellList : RulesCatalog.getInstance().getSpellLists()) {
                try {
                    if (characterPlayer.classifySpellList(spellList.getId()) == listType) {
                        final Skill skillWrapper = new Skill(spellList.getId());
                        skillWrapper.setName(spellList.getName());
                        skillWrapper.setCategoryId(category.getId());
                        spellListSkills.add(skillWrapper);
                    }
                } catch (Exception e) {
                    // Skip if classification fails
                }
            }
            spellListSkills.sort((a, b) -> {
                try {
                    return a.getName().getSpanish().compareTo(b.getName().getSpanish());
                } catch (Exception ex) {
                    return 0;
                }
            });
            return spellListSkills;
        }
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
        skills.addAll(regionalLanguageSkills(characterPlayer, category));
        skills.sort((a, b) -> {
            try {
                return a.getName().getSpanish().compareTo(b.getName().getSpanish());
            } catch (Exception e) {
                return 0;
            }
        });
        return deduplicateByName(characterPlayer, skills);
    }

    /**
     * The legacy application created a "Hablar/Escribir Idioma regional" skill for every language
     * line of the race (see the legacy {@code Race#setLanguages}, which added them to the
     * Communication category), so an unassigned optional language slot still printed a speaking and a
     * writing row with the slot's starting ranks. NG models those slots as {@link LanguageSlot}s, so
     * there is no catalog skill to print; these synthetic skills restore the rows (their ranks are
     * resolved by {@link CharacterPlayer}, and a character with no unassigned slot simply gets zero
     * ranks, which the sheet filters out). Only the Communication category gets them.
     */
    private static List<Skill> regionalLanguageSkills(CharacterPlayer characterPlayer, Category category) {
        final List<Skill> regionalLanguages = new ArrayList<>();
        if (characterPlayer == null || !COMMUNICATION_CATEGORY_ID.equals(category.getId())) {
            return regionalLanguages;
        }
        regionalLanguages.add(regionalLanguageSkill(CharacterPlayer.REGIONAL_LANGUAGE_SPEAKING_ID,
                "Hablar Idioma regional", "Speak Regional Language", category.getId()));
        regionalLanguages.add(regionalLanguageSkill(CharacterPlayer.REGIONAL_LANGUAGE_WRITING_ID,
                "Escribir Idioma regional", "Write Regional Language", category.getId()));
        return regionalLanguages;
    }

    private static Skill regionalLanguageSkill(String id, String spanishName, String englishName, String categoryId) {
        final Skill skill = new Skill(id);
        skill.setName(spanishName, englishName);
        skill.setCategoryId(categoryId);
        return skill;
    }

    /**
     * The NG catalog lists some skills twice under the same Spanish name (e.g. the Core
     * {@code hablarHablaCommon} and the RacesAndCultures {@code hablarCommonSpeech} are both "Hablar
     * Habla Común"). The legacy catalog had each once, so the sheet must print each name once. When a
     * character is available the entry actually populated with ranks wins; otherwise the first is
     * kept.
     */
    private static List<Skill> deduplicateByName(CharacterPlayer characterPlayer, List<Skill> skills) {
        final Map<String, Skill> unique = new LinkedHashMap<>();
        for (final Skill skill : skills) {
            String name;
            try {
                name = skill.getName().getSpanish();
            } catch (Exception e) {
                name = skill.getId();
            }
            final Skill previous = unique.get(name);
            if (previous == null || isMorePopulated(characterPlayer, skill, previous)) {
                unique.put(name, skill);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static boolean isMorePopulated(CharacterPlayer characterPlayer, Skill candidate, Skill previous) {
        if (characterPlayer == null) {
            return false;
        }
        try {
            return characterPlayer.getSkillTotalRanks(candidate.getId())
                    > characterPlayer.getSkillTotalRanks(previous.getId());
        } catch (Exception e) {
            return false;
        }
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
     * restricted, professional, common or generalized. Which variant a skill is depends on the
     * character (its profession, race, trainings and perks), exactly as in the legacy
     * {@code isProfessional(Skill)}/{@code isCommon(Skill)}/{@code isRestricted(Skill)}, so a
     * standard skill such as "Demonolog&#237;a" is painted as "(p)" when only the character's
     * profession may take it.
     */
    static String skillNameWithSufix(CharacterPlayer characterPlayer, Skill skill)
            throws InvalidXmlElementException {
        String skillName = LegacyTextBlocks.name(skill.getName());
        if (characterPlayer.isSkillGeneralized(skill.getId())) {
            skillName += " " + SkillType.GENERALIZED.getTag();
        }
        if (characterPlayer.isSkillProfessional(skill)) {
            skillName += " " + SkillType.PROFESSIONAL.getTag();
        } else if (characterPlayer.isSkillCommon(skill)) {
            skillName += " " + SkillType.COMMON.getTag();
        }
        if (characterPlayer.isSkillRestricted(skill)) {
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
        return skillSimpleBonus(characterPlayer, skill, null);
    }
    static int skillSimpleBonus(CharacterPlayer characterPlayer, Skill skill, Category categoryContext) throws InvalidXmlElementException {
        Category cat = categoryContext != null ? categoryContext : categoryOf(skill);
        return characterPlayer.getSkillDevelopmentBonus(cat, skill.getId()) - skillRanksValue(characterPlayer, skill, cat);
    }

    /**
     * The legacy {@code CharacterPlayer#getTotalValue(Category)}, which the skill row painted next to
     * the skill's ranks value: the total value of the skill's own category, the same number on
     * every row of that category and without the skill's own bonus.
     */
    static int categoryTotalValue(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        return categoryTotalValue(characterPlayer, skill, null);
    }
    static int categoryTotalValue(CharacterPlayer characterPlayer, Skill skill, Category categoryContext) throws InvalidXmlElementException {
        Category cat = categoryContext != null ? categoryContext : categoryOf(skill);
        return characterPlayer.getCategoryTotalBonus(cat);
    }

    /**
     * The legacy {@code CharacterPlayer#getPreviousRanks(Skill)}: the ranks the character already had
     * before the current level. The legacy total was {@code previous + current - specialities}, and
     * NG's {@link CharacterPlayer#getSkillTotalRanks(String)} already subtracts those specialities, so
     * the previous ranks are the total plus that cost, minus the current level's ranks. The legacy
     * "current level" only held the ranks *bought* there, never the ones a training granted (those are
     * part of {@code getTrainingRanks}, counted among the previous ranks), so the current level's
     * training-granted ranks are kept.
     */
    static int previousSkillRanks(CharacterPlayer characterPlayer, Skill skill) {
        final LevelUp current = characterPlayer.getCurrentLevel();
        final int currentBoughtRanks = current == null ? 0
                : current.getSkillRanks(skill.getId()) - current.getTrainingSkillRanks(skill.getId());
        return characterPlayer.getSkillTotalRanks(skill.getId())
                + characterPlayer.getSkillSpecializationsRankCost(skill.getId()) - currentBoughtRanks;
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
        if (skill.getCategoryId() != null && !skill.getCategoryId().isEmpty()) {
            try {
                return RulesCatalog.getInstance().getCategory(skill.getCategoryId());
            } catch (Exception e) {
                // fall through
            }
        }
        return null;
    }
}
