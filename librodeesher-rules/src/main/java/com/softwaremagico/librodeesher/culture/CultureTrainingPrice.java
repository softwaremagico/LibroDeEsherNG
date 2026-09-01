package com.softwaremagico.librodeesher.culture;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Discount/price multiplier for one training in a culture. */
public class CultureTrainingPrice {
    @JsonProperty("trainingId")
    private String trainingId;
    @JsonProperty("price")
    private Double price;

    public CultureTrainingPrice() {}
    public CultureTrainingPrice(String trainingId, Double price) {
        this.trainingId = trainingId;
        this.price = price;
    }
    public String getTrainingId() { return trainingId; }
    public void setTrainingId(String trainingId) { this.trainingId = trainingId; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
