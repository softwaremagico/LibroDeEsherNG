package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One entry of a training's "HABILIDADES" section: ranks granted in a category (or a choice of
 * categories), how many of its skills the player must/may pick, and how many extra rank points can be
 * freely distributed among those skills. For example:
 *
 * <pre>Influencia	3	2	2	4
 *   *  Seducción	3</pre>
 *
 * <p>means "grants 3 ranks in the Influencia category, the player must select exactly 2 of its
 * skills (min 2, max 2), with 4 extra rank points to distribute among them; Seducción alone is
 * guaranteed 3 of those points". {@link #getCategoryOptions()} has more than one entry when the
 * legacy file used the {@code {Cat1; Cat2}} choose-one-category syntax.</p>
 *
 * <p><strong>Known simplification:</strong> the legacy application also allowed two special
 * pseudo-category tokens here ("arma"/"ataque", expanded at read time into every weapon/special-attack
 * category via {@code CategoryFactory}). This migration keeps such tokens as literal, unexpanded
 * strings in {@link #getCategoryOptions()} instead of resolving them against
 * {@link com.softwaremagico.librodeesher.category.CategoryFactory}, since that expansion is a
 * cross-reference better performed by the code that consumes this data at runtime (where every
 * module's categories are known) than baked in at migration time.</p>
 */
public class TrainingCategoryGrant {

    @JacksonXmlElementWrapper(localName = "categoryOptions")
    @JacksonXmlProperty(localName = "category")
    private List<String> categoryOptions;

    @JsonProperty("ranksGranted")
    private Integer ranksGranted;

    @JsonProperty("minSkills")
    private Integer minSkills;

    @JsonProperty("maxSkills")
    private Integer maxSkills;

    @JsonProperty("ranksToDistribute")
    private Integer ranksToDistribute;

    @JacksonXmlElementWrapper(localName = "skills")
    @JacksonXmlProperty(localName = "skillGrant")
    private List<TrainingSkillGrant> skills;

    public TrainingCategoryGrant() {
        skills = new ArrayList<>();
    }

    public List<String> getCategoryOptions() {
        return categoryOptions == null ? Collections.emptyList() : categoryOptions;
    }

    public void setCategoryOptions(List<String> categoryOptions) {
        this.categoryOptions = categoryOptions;
    }

    /** Whether the player must choose one category among several, rather than getting a fixed one. */
    public boolean isChoice() {
        return getCategoryOptions().size() > 1;
    }

    public Integer getRanksGranted() {
        return ranksGranted;
    }

    public void setRanksGranted(Integer ranksGranted) {
        this.ranksGranted = ranksGranted;
    }

    public Integer getMinSkills() {
        return minSkills;
    }

    public void setMinSkills(Integer minSkills) {
        this.minSkills = minSkills;
    }

    public Integer getMaxSkills() {
        return maxSkills;
    }

    public void setMaxSkills(Integer maxSkills) {
        this.maxSkills = maxSkills;
    }

    public Integer getRanksToDistribute() {
        return ranksToDistribute;
    }

    public void setRanksToDistribute(Integer ranksToDistribute) {
        this.ranksToDistribute = ranksToDistribute;
    }

    public List<TrainingSkillGrant> getSkills() {
        return skills == null ? Collections.emptyList() : skills;
    }

    public void setSkills(List<TrainingSkillGrant> skills) {
        this.skills = skills;
    }
}
