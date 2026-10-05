package com.softwaremagico.librodeesher.pdf.legacy;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.persistence.CharacterData;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.LegacyCharacterJsonImporter;
import com.softwaremagico.librodeesher.pdf.StandardCharacterSheet;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The values page of the legacy sheet held sixty category rows and the catalog has grown past that,
 * so the last categories (Subterfugio·Sigilo and the three Técnica/Comercio ones) used to fall off
 * the page without any notice. Every category the options enable must be painted instead, and the
 * filler rows only appear when there are fewer categories than the legacy page had room for.
 */
@Test(groups = "pdf")
public class LegacyCategoriesSheetTest {

    private static final String NIHAL = "Nihal_level9_2_0_0.json";

    /** The legacy sheet of this very character is eight pages long. */
    private static final int LEGACY_PAGE_COUNT = 8;

    private CharacterPlayer character;
    private String sheetText;
    private int pageCount;

    @BeforeClass
    public void importNihalAndPaintItsSheet() throws Exception {
        final CharacterData data;
        try (final InputStream in = getClass().getResourceAsStream("/" + NIHAL)) {
            Assert.assertNotNull(in, "Missing snapshot " + NIHAL);
            data = LegacyCharacterJsonImporter.importLegacyJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        character = CharacterDataMapper.toCharacter(data);

        final byte[] pdf = new StandardCharacterSheet().generate(character);
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
    public void paintsEveryEnabledCategoryOfTheCatalog() throws InvalidXmlElementException {
        final List<String> missing = new ArrayList<>();
        for (final Category category : enabledCategories()) {
            if (!sheetText.contains(flatten(category.getName().getSpanish()))) {
                missing.add(category.getName().getSpanish());
            }
        }

        Assert.assertTrue(missing.isEmpty(), "categories missing from the sheet: " + missing);
    }

    /** The legacy values page had room for sixty rows and this character enables more categories than
     *  that, which is why the last ones used to fall off. */
    @Test
    public void paintsMoreCategoriesThanTheLegacyPageHadRoomFor() throws InvalidXmlElementException {
        Assert.assertTrue(enabledCategories().size() > 60,
                "the catalog no longer needs the rows the legacy page had room for: "
                        + enabledCategories().size());
    }

    @Test
    public void paintsTheLastCategoryOfTheCatalog() {
        Assert.assertTrue(sheetText.contains("Urbana"), "Urbana is the last category of the catalog");
    }

    @Test
    public void keepsTheLegacyPageCount() {
        Assert.assertEquals(pageCount, LEGACY_PAGE_COUNT, "the sheet grew a page");
    }

    private List<Category> enabledCategories() throws InvalidXmlElementException {
        final List<Category> enabled = new ArrayList<>();
        for (final Category category : LegacyValues.availableCategories()) {
            if (character.isCategoryEnabledByOptions(category)) {
                enabled.add(category);
            }
        }
        return enabled;
    }

    /** The printed text breaks the lines by column, so names are compared without their middle dots
     *  and with single spaces. */
    private static String flatten(String text) {
        return text.replace('·', ' ').replaceAll("\\s+", " ").trim();
    }
}