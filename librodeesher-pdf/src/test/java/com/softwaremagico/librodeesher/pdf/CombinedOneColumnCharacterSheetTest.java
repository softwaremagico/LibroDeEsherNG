package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.equipment.ObjectBonus;
import com.softwaremagico.librodeesher.language.TranslatedText;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

/** Regression coverage for the legacy {@code PdfCombinedSheet1Column} export path. */
@Test(groups = "pdf")
public class CombinedOneColumnCharacterSheetTest {

    @Test
    public void combinedOneColumnSheetGeneratesAValidPdf() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setName("Aragorn");
        character.setRaceId("horseCentaur");
        character.setProfessionId("fighter");
        character.applyProfessionMagicRealms(null);

        final byte[] pdf = new CombinedOneColumnCharacterSheet().generate(character);

        Assert.assertEquals(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII), "%PDF-");
        Assert.assertTrue(pdf.length > 3000);
    }

    @Test
    public void combinedOneColumnSheetPrintsEquipmentAndPerkDetails() throws Exception {
        final CharacterPlayer character = detailedCharacter();

        final String text = extractText(new CombinedOneColumnCharacterSheet().generate(character));

        Assert.assertTrue(text.contains("Equipment details"));
        Assert.assertTrue(text.contains("Magic item details"));
        Assert.assertTrue(text.contains("+12"));
        Assert.assertTrue(text.contains("+20"));
        Assert.assertTrue(text.contains("See page 52") || text.contains("Pagina 52"));
        Assert.assertTrue(text.contains("page 90") || text.contains("página 90"));
    }

    private static CharacterPlayer detailedCharacter() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setName("Aragorn");
        character.setRaceId("horseCentaur");
        character.setProfessionId("fighter");
        character.applyProfessionMagicRealms(null);
        character.addStandardEquipment(new Equipment(new TranslatedText("Rope", "Rope"),
                new TranslatedText("Equipment details", "Equipment details")));
        character.addMagicItem(new MagicObject(new TranslatedText("Ring", "Ring"),
                new TranslatedText("Magic item details", "Magic item details"),
                List.of(new ObjectBonus(BonusType.SKILL, "sword", 12))));
        character.addPerk("acrobat");
        Assert.assertTrue(character.addWeakness("acrobat", "slightAddiction"));
        return character;
    }

    private static String extractText(byte[] pdf) throws Exception {
        final PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
        final PdfTextExtractor extractor = new PdfTextExtractor(reader);
        final StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        reader.close();
        return text.toString();
    }
}
