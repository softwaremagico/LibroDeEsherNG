package com.softwaremagico.librodeesher.race;

import com.softwaremagico.librodeesher.character.SexType;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Random;

/** Verifies {@link Race#getRandomName}. */
@Test(groups = "race")
public class RaceRandomNameTest {

    @Test
    public void maleNameIsDrawnFromTheMaleAndFamilyNameLists() throws InvalidXmlElementException {
        final Race race = RaceFactory.getInstance().getElement("commonMen");
        final Random random = new Random(1);
        for (int i = 0; i < 30; i++) {
            final String name = race.getRandomName(SexType.MALE, random);
            Assert.assertTrue(isFirstAndFamily(name, race.getMaleNames(), race.getFamilyNames()), name);
        }
    }

    @Test
    public void femaleNameIsDrawnFromTheFemaleAndFamilyNameLists() throws InvalidXmlElementException {
        final Race race = RaceFactory.getInstance().getElement("commonMen");
        final Random random = new Random(2);
        for (int i = 0; i < 30; i++) {
            final String name = race.getRandomName(SexType.FEMALE, random);
            Assert.assertTrue(isFirstAndFamily(name, race.getFemaleNames(), race.getFamilyNames()), name);
        }
    }

    /** Whether {@code name} is exactly "someGivenName + space + someFamilyName". */
    private static boolean isFirstAndFamily(String name, List<String> givenNames, List<String> familyNames) {
        for (final String given : givenNames) {
            if (name.startsWith(given + " ") && familyNames.contains(name.substring(given.length() + 1))) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void everyRealRaceWithNamesProducesASeasonedName() throws InvalidXmlElementException {
        final Random random = new Random(3);
        final List<Race> races = RaceFactory.getInstance().getElements();
        for (final Race race : races) {
            if (race.getMaleNames().isEmpty() && race.getFemaleNames().isEmpty()) {
                continue;
            }
            final String maleName = race.getRandomName(SexType.MALE, random);
            Assert.assertFalse(maleName.isEmpty(), "race '" + race.getId() + "' produced an empty male name");
            Assert.assertFalse(maleName.startsWith(" "));
            Assert.assertFalse(maleName.endsWith(" "));
        }
    }

    @Test
    public void raceWithoutNamesDoesNotFail() {
        final Race race = new Race("nameless");
        Assert.assertEquals(race.getRandomName(SexType.MALE, new Random(4)), "");
    }

    @Test
    public void raceWithoutFamilyNamesReturnsOnlyTheGivenName() throws InvalidXmlElementException {
        final Race race = RaceFactory.getInstance().getElement("commonMen");
        race.setFamilyNames(List.of());
        final String name = race.getRandomName(SexType.MALE, new Random(5));
        Assert.assertEquals(name.split(" ").length, 1);
        Assert.assertTrue(race.getMaleNames().contains(name));
    }
}