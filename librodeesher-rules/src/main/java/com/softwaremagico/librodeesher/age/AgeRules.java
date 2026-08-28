package com.softwaremagico.librodeesher.age;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.dice.Dice;
import com.softwaremagico.librodeesher.level.LevelUp;

import java.util.List;

/**
 * Age-related characteristic decline rules.
 *
 * <p>The legacy {@code AgeRules.increaseAge(CharacterPlayer)}/{@code hasCharacteristicDecrease(
 * CharacterPlayer)} orchestration methods (which loop from the character's current age to its final
 * age, mutating the character's current level) are intentionally not ported yet: they need a
 * {@code CharacterPlayer} equivalent, which does not exist in this codebase yet. The pure formulas
 * they relied on are ported below, taking their inputs directly instead of a character object, so
 * they are usable (and testable) independently; the per-year orchestration loop will be added back
 * once the character layer exists.</p>
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
        final int value = (int) (Math.random() * 100 + 1) + constitutionTotalBonus * 2 + selfDisciplineTotalBonus
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
        if ((int) (Math.random() * 100 + 1) < PROBABILITY_PHYSICAL_CHARACTERISTIC) {
            return Characteristics.PHYSICAL_CHARACTERISTICS.get(Dice.getRoll(Characteristics.PHYSICAL_CHARACTERISTICS.size()) - 1);
        }
        return Characteristics.MENTAL_CHARACTERISTICS.get(Dice.getRoll(Characteristics.MENTAL_CHARACTERISTICS.size()) - 1);
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
