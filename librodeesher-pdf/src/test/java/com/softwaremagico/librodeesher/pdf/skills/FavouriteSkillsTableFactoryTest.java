package com.softwaremagico.librodeesher.pdf.skills;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies favourite skill selections are available to PDF renderers. */
@Test(groups = "pdf")
public class FavouriteSkillsTableFactoryTest {

    @Test
    public void favouriteSkillsTableRendersSelectedSkills() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.addFavouriteSkill("climbing");

        final var table = FavouriteSkillsTableFactory.getFavouriteSkillsTable(character);

        Assert.assertEquals(character.getFavouriteSkillIds(), java.util.List.of("climbing"));
        Assert.assertEquals(table.size(), 3);
    }
}
