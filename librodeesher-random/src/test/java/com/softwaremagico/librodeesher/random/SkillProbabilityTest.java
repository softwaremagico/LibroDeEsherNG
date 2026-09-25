package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Pins {@link SkillProbability} to the values the legacy {@code pj.random.SkillProbability}
 * heuristics produce for the id-based NG model. Every expectation below is the exact, manually
 * computed sum of the legacy bonus expressions, so a regression in any single term changes the
 * asserted value.
 */
@Test(groups = "skillProbability")
public class SkillProbabilityTest {

	@Test
	public void commonSkillsOfAFreshFighterReproduceTheLegacyHeuristics() throws InvalidXmlElementException {
		// boxeo (martialArtsStrikes, cost 3): stillNotUsed 0 (1 - 3 < 0), skillExpensiveness
		// max(1, 30 - 3^2*3) = 3, bestSkills common max(0, 75 - 3*20) = 15 -> 18.
		Assert.assertEquals(rankProbability("boxeo", null), 18);
		// frenzy (selfControl, cost 2): stillNotUsed 0 (1 - 2 < 0), skillExpensiveness
		// 30 - 2^2*3 = 18, bestSkills common 75 - 2*20 = 35 -> 53.
		Assert.assertEquals(rankProbability("frenzy", null), 53);
		// hablarHablaCommon (communication, cost 3): neither common nor professional for the
		// fighter, so only skillExpensiveness 3 minus the -40 of the untouched communication
		// category -> -37.
		Assert.assertEquals(rankProbability("hablarHablaCommon", null), -37);
	}

	@Test
	public void disabledByOptionsSkillsAreNeverSelected() throws InvalidXmlElementException {
		// Chi powers are not allowed by default, vetoing the whole CHI skill group.
		Assert.assertEquals(rankProbability("chiPowerFistElementalCold", null), -200);
	}

	@Test
	public void rareSkillsNotCommonForTheCharacterAreNeverSelected() throws InvalidXmlElementException {
		Assert.assertEquals(rankProbability("xenoLore", null), -200);
	}

	@Test
	public void weaponSkillsWithoutAnAssignedTierAreNotSelected() throws InvalidXmlElementException {
		// weaponsEdged has no category cost until a weapon tier is assigned.
		Assert.assertEquals(rankProbability("scimitar", null), 0);
	}

	@Test
	public void suggestedSkillAtTheStartOfTheLevelIsFullyMarked() throws InvalidXmlElementException {
		Assert.assertEquals(rankProbability("frenzy", Map.of("frenzy", 6)), 100);
	}

	@Test
	public void suggestedSkillKeptEnforcedWhileTheCostStaysReasonable() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		// One rank bought this level (cost 2): the next rank still costs 6, far below 40.
		Assert.assertTrue(fighter.setCurrentLevelSkillRanks("frenzy", 1));
		Assert.assertEquals(rankProbability("frenzy", Map.of("frenzy", 6), fighter), 100);
	}

	@Test
	public void favouriteSkillGetsItsExtraBonus() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		fighter.addFavouriteSkill("boxeo");
		Assert.assertEquals(rankProbability("boxeo", null, fighter), 43);
	}

	private static int rankProbability(String skillId, Map<String, Integer> suggested)
			throws InvalidXmlElementException {
		return rankProbability(skillId, suggested, freshFighter());
	}

	private static int rankProbability(String skillId, Map<String, Integer> suggested, CharacterPlayer character)
			throws InvalidXmlElementException {
		final Skill skill = RulesCatalog.getInstance().getSkill(skillId);
		return new SkillProbability(character, skill, suggested, 1, 1).getRankProbability();
	}

	private static CharacterPlayer freshFighter() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");
		character.applyProfessionMagicRealms(null);
		return character;
	}
}