package com.softwaremagico.librodeesher.characteristic;

import com.softwaremagico.librodeesher.dice.Roll;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link Characteristic}'s development formulas. */
@Test(groups = "characteristic")
public class CharacteristicTest {

    @Test
    public void doublesBelowSixAreADecrease() {
        final Roll roll = new Roll();
        roll.setFirstDice(3);
        roll.setSecondDice(3);
        Assert.assertEquals(Characteristic.getCharacteristicUpgrade(50, 90, roll), Integer.valueOf(-3));
    }

    @Test
    public void highDoublesDoubleTheirValueCappedByRemainingRoom() {
        final Roll roll = new Roll();
        roll.setFirstDice(8);
        roll.setSecondDice(8);
        Assert.assertEquals(Characteristic.getCharacteristicUpgrade(85, 90, roll), Integer.valueOf(5));
        Assert.assertEquals(Characteristic.getCharacteristicUpgrade(50, 90, roll), Integer.valueOf(16));
    }

    @Test
    public void closeToPotentialUsesTheLowerDie() {
        final Roll roll = new Roll();
        roll.setFirstDice(4);
        roll.setSecondDice(7);
        Assert.assertEquals(Characteristic.getCharacteristicUpgrade(85, 90, roll), Integer.valueOf(4));
    }

    @Test
    public void midRangeUsesTheHigherDie() {
        final Roll roll = new Roll();
        roll.setFirstDice(4);
        roll.setSecondDice(7);
        Assert.assertEquals(Characteristic.getCharacteristicUpgrade(75, 90, roll), Integer.valueOf(7));
    }

    @Test
    public void farFromPotentialSumsBothDice() {
        final Roll roll = new Roll();
        roll.setFirstDice(4);
        roll.setSecondDice(7);
        Assert.assertEquals(Characteristic.getCharacteristicUpgrade(50, 90, roll), Integer.valueOf(11));
    }

    @Test
    public void zeroTemporalOrPotentialValueIsCompatibilityNoOp() {
        final Roll roll = new Roll();
        Assert.assertEquals(Characteristic.getCharacteristicUpgrade(0, 90, roll), Integer.valueOf(0));
        Assert.assertEquals(Characteristic.getCharacteristicUpgrade(50, 0, roll), Integer.valueOf(0));
    }

    @Test
    public void costIsLinearBelow91AndQuadraticAbove() {
        Assert.assertEquals(Characteristic.getTemporalCost(90), Integer.valueOf(90));
        Assert.assertEquals(Characteristic.getTemporalCost(91), Integer.valueOf(91));
        Assert.assertEquals(Characteristic.getTemporalCost(100), Integer.valueOf(190));
    }

    @Test
    public void twoCharacteristicsWithTheSameAbbreviationAreEqual() {
        final Characteristic first = new Characteristic(CharacteristicAbbreviation.AGILITY);
        final Characteristic second = new Characteristic(CharacteristicAbbreviation.AGILITY);
        Assert.assertEquals(first, second);
        Assert.assertEquals(first.hashCode(), second.hashCode());
    }
}
