package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A flattened {@link com.softwaremagico.librodeesher.level.LevelUp} for persistence: the same
 * state, with unordered map/set fields carried as key-sorted maps and sorted lists so two saves of
 * the same character produce byte-identical text.
 */
public final class LevelData {

    @JsonProperty("categoryRanks")
    private Map<String, Integer> categoryRanks = new LinkedHashMap<>();

    @JsonProperty("skillRanks")
    private Map<String, Integer> skillRanks = new LinkedHashMap<>();

    @JsonProperty("spellListRanks")
    private Map<String, Integer> spellListRanks = new LinkedHashMap<>();

    @JsonProperty("generalizedSkills")
    private List<String> generalizedSkills = new ArrayList<>();

    @JsonProperty("spellsUpdated")
    private List<String> spellsUpdated = new ArrayList<>();

    @JsonProperty("trainings")
    private List<String> trainings = new ArrayList<>();

    @JsonProperty("skillSpecializations")
    private List<String> skillSpecializations = new ArrayList<>();

    @JsonProperty("favouriteSkills")
    private List<String> favouriteSkills = new ArrayList<>();

    @JsonProperty("characteristicUpdates")
    private List<CharacteristicRoll> characteristicUpdates = new ArrayList<>();

    @JsonProperty("ageModifications")
    private List<AgeModification> ageModifications = new ArrayList<>();

    public LevelData() {
        // Required by deserialization frameworks.
    }

    public Map<String, Integer> getCategoryRanks() {
        return categoryRanks;
    }

    public void setCategoryRanks(Map<String, Integer> categoryRanks) {
        this.categoryRanks = categoryRanks;
    }

    public Map<String, Integer> getSkillRanks() {
        return skillRanks;
    }

    public void setSkillRanks(Map<String, Integer> skillRanks) {
        this.skillRanks = skillRanks;
    }

    public Map<String, Integer> getSpellListRanks() {
        return spellListRanks;
    }

    public void setSpellListRanks(Map<String, Integer> spellListRanks) {
        this.spellListRanks = spellListRanks;
    }

    public List<String> getGeneralizedSkills() {
        return generalizedSkills;
    }

    public void setGeneralizedSkills(List<String> generalizedSkills) {
        this.generalizedSkills = generalizedSkills;
    }

    public List<String> getSpellsUpdated() {
        return spellsUpdated;
    }

    public void setSpellsUpdated(List<String> spellsUpdated) {
        this.spellsUpdated = spellsUpdated;
    }

    public List<String> getTrainings() {
        return trainings;
    }

    public void setTrainings(List<String> trainings) {
        this.trainings = trainings;
    }

    public List<String> getSkillSpecializations() {
        return skillSpecializations;
    }

    public void setSkillSpecializations(List<String> skillSpecializations) {
        this.skillSpecializations = skillSpecializations;
    }

    public List<String> getFavouriteSkills() {
        return favouriteSkills;
    }

    public void setFavouriteSkills(List<String> favouriteSkills) {
        this.favouriteSkills = favouriteSkills;
    }

    public List<CharacteristicRoll> getCharacteristicUpdates() {
        return characteristicUpdates;
    }

    public void setCharacteristicUpdates(List<CharacteristicRoll> characteristicUpdates) {
        this.characteristicUpdates = characteristicUpdates;
    }

    public List<AgeModification> getAgeModifications() {
        return ageModifications;
    }

    public void setAgeModifications(List<AgeModification> ageModifications) {
        this.ageModifications = ageModifications;
    }
}