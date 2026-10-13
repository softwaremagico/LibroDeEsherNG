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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regression coverage for the legacy one-column {@code PdfCombinedSheet1Column} export path. */
@Test(groups = "pdf")
public class LegacyCombinedOneColumnCharacterSheetTest {

    private static final Path OUTPUT_DIRECTORY = Path.of("/tmp/RolemasterPDF");
    private static final String SNAPSHOT = "Bellido__Kara_Trace_N5_Medio-elfo_Magente.rlm";
    private static final Pattern HEADER = Pattern.compile("Hoja Combinada de Habilidades \\((\\d+) de (\\d+)\\)");

    private CharacterPlayer character;
    private int pageCount;
    private String sheetText;

    @BeforeClass
    public void loadCharacterAndGenerateSheet() throws Exception {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readSnapshot(SNAPSHOT));
        character = CharacterDataMapper.toCharacter(data);

        final byte[] pdf = new LegacyCombinedOneColumnCharacterSheet().generate(character);
        Assert.assertEquals(new String(pdf, 0, 5, StandardCharsets.US_ASCII), "%PDF-");

        Files.createDirectories(OUTPUT_DIRECTORY);
        final Path target = OUTPUT_DIRECTORY.resolve("Bellido__Kara_Trace_N5_Medio-elfo_Magente_onecolumn.pdf");
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
    public void combinedPagesFollowTheTwoInformationPages() {
        Assert.assertTrue(pageCount >= 3, "there should be the two information pages plus columns");

        int combinedPages = 0;
        int declaredTotal = 0;
        final Matcher matcher = HEADER.matcher(sheetText);
        while (matcher.find()) {
            combinedPages++;
            declaredTotal = Integer.parseInt(matcher.group(2));
            Assert.assertEquals(Integer.parseInt(matcher.group(1)), combinedPages,
                    "headers should number the combined pages in order");
        }

        Assert.assertEquals(declaredTotal, pageCount - 2,
                "the legacy prints one column per page, so the declared header count is the combined page count");
        Assert.assertEquals(combinedPages, declaredTotal, "expected a numbered header on every combined page");
    }

    @Test
    public void usesTheOneColumnSkillTitles() {
        Assert.assertTrue(sheetText.contains("Habilidad:"), "missing skill name title");
        Assert.assertTrue(sheetText.contains("Rangos"), "missing one-column 'Rangos' title");
        Assert.assertTrue(sheetText.contains("B. Rangos:"), "missing 'B. Rangos:' title");
        Assert.assertTrue(sheetText.contains("Categoría") || sheetText.contains("Categoria"), "missing 'Categoría' title");
        Assert.assertTrue(sheetText.contains("Objeto:"), "missing 'Objeto:' title");
        Assert.assertTrue(sheetText.contains("Especial:"), "missing 'Especial:' title");
        Assert.assertTrue(sheetText.contains("Total:"), "missing 'Total:' title");
        Assert.assertFalse(sheetText.contains("Rng:"), "the one-column sheet should not use the short 'Rng:' title");
    }

    @Test
    public void retainsTheExpectedCategories() {
        Assert.assertTrue(sheetText.toLowerCase().contains("armadura"), "missing armor-related category");
        Assert.assertTrue(sheetText.toLowerCase().contains("hechizos"), "missing spell lists category");
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