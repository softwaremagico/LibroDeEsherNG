package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry of a training's "REQUISITOS PROFESIONALES" section: a minimum skill rank or
 * characteristic temporary value required to take the training, with a cost modification (usually
 * negative, i.e. a discount) applied when the requirement is met.
 *
 * <p>{@link #getName()} is not resolved to a {@link com.softwaremagico.librodeesher.skill.Skill} or a
 * characteristic at migration time (whether it is one or the other is only known once both are
 * cross-referenced against the character rules, left as future work); consuming code should look it
 * up in both places.</p>
 */
public class TrainingRequirement {

    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private Integer value;

    @JsonProperty("costModification")
    private Integer costModification;

    public TrainingRequirement() {
        // Required by Jackson.
    }

    public TrainingRequirement(String name, Integer value, Integer costModification) {
        this.name = name;
        this.value = value;
        this.costModification = costModification;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getCostModification() {
        return costModification;
    }

    public void setCostModification(Integer costModification) {
        this.costModification = costModification;
    }
}
