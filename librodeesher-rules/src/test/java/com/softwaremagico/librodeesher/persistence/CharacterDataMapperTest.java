package com.softwaremagico.librodeesher.persistence;

import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.character.SexType;
import com.softwaremagico.librodeesher.characteristic.Appearance;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.decision.Decision;
import com.softwaremagico.librodeesher.dice.Roll;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.equipment.ObjectBonus;
import com.softwaremagico.librodeesher.language.TranslatedText;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifies {@link CharacterDataMapper} snapshots a character into its flat DTO and rebuilds it
 * through the model's own mutators without losing or inventing state.
 */
@Test(groups = "persistence")
public class CharacterDataMapperTest {

	@Test
	public void roundTripsEveryFieldOfAFullyPopulatedCharacter() {
		final CharacterPlayer original = newFullyPopulatedCharacter();
		final CharacterPlayer restored = CharacterDataMapper.toCharacter(CharacterDataMapper.toData(original));
		assertCharactersEqual(original, restored);
	}

	@Test
	public void roundTripsAnEmptyCharacter() {
		final CharacterPlayer original = new CharacterPlayer();
		final CharacterPlayer restored = CharacterDataMapper.toCharacter(CharacterDataMapper.toData(original));
		Assert.assertEquals(restored.getLevel(), 1);
		assertCharactersEqual(original, restored);
	}

	@Test
	public void snapshotMapsAreKeySortedForStableOutput() {
		final CharacterData data = CharacterDataMapper.toData(newFullyPopulatedCharacter());
		Assert.assertTrue(isSorted(new ArrayList<>(data.getCharacteristicTemporalValues().keySet())));
		Assert.assertTrue(isSorted(new ArrayList<>(data.getCharacteristicPotentialValues().keySet())));
		Assert.assertTrue(isSorted(new ArrayList<>(data.getDecisions().keySet())));
		Assert.assertTrue(isSorted(new ArrayList<>(data.getHobbySkillRanks().keySet())));
		Assert.assertTrue(isSorted(new ArrayList<>(data.getHobbySpellListRanks().keySet())));
		Assert.assertTrue(isSorted(new ArrayList<>(data.getBackground().getLanguageRanks().keySet())));
		Assert.assertTrue(isSorted(new ArrayList<>(data.getLevels().get(0).getCategoryRanks().keySet())));
	}

