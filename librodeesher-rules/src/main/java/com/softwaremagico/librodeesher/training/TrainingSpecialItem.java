package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.language.TranslatedText;

/**
 * One entry of a training's "ESPECIAL" (background items) section: a chance of starting play with a
 * given item, optionally with a skill bonus.
 *
 * <p>This project does not model equipment/items at all (no inventory, no item-bonus system in
 * {@code CharacterPlayer}), so {@link #getName()} is free descriptive text like {@code RaceSpecial}'s
 * (translated, not an id) and {@link #getSkillId()} is translated to an id-like string but
 * <strong>not</strong> cross-referenced against a real {@code Skill}/{@code Category}: the raw column
 * is inconsistent enough across the shipped data (mixed casing, comma-separated lists, non-skill
 * markers like "No mágica"/"Cualquier habilidad") that resolving it properly is future work, tied to
 * building an equipment system in the first place.</p>
 */
public class TrainingSpecialItem {

    @JsonProperty("name")
    private TranslatedText name;

    @JsonProperty("probability")
    private Integer probability;

    @JsonProperty("bonus")
    private Integer bonus;

    @JsonProperty("skillId")
    private String skillId;

    public TrainingSpecialItem() {
        // Required by Jackson.
    }

    public TrainingSpecialItem(TranslatedText name, Integer probability, Integer bonus, String skillId) {
        this.name = name;
        this.probability = probability;
        this.bonus = bonus;
        this.skillId = skillId;
    }

    public TranslatedText getName() {
        return name;
    }

    public void setName(TranslatedText name) {
        this.name = name;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer getBonus() {
        return bonus;
    }

    public void setBonus(Integer bonus) {
        this.bonus = bonus;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }
}
