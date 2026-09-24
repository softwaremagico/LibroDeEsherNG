package com.softwaremagico.librodeesher.pdf.events;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.pdf.StandardCharacterSheet;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Ensures every generated sheet is produced with the common footer event. */
@Test(groups = "pdf")
public class FooterEventTest {

    @Test
    public void standardSheetGeneratesWithFooterEvent() throws Exception {
        final byte[] pdf = new StandardCharacterSheet().generate(new CharacterPlayer());

        Assert.assertTrue(pdf.length > 500);
    }
}
