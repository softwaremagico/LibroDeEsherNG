package com.softwaremagico.librodeesher.characteristic;

import com.softwaremagico.librodeesher.dice.Roll;

/** One of the ten characteristics a character has, identified by its {@link CharacteristicAbbreviation}. */
public class Characteristic {

    private CharacteristicAbbreviation abbreviation;

    public Characteristic(CharacteristicAbbreviation abbreviation) {
        this.abbreviation = abbreviation;
    }

    /**
     * Computes how many points a characteristic's temporal value increases after rolling {@code roll}
     * during a level-up, given its current temporal and potential values.
     *
     * <ul>
     *     <li>Doubles (both dice equal): a low double (&lt; 6) is a decrease; a high double (&ge; 6)
     *     doubles its value as an increase, capped at the remaining room to the potential value.</li>
     *     <li>Otherwise, the increase is the lower, higher, or sum of both dice depending on how much
     *     room is left before the potential value (bigger gaps use bigger dice combinations).</li>
     * </ul>
     */
    public static Integer getCharacteristicUpgrade(Integer temporalValue, Integer potentialValue, Roll roll) {
        // Compatibility with old inserted rolls.
        if (temporalValue == 0 || potentialValue == 0) {
            return 0;
        }
        if (roll.getFirstDice().equals(roll.getSecondDice())) {
            if (roll.getFirstDice() < 6) {
                return -roll.getFirstDice();
            }
            return Math.min(roll.getFirstDice() * 2, potentialValue - temporalValue);
        }
        if (potentialValue - temporalValue <= 10) {
            return Math.min(Math.min(roll.getFirstDice(), roll.getSecondDice()), potentialValue - temporalValue);
        } else if (potentialValue - temporalValue <= 20) {
            return Math.min(Math.max(roll.getFirstDice(), roll.getSecondDice()), potentialValue - temporalValue);
        }
        return Math.min(roll.getFirstDice() + roll.getSecondDice(), potentialValue - temporalValue);
    }

    /** Development points cost of a temporal value: 1 point per point up to 90, quadratic beyond that. */
    public static Integer getTemporalCost(Integer temporalValue) {
        if (temporalValue < 91) {
            return temporalValue;
        }
        final double cost = Math.pow(temporalValue - 90, 2) + 90;
        return (int) cost;
    }

    public CharacteristicAbbreviation getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(CharacteristicAbbreviation abbreviation) {
        this.abbreviation = abbreviation;
    }

    @Override
    public String toString() {
        return abbreviation.name() + " (" + abbreviation.getTag() + ")";
    }

    @Override
    public int hashCode() {
        return abbreviation == null ? 0 : abbreviation.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Characteristic)) {
            return false;
        }
        final Characteristic other = (Characteristic) obj;
        return abbreviation == other.abbreviation;
    }
}
