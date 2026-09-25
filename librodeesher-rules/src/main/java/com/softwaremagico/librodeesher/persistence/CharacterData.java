package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.character.SexType;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.perk.SelectedPerk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A flat, self-contained snapshot of every {@link com.softwaremagico.librodeesher.character.CharacterPlayer}
 * state field, without any of the throwing/derived getters the live character exposes. It is the
 * single serialization boundary for the character (JSON here; any future format can reuse the same
 * DTO), and it is rebuilt on load through the model's own mutators so no state is double-counted.
 */
public final class CharacterData {

    @JsonProperty("name")
    private String name;

    @JsonProperty("sex")
    private SexType sex;

    @JsonProperty("raceId")
    private String raceId;

    @JsonProperty("cultureId")
    private String cultureId;

    @JsonProperty("professionId")
    private String professionId;

    @JsonProperty("historyText")
    private String historyText;

    @JsonProperty("characteristicTemporalValues")
    private Map<String, Integer> characteristicTemporalValues = new LinkedHashMap<>();

    @JsonProperty("characteristicPotentialValues")
    private Map<String, Integer> characteristicPotentialValues = new LinkedHashMap<>();

    @JsonProperty("characteristicsConfirmed")
    private boolean characteristicsConfirmed;

    @JsonProperty("appearance")
    private int appearance;

    @JsonProperty("currentAge")
    private int currentAge;

    @JsonProperty("finalAge")
    private int finalAge;

    @JsonProperty("levels")
    private List<LevelData> levels = new ArrayList<>();

    @JsonProperty("background")
    private BackgroundData background = new BackgroundData();

    @JsonProperty("decisions")
    private Map<String, DecisionData> decisions = new LinkedHashMap<>();

    @JsonProperty("selectedPerks")
    private List<SelectedPerk> selectedPerks = new ArrayList<>();

    @JsonProperty("hobbySkillRanks")
    private Map<String, Integer> hobbySkillRanks = new LinkedHashMap<>();

    @JsonProperty("hobbySpellListRanks")
    private Map<String, Integer> hobbySpellListRanks = new LinkedHashMap<>();

    @JsonProperty("magicItems")
    private List<MagicObject> magicItems = new ArrayList<>();

    @JsonProperty("standardEquipment")
    private List<Equipment> standardEquipment = new ArrayList<>();

    @JsonProperty("firearmsAllowed")
    private boolean firearmsAllowed;

    @JsonProperty("chiPowersAllowed")
    private boolean chiPowersAllowed;

    @JsonProperty("otherRealmTrainingSpellsAllowed")
    private boolean otherRealmTrainingSpellsAllowed;

    @JsonProperty("magicAllowed")
    private boolean magicAllowed;

    @JsonProperty("darkSpellsAsBasicListsAllowed")
    private boolean darkSpellsAsBasicListsAllowed;

