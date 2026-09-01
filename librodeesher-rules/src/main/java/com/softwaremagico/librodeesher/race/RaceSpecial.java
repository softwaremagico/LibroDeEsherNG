package com.softwaremagico.librodeesher.race;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.language.TranslatedText;

/** One line from a race's ESPECIALES section, optionally with a point cost in brackets. */
public class RaceSpecial {

    @JsonProperty("text")
    private TranslatedText text;

    @JsonProperty("points")
    private Integer points;

    public RaceSpecial() {
        // Required by Jackson.
    }

    public RaceSpecial(TranslatedText text, Integer points) {
        this.text = text;
        this.points = points;
    }

    public TranslatedText getText() {
        return text;
    }

    public void setText(TranslatedText text) {
        this.text = text;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }
}
