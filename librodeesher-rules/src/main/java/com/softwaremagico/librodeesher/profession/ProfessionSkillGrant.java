package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Collections;
import java.util.List;

/**
 * One "choose N skills from &lt;X&gt;" entry of a profession's "HABILIDADES COMUNES"/"PROFESIONALES"/
 * "RESTRINGIDAS" section (the {@code "<X>#N"} syntax), as opposed to a plain skill name (which is
 * granted outright, with no choice at all, see {@link Profession#getCommonSkillIds()} and its
 * siblings).
 *
 * <p>{@code <X>} is either a whole category (every skill in it is a candidate, see {@link
 * #getCategoryId()}) or an explicit alternative list (see {@link #getSkillOptions()}); exactly one
 * of the two is set. Unlike {@link com.softwaremagico.librodeesher.training.ChoiceGroup}, which
 * always picks exactly one option, {@link #getRanksToChoose()} may be greater than one (e.g. "choose
 * 6 skills from the Oficios category").</p>
 */
public class ProfessionSkillGrant {

    @JsonProperty("ranksToChoose")
    private Integer ranksToChoose;

    @JsonProperty("categoryId")
    private String categoryId;

    @JacksonXmlElementWrapper(localName = "skillOptions")
    @JacksonXmlProperty(localName = "skillId")
    private List<String> skillOptions;

    public ProfessionSkillGrant() {
        // Required by Jackson.
    }

    public static ProfessionSkillGrant ofCategory(Integer ranksToChoose, String categoryId) {
        final ProfessionSkillGrant grant = new ProfessionSkillGrant();
        grant.ranksToChoose = ranksToChoose;
        grant.categoryId = categoryId;
        return grant;
    }

    public static ProfessionSkillGrant ofSkillOptions(Integer ranksToChoose, List<String> skillOptions) {
        final ProfessionSkillGrant grant = new ProfessionSkillGrant();
        grant.ranksToChoose = ranksToChoose;
        grant.skillOptions = skillOptions;
        return grant;
    }

    public Integer getRanksToChoose() {
        return ranksToChoose;
    }

    public void setRanksToChoose(Integer ranksToChoose) {
        this.ranksToChoose = ranksToChoose;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public List<String> getSkillOptions() {
        return skillOptions == null ? Collections.emptyList() : skillOptions;
    }

    public void setSkillOptions(List<String> skillOptions) {
        this.skillOptions = skillOptions;
    }
}
