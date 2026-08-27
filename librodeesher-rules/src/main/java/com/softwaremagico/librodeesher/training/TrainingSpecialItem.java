package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry of a training's "ESPECIAL" (background items) section: a chance of starting play with a
 * given item, optionally with a skill bonus.
 */
public class TrainingSpecialItem {

    @JsonProperty("name")
    private String name;

    @JsonProperty("probability")
    private Integer probability;

    @JsonProperty("bonus")
    private Integer bonus;

    @JsonProperty("skillName")
    private String skillName;

    public TrainingSpecialItem() {
        // Required by Jackson.
    }

    public TrainingSpecialItem(String name, Integer probability, Integer bonus, String skillName) {
        this.name = name;
        this.probability = probability;
        this.bonus = bonus;
        this.skillName = skillName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
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

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }
}
