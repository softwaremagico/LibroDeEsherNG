package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.decision.Decision;
import com.softwaremagico.librodeesher.magic.MagicSpellList;

import java.util.Collections;
import java.util.List;

/**
 * A spell-list rank grant nested under a {@link TrainingCategoryGrant}, the
 * {@link MagicSpellList} counterpart of {@link TrainingSkillGrant}. It exists because a "Listas
 * Básicas de Hechizos" (or "Listas Hechizos de Adiestramiento") grant does not name skills at all: it
 * names spell lists, e.g. from the legacy rulebook
 *
 * <pre>Listas Hechizos de Adiestramiento	0	2	2	4
 *   *  Besos del Houri	2
 *   *  Engaños del Houri	2</pre>
 *
 * meaning "no ranks in the category itself, exactly 2 of these spell lists, 4 ranks to distribute
 * among them, 2 already guaranteed in each of the two named ones".
 *
 * <p>The legacy {@code SkillFactory} could not model this: a training grant only had a skill slot, so
 * every one of these names was silently turned into a brand new, standalone {@code Skill} that
 * nothing else in the data knows about. Keeping them as real spell lists is what lets
 * {@link com.softwaremagico.librodeesher.level.LevelUp#addSpellListRanks} record them, so they count
 * towards the spell-list rank totals and the "more than 5/10 lists per level" cost multiplier.
 *
 * <p>{@link #getSpellListOptions()} has a single entry for a fixed grant, or several when the legacy
 * file used the {@code {alt1; alt2}} choose-one syntax, mirroring {@link TrainingSkillGrant#resolve}.
 * </p>
 */
public class TrainingSpellListGrant {

    @JacksonXmlElementWrapper(localName = "spellListOptions")
    @JacksonXmlProperty(localName = "spellList")
    private List<String> spellListOptions;

    @JsonProperty("ranksToDistribute")
    private Integer ranksToDistribute;

    @JsonProperty("choice")
    private Boolean choice;

    public TrainingSpellListGrant() {
        // Required by Jackson.
    }

    public TrainingSpellListGrant(List<String> spellListOptions, Integer ranksToDistribute) {
        this.spellListOptions = spellListOptions;
        this.ranksToDistribute = ranksToDistribute;
    }

    public List<String> getSpellListOptions() {
        return spellListOptions == null ? Collections.emptyList() : spellListOptions;
    }

    public void setSpellListOptions(List<String> spellListOptions) {
        this.spellListOptions = spellListOptions;
    }

    public boolean isExplicitChoice() {
        return Boolean.TRUE.equals(choice);
    }

    /** Whether the player must choose one spell list among several; see {@link #isExplicitChoice()}. */
    public boolean isChoice() {
        return getSpellListOptions().size() > 1;
    }

    public Boolean getChoice() {
        return choice;
    }

    public void setChoice(Boolean choice) {
        this.choice = choice;
    }

    /**
     * Resolves which spell list this grant applies to: the single option of a fixed grant (for which
     * {@code selectedSpellListId} is ignored), an explicit choice requires
     * {@code selectedSpellListId} to be one of {@link #getSpellListOptions()}, and a non-choice grant
     * with several options picks the first one unless {@code selectedSpellListId} is given. Mirrors
     * {@link TrainingSkillGrant#resolve(String)}.
     */
    public Decision resolve(String selectedSpellListId) {
        final List<String> options = getSpellListOptions();
        if (options.size() <= 1) {
            return Decision.fixed(options);
        }
        if (!isExplicitChoice()) {
            return Decision.select(options,
                    selectedSpellListId == null || selectedSpellListId.isBlank() ? options.get(0) : selectedSpellListId);
        }
        return Decision.select(options, selectedSpellListId);
    }

    public Integer getRanksToDistribute() {
        return ranksToDistribute;
    }

    public void setRanksToDistribute(Integer ranksToDistribute) {
        this.ranksToDistribute = ranksToDistribute;
    }
}
