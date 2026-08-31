package com.softwaremagico.librodeesher.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.language.TranslatedText;

import java.util.Objects;

/**
 * A piece of non-magic equipment (flavor only: no weight, no price, no game-mechanical effect),
 * matching the legacy {@code Equipment} exactly minus its persistence concerns. Every shipped
 * instance actually comes from a {@link com.softwaremagico.librodeesher.training.TrainingSpecialItem}
 * with no bonus (see {@link com.softwaremagico.librodeesher.training.TrainingSpecialItem#isMagic()});
 * a character can also be given one directly (not tied to any training), matching the legacy
 * {@code CharacterPlayer#addStandardEquipment}.
 */
public class Equipment {

    @JsonProperty("name")
    private TranslatedText name;

    @JsonProperty("description")
    private TranslatedText description;

    public Equipment() {
        // Required by Jackson.
    }

    public Equipment(TranslatedText name, TranslatedText description) {
        this.name = name;
        this.description = description;
    }

    public TranslatedText getName() {
        return name;
    }

    public void setName(TranslatedText name) {
        this.name = name;
    }

    public TranslatedText getDescription() {
        return description;
    }

    public void setDescription(TranslatedText description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Equipment equipment)) {
            return false;
        }
        return Objects.equals(name, equipment.name) && Objects.equals(description, equipment.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
