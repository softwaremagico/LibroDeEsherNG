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

    /** Every sheet generated from an imported snapshot lands here, mirroring the legacy tool output. */
    private static final Path OUTPUT_DIRECTORY = Path.of("/tmp/RolemasterPDF");

    @Test
    public void importedNihalGeneratesAValidPdfSheet() throws Exception {
        generateAndWriteSheet("Nihal_level9_2_0_0.json", "Nihal_level9.pdf");
    }

    @Test
    public void importedOphiliagliiGeneratesAValidPdfSheet() throws Exception {
        generateAndWriteSheet("Ophiliaglii_Humbiton_N7_Jhordi_Monje_Zen.json", "Ophiliaglii_Humbiton_N7.pdf");
    }

    @Test
    public void importedMorticiaLevelTenGeneratesAValidPdfSheet() throws Exception {
        generateAndWriteSheet("Morticia_Innodan_N10_Ilourianos_Elementalista.json",
                "Morticia_Innodan_N10_Ilourianos_Elementalista.pdf");
    }

    @Test
    public void importedMorticiaLevelElevenGeneratesAValidPdfSheet() throws Exception {
        generateAndWriteSheet("Morticia_Innodan_N11_Ilourianos_Elementalista.json",
                "Morticia_Innodan_N11_Ilourianos_Elementalista.pdf");
    }

    @Test
    public void importedMorticiaLevelTwelveGeneratesAValidPdfSheet() throws Exception {
        generateAndWriteSheet("Morticia_Innodan_N12_Ilourianos_Elementalista.json",
                "Morticia_Innodan_N12_Ilourianos_Elementalista.pdf");
    }

    private static void generateAndWriteSheet(String snapshotPath, String outputName)
            throws IOException, InvalidXmlElementException {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readSnapshot(snapshotPath));
        final CharacterPlayer character = CharacterDataMapper.toCharacter(data);

        final byte[] pdf = new StandardCharacterSheet().generate(character);

        Assert.assertEquals(new String(pdf, 0, 5, StandardCharsets.US_ASCII), "%PDF-");
        Assert.assertTrue(pdf.length > 3000, snapshotPath + " produced a " + pdf.length + " byte sheet");

        Files.createDirectories(OUTPUT_DIRECTORY);
        final Path target = OUTPUT_DIRECTORY.resolve(outputName);
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
