package com.softwaremagico.librodeesher.age;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.dice.Roll;

/**
 * One year's worth of age-related characteristic decrease, rolled by {@link AgeRules} whenever a
 * character grows old enough for their body/mind to start declining.
 */
public class AgeModification {

    public static final int INITIAL_AGE = 10;

    @JsonProperty("age")
    private int age;
    @JsonProperty("roll")
    private Roll roll;
    @JsonProperty("characteristicAbbreviation")
    private CharacteristicAbbreviation characteristicAbbreviation;
    @JsonProperty("raceType")
    private int raceType;

    protected AgeModification() {
        // Required by deserialization frameworks.
    }

    public AgeModification(int age, int raceType) {
        this.roll = new Roll();
        this.raceType = raceType;
        this.age = age;
        this.characteristicAbbreviation = AgeRules.getRandomAgeSelectedCharacteristic();
    }

    /**
     * How many points the selected characteristic decreases this year, depending on the race's
     * lifespan type (races that live longer decline more slowly).
     */
    public int getCharacteristicModification() {
        switch (raceType) {
            case 1:
                return ((roll.getFirstDice() + 1) / 2) - 1;
            case 2:
                return ((roll.getFirstDice() + 1) / 2) + 1;
            case 3:
                return roll.getFirstDice();
            case 4:
                return roll.getFirstDice() + 1;
            case 5:
                return roll.getFirstDice() + roll.getSecondDice() - 1;
            default:
                // Default value.
                return ((roll.getFirstDice() + 1) / 2) + 1;
        }
    }

    public int getAge() {
        return age;
    }

    public CharacteristicAbbreviation getCharacteristicAbbreviation() {
        return characteristicAbbreviation;
    }

    @Override
    public String toString() {
        return characteristicAbbreviation + " " + getCharacteristicModification();
    }
}
