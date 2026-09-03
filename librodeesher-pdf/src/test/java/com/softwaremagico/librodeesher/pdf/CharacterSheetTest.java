package com.softwaremagico.librodeesher.pdf;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verifies modern replacements for each legacy PDF export flow.
 */
@Test(groups = "pdf")
public class CharacterSheetTest {

    @Test
    public void standardPdfFlowGeneratesAWellFormedPdfForAWizard() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setName("Gandalf");
        character.setRaceId("horseCentaur");
        character.setProfessionId("wizard");
        character.applyProfessionMagicRealms(null);

        final byte[] pdf = new StandardCharacterSheet().generate(character);

        Assert.assertTrue(pdf.length > 3000, "Expected a complete PDF sheet, got " + pdf.length + " bytes.");
        Assert.assertEquals(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII), "%PDF-");
    }

    @Test
    public void combinedPdfFlowCreatesAPdfFileOnDisk() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setName("Aragorn");
        character.setRaceId("horseCentaur");
        character.setProfessionId("fighter");
        character.applyProfessionMagicRealms(null);

        final Path tempDir = Files.createTempDirectory("librodeesher-pdf-test");
        final Path target = tempDir.resolve("character-without-extension");
        new StandardCharacterSheet().createFile(character, target);

        final Path expected = tempDir.resolve("character-without-extension.pdf");
        Assert.assertTrue(Files.exists(expected));
        Assert.assertTrue(Files.size(expected) > 1000);
    }

    @Test
    public void toleratesACharacterWithNoRaceProfessionOrCulture() throws Exception {
        final byte[] pdf = new CharacterSheet().generate(new CharacterPlayer());

        Assert.assertTrue(pdf.length > 500);
    }
}
