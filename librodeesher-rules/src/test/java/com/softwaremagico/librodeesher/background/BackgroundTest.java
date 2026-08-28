package com.softwaremagico.librodeesher.background;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.dice.Roll;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link Background}'s point bookkeeping. */
@Test(groups = "background")
public class BackgroundTest {

    @Test
    public void skillAndCategoryPointsGrantTheirFixedBonus() {
        final Background background = new Background();
        Assert.assertEquals(background.getSkillBonus("stalking"), Integer.valueOf(0));

        background.setSkillPoint("stalking", true);
        Assert.assertTrue(background.isSkillPointSelected("stalking"));
        Assert.assertEquals(background.getSkillBonus("stalking"), Integer.valueOf(10));

        background.setCategoryPoint("outdoorEnvironment", true);
        Assert.assertTrue(background.isCategoryPointSelected("outdoorEnvironment"));
        Assert.assertEquals(background.getCategoryBonus("outdoorEnvironment"), Integer.valueOf(5));

        background.setSkillPoint("stalking", false);
        Assert.assertFalse(background.isSkillPointSelected("stalking"));
    }

    @Test
    public void selectingTheSamePointTwiceDoesNotDuplicateIt() {
        final Background background = new Background();
        background.setSkillPoint("stalking", true);
        background.setSkillPoint("stalking", true);
        Assert.assertEquals(background.getSkillIds().size(), 1);
    }

    @Test
    public void spentPointsAddUpSkillsCategoriesCharacteristicsAndLanguages() {
        final Background background = new Background();
        background.setSkillPoint("stalking", true);
        background.setCategoryPoint("outdoorEnvironment", true);
        background.addCharacteristicUpdate(CharacteristicAbbreviation.AGILITY, 50, 90, new Roll());
        background.setHistoryLanguageRank("commonSpeech", 20);

        // 1 skill + 1 category + 1 characteristic roll + ceil(20/20) language point.
        Assert.assertEquals(background.getSpentBackgroundPoints(), Integer.valueOf(4));
    }

    @Test
    public void languagePointCostRoundsUpPer20Ranks() {
        final Background background = new Background();
        background.setHistoryLanguageRank("commonSpeech", 21);
        Assert.assertEquals(background.getLanguagesTotalRanksAdded(), 21);
        Assert.assertEquals(background.getLanguagesPointCost(), 2);
    }

    @Test
    public void characteristicUpdatesAreFilterableByAbbreviation() {
        final Background background = new Background();
        background.addCharacteristicUpdate(CharacteristicAbbreviation.AGILITY, 50, 90, new Roll());
        background.addCharacteristicUpdate(CharacteristicAbbreviation.STRENGTH, 40, 80, new Roll());

        Assert.assertEquals(background.getCharacteristicUpdates(CharacteristicAbbreviation.AGILITY).size(), 1);
        Assert.assertEquals(background.getCharacteristicUpdates().size(), 2);
    }
}
