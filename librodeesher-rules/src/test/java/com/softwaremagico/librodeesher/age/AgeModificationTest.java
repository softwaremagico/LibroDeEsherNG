package com.softwaremagico.librodeesher.age;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link AgeModification}'s characteristic decrease formula per race type. */
@Test(groups = "age")
public class AgeModificationTest {

    @Test
    public void selectedCharacteristicIsAlwaysPhysicalOrMental() {
        final AgeModification modification = new AgeModification(80, 3);
        Assert.assertTrue(Characteristics.PHYSICAL_CHARACTERISTICS.contains(modification.getCharacteristicAbbreviation())
                || Characteristics.MENTAL_CHARACTERISTICS.contains(modification.getCharacteristicAbbreviation()));
    }

    @Test
    public void unknownRaceTypeFallsBackToTheDefaultFormula() {
        for (int i = 0; i < 20; i++) {
            final AgeModification modification = new AgeModification(80, 99);
            Assert.assertTrue(modification.getCharacteristicModification() >= 0);
        }
    }

    @Test
    public void raceTypeFiveCanCombineBothDice() {
        boolean sawMoreThanSingleDie = false;
        for (int i = 0; i < 100 && !sawMoreThanSingleDie; i++) {
            final AgeModification modification = new AgeModification(80, 5);
            if (modification.getCharacteristicModification() > 10) {
                sawMoreThanSingleDie = true;
            }
        }
        Assert.assertTrue(sawMoreThanSingleDie, "Expected at least one roll combining both dice over 100 tries.");
    }

    @Test
    public void toStringIncludesTheCharacteristicAndItsModification() {
        final AgeModification modification = new AgeModification(80, 3);
        Assert.assertEquals(modification.toString(),
                modification.getCharacteristicAbbreviation() + " " + modification.getCharacteristicModification());
    }
}
