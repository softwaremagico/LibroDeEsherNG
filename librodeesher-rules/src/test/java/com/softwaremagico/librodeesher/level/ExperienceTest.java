package com.softwaremagico.librodeesher.level;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link Experience}. */
@Test(groups = "level")
public class ExperienceTest {

    @Test
    public void firstLevelsCostTenThousandPerLevel() {
        Assert.assertEquals(Experience.getMinExperienceForLevel(1), 10000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(2), 20000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(3), 30000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(4), 40000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(5), 50000);
    }

    @Test
    public void middleBandsRaiseThePerLevelCost() {
        Assert.assertEquals(Experience.getMinExperienceForLevel(6), 70000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(9), 130000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(10), 150000);
    }

    @Test
    public void laterBandsKeepSteppingUp() {
        Assert.assertEquals(Experience.getMinExperienceForLevel(11), 180000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(15), 300000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(16), 340000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(19), 460000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(20), 500000);
    }

    @Test
    public void twentyFirstLevelOnwardsUsesTheLargestBand() {
        Assert.assertEquals(Experience.getMinExperienceForLevel(21), 550000);
        Assert.assertEquals(Experience.getMinExperienceForLevel(25), 750000);
    }

    @Test
    public void requirementIsNeverDecreasingAcrossLevels() {
        for (int level = 2; level < 50; level++) {
            Assert.assertTrue(Experience.getMinExperienceForLevel(level)
                    >= Experience.getMinExperienceForLevel(level - 1));
        }
    }
}