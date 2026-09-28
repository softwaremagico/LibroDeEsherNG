package com.softwaremagico.librodeesher.pdf.skills;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/** Verifies the two "most used" blocks, matching the legacy {@code PdfStandardSheet}. */
@Test(groups = "pdf")
public class FavouriteSkillsTableFactoryTest {

    @Test
    public void skillsTableHasTwoPaddedColumns() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.addFavouriteSkill("climbing");

        final var table = FavouriteSkillsTableFactory.getFavouriteSkillsTable(character);

        Assert.assertEquals(character.getFavouriteNoOffensiveSkills().stream().map(s -> s.getId()).toList(),
                List.of("climbing"));
        // One row per MOST_USED_SKILLS_LINES line, split over two columns.
        Assert.assertEquals(table.size(), CharacterPlayer.MOST_USED_SKILLS_LINES);
    }

    @Test
    public void attacksTableHasPaddedRows() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.addFavouriteSkill("sword");

        final var table = FavouriteSkillsTableFactory.getFavouriteAttacksTable(character);

        Assert.assertEquals(character.getFavouriteOffensiveSkills().stream().map(s -> s.getId()).toList(),
                List.of("sword"));
        Assert.assertEquals(table.size(), CharacterPlayer.MOST_USED_ATTACKS_LINES);
    }

    @Test
    public void skillsAreListedByTheirSpanishRulebookName() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        // The legacy caps by value and then re-sorts by the raw name read from the rulebook files,
        // which are the Spanish ones: "Acechar" (stalking) < "Trepar" (climbing).
        character.addFavouriteSkill("climbing");
        character.addFavouriteSkill("stalking");

        Assert.assertEquals(character.getFavouriteNoOffensiveSkills().stream().map(s -> s.getId()).toList(),
                List.of("stalking", "climbing"));
    }

    @Test
    public void offensiveFavouritesAreSplitOutOfTheSkillsTable() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.addFavouriteSkill("sword");
        character.addFavouriteSkill("climbing");
        character.addFavouriteSkill("stalking");

        final List<String> attacks = character.getFavouriteOffensiveSkills().stream()
                .map(s -> s.getId()).toList();
        final List<String> skills = character.getFavouriteNoOffensiveSkills().stream()
                .map(s -> s.getId()).toList();

        Assert.assertEquals(attacks, List.of("sword"));
        Assert.assertEquals(skills, List.of("stalking", "climbing"));
        // The two lists are disjoint and together cover every favourite.
        Assert.assertEquals(character.getFavouriteSkillIds().size(), attacks.size() + skills.size());
    }
}
