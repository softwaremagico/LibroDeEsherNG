package com.softwaremagico.librodeesher.pdf;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.persistence.CharacterData;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.LegacyCharacterJsonImporter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Test(groups = "pdf")
public class LegacyImportedCharacterSheetTest {

    @Test
    public void importedCharactersGenerateValidPdfSheets() throws Exception {
        generateAndWriteSheet("Nihal_level9_2_0_0.json", "Nihal_level9.pdf");
        generateAndWriteSheet("Ophiliaglii_Humbiton_N7_Jhordi_Monje_Zen.json", "Ophiliaglii_Humbiton_N7.pdf");
    }

    private static void generateAndWriteSheet(String snapshotPath, String outputName)
            throws IOException, InvalidXmlElementException {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readSnapshot(snapshotPath));
        final CharacterPlayer character = CharacterDataMapper.toCharacter(data);

        final byte[] pdf = new StandardCharacterSheet().generate(character);

        Assert.assertEquals(new String(pdf, 0, 5, StandardCharsets.US_ASCII), "%PDF-");
        Assert.assertTrue(pdf.length > 3000, snapshotPath + " produced a " + pdf.length + " byte sheet");

        final Path target = Path.of(System.getProperty("java.io.tmpdir"), outputName);
        Files.write(target, pdf);
        System.out.println("Wrote " + character.getName() + " PDF to " + target);
    }

    private static String readSnapshot(String path) throws IOException {
        try (final InputStream in = LegacyImportedCharacterSheetTest.class.getResourceAsStream("/" + path)) {
            Assert.assertNotNull(in, "Missing snapshot " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}