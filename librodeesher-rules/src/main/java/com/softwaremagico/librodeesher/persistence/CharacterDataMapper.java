package com.softwaremagico.librodeesher.persistence;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.characteristic.Appearance;
import com.softwaremagico.librodeesher.background.Background;
import com.softwaremagico.librodeesher.decision.Decision;
import com.softwaremagico.librodeesher.decision.Decisions;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.perk.SelectedPerk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Maps a {@link CharacterPlayer} to its flat {@link CharacterData} snapshot and back, restoring
 * every piece of state through the model's own mutators (never by replaying the decisions, which
 * would double-count already-applied grants). Maps and sets are sorted on the way out, so two saves
 * of the same character are byte-identical.
 */
public final class CharacterDataMapper {

    private CharacterDataMapper() {
        // Utility class.
    }

    /** Snapshots {@code character} into a flat {@link CharacterData}. */
    public static CharacterData toData(CharacterPlayer character) {
        final CharacterData data = new CharacterData();
        data.setName(character.getName());
        data.setSex(character.getSex());
        data.setRaceId(character.getRaceId());
        data.setCultureId(character.getCultureId());
        data.setProfessionId(character.getProfessionId());
        data.setHistoryText(character.getHistoryText());

        final Map<String, Integer> temporalValues = new TreeMap<>();
        final Map<String, Integer> potentialValues = new TreeMap<>();
        for (final CharacteristicAbbreviation abbreviation : realCharacteristics()) {
            temporalValues.put(abbreviation.name(), character.getCharacteristicTemporalValue(abbreviation));
            potentialValues.put(abbreviation.name(), character.getCharacteristicPotentialValue(abbreviation));
        }
        data.setCharacteristicTemporalValues(temporalValues);
        data.setCharacteristicPotentialValues(potentialValues);
        data.setCharacteristicsConfirmed(character.isCharacteristicsConfirmed());
        data.setAppearance(character.getAppearance().getDicesResult());
        data.setCurrentAge(character.getCurrentAge());
        data.setFinalAge(character.getFinalAge());

        final List<LevelData> levels = new ArrayList<>();
        for (final LevelUp levelUp : character.getLevels()) {
            final LevelData levelData = new LevelData();
            levelData.setCategoryRanks(new TreeMap<>(levelUp.getCategoryRanks()));
            levelData.setSkillRanks(new TreeMap<>(levelUp.getSkillRanks()));
            levelData.setSpellListRanks(new TreeMap<>(levelUp.getSpellListRanks()));
            levelData.setGeneralizedSkills(sorted(levelUp.getGeneralizedSkills()));
            levelData.setSpellsUpdated(new ArrayList<>(levelUp.getSpellsUpdated()));
            levelData.setTrainings(new ArrayList<>(levelUp.getTrainings()));
            levelData.setSkillSpecializations(sorted(levelUp.getSkillSpecializations()));
            levelData.setFavouriteSkills(sorted(levelUp.getFavouriteSkills()));
            levelData.setCharacteristicUpdates(new ArrayList<>(levelUp.getCharacteristicUpdates()));
            levelData.setAgeModifications(new ArrayList<>(levelUp.getAgeModifications()));
            levels.add(levelData);
        }
        data.setLevels(levels);

        final Background background = character.getBackground();
        final BackgroundData backgroundData = new BackgroundData();
        backgroundData.setCategoryIds(new ArrayList<>(background.getCategoryIds()));
        backgroundData.setSkillIds(new ArrayList<>(background.getSkillIds()));
        backgroundData.setCharacteristicUpdates(new ArrayList<>(background.getCharacteristicUpdates()));
        backgroundData.setLanguageRanks(new TreeMap<>(background.getLanguageRanks()));
        data.setBackground(backgroundData);

        final Map<String, DecisionData> decisionData = new TreeMap<>();
        for (final Map.Entry<String, Decision> entry : character.getDecisions().getAll().entrySet()) {
            final Decision decision = entry.getValue();
            decisionData.put(entry.getKey(), new DecisionData(
                    new ArrayList<>(decision.getOfferedOptions()), new ArrayList<>(decision.getSelectedOptions())));
        }
        data.setDecisions(decisionData);

        data.setSelectedPerks(new ArrayList<>(character.getSelectedPerks()));
        data.setHobbySkillRanks(new TreeMap<>(character.getHobbySkillRanks()));
        data.setHobbySpellListRanks(new TreeMap<>(character.getHobbySpellListRanks()));
        data.setMagicItems(new ArrayList<>(character.getAllMagicItems()));

        final List<Equipment> equipment = new ArrayList<>(character.getStandardEquipment());
        equipment.sort((left, right) -> left.getName().getSpanish().compareTo(right.getName().getSpanish()));
        data.setStandardEquipment(equipment);

        data.setFirearmsAllowed(character.isFirearmsAllowed());
        data.setChiPowersAllowed(character.isChiPowersAllowed());
        data.setOtherRealmTrainingSpellsAllowed(character.isOtherRealmTrainingSpellsAllowed());
        data.setMagicAllowed(character.isMagicAllowed());
        data.setDarkSpellsAsBasicListsAllowed(character.isDarkSpellsAsBasicListsAllowed());
        return data;
    }

