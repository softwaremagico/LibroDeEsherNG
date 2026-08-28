package com.softwaremagico.librodeesher.race;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Language ranks granted by a race at creation or available through background points. */
public class RaceLanguage {

    @JsonProperty("languageId")
    private String languageId;

    @JsonProperty("startingSpeakingRanks")
    private Integer startingSpeakingRanks;

    @JsonProperty("startingWritingRanks")
    private Integer startingWritingRanks;

    @JsonProperty("maxSpeakingRanks")
    private Integer maxSpeakingRanks;

    @JsonProperty("maxWritingRanks")
    private Integer maxWritingRanks;

    public RaceLanguage() {
        // Required by Jackson.
    }

    public RaceLanguage(String languageId, Integer startingSpeakingRanks, Integer startingWritingRanks,
                        Integer maxSpeakingRanks, Integer maxWritingRanks) {
        this.languageId = languageId;
        this.startingSpeakingRanks = startingSpeakingRanks;
        this.startingWritingRanks = startingWritingRanks;
        this.maxSpeakingRanks = maxSpeakingRanks;
        this.maxWritingRanks = maxWritingRanks;
    }

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
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
