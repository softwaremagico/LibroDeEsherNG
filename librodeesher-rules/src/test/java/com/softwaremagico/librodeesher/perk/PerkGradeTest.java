package com.softwaremagico.librodeesher.perk;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link PerkGrade#getBackgroundCost}. */
@Test(groups = "perk")
public class PerkGradeTest {

    @Test
    public void baseCostIncreasesWithGrade() {
        Assert.assertEquals(PerkGrade.MINIMUM.getBackgroundCost(null, false), 2);
        Assert.assertEquals(PerkGrade.MINOR.getBackgroundCost(null, false), 3);
        Assert.assertEquals(PerkGrade.MAJOR.getBackgroundCost(null, false), 4);
        Assert.assertEquals(PerkGrade.MAXIMUM.getBackgroundCost(null, false), 5);
    }

    @Test
    public void matchingWeaknessGradeDiscountsTwoPoints() {
        Assert.assertEquals(PerkGrade.MAJOR.getBackgroundCost(PerkGrade.MAJOR, false), 2);
    }

    @Test
    public void oneGradeBelowWeaknessDiscountsOnePoint() {
        Assert.assertEquals(PerkGrade.MAJOR.getBackgroundCost(PerkGrade.MINOR, false), 3);
    }

    @Test
    public void twoGradesBelowWeaknessDiscountsNothing() {
        Assert.assertEquals(PerkGrade.MAXIMUM.getBackgroundCost(PerkGrade.MINIMUM, false), 5);
    }

    @Test
    public void randomChoiceDiscountsOnePointDownToAMinimumOfOne() {
        Assert.assertEquals(PerkGrade.MINIMUM.getBackgroundCost(null, true), 1);
        Assert.assertEquals(PerkGrade.MAJOR.getBackgroundCost(PerkGrade.MAJOR, true), 1);
    }
}
