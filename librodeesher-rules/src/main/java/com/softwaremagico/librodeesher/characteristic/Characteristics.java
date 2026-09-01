package com.softwaremagico.librodeesher.characteristic;

import com.softwaremagico.librodeesher.dice.Dice;
import com.softwaremagico.librodeesher.dice.Roll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fixed rules governing the ten characteristics: the master list of {@link Characteristic}
 * instances, which ones count as "physical" vs. "mental" (used by {@code AgeRules} to pick a random
 * one to modify), and the tables converting a temporal value into its bonus or a rolled potential
 * value.
 */
public final class Characteristics {

    public static final int TOTAL_CHARACTERISTICS_POINTS = 660;
    public static final int INITIAL_CHARACTERISTIC_VALUE = 31;
    public static final int MAX_INITIAL_CHARACTERISTIC_VALUE = 100;

    public static final List<CharacteristicAbbreviation> MENTAL_CHARACTERISTICS = List.of(
            CharacteristicAbbreviation.MEMORY, CharacteristicAbbreviation.REASONING,
            CharacteristicAbbreviation.EMPATHY, CharacteristicAbbreviation.INTUITION,
            CharacteristicAbbreviation.PRESENCE);

    public static final List<CharacteristicAbbreviation> PHYSICAL_CHARACTERISTICS = List.of(
            CharacteristicAbbreviation.QUICKNESS, CharacteristicAbbreviation.STRENGTH,
            CharacteristicAbbreviation.SELF_DISCIPLINE, CharacteristicAbbreviation.CONSTITUTION,
            CharacteristicAbbreviation.AGILITY);

    private static final List<Characteristic> CHARACTERISTICS = List.of(
            new Characteristic(CharacteristicAbbreviation.AGILITY),
            new Characteristic(CharacteristicAbbreviation.CONSTITUTION),
            new Characteristic(CharacteristicAbbreviation.MEMORY),
            new Characteristic(CharacteristicAbbreviation.REASONING),
            new Characteristic(CharacteristicAbbreviation.SELF_DISCIPLINE),
            new Characteristic(CharacteristicAbbreviation.EMPATHY),
            new Characteristic(CharacteristicAbbreviation.INTUITION),
            new Characteristic(CharacteristicAbbreviation.PRESENCE),
            new Characteristic(CharacteristicAbbreviation.QUICKNESS),
            new Characteristic(CharacteristicAbbreviation.STRENGTH));

    private Characteristics() {
        // Utility class.
    }

    public static List<Characteristic> getCharacteristics() {
        return Collections.unmodifiableList(CHARACTERISTICS);
    }

    /**
     * Whether {@code abbreviation} is one of the ten two-letter characteristic tags <strong>excluding
     * Appearance</strong> (e.g. "Ag"), matching the legacy behaviour used to tell a characteristic tag
     * apart from a category/skill name while parsing profession/perk/training bonus columns.
     */
    public static boolean isCharacteristicValid(String abbreviation) {
        final CharacteristicAbbreviation resolved = CharacteristicAbbreviation.fromTag(abbreviation);
        return resolved != CharacteristicAbbreviation.NONE && resolved != CharacteristicAbbreviation.APPEARANCE;
    }

    public static Characteristic getCharacteristicFromAbbreviation(CharacteristicAbbreviation abbreviation) {
        for (final Characteristic characteristic : CHARACTERISTICS) {
            if (characteristic.getAbbreviation() == abbreviation) {
                return characteristic;
            }
        }
        return null;
    }

    /** The characteristic bonus (as printed on the character sheet) for a given temporal value. */
    public static Integer getTemporalBonus(Integer temporalValue) {
        if (temporalValue >= 100) {
            return 10 + (temporalValue - 100) * 2;
        }
        if (temporalValue >= 98) {
            return 9;
        }
        if (temporalValue >= 96) {
            return 8;
        }
        if (temporalValue >= 94) {
            return 7;
        }
        if (temporalValue >= 92) {
            return 6;
        }
        if (temporalValue >= 90) {
            return 5;
        }
        if (temporalValue >= 85) {
            return 4;
        }
        if (temporalValue >= 80) {
            return 3;
        }
        if (temporalValue >= 75) {
            return 2;
        }
        if (temporalValue >= 70) {
            return 1;
        }
        if (temporalValue >= 31) {
            return 0;
        }
        if (temporalValue >= 26) {
            return -1;
        }
        if (temporalValue >= 21) {
            return -2;
        }
        if (temporalValue >= 16) {
            return -3;
        }
        if (temporalValue >= 11) {
            return -4;
        }
        if (temporalValue >= 10) {
            return -5;
        }
        if (temporalValue >= 8) {
            return -6;
        }
        if (temporalValue >= 6) {
            return -7;
        }
        if (temporalValue >= 4) {
            return -8;
        }
        if (temporalValue >= 2) {
            return -9;
        }
        return -10;
    }

    /** Rolls a potential value for a given initial (rolled) temporal value. */
    public static Integer getPotential(Integer temporalValue) {
        if (temporalValue >= 100) {
            return Math.max(99 + Dice.getRoll(1, 2), temporalValue);
        }
        if (temporalValue >= 99) {
            return 98 + Dice.getRoll(1, 2);
        }
        if (temporalValue >= 98) {
            return 97 + Dice.getRoll(1, 3);
        }
        if (temporalValue >= 97) {
            return 96 + Dice.getRoll(1, 4);
        }
        if (temporalValue >= 96) {
            return 95 + Dice.getRoll(1, 5);
        }
        if (temporalValue >= 95) {
            return 94 + Dice.getRoll(1, 6);
        }
        if (temporalValue >= 94) {
            return 93 + Dice.getRoll(1, 7);
        }
        if (temporalValue >= 93) {
            return 92 + Dice.getRoll(1, 8);
        }
        if (temporalValue >= 92) {
            return 91 + Dice.getRoll(1, 9);
        }
        if (temporalValue >= 85) {
            return 90 + Dice.getRoll(1, 10);
        }
        if (temporalValue >= 75) {
            return 80 + Dice.getRoll(2, 10);
        }
        if (temporalValue >= 65) {
            return 70 + Dice.getRoll(3, 10);
        }
        if (temporalValue >= 55) {
            return 60 + Dice.getRoll(4, 10);
        }
        if (temporalValue >= 45) {
            return 50 + Dice.getRoll(5, 10);
        }
        if (temporalValue >= 35) {
            return 40 + Dice.getRoll(6, 10);
        }
        if (temporalValue >= 25) {
            return 30 + Dice.getRoll(7, 10);
        }
        if (temporalValue >= 20) {
            return 20 + Dice.getRoll(8, 10);
        }
        return 0;
    }

    /**
     * How much a temporal value increases after rolling {@code twoDices} during a level-up, given how
     * much room is left before the potential value (mirrors {@link Characteristic#getCharacteristicUpgrade},
     * without the "already maxed out" early exit).
     */
    public static Integer setTemporalIncrease(Integer currentTemporalValue, Integer potentialValue, Roll twoDices) {
        final int remaining = potentialValue - currentTemporalValue;
        if (!twoDices.getFirstDice().equals(twoDices.getSecondDice())) {
            if (remaining <= 10) {
                return Math.min(twoDices.getFirstDice(), twoDices.getSecondDice());
            } else if (remaining <= 20) {
                return Math.max(twoDices.getFirstDice(), twoDices.getSecondDice());
            }
            return twoDices.getFirstDice() + twoDices.getSecondDice();
        }
        if (twoDices.getFirstDice() < 6) {
            return -twoDices.getFirstDice();
        }
        return twoDices.getFirstDice() * 2;
    }
}
