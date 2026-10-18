package com.softwaremagico.librodeesher.age;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.dice.Roll;
import com.softwaremagico.librodeesher.level.LevelUp;

import java.util.List;
import java.util.function.Supplier;

/**
 * Age-related characteristic decline rules.
 *
 * <p>These are the pure formulas of the legacy {@code AgeRules}; they take their inputs directly
 * instead of a character object, so they are usable (and testable) independently. The legacy
 * orchestration that loops from the character's current age to its final age, mutating the
 * character's current level, lives in {@link com.softwaremagico.librodeesher.character.CharacterPlayer
 * #increaseAge()}, which drives them year by year through
 * {@link #hasCharacteristicDecrease(int, int, int, int, Supplier)}.</p>
 */
public final class AgeRules {

    private static final int PROBABILITY_PHYSICAL_CHARACTERISTIC = 60;

    private AgeRules() {
        // Utility class.
    }

    /**
     * Whether this year the character must roll a characteristic decrease, given their current age,
     * their race's expected lifespan, and their current Constitution/Self-Discipline total bonuses
     * (characteristic bonus plus any other modifiers already applied).
     */
    public static boolean hasCharacteristicDecrease(int currentAge, int expectedLifeYears, int constitutionTotalBonus,
                                                     int selfDisciplineTotalBonus) {
        return hasCharacteristicDecrease(currentAge, expectedLifeYears, constitutionTotalBonus,
                selfDisciplineTotalBonus, Roll::new);
    }

    /**
     * Same age check using the caller's roll source. A 2d10 result maps to each integer from 1 to 100
     * exactly once, preserving the legacy percentile-roll distribution.
     */
    public static boolean hasCharacteristicDecrease(int currentAge, int expectedLifeYears, int constitutionTotalBonus,
                                                     int selfDisciplineTotalBonus, Supplier<Roll> rollSupplier) {
        final int value = getPercentileRoll(rollSupplier) + constitutionTotalBonus * 2 + selfDisciplineTotalBonus
                + getAgeModification(currentAge, expectedLifeYears);
        return value < 100;
    }

    /** The age-period modifier used by {@link #hasCharacteristicDecrease}, keyed by life stage. */
    static int getAgeModification(int currentAge, int expectedLifeYears) {
        final float periodOfLife = ((float) currentAge) / expectedLifeYears;
        // Young / Adult / Middle age.
        if (periodOfLife < 0.75) {
            return 100;
        }
        // Old.
        if (periodOfLife < 0.80) {
            return -5;
        }
        // Very Old.
        if (periodOfLife < 0.94) {
            return -30;
        }
        // Venerable.
        if (periodOfLife < 1) {
            return -50;
        }
        return -75;
    }

    /** Picks a random characteristic to decrease this year (physical characteristics are more likely). */
    public static CharacteristicAbbreviation getRandomAgeSelectedCharacteristic() {
        return getRandomAgeSelectedCharacteristic(Roll::new);
    }

    /** Picks the age-decreased characteristic using the caller's roll source. */
    public static CharacteristicAbbreviation getRandomAgeSelectedCharacteristic(Supplier<Roll> rollSupplier) {
        if (getPercentileRoll(rollSupplier) < PROBABILITY_PHYSICAL_CHARACTERISTIC) {
            return Characteristics.PHYSICAL_CHARACTERISTICS.get(getUniformIndex(
                    Characteristics.PHYSICAL_CHARACTERISTICS.size(), rollSupplier));
        }
        return Characteristics.MENTAL_CHARACTERISTICS.get(getUniformIndex(
                Characteristics.MENTAL_CHARACTERISTICS.size(), rollSupplier));
    }

    private static int getPercentileRoll(Supplier<Roll> rollSupplier) {
        final Roll roll = rollSupplier.get();
        return (roll.getFirstDice() - 1) * 10 + roll.getSecondDice();
    }

    private static int getUniformIndex(int size, Supplier<Roll> rollSupplier) {
        final int acceptedValues = 100 - 100 % size;
        int value;
        do {
            value = getPercentileRoll(rollSupplier) - 1;
        } while (value >= acceptedValues);
        return value % size;
    }

    /** Sum, across every level, of the age-related temporal value change for one characteristic. */
    public static int getAgeCharacteristicTemporalValueChange(List<LevelUp> levelUps, CharacteristicAbbreviation abbreviation) {
        int modification = 0;
        for (final LevelUp levelUp : levelUps) {
            modification += getAgeCharacteristicTemporalValueChange(levelUp, abbreviation);
        }
        return modification;
    }

    /** The age-related temporal value change for one characteristic within a single level. */
    public static int getAgeCharacteristicTemporalValueChange(LevelUp levelUp, CharacteristicAbbreviation abbreviation) {
        int modification = 0;
        for (final AgeModification ageModification : levelUp.getAgeModifications()) {
            if (ageModification.getCharacteristicAbbreviation() == abbreviation) {
                modification += ageModification.getCharacteristicModification();
            }
        }
        return modification;
    }

    /**
     * Sum, across every level, of the age-related potential value change for one characteristic
     * (a third of the temporal change, rounded down, as the potential value declines more slowly).
     */
    public static int getAgeCharacteristicPotentialValueChange(List<LevelUp> levelUps, CharacteristicAbbreviation abbreviation) {
        int modification = 0;
        for (final LevelUp levelUp : levelUps) {
            for (final AgeModification ageModification : levelUp.getAgeModifications()) {
                if (ageModification.getCharacteristicAbbreviation() == abbreviation) {
                    modification += ageModification.getCharacteristicModification() / 3;
                }
            }
        }
        return modification;
    }
}
