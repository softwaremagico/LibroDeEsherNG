package com.softwaremagico.librodeesher.persistence;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.level.LevelUp;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * End-to-end check of the level exchange flow the application uses: a character at level N exports
 * its current level as JSON, a copy of the same character sitting at level N-1 imports it, and the
 * imported level must rebuild the very same character.
 *
 * <p>Runs over the real legacy snapshots instead of synthetic data, so it covers the fields that
 * only populated characters carry (hobby lists, generalised and specialised skills, favourite skills,
 * spell lists, age modifications and the per-level characteristic rolls).</p>
 */
@Test(groups = "persistence")
public class LevelJsonRoundTripTest {

    @DataProvider(name = "legacySnapshots")
    public Object[][] legacySnapshots() {
        return new Object[][]{
                {"/Nihal_level9_2_0_0.json"},
                {"/Ophiliaglii_Humbiton_N7_Jhordi_Monje_Zen.json"},
                {"/Morticia_Innodan_N10_Ilourianos_Elementalista.json"},
                {"/Morticia_Innodan_N11_Ilourianos_Elementalista.json"},
                {"/Morticia_Innodan_N12_Ilourianos_Elementalista.json"},
        };
    }

    @Test(dataProvider = "legacySnapshots")
    public void exportedLevelIsImportedBackAsTheNextOne(String resource) throws Exception {
        final CharacterPlayer original = characterFrom(resource);
        final int level = original.getLevel();
        Assert.assertTrue(level > 1, resource + " must have at least two levels to be replayed");

        final CharacterPlayer target = characterFrom(resource);
        target.getLevels().remove(target.getLevels().size() - 1);
        Assert.assertEquals(target.getLevel(), level - 1);

        final String json = LevelJsonManager.toJson(original);
        final LevelUp imported = LevelJsonManager.fromJson(target, json);
        final LevelUp expected = original.getCurrentLevel();

        Assert.assertEquals(imported.getCategoryRanks(), expected.getCategoryRanks(), "category ranks");
        Assert.assertEquals(imported.getSkillRanks(), expected.getSkillRanks(), "skill ranks");
        Assert.assertEquals(imported.getSpellListRanks(), expected.getSpellListRanks(), "spell list ranks");
        Assert.assertEquals(imported.getSpellsUpdated(), expected.getSpellsUpdated(), "updated spells");
        Assert.assertEquals(imported.getGeneralizedSkills(), expected.getGeneralizedSkills(), "generalised skills");
        Assert.assertEquals(imported.getSkillSpecializations(), expected.getSkillSpecializations(), "specialisations");
        Assert.assertEquals(imported.getFavouriteSkills(), expected.getFavouriteSkills(), "favourite skills");
        Assert.assertEquals(imported.getFavouriteSpellLists(), expected.getFavouriteSpellLists(), "favourite lists");
        Assert.assertEquals(imported.getTrainings(), expected.getTrainings(), "trainings");
        Assert.assertEquals(imported.getAge(), expected.getAge(), "age");
        Assert.assertEquals(imported.getAgeModifications().size(), expected.getAgeModifications().size(), "age modifications");
        assertSameCharacteristicRolls(imported, expected);
    }

