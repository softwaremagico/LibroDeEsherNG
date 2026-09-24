package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Pins {@link CategoryProbability} to the values the legacy {@code pj.random.CategoryProbability}
 * heuristics produce for the id-based NG model. Every expectation below is the exact, manually
 * computed sum of the legacy bonus expressions (characteristic bonus, preferred category, cost
 * probability, common-skill bonus and smart bonus), so a regression in any single term changes the
 * asserted value.
 */
@Test(groups = "categoryProbability")
public class CategoryProbabilityTest {

	@Test
	public void vetoedCategoryIsNeverSelected() throws InvalidXmlElementException {
		final Category outdoorEnvironment = RulesCatalog.getInstance().getCategory("outdoorEnvironment");
		outdoorEnvironment.setNotUsedInRandom(true);
		try {
			final CategoryProbability probability = new CategoryProbability(freshFighter(), outdoorEnvironment,
					null, 1, 1);
			Assert.assertEquals(probability.rankProbability(), CategoryProbability.VETO_PROBABILITY);
		} finally {
			outdoorEnvironment.setNotUsedInRandom(false);
		}
	}

	@Test
	public void freshFighterRanksReproduceTheLegacyHeuristics() throws InvalidXmlElementException {
		// martialArtsStrikes: cost 3 -> 50 - 3^2*2 = 32; boxeo/placaje are common (+40),
		// common-or-professional skill push (+50) and the two empty-category pushes (+30 +30) give
		// smart 110: total 32 + 40 + 110 = 182, capped at 90.
		Assert.assertEquals(rankProbability("martialArtsStrikes", null), 90);
		// outdoorEnvironment: cost 2 -> 42; no common skills, no common-or-professional push:
		// 42 + 60 = 102, capped at 90.
		Assert.assertEquals(rankProbability("outdoorEnvironment", null), 90);
		// subterfugeAttack: cost 6 -> max(1, 50 - 6^2*2) = 1; no common skills, no common-orb
		// professional push; smart only the two empty-category pushes: 1 + 60 = 61 (no cap
		// involved).
		Assert.assertEquals(rankProbability("subterfugeAttack", null), 61);
	}

	@Test
	public void unaffordableCategoryIsNotSelected() throws InvalidXmlElementException {
		final CharacterPlayer poor = freshFighter();
		for (final CharacteristicAbbreviation abbreviation : new CharacteristicAbbreviation[] {
				CharacteristicAbbreviation.AGILITY, CharacteristicAbbreviation.CONSTITUTION,
				CharacteristicAbbreviation.MEMORY, CharacteristicAbbreviation.REASONING,
				CharacteristicAbbreviation.SELF_DISCIPLINE }) {
			poor.setCharacteristicTemporalValue(abbreviation, 10);
		}
		// With an average of 10 the development budget is 10 points, below the 12-point first rank.
		Assert.assertEquals(rankProbability("scienceAnalyticSpecialized", null, poor), 0);
	}

	@Test
	public void categoryWithoutProfessionCostIsNotSelected() throws InvalidXmlElementException {
		// weaponsEdged has no profession cost until a weapon tier is assigned.
		Assert.assertEquals(rankProbability("weaponsEdged", null), 0);
	}

	@Test
	public void suggestedCategoryAtTheStartOfTheLevelIsFullyMarked() throws InvalidXmlElementException {
		Assert.assertEquals(rankProbability("outdoorEnvironment", Map.of("outdoorEnvironment", 3)), 100);
	}

	@Test
	public void suggestedCategoryKeptEnforcedWhileTheCostStaysReasonable() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		// One rank already bought this level (cost 3), keeping 28 points: next rank costs 8.
		Assert.assertTrue(fighter.setCurrentLevelCategoryRanks("martialArtsStrikes", 1));
		Assert.assertEquals(rankProbability("martialArtsStrikes", Map.of("martialArtsStrikes", 5), fighter), 100);
	}

	@Test
	public void armourBeyondItsThresholdIsSkippedByTheSmartBonus() throws InvalidXmlElementException {
		final CharacterPlayer tank = freshFighter();
		// Characteristic bonus 6 per occurrence: AGILITY appears twice and STRENGTH once -> 18 in
		// armourLight, pushing its total value above the legacy threshold of 10 so the smart bonus
		// vetoes (-1000) the category: 18 + cost 48 - 1000 = -934.
		tank.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 92);
		tank.setCharacteristicTemporalValue(CharacteristicAbbreviation.STRENGTH, 92);
		Assert.assertEquals(rankProbability("armorLight", null, tank), -934);
	}

	private static int rankProbability(String categoryId, Map<String, Integer> suggested)
			throws InvalidXmlElementException {
		return rankProbability(categoryId, suggested, freshFighter());
	}

	private static int rankProbability(String categoryId, Map<String, Integer> suggested, CharacterPlayer character)
			throws InvalidXmlElementException {
		final Category category = RulesCatalog.getInstance().getCategory(categoryId);
		return new CategoryProbability(character, category, suggested, 1, 1).rankProbability();
	}

	private static CharacterPlayer freshFighter() {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");
		return character;
	}
}