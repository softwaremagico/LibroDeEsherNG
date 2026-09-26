package com.softwaremagico.librodeesher.pdf;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.Appearance;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.decision.Decision;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.CharacterJsonManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies {@link CharacterPdfExporter} turns a JSON-saved character (a snapshot produced by the
 * persistence layer) into a real PDF sheet, both as bytes and as a file on disk.
 */
@Test(groups = "pdf")
public class CharacterPdfExporterTest {

    @Test
    public void exportsAJSONSavedWizardToAWellFormedPDF() throws Exception {
        final String json = CharacterJsonManager.toJson(CharacterDataMapper.toData(savedWizard()));

        final byte[] pdf = CharacterPdfExporter.exportFromJSON(json, new StandardCharacterSheet());

        Assert.assertTrue(pdf.length > 3000, "Expected a complete PDF sheet, got " + pdf.length + " bytes.");
        Assert.assertEquals(new String(pdf, 0, 5, StandardCharsets.US_ASCII), "%PDF-");
    }

    @Test
    public void exportsAJSONSavedCharacterToAPdfFileOnDisk() throws Exception {
        final String json = CharacterJsonManager.toJson(CharacterDataMapper.toData(savedWizard()));

        final Path tempDir = Files.createTempDirectory("librodeesher-pdf-export-test");
        final Path target = tempDir.resolve("saved-wizard");
        CharacterPdfExporter.exportToFileFromJSON(json, new CombinedOneColumnCharacterSheet(), target);

        final Path expected = tempDir.resolve("saved-wizard.pdf");
        Assert.assertTrue(Files.exists(expected), "Expected the exporter to append the .pdf extension.");
        Assert.assertTrue(Files.size(expected) > 1000);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsBlankJSON() throws Exception {
        CharacterPdfExporter.exportFromJSON("   ", new CharacterSheet());
    }

    private static CharacterPlayer savedWizard() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setName("Sauron");
        character.setRaceId("horseCentaur");
        character.setCultureId("aquaticMilitarista");
        character.setProfessionId("wizard");
        character.setCurrentAge(70);
        character.setFinalAge(400);
        character.setAppearance(new Appearance(30));
        character.setCharacteristicTemporalValue(CharacteristicAbbreviation.PRESENCE, 80);
        character.setCharacteristicPotentialValue(CharacteristicAbbreviation.PRESENCE, 90);
        character.applyProfessionMagicRealms(null);

        character.getCurrentLevel().setSpellListRanks("essenceLawOfLight", 2);
        character.getCurrentLevel().setSkillRanks("sword", 2, false);
        character.getCurrentLevel().setTrainings(List.of("martialArtist"));
        character.getCurrentLevel().getFavouriteSkills().add("sword");
        character.getDecisions().set("training:martialArtist:category:0",
                Decision.select(List.of("arms", "artistic", "athletic"), "arms"));
        return character;
    }
}