    @Test(dataProvider = "legacySnapshots")
    public void reimportingTheExportedLevelRebuildsTheSameCharacter(String resource) throws Exception {
        final CharacterPlayer original = characterFrom(resource);
        final int level = original.getLevel();

        final CharacterPlayer target = characterFrom(resource);
        target.getLevels().remove(target.getLevels().size() - 1);

        final LevelUp imported = LevelJsonManager.fromJson(target, LevelJsonManager.toJson(original));
        final LevelUp replayed = target.increaseLevel();
        replayed.setCategoryRanks(imported.getCategoryRanks());
        replayed.setSkillRanks(imported.getSkillRanks());
        replayed.setSpellListRanks(imported.getSpellListRanks());
        replayed.setSpellsUpdated(imported.getSpellsUpdated());
        replayed.setGeneralizedSkills(imported.getGeneralizedSkills());
        replayed.setSkillSpecializations(imported.getSkillSpecializations());
        replayed.setFavouriteSkills(imported.getFavouriteSkills());
        replayed.setFavouriteSpellLists(imported.getFavouriteSpellLists());
        replayed.setTrainings(imported.getTrainings());
        replayed.setAge(imported.getAge());
        for (final CharacteristicRoll roll : imported.getCharacteristicUpdates()) {
            replayed.addCharacteristicUpdate(roll.getCharacteristicAbbreviation(), roll.getCharacteristicTemporalValue(),
                    roll.getCharacteristicPotentialValue(), roll.getRoll());
        }
        for (final AgeModification modification : imported.getAgeModifications()) {
            replayed.addAgeModification(modification);
        }

        Assert.assertEquals(target.getLevel(), level);
        Assert.assertEquals(target.getCurrentAge(), original.getCurrentAge(), "current age");
        Assert.assertEquals(target.getAgeAtLevel(level), original.getAgeAtLevel(level), "age at the last level");
        Assert.assertEquals(target.getPowerPoints(), original.getPowerPoints(), "power points left");
        Assert.assertEquals(target.getSkillTotalRanks("secondaryPowerPointDevelopment"),
                original.getSkillTotalRanks("secondaryPowerPointDevelopment"), "secondary development ranks");
        for (final CharacteristicAbbreviation abbreviation : CharacteristicAbbreviation.values()) {
            Assert.assertEquals(target.getCharacteristicTemporalValue(abbreviation),
                    original.getCharacteristicTemporalValue(abbreviation), "temporal " + abbreviation);
            Assert.assertEquals(target.getCharacteristicPotentialValue(abbreviation),
                    original.getCharacteristicPotentialValue(abbreviation), "potential " + abbreviation);
        }
        Assert.assertEquals(target.getFinalAge(), original.getFinalAge(), "final age");
        Assert.assertEquals(target.getAppearanceTotal(), original.getAppearanceTotal(), "appearance");

        Assert.assertEquals(CharacterJsonManager.toJson(CharacterDataMapper.toData(target)),
                CharacterJsonManager.toJson(CharacterDataMapper.toData(original)),
                "the replayed character must be indistinguishable from the exported one");
    }

    /** {@link CharacteristicRoll} has no value equality, so the rolls are compared field by field. */
    private static void assertSameCharacteristicRolls(LevelUp imported, LevelUp expected) {
        final List<CharacteristicRoll> actualRolls = imported.getCharacteristicUpdates();
        final List<CharacteristicRoll> expectedRolls = expected.getCharacteristicUpdates();
        Assert.assertEquals(actualRolls.size(), expectedRolls.size(), "characteristic rolls");
        for (int i = 0; i < expectedRolls.size(); i++) {
            final CharacteristicRoll actual = actualRolls.get(i);
            final CharacteristicRoll original = expectedRolls.get(i);
            final String message = "characteristic roll " + i;
            Assert.assertEquals(actual.getCharacteristicAbbreviation(), original.getCharacteristicAbbreviation(), message);
            Assert.assertEquals(actual.getCharacteristicTemporalValue(), original.getCharacteristicTemporalValue(), message);
            Assert.assertEquals(actual.getCharacteristicPotentialValue(), original.getCharacteristicPotentialValue(), message);
            Assert.assertEquals(actual.getRoll() == null, original.getRoll() == null, message);
            if (original.getRoll() != null) {
                Assert.assertEquals(actual.getRoll().getFirstDice(), original.getRoll().getFirstDice(), message);
                Assert.assertEquals(actual.getRoll().getSecondDice(), original.getRoll().getSecondDice(), message);
            }
        }
    }

    private static CharacterPlayer characterFrom(String resource) throws IOException, InvalidXmlElementException {
        return CharacterDataMapper.toCharacter(LegacyCharacterJsonImporter.importLegacyJson(read(resource)));
    }

    private static String read(String resource) throws IOException {
        try (InputStream stream = LevelJsonRoundTripTest.class.getResourceAsStream(resource)) {
            Assert.assertNotNull(stream, "missing test resource " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}