package com.softwaremagico.librodeesher.character;

import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryType;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.decision.InvalidDecisionException;
import com.softwaremagico.librodeesher.dice.Roll;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.equipment.ObjectBonus;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.language.TranslatedText;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.magic.MagicListType;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.perk.PerkChoiceGrant;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.ProfessionMagicCost;
import com.softwaremagico.librodeesher.profession.ProfessionSkillGrant;
import com.softwaremagico.librodeesher.profession.RealmOfMagicGrant;
import com.softwaremagico.librodeesher.resistance.ResistanceType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.skill.SkillType;
import com.softwaremagico.librodeesher.training.ChoiceGroup;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingItemType;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;
import com.softwaremagico.librodeesher.training.TrainingSpecialItem;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies {@link CharacterPlayer}'s identity, characteristic and level
 * bookkeeping.
 */
@Test(groups = "character")
public class CharacterPlayerTest {

	@Test
	public void everyCharacteristicStartsAtTheInitialValue() {
		final CharacterPlayer character = new CharacterPlayer();
		for (final CharacteristicAbbreviation abbreviation : CharacteristicAbbreviation.values()) {
			if (abbreviation == CharacteristicAbbreviation.NONE
					|| abbreviation == CharacteristicAbbreviation.REALM_OF_MAGIC) {
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
	public void temporalBonusIsComputedFromTheStandardTable() {
		final CharacterPlayer character = new CharacterPlayer();
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.STRENGTH, 90);
		Assert.assertEquals(character.getCharacteristicTemporalBonus(CharacteristicAbbreviation.STRENGTH),
				Characteristics.getTemporalBonus(90));
	}

	@Test
	public void raceBonusIsZeroWithoutARaceSelected() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		Assert.assertEquals(character.getCharacteristicRaceBonus(CharacteristicAbbreviation.STRENGTH),
				Integer.valueOf(0));
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

		Assert.assertEquals(character.getCategoryDevelopmentBonus(category),
				CategoryType.STANDARD.getCategoryRankBonus(10));
	}

	@Test
	public void physicalDevelopmentCategoryGrantsItsFixedBonusRegardlessOfRanks() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final Category category = new Category("physicalDevelopment");
		category.setType(CategoryType.PD);

		Assert.assertEquals(character.getCategoryDevelopmentBonus(category), Integer.valueOf(10));
	}

	@Test
	public void physicalDevelopmentRanksGrantRaceBasedHitPoints() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("grayOrc");
		character.getCurrentLevel().setSkillRanks("physicalDevelopment", 3, false);