	static CharacterPlayer newFullyPopulatedCharacter() {
		final CharacterPlayer character = new CharacterPlayer();
		character.setName("Ánforo Élfico");
		character.setSex(SexType.MALE);
		character.setRaceId("commonMen");
		character.setCultureId("aquaticMilitarista");
		character.setProfessionId("fighter");
		character.setHistoryText("Trasfondo con acentos: áéíóú ñ.");

		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.STRENGTH, 60);
		character.setCharacteristicPotentialValue(CharacteristicAbbreviation.STRENGTH, 65);
		character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 50);
		character.setCharacteristicPotentialValue(CharacteristicAbbreviation.AGILITY, 52);
		character.setCharacteristicsAsConfirmed();
		character.setAppearance(new Appearance(37));
		character.setCurrentAge(25);
		character.setFinalAge(140);

		final LevelUp firstLevel = character.getCurrentLevel();
		firstLevel.setCategoryRanks("arms", 3);
		firstLevel.setSkillRanks("sword", 2, false);
		firstLevel.setSpellListRanks("defensive", 1);
		firstLevel.setGeneralizedSkills(new HashSet<>(List.of("ng_healing")));
		firstLevel.setSpellsUpdated(new ArrayList<>(List.of("fire_ball")));
		firstLevel.setTrainings(new ArrayList<>(List.of("martialArtist")));
		firstLevel.setSkillSpecializations(new HashSet<>(List.of("sword_throwing")));
		firstLevel.setFavouriteSkills(new HashSet<>(List.of("sword")));
		firstLevel.addCharacteristicUpdate(CharacteristicAbbreviation.STRENGTH, 60, 65, Roll.of(4, 6));
		firstLevel.getAgeModifications().add(new AgeModification(25, 2));

		final LevelUp secondLevel = character.increaseLevel();
		secondLevel.setSkillRanks("boxing", 1, false);

		character.getBackground().setCategoryIds(new ArrayList<>(List.of("soft_arms")));
		character.getBackground().setSkillIds(new ArrayList<>(List.of("running")));
		character.getBackground().addCharacteristicUpdate(CharacteristicAbbreviation.AGILITY, 50, 52, Roll.of(5, 5));
		character.getBackground().getLanguageRanks().put("human_language", 2);

		character.getDecisions().set("training:martialArtist:category:0",
				Decision.select(List.of("arms", "artistic", "athletic"), "arms"));
		character.getDecisions().set("training:martialArtist:characteristic:0", Decision.fixed(List.of("STRENGTH")));
		character.getDecisions().set("multichoice", Decision.selectMultiple(List.of("a", "b", "c"), List.of("a", "c"), 2));

		final SelectedPerk skeptic = new SelectedPerk();
		skeptic.setPerkId("skeptic");
		character.getSelectedPerks().add(skeptic);
		final SelectedPerk randomPerk = new SelectedPerk();
		randomPerk.setPerkId("ql");
		randomPerk.setWeaknessId("xenophobia");
		randomPerk.setRandom(true);
		character.getSelectedPerks().add(randomPerk);

		character.setHobbySkillRank("seamanship", 3);
		character.setHobbySpellListRank("trick", 1);

		character.addMagicItem(new MagicObject(new TranslatedText("Espada Mágica", "Magic Sword"),
				new TranslatedText("Brilla en la oscuridad", "It glows in the dark"),
				List.of(new ObjectBonus(BonusType.SKILL, "sword", 3))));
		character.addStandardEquipment(new Equipment(new TranslatedText("Daga", "Dagger"),
				new TranslatedText("Pequeña y ligera", "Small and light")));

		character.setFirearmsAllowed(true);
		character.setChiPowersAllowed(true);
		character.setOtherRealmTrainingSpellsAllowed(true);
		character.setMagicAllowed(true);
		character.setDarkSpellsAsBasicListsAllowed(true);
		return character;
	}

	private static void assertCharactersEqual(CharacterPlayer original, CharacterPlayer restored) {
		Assert.assertEquals(restored.getName(), original.getName());
		Assert.assertEquals(restored.getSex(), original.getSex());
		Assert.assertEquals(restored.getRaceId(), original.getRaceId());
		Assert.assertEquals(restored.getCultureId(), original.getCultureId());
		Assert.assertEquals(restored.getProfessionId(), original.getProfessionId());
		Assert.assertEquals(restored.getHistoryText(), original.getHistoryText());

		for (final CharacteristicAbbreviation abbreviation : realCharacteristics()) {
			Assert.assertEquals(restored.getCharacteristicTemporalValue(abbreviation),
					original.getCharacteristicTemporalValue(abbreviation), abbreviation + " temporal");
			Assert.assertEquals(restored.getCharacteristicPotentialValue(abbreviation),
					original.getCharacteristicPotentialValue(abbreviation), abbreviation + " potential");
		}
		Assert.assertEquals(restored.isCharacteristicsConfirmed(), original.isCharacteristicsConfirmed());
		Assert.assertEquals(restored.getAppearance().getDicesResult(), original.getAppearance().getDicesResult());
		Assert.assertEquals(restored.getCurrentAge(), original.getCurrentAge());
		Assert.assertEquals(restored.getFinalAge(), original.getFinalAge());

		Assert.assertEquals(restored.getLevel(), original.getLevel());
		final List<LevelUp> originalLevels = original.getLevels();
		final List<LevelUp> restoredLevels = restored.getLevels();
		Assert.assertEquals(restoredLevels.size(), originalLevels.size());
		for (int i = 0; i < originalLevels.size(); i++) {
			assertLevelsEqual(originalLevels.get(i), restoredLevels.get(i), i);
		}

		Assert.assertEquals(restored.getBackground().getCategoryIds(), original.getBackground().getCategoryIds());
		Assert.assertEquals(restored.getBackground().getSkillIds(), original.getBackground().getSkillIds());
		Assert.assertEquals(restored.getBackground().getLanguageRanks(), original.getBackground().getLanguageRanks());
		assertCharacteristicRollsEqual(restored.getBackground().getCharacteristicUpdates(),
				original.getBackground().getCharacteristicUpdates(), "background characteristic updates");

		Assert.assertEquals(restored.getDecisions().getAll().keySet(), original.getDecisions().getAll().keySet());
		for (final String key : original.getDecisions().getAll().keySet()) {
			final Decision originalDecision = original.getDecisions().get(key);
			final Decision restoredDecision = restored.getDecisions().get(key);
			Assert.assertEquals(restoredDecision.getOfferedOptions(), originalDecision.getOfferedOptions(), key);
			Assert.assertEquals(restoredDecision.getSelectedOptions(), originalDecision.getSelectedOptions(), key);
			Assert.assertEquals(restoredDecision.getSelectedOption(), originalDecision.getSelectedOption(), key);
		}

		Assert.assertEquals(restored.getSelectedPerks().size(), original.getSelectedPerks().size());
		for (int i = 0; i < original.getSelectedPerks().size(); i++) {
			final SelectedPerk originalPerk = original.getSelectedPerks().get(i);
			final SelectedPerk restoredPerk = restored.getSelectedPerks().get(i);
			Assert.assertEquals(restoredPerk.getPerkId(), originalPerk.getPerkId());
			Assert.assertEquals(restoredPerk.getWeaknessId(), originalPerk.getWeaknessId());
			Assert.assertEquals(restoredPerk.isRandom(), originalPerk.isRandom());
		}

		Assert.assertEquals(restored.getHobbySkillRanks(), original.getHobbySkillRanks());
		Assert.assertEquals(restored.getHobbySpellListRanks(), original.getHobbySpellListRanks());

		Assert.assertEquals(restored.getAllMagicItems().size(), original.getAllMagicItems().size());
		for (int i = 0; i < original.getAllMagicItems().size(); i++) {
			final MagicObject originalItem = original.getAllMagicItems().get(i);
			final MagicObject restoredItem = restored.getAllMagicItems().get(i);
			Assert.assertEquals(restoredItem.getName().getSpanish(), originalItem.getName().getSpanish());
			Assert.assertEquals(restoredItem.getName().getEnglish(), originalItem.getName().getEnglish());
			Assert.assertEquals(restoredItem.getDescription().getSpanish(), originalItem.getDescription().getSpanish());
			Assert.assertEquals(restoredItem.getBonuses().size(), originalItem.getBonuses().size());
			for (int bonusIndex = 0; bonusIndex < originalItem.getBonuses().size(); bonusIndex++) {
				final ObjectBonus originalBonus = originalItem.getBonuses().get(bonusIndex);
				final ObjectBonus restoredBonus = restoredItem.getBonuses().get(bonusIndex);
				Assert.assertEquals(restoredBonus.getType(), originalBonus.getType());
				Assert.assertEquals(restoredBonus.getBonusName(), originalBonus.getBonusName());
				Assert.assertEquals(restoredBonus.getBonus(), originalBonus.getBonus());
			}
		}

		Assert.assertEquals(restored.getAllNotMagicEquipment(), original.getAllNotMagicEquipment());

		Assert.assertEquals(restored.isFirearmsAllowed(), original.isFirearmsAllowed());
		Assert.assertEquals(restored.isChiPowersAllowed(), original.isChiPowersAllowed());
		Assert.assertEquals(restored.isOtherRealmTrainingSpellsAllowed(), original.isOtherRealmTrainingSpellsAllowed());
		Assert.assertEquals(restored.isMagicAllowed(), original.isMagicAllowed());
		Assert.assertEquals(restored.isDarkSpellsAsBasicListsAllowed(), original.isDarkSpellsAsBasicListsAllowed());
	}

	private static void assertLevelsEqual(LevelUp original, LevelUp restored, int index) {
		Assert.assertEquals(restored.getCategoryRanks(), original.getCategoryRanks(), "level " + index + " category ranks");
		Assert.assertEquals(restored.getSkillRanks(), original.getSkillRanks(), "level " + index + " skill ranks");
		Assert.assertEquals(restored.getSpellListRanks(), original.getSpellListRanks(), "level " + index + " spell list ranks");
		Assert.assertEquals(restored.getGeneralizedSkills(), original.getGeneralizedSkills(), "level " + index + " generalized skills");
		Assert.assertEquals(restored.getSkillSpecializations(), original.getSkillSpecializations(), "level " + index + " specializations");
		Assert.assertEquals(restored.getFavouriteSkills(), original.getFavouriteSkills(), "level " + index + " favourite skills");
		Assert.assertEquals(restored.getSpellsUpdated(), original.getSpellsUpdated(), "level " + index + " spells updated");
		Assert.assertEquals(restored.getTrainings(), original.getTrainings(), "level " + index + " trainings");
		assertCharacteristicRollsEqual(restored.getCharacteristicUpdates(), original.getCharacteristicUpdates(),
				"level " + index + " characteristic updates");
		Assert.assertEquals(restored.getAgeModifications().size(), original.getAgeModifications().size(),
				"level " + index + " age modifications");
		for (int i = 0; i < original.getAgeModifications().size(); i++) {
			final AgeModification originalModification = original.getAgeModifications().get(i);
			final AgeModification restoredModification = restored.getAgeModifications().get(i);
			Assert.assertEquals(restoredModification.getAge(), originalModification.getAge());
			Assert.assertEquals(restoredModification.getCharacteristicAbbreviation(),
					originalModification.getCharacteristicAbbreviation());
			Assert.assertEquals(restoredModification.getCharacteristicModification(),
					originalModification.getCharacteristicModification());
		}
	}

	private static void assertCharacteristicRollsEqual(List<CharacteristicRoll> restored, List<CharacteristicRoll> original,
			String what) {
		Assert.assertEquals(restored.size(), original.size(), what);
		for (int i = 0; i < original.size(); i++) {
			final CharacteristicRoll originalRoll = original.get(i);
			final CharacteristicRoll restoredRoll = restored.get(i);
			Assert.assertEquals(restoredRoll.getCharacteristicAbbreviation(), originalRoll.getCharacteristicAbbreviation(), what);
			Assert.assertEquals(restoredRoll.getCharacteristicTemporalValue(), originalRoll.getCharacteristicTemporalValue(), what);
			Assert.assertEquals(restoredRoll.getCharacteristicPotentialValue(), originalRoll.getCharacteristicPotentialValue(), what);
			Assert.assertEquals(restoredRoll.getRoll().getFirstDice(), originalRoll.getRoll().getFirstDice(), what);
			Assert.assertEquals(restoredRoll.getRoll().getSecondDice(), originalRoll.getRoll().getSecondDice(), what);
		}
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

	private static boolean isSorted(List<String> values) {
		for (int i = 1; i < values.size(); i++) {
			if (values.get(i - 1).compareTo(values.get(i)) > 0) {
				return false;
			}
		}
		return true;
	}
}