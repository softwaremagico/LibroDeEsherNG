package com.softwaremagico.librodeesher.character;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.level.LevelUp;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link CharacterPlayer}'s identity, characteristic and level bookkeeping. */
@Test(groups = "character")
public class CharacterPlayerTest {

    @Test
    public void everyCharacteristicStartsAtTheInitialValue() {
        final CharacterPlayer character = new CharacterPlayer();
        for (final CharacteristicAbbreviation abbreviation : CharacteristicAbbreviation.values()) {
            if (abbreviation == CharacteristicAbbreviation.NONE) {
                continue;
            }
            Assert.assertEquals(character.getCharacteristicTemporalValue(abbreviation),
                    Integer.valueOf(Characteristics.INITIAL_CHARACTERISTIC_VALUE));
        }
    }

    @Test
    public void startsAtLevelOneWithOneEmptyLevelUp() {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.getLevel(), 1);
        Assert.assertNotNull(character.getCurrentLevel());
        Assert.assertSame(character.getCurrentLevel(), character.getLevels().get(0));
    }

    @Test
    public void increaseLevelAddsANewCurrentLevel() {
        final CharacterPlayer character = new CharacterPlayer();
        final LevelUp firstLevel = character.getCurrentLevel();
        final LevelUp secondLevel = character.increaseLevel();

        Assert.assertEquals(character.getLevel(), 2);
        Assert.assertSame(character.getCurrentLevel(), secondLevel);
        Assert.assertNotSame(secondLevel, firstLevel);
    }

    @Test
    public void temporalBonusIsComputedFromTheStandardTable() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCharacteristicTemporalValue(CharacteristicAbbreviation.STRENGTH, 90);
        Assert.assertEquals(character.getCharacteristicTemporalBonus(CharacteristicAbbreviation.STRENGTH),
                Characteristics.getTemporalBonus(90));
    }

    @Test
    public void raceBonusIsZeroWithoutARaceSelected() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertEquals(character.getCharacteristicRaceBonus(CharacteristicAbbreviation.STRENGTH), Integer.valueOf(0));
        Assert.assertNull(character.getRace());
    }

    @Test
    public void totalBonusCombinesTemporalAndRaceBonuses() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("grayOrc");
        character.setCharacteristicTemporalValue(CharacteristicAbbreviation.CONSTITUTION, 90);

        final Integer temporalBonus = character.getCharacteristicTemporalBonus(CharacteristicAbbreviation.CONSTITUTION);
        final Integer raceBonus = character.getCharacteristicRaceBonus(CharacteristicAbbreviation.CONSTITUTION);
        Assert.assertEquals(raceBonus, Integer.valueOf(4));
        Assert.assertEquals(character.getCharacteristicTotalBonus(CharacteristicAbbreviation.CONSTITUTION),
                Integer.valueOf(temporalBonus + raceBonus));
    }

    @Test
    public void unselectedRaceCultureAndProfessionResolveToNull() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        Assert.assertNull(character.getRace());
        Assert.assertNull(character.getCulture());
        Assert.assertNull(character.getProfession());
    }

    @Test
    public void selectedRaceCultureAndProfessionResolveThroughTheCatalog() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("grayOrc");
        Assert.assertEquals(character.getRace().getId(), "grayOrc");
    }

    @Test
    public void rollingThePotentialValueUsesTheStandardTable() {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCharacteristicTemporalValue(CharacteristicAbbreviation.AGILITY, 100);
        final Integer potential = character.rollCharacteristicPotentialValue(CharacteristicAbbreviation.AGILITY);
        Assert.assertTrue(potential >= 100);
        Assert.assertEquals(character.getCharacteristicPotentialValue(CharacteristicAbbreviation.AGILITY), potential);
    }

    @Test
    public void appearanceTotalCombinesTheRollWithPresence() {
        final CharacterPlayer character = new CharacterPlayer();
        character.setCharacteristicPotentialValue(CharacteristicAbbreviation.PRESENCE, 90);
        Assert.assertEquals(character.getAppearanceTotal(), character.getAppearance().getTotal(90));
    }
}
