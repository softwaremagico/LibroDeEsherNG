package com.softwaremagico.librodeesher.characteristic;

import com.softwaremagico.librodeesher.dice.Roll;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies the fixed {@link Characteristics} tables and characteristic groupings. */
@Test(groups = "characteristic")
public class CharacteristicsTest {

    @Test
    public void everyCharacteristicIsRegisteredExactlyOnce() {
        Assert.assertEquals(Characteristics.getCharacteristics().size(), 10);
    }

    @Test
    public void physicalAndMentalGroupsCoverDifferentCharacteristics() {
        Assert.assertTrue(Characteristics.PHYSICAL_CHARACTERISTICS.contains(CharacteristicAbbreviation.STRENGTH));
        Assert.assertTrue(Characteristics.MENTAL_CHARACTERISTICS.contains(CharacteristicAbbreviation.REASONING));
        Assert.assertFalse(Characteristics.PHYSICAL_CHARACTERISTICS.contains(CharacteristicAbbreviation.REASONING));
        Assert.assertFalse(Characteristics.MENTAL_CHARACTERISTICS.contains(CharacteristicAbbreviation.STRENGTH));
    }

    @Test
    public void appearanceIsValidButExcludedFromCharacteristicChecks() {
        Assert.assertTrue(Characteristics.isCharacteristicValid("Fu"));
        Assert.assertFalse(Characteristics.isCharacteristicValid("Ap"));
        Assert.assertFalse(Characteristics.isCharacteristicValid("Xx"));
    }

    @Test
    public void getCharacteristicFromAbbreviationFindsTheSingleton() {
        final Characteristic strength = Characteristics.getCharacteristicFromAbbreviation(CharacteristicAbbreviation.STRENGTH);
        Assert.assertNotNull(strength);
        Assert.assertEquals(strength.getAbbreviation(), CharacteristicAbbreviation.STRENGTH);
    }

    @Test
    public void temporalBonusMatchesTheStandardTable() {
        Assert.assertEquals(Characteristics.getTemporalBonus(31), Integer.valueOf(0));
        Assert.assertEquals(Characteristics.getTemporalBonus(30), Integer.valueOf(-1));
        Assert.assertEquals(Characteristics.getTemporalBonus(70), Integer.valueOf(1));
        Assert.assertEquals(Characteristics.getTemporalBonus(100), Integer.valueOf(10));
        Assert.assertEquals(Characteristics.getTemporalBonus(102), Integer.valueOf(14));
        Assert.assertEquals(Characteristics.getTemporalBonus(1), Integer.valueOf(-10));
    }

    @Test
    public void potentialIsNeverBelowTheTemporalValueAtTheTopOfTheScale() {
        final Integer potential = Characteristics.getPotential(100);
        Assert.assertTrue(potential >= 100);
    }

    @Test
    public void setTemporalIncreaseUsesTheSameThresholdsAsCharacteristicUpgrade() {
        final Roll equalHighDice = new Roll();
        equalHighDice.setFirstDice(7);
        equalHighDice.setSecondDice(7);
        Assert.assertEquals(Characteristics.setTemporalIncrease(50, 90, equalHighDice), Integer.valueOf(14));

        final Roll equalLowDice = new Roll();
        equalLowDice.setFirstDice(3);
        equalLowDice.setSecondDice(3);
        Assert.assertEquals(Characteristics.setTemporalIncrease(50, 90, equalLowDice), Integer.valueOf(-3));

        final Roll distinctDice = new Roll();
        distinctDice.setFirstDice(4);
        distinctDice.setSecondDice(7);
        Assert.assertEquals(Characteristics.setTemporalIncrease(85, 90, distinctDice), Integer.valueOf(4));
    }
}
