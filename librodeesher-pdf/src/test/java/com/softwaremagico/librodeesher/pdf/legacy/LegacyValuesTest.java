package com.softwaremagico.librodeesher.pdf.legacy;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The numbers the legacy skill row paints next to every skill, which are the category's total and
 * the skill's ranks value, not the skill's own total value nor its development bonus.
 */
@Test(groups = "pdf")
public class LegacyValuesTest {

    private static final String COMMUNICATION = "communication";

    @Test
    public void skillRowShowsTheCategoryTotalOnEveryRowOfTheCategory() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("laan");
        final Skill hablarLaan = RulesCatalog.getInstance().getSkill("hablarLaan");
        final Skill hablarEmeriano = RulesCatalog.getInstance().getSkill("hablarEmeriano");
        final Category communication = RulesCatalog.getInstance().getCategory(COMMUNICATION);

        Assert.assertEquals(LegacyValues.categoryTotalValue(character, hablarLaan),
                character.getCategoryTotalBonus(communication).intValue());
        Assert.assertEquals(LegacyValues.categoryTotalValue(character, hablarLaan),
                LegacyValues.categoryTotalValue(character, hablarEmeriano));

        // The skill's own total value adds its development bonus, which the legacy sheet
        // prints in the row of the skill's ranks value instead.
        Assert.assertNotEquals(LegacyValues.categoryTotalValue(character, hablarLaan),
                character.getSkillTotalValue(hablarLaan).intValue());
    }

    @Test
    public void skillRowShowsTheRanksValueWithoutTheFlatBonuses() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("laan");
        final Skill hablarLaan = RulesCatalog.getInstance().getSkill("hablarLaan");

        // Laan grants 8 speaking ranks at creation, worth 3 each on a first level.
        Assert.assertEquals(character.getSkillTotalRanks("hablarLaan"), Integer.valueOf(8));
        Assert.assertEquals(LegacyValues.skillRanksValue(character, hablarLaan), 24);

        // The flat bonuses are painted in the column with the (H)/(T) marks instead.
        Assert.assertEquals(LegacyValues.skillSimpleBonus(character, hablarLaan), 0);
    }

    @Test
    public void previousSkillRanksCountTheRaceLanguageRanksAndDropTheCurrentLevel() throws InvalidXmlElementException {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("laan");
        final Skill hablarLaan = RulesCatalog.getInstance().getSkill("hablarLaan");

        // Nothing was bought at the current level, so previous and total ranks are the
        // same 8 ranks of the race.
        Assert.assertEquals(LegacyValues.previousSkillRanks(character, hablarLaan), 8);
    }
}