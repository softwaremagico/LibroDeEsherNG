package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.ObjectMapperFactory;
import com.softwaremagico.librodeesher.character.SexType;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.decision.DecisionKey;
import com.softwaremagico.librodeesher.decision.DecisionKind;
import com.softwaremagico.librodeesher.dice.Roll;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.equipment.ObjectBonus;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.language.TranslatedText;
import com.softwaremagico.librodeesher.magic.MagicSpellList;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Imports a character saved in the legacy {@code .rlm} JSON format (version 2.0.0) into a flat
 * {@link CharacterData} snapshot.
 *
 * <p>Legacy JSON stores rule names as Spanish text; every name is resolved against the current
 * {@link RulesCatalog} indexes (matching each Spanish rule name to its id), so the
 * import survives rulebook renames. Ranks the snapshot bookkeeps are the same ones
 * {@link CharacterDataMapper} writes for a live character (culture adolescence grants materialized
 * on level 1, level-up ranks on the level they were bought, spell-list ranks split from the legacy
 * "open list" skill ranks), so {@link CharacterDataMapper#toData} of the rebuilt character is
 * byte-identical to this snapshot.</p>
 */
public final class LegacyCharacterJsonImporter {

    private static final ObjectMapper JSON = ObjectMapperFactory.getJsonObjectMapper();

    private LegacyCharacterJsonImporter() {
        // Utility class.
    }

    /**
     * Imports the legacy character JSON into a {@link CharacterData} snapshot equivalent to what
     * {@link CharacterDataMapper#toData} would write for the same character.
     *
     * <p>The legacy application evolved its save format; the last 2.0.0 release saved each
     * character's {@code background}, an {@code insertedData} merged level and a
     * {@code configuration}, while 2.1.1 renamed the background section {@code historial} and
     * moved the option flags onto the top level. Both schemas are imported here, detected by the
     * presence of the 2.1.1 {@code historial} field.</p>
     *
     * @param legacyJson the raw content of a legacy {@code .rlm} JSON export (2.0.0 or 2.1.1).
     * @return a ready to save/serialize {@link CharacterData}.
     */
    public static CharacterData importLegacyJson(String legacyJson)
            throws IOException, InvalidXmlElementException {
        final JsonNode root = JSON.readTree(legacyJson);
        if (root.has("historial")) {
            return importLegacyV211Json(root);
        }
        return importLegacyV2Json(root);
    }

    /** The 2.0.0 schema: characteristics live under {@code insertedData}. */
    private static CharacterData importLegacyV2Json(JsonNode root) throws IOException, InvalidXmlElementException {
        final RulesCatalog rulesCatalog = RulesCatalog.getInstance();

        final Map<String, String> categoryIds = resolveNames(rulesCatalog.getCategories());
        final Map<String, String> skillIds = resolveNames(rulesCatalog.getSkills());
        final Map<String, String> spellListIds = resolveNames(rulesCatalog.getSpellLists());
        final Map<String, String> raceIds = resolveNames(rulesCatalog.getRaces());
        final Map<String, String> cultureIds = resolveNames(rulesCatalog.getCultures());
        final Map<String, String> professionIds = resolveNames(rulesCatalog.getProfessions());
        final Map<String, String> perkIds = resolveNames(rulesCatalog.getPerks());
        final Map<String, String> trainingIds = resolveNames(rulesCatalog.getTrainings());

        final CharacterData data = new CharacterData();
        data.setName(required(root, "name").asText());
        data.setSex(SexType.valueOf(required(root, "sex").asText()));
        data.setRaceId(requireResolved(required(root, "raceName").asText(), raceIds));
        data.setCultureId(requireResolved(required(root, "cultureName").asText(), cultureIds));
        data.setProfessionId(requireResolved(required(root, "professionName").asText(), professionIds));

        setCharacteristicsAndAge(data, root);

        final List<LevelData> levels = new ArrayList<>();
        // A 2.x save may fold the early levels into a single merged "inserted" record: the
        // character then keeps that development on level insertedLevels and one real level-up per
        // remaining entry, with the levels in between left empty as the legacy application did.
        final int inserted = insertedLevels(root);
        levels.add(inserted == 0
                ? buildLevelOne(rulesCatalog, root, categoryIds, skillIds, spellListIds, data.getCultureId())
                : buildCultureLevelOne(rulesCatalog, root, categoryIds, skillIds, data.getCultureId()));
        for (int i = 1; i < inserted - 1; i++) {
            levels.add(emptyLevel(data.getCurrentAge()));
        }
        if (inserted > 0) {
            levels.add(buildInsertedLevel(root, categoryIds, skillIds, spellListIds, data.getCurrentAge()));
        }
        for (final JsonNode levelUp : required(root, "levelUps")) {
            levels.add(fromLevelUp(levelUp, categoryIds, skillIds, spellListIds, trainingIds,
                    data.getCurrentAge()));
        }
        data.setLevels(levels);

        final BackgroundData background = new BackgroundData();
        background.setCategoryIds(resolveRequiredNames(root.path("background").path("categories"), categoryIds));
        background.setSkillIds(resolveSkillNames(root.path("background").path("skills"), skillIds));
        background.setLanguageRanks(languageRanks(root.path("cultureDecisions").path("languageRanks")));
        data.setBackground(background);

        final Map<String, DecisionData> decisions = new TreeMap<>();
        data.setDecisions(decisions);
        buildWeaponCostDecisions(root.path("professionDecisions").path("weaponsCost"), categoryIds, decisions);
        buildMagicRealmDecisions(root.path("realmOfMagic").path("magicRealmsAvailable"), data.getProfessionId(), decisions);
        buildPerkChoiceDecisions(root.path("perkDecisions"), perkIds, categoryIds, skillIds, decisions);
        data.setSelectedPerks(buildPerks(root.path("selectedPerks"), perkIds));
        data.setMagicItems(buildMagicItems(root.path("magicItems"), skillIds));

        applyConfiguration(root.path("configuration"), data);
        data.setRecommendedFavouriteSkillsIncluded(root.path("recommendedFavouriteSkillsIncluded").asBoolean(false));
        return data;
    }

    /** The 2.1.1 schema: characteristics and every development decision live on the top level. */
    private static CharacterData importLegacyV211Json(JsonNode root) throws InvalidXmlElementException {
        final RulesCatalog rulesCatalog = RulesCatalog.getInstance();

        final Map<String, String> categoryIds = resolveNames(rulesCatalog.getCategories());
        final Map<String, String> skillIds = resolveNames(rulesCatalog.getSkills());
        final Set<RealmOfMagic> realms = new HashSet<>();
        for (final JsonNode realm : root.path("realmOfMagic").path("magicRealmsAvailable")) {
            final RealmOfMagic resolved = RealmOfMagic.fromTag(realm.asText());
            if (resolved != null) {
                realms.add(resolved);
            }
        }
        final Map<String, String> spellListIds = resolveSpellListsPreferringRealm(rulesCatalog, realms);
        final Map<String, String> raceIds = resolveNames(rulesCatalog.getRaces());
        final Map<String, String> cultureIds = resolveNames(rulesCatalog.getCultures());
        final Map<String, String> professionIds = resolveNames(rulesCatalog.getProfessions());
        final Map<String, String> perkIds = resolveNames(rulesCatalog.getPerks());
        final Map<String, String> trainingIds = resolveNames(rulesCatalog.getTrainings());
        final int currentAge = 10;

        final CharacterData data = new CharacterData();
        data.setName(required(root, "name").asText());
        data.setSex(SexType.valueOf(required(root, "sex").asText()));
        data.setRaceId(requireResolved(required(root, "raceName").asText(), raceIds));
        data.setCultureId(requireResolved(required(root, "cultureName").asText(), cultureIds));
        data.setProfessionId(requireResolved(required(root, "professionName").asText(), professionIds));

        data.setCharacteristicTemporalValues(characteristicTemporalValues(root));
        data.setCharacteristicPotentialValues(characteristicPotentialValues(root));
        data.setCharacteristicsConfirmed(required(root, "characteristicsConfirmed").asBoolean(false));
        data.setAppearance(root.path("appearance").path("dicesResult").asInt(0));
        data.setCurrentAge(currentAge);
        data.setFinalAge(currentAge);

        final List<LevelData> levels = new ArrayList<>();
        final JsonNode levelUps = required(root, "levelUps");
        // As in the 2.0.0 schema, the legacy application may fold the early levels into a single
        // merged "inserted" record, keeping that development on level insertedLevels and one real
        // level-up per remaining entry, with the levels in between left empty.
        final int inserted = insertedLevels(root);
        if (inserted == 0) {
            levels.add(buildLevelOne(root, rulesCatalog, categoryIds, skillIds, spellListIds, trainingIds,
                    data.getCultureId(), currentAge));
        } else {
            levels.add(buildCultureLevelOne(rulesCatalog, root, categoryIds, skillIds, spellListIds,
                    data.getCultureId(), currentAge));
            for (int i = 1; i < inserted - 1; i++) {
                levels.add(emptyLevel(currentAge));
            }
            levels.add(buildInsertedLevel(root, categoryIds, skillIds, spellListIds, currentAge));
        }
        for (int i = inserted == 0 ? 1 : 0; i < levelUps.size(); i++) {
            levels.add(fromLevelUp(levelUps.get(i), categoryIds, skillIds, spellListIds, trainingIds, currentAge));
        }
        data.setLevels(levels);

        final BackgroundData background = new BackgroundData();
        final JsonNode historial = root.path("historial");
        background.setCategoryIds(resolveRequiredNames(historial.path("categories"), categoryIds));
        background.setSkillIds(resolveSkillNames(historial.path("skills"), skillIds));
        background.setLanguageRanks(languageRanks(root.path("cultureDecisions").path("languageRanks")));
        data.setBackground(background);
        data.setHobbySkillRanks(resolveRankedNames(root.path("cultureDecisions").path("hobbyRanks"), skillIds));

        final Map<String, DecisionData> decisions = new TreeMap<>();
        data.setDecisions(decisions);
        buildWeaponCostDecisions(root.path("professionDecisions").path("weaponsCost"), categoryIds, decisions);
        buildMagicRealmDecisions(root.path("realmOfMagic").path("magicRealmsAvailable"), data.getProfessionId(), decisions);
        buildPerkChoiceDecisions(root.path("perkDecisions"), perkIds, categoryIds, skillIds, decisions);
        buildSkillEnableDecisions(root.path("enabledSkill"), skillIds, decisions);
        data.setSelectedPerks(buildPerks(root.path("selectedPerks"), perkIds));
        data.setMagicItems(buildMagicItems(root.path("magicItems"), skillIds));

        data.setFirearmsAllowed(required(root, "firearmsAllowed").asBoolean(false));
        data.setChiPowersAllowed(required(root, "chiPowersAllowed").asBoolean(false));
        data.setOtherRealmTrainingSpellsAllowed(required(root, "otherRealmtrainingSpellsAllowed").asBoolean(false));
        data.setDarkSpellsAsBasicListsAllowed(required(root, "darkSpellsAsBasicListsAllowed").asBoolean(false));
        data.setRecommendedFavouriteSkillsIncluded(root.path("recommendedFavouriteSkillsIncluded").asBoolean(false));
        data.setHandWritingFont(root.path("handWritingFont").asBoolean(false));
        data.setSortPdfSkills(root.path("sortPdfSkills").asBoolean(false));
        data.setMagicAllowed(true);
        return data;
    }

    private static void setCharacteristicsAndAge(CharacterData data, JsonNode root) {
        data.setCharacteristicTemporalValues(characteristicTemporalValues(root));
        data.setCharacteristicPotentialValues(characteristicPotentialValues(root));
        data.setCharacteristicsConfirmed(root.path("characteristicsConfirmed").asBoolean(false));
        data.setAppearance(root.path("appearance").path("dicesResult").asInt(0));
        final int currentAge = root.path("currentAge").asInt(10);
        data.setCurrentAge(currentAge);
        data.setFinalAge(root.path("finalAge").asInt(currentAge));
    }

    /** The character's final temporal values: the base values, then the folded levels' development,
     *  then every level-up's update in order. */
    private static Map<String, Integer> characteristicTemporalValues(JsonNode root) {
        final Map<String, Integer> values =
                characteristicMap(root.path("characteristicsInitialTemporalValues"));
        values.putAll(characteristicMap(root.path("insertedData").path("characteristicsTemporalValuesModification")));
        for (final JsonNode levelUp : root.path("levelUps")) {
            for (final JsonNode update : recordedCharacteristicUpdates(levelUp)) {
                values.put(CharacteristicAbbreviation
                        .fromTag(update.path("characteristicAbbreviature").asText()).name(),
                        update.path("characteristicTemporalValue").asInt(0));
            }
        }
        return values;
    }

    /** The character's final potential values, following the same base/inserted/level-up order. */
    private static Map<String, Integer> characteristicPotentialValues(JsonNode root) {
        final Map<String, Integer> values = characteristicMap(root.path("characteristicsPotentialValues"));
        values.putAll(characteristicMap(root.path("insertedData").path("characteristicsPotentialValuesModification")));
        for (final JsonNode levelUp : root.path("levelUps")) {
            for (final JsonNode update : recordedCharacteristicUpdates(levelUp)) {
                values.put(CharacteristicAbbreviation
                        .fromTag(update.path("characteristicAbbreviature").asText()).name(),
                        update.path("characteristicPotentialValue").asInt(0));
            }
        }
        return values;
    }

    /**
     * The level-up's characteristic updates that actually recorded a development: the legacy
     * application stored a placeholder (temporal 31, potential 0) on the level right after a folded
     * one, as no roll was ever made there.
     */
    private static List<JsonNode> recordedCharacteristicUpdates(JsonNode levelUp) {
        final List<JsonNode> updates = new ArrayList<>();
        for (final JsonNode update : levelUp.path("characteristicsUpdates")) {
            if (update.path("characteristicPotentialValue").asInt(0) > 0
                    && CharacteristicAbbreviation.fromTag(update.path("characteristicAbbreviature").asText())
                    != CharacteristicAbbreviation.NONE) {
                updates.add(update);
            }
        }
        return updates;
    }

    /** How many levels the legacy application folded into the merged {@code insertedData} record. */
    private static int insertedLevels(JsonNode root) {
        return root.path("insertedData").path("insertedLevels").asInt(0);
    }

    /**
     * The final characteristic values, with the {@link CharacteristicAbbreviation#APPEARANCE}
     * placeholder added (a real characteristic maintained by {@link CharacterDataMapper}, but that
     * the legacy snapshot did not track) so the serialized map matches a snapshot of the same
     * character.
     */
    private static Map<String, Integer> characteristicMap(JsonNode node) {
        final Map<String, Integer> values = new TreeMap<>();
        node.fields().forEachRemaining(entry -> {
            final CharacteristicAbbreviation abbreviation = CharacteristicAbbreviation.fromTag(entry.getKey());
            if (abbreviation != CharacteristicAbbreviation.NONE) {
                values.put(abbreviation.name(), entry.getValue().asInt());
            }
        });
        values.put(CharacteristicAbbreviation.APPEARANCE.name(), 0);
        return values;
    }

    /** Indexes every spell list by its Spanish name, preferring (for shared names such as the
     *  Mentalism/Essence variants of a martial arts list) the variant of one of {@code realms}, so a
     *  Mentalism caster resolves "Evasiones" to the Mentalism list, not the Essence one. */
    private static NameIndex resolveSpellListsPreferringRealm(RulesCatalog rulesCatalog,
                                                              Set<RealmOfMagic> realms)
            throws InvalidXmlElementException {
        final NameIndex ids = resolveNames(rulesCatalog.getSpellLists());
        for (final MagicSpellList spellList : rulesCatalog.getSpellLists()) {
            if (spellList.getName() == null || spellList.getName().getSpanish() == null) {
                continue;
            }
            if (spellList.getRealm() != null && realms.contains(spellList.getRealm())) {
                // Several realms share list names ("Evasiones" is mentalism's and psionics'), so the
                // list of one of the character's own realms takes over the name.
                ids.prefer(spellList.getName().getSpanish(), spellList.getId());
            }
        }
        return ids;
    }

    /** The legacy skills that unlocked one specific enabled skill become one character-wide
     *  {@link DecisionKind#SKILL_ENABLE} decision per enabling skill, recorded at level 1, so the
     *  rebuilt character's {@code isSkillEnabled()} resolves as the original did. */
    private static void buildSkillEnableDecisions(JsonNode enabledSkill, Map<String, String> skillIds,
                                                  Map<String, DecisionData> decisions) {
        enabledSkill.fields().forEachRemaining(entry -> {
            final String enablingSkillId = skillIds.get(entry.getKey());
            final String enabledSkillId = skillIds.get(entry.getValue().asText());
            if (enablingSkillId != null && enabledSkillId != null) {
                final DecisionKey key = DecisionKey.characterWide(DecisionKind.SKILL_ENABLE, enablingSkillId, 0);
                decisions.put(key.toString(), new DecisionData(new DecisionKeyData(key), 1,
                        List.of(enabledSkillId), List.of(enabledSkillId)));
            }
        });
    }

    /** The background's trained categories, resolved by name (failing loudly when unknown). */
    private static List<String> resolveRequiredNames(JsonNode node, Map<String, String> ids) {
        final List<String> names = new ArrayList<>();
        for (final JsonNode child : node) {
            names.add(requireResolved(child.asText(), ids));
        }
        return names;
    }

    /** The legacy culture language ranks, keyed by their original (unresolved) names: the NG
     *  catalogs have no language elements of their own, and the keys round-trip verbatim. */
    private static Map<String, Integer> languageRanks(JsonNode node) {
        final Map<String, Integer> ranks = new TreeMap<>();
        for (final Map.Entry<String, JsonNode> entry : node.properties()) {
            final int rank = entry.getValue().asInt();
            if (rank > 0) {
                ranks.put(entry.getKey(), rank);
            }
        }
        return ranks;
    }

    /** Level 1: the culture's adolescence grants materialized as fixed ranks, plus the spell-list
     *  updates recorded by the first legacy level-up as pure metadata. */
    private static LevelData buildLevelOne(RulesCatalog rulesCatalog, JsonNode root, Map<String, String> categoryIds,
                                           Map<String, String> skillIds, Map<String, String> spellListIds,
                                           String cultureId) throws InvalidXmlElementException {
        final LevelData level = emptyLevel(root.path("currentAge").asInt(10));
        materializeCulture(rulesCatalog.getCulture(cultureId), categoryIds, skillIds, level);
        level.setSpellsUpdated(resolveListNames(required(root, "levelUps").get(0).path("spellsUpdated"), spellListIds));
        return level;
    }

    /** 2.0.0 level 1 of a save whose early levels were folded into an inserted record: the culture's
     *  adolescence grants materialized as fixed ranks, the first real level-up belonging to level
     *  {@code insertedLevels + 1}. */
    private static LevelData buildCultureLevelOne(RulesCatalog rulesCatalog, JsonNode root,
                                                  Map<String, String> categoryIds, Map<String, String> skillIds,
                                                  String cultureId) throws InvalidXmlElementException {
        final LevelData level = emptyLevel(root.path("currentAge").asInt(10));
        materializeCulture(rulesCatalog.getCulture(cultureId), categoryIds, skillIds, level);
        return level;
    }

    /** 2.1.1 level 1: the culture's adolescence grants materialized as fixed ranks, the weapon skills
     *  chosen in {@code cultureDecisions.skillRanks} materialized as skill ranks, and the first
     *  legacy level-up's own development (categories, skills, spell lists, trainings, experience and
     *  skill updates) recorded on top. */
    private static LevelData buildLevelOne(JsonNode root, RulesCatalog rulesCatalog, Map<String, String> categoryIds,
                                           Map<String, String> skillIds, Map<String, String> spellListIds,
                                           Map<String, String> trainingIds, String cultureId, int age)
            throws InvalidXmlElementException {
        final LevelData level = emptyLevel(age);
        materializeCulture(rulesCatalog.getCulture(cultureId), categoryIds, skillIds, level);
        mergeRanks(level.getSkillRanks(),
                resolveRankedNames(root.path("cultureDecisions").path("skillRanks"), skillIds));
        final LevelData firstLevelUp = fromLevelUp(required(root, "levelUps").get(0), categoryIds, skillIds,
                spellListIds, trainingIds, age);
        mergeRanks(level.getCategoryRanks(), firstLevelUp.getCategoryRanks());
        mergeRanks(level.getSkillRanks(), firstLevelUp.getSkillRanks());
        mergeRanks(level.getSpellListRanks(), firstLevelUp.getSpellListRanks());
        level.setSpellsUpdated(firstLevelUp.getSpellsUpdated());
        level.setTrainings(firstLevelUp.getTrainings());
        level.setFavouriteSkills(firstLevelUp.getFavouriteSkills());
        level.setCharacteristicUpdates(firstLevelUp.getCharacteristicUpdates());
        return level;
    }

    /** 2.1.1 level 1 of a save whose early levels were folded into an inserted record: the culture's
     *  adolescence grants and the weapon skills chosen in {@code cultureDecisions.skillRanks}, with
     *  the first real level-up belonging to level {@code insertedLevels + 1}. */
    private static LevelData buildCultureLevelOne(RulesCatalog rulesCatalog, JsonNode root,
                                                  Map<String, String> categoryIds, Map<String, String> skillIds,
                                                  Map<String, String> spellListIds, String cultureId, int age)
            throws InvalidXmlElementException {
        final LevelData level = emptyLevel(age);
        materializeCulture(rulesCatalog.getCulture(cultureId), categoryIds, skillIds, level);
        mergeRanks(level.getSkillRanks(),
                resolveRankedNames(root.path("cultureDecisions").path("skillRanks"), skillIds));
        return level;
    }

    /** The culture's adolescence grants: the fixed options (real ids only) get their ranks directly. */
    private static void materializeCulture(Culture culture, Map<String, String> categoryIds,
                                           Map<String, String> skillIds, LevelData level) {
        final Map<String, Integer> categoryRanks = new TreeMap<>();
        final Map<String, Integer> skillRanks = new TreeMap<>();
        for (final TrainingCategoryGrant grant : culture.getAdolescenceRanks()) {
            final int granted = grant.getRanksGranted();
            if (granted > 0) {
                for (final String option : grant.getCategoryOptions()) {
                    if (categoryIds.containsValue(option)) {
                        addRank(categoryRanks, option, granted);
                    }
                }
            }
            for (final TrainingSkillGrant skillGrant : grant.getSkills()) {
                final int ranksToDistribute = skillGrant.getRanksToDistribute();
                if (ranksToDistribute > 0) {
                    for (final String option : skillGrant.getSkillOptions()) {
                        if (skillIds.containsValue(option)) {
                            addRank(skillRanks, option, ranksToDistribute);
                        }
                    }
                }
            }
        }
        level.setCategoryRanks(categoryRanks);
        level.setSkillRanks(skillRanks);
    }

    /** The ranks accumulated by the legacy "inserted" development (old levels merged into the
     *  snapshot), whose categories/skills/lists are tracked in {@code insertedData}. */
    private static LevelData buildInsertedLevel(JsonNode root, Map<String, String> categoryIds,
                                                Map<String, String> skillIds, Map<String, String> spellListIds,
                                                int age) {
        final LevelData level = emptyLevel(age);
        final JsonNode inserted = required(root, "insertedData");
        level.setCategoryRanks(resolveRankedNames(inserted.path("categoriesRanksModification"), categoryIds));
        final Map<String, Integer> skillRanks = new TreeMap<>();
        final Map<String, Integer> spellListRanks = new TreeMap<>();
        splitSkillsAndLists(inserted.path("skillsRanksModification"), skillIds, spellListIds, skillRanks, spellListRanks);
        level.setSkillRanks(skillRanks);
        level.setSpellListRanks(spellListRanks);
        return level;
    }

    /** One legacy level-up (entered in the app at a specific level) to its {@link LevelData}. */
    private static LevelData fromLevelUp(JsonNode levelUp, Map<String, String> categoryIds,
                                         Map<String, String> skillIds, Map<String, String> spellListIds,
                                         Map<String, String> trainingIds, int age) {
        final LevelData level = emptyLevel(age);
        level.setCategoryRanks(resolveRankedNames(levelUp.path("categoriesRanks"), categoryIds));
        final Map<String, Integer> skillRanks = new TreeMap<>();
        final Map<String, Integer> spellListRanks = new TreeMap<>();
        splitSkillsAndLists(levelUp.path("skillsRanks"), skillIds, spellListIds, skillRanks, spellListRanks);
        level.setSkillRanks(skillRanks);
        level.setSpellListRanks(spellListRanks);
        level.setSpellsUpdated(resolveListNames(levelUp.path("spellsUpdated"), spellListIds));

        // The ranks granted by the trainings taken this level, baked into the level's skill ranks
        // (their home in the NG bookkeeping), and the trainings themselves recorded as metadata.
        final List<String> trainings = new ArrayList<>();
        for (final JsonNode training : levelUp.path("trainings")) {
            final String id = trainingIds.get(training.asText());
            if (id != null) {
                trainings.add(id);
            }
        }
        level.setTrainings(trainings);
        final Map<String, Integer> trainingRanks = new TreeMap<>();
        levelUp.path("trainingDecisions").fields().forEachRemaining(training -> {
            training.getValue().path("skillsSelected").fields().forEachRemaining(selection -> {
                selection.getValue().path("skillsRanks").properties().forEach(rank -> {
                    final int ranks = rank.getValue().asInt();
                    if (ranks == 0) {
                        return;
                    }
                    String id = skillIds.get(rank.getKey());
                    if (id != null) {
                        trainingRanks.merge(id, ranks, Integer::sum);
                        return;
                    }
                    id = spellListIds.get(rank.getKey());
                    if (id != null) {
                        trainingRanks.merge(id, ranks, Integer::sum);
                    }
                });
            });
        });
        trainingRanks.forEach((id, ranks) -> {
            if (spellListIds.containsValue(id)) {
                spellListRanks.merge(id, ranks, Integer::sum);
            } else {
                skillRanks.merge(id, ranks, Integer::sum);
            }
        });

        final List<String> favourites = new ArrayList<>();
        final List<String> favouriteSpellLists = new ArrayList<>();
        for (final JsonNode favourite : levelUp.path("favouriteSkills")) {
            // Favourites may name either a skill or a spell list (e.g. "Armadura del Caos"): the
            // legacy application accepted both, and the sheet prints both in its most-used block.
            final String name = normalize(favourite.asText());
            final String id = skillIds.get(name);
            if (id != null) {
                favourites.add(id);
            } else {
                final String spellListId = spellListIds.get(name);
                if (spellListId != null) {
                    favouriteSpellLists.add(spellListId);
                }
            }
        }
        favourites.sort(String::compareTo);
        favouriteSpellLists.sort(String::compareTo);
        level.setFavouriteSkills(favourites);
        level.setFavouriteSpellLists(favouriteSpellLists);

        final List<CharacteristicRoll> updates = new ArrayList<>();
        for (final JsonNode update : levelUp.path("characteristicsUpdates")) {
            final CharacteristicAbbreviation abbreviation =
                    CharacteristicAbbreviation.fromTag(update.path("characteristicAbbreviature").asText());
            if (abbreviation != CharacteristicAbbreviation.NONE) {
                updates.add(new CharacteristicRoll(abbreviation,
                        update.path("characteristicTemporalValue").asInt(0),
                        update.path("characteristicPotentialValue").asInt(0),
                        Roll.of(update.path("roll").path("firstDice").asInt(0),
                                update.path("roll").path("secondDice").asInt(0))));
            }
        }
        level.setCharacteristicUpdates(updates);
        return level;
    }

    /** The legacy weapon cost tiers become one character-wide {@link DecisionKind#WEAPON_COST_TIER}
     *  decision per weapon category, recorded at level 1 (map keys are the decision keys, sorted). */
    private static void buildWeaponCostDecisions(JsonNode weaponsCost, Map<String, String> categoryIds,
                                                 Map<String, DecisionData> decisions) {
        weaponsCost.fields().forEachRemaining(entry -> {
            final String categoryCostId = entry.getValue().path("categoryCostId").asText();
            if (!categoryCostId.startsWith("WEAPON")) {
                return;
            }
            final String weaponCategoryId = requireResolved(entry.getKey(), categoryIds);
            final int tierIndex = Integer.parseInt(categoryCostId.substring("WEAPON".length())) - 1;
            final DecisionKey key = DecisionKey.characterWide(DecisionKind.WEAPON_COST_TIER, "", tierIndex);
            decisions.put(key.toString(),
                    new DecisionData(new DecisionKeyData(key), 1, List.of(weaponCategoryId), List.of(weaponCategoryId)));
        });
    }

    /** The legacy realm of magic (a single, fixed realm in a 2.0.0 save) becomes one character-wide
     *  {@link DecisionKind#PROFESSION_REALM} decision per selected realm, recorded at level 1, so the
     *  rebuilt character's {@code getRealmsOfMagic()} (and so its power points and spell list access)
     *  resolves as the original did. */
    private static void buildMagicRealmDecisions(JsonNode magicRealmsAvailable, String professionId,
                                                 Map<String, DecisionData> decisions) {
        int index = 0;
        for (final JsonNode realm : magicRealmsAvailable) {
            final RealmOfMagic resolved = RealmOfMagic.fromTag(realm.asText());
            final DecisionKey key =
                    DecisionKey.characterWide(DecisionKind.PROFESSION_REALM, professionId, index++);
            decisions.put(key.toString(), new DecisionData(new DecisionKeyData(key), 1,
                    List.of(resolved.name()), List.of(resolved.name())));
        }
    }

    private static List<SelectedPerk> buildPerks(JsonNode node, Map<String, String> perkIds) {
        final List<SelectedPerk> perks = new ArrayList<>();
        for (final JsonNode perk : node) {
            final String perkId = perkIds.get(perk.path("name").asText());
            if (perkId != null) {
                perks.add(new SelectedPerk(perkId));
            }
        }
        return perks;
    }

    /**
     * The legacy {@code perkDecisions} of every selected perk (the choices for its
     * {@link DecisionKind#PERK_CHOICE} grants) become one character-wide decision per resolved
     * grant, in the order the legacy recorded them (category bonuses, then skill bonuses, then
     * common skills), recorded at level 1, so the rebuilt character's resolved
     * {@code PerkChoiceGrant}s (see {@code CharacterPlayer#getResolvedPerkChoiceGrants}) match the
     * original ones (e.g. "Cimitarra" chosen for "Experto en Armas Especializado (Menor)", which
     * makes it a common weapon).
     */
    private static void buildPerkChoiceDecisions(JsonNode perkDecisions, Map<String, String> perkIds,
                                                 Map<String, String> categoryIds, Map<String, String> skillIds,
                                                 Map<String, DecisionData> decisions) {
        perkDecisions.fields().forEachRemaining(entry -> {
            final String perkId = perkIds.get(entry.getKey());
            if (perkId == null) {
                return;
            }
            final JsonNode decision = entry.getValue();
            int grantIndex = 0;
            for (final JsonNode chosen : decision.path("categoriesBonusChosen")) {
                final String categoryId = categoryIds.get(chosen.asText());
                if (categoryId != null) {
                    putPerkChoiceDecision(decisions, perkId, grantIndex, categoryId);
                }
                grantIndex++;
            }
            for (final JsonNode chosen : decision.path("skillsBonusChosen")) {
                final String skillId = skillIds.get(chosen.asText());
                if (skillId != null) {
                    putPerkChoiceDecision(decisions, perkId, grantIndex, skillId);
                }
                grantIndex++;
            }
            for (final JsonNode chosen : decision.path("commonSkillsChosen")) {
                final String skillId = skillIds.get(chosen.asText());
                if (skillId != null) {
                    putPerkChoiceDecision(decisions, perkId, grantIndex, skillId);
                }
                grantIndex++;
            }
        });
    }

    private static void putPerkChoiceDecision(Map<String, DecisionData> decisions, String perkId, int grantIndex,
                                              String targetId) {
        final DecisionKey key = DecisionKey.characterWide(DecisionKind.PERK_CHOICE, perkId, grantIndex);
        decisions.put(key.toString(), new DecisionData(new DecisionKeyData(key), 1,
                List.of(targetId), List.of(targetId)));
    }

    private static List<MagicObject> buildMagicItems(JsonNode node, Map<String, String> skillIds) {
        final List<MagicObject> items = new ArrayList<>();
        for (final JsonNode item : node) {
            final List<ObjectBonus> bonuses = new ArrayList<>();
            for (final JsonNode bonus : item.path("bonus")) {
                final BonusType type = BonusType.valueOf(bonus.path("type").asText());
                if (type == BonusType.DEFENSIVE_BONUS) {
                    bonuses.add(new ObjectBonus(type, null, bonus.path("bonus").asInt()));
                } else {
                    bonuses.add(new ObjectBonus(type, skillIds.get(bonus.path("bonusName").asText()),
                            bonus.path("bonus").asInt()));
                }
            }
            items.add(new MagicObject(new TranslatedText(item.path("name").asText(), null),
                    new TranslatedText(item.path("description").asText(), null), bonuses));
        }
        return items;
    }

    private static void applyConfiguration(JsonNode node, CharacterData data) {
        data.setFirearmsAllowed(node.path("fireArmsActivated").asBoolean(false));
        data.setChiPowersAllowed(node.path("chiPowersAllowed").asBoolean(false));
        data.setOtherRealmTrainingSpellsAllowed(node.path("otherRealmsTrainingSpells").asBoolean(false));
        data.setMagicAllowed(node.path("magicAllowed").asBoolean(true));
        data.setDarkSpellsAsBasicListsAllowed(node.path("darkSpellsAsBasic").asBoolean(false));
        data.setHandWritingFont(node.path("handWrittingFont").asBoolean(false));
        data.setSortPdfSkills(node.path("sortPdfSkills").asBoolean(false));
    }

    private static LevelData emptyLevel(int age) {
        final LevelData level = new LevelData();
        level.setAge(age);
        level.setCategoryRanks(new TreeMap<>());
        level.setSkillRanks(new TreeMap<>());
        level.setSpellListRanks(new TreeMap<>());
        level.setGeneralizedSkills(new ArrayList<>());
        level.setSpellsUpdated(new ArrayList<>());
        level.setTrainings(new ArrayList<>());
        level.setSkillSpecializations(new ArrayList<>());
        level.setFavouriteSkills(new ArrayList<>());
        level.setCharacteristicUpdates(new ArrayList<>());
        level.setAgeModifications(new ArrayList<>());
        return level;
    }

    private static void splitSkillsAndLists(JsonNode node, Map<String, String> skillIds,
                                            Map<String, String> spellListIds,
                                            Map<String, Integer> skillRanks, Map<String, Integer> spellListRanks) {
        // Two passes, because the same element can be written twice in one node under different
        // spellings and the catalog spelling has to be the one that counts.
        final Set<String> canonicalNames = new HashSet<>();
        for (final Map.Entry<String, JsonNode> entry : nonZeroEntries(node)) {
            final int ranks = entry.getValue().asInt();
            final String skillId = exactId(entry.getKey(), skillIds);
            if (skillId != null) {
                addRank(skillRanks, skillId, ranks);
                canonicalNames.add(entry.getKey());
                continue;
            }
            final String listId = exactId(entry.getKey(), spellListIds);
            if (listId != null) {
                addRank(spellListRanks, listId, ranks);
                canonicalNames.add(entry.getKey());
            }
        }
        for (final Map.Entry<String, JsonNode> entry : nonZeroEntries(node)) {
            if (canonicalNames.contains(entry.getKey())) {
                continue;
            }
            final int ranks = entry.getValue().asInt();
            final String skillId = skillIds.get(entry.getKey());
            if (skillId != null) {
                if (!skillRanks.containsKey(skillId)) {
                    skillRanks.put(skillId, ranks);
                }
                continue;
            }
            final String listId = spellListIds.get(entry.getKey());
            if (listId != null && !spellListRanks.containsKey(listId)) {
                spellListRanks.put(listId, ranks);
            }
        }
    }

    /** The entries of a legacy rank node that give something to some element. */
    private static List<Map.Entry<String, JsonNode>> nonZeroEntries(JsonNode node) {
        final List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
        for (final Map.Entry<String, JsonNode> entry : node.properties()) {
            if (entry.getValue().asInt() != 0) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /** The id indexed under exactly this name, or null when the name is only found when the accents
     *  and the capitals are ignored. */
    private static String exactId(String name, Map<String, String> ids) {
        return ids instanceof NameIndex index ? index.exactId(name) : ids.get(name);
    }

    private static Map<String, Integer> resolveRankedNames(JsonNode node, Map<String, String> ids) {
        final Map<String, Integer> ranks = new TreeMap<>();
        // Two passes, because the same element can be written twice in one node under different
        // spellings and the catalog spelling has to be the one that counts.
        final Set<String> canonicalNames = new HashSet<>();
        for (final Map.Entry<String, JsonNode> entry : nonZeroEntries(node)) {
            final String id = exactId(entry.getKey(), ids);
            if (id != null) {
                addRank(ranks, id, entry.getValue().asInt());
                canonicalNames.add(entry.getKey());
            }
        }
        for (final Map.Entry<String, JsonNode> entry : nonZeroEntries(node)) {
            if (canonicalNames.contains(entry.getKey())) {
                continue;
            }
            final String id = ids.get(entry.getKey());
            if (id != null && !ranks.containsKey(id)) {
                ranks.put(id, entry.getValue().asInt());
            }
        }
        return ranks;
    }

    private static List<String> resolveListNames(JsonNode node, Map<String, String> spellListIds) {
        final List<String> names = new ArrayList<>();
        for (final JsonNode child : node) {
            final String id = spellListIds.get(child.asText());
            if (id != null) {
                names.add(id);
            }
        }
        return names;
    }

    private static List<String> resolveSkillNames(JsonNode node, Map<String, String> skillIds) {
        final List<String> skills = new ArrayList<>();
        for (final JsonNode skill : node) {
            final String id = skillIds.get(skill.asText());
            if (id != null) {
                skills.add(id);
            }
        }
        return skills;
    }

    /** Indexes every rule element by its Spanish name (first occurrence wins), so legacy names can
     *  be resolved to ids. */
    private static NameIndex resolveNames(List<? extends Element> elements) {
        final NameIndex ids = new NameIndex();
        for (final Element element : elements) {
            if (element.getName() != null && element.getName().getSpanish() != null) {
                ids.add(element.getName().getSpanish(), element.getId());
            }
        }
        return ids;
    }

    /**
     * Map of element ids by Spanish name that also accepts the spellings other tools produced: an
     * exact name always wins, and only when there is none the name is matched ignoring case and
     * accents. The fallback is needed because saves are not always spelled as the rulebook is (Gson
     * serialized the realm enum without its accent, "Canalizacion", and some saves carry lowercase
     * names), and it must never override an exact match: two different elements may differ only in
     * case or accents, so the fallback is kept apart from the exact keys.
     */
    private static final class NameIndex extends LinkedHashMap<String, String> {

        private final Map<String, String> byNormalizedName = new HashMap<>();

        private void add(final String name, final String id) {
            putIfAbsent(name, id);
            byNormalizedName.putIfAbsent(normalize(name), id);
        }

        /** The id indexed under exactly this name, or null when it is only found with
         *  {@link #normalize}. */
        private String exactId(final String name) {
            return super.get(name);
        }

        /** Adds a name that must win over whatever was indexed before it. */
        private void prefer(final String name, final String id) {
            put(name, id);
            byNormalizedName.put(normalize(name), id);
        }

        @Override
        public String get(final Object name) {
            final String id = super.get(name);
            return id != null ? id : byNormalizedName.get(normalize(String.valueOf(name)));
        }
    }

    private static String requireResolved(String name, Map<String, String> ids) {
        final String id = ids.get(name);
        if (id == null) {
            throw new IllegalArgumentException("No known rule element named \"" + name + "\".");
        }
        return id;
    }

    /** Accent- and case-insensitive name key, shared with the rest of the application. */
    private static String normalize(final String name) {
        return Translations.normalize(name);
    }

    private static void addRank(Map<String, Integer> ranks, String id, int value) {
        ranks.merge(id, value, Integer::sum);
    }

    private static void mergeRanks(Map<String, Integer> target, Map<String, Integer> source) {
        source.forEach((id, ranks) -> target.merge(id, ranks, Integer::sum));
    }

    private static JsonNode required(JsonNode node, String field) {
        final JsonNode value = node.get(field);
        if (value == null) {
            throw new IllegalArgumentException("The legacy character JSON has no \"" + field + "\" field.");
        }
        return value;
    }
}