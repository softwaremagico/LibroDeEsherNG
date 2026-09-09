package com.softwaremagico.librodeesher.pdf.skills;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies both legacy skill ordering choices can be rendered. */
@Test(groups = "pdf")
public class SkillsTableFactoryTest {

    @Test
    public void skillsTableSupportsCategoryAndAlphabeticalOrder() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();

        final var grouped = SkillsTableFactory.getSkillsTable(character, false);
        final var alphabetical = SkillsTableFactory.getSkillsTable(character, true);

        Assert.assertTrue(grouped.size() > 1);
        Assert.assertTrue(alphabetical.size() > 1);

        character.setProfessionId("fighter");
        Assert.assertTrue(SkillsTableFactory.getSkillsTable(character, false).size() > 1);
    }
}
