package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Pins {@link ProfessionRandomness} to the legacy {@code pj.random.ProfessionRandomness} values for
 * the id-based NG model. The warrior paths and the general (adrenal/martial-arts) rules are covered
 * here; the BASIC/OPEN/CLOSED spell-list paths mirror the legacy expressions verbatim but NG data
 * has no individual spell skills yet (only spell lists), so they share the same preference contract
 * and stay uncovered until spells land.
 */
@Test(groups = "professionRandomness")
public class ProfessionRandomnessTest {

	@Test
	public void fighterPrefersPhysicalDevelopmentWhileItIsEmpty() throws InvalidXmlElementException {
		Assert.assertEquals(preferredSkillByProfession("physicalDevelopment"), 20);
	}

	@Test
	public void fighterInvestsInPowerPointDevelopmentOnlyUntilItsBonusMatchesTheOpenMaximum()
			throws InvalidXmlElementException {
		// Fresh: total bonus 0 is below the fighter's open/closed-list maximum (1), so it invests.
		final CharacterPlayer fighter = freshFighter();
		Assert.assertEquals(preferredSkillByProfession("powerPointDevelopment", fighter), 10);
		// Even after a rank the PPD bonus stays 0 (ranks grant power points, not a development
		// bonus), so the preference holds until the open-list maximum is even higher.
		Assert.assertTrue(fighter.setCurrentLevelSkillRanks("powerPointDevelopment", 1));
		Assert.assertEquals(preferredSkillByProfession("powerPointDevelopment", fighter), 10);
	}

	@Test
	public void adrenalSkillsArePenalisedForCharactersThatAreNotMonks() throws InvalidXmlElementException {
		Assert.assertEquals(preferredSkillByProfession("aguanteAdrenal"), ProfessionRandomness.ADRENAL_PENALTY);
	}

	@Test
	public void monksAreNotPenalisedForAdrenalSkills() throws InvalidXmlElementException {
		final CharacterPlayer monk = freshMonk();
		Assert.assertEquals(preferredSkillByProfession("aguanteAdrenal", monk), 0);
	}

	@Test
	public void categoriesWithPreferredSkillsShareTheBonus() throws InvalidXmlElementException {
		final CharacterPlayer fighter = freshFighter();
		Assert.assertEquals(ProfessionRandomness.preferredCategoryByProfession(fighter,
				RulesCatalog.getInstance().getCategory("physicalDevelopment"), 1), 20);
		Assert.assertEquals(ProfessionRandomness.preferredCategoryByProfession(fighter,
				RulesCatalog.getInstance().getCategory("selfControl"), 1), 0);
	}

	private static int preferredSkillByProfession(String skillId) throws InvalidXmlElementException {
		return preferredSkillByProfession(skillId, freshFighter());
	}

	private static int preferredSkillByProfession(String skillId, CharacterPlayer character)
			throws InvalidXmlElementException {
		return ProfessionRandomness.preferredSkillByProfession(character,
				RulesCatalog.getInstance().getSkill(skillId), 1);
	}

	private static CharacterPlayer freshFighter() throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("fighter");
		character.applyProfessionMagicRealms(null);
		return character;
	}

	private static CharacterPlayer freshMonk() {
		final CharacterPlayer character = new CharacterPlayer();
		character.setProfessionId("monk");
		return character;
	}
}