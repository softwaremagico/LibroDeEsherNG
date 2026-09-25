package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.rules.RulesCatalog;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Pins {@link RandomCharacterPlayer}'s development core: deterministic under a fixed {@link
 * RandomValues} seed, never over-drawing the development budget, honouring the requested final
 * level, and assigning the profession's weapon-cost tiers to real weapon categories.
 *
 * <p>Every character used here is a fresh fighter (the same helper as {@code
 * TrainingProbabilityTest}, plus a race and a culture for the available-training lookups), so the
 * roll space is the standard 51-training pool against a 31-point development budget.</p>
 */
@Test(groups = "characterRandom")
public class RandomCharacterPlayerTest {

	@Test
	public void sameSeedProducesTheSameDevelopmentResult() throws InvalidXmlElementException {
		final CharacterPlayer first = freshFighter();
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(first, 3).createRandomValues();

		final CharacterPlayer second = freshFighter();
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(second, 3).createRandomValues();

		Assert.assertEquals(snapshot(second), snapshot(first), "Same seed must reproduce the character");
		Assert.assertEquals(second.getStandardEquipment().size(), first.getStandardEquipment().size(),
				"Same seed must reproduce the training special items");
	}

	@Test
	public void developmentBudgetIsNeverOverdrawn() throws InvalidXmlElementException {
		for (int seed = 1; seed <= 8; seed++) {
			final CharacterPlayer character = freshFighter();
			RandomValues.setRandomSeed(seed);
			new RandomCharacterPlayer(character, 3).createRandomValues();
			Assert.assertTrue(character.getRemainingDevelopmentPoints() >= 0,
					"Remaining development points must never go negative (seed " + seed + ")");
			Assert.assertTrue(character.getRemainingDevelopmentPoints() <= 31,
					"A fresh character cannot have more points than its budget");
		}
	}

	@Test
	public void characterKeepsGrowingToTheRequestedFinalLevel() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		RandomValues.setRandomSeed(7L);
		new RandomCharacterPlayer(character, 3).createRandomValues();
		Assert.assertEquals(character.getLevels().size(), 3);

		final List<String> catalogIds = new ArrayList<>();
		for (final com.softwaremagico.librodeesher.training.Training training : RulesCatalog.getInstance()
				.getTrainings()) {
			catalogIds.add(training.getId());
		}
		for (final String trainingId : character.getSelectedTrainingIds()) {
			Assert.assertTrue(catalogIds.contains(trainingId), "Selected training must exist in the catalog");
		}
	}

	@Test
	public void anAffordableSuggestedTrainingIsSelectedForItsWholeCost() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		// fighter prices soldier at 15 out of the 31 fresh points; the category/skill ranks the
		// training grants also count against the current-level budget (legacy semantics), so at
		// least those 15 are gone.
		final int remainingBefore = character.getRemainingDevelopmentPoints();
		RandomCharacterPlayer.setRandomTraining(character, "soldier", 0);
		Assert.assertTrue(character.getSelectedTrainingIds().contains("soldier"));
		Assert.assertTrue(remainingBefore - character.getRemainingDevelopmentPoints() >= 15,
				"selecting soldier must spend its 15-point cost");
		Assert.assertTrue(character.getRemainingDevelopmentPoints() >= 0);
	}

	@Test
	public void anUnaffordableTrainingIsNotSelected() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		// houri costs 36 > 31: never added.
		RandomCharacterPlayer.setRandomTraining(character, "houri", 0);
		Assert.assertFalse(character.getSelectedTrainingIds().contains("houri"));
		Assert.assertEquals(character.getRemainingDevelopmentPoints(), 31);
	}

	@Test
	public void weaponCostTiersAreAssignedToRealWeaponCategories() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		RandomValues.setRandomSeed(99L);
		new RandomCharacterPlayer(character, 2).createRandomValues();
		final int tiers = character.getProfession().getWeaponCategoryCostTiers().size();
		Assert.assertTrue(tiers > 0, "a fighter should have weapon-cost tiers");
		for (int tier = 0; tier < tiers; tier++) {
			Assert.assertTrue(character.isWeaponCategoryCostTierAssigned(tier),
					"tier " + tier + " should be assigned to a weapon category");
			final String selected = character.getDecisions().getSelectedOption("weaponCostTier:" + tier);
			Assert.assertNotNull(selected, "tier " + tier + " must have a decision");
			Assert.assertTrue(selected.startsWith("weapons"), "tier " + tier + " selected '" + selected + "'");
		}
	}

	@Test
	public void suggestedRanksAreRememberedUntilCleared() {
		final RandomCharacterPlayer random = new RandomCharacterPlayer(freshFighterChecked());
		random.setSuggestedSkillRanks("running", 3);
		Assert.assertEquals(random.getSuggestedSkillsRanks().get("running"), Integer.valueOf(3));
		random.setSuggestedSkillRanks("running", 0);
		Assert.assertFalse(random.getSuggestedSkillsRanks().containsKey("running"));
		random.setSuggestedCategoryRanks("perceptionSenses", 2);
		Assert.assertEquals(random.getSuggestedCategoriesRanks().get("perceptionSenses"), Integer.valueOf(2));
		random.setSuggestedCategoryRanks("perceptionSenses", -1);
		Assert.assertFalse(random.getSuggestedCategoriesRanks().containsKey("perceptionSenses"));
		random.setSpecializationLevel(null);
		Assert.assertEquals(random.getSpecializationLevel(), 0);
	}

	private static CharacterPlayer freshFighterChecked() {
		try {
			return freshFighter();
		} catch (final InvalidXmlElementException e) {
			throw new IllegalStateException(e);
		}
	}

	private static CharacterPlayer freshFighter() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("commonMen");
		character.setCultureId("aquaticMilitarista");
		character.setProfessionId("fighter");
		character.applyProfessionMagicRealms(null);
		return character;
	}

	/** Deterministic view of everything the development core decides, for cross-run comparison. */
	private static String snapshot(CharacterPlayer character) throws InvalidXmlElementException {
		final StringBuilder snapshot = new StringBuilder();
		snapshot.append("remaining=").append(character.getRemainingDevelopmentPoints()).append('\n');
		snapshot.append("trainings=").append(character.getSelectedTrainingIds()).append('\n');
		for (final LevelUp level : character.getLevels()) {
			final List<String> categories = new ArrayList<>(level.getCategoryRanks().keySet());
			categories.sort(String::compareTo);
			for (final String id : categories) {
				snapshot.append("category:").append(id).append('=').append(level.getCategoryRanks().get(id)).append('\n');
			}
			final List<String> skills = new ArrayList<>(level.getSkillRanks().keySet());
			skills.sort(String::compareTo);
			for (final String id : skills) {
				snapshot.append("skill:").append(id).append('=').append(level.getSkillRanks().get(id)).append('\n');
			}
		}
		final int tiers = character.getProfession().getWeaponCategoryCostTiers().size();
		for (int tier = 0; tier < tiers; tier++) {
			if (character.isWeaponCategoryCostTierAssigned(tier)) {
				snapshot.append("weaponCostTier:").append(tier).append('=')
						.append(character.getDecisions().getSelectedOption("weaponCostTier:" + tier)).append('\n');
			}
		}
		return snapshot.toString();
	}
}