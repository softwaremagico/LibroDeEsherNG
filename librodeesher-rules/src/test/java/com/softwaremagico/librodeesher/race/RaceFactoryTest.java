package com.softwaremagico.librodeesher.race;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link RaceFactory} against the real generated {@code races.xml} data. */
@Test(groups = "raceFactory")
public class RaceFactoryTest {

    @Test
    public void readsRacesFromEveryEnabledModule() throws InvalidXmlElementException {
        Assert.assertTrue(RaceFactory.getInstance().getElements().size() >= 60);
    }

    @Test
    public void grayOrcHasExpectedRaceData() throws InvalidXmlElementException {
        final Race grayOrc = RaceFactory.getInstance().getElement("grayOrc");

        Assert.assertEquals(grayOrc.getName().getSpanish(), "Orco Gris");
        Assert.assertFalse(grayOrc.getCharacteristicBonuses().isEmpty());
        Assert.assertFalse(grayOrc.getResistanceBonuses().isEmpty());
        Assert.assertFalse(grayOrc.getProgressionRankValues().isEmpty());
        Assert.assertTrue(grayOrc.getExpectedLifeYears() > 0);
    }

    @Test(expectedExceptions = InvalidXmlElementException.class)
    public void unknownRaceIdThrows() throws InvalidXmlElementException {
        RaceFactory.getInstance().getElement("doesNotExist");
    }
}
