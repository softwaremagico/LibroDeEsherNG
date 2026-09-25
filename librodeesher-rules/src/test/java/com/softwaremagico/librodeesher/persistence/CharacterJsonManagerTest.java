package com.softwaremagico.librodeesher.persistence;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Verifies {@link CharacterJsonManager} round-trips a character snapshot through JSON with exactly
 * stable (byte-identical) text, tolerates future extra fields, and treats null/blank input as
 * "no character".
 */
@Test(groups = "persistence")
public class CharacterJsonManagerTest {

	@Test
	public void jsonRoundTripsByteIdenticallyForTheSameSnapshot() {
		final CharacterData data = CharacterDataMapper.toData(CharacterDataMapperTest.newFullyPopulatedCharacter());

		final String json = CharacterJsonManager.toJson(data);
		final CharacterData decoded = CharacterJsonManager.fromJson(json);

		Assert.assertEquals(CharacterJsonManager.toJson(decoded), json);
		Assert.assertEquals(decoded.getName(), "Ánforo Élfico");
		Assert.assertEquals(decoded.getDecisions().get("training:martialArtist:category:0").getSelectedOptions(),
				List.of("arms"));
		Assert.assertEquals(decoded.getHobbySkillRanks().get("seamanship"), Integer.valueOf(3));
		Assert.assertEquals(decoded.getLevels().get(0).getCategoryRanks().get("arms"), Integer.valueOf(3));
		Assert.assertTrue(decoded.isMagicAllowed());
	}

	@Test
	public void nullAndBlankTextRoundTripToNull() {
		Assert.assertNull(CharacterJsonManager.fromJson(null));
		Assert.assertNull(CharacterJsonManager.fromJson(""));
		Assert.assertNull(CharacterJsonManager.fromJson("   "));
		Assert.assertNull(CharacterJsonManager.fromJson("null"));
		Assert.assertNull(CharacterJsonManager.toJson(null));
	}

	@Test
	public void unknownTopLevelFieldsAreToleratedOnLoad() {
		final CharacterData data = CharacterDataMapper.toData(CharacterDataMapperTest.newFullyPopulatedCharacter());
		final String withFutureField = CharacterJsonManager.toJson(data)
				.replace("\"characteristicsConfirmed\"",
						"\"futureField\": { \"x\": 1 },\n      \"characteristicsConfirmed\"");

		final CharacterData decoded = CharacterJsonManager.fromJson(withFutureField);

		Assert.assertEquals(decoded.getName(), data.getName());
		Assert.assertEquals(decoded.getDecisions().keySet(), data.getDecisions().keySet());
	}
}