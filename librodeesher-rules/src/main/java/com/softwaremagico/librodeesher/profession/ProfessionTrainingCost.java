package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.training.TrainingType;

/**
 * Background point cost (and preference) of a specific training for a profession, as printed in the
 * "ADIESTRAMIENTO" section of a {@code profesiones/*.txt} file.
 */
public class ProfessionTrainingCost {

    @JsonProperty("trainingName")
    private String trainingName;

    @JsonProperty("cost")
    private Integer cost;

    /** Alternate cost used in non-magic campaigns, when the file provides a third column. */
    @JsonProperty("costNotMagic")
    private Integer costNotMagic;

    @JsonProperty("type")
    private TrainingType type;

    public ProfessionTrainingCost() {
        // Required by Jackson.
    }

    public ProfessionTrainingCost(String trainingName, Integer cost, Integer costNotMagic, TrainingType type) {
        this.trainingName = trainingName;
        this.cost = cost;
        this.costNotMagic = costNotMagic;
        this.type = type;
    }

    public String getTrainingName() {
        return trainingName;
    }

    public void setTrainingName(String trainingName) {
        this.trainingName = trainingName;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public Integer getCostNotMagic() {
        return costNotMagic != null ? costNotMagic : cost;
    }

    public void setCostNotMagic(Integer costNotMagic) {
        this.costNotMagic = costNotMagic;
    }

    public TrainingType getType() {
        return type;
    }

    public void setType(TrainingType type) {
        this.type = type;
    }
}
