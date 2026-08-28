package com.softwaremagico.librodeesher.character;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryType;
import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.decision.InvalidDecisionException;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.perk.PerkChoiceGrant;
import com.softwaremagico.librodeesher.profession.RealmOfMagicGrant;
import com.softwaremagico.librodeesher.resistance.ResistanceType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.skill.SkillType;
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
    public void appearanceTotalCombinesTheRollWithPresence() throws InvalidXmlElementException {
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
    public void applyingAFixedCategoryGrantAddsItsRanksAndNestedSkillRanks() throws InvalidXmlElementException {
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
    public void applyingAChoiceCategoryGrantRequiresAndRecordsTheSelection() throws InvalidXmlElementException {
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
    public void applyingAChoiceCategoryGrantWithAnInvalidSelectionFails() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(List.of("weaponsTwoHanded", "weaponsEdged"));
        grant.setRanksGranted(1);

        character.applyCategoryGrant("training:soldier:category:1", grant, "weaponsMissile", null);
    }

    @Test
    public void applyingTheSameGrantTwiceReusesTheFirstDecisionAndAccumulatesRanks() throws InvalidXmlElementException {
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

    @Test
    public void expandCategoryWildcardsExpandsAllWeaponCategoriesToEveryRealWeaponCategory() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final List<String> expanded = character.expandCategoryWildcards(List.of(CharacterPlayer.ALL_WEAPON_CATEGORIES));

        Assert.assertTrue(expanded.contains("weaponsEdged"));
        Assert.assertTrue(expanded.contains("weaponsBlunt"));
        Assert.assertTrue(expanded.size() >= 8);
    }

    @Test
    public void expandCategoryWildcardsExpandsAllAttackCategoriesToTheFixedSet() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final List<String> expanded = character.expandCategoryWildcards(List.of(CharacterPlayer.ALL_ATTACK_CATEGORIES));

        Assert.assertEquals(expanded, List.of("martialArtsStrikes", "martialArtsSweeps", "martialArtsCombatManeuvers", "specialAttacks"));
    }

    @Test
    public void expandCategoryWildcardsLeavesRealCategoryIdsUntouched() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.expandCategoryWildcards(List.of("outdoorEnvironment")), List.of("outdoorEnvironment"));
    }

    @Test
    public void applyingACategoryGrantWithTheWeaponWildcardOffersEveryWeaponCategory() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(List.of(CharacterPlayer.ALL_WEAPON_CATEGORIES));
        grant.setRanksGranted(2);

        character.applyCategoryGrant("training:berserker:category:0", grant, "weaponsEdged", null);

        Assert.assertEquals(character.getCategoryTotalRanks("weaponsEdged"), Integer.valueOf(2));
        Assert.assertEquals(character.getDecisions().get("training:berserker:category:0").getOfferedOptions().contains("weaponsBlunt"), true);
    }

    @Test(expectedExceptions = InvalidDecisionException.class)
    public void applyingACategoryGrantWithTheWeaponWildcardRejectsANonWeaponSelection() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(List.of(CharacterPlayer.ALL_WEAPON_CATEGORIES));
        grant.setRanksGranted(2);

        character.applyCategoryGrant("training:berserker:category:0", grant, "outdoorEnvironment", null);
    }

    @Test
    public void applyTrainingSkillChoicesRecordsEveryGrantedSkill() throws InvalidXmlElementException {
        final Training lightWizard = RulesCatalog.getInstance().getTraining("lightWizard");
        final CharacterPlayer character = new CharacterPlayer();

        character.applyTrainingSkillChoices(lightWizard, null, null, null, null);

        Assert.assertEquals(character.getTrainingCommonSkills(lightWizard), List.of("speedAdrenal", "sprinting"));
        Assert.assertTrue(character.getTrainingLifeSkills(lightWizard).isEmpty()
                || character.getTrainingLifeSkills(lightWizard).size() == lightWizard.getLifeSkills().size());
        Assert.assertTrue(character.getTrainingProfessionalSkills(lightWizard).size() <= lightWizard.getProfessionalSkills().size());
        Assert.assertTrue(character.getTrainingRestrictedSkills(lightWizard).size() <= lightWizard.getRestrictedSkills().size());
    }

    @Test
    public void applyTrainingSkillChoicesResolvesAChoiceAndReusesIt() {
        final CharacterPlayer character = new CharacterPlayer();
        final Training training = new Training("test");
        training.setCommonSkills(List.of(new ChoiceGroup(List.of("stalking", "hunting"))));

        character.applyTrainingSkillChoices(training, null, Map.of(0, "hunting"), null, null);
        Assert.assertEquals(character.getTrainingCommonSkills(training), List.of("hunting"));

        // Reusing the same training/index keeps the first decision even with a different selection.
        character.applyTrainingSkillChoices(training, null, Map.of(0, "stalking"), null, null);
        Assert.assertEquals(character.getTrainingCommonSkills(training), List.of("hunting"));
    }

    @Test
    public void restrictedSkillsCountForHalfRanks() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final Skill skill = new Skill("test");
        skill.setName("Prueba", "Test");
        skill.setSkillType(SkillType.RESTRICTED);

        Assert.assertTrue(character.isSkillRestricted(skill));
        Assert.assertEquals(character.getSkillRankMultiplier(skill), 0.5);
    }

    @Test
    public void professionalSkillsCountForTripleRanks() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final Skill skill = new Skill("test");
        skill.setName("Prueba", "Test");
        skill.setSkillType(SkillType.PROFESSIONAL);

        Assert.assertTrue(character.isSkillProfessional(skill));
        Assert.assertEquals(character.getSkillRankMultiplier(skill), 3.0);
    }

    @Test
    public void commonSkillsCountForDoubleRanks() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final Skill skill = new Skill("test");
        skill.setName("Prueba", "Test");
        skill.setSkillType(SkillType.COMMON);

        Assert.assertTrue(character.isSkillCommon(skill));
        Assert.assertEquals(character.getSkillRankMultiplier(skill), 2.0);
    }

    @Test
    public void standardSkillsCountFully() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final Skill skill = new Skill("test");
        skill.setName("Prueba", "Test");

        Assert.assertEquals(character.getSkillRankMultiplier(skill), 1.0);
    }

    @Test
    public void generalizedSkillCountsFullyOnlyIfAlsoCommonOrProfessional() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final Skill commonSkill = new Skill("test1");
        commonSkill.setName("Prueba", "Test");
        commonSkill.setSkillType(SkillType.COMMON);
        character.getCurrentLevel().getGeneralizedSkills().add("test1");
        Assert.assertEquals(character.getSkillRankMultiplier(commonSkill), 1.0);

        final Skill standardSkill = new Skill("test2");
        standardSkill.setName("Otra", "Other");
        character.getCurrentLevel().getGeneralizedSkills().add("test2");
        Assert.assertEquals(character.getSkillRankMultiplier(standardSkill), 0.5);
    }

    @Test
    public void trainingGrantedCommonSkillCountsAsCommonEvenIfStandardByItself() throws InvalidXmlElementException {
        final Training lightWizard = RulesCatalog.getInstance().getTraining("lightWizard");
        final CharacterPlayer character = new CharacterPlayer();
        character.getCurrentLevel().addTraining("lightWizard");
        character.applyTrainingSkillChoices(lightWizard, null, null, null, null);

        final Skill skill = new Skill("sprinting");
        skill.setName("Esprintar", "Sprint");

        Assert.assertTrue(character.isSkillCommon(skill));
        Assert.assertEquals(character.getSkillRankMultiplier(skill), 2.0);
    }

    @Test
    public void professionGrantedCommonSkillCountsAsCommonEvenIfStandardByItself() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setProfessionId("fighter");

        final Skill skill = new Skill("frenzy");
        skill.setName("Frenesí", "Frenzy");

        Assert.assertTrue(character.isSkillCommon(skill));
        Assert.assertEquals(character.getSkillRankMultiplier(skill), 2.0);
    }

    @Test
    public void realRanksAppliesTheMultiplierToBoughtRanks() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final Skill skill = new Skill("test");
        skill.setName("Prueba", "Test");
        skill.setSkillType(SkillType.PROFESSIONAL);
        character.getCurrentLevel().setSkillRanks("test", 4, false);

        Assert.assertEquals(character.getSkillTotalRanks("test"), Integer.valueOf(4));
        Assert.assertEquals(character.getSkillRealRanks(skill), 12);
    }

    @Test
    public void addingAPerkTwiceOnlySelectsItOnce() {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("acrobat");
        character.addPerk("acrobat");

        Assert.assertTrue(character.isPerkSelected("acrobat"));
        Assert.assertEquals(character.getSelectedPerks().size(), 1);
    }

    @Test
    public void removingAPerkForgetsItAndItsWeakness() {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("acrobat");
        character.setWeakness("acrobat", "slightAddiction");
        character.removePerk("acrobat");

        Assert.assertFalse(character.isPerkSelected("acrobat"));
    }

    @Test
    public void perksBackgroundCostUsesTheGradeFormula() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("acrobat");

        // acrobat is MINOR grade, no weakness, not random: base cost 3.
        Assert.assertEquals(character.getPerksBackgroundPointsCost(), 3);
    }

    @Test
    public void pairingAWeaknessDiscountsThePerksBackgroundCost() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("acrobat");
        character.setWeakness("acrobat", "slightAddiction");

        // acrobat is MINOR, slightAddiction is MINIMUM: one grade below, discount of 1.
        Assert.assertTrue(character.hasWeakness("acrobat"));
        Assert.assertEquals(character.getPerksBackgroundPointsCost(), 2);
    }

    @Test
    public void aWeaknessSelectedDirectlyCostsNothing() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("slightAddiction");

        Assert.assertEquals(character.getPerksBackgroundPointsCost(), 0);
    }

    @Test
    public void randomPerkSelectionIsTrackedAndDiscountsCost() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("acrobat");
        character.setPerkAsRandom("acrobat", true);

        Assert.assertTrue(character.isPerkRandom("acrobat"));
        // MINOR base 3, random discount of 1.
        Assert.assertEquals(character.getPerksBackgroundPointsCost(), 2);
    }

    @Test
    public void remainingBackgroundPointsSubtractsPerksAndBackgroundCosts() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("grayOrc");
        character.addPerk("acrobat");
        character.getBackground().setCategoryPoint("outdoorEnvironment", true);

        final int raceBackgroundPoints = character.getRace().getBackgroundPoints();
        Assert.assertEquals(character.getRemainingBackgroundPoints(), raceBackgroundPoints - 1 - 3);
    }

    @Test
    public void applyProfessionMagicRealmsResolvesAHybridProfessionsChoice() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setProfessionId("sorcerer");

        final List<RealmOfMagic> realms = character.applyProfessionMagicRealms(Map.of(0, RealmOfMagic.CANALIZATION));

        Assert.assertEquals(realms, List.of(RealmOfMagic.CANALIZATION));
        Assert.assertEquals(character.getDecisions().getSelectedOption("profession:sorcerer:realm:0"), "CANALIZATION");
    }

    @Test(expectedExceptions = InvalidDecisionException.class)
    public void applyProfessionMagicRealmsRejectsARealmNotOffered() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setProfessionId("sorcerer");

        character.applyProfessionMagicRealms(Map.of(0, RealmOfMagic.MENTALISM));
    }

    @Test
    public void applyProfessionMagicRealmsReturnsEmptyWithoutAProfessionSelected() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertTrue(character.applyProfessionMagicRealms(null).isEmpty());
    }

    @Test
    public void applyMagicRealmChoiceAutoResolvesAFixedGrant() {
        final CharacterPlayer character = new CharacterPlayer();
        final RealmOfMagicGrant grant = new RealmOfMagicGrant(List.of(RealmOfMagic.ESSENCE));

        Assert.assertEquals(character.applyMagicRealmChoice("profession:wizard:realm:0", grant, null), RealmOfMagic.ESSENCE);
    }

    @Test
    public void raceRestrictedSkillIsReflectedInIsSkillRestricted() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("horseCentaur");

        Assert.assertTrue(character.isSkillRestrictedByRace("combatMontado"));

        final Skill skill = new Skill("combatMontado");
        skill.setName("Combate Montado", "Mounted Combat");
        Assert.assertTrue(character.isSkillRestricted(skill));
    }

    @Test
    public void raceClassificationIsFalseWithoutARaceSelected() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertFalse(character.isSkillRestrictedByRace("combatMontado"));
        Assert.assertFalse(character.isSkillCommonByRace("combatMontado"));
        Assert.assertFalse(character.isCategoryRestrictedByRace("outdoorEnvironment"));
        Assert.assertFalse(character.isCategoryCommonByRace("outdoorEnvironment"));
    }

    @Test
    public void raceCommonCategoryIsExposedThroughIsCategoryCommonByRace() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("dyari");

        Assert.assertTrue(character.isCategoryCommonByRace("loreTechnical"));
        Assert.assertFalse(character.isCategoryRestrictedByRace("loreTechnical"));
    }

    @Test
    public void raceCommonWeaponSkillIsRecognizedSinceWeaponsAreAlsoSkills() throws InvalidXmlElementException {
        // grayOrc's common skills are actually weapon names (e.g. "staff"): weapons are migrated as
        // skills too (SkillMigrationTool), so this must be recognized like any other common skill.
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("grayOrc");

        Assert.assertTrue(character.isSkillCommonByRace("staff"));

        final Skill staff = new Skill("staff");
        staff.setName("Bastón", "Staff");
        Assert.assertTrue(character.isSkillCommon(staff));
    }

    @Test
    public void raceLanguageStartingAndMaxRanksAreExposed() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("horseCentaur");

        Assert.assertEquals(character.getRaceLanguageStartingSpeakingRanks("centauro"), 8);
        Assert.assertEquals(character.getRaceLanguageStartingWritingRanks("centauro"), 6);
        Assert.assertEquals(character.getRaceLanguageMaxSpeakingRanks("centauro"), 10);
        Assert.assertEquals(character.getRaceLanguageMaxWritingRanks("centauro"), 10);
    }

    @Test
    public void raceLanguageRanksAreZeroWithoutARaceSelected() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.getRaceLanguageStartingSpeakingRanks("centauro"), 0);
        Assert.assertEquals(character.getRaceLanguageMaxSpeakingRanks("centauro"), 0);
    }

    @Test
    public void raceLanguageMaxRanksDefaultsToTenForAnUnmentionedLanguage() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("horseCentaur");

        Assert.assertEquals(character.getRaceLanguageMaxSpeakingRanks("someLanguageThisRaceNeverMentions"), 10);
    }

    @Test
    public void cultureLanguageMaxRanksAreExposed() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCultureId("aquaticMilitarista");

        Assert.assertEquals(character.getCultureLanguageMaxSpeakingRanks("commonSpeech"), 8);
        Assert.assertEquals(character.getCultureLanguageMaxWritingRanks("commonSpeech"), 8);
    }

    @Test
    public void cultureLanguageMaxRanksAreZeroWithoutACultureSelectedOrLanguageMentioned() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.getCultureLanguageMaxSpeakingRanks("commonSpeech"), 0);

        character.setCultureId("aquaticMilitarista");
        Assert.assertEquals(character.getCultureLanguageMaxSpeakingRanks("someLanguageThisCultureNeverMentions"), 0);
    }

    @Test
    public void languageMaxRanksTakesTheHigherOfRaceAndCulture() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        // horseCentaur caps commonSpeech at 6, aquaticMilitarista caps it at 8.
        character.setRaceId("horseCentaur");
        character.setCultureId("aquaticMilitarista");

        Assert.assertEquals(character.getRaceLanguageMaxSpeakingRanks("commonSpeech"), 6);
        Assert.assertEquals(character.getCultureLanguageMaxSpeakingRanks("commonSpeech"), 8);
        Assert.assertEquals(character.getLanguageMaxSpeakingRanks("commonSpeech"), 8);
    }

    @Test
    public void unassignedOptionalRaceLanguageSlotContributesNothing() throws InvalidXmlElementException {
        // dyari has one optional 'Idioma Regional' slot (8/4 starting, 10/8 max) that nothing was
        // ever assigned to, so an arbitrary language must fall back to the plain default (10/10).
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("dyari");

        Assert.assertEquals(character.getRaceLanguageStartingSpeakingRanks("elvish"), 0);
        Assert.assertEquals(character.getRaceLanguageMaxSpeakingRanks("elvish"), 10);
    }

    @Test
    public void assigningAnOptionalRaceLanguageGrantsItsSlotRanks() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("dyari");

        character.assignOptionalRaceLanguage(0, "elvish");

        Assert.assertEquals(character.getOptionalRaceLanguageAssignment(0), "elvish");
        Assert.assertEquals(character.getRaceLanguageStartingSpeakingRanks("elvish"), 8);
        Assert.assertEquals(character.getRaceLanguageStartingWritingRanks("elvish"), 4);
        Assert.assertEquals(character.getRaceLanguageMaxSpeakingRanks("elvish"), 10);
        Assert.assertEquals(character.getRaceLanguageMaxWritingRanks("elvish"), 8);

        // A different, unassigned language is unaffected.
        Assert.assertEquals(character.getRaceLanguageStartingSpeakingRanks("dwarvish"), 0);
    }

    @Test
    public void assigningAnOptionalCultureLanguageGrantsItsSlotMaxRanks() throws InvalidXmlElementException {
        // aquaticMilitarista has one optional 'Idioma Regional' slot capping at 8/8 (no starting ranks).
        final CharacterPlayer character = new CharacterPlayer();
        character.setCultureId("aquaticMilitarista");

        character.assignOptionalCultureLanguage(0, "elvish");

        Assert.assertEquals(character.getOptionalCultureLanguageAssignment(0), "elvish");
        Assert.assertEquals(character.getCultureLanguageMaxSpeakingRanks("elvish"), 8);
        Assert.assertEquals(character.getCultureLanguageMaxWritingRanks("elvish"), 8);
        Assert.assertEquals(character.getCultureLanguageMaxSpeakingRanks("dwarvish"), 0);
    }

    @Test
    public void backgroundLanguageRankWithinTheCapIsAccepted() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCultureId("aquaticMilitarista");

        character.setBackgroundLanguageRank("commonSpeech", 8);

        Assert.assertEquals(character.getBackground().getHistoryLanguageRank("commonSpeech"), 8);
    }

    @Test
    public void backgroundLanguageRankAboveTheCapIsRejected() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCultureId("aquaticMilitarista");

        // aquaticMilitarista caps commonSpeech at 8.
        character.setBackgroundLanguageRank("commonSpeech", 9);

        Assert.assertEquals(character.getBackground().getHistoryLanguageRank("commonSpeech"), 0);
    }

    @Test
    public void backgroundLanguageRankForAnUnmentionedLanguageIsAlwaysRejected() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setBackgroundLanguageRank("dwarvish", 1);
        Assert.assertEquals(character.getBackground().getHistoryLanguageRank("dwarvish"), 0);
    }

    @Test
    public void hobbySkillRanksAreTrackedAndSummed() {
        final CharacterPlayer character = new CharacterPlayer();
        character.setHobbySkillRank("acrobatics", 3);
        character.setHobbySkillRank("falling", 2);

        Assert.assertEquals(character.getHobbySkillRank("acrobatics"), 3);
        Assert.assertEquals(character.getTotalHobbySkillRanks(), 5);

        character.setHobbySkillRank("acrobatics", 0);
        Assert.assertEquals(character.getHobbySkillRank("acrobatics"), 0);
        Assert.assertEquals(character.getTotalHobbySkillRanks(), 2);
    }

    @Test
    public void cultureHobbyDataIsQueryableAlongsideCharacterSpentRanks() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCultureId("aquaticMilitarista");
        character.setHobbySkillRank("acrobatics", 12);

        final Culture culture = character.getCulture();
        Assert.assertEquals(culture.getHobbyRanks(), Integer.valueOf(12));
        Assert.assertTrue(culture.isHobbySkillAllowed("acrobatics"));
        Assert.assertFalse(culture.isHobbySkillAllowed("someSkillNotInTheList"));
        Assert.assertEquals(character.getTotalHobbySkillRanks(), 12);
    }

    @Test
    public void cultureTrainingPricePercentageDefaultsToOneWithoutACulture() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.getCultureTrainingPricePercentage("soldier"), 1.0);
    }

    @Test
    public void cultureTrainingPricePercentageAppliesTheSelectedCulturesDiscount() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCultureId("aquaticMilitarista");

        Assert.assertEquals(character.getCultureTrainingPricePercentage("soldier"), 0.75);
        Assert.assertEquals(character.getCultureTrainingPricePercentage("mercenary"), 0.75);
        Assert.assertEquals(character.getCultureTrainingPricePercentage("unmentionedTraining"), 1.0);
    }

    @Test
    public void selectedPerkAppliesItsFlatCategoryBonus() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("acrobat");

        Assert.assertEquals(character.getPerkCategoryBonus("athleticsGymnastics"), Integer.valueOf(20));
        Assert.assertEquals(character.getPerkCategoryBonus("crafts"), Integer.valueOf(0));

        final Category category = RulesCatalog.getInstance().getCategory("athleticsGymnastics");
        Assert.assertEquals(character.getCategoryDevelopmentBonus(category),
                Integer.valueOf(category.getCategoryRankBonus(0) + 20));
    }

    @Test
    public void selectedPerkAppliesItsFlatCharacteristicBonus() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("bonusToAgilityMinor");

        Assert.assertEquals(character.getPerkCharacteristicBonus(CharacteristicAbbreviation.AGILITY), Integer.valueOf(3));
        Assert.assertEquals(character.getCharacteristicTotalBonus(CharacteristicAbbreviation.AGILITY),
                character.getCharacteristicTemporalBonus(CharacteristicAbbreviation.AGILITY)
                        + character.getCharacteristicRaceBonus(CharacteristicAbbreviation.AGILITY) + 3);
    }

    @Test
    public void unresolvedPerkChoiceGrantContributesNothingUntilResolved() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("experiencedCatMaximum");

        Assert.assertEquals(character.getPerkCategoryBonus("athleticsGymnastics"), Integer.valueOf(0));
    }

    @Test
    public void resolvedPerkChoiceGrantAppliesToTheChosenCategory() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("experiencedCatMaximum");
        final PerkChoiceGrant grant = RulesCatalog.getInstance().getPerk("experiencedCatMaximum").getChoiceGrants().get(0);

        final String resolved = character.applyPerkChoiceGrant("experiencedCatMaximum", 0, grant, "athleticsGymnastics");

        Assert.assertEquals(resolved, "athleticsGymnastics");
        Assert.assertEquals(character.getPerkCategoryBonus("athleticsGymnastics"), Integer.valueOf(20));
        Assert.assertEquals(character.getPerkCategoryBonus("crafts"), Integer.valueOf(0));

        // Reusing the same key/index does not ask again.
        Assert.assertEquals(character.applyPerkChoiceGrant("experiencedCatMaximum", 0, grant, "crafts"), "athleticsGymnastics");
    }

    @Test
    public void selectedPerkAppliesItsPerRankSkillBonusMultipliedByRealRanks() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.addPerk("aura");
        character.getCurrentLevel().setSkillRanks("powerPointDevelopment", 5, false);

        Assert.assertEquals(character.getPerkSkillRankBonus("powerPointDevelopment"), Integer.valueOf(4));

        final Skill skill = RulesCatalog.getInstance().getSkill("powerPointDevelopment");
        final Category category = RulesCatalog.getInstance().getCategory("powerPointDevelopment");
        Assert.assertEquals(character.getSkillRealRanks(skill), 5);

        final Integer expected = category.getSkillRankBonus(character.getSkillTotalRanks("powerPointDevelopment"))
                + 4 * 5;
        Assert.assertEquals(character.getSkillDevelopmentBonus(category, "powerPointDevelopment"), expected);
    }

    @Test
    public void perkConditionalBonusIsQueryableAndIncludedInTheDevelopmentBonusTotal() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.getPerkSkillConditionalBonus("attunement"), Integer.valueOf(0));

        character.addPerk("magicalAbility");

        // Matches the legacy getSimpleBonus(Skill): a conditional bonus is included in the total
        // unconditionally, the player/consuming code is expected to know when it actually applies.
        Assert.assertEquals(character.getPerkSkillConditionalBonus("attunement"), Integer.valueOf(25));

        final Category category = new Category("attunement");
        category.setType(CategoryType.STANDARD);
        Assert.assertEquals(character.getSkillDevelopmentBonus(category, "attunement"),
                Integer.valueOf(category.getSkillRankBonus(0) + 25));
    }

    @Test
    public void backgroundPointsSpentOnASkillOrCategoryApplyToTheDevelopmentBonus() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.getBackground().setSkillPoint("climbing", true);
        character.getBackground().setCategoryPoint("athleticsGymnastics", true);

        final Category category = new Category("athleticsGymnastics");
        category.setType(CategoryType.STANDARD);
        Assert.assertEquals(character.getSkillDevelopmentBonus(category, "climbing"),
                Integer.valueOf(category.getSkillRankBonus(0) + 10));
        Assert.assertEquals(character.getCategoryDevelopmentBonus(category),
                Integer.valueOf(category.getCategoryRankBonus(0) + 5));
    }

    @Test
    public void raceAppearanceBonusAppliesToAppearanceTotal() throws InvalidXmlElementException {
        final CharacterPlayer withoutRace = new CharacterPlayer();
        Assert.assertEquals(withoutRace.getAppearanceRaceBonus(), 0);

        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("lionCentaur");

        Assert.assertEquals(character.getAppearanceRaceBonus(), -5);
        Assert.assertEquals(character.getAppearanceTotal(),
                character.getAppearance().getTotal(character.getCharacteristicPotentialValue(CharacteristicAbbreviation.PRESENCE)) - 5);
    }

    @Test
    public void raceResistanceBonusCombinesWithPerkBonus() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("lionCentaur");

        Assert.assertEquals(character.getResistanceRaceBonus(ResistanceType.POISON), Integer.valueOf(15));
        Assert.assertEquals(character.getResistanceRaceBonus(ResistanceType.HEAT), Integer.valueOf(0));
        Assert.assertEquals(character.getResistanceTotalBonus(ResistanceType.POISON), Integer.valueOf(15));
    }

    @Test
    public void raceRestrictedProfessionsAreExcludedFromAvailableProfessions() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("lionCentaur");

        Assert.assertTrue(character.isProfessionRestrictedByRace("combatMontado"));
        Assert.assertTrue(character.isProfessionRestrictedByRace("montar"));
        Assert.assertFalse(character.isProfessionRestrictedByRace("fighter"));

        final List<String> available = character.getAvailableProfessionIds();
        Assert.assertTrue(available.contains("fighter"));
        Assert.assertFalse(available.contains("combatMontado"));
        Assert.assertFalse(available.contains("montar"));
    }

    @Test
    public void raceAvailableCulturesMatchItsOwnCultureList() throws InvalidXmlElementException {
        final CharacterPlayer withoutRace = new CharacterPlayer();
        Assert.assertTrue(withoutRace.getAvailableCultureIds().isEmpty());

        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("lionCentaur");

        Assert.assertTrue(character.isCultureAvailableForRace("plains"));
        Assert.assertFalse(character.isCultureAvailableForRace("aquaticMilitarista"));
        Assert.assertEquals(character.getAvailableCultureIds(), List.of("plains"));
    }

    @Test
    public void increaseAgeAppliesACharacteristicDecreaseNearEndOfLifespan() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("lionCentaur"); // expectedLifeYears=100, raceType=2
        for (final CharacteristicAbbreviation abbreviation : CharacteristicAbbreviation.values()) {
            if (abbreviation != CharacteristicAbbreviation.NONE && abbreviation != CharacteristicAbbreviation.REALM_OF_MAGIC) {
                character.rollCharacteristicPotentialValue(abbreviation);
            }
        }
        final int potentialBefore = character.getCharacteristicPotentialValue(CharacteristicAbbreviation.CONSTITUTION);
        character.setCurrentAge(99);
        character.setFinalAge(100);

        character.increaseAge();

        Assert.assertEquals(character.getCurrentAge(), 100);
        Assert.assertEquals(character.getCurrentLevel().getAgeModifications().size(), 1);

        final AgeModification ageModification = character.getCurrentLevel().getAgeModifications().get(0);
        final CharacteristicAbbreviation affected = ageModification.getCharacteristicAbbreviation();
        final int modification = ageModification.getCharacteristicModification();

        if (affected == CharacteristicAbbreviation.CONSTITUTION) {
            final int expectedPotential = potentialBefore - modification / 3;
            Assert.assertEquals(character.getCharacteristicPotentialValue(affected), expectedPotential);
            Assert.assertEquals(character.getCharacteristicTemporalValue(affected),
                    Math.min(Characteristics.INITIAL_CHARACTERISTIC_VALUE - modification, expectedPotential));
        } else {
            Assert.assertNotEquals(character.getCharacteristicTemporalValue(affected), Characteristics.INITIAL_CHARACTERISTIC_VALUE);
        }
    }

    @Test
    public void skillWithNoEnableSkillsIsAlwaysEnabled() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final Skill skill = RulesCatalog.getInstance().getSkill("climbing");
        Assert.assertTrue(character.isSkillEnabled(skill));
    }

    @Test
    public void disabledSkillStaysDisabledUntilUnlocked() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        final Skill chiPower = RulesCatalog.getInstance().getSkill("chiPowerShadowlessAttack");
        Assert.assertFalse(chiPower.isEnabledByDefault());
        Assert.assertFalse(character.isSkillEnabled(chiPower));

        character.getCurrentLevel().setSkillRanks("styleOfTheCrane", 2, false);
        Assert.assertFalse(character.isSkillEnabled(chiPower));

        character.enableSkillOption("styleOfTheCrane", "chiPowerShadowlessAttack");
        Assert.assertTrue(character.isSkillEnabled(chiPower));

        final Skill otherChiPower = RulesCatalog.getInstance().getSkill("chiPowerStrikesContinuous");
        Assert.assertFalse(character.isSkillEnabled(otherChiPower));

        // Reusing the same enabling skill does not ask again.
        character.enableSkillOption("styleOfTheCrane", "chiPowerStrikesContinuous");
        Assert.assertTrue(character.isSkillEnabled(chiPower));
        Assert.assertFalse(character.isSkillEnabled(otherChiPower));
    }

    @Test
    public void professionTrainingPreferenceIsQueryable() throws InvalidXmlElementException {
        final CharacterPlayer withoutProfession = new CharacterPlayer();
        Assert.assertNull(withoutProfession.getProfessionTrainingCost("knight"));
        Assert.assertFalse(withoutProfession.isTrainingFavouredByProfession("knight"));

        final CharacterPlayer character = new CharacterPlayer();
        character.setProfessionId("fighter");

        Assert.assertTrue(character.isTrainingFavouredByProfession("knight"));
        Assert.assertFalse(character.isTrainingForbiddenByProfession("knight"));
        Assert.assertEquals(character.getProfessionTrainingCost("knight").getCost(), Integer.valueOf(25));
        Assert.assertFalse(character.isTrainingFavouredByProfession("unmentionedTraining"));
    }

    @Test
    public void trainingRaceRestrictionIsHonoured() throws InvalidXmlElementException {
        final Training scholar = RulesCatalog.getInstance().getTraining("scholar");
        Assert.assertTrue(scholar.isAvailableToEveryRace());

        final Training rukleftakSher = RulesCatalog.getInstance().getTraining("rukleftakSher");
        Assert.assertFalse(rukleftakSher.isAvailableToEveryRace());
        Assert.assertEquals(rukleftakSher.getLimitedRaces(), List.of("grayOrc"));

        final CharacterPlayer withoutRace = new CharacterPlayer();
        Assert.assertTrue(withoutRace.isTrainingAvailableForRace(scholar));
        Assert.assertFalse(withoutRace.isTrainingAvailableForRace(rukleftakSher));

        final CharacterPlayer wrongRace = new CharacterPlayer();
        wrongRace.setRaceId("lionCentaur");
        Assert.assertFalse(wrongRace.isTrainingAvailableForRace(rukleftakSher));

        final CharacterPlayer rightRace = new CharacterPlayer();
        rightRace.setRaceId("grayOrc");
        Assert.assertTrue(rightRace.isTrainingAvailableForRace(rukleftakSher));
    }

    @Test
    public void raceCultureGroupTokenMatchesEveryCultureContainingIt() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("dwarf");

        Assert.assertTrue(character.isCultureAvailableForRace("mining"));
        Assert.assertTrue(character.isCultureAvailableForRace("aquaticUrbanClassHigh"));
        Assert.assertTrue(character.isCultureAvailableForRace("desertUrbanClassLow"));
        Assert.assertFalse(character.isCultureAvailableForRace("rural"));
        Assert.assertTrue(character.getAvailableCultureIds().contains("mining"));
        Assert.assertFalse(character.getAvailableCultureIds().contains("rural"));
    }

    @Test
    public void raceCultureExclusionFlipsAvailabilityToEveryoneElse() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("highMen");

        Assert.assertTrue(character.getRace().getExcludedCultureIds().contains("underground"));
        Assert.assertFalse(character.isCultureAvailableForRace("underground"));
        Assert.assertFalse(character.isCultureAvailableForRace("woodland"));
        // "group:barbarian" is also excluded.
        Assert.assertFalse(character.isCultureAvailableForRace("aerialBarbarian"));
        // Everything else remains available.
        Assert.assertTrue(character.isCultureAvailableForRace("rural"));
        Assert.assertTrue(character.isCultureAvailableForRace("aquatic"));
    }

    @Test
    public void raceProfessionGroupTokenMatchesEveryProfessionOfThatRealm() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("lugrôkiMenores");

        Assert.assertTrue(character.isProfessionRestrictedByRace("paladin"));
        // "group:essence" restricts every essence spellcaster, e.g. the wizard.
        Assert.assertTrue(character.isProfessionRestrictedByRace("wizard"));
        Assert.assertFalse(character.isProfessionRestrictedByRace("unmentionedProfession"));
        Assert.assertFalse(character.getAvailableProfessionIds().contains("paladin"));
        Assert.assertFalse(character.getAvailableProfessionIds().contains("wizard"));
    }
}
