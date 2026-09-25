package com.softwaremagico.librodeesher.characteristic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.dice.Dice;

/**
 * A character's physical appearance roll: a fixed 5d10 rolled once at character creation, combined
 * with the Presence characteristic's potential value to get the final appearance score.
 */
public class Appearance {

    @JsonProperty("dicesResult")
    private int dicesResult;

    public Appearance() {
        dicesResult = Dice.getRoll(5, 10);
    }

    /**
     * Rebuilds an appearance from a previously persisted dice roll (used when restoring a character,
     * so the roll is never thrown a second time).
     */
    public Appearance(int dicesResult) {
        this.dicesResult = dicesResult;
    }

    public int getTotal(int presencePotentialValue) {
        return presencePotentialValue - 25 + dicesResult;
    }

    public int getDicesResult() {
        return dicesResult;
    }

    public void setDicesResult(int dicesResult) {
        this.dicesResult = dicesResult;
    }
}
