package com.softwaremagico.librodeesher.characteristic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.dice.Roll;

/**
 * A single characteristic development roll made during a level-up: which characteristic, its
 * temporal/potential values right before the roll, and the roll itself (see
 * {@link Characteristic#getCharacteristicUpgrade}).
 */
public class CharacteristicRoll {

    @JsonProperty("characteristicAbbreviation")
    private CharacteristicAbbreviation characteristicAbbreviation;
    @JsonProperty("characteristicTemporalValue")
    private Integer characteristicTemporalValue;
    @JsonProperty("characteristicPotentialValue")
    private Integer characteristicPotentialValue;
    @JsonProperty("roll")
    private Roll roll;

    protected CharacteristicRoll() {
        // Required by deserialization frameworks.
    }

    public CharacteristicRoll(CharacteristicAbbreviation characteristicAbbreviation, Integer characteristicTemporalValue,
                               Integer characteristicPotentialValue, Roll roll) {
        this.characteristicAbbreviation = characteristicAbbreviation;
        this.characteristicTemporalValue = characteristicTemporalValue;
        this.characteristicPotentialValue = characteristicPotentialValue;
        this.roll = roll;
    }

    public CharacteristicAbbreviation getCharacteristicAbbreviation() {
        return characteristicAbbreviation;
    }

    public Integer getCharacteristicTemporalValue() {
        return characteristicTemporalValue;
    }

    public void setCharacteristicTemporalValue(Integer characteristicTemporalValue) {
        this.characteristicTemporalValue = characteristicTemporalValue;
    }

    public Integer getCharacteristicPotentialValue() {
        return characteristicPotentialValue;
    }

    public void setCharacteristicPotentialValue(Integer characteristicPotentialValue) {
        this.characteristicPotentialValue = characteristicPotentialValue;
    }

    public Roll getRoll() {
        return roll;
    }

    @Override
    public String toString() {
        return characteristicAbbreviation + "[" + characteristicTemporalValue + " ->" + characteristicPotentialValue + "]: " + roll;
    }
}