    public CharacterData() {
        // Required by deserialization frameworks.
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SexType getSex() {
        return sex;
    }

    public void setSex(SexType sex) {
        this.sex = sex;
    }

    public String getRaceId() {
        return raceId;
    }

    public void setRaceId(String raceId) {
        this.raceId = raceId;
    }

    public String getCultureId() {
        return cultureId;
    }

    public void setCultureId(String cultureId) {
        this.cultureId = cultureId;
    }

    public String getProfessionId() {
        return professionId;
    }

    public void setProfessionId(String professionId) {
        this.professionId = professionId;
    }

    public String getHistoryText() {
        return historyText;
    }

    public void setHistoryText(String historyText) {
        this.historyText = historyText;
    }

    public Map<String, Integer> getCharacteristicTemporalValues() {
        return characteristicTemporalValues;
    }

    public void setCharacteristicTemporalValues(Map<String, Integer> characteristicTemporalValues) {
        this.characteristicTemporalValues = characteristicTemporalValues;
    }

    public Map<String, Integer> getCharacteristicPotentialValues() {
        return characteristicPotentialValues;
    }

    public void setCharacteristicPotentialValues(Map<String, Integer> characteristicPotentialValues) {
        this.characteristicPotentialValues = characteristicPotentialValues;
    }

    public boolean isCharacteristicsConfirmed() {
        return characteristicsConfirmed;
    }

    public void setCharacteristicsConfirmed(boolean characteristicsConfirmed) {
        this.characteristicsConfirmed = characteristicsConfirmed;
    }

    public int getAppearance() {
        return appearance;
    }

    public void setAppearance(int appearance) {
        this.appearance = appearance;
    }

    public int getCurrentAge() {
        return currentAge;
    }

    public void setCurrentAge(int currentAge) {
        this.currentAge = currentAge;
    }

    public int getFinalAge() {
        return finalAge;
    }

    public void setFinalAge(int finalAge) {
        this.finalAge = finalAge;
    }

    public List<LevelData> getLevels() {
        return levels;
    }

    public void setLevels(List<LevelData> levels) {
        this.levels = levels;
    }

    public BackgroundData getBackground() {
        return background;
    }

    public void setBackground(BackgroundData background) {
        this.background = background;
    }

    public Map<String, DecisionData> getDecisions() {
        return decisions;
    }

    public void setDecisions(Map<String, DecisionData> decisions) {
        this.decisions = decisions;
    }

    public List<SelectedPerk> getSelectedPerks() {
        return selectedPerks;
    }

    public void setSelectedPerks(List<SelectedPerk> selectedPerks) {
        this.selectedPerks = selectedPerks;
    }

    public Map<String, Integer> getHobbySkillRanks() {
        return hobbySkillRanks;
    }

    public void setHobbySkillRanks(Map<String, Integer> hobbySkillRanks) {
        this.hobbySkillRanks = hobbySkillRanks;
    }

    public Map<String, Integer> getHobbySpellListRanks() {
        return hobbySpellListRanks;
    }

    public void setHobbySpellListRanks(Map<String, Integer> hobbySpellListRanks) {
        this.hobbySpellListRanks = hobbySpellListRanks;
    }

    public List<MagicObject> getMagicItems() {
        return magicItems;
    }

    public void setMagicItems(List<MagicObject> magicItems) {
        this.magicItems = magicItems;
    }

    public List<Equipment> getStandardEquipment() {
        return standardEquipment;
    }

    public void setStandardEquipment(List<Equipment> standardEquipment) {
        this.standardEquipment = standardEquipment;
    }

    public boolean isFirearmsAllowed() {
        return firearmsAllowed;
    }

    public void setFirearmsAllowed(boolean firearmsAllowed) {
        this.firearmsAllowed = firearmsAllowed;
    }

    public boolean isChiPowersAllowed() {
        return chiPowersAllowed;
    }

    public void setChiPowersAllowed(boolean chiPowersAllowed) {
        this.chiPowersAllowed = chiPowersAllowed;
    }

    public boolean isOtherRealmTrainingSpellsAllowed() {
        return otherRealmTrainingSpellsAllowed;
    }

    public void setOtherRealmTrainingSpellsAllowed(boolean otherRealmTrainingSpellsAllowed) {
        this.otherRealmTrainingSpellsAllowed = otherRealmTrainingSpellsAllowed;
    }

    public boolean isMagicAllowed() {
        return magicAllowed;
    }

    public void setMagicAllowed(boolean magicAllowed) {
        this.magicAllowed = magicAllowed;
    }

    public boolean isDarkSpellsAsBasicListsAllowed() {
        return darkSpellsAsBasicListsAllowed;
    }

    public void setDarkSpellsAsBasicListsAllowed(boolean darkSpellsAsBasicListsAllowed) {
        this.darkSpellsAsBasicListsAllowed = darkSpellsAsBasicListsAllowed;
    }
}