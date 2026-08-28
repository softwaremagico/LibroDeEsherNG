package com.softwaremagico.librodeesher.age;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.level.LevelUp;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/** Verifies {@link AgeRules}. */
@Test(groups = "age")
public class AgeRulesTest {

    @Test
    public void randomAgeSelectedCharacteristicIsAlwaysPhysicalOrMental() {
        for (int i = 0; i < 50; i++) {
            final CharacteristicAbbreviation abbreviation = AgeRules.getRandomAgeSelectedCharacteristic();
            Assert.assertTrue(Characteristics.PHYSICAL_CHARACTERISTICS.contains(abbreviation)
                    || Characteristics.MENTAL_CHARACTERISTICS.contains(abbreviation));
        }
    }

    @Test
    public void ageModificationIsFlatDuringYouthAdulthoodAndMiddleAge() {
        Assert.assertEquals(AgeRules.getAgeModification(10, 100), 100);
        Assert.assertEquals(AgeRules.getAgeModification(74, 100), 100);
    }

    @Test
    public void ageModificationWorsensAsPeriodOfLifeApproachesTheExpectedLifespan() {
        Assert.assertEquals(AgeRules.getAgeModification(78, 100), -5);
        Assert.assertEquals(AgeRules.getAgeModification(90, 100), -30);
        Assert.assertEquals(AgeRules.getAgeModification(99, 100), -50);
        Assert.assertEquals(AgeRules.getAgeModification(100, 100), -75);
    }

    @Test
    public void temporalValueChangeSumsEveryLevelsAgeModifications() {
        final CharacteristicAbbreviation abbreviation = CharacteristicAbbreviation.STRENGTH;
        final LevelUp firstLevel = new LevelUp();
        firstLevel.addAgeModification(forcedAgeModification(abbreviation, 80, 3));
        final LevelUp secondLevel = new LevelUp();
        secondLevel.addAgeModification(forcedAgeModification(abbreviation, 81, 3));

        final int perLevelSum = AgeRules.getAgeCharacteristicTemporalValueChange(firstLevel, abbreviation)
                + AgeRules.getAgeCharacteristicTemporalValueChange(secondLevel, abbreviation);
        final int total = AgeRules.getAgeCharacteristicTemporalValueChange(List.of(firstLevel, secondLevel), abbreviation);
        Assert.assertEquals(total, perLevelSum);
        Assert.assertTrue(total > 0);
    }

    @Test
    public void potentialValueChangeIsAThirdOfTheTemporalChange() {
        final LevelUp levelUp = new LevelUp();
        final AgeModification modification = new AgeModification(80, 3);
        levelUp.addAgeModification(modification);

        final int temporalChange = AgeRules.getAgeCharacteristicTemporalValueChange(levelUp,
                modification.getCharacteristicAbbreviation());
        final int potentialChange = AgeRules.getAgeCharacteristicPotentialValueChange(List.of(levelUp),
                modification.getCharacteristicAbbreviation());
        Assert.assertEquals(potentialChange, temporalChange / 3);
    }

    /** Re-rolls an {@link AgeModification} until it happens to target {@code abbreviation} (there is no setter). */
    private static AgeModification forcedAgeModification(CharacteristicAbbreviation abbreviation, int age, int raceType) {
        AgeModification modification;
        do {
            modification = new AgeModification(age, raceType);
        } while (modification.getCharacteristicAbbreviation() != abbreviation);
        return modification;
    }
}
