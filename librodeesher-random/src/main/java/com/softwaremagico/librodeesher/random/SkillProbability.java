package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryType;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.skill.SkillGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * How likely the random character generator is to add another rank to a skill right now, mirroring
 * the legacy {@code SkillProbability} heuristic structure on the id-based NG model. The regular
 * cost/rank/specialization/favourite/common/professional/restricted terms reproduce the legacy math
 * exactly; the checks the legacy performed on Spanish <em>names</em> (e.g. the horse/wolf/bear/camel
 * mount rules, the "regional/cultural/general knowledge" categories or the famous spell-list names
 * of {@code wizardPreferredSkills}) have no equivalent id in the migrated modules, so they are
 * replaced by the closest id-based test (communication category, "rock" skills, armour categories,
 * racial attacks, weapon categories) or dropped where no NG element matches (returning 0, the same
 * neutral value the absent legacy bonus contributed).
 *
 * <p>Like its legacy counterpart this class is pure RNG-free ranking; {@link #getRankProbability()}
 * returns a percentage in a [0..150] band (capped at 90 when between 90 and 150), 0 when nothing is
 * affordable, 100 while a suggested target is not yet reached, and a large negative value for
 * skills that must be avoided for this character. Spells are handled like in the NG data model: a
 * skill whose category is a spell list ranks up as the whole list does.</p>
 */
public class SkillProbability {

    private static final int MAX_VALUE = 200;
    private static final int LAST_DEVELOPMENT_POINTS_RANGE = 10;

    private static final String COMMUNICATION_CATEGORY = "communication";
    private static final String RACIAL_ATTACK_PREFIX = "attackRacial";
    private static final String MARTIAL_ARTS_PREFIX = "martialArts";
    private static final String ADRENAL_TAG = "adrenal";
    private static final String LYCHANTROPY_TAG = "lycanthrope";

    // NG ids of the legacy Spanish-named mount skills and knowledge skills, see
    // randomnessByRace()/culturalSkill().
    private static final String MOUNT_HORSES = "montarCaballos";
    private static final String MOUNT_WOLVES = "montarLobos";
    private static final String MOUNT_BEARS = "montarOsos";
    private static final String MOUNT_CAMELS = "montarCamellos";
    private static final String KNOWLEDGE_REGIONAL = "loreRegional";
    private static final String KNOWLEDGE_CULTURAL = "loreCultural";
    private static final String KNOWLEDGE_FAUNA = "loreOfFauna";
    private static final String KNOWLEDGE_FLORA = "loreOfFlora";

    // The five "famous" spell lists the legacy wizardPreferredSkills() favoured per realm, mapped
    // from their Spanish list names to the migrated catalog ids.
    private static final String ESSENCE_SHIELD_LIST = "essenceMasteryOfEscudos";
    private static final String ESSENCE_QUICKNESS_LIST = "essencePathsOfQuickness";
    private static final String MENTALISM_DODGE_LIST = "mentalismEvasionOfTheAttacks";
    private static final String MENTALISM_SELFHEALING_LIST = "mentalismSelfHealing";
    private static final String MENTALISM_SPEED_LIST = "mentalismSpeed";

    // Legacy Spanish substrings used to classify races, cultures and knowledge skills.
    private static final String ORC_RACE_NAME = "orco";
    private static final String DWARF_RACE_NAME = "enano";
    private static final String DESERT_CULTURE_NAME = "desierto";
    // Longest generic knowledge name ("Conocimiento de la Fauna"); a culture-qualified variant is
    // strictly longer.
    private static final int GENERIC_KNOWLEDGE_NAME_MAX_LENGTH = 23;

    private final CharacterPlayer characterPlayer;
    private final Skill skill;
    private final Map<String, Integer> suggestedSkillsRanks;
    private final int specializationLevel;
    private final int finalLevel;

    /**
     * @param suggestedSkillsRanks desired total ranks per skill id ({@code null} when no target
     *                             profile applies, e.g. none was lifted from an archetype).
     * @param specializationLevel   how much the character invests in the few skills/categories it
     *                              already favours (the legacy "one or few skills, many ranks" knob).
     * @param finalLevel            the level the character is being generated up to.
     */
    public SkillProbability(CharacterPlayer characterPlayer, Skill skill, Map<String, Integer> suggestedSkillsRanks,
            int specializationLevel, int finalLevel) {
        this.characterPlayer = characterPlayer;
        this.skill = skill;
        this.suggestedSkillsRanks = suggestedSkillsRanks;
        this.specializationLevel = specializationLevel;
        this.finalLevel = finalLevel;
    }

    /** Probability of adding a new rank to {@link #skill}, matching the legacy {@code SkillProbability#getRankProbability()}. */
    public int getRankProbability() throws InvalidXmlElementException {
        int probability = 0;

        // Avoid disabled skills.
        if (!characterPlayer.isSkillEnabled(skill) || characterPlayer.isSkillDisabledByOptions(skill)) {
            return -MAX_VALUE;
        }

        // Avoid strange skills.
        if (skill.isRare() && !isCommonForTheCharacter()) {
            return -MAX_VALUE;
        }

        final Integer cost = characterPlayer.getSkillDevelopmentCost(skill.getId(), currentLevelRanks());
        if (cost == null || cost > characterPlayer.getRemainingDevelopmentPoints()) {
            return 0;
        }

        // Suggested ranks: buy straight to the target profile.
        if (suggestedSkillsRanks != null && suggestedSkillsRanks.get(skill.getId()) != null) {
            final Integer suggested = suggestedSkillsRanks.get(skill.getId());
            if (totalRanks() < suggested) {
                if (currentLevelRanks() == 0) {
                    return 100;
                } else if (totalRanks() < suggested - finalLevel
                        && characterPlayer.getSkillDevelopmentCost(skill.getId(), currentLevelRanks()) < CharacterPlayer.MAX_REASONABLE_COST) {
                    return 100;
                }
            }
        }

        // Favourite skills are preferred.
        if (characterPlayer.getFavouriteSkillIds().contains(skill.getId())) {
            probability += 25;
        }

        // The heuristics below only push skills that are not already loaded this level.
        if (currentLevelRanks() <= 3) {
            probability += increasedCategory() / 3;
            probability += preferredSkill();
            probability += skillsPerCategory();
            probability += raceSkills();
            probability += bestSkills(cost);
            probability += skillExpensiveness(cost);
            probability += applyCharacterSpecialization();
            probability += stillNotUsedSkill(cost);
            probability += wizardPreferredSkills();
            probability += warriorsPreferredSkills();
            probability += smartRandomness();
            probability += ridicolousSkill();
            probability += culturalSkill();
            probability += maxRanks();
            if (probability > 150) {
                return 150;
            }
            if (probability > 90) {
                probability = 90;
            }
            return probability;
        }
        return 0;
    }

    /**
     * Skills without ranks that are cheap to buy: more room to spend, fewer existing ranks, the more
     * attractive they become (never negative).
     */
    private int stillNotUsedSkill(Integer cost) throws InvalidXmlElementException {
        return Math.max(characterPlayer.getLevels().size() - cost - realRanks() * 2, 0);
    }

    /**
     * Cost in development points: prohibitive costs veto the skill outright; spells are treated a
     * little softer than common skills; the last affordable development point is only usable by a
     * rank costing exactly one point.
     */
    private int skillExpensiveness(Integer cost) throws InvalidXmlElementException {
        if (cost > CharacterPlayer.MAX_REASONABLE_COST) {
            return -1000;
        }
        if (characterPlayer.isSpellSkill(skill.getId())) {
            if (characterPlayer.isHybridWizard()) {
                return 30 - (int) Math.pow(cost - 2, 2);
            }
            if (characterPlayer.isWizard()) {
                return 30 - (int) Math.pow(cost, 2);
            }
        }
        if (characterPlayer.getRemainingDevelopmentPoints() > 1) {
            return Math.max(1, 30 - (int) Math.pow(cost, 2) * 3);
        }
        return cost == 1 ? 30 : 0;
    }

    /** Some extra criteria to avoid weird characters. */
    private int smartRandomness() throws InvalidXmlElementException {
        return randomnessByRace()
                + randomnessByCulture()
                + ProfessionRandomness.preferredSkillByProfession(characterPlayer, skill, specializationLevel)
                + randomnessByRanks()
                + randomnessBySkill()
                + randomnessByOptions();
    }

    /**
     * Legacy race/nature rules ("no horses for orcs, wolves only for orcs, bears only for dwarves,
     * camels only in deserts") targeted Spanish skill names; the migrated modules keep the same
     * skills under stable ids ({@code montarCaballos}, {@code montarLobos}, {@code montarOsos},
     * {@code montarCamellos}), so the rules are keyed on them and on the translated race/culture
     * names. The elemental-mount veto had no NG equivalent (no "montura elemental" skill in the
     * migrated modules), so it keeps the neutral 0.
     */
    private int randomnessByRace() throws InvalidXmlElementException {
        final Race race = characterPlayer.getRace();
        if (race == null) {
            // Without a race there is no race-based restriction.
            return 0;
        }
        final String raceName = race.getName().getTranslatedText();
        if (MOUNT_HORSES.equals(skill.getId()) && raceName.toLowerCase().contains(ORC_RACE_NAME)) {
            return -MAX_VALUE;
        }
        if (MOUNT_WOLVES.equals(skill.getId()) && !raceName.toLowerCase().contains(ORC_RACE_NAME)) {
            return -MAX_VALUE;
        }
        if (MOUNT_BEARS.equals(skill.getId()) && !raceName.toLowerCase().contains(DWARF_RACE_NAME)) {
            return -MAX_VALUE;
        }
        if (MOUNT_CAMELS.equals(skill.getId())) {
            final Culture culture = characterPlayer.getCulture();
            if (culture == null
                    || !culture.getName().getTranslatedText().toLowerCase().contains(DESERT_CULTURE_NAME)) {
                return -MAX_VALUE;
            }
        }
        return 0;
    }

    /** Avoid random communication skills (languages!) the character never touched. */
    private int randomnessByCulture() throws InvalidXmlElementException {
        if (COMMUNICATION_CATEGORY.equals(skill.getCategoryId()) && totalRanks() == 0) {
            return -40;
        }
        return 0;
    }

    /** Greater specialization allows more skills per category. */
    private int skillsPerCategory() {
        return characterPlayer.getCurrentLevelCategoryRanks(skill.getCategoryId()) * specializationLevel * 2;
    }

    private int randomnessByRanks() throws InvalidXmlElementException {
        int bonus = 0;

        // No more than 50 ranks.
        if (realRanks() >= 50) {
            return -MAX_VALUE;
        }

        // Point life is always good!
        if (category().getType() == CategoryType.PD && realRanks() == 0) {
            bonus += 40;
        }

        // No more than 10 ranks per communication skill.
        if (COMMUNICATION_CATEGORY.equals(skill.getCategoryId()) && realRanks() > 9) {
            return -MAX_VALUE;
        }

        // Not so many communication skills.
        if (COMMUNICATION_CATEGORY.equals(skill.getCategoryId()) && characterPlayer.getSkillTotalBonus(skill) > 60) {
            bonus -= 40;
        }

        // Check armour values; armours are also useless if the race has natural armour.
        if ("armorLight".equals(skill.getCategoryId())
                && (naturalArmorType() > 2 || characterPlayer.getSkillTotalBonus(skill) > 30)) {
            return -MAX_VALUE;
        }
        if ("armorMiddle".equals(skill.getCategoryId())
                && (naturalArmorType() > 4 || characterPlayer.getSkillTotalBonus(skill) > 100)) {
            return -MAX_VALUE;
        }
        if ("armorHeavy".equals(skill.getCategoryId())
                && (naturalArmorType() > 12 || characterPlayer.getSkillTotalBonus(skill) > 120)) {
            return -MAX_VALUE;
        }

        // Only one rank per level for very high skills.
        if (realRanks() > 10 && currentLevelRanks() > 0) {
            bonus -= 50;
        }

        return bonus;
    }

    private int randomnessBySkill() throws InvalidXmlElementException {
        int bonus = 0;

        // No rocks in random characters.
        if (skill.getId().toLowerCase().contains("rock") && realRanks() < 1) {
            return -MAX_VALUE;
        }

        // Not so many skills of the same category.
        if (!skillsWithNewRanks().contains(skill.getId())) {
            bonus -= skillsWithNewRanks().size() * (specializationLevel + 2);
        }
        return bonus;
    }

    private int randomnessByOptions() {
        if (!characterPlayer.isFirearmsAllowed()
                && (skill.getSkillGroup() == SkillGroup.FIREARM || firearmCategory())) {
            return -MAX_VALUE;
        }
        if (!characterPlayer.isChiPowersAllowed() && skill.getSkillGroup() == SkillGroup.CHI) {
            return -MAX_VALUE;
        }
        return 0;
    }

    private boolean firearmCategory() {
        return CharacterPlayer.isWeaponCategoryId(skill.getCategoryId()) && skill.getCategoryId().contains("firearm");
    }

    /** Some natural attacks are favoured by the race that has them common. */
    private int raceSkills() throws InvalidXmlElementException {
        if (skill.getId().startsWith(RACIAL_ATTACK_PREFIX) && characterPlayer.isSkillCommonByRace(skill.getId())) {
            return 20;
        }
        return 0;
    }

    /**
     * Legacy preference for the five "famous" spell lists by realm (Shield and Quickness for
     * Essence casters; Dodge, Auto-Health and Speed for Mentalism casters) keyed on the migrated
     * spell list ids. The migrated modules model spell lists as categories and have no skill whose
     * category is a spell list, so no real call can reach these branches today; the mapping is kept
     * so that the rule works unchanged should the model ever gain spell skills. The structural
     * basic/open/closed list preference is additionally applied by
     * {@link ProfessionRandomness#preferredSkillByProfession}.
     */
    private int wizardPreferredSkills() throws InvalidXmlElementException {
        if (!characterPlayer.isWizard()) {
            return 0;
        }
        final int base = famousListBase(characterPlayer.getRealmsOfMagic(), skill.getCategoryId());
        return base == 0 ? 0 : famousSpellListBonus(base, base >= 50 ? 8 : 5);
    }

    /** {@code base - currentLevelRanks * nextRankCost * factor}, the legacy famous-list formula. */
    private int famousSpellListBonus(int base, int factor) throws InvalidXmlElementException {
        final String listId = skill.getCategoryId();
        final Integer nextRankCost = characterPlayer.getSpellListDevelopmentCost(listId,
                characterPlayer.getSpellListTotalRanks(listId),
                characterPlayer.getCurrentLevel().getSpellListRanks(listId));
        if (nextRankCost == null) {
            return 0;
        }
        return Math.max(base - characterPlayer.getCurrentLevel().getSpellListRanks(listId)
                * nextRankCost * (factor - specializationLevel), 0);
    }

    /**
     * The legacy "famous list" base by realm and spell list id: the five lists a caster of that
     * realm most wants (Shield and Quickness for Essence; Dodge, Auto-Health and Speed for
     * Mentalism), each with its legacy decay factor (8 ranks per point of base 50, 5 for the
     * smaller ones), or 0 when {@code categoryId} is not one of them.
     */
    static int famousListBase(List<RealmOfMagic> realms, String categoryId) {
        if (realms.contains(RealmOfMagic.ESSENCE)) {
            if (ESSENCE_SHIELD_LIST.equals(categoryId)) {
                return 50;
            }
            if (ESSENCE_QUICKNESS_LIST.equals(categoryId)) {
                return 20;
            }
        }
        if (realms.contains(RealmOfMagic.MENTALISM)) {
            if (MENTALISM_DODGE_LIST.equals(categoryId)) {
                return 50;
            }
            if (MENTALISM_SELFHEALING_LIST.equals(categoryId)) {
                return 30;
            }
            if (MENTALISM_SPEED_LIST.equals(categoryId)) {
                return 20;
            }
        }
        return 0;
    }

    /** Skills for warriors: weapons and natural attacks. */
    private int warriorsPreferredSkills() throws InvalidXmlElementException {
        if (characterPlayer.isFighter()) {
            if (CharacterPlayer.isWeaponCategoryId(skill.getCategoryId())) {
                return 10;
            }
            if (skill.getId().startsWith(RACIAL_ATTACK_PREFIX)) {
                return 30;
            }
        }
        return 0;
    }

    /**
     * Selects if one character learns one few skills with lots of ranks or lots of skills with few
     * ranks.
     */
    private int applyCharacterSpecialization() throws InvalidXmlElementException {
        if (CharacterPlayer.isWeaponCategoryId(skill.getCategoryId())) {
            return -characterPlayer.getWeaponsLearnedInCurrentLevel() * (specializationLevel + 1) * 5;
        }
        return -skillsWithNewRanks().size() * (specializationLevel + 1) * 5;
    }

    /** More important skills with ranks. */
    private int preferredSkill() throws InvalidXmlElementException {
        return Math.min(50, realRanks() * specializationLevel * 3
                + characterPlayer.getSpecializedSkillRanks(skill) * specializationLevel * 5);
    }

    /** Select common and professional skills. */
    private int bestSkills(Integer cost) throws InvalidXmlElementException {
        if (characterPlayer.isSkillGeneralized(skill.getId())) {
            return Math.max(0, 50 - cost * 20);
        }
        if (characterPlayer.isSkillRestricted(skill)) {
            return -MAX_VALUE;
        }
        if (characterPlayer.isSkillProfessional(skill)) {
            return Math.max(0, 90 - cost * 20);
        }
        if (characterPlayer.isSkillCommon(skill)) {
            return Math.max(0, 75 - cost * 20);
        }
        return 0;
    }

    /** Avoid weird skills. */
    private int ridicolousSkill() throws InvalidXmlElementException {
        if (realRanks() == 0 && skill.getId().toLowerCase().contains(LYCHANTROPY_TAG)) {
            return -MAX_VALUE;
        }
        return 0;
    }

    /**
     * Legacy "no knowledge from other cultures" rule: a regional/cultural/fauna/flora knowledge
     * skill that claimed a foreign culture was vetoed, while the character's own culture granted its
     * own knowledge skills through its adolescence category grants. The migrated modules keep the
     * four knowledge skills generic ({@code loreRegional}, {@code loreCultural}, {@code loreOfFauna},
     * {@code loreOfFlora}): their names carry no culture, so they are every culture's own knowledge
     * and nothing is vetoed. If a culture-qualified variant ever appears (its name longer than any
     * generic one), it is vetoed for every culture whose name it does not contain, as in the legacy.
     */
    private int culturalSkill() throws InvalidXmlElementException {
        if (!knowledgeSkill()) {
            return 0;
        }
        final Culture culture = characterPlayer.getCulture();
        if (culture == null) {
            return 0;
        }
        final String skillName = skill.getName().getTranslatedText();
        if (skillName.length() > GENERIC_KNOWLEDGE_NAME_MAX_LENGTH
                && !skillName.toLowerCase().contains(culture.getName().getTranslatedText().toLowerCase())) {
            return -MAX_VALUE;
        }
        return 0;
    }

    private boolean knowledgeSkill() {
        return KNOWLEDGE_REGIONAL.equals(skill.getId()) || KNOWLEDGE_CULTURAL.equals(skill.getId())
                || KNOWLEDGE_FAUNA.equals(skill.getId()) || KNOWLEDGE_FLORA.equals(skill.getId());
    }

    private int maxRanks() throws InvalidXmlElementException {
        if (realRanks() > 10) {
            return -MAX_VALUE;
        }
        return 0;
    }

    /** Preferred categories are the ones with more ranks assigned already. */
    private int increasedCategory() throws InvalidXmlElementException {
        int prob = characterPlayer.getCurrentLevelCategoryRanks(skill.getCategoryId()) * (specializationLevel + 4);
        if (skillsWithRanks().isEmpty() && characterPlayer.getCurrentLevelCategoryRanks(skill.getCategoryId()) > 0) {
            prob += 20;
        }
        return prob;
    }

    private boolean isCommonForTheCharacter() throws InvalidXmlElementException {
        return characterPlayer.isSkillCommon(skill) || characterPlayer.isSkillCommonByRace(skill.getId());
    }

    private Category category() throws InvalidXmlElementException {
        return RulesCatalog.getInstance().getCategory(skill.getCategoryId());
    }

    private int naturalArmorType() throws InvalidXmlElementException {
        final var race = characterPlayer.getRace();
        return race == null ? 0 : race.getNaturalArmorType();
    }

    /** Skills of the same category that already have ranks (spell lists rank up as a whole). */
    private List<String> skillsWithRanks() throws InvalidXmlElementException {
        if (characterPlayer.isSpellSkill(skill.getId())) {
            return spellListWithRanks();
        }
        return characterPlayer.getCategorySkillsWithRanks(category());
    }

    /** Skills of the same category that received a rank at the current level. */
    private List<String> skillsWithNewRanks() throws InvalidXmlElementException {
        final int currentListRanks = characterPlayer.getCurrentLevel().getSpellListRanks(skill.getCategoryId());
        if (characterPlayer.isSpellSkill(skill.getId())) {
            return currentListRanks > 0 ? spellListSkills() : new ArrayList<>();
        }
        return characterPlayer.getCategorySkillsWithNewRanks(category());
    }

    private List<String> spellListWithRanks() {
        return characterPlayer.getSpellListTotalRanks(skill.getCategoryId()) > 0 ? spellListSkills() : new ArrayList<>();
    }

    private List<String> spellListSkills() {
        try {
            return characterPlayer.getSpellListSkillIds(skill.getCategoryId());
        } catch (final InvalidXmlElementException e) {
            return new ArrayList<>();
        }
    }

    private int totalRanks() throws InvalidXmlElementException {
        if (characterPlayer.isSpellSkill(skill.getId())) {
            return characterPlayer.getSpellListTotalRanks(skill.getCategoryId());
        }
        if (isSingleTrackCategory()) {
            return characterPlayer.getCategoryTotalRanks(skill.getCategoryId());
        }
        return characterPlayer.getSkillTotalRanks(skill.getId());
    }

    private int currentLevelRanks() throws InvalidXmlElementException {
        if (characterPlayer.isSpellSkill(skill.getId())) {
            return characterPlayer.getCurrentLevel().getSpellListRanks(skill.getCategoryId());
        }
        if (isSingleTrackCategory()) {
            return characterPlayer.getCurrentLevelCategoryRanks(skill.getCategoryId());
        }
        return characterPlayer.getSkillCurrentLevelRanks(skill.getId());
    }

    private int realRanks() throws InvalidXmlElementException {
        if (characterPlayer.isSpellSkill(skill.getId())) {
            return characterPlayer.getSpellListTotalRanks(skill.getCategoryId());
        }
        if (isSingleTrackCategory()) {
            return characterPlayer.getCategoryTotalRanks(skill.getCategoryId());
        }
        return characterPlayer.getSkillRealRanks(skill);
    }

    private boolean isSingleTrackCategory() throws InvalidXmlElementException {
        final CategoryType type = category().getType();
        return type == CategoryType.PD || type == CategoryType.PPD;
    }
}