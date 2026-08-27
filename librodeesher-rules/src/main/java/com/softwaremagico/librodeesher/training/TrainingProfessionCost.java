package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Background point cost (and preference) of a training for a specific profession, as printed in the
 * optional trailing section of a {@code adiestramientos/*.txt} file. In practice, none of the real
 * "LibroDeEsher" training files use this section (profession-specific training costs are handled
 * elsewhere), but it is still parsed for completeness/fidelity with the legacy format.
 */
public class TrainingProfessionCost {

    @JsonProperty("profession")
    private String profession;

    @JsonProperty("cost")
    private Integer cost;

    @JsonProperty("type")
    private TrainingType type;

    public TrainingProfessionCost() {
        // Required by Jackson.
    }

    public TrainingProfessionCost(String profession, Integer cost, TrainingType type) {
        this.profession = profession;
        this.cost = cost;
        this.type = type;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public TrainingType getType() {
        return type;
    }

    public void setType(TrainingType type) {
        this.type = type;
    }
}
