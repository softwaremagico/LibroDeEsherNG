package com.softwaremagico.librodeesher.pdf;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verifies {@link CharacterSheet} actually produces a well-formed, non-trivial PDF for a real
 * character (built from the shipped rulebook data, same as {@code CharacterPlayerTest}), both as a
 * byte array and as a file on disk.
 */
@Test(groups = "pdf")
public class CharacterSheetTest {

    @Test
    public void generatesAWellFormedPdfForAWizard() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setName("Gandalf");
        character.setRaceId("horseCentaur");
        character.setProfessionId("wizard");
        character.applyProfessionMagicRealms(null);

        final byte[] pdf = new CharacterSheet().generate(character);

        Assert.assertTrue(pdf.length > 1000, "Expected a non-trivial PDF, got " + pdf.length + " bytes.");
        Assert.assertEquals(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII), "%PDF-");
    }

    @Test
    public void createsAPdfFileOnDisk() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setName("Aragorn");
        character.setRaceId("horseCentaur");
        character.setProfessionId("fighter");
        character.applyProfessionMagicRealms(null);

        final Path tempDir = Files.createTempDirectory("librodeesher-pdf-test");
        final Path target = tempDir.resolve("character-without-extension");
        new CharacterSheet().createFile(character, target);

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
