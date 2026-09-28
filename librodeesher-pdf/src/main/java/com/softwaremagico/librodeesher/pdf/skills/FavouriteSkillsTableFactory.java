package com.softwaremagico.librodeesher.pdf.skills;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.skill.Skill;

import java.util.List;

/**
 * Renders the character sheet's two "most used" blocks, matching the legacy
 * {@code PdfStandardSheet#createMostUsedSkillsTable} and {@code createMostUsedAttacksTable}.
 */
public final class FavouriteSkillsTableFactory extends BaseElement {

    private FavouriteSkillsTableFactory() {
        // Only static helpers.
    }

    /**
     * The "most used skills" block: every non-offensive favourite, split over two columns and
     * padded with blank lines, as the legacy {@code PdfStandardSheet#createMostUsedSkillsTable} does.
     */
    public static PdfPTable getFavouriteSkillsTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(new float[]{0.49f, 0.51f});
        setTableProperties(table);
        final List<Skill> skills = characterPlayer.getFavouriteNoOffensiveSkills();
        final int lines = CharacterPlayer.MOST_USED_SKILLS_LINES;
        for (int i = 0; i < lines; i++) {
            table.addCell(getPlainCell(skillLineAt(characterPlayer, skills, i)));
            table.addCell(getPlainCell(skillLineAt(characterPlayer, skills, i + lines)));
        }
        return table;
    }

    /**
     * The "most used attacks" block: the offensive favourites, padded with blank lines, as the legacy
     * {@code PdfStandardSheet#createMostUsedAttacksTable} does.
     */
    public static PdfPTable getFavouriteAttacksTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(new float[]{3.1f, 1f, 1f});
        setTableProperties(table);
        final List<Skill> skills = characterPlayer.getFavouriteOffensiveSkills();
        for (int i = 0; i < CharacterPlayer.MOST_USED_ATTACKS_LINES; i++) {
            table.addCell(getPlainCell(skillNameAt(characterPlayer, skills, i)));
            table.addCell(getValueCell(skillRanksAt(characterPlayer, skills, i)));
            table.addCell(getValueCell(skillBonusAt(characterPlayer, skills, i)));
        }
        return table;
    }

    /**
     * The legacy filled the left column with the first {@link CharacterPlayer#MOST_USED_SKILLS_LINES}
     * skills and the right one with the next ones (its column builder consumed the shared list), so
     * the right column starts where the left one ends; anything past the list is a blank line.
     */
    private static String skillLineAt(CharacterPlayer characterPlayer, List<Skill> skills, int index)
            throws InvalidXmlElementException {
        return index >= skills.size() ? "" : skillLine(characterPlayer, skills.get(index));
    }

    private static String skillNameAt(CharacterPlayer characterPlayer, List<Skill> skills, int index)
            throws InvalidXmlElementException {
        return index >= skills.size() ? "" : getText(skills.get(index).getName());
    }

    private static String skillRanksAt(CharacterPlayer characterPlayer, List<Skill> skills, int index)
            throws InvalidXmlElementException {
        if (index >= skills.size()) {
            return "";
        }
        return String.valueOf(characterPlayer.getSkillTotalRanks(skills.get(index).getId()));
    }

    private static String skillBonusAt(CharacterPlayer characterPlayer, List<Skill> skills, int index)
            throws InvalidXmlElementException {
        if (index >= skills.size()) {
            return "";
        }
        return String.valueOf(characterPlayer.getSkillTotalValue(skills.get(index)));
    }

    private static String skillLine(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        return getText(skill.getName()) + "  "
                + characterPlayer.getSkillTotalRanks(skill.getId()) + "  "
                + characterPlayer.getSkillTotalValue(skill);
    }
}
