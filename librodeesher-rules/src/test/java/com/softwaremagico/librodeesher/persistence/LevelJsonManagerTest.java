package com.softwaremagico.librodeesher.persistence;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.level.LevelUp;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies {@link LevelJsonManager} exports a character's current level and lets a copy of the same
 * character (one level behind) import it, rejecting any other character or any level that is not the
 * very next one.
 */
@Test(groups = "persistence")
public class LevelJsonManagerTest {

	@Test
	public void exportsTheCurrentLevelAndImportsItAsTheNextOne() throws Exception {
		final CharacterPlayer original = CharacterDataMapperTest.newFullyPopulatedCharacter();
		final CharacterPlayer target = cloneOneLevelBehind(original);

		final String json = LevelJsonManager.toJson(original);
		final LevelUp imported = LevelJsonManager.fromJson(target, json);

		final LevelUp expected = original.getLevels().get(original.getLevels().size() - 1);
		Assert.assertEquals(imported.getCategoryRanks(), expected.getCategoryRanks());
		Assert.assertEquals(imported.getSkillRanks(), expected.getSkillRanks());
		Assert.assertEquals(imported.getSpellListRanks(), expected.getSpellListRanks());
		Assert.assertEquals(imported.getGeneralizedSkills(), expected.getGeneralizedSkills());
		Assert.assertEquals(imported.getSkillSpecializations(), expected.getSkillSpecializations());
		Assert.assertEquals(imported.getFavouriteSkills(), expected.getFavouriteSkills());
		Assert.assertEquals(imported.getSpellsUpdated(), expected.getSpellsUpdated());
		Assert.assertEquals(imported.getTrainings(), expected.getTrainings());
		Assert.assertEquals(imported.getCharacteristicUpdates().size(), expected.getCharacteristicUpdates().size());
		Assert.assertEquals(imported.getAgeModifications().size(), expected.getAgeModifications().size());
	}

	@Test(expectedExceptions = InvalidCharacterException.class)
	public void rejectsALevelFromADifferentCharacter() throws Exception {
		final CharacterPlayer original = CharacterDataMapperTest.newFullyPopulatedCharacter();
		final String json = LevelJsonManager.toJson(original);

		final CharacterPlayer other = CharacterDataMapperTest.newFullyPopulatedCharacter();
		other.setName("Someone Else");
		LevelJsonManager.fromJson(other, json);
	}

	@Test(expectedExceptions = InvalidLevelException.class)
	public void rejectsALevelThatIsNotTheNextOne() throws Exception {
		final CharacterPlayer original = CharacterDataMapperTest.newFullyPopulatedCharacter();
		final String json = LevelJsonManager.toJson(original);

		final CharacterPlayer sameLevelClone = CharacterDataMapper.toCharacter(CharacterDataMapper.toData(original));
		LevelJsonManager.fromJson(sameLevelClone, json);
	}

	@Test
	public void nullAndBlankRoundTripToNull() throws Exception {
		Assert.assertNull(LevelJsonManager.toJson(null));
		Assert.assertNull(LevelJsonManager.fromJson(CharacterDataMapperTest.newFullyPopulatedCharacter(), null));
		Assert.assertNull(LevelJsonManager.fromJson(CharacterDataMapperTest.newFullyPopulatedCharacter(), "   "));
		Assert.assertNull(LevelJsonManager.fromJson(null, "{}"));
	}

	private static CharacterPlayer cloneOneLevelBehind(CharacterPlayer original) {
		final CharacterPlayer clone = CharacterDataMapper.toCharacter(CharacterDataMapper.toData(original));
		clone.getLevels().remove(clone.getLevels().size() - 1);
		return clone;
	}
}