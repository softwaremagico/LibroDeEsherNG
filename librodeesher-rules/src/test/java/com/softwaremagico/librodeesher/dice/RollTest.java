package com.softwaremagico.librodeesher.dice;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies explicit dice results do not draw from the dice generator. */
@Test(groups = "dice")
public class RollTest {

    @Test
    public void ofDoesNotConsumeDiceRandomness() {
        Dice.setRandomSeed(71L);
        final int expectedNextRoll = Dice.getRoll(10);

        Dice.setRandomSeed(71L);
        final Roll explicitRoll = Roll.of(4, 6);
        final int actualNextRoll = Dice.getRoll(10);

        Assert.assertEquals(explicitRoll.getFirstDice(), Integer.valueOf(4));
        Assert.assertEquals(explicitRoll.getSecondDice(), Integer.valueOf(6));
        Assert.assertEquals(actualNextRoll, expectedNextRoll);
    }
}
