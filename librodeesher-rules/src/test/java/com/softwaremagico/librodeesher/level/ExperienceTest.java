package com.softwaremagico.librodeesher.level;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies Rolemaster's legacy level experience thresholds. */
@Test(groups = "level")
public class ExperienceTest {

    @Test
    public void minimumExperienceProgressesAcrossEveryCostBand() {
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(1), 10_000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(5), 50_000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(10), 150_000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(15), 300_000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(20), 500_000);
        Assert.assertEquals(Experience.getMinimumExperienceForLevel(21), 550_000);
    }
}
