package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.persistence.CharacterData;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.LegacyCharacterJsonImporter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
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
        generateAndWriteSheet("Nihal_level9_2_0_0.json", "Nihal_level9.pdf",
                new StandardCharacterSheet(), 8, null);
    }

    @Test
    public void importedOphiliagliiGeneratesAValidPdfSheet() throws Exception {
        generateAndWriteSheet("Ophiliaglii_Humbiton_N7_Jhordi_Monje_Zen.json", "Ophiliaglii_Humbiton_N7.pdf",
                new LegacyCombinedCharacterSheet(), 5, "Hoja Combinada de Habilidades (1 de 3)");
    }

    @Test
    public void importedMorticiaLevelTenGeneratesAValidPdfSheet() throws Exception {
        generateAndWriteSheet("Morticia_Innodan_N10_Ilourianos_Elementalista.json",
                "Morticia_Innodan_N10_Ilourianos_Elementalista.pdf", new StandardCharacterSheet(), 8, null);
    }

    @Test
    public void importedMorticiaLevelElevenGeneratesAValidPdfSheet() throws Exception {
        generateAndWriteSheet("Morticia_Innodan_N11_Ilourianos_Elementalista.json",
                "Morticia_Innodan_N11_Ilourianos_Elementalista.pdf", new LegacyCombinedCharacterSheet(), 5,
                "Hoja Combinada de Habilidades (1 de 3)");
    }

    @Test
    public void importedMorticiaLevelTwelveGeneratesAValidPdfSheet() throws Exception {
        generateAndWriteSheet("Morticia_Innodan_N12_Ilourianos_Elementalista.json",
                "Morticia_Innodan_N12_Ilourianos_Elementalista.pdf",
                new LegacyCombinedSkillsCharacterSheet(), 5, "Hoja de Habilidades (1 de 3)");
    }

    @Test
    public void importedMorticiaPdfShowsEverySelectedMultiChoicePerkTarget() throws Exception {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(
                readSnapshot("Morticia_Innodan_N10_Ilourianos_Elementalista.json"));
        final byte[] pdf = new StandardCharacterSheet().generate(CharacterDataMapper.toCharacter(data));
        final PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
        final PdfTextExtractor extractor = new PdfTextExtractor(reader);
        final StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        reader.close();

        final String flatText = text.toString().replace('·', ' ').replaceAll("\\s+", " ");
        Assert.assertTrue(flatText.contains("Armas 2manos | Armas Arrojadizas | Armas Artillería"),
                "the weapon curse must print its selected categories");
        Assert.assertTrue(flatText.contains("Armas Fuego 2manos} (-20)[-1]"),
                "the whole multi-selection must be retained");
        Assert.assertTrue(flatText.contains("{Influencia} (20)"),
                "the selected category for Experimentado must be printed");
    }

    private static void generateAndWriteSheet(String snapshotPath, String outputName, PdfDocument sheet,
            int expectedPageCount, String expectedSkillsHeader)
            throws IOException, InvalidXmlElementException {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readSnapshot(snapshotPath));
        final CharacterPlayer character = CharacterDataMapper.toCharacter(data);

        final byte[] pdf = sheet.generate(character);

        Assert.assertEquals(new String(pdf, 0, 5, StandardCharsets.US_ASCII), "%PDF-");
        Assert.assertTrue(pdf.length > 3000, snapshotPath + " produced a " + pdf.length + " byte sheet");
        Files.createDirectories(OUTPUT_DIRECTORY);
        final Path target = OUTPUT_DIRECTORY.resolve(outputName);
        Files.write(target, pdf);

        final PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
        Assert.assertEquals(reader.getNumberOfPages(), expectedPageCount,
                snapshotPath + " should use the same sheet format and page count as its legacy PDF");
        if (expectedSkillsHeader != null) {
            final PdfTextExtractor extractor = new PdfTextExtractor(reader);
            final StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append(' ');
            }
            Assert.assertTrue(text.toString().replaceAll("\\s+", " ").contains(expectedSkillsHeader),
                    snapshotPath + " should retain its legacy skills-page heading");
        }
        reader.close();

        System.out.println("Wrote " + character.getName() + " PDF to " + target);
    }

    private static String readSnapshot(String path) throws IOException {
        try (final InputStream in = LegacyImportedCharacterSheetTest.class.getResourceAsStream("/" + path)) {
            Assert.assertNotNull(in, "Missing snapshot " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
