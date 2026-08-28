package com.softwaremagico.librodeesher.level;

import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.dice.Roll;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/** Verifies {@link LevelUp}'s per-level bookkeeping. */
@Test(groups = "level")
public class LevelUpTest {

    @Test
    public void categoryAndSkillRanksAreRemovedWhenSetToZero() {
        final LevelUp levelUp = new LevelUp();
        levelUp.setCategoryRanks("outdoorEnvironment", 3);
        Assert.assertEquals(levelUp.getCategoryRanks("outdoorEnvironment"), Integer.valueOf(3));
        Assert.assertEquals(levelUp.getCategoriesWithRanks(), List.of("outdoorEnvironment"));

        levelUp.setCategoryRanks("outdoorEnvironment", 0);
        Assert.assertEquals(levelUp.getCategoryRanks("outdoorEnvironment"), Integer.valueOf(0));
        Assert.assertTrue(levelUp.getCategoriesWithRanks().isEmpty());
    }

    @Test
    public void spellSkillsAreTrackedSeparatelyFromRegularSkills() {
        final LevelUp levelUp = new LevelUp();
        levelUp.setSkillRanks("regularSkill", 2, false);
        levelUp.setSkillRanks("fireBall", 2, true);

        Assert.assertTrue(levelUp.getSpellsUpdated().contains("fireBall"));
        Assert.assertFalse(levelUp.getSpellsUpdated().contains("regularSkill"));

        levelUp.setSkillRanks("fireBall", 0, true);
        Assert.assertFalse(levelUp.getSpellsUpdated().contains("fireBall"));
    }

    @Test
    public void spellRankMultiplierIncreasesAfterFiveAndTenLists() {
        final LevelUp levelUp = new LevelUp();
        for (int i = 0; i < 5; i++) {
            levelUp.setSkillRanks("spell" + i, 1, true);
        }
        Assert.assertEquals(levelUp.getSpellRankMultiplier("spell0"), Integer.valueOf(1));
        Assert.assertEquals(levelUp.getSpellRankMultiplier("newSpell"), Integer.valueOf(2));

        for (int i = 5; i < 10; i++) {
            levelUp.setSkillRanks("spell" + i, 1, true);
        }
        Assert.assertEquals(levelUp.getSpellRankMultiplier("anotherNewSpell"), Integer.valueOf(4));
    }

    @Test
    public void trainingsCanBeAddedAndRemoved() {
        final LevelUp levelUp = new LevelUp();
        levelUp.addTraining("soldier");
        Assert.assertEquals(levelUp.getTrainings(), List.of("soldier"));
        levelUp.removeTraining("soldier");
        Assert.assertTrue(levelUp.getTrainings().isEmpty());
    }

    @Test
    public void skillSpecializationsAreFilteredAgainstASkillsOwnList() {
        final LevelUp levelUp = new LevelUp();
        levelUp.addSkillSpecialization("longSword");
        final List<String> allSpecialities = List.of("longSword", "shortSword");

        Assert.assertEquals(levelUp.getSkillSpecializations(allSpecialities), List.of("longSword"));
        Assert.assertEquals(levelUp.getRanksSpentInSpecializations(allSpecialities), Integer.valueOf(1));
    }

    @Test
    public void characteristicUpdatesCanBeAddedAndUpdated() {
        final LevelUp levelUp = new LevelUp();
        levelUp.addCharacteristicUpdate(CharacteristicAbbreviation.AGILITY, 50, 90, new Roll());
        levelUp.updateCharacteristicRoll(CharacteristicAbbreviation.AGILITY, 55, 90);

        Assert.assertEquals(levelUp.getCharacteristicUpdate(CharacteristicAbbreviation.AGILITY).getCharacteristicTemporalValue(),
                Integer.valueOf(55));
    }

    @Test
    public void ageModificationsAccumulate() {
        final LevelUp levelUp = new LevelUp();
        levelUp.addAgeModification(new AgeModification(40, 3));
        Assert.assertEquals(levelUp.getAgeModifications().size(), 1);
    }
}
