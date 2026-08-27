package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Collections;
import java.util.List;

/**
 * A skill rank grant nested under a {@link TrainingCategoryGrant}, e.g. {@code "Callejeo\t1"} (a
 * fixed grant of 1 rank in "Callejeo") or {@code "{Rastrear; Acechar}\t-3"} (3 ranks to distribute,
 * player's choice of either skill).
 *
 * <p>{@link #getSkillOptions()} has a single entry for a fixed grant, or several when the legacy file
 * used the {@code {alt1;alt2}} choose-one syntax (the leading "-" in the legacy rank number is purely
 * a visual marker for "choice" and carries no other meaning; it is stripped here).</p>
 */
public class TrainingSkillGrant {

    @JacksonXmlElementWrapper(localName = "skillOptions")
    @JacksonXmlProperty(localName = "skill")
    private List<String> skillOptions;

    @JsonProperty("ranksToDistribute")
    private Integer ranksToDistribute;

    public TrainingSkillGrant() {
        // Required by Jackson.
    }

    public TrainingSkillGrant(List<String> skillOptions, Integer ranksToDistribute) {
        this.skillOptions = skillOptions;
        this.ranksToDistribute = ranksToDistribute;
    }

    public List<String> getSkillOptions() {
        return skillOptions == null ? Collections.emptyList() : skillOptions;
    }

    public void setSkillOptions(List<String> skillOptions) {
        this.skillOptions = skillOptions;
    }

    /** Whether the player must choose one skill among several, rather than getting a fixed one. */
    public boolean isChoice() {
        return getSkillOptions().size() > 1;
    }

    public Integer getRanksToDistribute() {
        return ranksToDistribute;
    }

    public void setRanksToDistribute(Integer ranksToDistribute) {
        this.ranksToDistribute = ranksToDistribute;
    }
}
