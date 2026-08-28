package com.softwaremagico.librodeesher.perk;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One "choose N skills/categories from &lt;scope&gt;" entry of a perk's "bonuses" column (the {@code
 * "{...}"} syntax, see {@link Perk#getChoiceGrants()}), matching the legacy {@code
 * PerkFactory#addListToChooseBonus}. {@code <scope>} is either one of the four special markers (see
 * {@link #getScope()}) or a single, specific category/skill ({@link #getCategoryId()}/{@link
 * #getSkillId()}) - the shipped data never lists more than one explicit option or mixes categories
 * and skills in the same entry, unlike the legacy grammar's more general "{alt1;alt2}" support.
 */
public class PerkChoiceGrant {

    @JsonProperty("optionsToChoose")
    private Integer optionsToChoose;

    @JsonProperty("scope")
    private PerkChoiceScope scope;

    @JsonProperty("categoryId")
    private String categoryId;

    @JsonProperty("skillId")
    private String skillId;

    @JsonProperty("kind")
    private PerkBonusKind kind;

    @JsonProperty("value")
    private Integer value;

    public PerkChoiceGrant() {
        // Required by Jackson.
    }

    public Integer getOptionsToChoose() {
        return optionsToChoose;
    }

    public void setOptionsToChoose(Integer optionsToChoose) {
        this.optionsToChoose = optionsToChoose;
    }

    public PerkChoiceScope getScope() {
        return scope;
    }

    public void setScope(PerkChoiceScope scope) {
        this.scope = scope;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public PerkBonusKind getKind() {
        return kind;
    }

    public void setKind(PerkBonusKind kind) {
        this.kind = kind;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
