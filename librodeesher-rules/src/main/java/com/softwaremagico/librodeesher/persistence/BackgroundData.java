package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A flattened {@link com.softwaremagico.librodeesher.background.Background} for persistence, with
 * the language-rank map key-sorted so saves are byte-stable.
 */
public final class BackgroundData {

    @JsonProperty("categoryIds")
    private List<String> categoryIds = new ArrayList<>();

    @JsonProperty("skillIds")
    private List<String> skillIds = new ArrayList<>();

    @JsonProperty("characteristicUpdates")
    private List<CharacteristicRoll> characteristicUpdates = new ArrayList<>();

    @JsonProperty("languageRanks")
    private Map<String, Integer> languageRanks = new LinkedHashMap<>();

    public BackgroundData() {
        // Required by deserialization frameworks.
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public List<String> getSkillIds() {
        return skillIds;
    }

    public void setSkillIds(List<String> skillIds) {
        this.skillIds = skillIds;
    }

    public List<CharacteristicRoll> getCharacteristicUpdates() {
        return characteristicUpdates;
    }

    public void setCharacteristicUpdates(List<CharacteristicRoll> characteristicUpdates) {
        this.characteristicUpdates = characteristicUpdates;
    }

    public Map<String, Integer> getLanguageRanks() {
        return languageRanks;
    }

    public void setLanguageRanks(Map<String, Integer> languageRanks) {
        this.languageRanks = languageRanks;
    }
}