package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
     * @param legacyJson the raw content of a legacy {@code .rlm} JSON export (2.0.0).
     * @return a ready to save/serialize {@link CharacterData}.
     */
    public static CharacterData importLegacyJson(String legacyJson)
            throws IOException, InvalidXmlElementException {
        final JsonNode root = JSON.readTree(legacyJson);
        final RulesCatalog rulesCatalog = RulesCatalog.getInstance();

        final Map<String, String> categoryIds = resolveNames(rulesCatalog.getCategories());
        final Map<String, String> skillIds = resolveNames(rulesCatalog.getSkills());
        final Map<String, String> spellListIds = resolveNames(rulesCatalog.getSpellLists());
        final Map<String, String> raceIds = resolveNames(rulesCatalog.getRaces());
        final Map<String, String> cultureIds = resolveNames(rulesCatalog.getCultures());
        final Map<String, String> professionIds = resolveNames(rulesCatalog.getProfessions());
        final Map<String, String> perkIds = resolveNames(rulesCatalog.getPerks());

        final CharacterData data = new CharacterData();
        data.setName(required(root, "name").asText());
        data.setSex(SexType.valueOf(required(root, "sex").asText()));
        data.setRaceId(requireResolved(required(root, "raceName").asText(), raceIds));
        data.setCultureId(requireResolved(required(root, "cultureName").asText(), cultureIds));
        data.setProfessionId(requireResolved(required(root, "professionName").asText(), professionIds));

        setCharacteristicsAndAge(data, root);

        final List<LevelData> levels = new ArrayList<>();
        levels.add(buildLevelOne(rulesCatalog, root, categoryIds, skillIds, spellListIds, data.getCultureId()));
        for (int i = 1; i < 6; i++) {
            levels.add(emptyLevel(data.getCurrentAge()));
        }
        levels.add(buildInsertedLevel(root, categoryIds, skillIds, spellListIds, data.getCurrentAge()));
        final JsonNode levelUps = required(root, "levelUps");
        levels.add(fromLevelUp(levelUps.get(1), categoryIds, skillIds, spellListIds, data.getCurrentAge()));
        levels.add(fromLevelUp(levelUps.get(2), categoryIds, skillIds, spellListIds, data.getCurrentAge()));
        data.setLevels(levels);

        final BackgroundData background = new BackgroundData();
        background.setCategoryIds(List.of(requireResolved("Influencia", categoryIds)));
        background.setSkillIds(resolveSkillNames(root.path("background").path("skills"), skillIds));
        data.setBackground(background);

        final Map<String, DecisionData> decisions = new TreeMap<>();
        data.setDecisions(decisions);
        buildWeaponCostDecisions(root.path("professionDecisions").path("weaponsCost"), categoryIds, decisions);
        buildMagicRealmDecisions(root.path("realmOfMagic").path("magicRealmsAvailable"), data.getProfessionId(), decisions);
        data.setSelectedPerks(buildPerks(root.path("selectedPerks"), perkIds));
        data.setMagicItems(buildMagicItems(root.path("magicItems"), skillIds));

        applyConfiguration(root.path("configuration"), data);
        data.setRecommendedFavouriteSkillsIncluded(root.path("recommendedFavouriteSkillsIncluded").asBoolean(false));
        return data;
    }

    private static void setCharacteristicsAndAge(CharacterData data, JsonNode root) {
        data.setCharacteristicTemporalValues(
                characteristicMap(required(root, "insertedData").path("characteristicsTemporalValuesModification")));
        data.setCharacteristicPotentialValues(
                characteristicMap(required(root, "insertedData").path("characteristicsPotentialValuesModification")));
        data.setCharacteristicsConfirmed(root.path("characteristicsConfirmed").asBoolean(false));
        data.setAppearance(root.path("appearance").path("dicesResult").asInt(0));
        final int currentAge = root.path("currentAge").asInt(10);
        data.setCurrentAge(currentAge);
        data.setFinalAge(root.path("finalAge").asInt(currentAge));
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
                                         Map<String, String> skillIds, Map<String, String> spellListIds, int age) {
        final LevelData level = emptyLevel(age);
        level.setCategoryRanks(resolveRankedNames(levelUp.path("categoriesRanks"), categoryIds));
        final Map<String, Integer> skillRanks = new TreeMap<>();
        final Map<String, Integer> spellListRanks = new TreeMap<>();
        splitSkillsAndLists(levelUp.path("skillsRanks"), skillIds, spellListIds, skillRanks, spellListRanks);
        level.setSkillRanks(skillRanks);
        level.setSpellListRanks(spellListRanks);
        level.setSpellsUpdated(resolveListNames(levelUp.path("spellsUpdated"), spellListIds));

        final List<String> favourites = new ArrayList<>();
        for (final JsonNode favourite : levelUp.path("favouriteSkills")) {
            final String id = resolveFavourite(normalize(favourite.asText()), skillIds, spellListIds);
            if (id != null) {
                favourites.add(id);
            }
        }
        favourites.sort(String::compareTo);
        level.setFavouriteSkills(favourites);

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
            final String perkId = perkIds.get(normalize(perk.path("name").asText()));
            if (perkId != null) {
                perks.add(new SelectedPerk(perkId));
            }
        }
        return perks;
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
                    bonuses.add(new ObjectBonus(type, skillIds.get(normalize(bonus.path("bonusName").asText())),
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
        for (final Map.Entry<String, JsonNode> entry : node.properties()) {
            final int ranks = entry.getValue().asInt();
            if (ranks == 0) {
                continue;
            }
            String id = skillIds.get(normalize(entry.getKey()));
            if (id != null) {
                addRank(skillRanks, id, ranks);
                continue;
            }
            id = spellListIds.get(normalize(entry.getKey()));
            if (id != null) {
                addRank(spellListRanks, id, ranks);
            }
        }
    }

    private static Map<String, Integer> resolveRankedNames(JsonNode node, Map<String, String> ids) {
        final Map<String, Integer> ranks = new TreeMap<>();
        for (final Map.Entry<String, JsonNode> entry : node.properties()) {
            final int value = entry.getValue().asInt();
            if (value == 0) {
                continue;
            }
            final String id = ids.get(normalize(entry.getKey()));
            if (id != null) {
                addRank(ranks, id, value);
            }
        }
        return ranks;
    }

    private static List<String> resolveListNames(JsonNode node, Map<String, String> spellListIds) {
        final List<String> names = new ArrayList<>();
        for (final JsonNode child : node) {
            final String id = spellListIds.get(normalize(child.asText()));
            if (id != null) {
                names.add(id);
            }
        }
        return names;
    }

    private static List<String> resolveSkillNames(JsonNode node, Map<String, String> skillIds) {
        final List<String> skills = new ArrayList<>();
        for (final JsonNode skill : node) {
            final String id = skillIds.get(normalize(skill.asText()));
            if (id != null) {
                skills.add(id);
            }
        }
        return skills;
    }

    private static String resolveFavourite(String name, Map<String, String> skillIds, Map<String, String> spellListIds) {
        String id = skillIds.get(name);
        if (id == null) {
            id = spellListIds.get(name);
        }
        return id;
    }

    /** Indexes every rule element by its Spanish name (first occurrence wins), so legacy names can
     *  be resolved to ids. */
    private static Map<String, String> resolveNames(List<? extends Element> elements) {
        final Map<String, String> ids = new LinkedHashMap<>();
        for (final Element element : elements) {
            if (element.getName() != null && element.getName().getSpanish() != null) {
                ids.putIfAbsent(normalize(element.getName().getSpanish()), element.getId());
            }
        }
        return ids;
    }

    private static String requireResolved(String name, Map<String, String> ids) {
        final String id = ids.get(normalize(name));
        if (id == null) {
            throw new IllegalArgumentException("No known rule element named \"" + name + "\".");
        }
        return id;
    }

    private static String normalize(final String name) {
        return name == null ? "" : name.replaceAll("\\s+", " ").trim();
    }

    private static void addRank(Map<String, Integer> ranks, String id, int value) {
        ranks.merge(id, value, Integer::sum);
    }

    private static JsonNode required(JsonNode node, String field) {
        final JsonNode value = node.get(field);
        if (value == null) {
            throw new IllegalArgumentException("The legacy character JSON has no \"" + field + "\" field.");
        }
        return value;
    }
}