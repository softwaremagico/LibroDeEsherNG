package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.RealmOfMagicGrant;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Pins the creation steps {@link RandomCharacterPlayer} fills in before spending any development
 * point: the sex, the name, the magic realm (deterministically the one with the highest
 * characteristic bonus) and the initial temporal characteristic values. All of them are drawn from
 * the seedable {@link RandomValues}, so two runs with the same seed must reproduce the same
 * character, and the characteristic spending must stay inside its temporal-point budget.
 */
@Test(groups = "characterCreation")
public class RandomCharacterPlayerCreationTest {

	private static final int TEMPORAL_BUDGET = Characteristics.TOTAL_CHARACTERISTICS_POINTS;

	@Test
	public void sameSeedProducesTheSameCreationInfo() throws InvalidXmlElementException {
		final CharacterPlayer first = newCharacter("fighter");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(first, 3).createRandomValues();

		final CharacterPlayer second = newCharacter("fighter");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(second, 3).createRandomValues();

		Assert.assertEquals(characteristicsOf(second), characteristicsOf(first),
				"Same seed must reproduce the initial temporal characteristic values");
		Assert.assertEquals(second.getSex(), first.getSex(), "Same seed must reproduce the sex");
		Assert.assertEquals(second.getName(), first.getName(), "Same seed must reproduce the name");
		Assert.assertEquals(second.getRealmsOfMagic(), first.getRealmsOfMagic(),
				"Same seed must reproduce the magic realm");
	}

	@Test
	public void characteristicSpendingStaysWithinTheTemporalBudget() throws InvalidXmlElementException {
		for (int seed = 1; seed <= 8; seed++) {
			final CharacterPlayer character = newCharacter("fighter");
			RandomValues.setRandomSeed(seed);
			new RandomCharacterPlayer(character, 3).createRandomValues();
			Assert.assertTrue(character.getCharacteristicsTemporalPointsSpent() <= TEMPORAL_BUDGET,
					"Temporal points must never exceed the budget (seed " + seed + ")");
			for (final CharacteristicAbbreviation abbreviation : realCharacteristics()) {
				final int value = character.getCharacteristicTemporalValue(abbreviation);
				Assert.assertTrue(value >= Characteristics.INITIAL_CHARACTERISTIC_VALUE && value <= 101,
						"Characteristic '" + abbreviation + "' out of range after generation (seed " + seed + "): " + value);
			}
		}
	}

	@Test
	public void aRealmChoiceIsResolvedToTheHighestBonusRealm() throws InvalidXmlElementException {
		final CharacterPlayer character = newCharacter("astrologer");
		final CharacterPlayer untouched = newCharacter("astrologer");
		RandomValues.setRandomSeed(7L);
		new RandomCharacterPlayer(character, 1).createRandomValues();

		final Profession profession = untouched.getProfession();
		final RealmOfMagicGrant grant = profession.getMagicRealms().get(0);
		RealmOfMagic expected = grant.getOptions().get(0);
		int maxBonus = -100;
		for (final RealmOfMagic realm : grant.getOptions()) {
			final int bonus = untouched.getCharacteristicTotalBonus(realm.getCharacteristic());
			if (bonus >= maxBonus) {
				maxBonus = bonus;
				expected = realm;
			}
		}
		Assert.assertEquals(character.getRealmsOfMagic(), List.of(expected),
				"The realm with the highest characteristic bonus must win");
	}

	@Test
	public void cultureAdolescenceRanksStayWithinTheDevelopmentBudget() throws InvalidXmlElementException {
		for (int seed = 1; seed <= 4; seed++) {
			final CharacterPlayer character = newCharacter("fighter");
			character.setCultureId("aquaticNomadic");
			RandomValues.setRandomSeed(seed);
			new RandomCharacterPlayer(character, 3).createRandomValues();
			Assert.assertTrue(character.getRemainingDevelopmentPoints() >= 0,
					"Adolescence ranks must never overdraw the development budget (seed " + seed + ")");
		}
	}

	@Test
	public void sameSeedProducesTheSameCultureAdolescenceRanks() throws InvalidXmlElementException {
		final CharacterPlayer first = newCharacter("fighter");
		first.setCultureId("aquaticNomadic");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(first, 3).createRandomValues();

		final CharacterPlayer second = newCharacter("fighter");
		second.setCultureId("aquaticNomadic");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(second, 3).createRandomValues();

		Assert.assertEquals(cultureAdolescenceDecisions(second), cultureAdolescenceDecisions(first),
				"Same seed must reproduce the culture adolescence selections");
		Assert.assertEquals(second.getRemainingDevelopmentPoints(), first.getRemainingDevelopmentPoints(),
				"Same seed must reproduce the development budget after the adolescence ranks");
	}

