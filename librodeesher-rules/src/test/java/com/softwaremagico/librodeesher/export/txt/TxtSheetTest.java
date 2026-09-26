package com.softwaremagico.librodeesher.export.txt;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapperTest;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verifies the plain-text character sheet renders every section of the standard export and never
 * fails, even for a blank, race-less, profession-less character.
 */
@Test(groups = "txt")
public class TxtSheetTest {

	@Test
	public void exportsHeaderWithRaceCultureProfessionAndTrainings() throws InvalidXmlElementException {
		final String sheet = TxtSheet.getCharacterStandardSheetAsText(CharacterDataMapperTest.newFullyPopulatedCharacter());
		Assert.assertTrue(sheet.startsWith("Ánforo Élfico\tLevel 2"), sheet);
		Assert.assertTrue(sheet.contains(text(RulesCatalog.getInstance().getProfession("fighter").getName()))
				|| sheet.contains("fighter"));
		Assert.assertTrue(sheet.contains("(" + text(RulesCatalog.getInstance().getCulture("aquaticMilitarista").getName())
				+ ")"));
		Assert.assertTrue(sheet.contains(text(RulesCatalog.getInstance().getTraining("martialArtist").getName())));
	}

	@Test
	public void exportsCharacteristicsResistancesSkillsAndHistory() throws InvalidXmlElementException {
		final String sheet = TxtSheet.getCharacterStandardSheetAsText(CharacterDataMapperTest.newFullyPopulatedCharacter());
		Assert.assertTrue(characteristicRow(sheet, "ST", 60, 65), sheet);
		Assert.assertTrue(sheet.contains("Caract"), sheet);
		Assert.assertTrue(sheet.contains("Resistance rolls"), sheet);
		Assert.assertTrue(sheet.contains("CHANNELING"), sheet);
		Assert.assertTrue(sheet.contains("Categories and skills"), sheet);
		final String sword = text(RulesCatalog.getInstance().getSkill("sword").getName());
		Assert.assertTrue(sheet.contains(sword), sheet);
		Assert.assertTrue(sheet.contains("TA9"), sheet);
		Assert.assertTrue(sheet.contains("Trasfondo con acentos: áéíóú ñ."), sheet);
	}

	@Test
	public void exportsTalentsSpecialsAndEquipment() throws InvalidXmlElementException {
		final String sheet = TxtSheet.getCharacterStandardSheetAsText(CharacterDataMapperTest.newFullyPopulatedCharacter());
		Assert.assertTrue(sheet.contains("Talents"), sheet);
		Assert.assertTrue(sheet.contains("Race specials"), sheet);
		Assert.assertTrue(sheet.contains("Equipment"), sheet);
		Assert.assertTrue(sheet.contains("Espada Mágica"), sheet);
		Assert.assertTrue(sheet.contains("Brilla en la oscuridad"), sheet);
		Assert.assertTrue(sheet.contains("+3 to sword"), sheet);
		Assert.assertTrue(sheet.contains("Daga"), sheet);
	}

	@Test
	public void blankCharactersStillRenderAReadableSheet() throws InvalidXmlElementException {
		final String sheet = TxtSheet.getCharacterStandardSheetAsText(new CharacterPlayer());
		Assert.assertTrue(sheet.contains("Unknown Race"), sheet);
		Assert.assertTrue(sheet.contains("Unknown Culture"), sheet);
		Assert.assertTrue(sheet.contains("Unknown profession"), sheet);
		Assert.assertTrue(sheet.contains("Level 1"), sheet);
		Assert.assertTrue(sheet.contains("Caract"), sheet);
	}

	@Test
	public void outputIsDeterministic() throws InvalidXmlElementException {
		final CharacterPlayer character = CharacterDataMapperTest.newFullyPopulatedCharacter();
		Assert.assertEquals(TxtSheet.getCharacterStandardSheetAsText(character),
				TxtSheet.getCharacterStandardSheetAsText(character));
	}

	@Test
	public void nullCharacterRendersAnEmptySheet() throws InvalidXmlElementException {
		Assert.assertEquals(TxtSheet.getCharacterStandardSheetAsText(null), "");
	}

	@Test
	public void createFileWritesTheSheetWithTxtExtension() throws InvalidXmlElementException, IOException {
		final Path target = Files.createTempFile("txt-sheet", "");
		try {
			TxtSheet.createFile(CharacterDataMapperTest.newFullyPopulatedCharacter(), target);
			final Path written = Path.of(target + ".txt");
			Assert.assertTrue(Files.exists(written));
			Assert.assertEquals(Files.readString(written),
					TxtSheet.getCharacterStandardSheetAsText(CharacterDataMapperTest.newFullyPopulatedCharacter()));
			Files.deleteIfExists(written);
		} finally {
			Files.deleteIfExists(target);
		}
	}

	private static boolean characteristicRow(String sheet, String code, int temporal, int potential) {
		for (final String line : sheet.split("\\R")) {
			if (line.matches("^" + code + "\\s+" + temporal + "\\s+" + potential + "\\s.*")) {
				return true;
			}
		}
		return false;
	}

	private static String text(com.softwaremagico.librodeesher.language.TranslatedText translatedText) {
		return translatedText == null ? "" : translatedText.getTranslatedText();
	}
}