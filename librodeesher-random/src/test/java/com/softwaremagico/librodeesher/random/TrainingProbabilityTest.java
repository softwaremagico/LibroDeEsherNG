package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.ElementalTriad;
import com.softwaremagico.librodeesher.rules.RulesCatalog;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Pins {@link TrainingProbability} to the values the legacy {@code pj.random.TrainingProbability}
 * heuristics produce for the id-based NG model. Every expectation below is the exact, manually
 * computed sum of the legacy bonus expressions, so a regression in any single term changes the
 * asserted value.
 *
 * <p>The characters are professors with fresh development budgets of 31 points, so a training's cost
 * is only ever gated by affordability when it exceeds that budget (or when a previous selection
 * already spent points on it).</p>
 */
@Test(groups = "trainingProbability")
public class TrainingProbabilityTest {

	@Test
	public void trainingsOtherProfessionsDoNotPriceAreScoredZero() throws InvalidXmlElementException {
		// fireWizard is only priced for elementalist professions, so a fighter has no cost for it
		// at all (the legacy INVALID_COST-200 sentinel, always over budget).
		Assert.assertEquals(score("fireWizard", null, 0, 1, freshFighter()), 0);
	}

	@Test
	public void tooExpensiveTrainingsAreScoredZero() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		Assert.assertEquals(score("houri", null, 0, 1, fighter), 0);
		// Suggested trainings do not bypass the affordability gate: 43 > 31 budget.
		Assert.assertEquals(score("noviceWizard", List.of("noviceWizard"), 0, 1, fighter), 0);
	}

	@Test
	public void standardTrainingsScoreTheBaseFormula() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		// soldier: cost 15 -> (28-15)*1.5 + 1 (levels) = 20.5 -> 20, no bonuses, /1.
		Assert.assertEquals(score("soldier", null, 0, 1, fighter), 20);
		// traveler: cost 13 -> (28-13)*1.5 + 1 = 23.5 -> 23.
		Assert.assertEquals(score("traveler", null, 0, 1, fighter), 23);
		// friendOfTheBeasts: cost 26 -> (28-26)*1.5 + 1 = 4.
		Assert.assertEquals(score("friendOfTheBeasts", null, 0, 1, fighter), 4);
	}

	@Test
	public void favouriteTrainingsGetAPlusFifteen() throws InvalidXmlElementException {
		// guard costs 15 (STANDARD value) but is FAVOURITE for a fighter: 20 + 15.
		Assert.assertEquals(score("guard", null, 0, 1, freshFighter()), 35);
	}

	@Test
	public void suggestedTrainingsAreAcceptedStraightAway() throws InvalidXmlElementException {
		// An affordable suggested training on an empty current level short circuits to 100.
		Assert.assertEquals(score("soldier", List.of("soldier"), 0, 1, freshFighter()), 100);
	}

	@Test
	public void suggestedTrainingsControlTheRemainingLevels() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		fighter.addTraining("soldier");
		// soldier is already selected, so it drops out of the suggestions; the other two are
		// affordable and the suggestion list (3 entries) is bigger than finalLevel - levels (3-1),
		// so they short circuit to 100. friendOfTheBeasts costs 26 > 16 remaining and is gated out.
		Assert.assertEquals(score("guard", List.of("guard", "traveler", "bandit"), 0, 3, fighter), 100);
		Assert.assertEquals(score("traveler", List.of("guard", "traveler", "bandit"), 0, 3, fighter), 100);
		Assert.assertEquals(score("bandit", List.of("guard", "traveler", "bandit"), 0, 3, fighter), 0);
	}

	@Test
	public void selectedTrainingsArePenalisedAndTheDivisorGrows() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		fighter.addTraining("soldier");
		// soldier: back to the base formula with 1 selected pupil: 20.5 - 25 = -4.5 -> -4, divided by
		// (1 current + 1).
		Assert.assertEquals(score("soldier", null, 0, 1, fighter), -2);
		// traveler: 23.5 - 25 = -1.5 -> -1, /2 = 0.
		Assert.assertEquals(score("traveler", null, 0, 1, fighter), 0);
		// friendOfTheBeasts now costs 26 > 16 remaining.
		Assert.assertEquals(score("friendOfTheBeasts", null, 0, 1, fighter), 0);
		// guard: -4 + 15 (favourite) = 11, /2 = 5.
		Assert.assertEquals(score("guard", null, 0, 1, fighter), 5);
	}

	@Test
	public void aCultureMentioningTheTrainingAddsItsDiscountAndBonus() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		fighter.setCultureId("aquaticMilitarista");
		// soldier costs 15 but aquaticMilitarista prices it at 0.75 -> cost 12 (ceil).
		// (28-12)*1.5 + 1 = 25, +10 for the culture's favourite-training bonus.
		Assert.assertEquals(score("soldier", null, 0, 1, fighter), 35);
	}

	@Test
	public void forbiddenTrainingsAreVetoed() throws InvalidXmlElementException {
		// alchemistOfChanneling forbids soldier (cost 25): (28-25)*1.5 + 1 = 5.5 -> 5, -1500.
		Assert.assertEquals(score("soldier", null, 0, 1, freshAlchemist()), -1495);
	}

	@Test
	public void elementalistMustTakeItsOwnTrainingFirst() throws InvalidXmlElementException {
		// The elementalist profession prices fireWizard at 29 (affordable), the profession is
		// elementalist and no training was selected yet: forced to 1000.
		Assert.assertEquals(score("fireWizard", null, 0, 1, freshElementalist()), 1000);
	}

	@Test
	public void shuffleTrainingsOrdersTheSuggestedOnesFirst() throws InvalidXmlElementException {
		final List<String> shuffled = TrainingProbability.shuffleTrainings(freshFighter(),
				List.of("soldier", "guard"));
		// The legacy loop inserts each suggestion at the front, reversing their order.
		Assert.assertEquals(shuffled.get(0), "guard");
		Assert.assertEquals(shuffled.get(1), "soldier");
		// Both suggestions were already in the pool, and nothing is selected yet.
		Assert.assertEquals(shuffled.size(), rulesCount());
	}

	@Test
	public void shuffleTrainingsRemovesTheAlreadyAcquiredOnes() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		fighter.addTraining("soldier");
		final List<String> shuffled = TrainingProbability.shuffleTrainings(fighter, null);
		Assert.assertFalse(shuffled.contains("soldier"));
		Assert.assertEquals(shuffled.size(), rulesCount() - 1);
	}

	@Test
	public void shuffleTrainingsOrdersElementalistTrainingsFirstForElementalists() throws InvalidXmlElementException {
		final List<String> shuffled = TrainingProbability.shuffleTrainings(freshElementalist(), null);
		Assert.assertTrue(ElementalTriad.isElementalistTraining(shuffled.get(0)));
	}

	private static int score(String trainingId, List<String> suggested, int specialization, int finalLevel,
			CharacterPlayer character) throws InvalidXmlElementException {
		return TrainingProbability.trainingRandomness(character, trainingId, specialization, suggested, finalLevel);
	}

	private static int rulesCount() throws InvalidXmlElementException {
		return RulesCatalog.getInstance().getTrainings().size();
	}

	private static CharacterPlayer freshFighter() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");
		character.applyProfessionMagicRealms(null);
		return character;
	}

	private static CharacterPlayer freshAlchemist() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("alchemistOfChanneling");
		character.applyProfessionMagicRealms(null);
		return character;
	}

	private static CharacterPlayer freshElementalist() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("elementalist");
		character.applyProfessionMagicRealms(null);
		return character;
	}
}