package com.softwaremagico.librodeesher.race;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Language ranks granted by a race at creation or available through background points. */
public class RaceLanguage {

    @JsonProperty("name")
    private String name;

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

    public RaceLanguage(String name, Integer startingSpeakingRanks, Integer startingWritingRanks,
                        Integer maxSpeakingRanks, Integer maxWritingRanks) {
        this.name = name;
        this.startingSpeakingRanks = startingSpeakingRanks;
        this.startingWritingRanks = startingWritingRanks;
        this.maxSpeakingRanks = maxSpeakingRanks;
        this.maxWritingRanks = maxWritingRanks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