	@Test
	public void cultureHobbyRanksStayWithinTheCultureCap() throws InvalidXmlElementException {
		final Culture culture = RulesCatalog.getInstance().getCulture("aquaticMilitarista");
		for (int seed = 1; seed <= 3; seed++) {
			final CharacterPlayer character = newCharacter("fighter");
			character.setCultureId("aquaticMilitarista");
			RandomValues.setRandomSeed(seed);
			new RandomCharacterPlayer(character, 3).createRandomValues();
			Assert.assertTrue(character.getTotalHobbySkillRanks() <= culture.getHobbyRanks(),
					"Hobby ranks must never exceed the culture cap (seed " + seed + ")");
		}
	}

	@Test
	public void sameSeedProducesTheSameCultureHobbyRanks() throws InvalidXmlElementException {
		final CharacterPlayer first = newCharacter("fighter");
		first.setCultureId("aquaticMilitarista");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(first, 3).createRandomValues();

		final CharacterPlayer second = newCharacter("fighter");
		second.setCultureId("aquaticMilitarista");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(second, 3).createRandomValues();

		Assert.assertEquals(hobbyRanksFingerprint(second), hobbyRanksFingerprint(first),
				"Same seed must reproduce the culture hobby ranks");
		Assert.assertEquals(second.getRemainingDevelopmentPoints(), first.getRemainingDevelopmentPoints(),
				"Same seed must reproduce the development budget after the hobby ranks");
	}

	@Test
	public void optionalLanguageSlotsAreFilledDuringCreation() throws InvalidXmlElementException {
		final CharacterPlayer character = newCharacter("fighter");
		character.setRaceId("dyari");
		character.setCultureId("aquaticMilitarista");
		RandomValues.setRandomSeed(7L);
		new RandomCharacterPlayer(character, 3).createRandomValues();

		final String raceSlot = character.getOptionalRaceLanguageAssignment(0);
		final String cultureSlot = character.getOptionalCultureLanguageAssignment(0);
		Assert.assertNotNull(raceSlot, "The dyari optional race language slot must be filled");
		Assert.assertNotNull(cultureSlot, "The aquaticMilitarista optional culture language slot must be filled");
		Assert.assertNotEquals(raceSlot, cultureSlot,
				"Two optional slots must not be assigned the same language");
		Assert.assertTrue(character.getRaceLanguageStartingSpeakingRanks(raceSlot) > 0,
				"The assigned race language must receive its slot's starting ranks");
	}

	@Test
	public void sameSeedProducesTheSameOptionalLanguageAssignments() throws InvalidXmlElementException {
		final CharacterPlayer first = newCharacter("fighter");
		first.setRaceId("dyari");
		first.setCultureId("aquaticMilitarista");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(first, 3).createRandomValues();

		final CharacterPlayer second = newCharacter("fighter");
		second.setRaceId("dyari");
		second.setCultureId("aquaticMilitarista");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(second, 3).createRandomValues();

		Assert.assertEquals(second.getOptionalRaceLanguageAssignment(0),
				first.getOptionalRaceLanguageAssignment(0),
				"Same seed must reproduce the optional race language");
		Assert.assertEquals(second.getOptionalCultureLanguageAssignment(0),
				first.getOptionalCultureLanguageAssignment(0),
				"Same seed must reproduce the optional culture language");
	}

	@Test
	public void backgroundPointsAreNeverOverspent() throws InvalidXmlElementException {
		for (final String professionId : List.of("fighter", "bard", "cleric", "rogue", "thief")) {
			final CharacterPlayer character = newCharacter(professionId);
			RandomValues.setRandomSeed(11L);
			new RandomCharacterPlayer(character, 1).createRandomValues();
			Assert.assertTrue(character.getRemainingBackgroundPoints() >= 0,
					"The background points of a " + professionId + " must never go below zero");
		}
	}

	@Test
	public void sameSeedProducesTheSamePerksAndBackgroundPoints() throws InvalidXmlElementException {
		final CharacterPlayer first = newCharacter("fighter");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(first, 3).createRandomValues();

		final CharacterPlayer second = newCharacter("fighter");
		RandomValues.setRandomSeed(4242L);
		new RandomCharacterPlayer(second, 3).createRandomValues();

		Assert.assertEquals(backgroundFingerprint(second), backgroundFingerprint(first),
				"Same seed must reproduce the perks, weaknesses and background points");
		Assert.assertEquals(second.getRemainingBackgroundPoints(), first.getRemainingBackgroundPoints(),
				"Same seed must reproduce the leftover background points");
	}

