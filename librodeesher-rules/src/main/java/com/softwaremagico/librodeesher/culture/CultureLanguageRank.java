package com.softwaremagico.librodeesher.culture;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Maximum ranks for speaking/writing one language in a culture. */
public class CultureLanguageRank {
    @JsonProperty("languageId")
    private String languageId;
    @JsonProperty("maxSpeakingRanks")
    private Integer maxSpeakingRanks;
    @JsonProperty("maxWritingRanks")
    private Integer maxWritingRanks;

    public CultureLanguageRank() {}
    public CultureLanguageRank(String languageId, Integer maxSpeakingRanks, Integer maxWritingRanks) {
        this.languageId = languageId;
        this.maxSpeakingRanks = maxSpeakingRanks;
        this.maxWritingRanks = maxWritingRanks;
    }
    public String getLanguageId() { return languageId; }
    public void setLanguageId(String languageId) { this.languageId = languageId; }
    public Integer getMaxSpeakingRanks() { return maxSpeakingRanks; }
    public void setMaxSpeakingRanks(Integer maxSpeakingRanks) { this.maxSpeakingRanks = maxSpeakingRanks; }
    public Integer getMaxWritingRanks() { return maxWritingRanks; }
    public void setMaxWritingRanks(Integer maxWritingRanks) { this.maxWritingRanks = maxWritingRanks; }
}
