package com.softwaremagico.librodeesher.pdf.skills;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;

/** Renders manually selected frequently-used skills from the legacy character sheet. */
public final class FavouriteSkillsTableFactory extends BaseElement {
    private FavouriteSkillsTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getFavouriteSkillsTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(new float[]{3f, 1f, 1f});
        setTableProperties(table);
        table.addCell(getTitleCell("Favourite Skills", 3));
        table.addCell(getLabelCell("Skill"));
        table.addCell(getLabelCell("Ranks"));
        table.addCell(getLabelCell("Total"));
        for (final String skillId : characterPlayer.getFavouriteSkillIds()) {
            final Skill skill = RulesCatalog.getInstance().getSkill(skillId);
            table.addCell(getPlainCell(getText(skill.getName())));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getSkillTotalRanks(skillId))));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getSkillTotalBonus(
                    RulesCatalog.getInstance().getCategory(skill.getCategoryId()), skillId))));
        }
        return table;
    }
}
