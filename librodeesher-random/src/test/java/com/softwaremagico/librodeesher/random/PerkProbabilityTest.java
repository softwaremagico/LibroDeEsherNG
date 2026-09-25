package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Pins {@link PerkProbability} to the values the legacy {@code pj.random.PerkProbability}
 * heuristics produce for the id-based NG model. Every expectation below is the exact, manually
 * computed sum of the legacy bonus expressions, so a regression in any single term changes the
 * asserted value.
 *
 * <p>The characters use the {@code commonMen} race, whose NG background-point pool is 6 (the legacy
 * {@code Race#getPerksPoints()} is not shipped, see {@code PerkProbability}'s javadoc): a selected
 * weakness widens that pool the same way the legacy generator paired perks with weaknesses.</p>
 */
@Test(groups = "perkProbability")
public class PerkProbabilityTest {

	@Test
	public void magicalPerksAreExcludedWhenMagicIsDisabled() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		character.setRaceId("commonMen");
		character.setMagicAllowed(false);
		// sensibleToThePower is a MAGICAL-type perk: the type gate returns 0 before anything else.
		Assert.assertEquals(probability("sensibleToThePower", null, character), 0);
	}

	@Test
	public void suggestedPerksAreAcceptedStraightAway() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		character.setRaceId("commonMen");
		// A suggested perk short circuits even the affordability and allowed gates.
		Assert.assertEquals(probability("acrobat", List.of("acrobat"), character), 100);
	}

	@Test
	public void wizardsNeverTakeTheScepticPerk() throws InvalidXmlElementException {
		// Without a race the budget veto is masked by the "standard perk" fallback, but the sceptic
		// veto of {@code smartRandomness()} still applies to a wizard and vetoes it outright.
		Assert.assertEquals(probability("skeptic", null, freshWizard()), PerkProbability.VETO_PROBABILITY);
	}

	@Test
	public void affordablePerksWithoutABonusUseTheMovementTerm() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		character.setRaceId("commonMen");
		// speedDeslumbrante: cost 5 (affordable), grade MINOR -3, movement bonus 10/2 = 5 -> 2,
		// positive, so it no longer needs the "standard perk" fallback.
		Assert.assertEquals(probability("speedDeslumbrante", null, character), 2);
	}

	@Test
	public void perksWithoutInterestingSkillsFallBackToTheStandardValue() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		character.setRaceId("commonMen");
		character.addPerk("minorAddiction");
		// speedDeslumbrante with one perk already selected: -10 (perks count) - 3 (grade) + 5
		// (movement) = -8 < 0 and no skill/category contribution, so the "standard perk" fallback
		// 2 - 1 + 0 = 1 applies.
		Assert.assertEquals(probability("speedDeslumbrante", null, character), 1);
	}

	@Test
	public void unaffordablePerksFallBackToTheStandardValue() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		character.setRaceId("commonMen");
		// acrobat costs 7 but the commonMen pool is 6 and athleticsGymnastics is not interesting
		// for a fresh fighter, so the cost veto is replaced by the "standard perk" fallback
		// 2 - 0 + 0 = 2.
		Assert.assertEquals(probability("acrobat", null, character), 2);
	}

	@Test
	public void specializingInThePerkedCategoriesRaisesTheProbability() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		character.setRaceId("commonMen");
		character.addPerk("minorAddiction");
		character.setCurrentLevelCategoryRanks("martialArtsSweeps", 1);
		character.setCurrentLevelCategoryRanks("martialArtsStrikes", 1);
		// trainingInArtesMarcialesMinor grants a flat +10 to five categories; the two with ranks
		// (sweeps, strikes) contribute 20, capped at 15. Cost 10 is affordable thanks to the
		// weakness (spent -20): -10 (one perk selected) - 3 (grade) + 15 = 2.
		Assert.assertEquals(probability("trainingInArtesMarcialesMinor", null, character), 2);
	}

	@Test
	public void theSkillBonusTermIsCappedAndPenaltiesKeepCounting() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		character.setRaceId("commonMen");
		character.addPerk("minorAddiction");
		character.addPerk("slightAddiction");
		character.setCurrentLevelCategoryRanks("martialArtsSweeps", 1);
		character.setCurrentLevelCategoryRanks("martialArtsStrikes", 1);
		// Same perk, two weaknesses selected (spent -25): -20 (perks count) - 3 (grade) + 15
		// (capped skill bonus) = -8.
		Assert.assertEquals(probability("trainingInArtesMarcialesMinor", null, character), -8);
	}

	@Test
	public void similarPerksOfTheSameFamilyAreExcluded() throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		character.setRaceId("commonMen");
		// Both share the name basic "Adiestramiento en Artes Marciales", so the whole inner block
		// is skipped and the probability stays at 0.
		character.addPerk("trainingInArtesMarcialesMaximum");
		Assert.assertEquals(probability("trainingInArtesMarcialesMinor", null, character), 0);
	}

	private static int probability(String perkId, List<String> suggested, CharacterPlayer character)
			throws InvalidXmlElementException {
		final Perk perk = RulesCatalog.getInstance().getPerk(perkId);
		return new PerkProbability(character, perk, 0, suggested).getProbability();
	}

	private static CharacterPlayer freshFighter() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");
		character.applyProfessionMagicRealms(null);
		return character;
	}

	private static CharacterPlayer freshWizard() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("wizard");
		character.applyProfessionMagicRealms(null);
		return character;
	}
}