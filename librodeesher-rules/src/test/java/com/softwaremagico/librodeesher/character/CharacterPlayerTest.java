package com.softwaremagico.librodeesher.character;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryType;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.decision.InvalidDecisionException;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.training.ChoiceGroup;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Verifies {@link CharacterPlayer}'s identity, characteristic and level bookkeeping. */
@Test(groups = "character")
public class CharacterPlayerTest {

    @Test
    public void everyCharacteristicStartsAtTheInitialValue() {
        final CharacterPlayer character = new CharacterPlayer();
        for (final CharacteristicAbbreviation abbreviation : CharacteristicAbbreviation.values()) {
            if (abbreviation == CharacteristicAbbreviation.NONE || abbreviation == CharacteristicAbbreviation.REALM_OF_MAGIC) {
                continue;
            }
            Assert.assertEquals(character.getCharacteristicTemporalValue(abbreviation),
                    Integer.valueOf(Characteristics.INITIAL_CHARACTERISTIC_VALUE));
        }
    }

    @Test
    public void startsAtLevelOneWithOneEmptyLevelUp() {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.getLevel(), 1);
        Assert.assertNotNull(character.getCurrentLevel());
        Assert.assertSame(character.getCurrentLevel(), character.getLevels().get(0));
    }

    @Test
    public void increaseLevelAddsANewCurrentLevel() {
        final CharacterPlayer character = new CharacterPlayer();
        final LevelUp firstLevel = character.getCurrentLevel();
        final LevelUp secondLevel = character.increaseLevel();

        Assert.assertEquals(character.getLevel(), 2);
        Assert.assertSame(character.getCurrentLevel(), secondLevel);
        Assert.assertNotSame(secondLevel, firstLevel);
    }

    @Test
    public void temporalBonusIsComputedFromTheStandardTable() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCharacteristicTemporalValue(CharacteristicAbbreviation.STRENGTH, 90);
        Assert.assertEquals(character.getCharacteristicTemporalBonus(CharacteristicAbbreviation.STRENGTH),
                Characteristics.getTemporalBonus(90));
    }

    @Test
    public void raceBonusIsZeroWithoutARaceSelected() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.getCharacteristicRaceBonus(CharacteristicAbbreviation.STRENGTH), Integer.valueOf(0));
        Assert.assertNull(character.getRace());
    }

    @Test
    public void totalBonusCombinesTemporalAndRaceBonuses() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("grayOrc");
        character.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 90);

        final Integer temporalBonus = character.getCharacteristicTemporalBonus(CharacteristicAbbreviation.CONSTITUTION);
        final Integer raceBonus = character.getCharacteristicRaceBonus(CharacteristicAbbreviation.CONSTITUTION);
        Assert.assertEquals(raceBonus, Integer.valueOf(4));
        Assert.assertEquals(character.getCharacteristicTotalBonus(CharacteristicAbbreviation.CONSTITUTION),
                Integer.valueOf(temporalBonus + raceBonus));
    }

    @Test
    public void unselectedRaceCultureAndProfessionResolveToNull() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertNull(character.getRace());
        Assert.assertNull(character.getCulture());
        Assert.assertNull(character.getProfession());
    }

    @Test
    public void selectedRaceCultureAndProfessionResolveThroughTheCatalog() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("grayOrc");
        Assert.assertEquals(character.getRace().getId(), "grayOrc");
    }

    @Test
    public void rollingThePotentialValueUsesTheStandardTable() {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 100);
        final Integer potential = character.rollCharacteristicPotentialValue(CharacteristicAbbreviation.AGILITY);
        Assert.assertTrue(potential >= 100);
        Assert.assertEquals(character.getCharacteristicPotentialValue(CharacteristicAbbreviation.AGILITY), potential);
    }

    @Test
    public void appearanceTotalCombinesTheRollWithPresence() {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCharacteristicPotentialValue(CharacteristicAbbreviation.PRESENCE, 90);
        Assert.assertEquals(character.getAppearanceTotal(), character.getAppearance().getTotal(90));
    }

    @Test
    public void categoryAndSkillRanksAccumulateAcrossEveryLevel() {
        final CharacterPlayer character = new CharacterPlayer();
        character.getCurrentLevel().setCategoryRanks("outdoorEnvironment", 3);
        character.getCurrentLevel().setSkillRanks("tracking", 2, false);
        character.increaseLevel();
        character.getCurrentLevel().setCategoryRanks("outdoorEnvironment", 1);
        character.getCurrentLevel().setSkillRanks("tracking", 1, false);

        Assert.assertEquals(character.getCategoryTotalRanks("outdoorEnvironment"), Integer.valueOf(4));
        Assert.assertEquals(character.getSkillTotalRanks("tracking"), Integer.valueOf(3));
        Assert.assertEquals(character.getCategoryTotalRanks("unrelatedCategory"), Integer.valueOf(0));
    }

    @Test
    public void categoryDevelopmentBonusUsesTheCategoryTypeProgressionTable() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.getCurrentLevel().setCategoryRanks("weaponsEdged", 10);

        final Category category = new Category("weaponsEdged");
        category.setType(CategoryType.STANDARD);

        Assert.assertEquals(character.getCategoryDevelopmentBonus(category), CategoryType.STANDARD.getCategoryRankBonus(10));
    }

    @Test
    public void physicalDevelopmentCategoryGrantsItsFixedBonusRegardlessOfRanks() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final Category category = new Category("physicalDevelopment");
        category.setType(CategoryType.PD);

        Assert.assertEquals(character.getCategoryDevelopmentBonus(category), Integer.valueOf(10));
    }

    @Test
    public void skillDevelopmentBonusUsesItsCategorysProgressionTable() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.getCurrentLevel().setSkillRanks("tracking", 10, false);

        final Category category = new Category("outdoorEnvironment");
        category.setType(CategoryType.STANDARD);

        Assert.assertEquals(character.getSkillDevelopmentBonus(category, "tracking"), CategoryType.STANDARD.getSkillRankBonus(10));
    }

    @Test
    public void professionBonusIsZeroWithoutAProfessionSelected() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.getProfessionBonus("loreArcane"), Integer.valueOf(0));
        Assert.assertFalse(character.isPreferredCharacteristic(CharacteristicAbbreviation.EMPATHY));
    }

    @Test
    public void professionBonusIsAddedToCategoryAndSkillDevelopmentBonus() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setProfessionId("wizard");
        Assert.assertEquals(character.getProfessionBonus("loreArcane"), Integer.valueOf(10));
        Assert.assertEquals(character.getProfessionBonus("unknownCategoryOrSkill"), Integer.valueOf(0));

        final Category category = new Category("loreArcane");
        category.setType(CategoryType.STANDARD);
        Assert.assertEquals(character.getCategoryDevelopmentBonus(category),
                CategoryType.STANDARD.getCategoryRankBonus(0) + 10);
        Assert.assertEquals(character.getSkillDevelopmentBonus(category, "loreArcane"),
                CategoryType.STANDARD.getSkillRankBonus(0) + 10);
    }

    @Test
    public void preferredCharacteristicIsThePrimaryOrSecondaryOnly() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setProfessionId("wizard");

        Assert.assertTrue(character.isPreferredCharacteristic(CharacteristicAbbreviation.EMPATHY));
        Assert.assertTrue(character.isPreferredCharacteristic(CharacteristicAbbreviation.REASONING));
        Assert.assertFalse(character.isPreferredCharacteristic(CharacteristicAbbreviation.CONSTITUTION));
    }

    @Test
    public void applyingAFixedCategoryGrantAddsItsRanksAndNestedSkillRanks() {
        final CharacterPlayer character = new CharacterPlayer();
        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(List.of("outdoorEnvironment"));
        grant.setRanksGranted(2);
        grant.getSkills().add(new TrainingSkillGrant(List.of("tracking"), 2));

        character.applyCategoryGrant("training:scout:category:0", grant, null, null);

        Assert.assertEquals(character.getCategoryTotalRanks("outdoorEnvironment"), Integer.valueOf(2));
        Assert.assertEquals(character.getSkillTotalRanks("tracking"), Integer.valueOf(2));
        Assert.assertEquals(character.getDecisions().getSelectedOption("training:scout:category:0"), "outdoorEnvironment");
        Assert.assertEquals(character.getDecisions().getSelectedOption("training:scout:category:0:skill:0"), "tracking");
    }

    @Test
    public void applyingAChoiceCategoryGrantRequiresAndRecordsTheSelection() {
        final CharacterPlayer character = new CharacterPlayer();
        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(List.of("weaponsTwoHanded", "weaponsEdged"));
        grant.setRanksGranted(1);

        character.applyCategoryGrant("training:soldier:category:1", grant, "weaponsEdged", null);

        Assert.assertEquals(character.getCategoryTotalRanks("weaponsEdged"), Integer.valueOf(1));
        Assert.assertEquals(character.getCategoryTotalRanks("weaponsTwoHanded"), Integer.valueOf(0));
        Assert.assertEquals(character.getDecisions().getSelectedOption("training:soldier:category:1"), "weaponsEdged");
    }

    @Test(expectedExceptions = InvalidDecisionException.class)
    public void applyingAChoiceCategoryGrantWithAnInvalidSelectionFails() {
        final CharacterPlayer character = new CharacterPlayer();
        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(List.of("weaponsTwoHanded", "weaponsEdged"));
        grant.setRanksGranted(1);

        character.applyCategoryGrant("training:soldier:category:1", grant, "weaponsMissile", null);
    }

    @Test
    public void applyingTheSameGrantTwiceReusesTheFirstDecisionAndAccumulatesRanks() {
        final CharacterPlayer character = new CharacterPlayer();
        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(List.of("weaponsTwoHanded", "weaponsEdged"));
        grant.setRanksGranted(1);

        character.applyCategoryGrant("training:soldier:category:1", grant, "weaponsEdged", null);
        // A different (and otherwise invalid, since it wasn't offered before) id is ignored because
        // the decision was already made.
        character.applyCategoryGrant("training:soldier:category:1", grant, "somethingElse", null);

        Assert.assertEquals(character.getCategoryTotalRanks("weaponsEdged"), Integer.valueOf(2));
        Assert.assertEquals(character.getDecisions().getSelectedOption("training:soldier:category:1"), "weaponsEdged");
    }

    @Test
    public void applyTrainingCategoriesAppliesEveryGrantOfARealTraining() throws InvalidXmlElementException {
        final Training soldier = RulesCatalog.getInstance().getTraining("soldier");
        final CharacterPlayer character = new CharacterPlayer();

        final Map<Integer, String> categorySelections = new HashMap<>();
        for (int i = 0; i < soldier.getCategories().size(); i++) {
            final TrainingCategoryGrant grant = soldier.getCategories().get(i);
            if (grant.isChoice()) {
                categorySelections.put(i, grant.getCategoryOptions().get(0));
            }
        }
        character.applyTrainingCategories(soldier, categorySelections, null);

        // Every grant got a decision recorded, and every decided category actually has ranks.
        for (int i = 0; i < soldier.getCategories().size(); i++) {
            final String key = "training:soldier:category:" + i;
            Assert.assertTrue(character.getDecisions().isDecided(key));
            Assert.assertTrue(character.getCategoryTotalRanks(character.getDecisions().getSelectedOption(key)) >= 0);
        }
    }

    @Test
    public void applyCultureAdolescenceRanksAppliesEveryGrantOfARealCulture() throws InvalidXmlElementException {
        final Culture culture = RulesCatalog.getInstance().getCulture("aquaticMilitarista");
        final CharacterPlayer character = new CharacterPlayer();

        character.applyCultureAdolescenceRanks(culture, null, null);

        for (int i = 0; i < culture.getAdolescenceRanks().size(); i++) {
            Assert.assertTrue(character.getDecisions().isDecided("culture:aquaticMilitarista:adolescence:" + i));
        }
    }

    @Test
    public void applyCharacteristicUpgradeRollsAndIncreasesTheChosenCharacteristic() throws InvalidXmlElementException {
        final Training adventurer = RulesCatalog.getInstance().getTraining("adventurer");
        final CharacterPlayer character = new CharacterPlayer();
        final Integer before = character.getCharacteristicTemporalValue(CharacteristicAbbreviation.STRENGTH);

        final CharacteristicRoll roll = character.applyCharacteristicUpgrade("training:adventurer:characteristic:0",
                adventurer.getCharacteristicUpgrades().get(0), CharacteristicAbbreviation.STRENGTH);

        Assert.assertEquals(roll.getCharacteristicAbbreviation(), CharacteristicAbbreviation.STRENGTH);
        Assert.assertEquals(roll.getCharacteristicTemporalValue(), before);
        Assert.assertEquals(character.getDecisions().getSelectedOption("training:adventurer:characteristic:0"), "STRENGTH");
    }

    @Test(expectedExceptions = InvalidDecisionException.class)
    public void applyCharacteristicUpgradeRejectsACharacteristicNotOffered() throws InvalidXmlElementException {
        final Training adventurer = RulesCatalog.getInstance().getTraining("adventurer");
        final CharacterPlayer character = new CharacterPlayer();
        character.applyCharacteristicUpgrade("training:adventurer:characteristic:0",
                adventurer.getCharacteristicUpgrades().get(0), CharacteristicAbbreviation.NONE);
    }

    @Test
    public void applyCharacteristicUpgradeReusesTheFirstDecision() throws InvalidXmlElementException {
        final Training adventurer = RulesCatalog.getInstance().getTraining("adventurer");
        final CharacterPlayer character = new CharacterPlayer();
        final ChoiceGroup group = adventurer.getCharacteristicUpgrades().get(0);

        character.applyCharacteristicUpgrade("training:adventurer:characteristic:0", group, CharacteristicAbbreviation.STRENGTH);
        character.applyCharacteristicUpgrade("training:adventurer:characteristic:0", group, CharacteristicAbbreviation.AGILITY);

        Assert.assertEquals(character.getDecisions().getSelectedOption("training:adventurer:characteristic:0"), "STRENGTH");
    }
}
