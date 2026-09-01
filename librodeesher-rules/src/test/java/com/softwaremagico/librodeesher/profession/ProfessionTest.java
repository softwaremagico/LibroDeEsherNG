package com.softwaremagico.librodeesher.profession;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/** Verifies {@link Profession}'s bonus lookup and preferred-characteristic query in isolation. */
@Test(groups = "profession")
public class ProfessionTest {

    @Test
    public void bonusIsZeroWhenNotGranted() {
        final Profession profession = new Profession("test");
        profession.setBonuses(List.of(new ProfessionBonus("loreArcane", 10)));

        Assert.assertEquals(profession.getBonus("loreArcane"), Integer.valueOf(10));
        Assert.assertEquals(profession.getBonus("unrelated"), Integer.valueOf(0));
    }

    @Test
    public void onlyThePrimaryAndSecondaryPreferenceCount() {
        final Profession profession = new Profession("test");
        profession.setCharacteristicPreferences(List.of(CharacteristicAbbreviation.EMPATHY, CharacteristicAbbreviation.REASONING,
                CharacteristicAbbreviation.MEMORY));

        Assert.assertTrue(profession.isPreferredCharacteristic(CharacteristicAbbreviation.EMPATHY));
        Assert.assertTrue(profession.isPreferredCharacteristic(CharacteristicAbbreviation.REASONING));
        Assert.assertFalse(profession.isPreferredCharacteristic(CharacteristicAbbreviation.MEMORY));
        Assert.assertFalse(profession.isPreferredCharacteristic(CharacteristicAbbreviation.STRENGTH));
    }

    @Test
    public void indifferentProfessionPrefersNoCharacteristic() {
        final Profession profession = new Profession("test");
        Assert.assertTrue(profession.isIndifferentToCharacteristics());
        Assert.assertFalse(profession.isPreferredCharacteristic(CharacteristicAbbreviation.EMPATHY));
    }
}
