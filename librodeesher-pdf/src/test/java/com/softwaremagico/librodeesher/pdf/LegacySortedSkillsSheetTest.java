package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.persistence.CharacterData;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.LegacyCharacterJsonImporter;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The sorted variant of the legacy skills page: one alphabetical sequence for skills and spell lists. */
@Test(groups = "pdf")
public class LegacySortedSkillsSheetTest {

    private static final String NIHAL = "Nihal_level9_2_0_0.json";

    private CharacterPlayer character;
    private String sortedSheetText;

    @BeforeClass
    public void importNihalAndPaintItsSortedSheet() throws Exception {
        final CharacterData data;
        try (final InputStream in = getClass().getResourceAsStream("/" + NIHAL)) {
            Assert.assertNotNull(in, "Missing snapshot " + NIHAL);
            data = LegacyCharacterJsonImporter.importLegacyJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        character = CharacterDataMapper.toCharacter(data);
        character.setSortPdfSkills(true);

        final byte[] pdf = new StandardCharacterSheet().generate(character);
        final PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
        final PdfTextExtractor extractor = new PdfTextExtractor(reader);
        final StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        reader.close();
        sortedSheetText = text.toString();
    }

    @Test
    public void paintsTheSkillsPageInOneAlphabeticalSequence() {
        final int firstList = sortedSheetText.indexOf("Arma del Caos");
        final int lastList = sortedSheetText.indexOf("Oscuridad");
        final int skillAfterTheLists = sortedSheetText.indexOf("Preparar Hierbas");
        final int lastSkill = sortedSheetText.indexOf("Rumorear");

        Assert.assertTrue(firstList > 0, "the learned spell lists are painted");
        Assert.assertTrue(firstList < lastList, "the spell lists keep their own alphabetical order");
        Assert.assertTrue(lastList < skillAfterTheLists, "the spell lists are mixed in, not appended at the end");
        Assert.assertTrue(skillAfterTheLists < lastSkill, "the skills keep their alphabetical order");
        // "Arma del Caos" also appears in the compact summary line of page 1, so the row is
        // looked up by its rank to prove it is painted once and not twice.
        int paintedRows = 0;
        final Matcher rows = Pattern.compile("Arma del Caos\\s*7\\b").matcher(sortedSheetText);
        while (rows.find()) {
            paintedRows++;
        }
        Assert.assertEquals(paintedRows, 1, "every row is painted once");
    }

    @Test
    public void suffixDependsOnWhatTheCharacterMayTakeTheSkillFor() {
        Assert.assertTrue(sortedSheetText.contains("Demonología (p)"),
                "the sheet says that only a profession grants Demonología");
        Assert.assertFalse(sortedSheetText.contains("Demonología\n"),
                "the professional suffix is not dropped");
    }

}
