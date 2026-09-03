package com.softwaremagico.librodeesher.pdf;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Regression coverage for the legacy {@code PdfCombinedSheet2Columns} export path. */
@Test(groups = "pdf")
public class CombinedTwoColumnsCharacterSheetTest {

    @Test
    public void combinedTwoColumnsSheetGeneratesAValidPdf() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setName("Gandalf");
        character.setRaceId("horseCentaur");
        character.setProfessionId("wizard");
        character.applyProfessionMagicRealms(null);

        final byte[] pdf = new CombinedTwoColumnsCharacterSheet().generate(character);

        Assert.assertEquals(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII), "%PDF-");
        Assert.assertTrue(pdf.length > 3000);
    }
}
