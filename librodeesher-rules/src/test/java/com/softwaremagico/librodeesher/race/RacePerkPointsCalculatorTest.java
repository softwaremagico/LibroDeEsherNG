package com.softwaremagico.librodeesher.race;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies {@link RacePerkPointsCalculator} against real race data, matching the legacy
 * {@code PerksTests}' per-race assertions exactly (the same 9 races, the same expected values).
 */
@Test(groups = "racePerkPointsCalculator")
public class RacePerkPointsCalculatorTest {

    @Test
    public void laanPerkPoints() throws InvalidXmlElementException {
        assertPerkPoints("laan", 45);
    }

    @Test
    public void horseCentaurPerkPoints() throws InvalidXmlElementException {
        assertPerkPoints("horseCentaur", 55);
    }

    @Test
    public void lionCentaurPerkPoints() throws InvalidXmlElementException {
        assertPerkPoints("lionCentaur", 50);
    }

    @Test
    public void dyariPerkPoints() throws InvalidXmlElementException {
        assertPerkPoints("dyari", 30);
    }

    @Test
    public void dragonetPerkPoints() throws InvalidXmlElementException {
        assertPerkPoints("dragonet", 0);
    }

    @Test
    public void warTrollPerkPoints() throws InvalidXmlElementException {
        assertPerkPoints("warTroll", 0);
    }

    @Test
    public void funguidosPerkPoints() throws InvalidXmlElementException {
        assertPerkPoints("funguidos", 15);
    }

    @Test
    public void ilourianosPerkPoints() throws InvalidXmlElementException {
        assertPerkPoints("ilourianos", 50);
    }

    @Test
    public void jhordiPerkPoints() throws InvalidXmlElementException {
        assertPerkPoints("jhordi", 55);
    }

    /**
     * Matches the legacy {@code ReadFilesTest#readAllRaceFiles}: every race's perk points can be
     * computed without failure, except the one documented, pre-existing data quirk (see {@link
     * RacePerkPointsCalculator}'s own javadoc) that would also make the legacy calculator fail.
     */
    @Test
    public void everyRaceComputesPerkPointsExceptTheKnownDataQuirk() throws InvalidXmlElementException {
        int computed = 0;
        for (final Race race : RaceFactory.getInstance().getElements()) {
            if (race.getId().equals("grayOrc")) {
                Assert.assertThrows(IllegalStateException.class, () -> new RacePerkPointsCalculator(race).getPerkPoints());
                continue;
            }
            final int perkPoints = new RacePerkPointsCalculator(race).getPerkPoints();
            Assert.assertTrue(perkPoints >= 0 && perkPoints <= 65);
            computed++;
        }
        Assert.assertTrue(computed > 60);
    }

    private static void assertPerkPoints(String raceId, int expected) throws InvalidXmlElementException {
        final Race race = RaceFactory.getInstance().getElement(raceId);
        Assert.assertEquals(new RacePerkPointsCalculator(race).getPerkPoints(), expected);
    }
}
