package com.softwaremagico.librodeesher.pdf.info;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies personal and racial values are available to every PDF layout. */
@Test(groups = "pdf")
public class RaceDetailsTableFactoryTest {

    @Test
    public void raceDetailsTableIncludesPhysicalProfileValues() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("horseCentaur");

        final var table = RaceDetailsTableFactory.getRaceDetailsTable(character);

        Assert.assertEquals(table.size(), 5);
    }
}
