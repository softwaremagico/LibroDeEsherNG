package com.softwaremagico.librodeesher.level;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link Experience}. */
@Test(groups = "level")
public class ExperienceTest {

    @Test
    public void firstLevelsCostTenThousandPerLevel() {
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(1), 10000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(2), 20000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(3), 30000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(4), 40000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(5), 50000);
    }

    @Test
    public void middleBandsRaiseThePerLevelCost() {
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(6), 70000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(9), 130000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(10), 150000);
    }

    @Test
    public void laterBandsKeepSteppingUp() {
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(11), 180000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(15), 300000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(16), 340000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(19), 460000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(20), 500000);
    }

    @Test
    public void twentyFirstLevelOnwardsUsesTheLargestBand() {
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(21), 550000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(25), 750000);
    }

    @Test
    public void requirementIsNeverDecreasingAcrossLevels() {
        for (int level = 2; level < 50; level++) {
            Assert.assertTrue(Experience.getMinimumExperienceForLevel(level)
                    >= Experience.getMinimumExperienceForLevel(level - 1));
        }
    }
}