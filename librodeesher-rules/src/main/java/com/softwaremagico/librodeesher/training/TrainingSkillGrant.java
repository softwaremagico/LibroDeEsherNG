package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.decision.Decision;

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

    @JsonProperty("choice")
    private Boolean choice;

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

    /**
     * Whether the player must choose one skill among several rather than getting a fixed one,
     * per the migrated rule data: an explicit {@code <choice>true</choice>}. Grants that expanded
     * a single marker into several {@link #getSkillOptions()} keep their original
     * {@code <choice>false</choice>}, meaning "no user prompt, pick any candidate".
     */
    public boolean isExplicitChoice() {
        return Boolean.TRUE.equals(choice);
    }

    /** Whether the player must choose one skill among several once expanded; see {@link #isExplicitChoice()}. */
    public boolean isChoice() {
        return getSkillOptions().size() > 1;
    }

    public Boolean getChoice() {
        return choice;
    }

    public void setChoice(Boolean choice) {
        this.choice = choice;
    }

    /**
     * Resolves which skill this grant applies to: the single option of a fixed grant (for which
     * {@code selectedSkillId} is ignored), an explicit choice requires {@code selectedSkillId} to
     * be one of {@link #getSkillOptions()}, and a non-choice grant with several expanded options
     * (see {@link #getResolvedAdolescenceRanks}) picks the first one unless {@code selectedSkillId}
     * is given.
     */
    public Decision resolve(String selectedSkillId) {
        final List<String> options = getSkillOptions();
        if (options.size() <= 1) {
            return Decision.fixed(options);
        }
        if (!isExplicitChoice()) {
            return Decision.select(options,
                    selectedSkillId == null || selectedSkillId.isBlank() ? options.get(0) : selectedSkillId);
        }
        return Decision.select(options, selectedSkillId);
    }

    public Integer getRanksToDistribute() {
        return ranksToDistribute;
    }

    public void setRanksToDistribute(Integer ranksToDistribute) {
        this.ranksToDistribute = ranksToDistribute;
    }
}
