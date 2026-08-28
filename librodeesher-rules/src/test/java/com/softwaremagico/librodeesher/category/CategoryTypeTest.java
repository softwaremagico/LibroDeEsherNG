package com.softwaremagico.librodeesher.category;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies the fixed rank-bonus/rank-cost progression tables per {@link CategoryType}. */
@Test(groups = "category")
public class CategoryTypeTest {

    @Test
    public void standardSkillRankBonusMatchesTheClassicTable() {
        Assert.assertEquals(CategoryType.STANDARD.getSkillRankBonus(0), Integer.valueOf(-15));
        Assert.assertEquals(CategoryType.STANDARD.getSkillRankBonus(1), Integer.valueOf(3));
        Assert.assertEquals(CategoryType.STANDARD.getSkillRankBonus(10), Integer.valueOf(30));
        Assert.assertEquals(CategoryType.STANDARD.getSkillRankBonus(20), Integer.valueOf(50));
        Assert.assertEquals(CategoryType.STANDARD.getSkillRankBonus(30), Integer.valueOf(60));
        Assert.assertEquals(CategoryType.STANDARD.getSkillRankBonus(32), Integer.valueOf(61));
    }

    @Test
    public void standardCategoryRankBonusMatchesTheClassicTable() {
        Assert.assertEquals(CategoryType.STANDARD.getCategoryRankBonus(0), Integer.valueOf(-15));
        Assert.assertEquals(CategoryType.STANDARD.getCategoryRankBonus(10), Integer.valueOf(20));
        Assert.assertEquals(CategoryType.STANDARD.getCategoryRankBonus(20), Integer.valueOf(30));
        Assert.assertEquals(CategoryType.STANDARD.getCategoryRankBonus(30), Integer.valueOf(35));
        Assert.assertEquals(CategoryType.STANDARD.getCategoryRankBonus(40), Integer.valueOf(35));
    }

    @Test
    public void combinedSkillRankBonusMatchesTheClassicTable() {
        Assert.assertEquals(CategoryType.COMBINED.getSkillRankBonus(0), Integer.valueOf(-30));
        Assert.assertEquals(CategoryType.COMBINED.getSkillRankBonus(10), Integer.valueOf(50));
        Assert.assertEquals(CategoryType.COMBINED.getSkillRankBonus(20), Integer.valueOf(80));
        Assert.assertEquals(CategoryType.COMBINED.getSkillRankBonus(30), Integer.valueOf(95));
    }

    @Test
    public void limitedSkillRankBonusCapsAtTwentyFive() {
        Assert.assertEquals(CategoryType.LIMITED.getSkillRankBonus(0), Integer.valueOf(0));
        Assert.assertEquals(CategoryType.LIMITED.getSkillRankBonus(20), Integer.valueOf(20));
        Assert.assertEquals(CategoryType.LIMITED.getSkillRankBonus(30), Integer.valueOf(25));
        Assert.assertEquals(CategoryType.LIMITED.getSkillRankBonus(40), Integer.valueOf(25));
    }

    @Test
    public void specialSkillRankBonusGrowsFasterThanStandard() {
        Assert.assertEquals(CategoryType.SPECIAL.getSkillRankBonus(10), Integer.valueOf(60));
        Assert.assertEquals(CategoryType.SPECIAL.getSkillRankBonus(31), Integer.valueOf(153));
    }

    @Test
    public void ppdAndPdGrantNoRankScalingBonus() {
        Assert.assertEquals(CategoryType.PPD.getSkillRankBonus(20), Integer.valueOf(0));
        Assert.assertEquals(CategoryType.PD.getSkillRankBonus(20), Integer.valueOf(0));
    }

    @Test
    public void onlyPdHasAFixedBonus() {
        Assert.assertEquals(CategoryType.PD.getFixedBonus(), Integer.valueOf(10));
        Assert.assertEquals(CategoryType.STANDARD.getFixedBonus(), Integer.valueOf(0));
        Assert.assertEquals(CategoryType.COMBINED.getFixedBonus(), Integer.valueOf(0));
    }

    @Test
    public void onlyStandardCategoriesTrackIndividualSkillRanks() {
        Assert.assertTrue(CategoryType.STANDARD.hasRanks());
        Assert.assertFalse(CategoryType.COMBINED.hasRanks());
        Assert.assertFalse(CategoryType.LIMITED.hasRanks());
        Assert.assertFalse(CategoryType.SPECIAL.hasRanks());
        Assert.assertFalse(CategoryType.PPD.hasRanks());
        Assert.assertFalse(CategoryType.PD.hasRanks());
    }
}
