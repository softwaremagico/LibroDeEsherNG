package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
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

	@Test
	public void famousListsMapRealmAndIdToTheirLegacyBase() {
		// Essence: Shield and Quickness. Mentalism: Dodge, Auto-Health and Speed.
		Assert.assertEquals(SkillProbability.famousListBase(List.of(RealmOfMagic.ESSENCE), "essenceMasteryOfEscudos"), 50);
		Assert.assertEquals(SkillProbability.famousListBase(List.of(RealmOfMagic.ESSENCE), "essencePathsOfQuickness"), 20);
		Assert.assertEquals(SkillProbability.famousListBase(List.of(RealmOfMagic.MENTALISM), "mentalismEvasionOfTheAttacks"), 50);
		Assert.assertEquals(SkillProbability.famousListBase(List.of(RealmOfMagic.MENTALISM), "mentalismSelfHealing"), 30);
		Assert.assertEquals(SkillProbability.famousListBase(List.of(RealmOfMagic.MENTALISM), "mentalismSpeed"), 20);
		// The other realm's lists, and any other category, get no famous bonus.
		Assert.assertEquals(SkillProbability.famousListBase(List.of(RealmOfMagic.MENTALISM), "essenceMasteryOfEscudos"), 0);
		Assert.assertEquals(SkillProbability.famousListBase(List.of(RealmOfMagic.CANALIZATION, RealmOfMagic.ESSENCE), "mentalismSpeed"), 0);
		Assert.assertEquals(SkillProbability.famousListBase(List.of(RealmOfMagic.ESSENCE), "boxeo"), 0);
	}

	@Test
	public void mountSkillsRespectTheRaceRestrictions() throws InvalidXmlElementException {
		// The veto lowers the very same base by exactly -200: wolves only for orcs, horses only for
		// non-orcs, bears only for dwarves.
		final CharacterPlayer orc = freshFighterWithRace("commonOrcs");
		final CharacterPlayer human = freshFighterWithRace("highMen");
		final CharacterPlayer dwarf = freshFighterWithRace("dwarf");
		Assert.assertEquals(rankProbability("montarLobos", null, orc),
				rankProbability("montarLobos", null, human) + 200);
		Assert.assertEquals(rankProbability("montarCaballos", null, human),
				rankProbability("montarCaballos", null, orc) + 200);
		Assert.assertEquals(rankProbability("montarOsos", null, dwarf),
				rankProbability("montarOsos", null, human) + 200);
	}

	@Test
	public void genericKnowledgeSkillsAreNotVetoedAsForeign() throws InvalidXmlElementException {
		// The migrated knowledge skills carry no culture, so they belong to every culture and a
		// foreign-knowledge veto (which would make the probability negative) never fires.
		final CharacterPlayer fighter = freshFighter();
		for (final String skillId : new String[] { "loreRegional", "loreCultural", "loreOfFauna", "loreOfFlora" }) {
			Assert.assertTrue(rankProbability(skillId, null, fighter) >= 0, skillId + " must not be vetoed");
		}
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

	private static CharacterPlayer freshFighterWithRace(String raceId) throws InvalidXmlElementException {
		final CharacterPlayer character = freshFighter();
		character.setRaceId(raceId);
		return character;
	}
}