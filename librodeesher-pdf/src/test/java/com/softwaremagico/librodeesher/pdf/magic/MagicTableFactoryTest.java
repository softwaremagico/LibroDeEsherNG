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

        Assert.assertEquals(character.getOtherRealmOpenSpellLists().getFirst().getRealm(), RealmOfMagic.MENTALISM);

        final var table = MagicTableFactory.getMagicTable(character);

        Assert.assertTrue(table.size() > character.getOpenSpellLists().size() * 4 + 5);
    }
}
