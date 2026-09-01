package com.softwaremagico.librodeesher.dice;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The result of rolling two ten-sided dice, used throughout the character rules (characteristic
 * development, age effects, ...) wherever the original game asks for "2d10".
 */
public class Roll {

    private static final int DEFAULT_FACES = 10;

    @JsonProperty("firstDice")
    private Integer firstDice;
    @JsonProperty("secondDice")
    private Integer secondDice;

    /** Rolls two new d10. */
    public Roll() {
        this(DEFAULT_FACES);
    }

    /** Rolls two new dice of {@code faces} faces. */
    public Roll(int faces) {
        firstDice = Dice.getRoll(faces);
        secondDice = Dice.getRoll(faces);
    }

    /** Copies an already-rolled result (e.g. when reading it back from storage). */
    public Roll(Roll roll) {
        this.firstDice = roll.getFirstDice();
        this.secondDice = roll.getSecondDice();
    }

    public Integer getFirstDice() {
        return firstDice;
    }

    public void setFirstDice(Integer firstDice) {
        this.firstDice = firstDice;
    }

    public Integer getSecondDice() {
        return secondDice;
    }

    public void setSecondDice(Integer secondDice) {
        this.secondDice = secondDice;
    }

    @Override
    public String toString() {
        return "(" + firstDice + "," + secondDice + ")";
    }
}
