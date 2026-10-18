package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.persistence.CharacterData;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.CharacterJsonManager;
import com.softwaremagico.librodeesher.persistence.LegacyCharacterJsonImporter;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Test(groups = "pdf")
public class LegacyPjsRegressionTest {

    private static final String FIXTURE_ROOT = "/legacy-fixtures/";
    private static final Map<String, String> EXISTING_SNAPSHOTS = Map.of(
            "Bellido/Kara_Trace_N5_Medio-elfo_Magente.rlm", "/Bellido__Kara_Trace_N5_Medio-elfo_Magente.rlm",
            "Carlos/Fthagtli_Milupa_N5_Garks_Monje Guerrero.rlm",
            "/Fthagtli_Milupa_N5_Garks_Monje Guerrero.rlm",
            "Kobold Barrendero/Gorju_Ping_N5_Kobolds_Lego.rlm", "/Gorju_Ping_N5_Kobolds_Lego.rlm");
    private static final Pattern TEXT_TOKEN = Pattern.compile("[\\p{L}]{3,}");

    @Test(dataProvider = "legacySnapshots")
    public void jsonExportAndImportPreserveTheCharacter(String snapshotPath)
            throws IOException, InvalidXmlElementException {
        final CharacterData original = LegacyCharacterJsonImporter.importLegacyJson(readSnapshot(snapshotPath));
        final String exportedJson = CharacterJsonManager.toJson(original);
        final CharacterData imported = CharacterJsonManager.fromJson(exportedJson);

        Assert.assertEquals(CharacterJsonManager.toJson(imported), exportedJson,
                snapshotPath + " changed while exporting and importing JSON");

        final CharacterPlayer restoredCharacter = CharacterDataMapper.toCharacter(imported);
        Assert.assertEquals(CharacterJsonManager.toJson(CharacterDataMapper.toData(restoredCharacter)), exportedJson,
                snapshotPath + " changed when the imported JSON was restored to a character");
    }

    @Test(dataProvider = "legacyPdfPairs")
    public void generatedPdfMatchesItsLegacyReference(String snapshotPath, String referencePdfPath, String sheetType)
            throws Exception {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readSnapshot(snapshotPath));
        final CharacterPlayer character = CharacterDataMapper.toCharacter(data);
        final PdfDocument sheet = createSheet(sheetType);

