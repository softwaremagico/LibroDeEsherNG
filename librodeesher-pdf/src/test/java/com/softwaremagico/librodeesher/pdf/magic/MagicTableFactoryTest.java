package com.softwaremagico.librodeesher.pdf.magic;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies every spell list available to a character is rendered. */
@Test(groups = "pdf")
public class MagicTableFactoryTest {

    @Test
    public void magicTableIncludesSpellListsFromOtherRealms() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setProfessionId("wizard");
        character.applyProfessionMagicRealms(null);

        Assert.assertNotEquals(character.getOtherRealmOpenSpellLists().get(0).getRealm(), RealmOfMagic.ESSENCE);

        final var table = MagicTableFactory.getMagicTable(character);

        Assert.assertTrue(table.size() > character.getOpenSpellLists().size() * 5 + 6);
    }

    @Test
    public void magicTableIncludesPerkBonusForSpellListTypes() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setProfessionId("wizard");
        character.applyProfessionMagicRealms(null);
        character.addPerk("magicalAbility");

        final var table = MagicTableFactory.getMagicTable(character);

        Assert.assertTrue(table.size() > 6);
    }
}
