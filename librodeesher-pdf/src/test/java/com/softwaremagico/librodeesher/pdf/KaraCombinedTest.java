package com.softwaremagico.librodeesher.pdf;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.persistence.CharacterData;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.LegacyCharacterJsonImporter;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Test(groups = "pdf")
public class KaraCombinedTest {

    private static final Path OUTPUT_DIRECTORY = Path.of("/tmp/RolemasterPDF");
    private static final String SNAPSHOT = "Bellido__Kara_Trace_N5_Medio-elfo_Magente.rlm";
    private static final int EXPECTED_PAGES = 5;

    private CharacterPlayer character;
    private int pageCount;
    private String sheetText;

    @BeforeClass
    public void loadCharacterAndGenerateSheet() throws Exception {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readSnapshot(SNAPSHOT));
        character = CharacterDataMapper.toCharacter(data);

        final byte[] pdf = new LegacyCombinedCharacterSheet().generate(character);
        Assert.assertEquals(new String(pdf, 0, 5, StandardCharsets.US_ASCII), "%PDF-");

        Files.createDirectories(OUTPUT_DIRECTORY);
        final Path target = OUTPUT_DIRECTORY.resolve("Bellido__Kara_Trace_N5_Medio-elfo_Magente_combined.pdf");
        Files.write(target, pdf);

        final PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
        pageCount = reader.getNumberOfPages();
        final PdfTextExtractor extractor = new PdfTextExtractor(reader);
        final StringBuilder text = new StringBuilder();
        for (int page = 1; page <= pageCount; page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        reader.close();
        sheetText = flatten(text.toString());
    }

    @Test
    public void generatesFivePagesForKaraN5() {
        Assert.assertEquals(pageCount, EXPECTED_PAGES, "combined sheet should have 5 pages");
    }

    @Test
    public void containsPageHeaders() {
        Assert.assertTrue(sheetText.contains("(1 de 3)"), "missing (1 de 3)");
        Assert.assertTrue(sheetText.contains("(2 de 3)"), "missing (2 de 3)");
        Assert.assertTrue(sheetText.contains("(3 de 3)"), "missing (3 de 3)");
    }

    @Test
    public void containsExpectedCategories() {
        Assert.assertTrue(sheetText.toLowerCase().contains("armadura") && sheetText.toLowerCase().contains("ligera"),
                "missing armor/light related");
        Assert.assertTrue(sheetText.contains("Listas Abiertas de Hechizos") || sheetText.contains("Listas Abiertas de Hechizos".replace('·', ' ')),
                "missing spell lists category");
    }

    private static String readSnapshot(String path) throws IOException {
        try (final InputStream in = LegacyImportedCharacterSheetTest.class.getResourceAsStream("/" + path)) {
            Assert.assertNotNull(in, "Missing snapshot " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String flatten(String text) {
        return text.replace('·', ' ').replaceAll("\\s+", " ").trim();
    }
}
