package com.softwaremagico.librodeesher.pdf.details;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies history can be rendered for the modern character sheet. */
@Test(groups = "pdf")
public class HistoryTableFactoryTest {

    @Test
    public void historyTableContainsTheCharacterHistory() {
        final CharacterPlayer character = new CharacterPlayer();
        character.setHistoryText("Raised among the hills.");

        final var table = HistoryTableFactory.getHistoryTable(character);

        Assert.assertEquals(table.size(), 2);
    }
}
