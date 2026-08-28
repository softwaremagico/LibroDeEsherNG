package com.softwaremagico.librodeesher.language;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An anonymous "pick any race/regional language" language slot (the legacy "Idioma Racial"/"Idioma
 * Regional" marker in a race's or culture's languages section): grants starting and maximum ranks
 * without specifying which language fills it, the actual language being chosen by the player (see
 * {@code CharacterPlayer}'s optional-language assignment methods).
 */
public class LanguageSlot {

    @JsonProperty("startingSpeakingRanks")
    private Integer startingSpeakingRanks;

    @JsonProperty("startingWritingRanks")
    private Integer startingWritingRanks;

    @JsonProperty("maxSpeakingRanks")
    private Integer maxSpeakingRanks;

    @JsonProperty("maxWritingRanks")
    private Integer maxWritingRanks;

    public LanguageSlot() {
        // Required by Jackson.
    }

    public LanguageSlot(Integer startingSpeakingRanks, Integer startingWritingRanks, Integer maxSpeakingRanks,
                         Integer maxWritingRanks) {
        this.startingSpeakingRanks = startingSpeakingRanks;
        this.startingWritingRanks = startingWritingRanks;
        this.maxSpeakingRanks = maxSpeakingRanks;
        this.maxWritingRanks = maxWritingRanks;
    }

    public Integer getStartingSpeakingRanks() {
        return startingSpeakingRanks;
    }

    public void setStartingSpeakingRanks(Integer startingSpeakingRanks) {
        this.startingSpeakingRanks = startingSpeakingRanks;
    }

    public Integer getStartingWritingRanks() {
        return startingWritingRanks;
    }

    public void setStartingWritingRanks(Integer startingWritingRanks) {
        this.startingWritingRanks = startingWritingRanks;
    }

    public Integer getMaxSpeakingRanks() {
        return maxSpeakingRanks;
    }

    public void setMaxSpeakingRanks(Integer maxSpeakingRanks) {
        this.maxSpeakingRanks = maxSpeakingRanks;
    }

    public Integer getMaxWritingRanks() {
        return maxWritingRanks;
    }

    public void setMaxWritingRanks(Integer maxWritingRanks) {
        this.maxWritingRanks = maxWritingRanks;
    }
}