    /** Rebuilds a {@link CharacterPlayer} from a {@link CharacterData} snapshot. */
    public static CharacterPlayer toCharacter(CharacterData data) {
        final CharacterPlayer character = new CharacterPlayer();
        character.setName(data.getName());
        character.setSex(data.getSex() == null ? null : data.getSex());
        character.setRaceId(data.getRaceId());
        character.setCultureId(data.getCultureId());
        character.setProfessionId(data.getProfessionId());
        character.setHistoryText(data.getHistoryText());

        for (final Map.Entry<String, Integer> entry : data.getCharacteristicTemporalValues().entrySet()) {
            character.setCharacteristicTemporalValue(CharacteristicAbbreviation.valueOf(entry.getKey()),
                    entry.getValue());
        }
        for (final Map.Entry<String, Integer> entry : data.getCharacteristicPotentialValues().entrySet()) {
            character.setCharacteristicPotentialValue(CharacteristicAbbreviation.valueOf(entry.getKey()),
                    entry.getValue());
        }
        if (data.isCharacteristicsConfirmed()) {
            character.setCharacteristicsAsConfirmed();
        }
        character.setAppearance(new Appearance(data.getAppearance()));
        character.setCurrentAge(data.getCurrentAge());
        character.setFinalAge(data.getFinalAge());

        if (!data.getLevels().isEmpty()) {
            character.getLevels().clear();
            for (final LevelData levelData : data.getLevels()) {
                character.getLevels().add(toLevelUp(levelData));
            }
        }

        final Background background = character.getBackground();
        background.setCategoryIds(new ArrayList<>(data.getBackground().getCategoryIds()));
        background.setSkillIds(new ArrayList<>(data.getBackground().getSkillIds()));
        background.getCharacteristicUpdates().clear();
        for (final CharacteristicRoll roll : data.getBackground().getCharacteristicUpdates()) {
            background.addCharacteristicUpdate(roll.getCharacteristicAbbreviation(),
                    roll.getCharacteristicTemporalValue(), roll.getCharacteristicPotentialValue(), roll.getRoll());
        }
        final Map<String, Integer> languageRanks = background.getLanguageRanks();
        languageRanks.clear();
        languageRanks.putAll(data.getBackground().getLanguageRanks());

        final Decisions decisions = character.getDecisions();
        for (final Map.Entry<String, DecisionData> entry : data.getDecisions().entrySet()) {
            final DecisionData decisionData = entry.getValue();
            final Decision decision;
            if (decisionData.getSelectedOptions().size() == 1) {
                decision = Decision.select(decisionData.getOfferedOptions(), decisionData.getSelectedOptions().get(0));
            } else {
                decision = Decision.selectMultiple(decisionData.getOfferedOptions(), decisionData.getSelectedOptions(),
                        decisionData.getSelectedOptions().size());
            }
            decisions.set(entry.getKey(), decision);
        }

        character.getSelectedPerks().clear();
        for (final SelectedPerk perk : data.getSelectedPerks()) {
            final SelectedPerk restoredPerk = new SelectedPerk();
            restoredPerk.setPerkId(perk.getPerkId());
            restoredPerk.setWeaknessId(perk.getWeaknessId());
            restoredPerk.setRandom(perk.isRandom());
            character.getSelectedPerks().add(restoredPerk);
        }

        for (final Map.Entry<String, Integer> entry : data.getHobbySkillRanks().entrySet()) {
            character.setHobbySkillRank(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, Integer> entry : data.getHobbySpellListRanks().entrySet()) {
            character.setHobbySpellListRank(entry.getKey(), entry.getValue());
        }
        for (final MagicObject magicObject : data.getMagicItems()) {
            character.addMagicItem(magicObject);
        }
        for (final Equipment equipment : data.getStandardEquipment()) {
            character.addStandardEquipment(equipment);
        }

        character.setFirearmsAllowed(data.isFirearmsAllowed());
        character.setChiPowersAllowed(data.isChiPowersAllowed());
        character.setOtherRealmTrainingSpellsAllowed(data.isOtherRealmTrainingSpellsAllowed());
        character.setMagicAllowed(data.isMagicAllowed());
        character.setDarkSpellsAsBasicListsAllowed(data.isDarkSpellsAsBasicListsAllowed());
        return character;
    }

    /** Rebuilds a {@link LevelUp} from a flat {@link LevelData} (used when importing a shared level). */
    public static LevelUp toLevelUp(LevelData levelData) {
        final LevelUp levelUp = new LevelUp();
        levelUp.setCategoryRanks(new HashMap<>(levelData.getCategoryRanks()));
        levelUp.setSkillRanks(new HashMap<>(levelData.getSkillRanks()));
        levelUp.setSpellListRanks(new HashMap<>(levelData.getSpellListRanks()));
        levelUp.setTrainings(new ArrayList<>(levelData.getTrainings()));
        levelUp.setSpellsUpdated(new ArrayList<>(levelData.getSpellsUpdated()));
        levelUp.setGeneralizedSkills(new HashSet<>(levelData.getGeneralizedSkills()));
        levelUp.setSkillSpecializations(new HashSet<>(levelData.getSkillSpecializations()));
        levelUp.setFavouriteSkills(new HashSet<>(levelData.getFavouriteSkills()));
        for (final CharacteristicRoll roll : levelData.getCharacteristicUpdates()) {
            levelUp.addCharacteristicUpdate(roll.getCharacteristicAbbreviation(), roll.getCharacteristicTemporalValue(),
                    roll.getCharacteristicPotentialValue(), roll.getRoll());
        }
        levelUp.getAgeModifications().addAll(levelData.getAgeModifications());
        return levelUp;
    }

    /**
     * A byte-stable fingerprint of everything that defines a character's identity at creation time
     * (identity fields, characteristics, appearance, ages, background, perks and flags), without any
     * of the state that grows with each level (level bookkeeping, hobby ranks, equipment, magic
     * items and decisions). Two copies of the same character share the fingerprint regardless of how
     * many levels they have progressed, which is what {@link LevelJsonManager} uses to accept a
     * shared level for the right character.
     */
    public static String toIdentitySnapshot(CharacterPlayer character) {
        final CharacterData data = toData(character);
        data.setLevels(new ArrayList<>());
        data.setDecisions(new LinkedHashMap<>());
        data.setHobbySkillRanks(new LinkedHashMap<>());
        data.setHobbySpellListRanks(new LinkedHashMap<>());
        data.setMagicItems(new ArrayList<>());
        data.setStandardEquipment(new ArrayList<>());
        return CharacterJsonManager.toJson(data);
    }

    private static List<CharacteristicAbbreviation> realCharacteristics() {
        final List<CharacteristicAbbreviation> abbreviations = new ArrayList<>();
        for (final CharacteristicAbbreviation abbreviation : CharacteristicAbbreviation.values()) {
            if (abbreviation != CharacteristicAbbreviation.NONE
                    && abbreviation != CharacteristicAbbreviation.REALM_OF_MAGIC) {
                abbreviations.add(abbreviation);
            }
        }
        return abbreviations;
    }

    private static List<String> sorted(Iterable<String> values) {
        final List<String> sorted = new ArrayList<>();
        for (final String value : values) {
            sorted.add(value);
        }
        sorted.sort(String::compareTo);
        return sorted;
    }
}