        final byte[] generatedPdf = sheet.generate(character);
        final PdfReader generatedReader = new PdfReader(generatedPdf);
        final PdfReader referenceReader = new PdfReader(readFixture("pdfs/" + referencePdfPath));
        try {
            Assert.assertEquals(generatedReader.getNumberOfPages(), referenceReader.getNumberOfPages(),
                    referencePdfPath + " should keep the legacy page count");

            final String generatedText = extractText(generatedReader);
            final String referenceText = extractText(referenceReader);
            final Set<String> generatedTokens = tokens(generatedText);
            final Set<String> referenceTokens = tokens(referenceText);
            final Set<String> commonTokens = new HashSet<>(generatedTokens);
            commonTokens.retainAll(referenceTokens);
            final Set<String> allTokens = new HashSet<>(generatedTokens);
            allTokens.addAll(referenceTokens);
            final double similarity = (double) commonTokens.size() / allTokens.size();

            Assert.assertTrue(similarity >= 0.80,
                    referencePdfPath + " word similarity was " + similarity + " (expected at least 0.80)");
            Assert.assertTrue(generatedTokens.containsAll(tokens(data.getName())),
                    referencePdfPath + " should show the imported character's name");

            for (int page = 1; page <= generatedReader.getNumberOfPages(); page++) {
                final Rectangle generatedSize = generatedReader.getPageSize(page);
                final Rectangle referenceSize = referenceReader.getPageSize(page);
                Assert.assertEquals(generatedSize.getWidth(), referenceSize.getWidth(), 2f,
                        referencePdfPath + " page " + page + " width differs from the reference");
                Assert.assertEquals(generatedSize.getHeight(), referenceSize.getHeight(), 2f,
                        referencePdfPath + " page " + page + " height differs from the reference");
            }
        } finally {
            generatedReader.close();
            referenceReader.close();
        }
    }

    @DataProvider(name = "legacySnapshots")
    public Object[][] legacySnapshots() {
        return new Object[][]{
                {"Bellido/Kara_Trace_N5_Medio-elfo_Magente.rlm"},
                {"Bellido/Kara_Trace_N6_Medio-elfo_Magente.rlm"},
                {"Carlos/Fthagtli_Milupa_N1_Garks_Monje Guerrero.rlm"},
                {"Carlos/Fthagtli_Milupa_N5_Garks_Monje Guerrero.rlm"},
                {"Germes/Skaag_El_Carnicero_N5.rlm"},
                {"Germes/Skaag_El_Carnicero_N6.rlm"},
                {"Germes/Skaag_El_Carnicero_N7_Troll de Guerra_Luchador.rlm"},
                {"Germes/Skaag_El_Carnicero_N8_Troll de Guerra_Luchador.rlm"},
                {"Javi/Morticia_Innodan_N10_Ilourianos_Elementalista.rlm"},
                {"Javi/Morticia_Innodan_N11_Ilourianos_Elementalista.rlm"},
                {"Javi/Morticia_Innodan_N12_Ilourianos_Elementalista.rlm"},
                {"Jorge/Ophiliaglii_Humbiton_N7_Jhordi_Monje Zen.rlm"},
                {"Kobold Barrendero/Gorju_Ping_N5_Kobolds_Lego.rlm"},
                {"Paola/Nihal_Dragoniana_N7.rlm"},
                {"Paola/Nihal_Dragoniana_N7_Laan_Lord del Caos.rlm"},
                {"Paola/Nihal_Dragoniana_N8_Laan_Lord del Caos.rlm"},
                {"Paola/Nihal_Dragoniana_N9_Laan_Lord del Caos.rlm"}
        };
    }

    @DataProvider(name = "legacyPdfPairs")
    public Object[][] legacyPdfPairs() {
        return new Object[][]{
                {"Bellido/Kara_Trace_N5_Medio-elfo_Magente.rlm",
                        "Bellido/Kara_Trace_N5_Medio-elfo_Magente.pdf", "combined"},
                {"Bellido/Kara_Trace_N6_Medio-elfo_Magente.rlm",
                        "Bellido/Kara_Trace_N6_Medio-elfo_Magente.pdf", "combined"},
                {"Carlos/Fthagtli_Milupa_N5_Garks_Monje Guerrero.rlm",
                        "Carlos/Fthagtli_Milupa_N5_Garks_Monje Guerrero.pdf", "standard"},
                {"Germes/Skaag_El_Carnicero_N6.rlm", "Germes/Skaag_El_Carnicero_N6.pdf", "standard"},
                {"Germes/Skaag_El_Carnicero_N6.rlm",
                        "Germes/Skaag_El_Carnicero_N6_Combinada.pdf", "combined"},
                {"Germes/Skaag_El_Carnicero_N6.rlm",
                        "Germes/Skaag_El_Carnicero_N6_Troll de Guerra_Luchador.pdf", "combined"},
                {"Germes/Skaag_El_Carnicero_N7_Troll de Guerra_Luchador.rlm",
                        "Germes/Skaag_El_Carnicero_N7_Troll de Guerra_Luchador.pdf", "combined"},
                {"Germes/Skaag_El_Carnicero_N8_Troll de Guerra_Luchador.rlm",
                        "Germes/Skaag_El_Carnicero_N8_Troll de Guerra_Luchador.pdf", "combined"},
                {"Javi/Morticia_Innodan_N10_Ilourianos_Elementalista.rlm",
                        "Javi/Morticia_Innodan_N10_Ilourianos_Elementalista.pdf", "standard"},
                {"Javi/Morticia_Innodan_N11_Ilourianos_Elementalista.rlm",
                        "Javi/Morticia_Innodan_N11_Ilourianos_Elementalista.pdf", "combined"},
                {"Javi/Morticia_Innodan_N12_Ilourianos_Elementalista.rlm",
                        "Javi/Morticia_Innodan_N12_Ilourianos_Elementalista.pdf", "combined-skills"},
                {"Jorge/Ophiliaglii_Humbiton_N7_Jhordi_Monje Zen.rlm",
                        "Jorge/Ophiliaglii_Humbiton_N7_Jhordi_Monje Zen.pdf", "combined"},
                {"Kobold Barrendero/Gorju_Ping_N5_Kobolds_Lego.rlm",
                        "Kobold Barrendero/Gorju_Ping_N5_Kobolds_Lego.pdf", "standard"},
                {"Paola/Nihal_Dragoniana_N7.rlm", "Paola/Nihal_Dragoniana_N7.pdf", "standard"},
                {"Paola/Nihal_Dragoniana_N7_Laan_Lord del Caos.rlm",
                        "Paola/Nihal_Dragoniana_N7_Laan_Lord del Caos.pdf", "standard"},
                {"Paola/Nihal_Dragoniana_N7_Laan_Lord del Caos.rlm",
                        "Paola/Nihal_Dragoniana_N7_Laan_Lord del Caos_std.pdf", "standard"},
                {"Paola/Nihal_Dragoniana_N7_Laan_Lord del Caos.rlm",
                        "Paola/Nihal_Dragoniana_N7_Laan_Lord del Caos_cmb.pdf", "combined"},
                {"Paola/Nihal_Dragoniana_N7_Laan_Lord del Caos.rlm",
                        "Paola/Nihal Dragoniana_N7_Laan_Lord del Caos.pdf", "standard"},
                {"Paola/Nihal_Dragoniana_N7_Laan_Lord del Caos.rlm",
                        "Paola/Nihal Dragoniana_N7_Laan_Lord del Caos_Comb.pdf", "combined"},
                {"Paola/Nihal_Dragoniana_N8_Laan_Lord del Caos.rlm",
                        "Paola/Nihal_Dragoniana_N8_Laan_Lord del Caos.pdf", "standard"},
                {"Paola/Nihal_Dragoniana_N9_Laan_Lord del Caos.rlm",
                        "Paola/Nihal_Dragoniana_N9_Laan_Lord del Caos.pdf", "standard"}
        };
    }

    private static PdfDocument createSheet(String sheetType) {
        return switch (sheetType) {
            case "standard" -> new StandardCharacterSheet();
            case "combined" -> new LegacyCombinedCharacterSheet();
            case "combined-skills" -> new LegacyCombinedSkillsCharacterSheet();
            case "one-column" -> new LegacyCombinedOneColumnCharacterSheet();
            default -> throw new IllegalArgumentException("Unknown sheet type: " + sheetType);
        };
    }

    private static byte[] readFixture(String path) throws IOException {
        try (InputStream input = LegacyPjsRegressionTest.class.getResourceAsStream(FIXTURE_ROOT + path)) {
            Assert.assertNotNull(input, "Missing test fixture " + path);
            return input.readAllBytes();
        }
    }

    private static String readSnapshot(String path) throws IOException {
        final String resource = EXISTING_SNAPSHOTS.getOrDefault(path, FIXTURE_ROOT + "snapshots/" + path);
        try (InputStream input = LegacyPjsRegressionTest.class.getResourceAsStream(resource)) {
            Assert.assertNotNull(input, "Missing character snapshot " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractText(PdfReader reader) throws IOException {
        final PdfTextExtractor extractor = new PdfTextExtractor(reader);
        final StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        return text.toString();
    }

    private static Set<String> tokens(String text) {
        final String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        final Set<String> tokens = new HashSet<>();
        final Matcher matcher = TEXT_TOKEN.matcher(normalized);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }
}
