package com.softwaremagico.librodeesher.pdf.info;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies derived stats include the legacy experience threshold. */
@Test(groups = "pdf")
public class DerivedStatsTableFactoryTest {

    @Test
    public void derivedStatsTableIncludesMinimumExperienceForCurrentLevel() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.increaseLevel();

        final var table = DerivedStatsTableFactory.getDerivedStatsTable(character);

        Assert.assertEquals(table.size(), 9);
    }
}
