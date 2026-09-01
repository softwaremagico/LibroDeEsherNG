package com.softwaremagico.librodeesher.race;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.language.LanguageSlot;
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

    /**
     * Matches the legacy {@code ReadFilesTest#readRaceOptionalLanguages}, against the same 3 real
     * races.
     */
    @Test
    public void racesExposeTheirOptionalLanguageSlots() throws InvalidXmlElementException {
        final Race doppleganger = RaceFactory.getInstance().getElement("doppleganger");
        Assert.assertEquals(doppleganger.getOptionalRaceLanguages().size(), 1);
        final LanguageSlot slot = doppleganger.getOptionalRaceLanguages().get(0);
        Assert.assertEquals(slot.getStartingSpeakingRanks(), Integer.valueOf(0));
        Assert.assertEquals(slot.getStartingWritingRanks(), Integer.valueOf(0));
        Assert.assertEquals(slot.getMaxSpeakingRanks(), Integer.valueOf(10));
        Assert.assertEquals(slot.getMaxWritingRanks(), Integer.valueOf(10));

        final Race laan = RaceFactory.getInstance().getElement("laan");
        Assert.assertEquals(laan.getOptionalRaceLanguages().size(), 2);

        final Race punkari = RaceFactory.getInstance().getElement("punkari");
        Assert.assertEquals(punkari.getOptionalRaceLanguages().size(), 1);
    }
}
