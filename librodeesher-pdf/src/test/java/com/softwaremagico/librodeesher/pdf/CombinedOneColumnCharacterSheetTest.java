package com.softwaremagico.librodeesher.pdf;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

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
}