		Assert.assertEquals(character.getHitPoints(), RulesCatalog.getInstance().getRace("grayOrc")
				.getProgressionRankValue("physicalDevelopment", 3).intValue());
	}

	@Test
	public void skillDevelopmentBonusUsesItsCategorysProgressionTable() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.getCurrentLevel().setSkillRanks("tracking", 10, false);

		final Category category = new Category("outdoorEnvironment");
		category.setType(CategoryType.STANDARD);

		Assert.assertEquals(character.getSkillDevelopmentBonus(category, "tracking"),
				CategoryType.STANDARD.getSkillRankBonus(10));
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

		character.applyCategoryGrant("training:scout:category:0", grant, null, null, null);

		Assert.assertEquals(character.getCategoryTotalRanks("outdoorEnvironment"), Integer.valueOf(2));
		Assert.assertEquals(character.getSkillTotalRanks("tracking"), Integer.valueOf(2));
		Assert.assertEquals(character.getDecisions().getSelectedOption("training:scout:category:0"),
				"outdoorEnvironment");
		Assert.assertEquals(character.getDecisions().getSelectedOption("training:scout:category:0:skill:0"),
				"tracking");
	}

	@Test
	public void applyingAChoiceCategoryGrantRequiresAndRecordsTheSelection() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final TrainingCategoryGrant grant = new TrainingCategoryGrant();
		grant.setCategoryOptions(List.of("weaponsTwoHanded", "weaponsEdged"));
		grant.setRanksGranted(1);

		character.applyCategoryGrant("training:soldier:category:1", grant, "weaponsEdged", null, null);

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

		character.applyCategoryGrant("training:soldier:category:1", grant, "weaponsMissile", null, null);
	}

	@Test
	public void applyingTheSameGrantTwiceReusesTheFirstDecisionAndAccumulatesRanks() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final TrainingCategoryGrant grant = new TrainingCategoryGrant();
		grant.setCategoryOptions(List.of("weaponsTwoHanded", "weaponsEdged"));
		grant.setRanksGranted(1);

		character.applyCategoryGrant("training:soldier:category:1", grant, "weaponsEdged", null, null);
		// A different (and otherwise invalid, since it wasn't offered before) id is
		// ignored because
		// the decision was already made.
		character.applyCategoryGrant("training:soldier:category:1", grant, "somethingElse", null, null);

		Assert.assertEquals(character.getCategoryTotalRanks("weaponsEdged"), Integer.valueOf(2));
		Assert.assertEquals(character.getDecisions().getSelectedOption("training:soldier:category:1"), "weaponsEdged");
	}

	@Test
	public void applyTrainingCategoriesAppliesEveryGrantOfARealTraining() throws InvalidXmlElementException {
		final Training soldier = RulesCatalog.getInstance().getTraining("soldier");
		final CharacterPlayer character = new CharacterPlayer();

		final Map<Integer, String> categorySelections = new HashMap<>();
		final Map<Integer, Map<String, Integer>> additionalSkillRanksSelections = new HashMap<>();
		for (int i = 0; i < soldier.getCategories().size(); i++) {
			final TrainingCategoryGrant grant = soldier.getCategories().get(i);
			final String selectedCategoryId = grant.getCategoryOptions().get(0);
			if (grant.isChoice()) {
				categorySelections.put(i, selectedCategoryId);
			}
			// A grant with no named skills (see TrainingCategoryGrant's class doc) leaves
			// the
			// player free to pick which of the category's skills receive its
			// ranksToDistribute:
			// just pick the first one here (weapon categories list their skills
			// dynamically, in
			// WeaponFactory, rather than in the category itself, see
			// Category#hasDynamicSkills).
			if (grant.getSkills().isEmpty() && grant.getRanksToDistribute() > 0) {
				final Category category = RulesCatalog.getInstance().getCategory(selectedCategoryId);
				final String firstSkillId = category.hasDynamicSkills()
						? RulesCatalog.getInstance().getWeapons().stream()
								.filter(weapon -> selectedCategoryId.equals(weapon.getCategoryId())).findFirst()
								.orElseThrow().getId()
						: category.getSkills().get(0);
				additionalSkillRanksSelections.put(i, Map.of(firstSkillId, grant.getRanksToDistribute()));
			}
		}
		character.applyTrainingCategories(soldier, categorySelections, null, additionalSkillRanksSelections);

		// Every grant got a decision recorded, and every decided category actually has
		// ranks.
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

		character.applyCultureAdolescenceRanks(culture, null, null, null);

		for (int i = 0; i < culture.getAdolescenceRanks().size(); i++) {
			Assert.assertTrue(character.getDecisions().isDecided("culture:aquaticMilitarista:adolescence:" + i));
		}
	}

	/**
	 * Matches the legacy {@code PulpTests#checkCultureRanks} exactly (same
	 * race/culture/category, same expected rank count); unlike the legacy
	 * application (which applied a culture's adolescence ranks automatically as a
	 * side effect of {@code setCulture}), NG requires an explicit
	 * {@link CharacterPlayer#applyCultureAdolescenceRanks} call - consistent with
	 * every other grant in this class (profession/training/perk choices are
	 * likewise never auto-applied on selection), so a caller (e.g. a chargen wizard
	 * UI) drives exactly when/how each decision gets made.
	 */
	@Test
	public void pulpIndustrializedCultureGrantsOneRankOfSearchingLikeLegacy() throws InvalidXmlElementException {
		final Culture culture = RulesCatalog.getInstance().getCulture("industrializedUrbanClassHigh");
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("modernMan");

		final Map<Integer, String> categorySelections = new HashMap<>();
		final Map<Integer, Map<String, Integer>> additionalSkillRanksSelections = new HashMap<>();
		for (int i = 0; i < culture.getAdolescenceRanks().size(); i++) {
			final TrainingCategoryGrant grant = culture.getAdolescenceRanks().get(i);
			final String selectedCategoryId = grant.getCategoryOptions().get(0);
			if (grant.isChoice()) {
				categorySelections.put(i, selectedCategoryId);
			}
			if (grant.getSkills().isEmpty() && grant.getRanksToDistribute() > 0) {
				final Category category = RulesCatalog.getInstance().getCategory(selectedCategoryId);
				final String firstSkillId = category.hasDynamicSkills()
						? RulesCatalog.getInstance().getWeapons().stream()
								.filter(weapon -> selectedCategoryId.equals(weapon.getCategoryId())).findFirst()
								.orElseThrow().getId()
						: category.getSkills().get(0);
				additionalSkillRanksSelections.put(i, Map.of(firstSkillId, grant.getRanksToDistribute()));
			}
		}
		character.applyCultureAdolescenceRanks(culture, categorySelections, null, additionalSkillRanksSelections);

		Assert.assertEquals(character.getCategoryTotalRanks("perceptionSearching"), Integer.valueOf(1));
	}

	@Test
	public void hobbyRanksAreFreeAndIncludedInSkillTotals() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setHobbySkillRank("climbing", 3);

		Assert.assertEquals(character.getSkillTotalRanks("climbing"), Integer.valueOf(3));
		Assert.assertEquals(character.getSpentDevelopmentPoints(), Integer.valueOf(0));
		Assert.assertEquals(character.getTotalHobbySkillRanks(), 3);
	}

	@Test
	public void cultureSpellListHobbyAllowsOpenAndRaceListsOnly() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setCultureId("aquaticMilitarista");
		character.setRaceId("horseCentaur");

		final Culture culture = character.getCulture();
		culture.setHobbyIds(List.of("listOfSpells"));
		Assert.assertTrue(character.isHobbySpellListAllowed("essenceBarrierAgainstSpells"));
		Assert.assertFalse(character.isHobbySpellListAllowed("essenceLawOfLight"));

		character.setHobbySpellListRank("essenceBarrierAgainstSpells", 2);
		Assert.assertEquals(character.getSpellListTotalRanks("essenceBarrierAgainstSpells"), Integer.valueOf(2));
		Assert.assertEquals(character.getTotalHobbySkillRanks(), 2);
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
		Assert.assertEquals(character.getDecisions().getSelectedOption("training:adventurer:characteristic:0"),
				"STRENGTH");
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

		character.applyCharacteristicUpgrade("training:adventurer:characteristic:0", group,
				CharacteristicAbbreviation.STRENGTH);
		character.applyCharacteristicUpgrade("training:adventurer:characteristic:0", group,
				CharacteristicAbbreviation.AGILITY);

		Assert.assertEquals(character.getDecisions().getSelectedOption("training:adventurer:characteristic:0"),
				"STRENGTH");
	}

	@Test
	public void backgroundCharacteristicUpdateAppliesAndIsIncludedInBackgroundCost() {
		final CharacterPlayer character = new CharacterPlayer();
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 50);
		character.setCharacteristicPotentialValue(CharacteristicAbbreviation.AGILITY, 90);
		final Roll roll = new Roll(10);
		roll.setFirstDice(4);
		roll.setSecondDice(6);

		final CharacteristicRoll update = character.applyBackgroundCharacteristicUpdate(CharacteristicAbbreviation.AGILITY, roll);

		Assert.assertEquals(update.getCharacteristicAbbreviation(), CharacteristicAbbreviation.AGILITY);
		Assert.assertEquals(character.getCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY), Integer.valueOf(60));
		Assert.assertEquals(character.getBackground().getCharacteristicUpdates(CharacteristicAbbreviation.AGILITY).size(), 1);
		Assert.assertEquals(character.getBackground().getSpentBackgroundPoints(), Integer.valueOf(1));
	}

	@Test
	public void expandCategoryWildcardsExpandsAllWeaponCategoriesToEveryRealWeaponCategory()
			throws InvalidXmlElementException {
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

		Assert.assertEquals(expanded,
				List.of("martialArtsStrikes", "martialArtsSweeps", "martialArtsCombatManeuvers", "specialAttacks"));
	}

	@Test
	public void expandCategoryWildcardsLeavesRealCategoryIdsUntouched() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		Assert.assertEquals(character.expandCategoryWildcards(List.of("outdoorEnvironment")),
				List.of("outdoorEnvironment"));
	}

	@Test
	public void applyingACategoryGrantWithTheWeaponWildcardOffersEveryWeaponCategory()
			throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final TrainingCategoryGrant grant = new TrainingCategoryGrant();
		grant.setCategoryOptions(List.of(CharacterPlayer.ALL_WEAPON_CATEGORIES));
		grant.setRanksGranted(2);

		character.applyCategoryGrant("training:berserker:category:0", grant, "weaponsEdged", null, null);

		Assert.assertEquals(character.getCategoryTotalRanks("weaponsEdged"), Integer.valueOf(2));
		Assert.assertEquals(character.getDecisions().get("training:berserker:category:0").getOfferedOptions()
				.contains("weaponsBlunt"), true);
	}

	@Test(expectedExceptions = InvalidDecisionException.class)
	public void applyingACategoryGrantWithTheWeaponWildcardRejectsANonWeaponSelection()
			throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final TrainingCategoryGrant grant = new TrainingCategoryGrant();
		grant.setCategoryOptions(List.of(CharacterPlayer.ALL_WEAPON_CATEGORIES));
		grant.setRanksGranted(2);

		character.applyCategoryGrant("training:berserker:category:0", grant, "outdoorEnvironment", null, null);
	}

	@Test
	public void applyTrainingSkillChoicesRecordsEveryGrantedSkill() throws InvalidXmlElementException {
		final Training lightWizard = RulesCatalog.getInstance().getTraining("lightWizard");
		final CharacterPlayer character = new CharacterPlayer();

		character.applyTrainingSkillChoices(lightWizard, null, null, null, null);

		Assert.assertEquals(character.getTrainingCommonSkills(lightWizard), List.of("speedAdrenal", "sprinting"));
		Assert.assertTrue(character.getTrainingLifeSkills(lightWizard).isEmpty()
				|| character.getTrainingLifeSkills(lightWizard).size() == lightWizard.getLifeSkills().size());
		Assert.assertTrue(character.getTrainingProfessionalSkills(lightWizard).size() <= lightWizard
				.getProfessionalSkills().size());
		Assert.assertTrue(
				character.getTrainingRestrictedSkills(lightWizard).size() <= lightWizard.getRestrictedSkills().size());
	}

	@Test
	public void applyTrainingSkillChoicesResolvesAChoiceAndReusesIt() {
		final CharacterPlayer character = new CharacterPlayer();
		final Training training = new Training("test");
		training.setCommonSkills(List.of(new ChoiceGroup(List.of("stalking", "hunting"))));

		character.applyTrainingSkillChoices(training, null, Map.of(0, "hunting"), null, null);
		Assert.assertEquals(character.getTrainingCommonSkills(training), List.of("hunting"));

		// Reusing the same training/index keeps the first decision even with a
		// different selection.
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
	public void randomPerksCannotBeRemovedAndWeaknessesCanBeUnpaired() {
		final CharacterPlayer character = new CharacterPlayer();
		character.addPerk("acrobat");
		character.setWeakness("acrobat", "slightAddiction");

		Assert.assertTrue(character.removeWeakness("slightAddiction"));
		Assert.assertFalse(character.hasWeakness("acrobat"));
		Assert.assertFalse(character.removeWeakness("slightAddiction"));

		character.setPerkAsRandom("acrobat", true);
		character.removePerk("acrobat");
		Assert.assertTrue(character.isPerkSelected("acrobat"));
	}

	@Test
	public void availableWeaknessesFollowTheLegacyGradeAndUniquenessRules() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.addPerk("acrobat");

		Assert.assertTrue(character.getAvailableWeaknessIds("acrobat").contains("slightAddiction"));
		Assert.assertTrue(character.addWeakness("acrobat", "slightAddiction"));
		Assert.assertTrue(character.getAvailableWeaknessIds("acrobat").isEmpty());
		Assert.assertFalse(character.addWeakness("acrobat", "slightAddiction"));
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

		Assert.assertEquals(character.applyMagicRealmChoice("profession:wizard:realm:0", grant, null),
				RealmOfMagic.ESSENCE);
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
		// grayOrc's common skills are actually weapon names (e.g. "staff"): weapons are
		// migrated as
		// skills too (SkillMigrationTool), so this must be recognized like any other
		// common skill.
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
	public void cultureLanguageMaxRanksAreZeroWithoutACultureSelectedOrLanguageMentioned()
			throws InvalidXmlElementException {
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
		// dyari has one optional 'Idioma Regional' slot (8/4 starting, 10/8 max) that
		// nothing was
		// ever assigned to, so an arbitrary language must fall back to the plain
		// default (10/10).
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
		// aquaticMilitarista has one optional 'Idioma Regional' slot capping at 8/8 (no
		// starting ranks).
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

		Assert.assertEquals(character.getPerkCharacteristicBonus(CharacteristicAbbreviation.AGILITY),
				Integer.valueOf(3));
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
		final PerkChoiceGrant grant = RulesCatalog.getInstance().getPerk("experiencedCatMaximum").getChoiceGrants()
				.get(0);

		final String resolved = character.applyPerkChoiceGrant("experiencedCatMaximum", 0, grant,
				"athleticsGymnastics");

		Assert.assertEquals(resolved, "athleticsGymnastics");
		Assert.assertEquals(character.getPerkCategoryBonus("athleticsGymnastics"), Integer.valueOf(20));
		Assert.assertEquals(character.getPerkCategoryBonus("crafts"), Integer.valueOf(0));

		// Reusing the same key/index does not ask again.
		Assert.assertEquals(character.applyPerkChoiceGrant("experiencedCatMaximum", 0, grant, "crafts"),
				"athleticsGymnastics");
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
	public void specializedSkillRanksFollowTheLegacyCategoryRule() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final Skill skill = RulesCatalog.getInstance().getSkill("softLeather");
		character.getCurrentLevel().setSkillRanks(skill.getId(), 4, false);

		Assert.assertEquals(character.getSpecializedSkillRanks(skill), 6);
		character.getCurrentLevel().setCategoryRanks(skill.getCategoryId(), 1);
		Assert.assertEquals(character.getSpecializedSkillRanks(skill), 8);
	}

	@Test
	public void perkConditionalBonusIsQueryableAndIncludedInTheDevelopmentBonusTotal()
			throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		Assert.assertEquals(character.getPerkSkillConditionalBonus("attunement"), Integer.valueOf(0));

		character.addPerk("magicalAbility");

		// Matches the legacy getSimpleBonus(Skill): a conditional bonus is included in
		// the total
		// unconditionally, the player/consuming code is expected to know when it
		// actually applies.
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
		Assert.assertEquals(character.getAppearanceTotal(), character.getAppearance()
				.getTotal(character.getCharacteristicPotentialValue(CharacteristicAbbreviation.PRESENCE)) - 5);
	}

	@Test
	public void raceResistanceBonusCombinesWithPerkBonus() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("lionCentaur");

		Assert.assertEquals(character.getResistanceRaceBonus(ResistanceType.POISON), Integer.valueOf(15));
		Assert.assertEquals(character.getResistanceRaceBonus(ResistanceType.HEAT), Integer.valueOf(0));
		// POISON's own total also includes 3 times the race's own CONSTITUTION bonus
		// (+4 for
		// "lionCentaur"); HEAT has no governing characteristic at all, so it stays
		// race-only.
		Assert.assertEquals(character.getResistanceTotalBonus(ResistanceType.POISON), Integer.valueOf(15 + 4 * 3));
		Assert.assertEquals(character.getResistanceTotalBonus(ResistanceType.HEAT), Integer.valueOf(0));
	}

	@Test
	public void magicalAbilityPerkBonusesApplyToOwnRealmResistanceAndSpellListTypes()
			throws InvalidXmlElementException {
		// "magicalAbility" ("Capacidad Mágica") grants: +50 to the character's own
		// realm's
		// resistance (the "TR Reino" marker), and +25 to every spell list of several
		// MagicListType
		// classifications (through the legacy per-type synthetic Category).
		final CharacterPlayer nonCaster = new CharacterPlayer();
		nonCaster.setProfessionId("fighter");
		nonCaster.addPerk("magicalAbility");
		// "fighter" is not a real spell caster (see Profession#isSpellCaster), so it
		// has no
		// "own realm" for the resistance bonus to apply to.
		Assert.assertEquals(nonCaster.getResistanceTotalBonus(ResistanceType.ESSENCE), Integer.valueOf(0));
		// The spell list type bonus does not depend on having a realm at all.
		Assert.assertEquals(nonCaster.getPerkSpellListTypeBonus(MagicListType.BASIC), Integer.valueOf(25));
		Assert.assertEquals(nonCaster.getPerkSpellListTypeBonus(MagicListType.OPEN), Integer.valueOf(25));
		Assert.assertEquals(nonCaster.getPerkSpellListTypeBonus(MagicListType.CLOSED), Integer.valueOf(25));

		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		wizard.applyProfessionMagicRealms(null);
		wizard.addPerk("magicalAbility");
		Assert.assertEquals(wizard.getResistanceTotalBonus(ResistanceType.ESSENCE), Integer.valueOf(50));
		// A resistance type that is not the character's own realm at all is unaffected.
		Assert.assertEquals(wizard.getResistanceTotalBonus(ResistanceType.MENTALISM), Integer.valueOf(0));
		Assert.assertEquals(wizard.getResistanceTotalBonus(ResistanceType.POISON), Integer.valueOf(0));
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
			if (abbreviation != CharacteristicAbbreviation.NONE
					&& abbreviation != CharacteristicAbbreviation.REALM_OF_MAGIC) {
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
			Assert.assertNotEquals(character.getCharacteristicTemporalValue(affected),
					Characteristics.INITIAL_CHARACTERISTIC_VALUE);
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
		Assert.assertFalse(character.isTrainingFavouredByProfession("soldier"));
	}

	@Test
	public void trainingsOwnProfessionCostIsUsedWhenTheProfessionDoesNotMentionIt() throws InvalidXmlElementException {
		// "streetSweeper" is the only shipped training with its own "REQUISITOS
		// PROFESIONALES"-like
		// trailing section (see TrainingProfessionCost); "layman" does not mention it
		// on its own side.
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("layman");

		Assert.assertNull(character.getProfessionTrainingCost("streetSweeper"));
		Assert.assertFalse(character.isTrainingFavouredByProfession("streetSweeper"));
		Assert.assertFalse(character.isTrainingForbiddenByProfession("streetSweeper"));

		final CharacterPlayer otherProfession = new CharacterPlayer();
		otherProfession.setProfessionId("fighter");
		Assert.assertFalse(otherProfession.isTrainingFavouredByProfession("streetSweeper"));
	}

	@Test
	public void professionCategoryDevelopmentCostIsQueryable() throws InvalidXmlElementException {
		final CharacterPlayer withoutProfession = new CharacterPlayer();
		Assert.assertNull(withoutProfession.getProfessionCategoryCost("armorLight"));
		Assert.assertNull(withoutProfession.getCategoryDevelopmentCost("armorLight", 0));

		final CharacterPlayer fighter = new CharacterPlayer();
		fighter.setProfessionId("fighter");

		Assert.assertEquals(fighter.getProfessionCategoryCost("armorLight").getRankCosts(), List.of(1, 1, 1));
		Assert.assertEquals(fighter.getCategoryDevelopmentCost("armorLight", 0), Integer.valueOf(1));
		// "artPerforming" only lists 2 rank costs ("2/5"): a 3rd rank bought in the
		// same level has no
		// defined cost.
		Assert.assertEquals(fighter.getCategoryDevelopmentCost("artPerforming", 0), Integer.valueOf(2));
		Assert.assertEquals(fighter.getCategoryDevelopmentCost("artPerforming", 1), Integer.valueOf(5));
		Assert.assertNull(fighter.getCategoryDevelopmentCost("artPerforming", 2));

		// A weapon category is not in a profession's plain categoryCosts table (see
		// getWeaponCategoryCostTiers, future work).
		Assert.assertNull(fighter.getProfessionCategoryCost("weaponsEdged"));
	}

	@Test
	public void weaponCategoryCostTiersAreFreelyAssignedByThePlayer() throws InvalidXmlElementException {
		final CharacterPlayer fighter = new CharacterPlayer();
		fighter.setProfessionId("fighter");
		Assert.assertNull(fighter.getAssignedWeaponCategoryCostTier("weaponsEdged"));

		// Cheapest tier ("1/5") to the player's favourite weapon category.
		Assert.assertFalse(fighter.isWeaponCategoryCostTierAssigned(0));
		fighter.assignWeaponCategoryCostTier(0, "weaponsEdged");
		Assert.assertTrue(fighter.isWeaponCategoryCostTierAssigned(0));
		Assert.assertEquals(fighter.getAssignedWeaponCategoryCostTier("weaponsEdged").getRankCosts(), List.of(1, 5));

		// A different tier ("2/5") to a different category.
		fighter.assignWeaponCategoryCostTier(1, "weaponsBlunt");
		Assert.assertEquals(fighter.getAssignedWeaponCategoryCostTier("weaponsBlunt").getRankCosts(), List.of(2, 5));

		// Not a real weapon category, and an out-of-range tier index.
		Assert.assertThrows(IllegalArgumentException.class,
				() -> fighter.assignWeaponCategoryCostTier(2, "armorLight"));
		Assert.assertThrows(IllegalArgumentException.class,
				() -> fighter.assignWeaponCategoryCostTier(99, "weaponsThrown"));

		// No profession selected at all.
		final CharacterPlayer withoutProfession = new CharacterPlayer();
		Assert.assertThrows(IllegalStateException.class,
				() -> withoutProfession.assignWeaponCategoryCostTier(0, "weaponsEdged"));
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
		character.setRaceId("lugrokiMenores");

		Assert.assertTrue(character.isProfessionRestrictedByRace("paladin"));
		// "group:essence" restricts every essence spellcaster, e.g. the wizard.
		Assert.assertTrue(character.isProfessionRestrictedByRace("wizard"));
		Assert.assertFalse(character.isProfessionRestrictedByRace("unmentionedProfession"));
		Assert.assertFalse(character.getAvailableProfessionIds().contains("paladin"));
		Assert.assertFalse(character.getAvailableProfessionIds().contains("wizard"));
	}

	@Test
	public void professionSkillGrantResolvesAChoiceOfSkillOptions() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("thief");
		final ProfessionSkillGrant grant = character.getProfession().getCommonSkillChoices().get(0);
		Assert.assertEquals(grant.getRanksToChoose(), Integer.valueOf(1));

		final List<String> resolved = character.applyProfessionSkillGrant("thief",
				CharacterPlayer.PROFESSION_COMMON_SKILLS_SECTION, 0, grant,
				List.of("perceptionOfTheEnvironmentCombat"));

		Assert.assertEquals(resolved, List.of("perceptionOfTheEnvironmentCombat"));

		final Skill chosenSkill = RulesCatalog.getInstance().getSkill("perceptionOfTheEnvironmentCombat");
		Assert.assertTrue(character.isSkillCommon(chosenSkill));
		final Skill otherOption = RulesCatalog.getInstance().getSkill("perceptionOfTheEnvironmentCiudades");
		Assert.assertFalse(character.isSkillCommon(otherOption));

		// Reusing the same key/index does not ask again.
		Assert.assertEquals(
				character.applyProfessionSkillGrant("thief", CharacterPlayer.PROFESSION_COMMON_SKILLS_SECTION, 0, grant,
						List.of("perceptionOfTheEnvironmentCiudades")),
				List.of("perceptionOfTheEnvironmentCombat"));
	}

	@Test
	public void professionSkillGrantResolvesChoosingNSkillsFromACategory() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("alchemistOfMentalism");
		final ProfessionSkillGrant grant = character.getProfession().getProfessionalSkillChoices().get(0);
		Assert.assertEquals(grant.getCategoryId(), "crafts");
		Assert.assertEquals(grant.getRanksToChoose(), Integer.valueOf(6));

		final List<String> chosen = List.of("cocinar", "manejoOfCuerdas", "trabajarTheLeather", "trabajarTheMetal",
				"trabajarTheMadera", "trabajarTheStone");
		final List<String> resolved = character.applyProfessionSkillGrant("alchemistOfMentalism",
				CharacterPlayer.PROFESSION_PROFESSIONAL_SKILLS_SECTION, 0, grant, chosen);

		Assert.assertEquals(resolved, chosen);
		Assert.assertTrue(character.isSkillProfessional(RulesCatalog.getInstance().getSkill("cocinar")));
		Assert.assertFalse(character.isSkillProfessional(RulesCatalog.getInstance().getSkill("coserTejer")));
	}

	@Test(expectedExceptions = InvalidDecisionException.class)
	public void professionSkillGrantRejectsTheWrongNumberOfSelections() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("alchemistOfMentalism");
		final ProfessionSkillGrant grant = character.getProfession().getProfessionalSkillChoices().get(0);

		character.applyProfessionSkillGrant("alchemistOfMentalism",
				CharacterPlayer.PROFESSION_PROFESSIONAL_SKILLS_SECTION, 0, grant, List.of("cocinar"));
	}

	@Test
	public void perkAvailabilityIsRestrictedToItsRaceOrProfession() throws InvalidXmlElementException {
		final CharacterPlayer withoutRace = new CharacterPlayer();
		Assert.assertFalse(withoutRace.isPerkAllowedForCharacter("anxious"));

		final CharacterPlayer wrongRace = new CharacterPlayer();
		wrongRace.setRaceId("lionCentaur");
		Assert.assertFalse(wrongRace.isPerkAllowedForCharacter("anxious"));

		final CharacterPlayer rightRace = new CharacterPlayer();
		rightRace.setRaceId("grayOrc");
		Assert.assertTrue(rightRace.isPerkAllowedForCharacter("anxious"));

		// Unrestricted perks are always allowed.
		Assert.assertTrue(withoutRace.isPerkAllowedForCharacter("acrobat"));
	}

	@Test
	public void addAllowedPerkRespectsRaceRestrictionsAndRejectsDuplicates() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		Assert.assertFalse(character.addAllowedPerk("anxious"));

		character.setRaceId("grayOrc");
		Assert.assertTrue(character.addAllowedPerk("anxious"));
		Assert.assertFalse(character.addAllowedPerk("anxious"));
		Assert.assertEquals(character.getSelectedPerks().size(), 1);
	}

	@Test
	public void availablePerksRespectRestrictionsSelectionsAndBackgroundPoints() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("grayOrc");
		Assert.assertTrue(character.getAvailablePerkIds().contains("anxious"));
		Assert.assertTrue(character.getAvailablePerkIds().contains("acrobat"));

		character.addPerk("acrobat");
		Assert.assertFalse(character.getAvailablePerkIds().contains("acrobat"));

		character.getBackground().setCategoryPoint("outdoorEnvironment", true);
		Assert.assertFalse(character.getAvailablePerkIds().contains("anxious"));
	}

	@Test
	public void addAvailablePerkEnforcesTheCurrentBackgroundPointBudget() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("grayOrc");
		Assert.assertTrue(character.addAvailablePerk("acrobat"));
		Assert.assertFalse(character.addAvailablePerk("acrobat"));

		character.getBackground().setCategoryPoint("outdoorEnvironment", true);
		Assert.assertFalse(character.addAvailablePerk("anxious"));
	}

	@Test
	public void skillSpecializationsAreExcludedFromTotalRanksButRecorded() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final LevelUp firstLevel = new LevelUp();
		firstLevel.setSkillRanks("softLeather", 5, false);
		character.getLevels().add(firstLevel);

		// Without any specialization selected, all bought ranks count.
		Assert.assertEquals(character.getSkillTotalRanks("softLeather"), Integer.valueOf(5));
		Assert.assertTrue(character.getSkillSpecializations("softLeather").isEmpty());

		// Selecting one of "softLeather"'s specialities (TA5/TA6) removes it from the
		// countable ranks:
		// it was bought as one of the 5 ranks, but it unlocks a specific armor training
		// number instead
		// of contributing to the skill's plain bonus.
		character.addSkillSpecialization("softLeather", "TA5");
		Assert.assertEquals(character.getSkillTotalRanks("softLeather"), Integer.valueOf(4));
		Assert.assertEquals(character.getSkillSpecializations("softLeather"), List.of("TA5"));

		// A specialization that does not belong to the skill is rejected.
		Assert.assertThrows(IllegalArgumentException.class,
				() -> character.addSkillSpecialization("softLeather", "TA9"));

		// Skills without declared specialities are unaffected.
		firstLevel.setSkillRanks("tracking", 3, false);
		Assert.assertEquals(character.getSkillTotalRanks("tracking"), Integer.valueOf(3));
		Assert.assertTrue(character.getSkillSpecializations("tracking").isEmpty());
	}

	@Test
	public void generalizingASkillIsMutuallyExclusiveWithSpecializingIt() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final LevelUp firstLevel = new LevelUp();
		character.getLevels().add(firstLevel);

		character.addSkillSpecialization("softLeather", "TA5");
		Assert.assertEquals(character.getSkillSpecializations("softLeather"), List.of("TA5"));
		Assert.assertFalse(character.isSkillGeneralized("softLeather"));

		// Generalizing the same skill clears its previously selected specializations.
		character.generalizeSkill("softLeather");
		Assert.assertTrue(character.isSkillGeneralized("softLeather"));
		Assert.assertTrue(character.getSkillSpecializations("softLeather").isEmpty());

		// Specializing it again clears the "generalized" mark back.
		character.addSkillSpecialization("softLeather", "TA6");
		Assert.assertFalse(character.isSkillGeneralized("softLeather"));
		Assert.assertEquals(character.getSkillSpecializations("softLeather"), List.of("TA6"));
	}

	@Test
	public void availableTrainingsExcludeAlreadySelectedOnes() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("horseCentaur");
		character.setProfessionId("fighter");

		final List<String> available = character.getAvailableTrainingIds();
		Assert.assertTrue(available.contains("soldier"));
		Assert.assertEquals(available.stream().sorted().toList(), available);

		character.getCurrentLevel().addTraining("soldier");
		Assert.assertFalse(character.getAvailableTrainingIds().contains("soldier"));
	}

	@Test
	public void availableTrainingsExcludeThoseOutsideTheDevelopmentPointBudget() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("horseCentaur");
		character.setProfessionId("fighter");
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.MEMORY, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.REASONING, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.SELF_DISCIPLINE, 500);
		Assert.assertTrue(character.getAvailableTrainingIds().contains("soldier"));

		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.MEMORY, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.REASONING, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.SELF_DISCIPLINE, 1);

		Assert.assertTrue(character.getRemainingDevelopmentPoints() < character.getTrainingDevelopmentCost("soldier"));
		Assert.assertFalse(character.getAvailableTrainingIds().contains("soldier"));
	}

	@Test
	public void addTrainingOnlySelectsAnAvailableTraining() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("horseCentaur");
		character.setProfessionId("fighter");

		Assert.assertTrue(character.addTraining("soldier"));
		Assert.assertEquals(character.getSelectedTrainingIds(), List.of("soldier"));
		Assert.assertFalse(character.addTraining("soldier"));
		Assert.assertFalse(character.addTraining("does-not-exist"));
	}

	@Test
	public void removingTrainingOnlyAffectsTheCurrentLevel() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("horseCentaur");
		character.setProfessionId("fighter");
		Assert.assertTrue(character.addTraining("soldier"));

		character.increaseLevel();
		Assert.assertFalse(character.removeCurrentLevelTraining("soldier"));
		Assert.assertEquals(character.getSelectedTrainingIds(), List.of("soldier"));

		character.getCurrentLevel().addTraining("scout");
		Assert.assertTrue(character.removeCurrentLevelTraining("scout"));
		Assert.assertEquals(character.getSelectedTrainingIds(), List.of("soldier"));
	}

	@Test
	public void spellCasterProfessionResolvesItsRealmAndSpellLists() throws InvalidXmlElementException {
		final CharacterPlayer nonCaster = new CharacterPlayer();
		nonCaster.setProfessionId("fighter");
		Assert.assertFalse(nonCaster.isSpellCaster());
		Assert.assertTrue(nonCaster.getRealmsOfMagic().isEmpty());
		Assert.assertTrue(nonCaster.getBasicSpellLists().isEmpty());

		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		Assert.assertTrue(wizard.isSpellCaster());

		// "wizard" has a single, fixed realm: no choice needed.
		final Profession profession = RulesCatalog.getInstance().getProfession("wizard");
		final List<RealmOfMagic> realms = wizard.applyProfessionMagicRealms(null);
		Assert.assertEquals(realms, List.of(RealmOfMagic.ESSENCE));
		Assert.assertEquals(wizard.getRealmsOfMagic(), List.of(RealmOfMagic.ESSENCE));

		// "Ley de la Luz" ("Law of Light") is one of wizard's own basic lists.
		final List<String> basicListIds = wizard.getBasicSpellLists().stream().map(list -> list.getId()).toList();
		Assert.assertTrue(basicListIds.contains("essenceLawOfLight"));
		Assert.assertEquals(wizard.classifySpellList("essenceLawOfLight"), MagicListType.BASIC);
		Assert.assertEquals(wizard.getSpellListDevelopmentCost("essenceLawOfLight", 0, 0),
				profession.getMagicCost(MagicListType.BASIC, 0).getRankCost(0));

		// "Barrera Contra Hechizos" is an open list of the Essence realm, not one of
		// wizard's own.
		Assert.assertFalse(basicListIds.contains("essenceBarrierAgainstSpells"));
		Assert.assertEquals(wizard.classifySpellList("essenceBarrierAgainstSpells"), MagicListType.OPEN);
		final List<String> openListIds = wizard.getOpenSpellLists().stream().map(list -> list.getId()).toList();
		Assert.assertTrue(openListIds.contains("essenceBarrierAgainstSpells"));

		// "Bridas de los Hechizos" is a closed list of the Essence realm.
		Assert.assertEquals(wizard.classifySpellList("essenceReinsOfTheSpells"), MagicListType.CLOSED);
		final List<String> closedListIds = wizard.getClosedSpellLists().stream().map(list -> list.getId()).toList();
		Assert.assertTrue(closedListIds.contains("essenceReinsOfTheSpells"));

		// "Destrucción de la Carne" is one of "sorcerer"'s own basic lists: for
		// "wizard" it is just
		// another real profession's list of the same (own) realm.
		Assert.assertEquals(wizard.classifySpellList("essenceDestructionOfTheFlesh"), MagicListType.OTHER_PROFESSION);
		final List<String> otherProfessionListIds = wizard.getOtherProfessionSpellLists().stream()
				.map(list -> list.getId()).toList();
		Assert.assertTrue(otherProfessionListIds.contains("essenceDestructionOfTheFlesh"));

		// A Mentalism-realm open list, a realm "wizard" does not have: classified as
		// belonging to
		// another realm rather than left unclassified.
		Assert.assertEquals(wizard.classifySpellList("mentalismSelfHealing"), MagicListType.OTHER_REALM_OPEN);
		final List<String> otherRealmOpenListIds = wizard.getOtherRealmOpenSpellLists().stream()
				.map(list -> list.getId()).toList();
		Assert.assertTrue(otherRealmOpenListIds.contains("mentalismSelfHealing"));
	}

	@Test
	public void darkSpellListsCanBeConfiguredAsBasicLists() throws InvalidXmlElementException {
		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		wizard.applyProfessionMagicRealms(null);

		Assert.assertFalse(wizard.getBasicSpellLists().stream().anyMatch(list -> list.isDarkList()));
		wizard.setDarkSpellsAsBasicListsAllowed(true);
		Assert.assertTrue(wizard.getBasicSpellLists().stream().anyMatch(list -> list.isDarkList()));
		final String darkListId = wizard.getBasicSpellLists().stream().filter(list -> list.isDarkList()).findFirst().orElseThrow().getId();
		Assert.assertEquals(wizard.classifySpellList(darkListId), MagicListType.BASIC);
	}

	@Test
	public void spellListRanksAccumulateAndSpendDevelopmentPoints() throws InvalidXmlElementException {
		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		wizard.applyProfessionMagicRealms(null);
		wizard.getCurrentLevel().setSpellListRanks("essenceLawOfLight", 2);

		final Profession profession = RulesCatalog.getInstance().getProfession("wizard");
		final int expectedFirstLevelCost = profession.getMagicCost(MagicListType.BASIC, 0).getRankCost(0)
				+ profession.getMagicCost(MagicListType.BASIC, 1).getRankCost(1);
		Assert.assertEquals(wizard.getSpellListTotalRanks("essenceLawOfLight"), Integer.valueOf(2));
		Assert.assertEquals(wizard.getSpentDevelopmentPoints(), Integer.valueOf(expectedFirstLevelCost));

		wizard.increaseLevel();
		wizard.getCurrentLevel().setSpellListRanks("essenceLawOfLight", 1);

		Assert.assertEquals(wizard.getSpellListTotalRanks("essenceLawOfLight"), Integer.valueOf(3));
		Assert.assertEquals(wizard.getSpentDevelopmentPoints(),
				wizard.getSpellListDevelopmentCost("essenceLawOfLight", 2, 0));
	}

	@Test
	public void sixthSpellListAcquiredInALevelDoublesItsDevelopmentCost() throws InvalidXmlElementException {
		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		wizard.applyProfessionMagicRealms(null);
		final List<String> lists = wizard.getOpenSpellLists().stream().map(list -> list.getId()).limit(6).toList();

		for (int index = 0; index < 5; index++) {
			wizard.getCurrentLevel().setSpellListRanks(lists.get(index), 1);
		}

		final Integer baseCost = RulesCatalog.getInstance().getProfession("wizard")
				.getMagicCost(MagicListType.OPEN, 0).getRankCost(0);
		Assert.assertEquals(wizard.getSpellListDevelopmentCost(lists.get(5), 0, 0),
				Integer.valueOf(baseCost * 2));
	}

	@Test
	public void maximumSpellListRanksComeFromTheActiveMagicCostBracket() throws InvalidXmlElementException {
		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		wizard.applyProfessionMagicRealms(null);

		final ProfessionMagicCost firstBracket = wizard.getProfession().getMagicCost(MagicListType.BASIC, 0);
		Assert.assertEquals(wizard.getMaximumSpellListRanksThisLevel("essenceLawOfLight"),
				firstBracket.getRankCosts().size());
		Assert.assertEquals(wizard.getMaximumSpellListRanksThisLevel("mentalismSelfHealing"), 0);
	}

	@Test
	public void settingSpellListRanksRespectsTheMagicLimitAndDevelopmentPointBudget() throws InvalidXmlElementException {
		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		wizard.applyProfessionMagicRealms(null);
		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 500);
		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 500);
		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.MEMORY, 500);
		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.REASONING, 500);
		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.SELF_DISCIPLINE, 500);

		Assert.assertTrue(wizard.setCurrentLevelSpellListRanks("essenceLawOfLight", 3));
		Assert.assertEquals(wizard.getCurrentLevel().getSpellListRanks("essenceLawOfLight"), Integer.valueOf(3));
		Assert.assertFalse(wizard.setCurrentLevelSpellListRanks("essenceLawOfLight", 4));

		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 1);
		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 1);
		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.MEMORY, 1);
		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.REASONING, 1);
		wizard.setCharacteristicTemporalValue(CharacteristicAbbreviation.SELF_DISCIPLINE, 1);
		Assert.assertFalse(wizard.setCurrentLevelSpellListRanks("essenceLawOfLight", 1));
		Assert.assertEquals(wizard.getCurrentLevel().getSpellListRanks("essenceLawOfLight"), Integer.valueOf(3));
	}

	@Test
	public void availableSpellListsRespectActiveMagicCostsAndRankLimits() throws InvalidXmlElementException {
		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		wizard.applyProfessionMagicRealms(null);
		Assert.assertTrue(wizard.getAvailableSpellListIds().contains("essenceLawOfLight"));
		Assert.assertEquals(wizard.getAvailableSpellListIds().stream().sorted().toList(), wizard.getAvailableSpellListIds());

		wizard.getCurrentLevel().setSpellListRanks("essenceLawOfLight",
				wizard.getMaximumSpellListRanksThisLevel("essenceLawOfLight"));
		Assert.assertFalse(wizard.getAvailableSpellListIds().contains("essenceLawOfLight"));
	}

	@Test
	public void elementalistTrainingUnlocksItsOwnAndItsTriadsSpellLists() throws InvalidXmlElementException {
		// "wizardOfTheAir" ("Mago del Aire") is one of the 3 shipped elementalist
		// trainings, part of
		// the "second triad" (with "lightWizard"); "fireWizard" is the lone shipped
		// member of the
		// "first" (complementary) triad. See ElementalTriad.
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("wizard");
		character.applyProfessionMagicRealms(null);
		final LevelUp firstLevel = new LevelUp();
		firstLevel.addTraining("wizardOfTheAir");
		character.getLevels().add(firstLevel);

		// The elementalist training's own exclusive list is one of the character's
		// basic lists.
		Assert.assertEquals(character.classifySpellList("essenceLawOfWind"), MagicListType.BASIC);
		final List<String> basicListIds = character.getBasicSpellLists().stream().map(list -> list.getId()).toList();
		Assert.assertTrue(basicListIds.contains("essenceLawOfWind"));

		// "lightWizard"'s exclusive list is of the same ("second") triad.
		Assert.assertEquals(character.classifySpellList("essenceMasteryOfLight"), MagicListType.TRIAD);
		final List<String> triadListIds = character.getTriadSpellLists().stream().map(list -> list.getId()).toList();
		Assert.assertTrue(triadListIds.contains("essenceMasteryOfLight"));

		// "fireWizard"'s exclusive list is of the complementary ("first") triad.
		Assert.assertEquals(character.classifySpellList("essencePathOfFlames"), MagicListType.COMPLEMENTARY_TRIAD);
		final List<String> complementaryTriadListIds = character.getComplementaryTriadSpellLists().stream()
				.map(list -> list.getId()).toList();
		Assert.assertTrue(complementaryTriadListIds.contains("essencePathOfFlames"));
	}

	@Test
	public void perCharacterOptionTogglesAreIndependentAndDefaultLikeLegacy() throws InvalidXmlElementException {
		// Matches the legacy ConfigTests: every toggle defaults like the legacy global
		// Config's own
		// hardcoded defaults (firearms/chi powers/other-realm-training-spells off,
		// magic on), and two
		// characters' toggles never affect each other.
		final CharacterPlayer character1 = new CharacterPlayer();
		Assert.assertFalse(character1.isFirearmsAllowed());
		Assert.assertFalse(character1.isChiPowersAllowed());
		Assert.assertFalse(character1.isOtherRealmTrainingSpellsAllowed());
		Assert.assertTrue(character1.isMagicAllowed());

		character1.setFirearmsAllowed(true);
		Assert.assertTrue(character1.isFirearmsAllowed());

		final CharacterPlayer character2 = new CharacterPlayer();
		Assert.assertFalse(character2.isFirearmsAllowed());
		character2.setFirearmsAllowed(false);
		Assert.assertTrue(character1.isFirearmsAllowed());
		Assert.assertFalse(character2.isFirearmsAllowed());
	}

	@Test
	public void skillGroupTogglesDisableChiAndFirearmSkills() throws InvalidXmlElementException {
		final Skill chiPower = RulesCatalog.getInstance().getSkill("chiPowerClothLance");
		final Skill firearmSkill = RulesCatalog.getInstance().getSkill("suppressionFire");
		final Skill standardSkill = RulesCatalog.getInstance().getSkill("climbing");

		final CharacterPlayer character = new CharacterPlayer();
		Assert.assertTrue(character.isSkillDisabledByOptions(chiPower));
		Assert.assertTrue(character.isSkillDisabledByOptions(firearmSkill));
		Assert.assertFalse(character.isSkillDisabledByOptions(standardSkill));

		character.setChiPowersAllowed(true);
		Assert.assertFalse(character.isSkillDisabledByOptions(chiPower));
		Assert.assertTrue(character.isSkillDisabledByOptions(firearmSkill));

		character.setFirearmsAllowed(true);
		Assert.assertFalse(character.isSkillDisabledByOptions(firearmSkill));
	}

	@Test
	public void magicAllowedToggleHidesEveryRealmOfMagic() throws InvalidXmlElementException {
		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		wizard.applyProfessionMagicRealms(null);
		Assert.assertEquals(wizard.getRealmsOfMagic(), List.of(RealmOfMagic.ESSENCE));
		Assert.assertFalse(wizard.getBasicSpellLists().isEmpty());

		wizard.setMagicAllowed(false);
		Assert.assertTrue(wizard.getRealmsOfMagic().isEmpty());
		Assert.assertTrue(wizard.getBasicSpellLists().isEmpty());
	}

	@Test
	public void otherRealmTrainingSpellsToggleHidesThatClassification() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("wizard");
		character.applyProfessionMagicRealms(null);
		final LevelUp firstLevel = new LevelUp();
		firstLevel.addTraining("wizardOfTheAir");
		character.getLevels().add(firstLevel);

		// Off by default (matching the legacy Config's own hardcoded default):
		// other-realm lists
		// owned by a selected training never classify, or show up in
		// getOtherRealmTrainingSpellLists.
		Assert.assertFalse(character.isOtherRealmTrainingSpellsAllowed());
		Assert.assertTrue(character.getOtherRealmTrainingSpellLists().isEmpty());
	}

	@Test
	public void professionCastingStyleIsClassifiedLikeLegacy() throws InvalidXmlElementException {
		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		wizard.applyProfessionMagicRealms(null);
		Assert.assertTrue(wizard.isPureWizard());
		Assert.assertFalse(wizard.isHybridWizard());
		Assert.assertFalse(wizard.isSemiWizard());
		Assert.assertTrue(wizard.isWizard());
		Assert.assertFalse(wizard.isFighter());

		final CharacterPlayer fighter = new CharacterPlayer();
		fighter.setProfessionId("fighter");
		fighter.applyProfessionMagicRealms(null);
		Assert.assertFalse(fighter.isWizard());
		Assert.assertTrue(fighter.isFighter());
	}

	@Test
	public void movementArmourAndDefensiveBonusAreQueryable() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		// No perks, no race: base values only.
		Assert.assertEquals(character.getMovementCapacity(), 15);
		Assert.assertEquals(character.getArmourClass(), 1);
		Assert.assertEquals(character.getDefensiveBonus(), 0);
	}

	@Test
	public void primeCharacteristicIsBumpedToAMinimumOfNinetyAtCreation() throws InvalidXmlElementException {
		final CharacterPlayer wizard = new CharacterPlayer();
		wizard.setProfessionId("wizard");
		final Profession profession = RulesCatalog.getInstance().getProfession("wizard");
		final CharacteristicAbbreviation primary = profession.getCharacteristicPreferences().get(0);
		final CharacteristicAbbreviation secondary = profession.getCharacteristicPreferences().get(1);
		final CharacteristicAbbreviation nonPreferred = java.util.Arrays.stream(CharacteristicAbbreviation.values())
				.filter(a -> a != primary && a != secondary && a != CharacteristicAbbreviation.NONE
						&& a != CharacteristicAbbreviation.REALM_OF_MAGIC && a != CharacteristicAbbreviation.APPEARANCE)
				.findFirst().orElseThrow();

		wizard.setCharacteristicInitialTemporalValue(primary, 45);
		Assert.assertEquals(wizard.getCharacteristicTemporalValue(primary), Integer.valueOf(90));

		wizard.setCharacteristicInitialTemporalValue(primary, 95);
		Assert.assertEquals(wizard.getCharacteristicTemporalValue(primary), Integer.valueOf(95));

		wizard.setCharacteristicInitialTemporalValue(nonPreferred, 45);
		Assert.assertEquals(wizard.getCharacteristicTemporalValue(nonPreferred), Integer.valueOf(45));
	}

	@Test
	public void totalDevelopmentPointsAveragesTheFiveGoverningCharacteristics() {
		final CharacterPlayer character = new CharacterPlayer();
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 100);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 90);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.MEMORY, 80);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.REASONING, 70);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.SELF_DISCIPLINE, 60);
		// Other characteristics do not count towards this total.
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.STRENGTH, 999);

		Assert.assertEquals(character.getTotalDevelopmentPoints(), Integer.valueOf((100 + 90 + 80 + 70 + 60) / 5));
	}

	@Test
	public void powerPointsUsesTheRacesOwnProgressionForEachRealmOfMagic() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("wizard");
		character.applyProfessionMagicRealms(null);
		character.setRaceId("horseCentaur");
		// horseCentaur's ppEssence is "0/6/5/4/3": 6 power points per rank up to 10.
		character.getCurrentLevel().setSkillRanks("powerPointDevelopment", 4, false);

		Assert.assertEquals(character.getPowerPoints(), 4 * 6);
		Assert.assertEquals(character.getPowerPointsDevelopmentCost(), List.of(0f, 6f, 5f, 4f, 3f));
	}

	@Test
	public void powerPointsIsZeroWithoutARaceOrProfessionSelected() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.getCurrentLevel().setSkillRanks("powerPointDevelopment", 4, false);

		Assert.assertEquals(character.getPowerPoints(), 0);
		Assert.assertTrue(character.getPowerPointsDevelopmentCost().isEmpty());
	}

	@Test
	public void spentDevelopmentPointsSumsCategoryRanksSkillRanksAndTrainings() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");

		final Category category = RulesCatalog.getInstance().getCategory("outdoorEnvironment");
		Assert.assertNotNull(category);
		character.getCurrentLevel().setCategoryRanks("outdoorEnvironment", 2);
		final Integer categoryCost = character.getCategoryDevelopmentCost("outdoorEnvironment", 0)
				+ character.getCategoryDevelopmentCost("outdoorEnvironment", 1);

		Assert.assertEquals(character.getSpentDevelopmentPoints(), categoryCost);

		Assert.assertEquals(character.getRemainingDevelopmentPoints(),
				Integer.valueOf(character.getTotalDevelopmentPoints() - categoryCost));
	}

	@Test
	public void maximumCategoryRanksComeFromTheProfessionCostTable() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");

		Assert.assertEquals(character.getMaximumCategoryRanksThisLevel("outdoorEnvironment"), 3);
		Assert.assertEquals(character.getMaximumCategoryRanksThisLevel("does-not-exist"), 0);
	}

	@Test
	public void settingCategoryRanksRespectsTheLimitAndDevelopmentPointBudget() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.MEMORY, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.REASONING, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.SELF_DISCIPLINE, 500);

		Assert.assertTrue(character.setCurrentLevelCategoryRanks("outdoorEnvironment", 3));
		Assert.assertEquals(character.getCurrentLevel().getCategoryRanks("outdoorEnvironment"), Integer.valueOf(3));
		Assert.assertFalse(character.setCurrentLevelCategoryRanks("outdoorEnvironment", 4));

		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.MEMORY, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.REASONING, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.SELF_DISCIPLINE, 1);
		Assert.assertFalse(character.setCurrentLevelCategoryRanks("outdoorEnvironment", 1));
		Assert.assertEquals(character.getCurrentLevel().getCategoryRanks("outdoorEnvironment"), Integer.valueOf(3));
	}

	@Test
	public void settingSkillRanksRespectsEnablementLimitAndDevelopmentPointBudget() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.MEMORY, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.REASONING, 500);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.SELF_DISCIPLINE, 500);

		Assert.assertTrue(character.setCurrentLevelSkillRanks("tracking", 3));
		Assert.assertEquals(character.getCurrentLevel().getSkillRanks("tracking"), Integer.valueOf(3));
		Assert.assertFalse(character.setCurrentLevelSkillRanks("tracking", 4));
		Assert.assertFalse(character.setCurrentLevelSkillRanks("chiPowerOfTheCrane", 1));

		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.MEMORY, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.REASONING, 1);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.SELF_DISCIPLINE, 1);
		Assert.assertFalse(character.setCurrentLevelSkillRanks("tracking", 1));
		Assert.assertEquals(character.getCurrentLevel().getSkillRanks("tracking"), Integer.valueOf(3));
	}

	@Test
	public void availableSkillsRespectEnablementOptionsAndRankLimits() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");
		Assert.assertTrue(character.getAvailableSkillIds().contains("tracking"));
		Assert.assertFalse(character.getAvailableSkillIds().contains("chiPowerOfTheCrane"));
		Assert.assertEquals(character.getAvailableSkillIds().stream().sorted().toList(), character.getAvailableSkillIds());

		character.getCurrentLevel().setSkillRanks("tracking", 3, false);
		Assert.assertFalse(character.getAvailableSkillIds().contains("tracking"));
	}

	@Test
	public void itemBonusTakesTheBestSingleMagicItemNotTheSum() {
		final CharacterPlayer character = new CharacterPlayer();
		final MagicObject weakRing = new MagicObject(new TranslatedText("Anillo débil", "Weak ring"), null,
				List.of(new ObjectBonus(BonusType.SKILL, "climbing", 5)));
		final MagicObject strongRing = new MagicObject(new TranslatedText("Anillo fuerte", "Strong ring"), null,
				List.of(new ObjectBonus(BonusType.SKILL, "climbing", 15)));
		character.addMagicItem(weakRing);
		character.addMagicItem(strongRing);

		// Magic items of the same kind do not stack: only the best one counts.
		Assert.assertEquals(character.getItemBonus(BonusType.SKILL, "climbing"), 15);
		Assert.assertEquals(character.getItemBonus(BonusType.SKILL, "unrelatedSkill"), 0);
		Assert.assertEquals(character.getAllMagicItems(), List.of(weakRing, strongRing));

		character.removeMagicItem(strongRing);
		Assert.assertEquals(character.getItemBonus(BonusType.SKILL, "climbing"), 5);
	}

	@Test
	public void itemBonusIsAddedOnTopOfDevelopmentBonusForTotals() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final Category category = new Category("climbing");
		category.setType(CategoryType.STANDARD);
		character.addMagicItem(new MagicObject(new TranslatedText("Botas", "Boots"), null,
				List.of(new ObjectBonus(BonusType.SKILL, "climbing", 10))));
		character.addMagicItem(new MagicObject(new TranslatedText("Cuerda", "Rope"), null,
				List.of(new ObjectBonus(BonusType.CATEGORY, "climbing", 20))));

		Assert.assertEquals(character.getSkillTotalBonus(category, "climbing"),
				Integer.valueOf(character.getSkillDevelopmentBonus(category, "climbing") + 10));
		Assert.assertEquals(character.getCategoryTotalBonus(category),
				Integer.valueOf(character.getCategoryDevelopmentBonus(category) + 20));
	}

	@Test
	public void categoryTotalBonusIncludesEachAssociatedCharacteristic() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 90);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.STRENGTH, 80);
		final Category category = new Category("athletics");
		category.setType(CategoryType.STANDARD);
		category.setCharacteristics(List.of(CharacteristicAbbreviation.AGILITY, CharacteristicAbbreviation.STRENGTH,
				CharacteristicAbbreviation.AGILITY));

		final int characteristicBonus = character.getCharacteristicTotalBonus(CharacteristicAbbreviation.AGILITY) * 2
				+ character.getCharacteristicTotalBonus(CharacteristicAbbreviation.STRENGTH);
		Assert.assertEquals(character.getCategoryCharacteristicBonus(category), Integer.valueOf(characteristicBonus));
		Assert.assertEquals(character.getCategoryTotalBonus(category),
				Integer.valueOf(character.getCategoryDevelopmentBonus(category) + characteristicBonus));
	}

	@Test
	public void defensiveBonusIncludesTheBestDefensiveMagicItem() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		final int withoutItem = character.getDefensiveBonus();
		character.addMagicItem(new MagicObject(new TranslatedText("Capa", "Cloak"), null,
				List.of(new ObjectBonus(BonusType.DEFENSIVE_BONUS, null, 15))));

		Assert.assertEquals(character.getDefensiveBonus(), withoutItem + 15);
	}

	@Test
	public void applyingATrainingSpecialItemAddsAMagicObjectOrPlainEquipment() {
		final Training training = new Training("scout");
		training.setSpecialItems(List.of(
				new TrainingSpecialItem(new TranslatedText("Botas buenas", "Good boots"), null, 30, 10,
						TrainingItemType.SKILL, "climbing"),
				new TrainingSpecialItem(new TranslatedText("Amigos", "Friends"), null, 20, 0, TrainingItemType.UNKNOWN,
						null)));
		final CharacterPlayer character = new CharacterPlayer();

		character.applyTrainingSpecialItem(training, 0);
		Assert.assertEquals(character.getAllMagicItems().size(), 1);
		Assert.assertEquals(character.getAllMagicItems().get(0).getSkillBonus("climbing"), 10);
		Assert.assertTrue(character.getAllNotMagicEquipment().isEmpty());

		character.applyTrainingSpecialItem(training, 1);
		Assert.assertEquals(character.getAllMagicItems().size(), 1);
		Assert.assertEquals(character.getAllNotMagicEquipment().size(), 1);
		Assert.assertEquals(character.getAllNotMagicEquipment().iterator().next().getName().getSpanish(), "Amigos");
	}
}