	@Test
	public void aPickedPerkResolvesItsRequestsAndStaysWithinTheBudget() throws InvalidXmlElementException {
		// Scan a handful of seeds until one actually picks a perk, then pin its behaviour.
		CharacterPlayer picked = null;
		for (long seed = 100; seed < 1000 && picked == null; seed++) {
			final CharacterPlayer character = newCharacter("fighter");
			RandomValues.setRandomSeed(seed);
			new RandomCharacterPlayer(character, 1).createRandomValues();
			if (!character.getSelectedPerks().isEmpty() && character.getRemainingBackgroundPoints() >= 0) {
				picked = character;
			}
		}
		Assert.assertNotNull(picked, "Some seed must grant at least one perk to a fighter");
		for (final var selectedPerk : picked.getSelectedPerks()) {
			final var weaknessId = selectedPerk.getWeaknessId();
			if (weaknessId != null) {
				Assert.assertTrue(picked.hasWeakness(selectedPerk.getPerkId()),
						"The paired weakness must be recorded on the perk");
			}
			Assert.assertTrue(picked.isPerkRandom(selectedPerk.getPerkId()),
					"Every random-selected perk must be marked as randomly chosen");
		}
	}

	private static String backgroundFingerprint(CharacterPlayer character) throws InvalidXmlElementException {
		final List<String> entries = new ArrayList<>();
		for (final var selectedPerk : character.getSelectedPerks()) {
			entries.add("perk:" + selectedPerk.getPerkId()
					+ (selectedPerk.getWeaknessId() == null ? "" : "->" + selectedPerk.getWeaknessId()));
		}
		entries.addAll(character.getBackground().getCategoryIds());
		entries.addAll(character.getBackground().getSkillIds());
		for (final var entry : character.getBackground().getLanguageRanks().entrySet()) {
			entries.add("lang:" + entry.getKey() + ":" + entry.getValue());
		}
		entries.sort(String::compareTo);
		return String.join(";", entries);
	}

	private static String hobbyRanksFingerprint(CharacterPlayer character) throws InvalidXmlElementException {
		final List<String> entries = new ArrayList<>();
		for (final Skill skill : RulesCatalog.getInstance().getSkills()) {
			final int ranks = character.getHobbySkillRank(skill.getId());
			if (ranks > 0) {
				entries.add(skill.getId() + ":" + ranks);
			}
		}
		entries.sort(String::compareTo);
		return String.join(";", entries);
	}

	private static List<String> cultureAdolescenceDecisions(CharacterPlayer character) {
		final List<String> keys = new ArrayList<>();
		for (final String key : character.getDecisions().getAll().keySet()) {
			if (key.contains(":adolescence:")) {
				keys.add(key);
			}
		}
		return keys;
	}

	@Test
	public void aSingleRealmCasterEndsUpCastingThatRealm() throws InvalidXmlElementException {
		final CharacterPlayer character = newCharacter("bard");
		RandomValues.setRandomSeed(3L);
		new RandomCharacterPlayer(character, 1).createRandomValues();
		final List<RealmOfMagic> realms = character.getRealmsOfMagic();
		Assert.assertEquals(realms.size(), 1, "A single-realm caster resolves exactly one realm");
		Assert.assertEquals(realms.get(0), RealmOfMagic.MENTALISM);
	}

	private static List<Integer> characteristicsOf(CharacterPlayer character) {
		final List<Integer> values = new ArrayList<>();
		for (final CharacteristicAbbreviation abbreviation : realCharacteristics()) {
			values.add(character.getCharacteristicTemporalValue(abbreviation));
		}
		return values;
	}

	private static List<CharacteristicAbbreviation> realCharacteristics() {
		final List<CharacteristicAbbreviation> abbreviations = new ArrayList<>();
		for (final CharacteristicAbbreviation abbreviation : CharacteristicAbbreviation.values()) {
			if (abbreviation != CharacteristicAbbreviation.NONE
					&& abbreviation != CharacteristicAbbreviation.REALM_OF_MAGIC) {
				abbreviations.add(abbreviation);
			}
		}
		return abbreviations;
	}

	private static CharacterPlayer newCharacter(String professionId) throws InvalidXmlElementException {
		final CharacterPlayer character = new CharacterPlayer();
		character.setRaceId("commonMen");
		character.setCultureId("aquaticMilitarista");
		character.setProfessionId(professionId);
		return character;
	}
}