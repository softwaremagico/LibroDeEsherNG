package com.softwaremagico.librodeesher.pdf.info;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.race.Race;

/**
 * The character sheet's header table: name, race, culture, profession and current level.
 */
public class CharacterBasicsTableFactory extends BaseElement {

    private static final float[] WIDTHS = {2f, 1f, 1f, 1f, 1f};

    private CharacterBasicsTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getCharacterBasicsTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);

        table.addCell(getTitleCell("Name", 1));
        table.addCell(getTitleCell("Race", 1));
        table.addCell(getTitleCell("Culture", 1));
        table.addCell(getTitleCell("Profession", 1));
        table.addCell(getTitleCell("Level", 1));

        final Race race = characterPlayer.getRace();
        final Culture culture = characterPlayer.getCulture();
        final Profession profession = characterPlayer.getProfession();

        table.addCell(getPlainCell(characterPlayer.getName() == null ? "" : characterPlayer.getName()));
        table.addCell(getPlainCell(race == null ? "" : race.getName().getEnglish()));
        table.addCell(getPlainCell(culture == null ? "" : culture.getName().getEnglish()));
        table.addCell(getPlainCell(profession == null ? "" : profession.getName().getEnglish()));
        table.addCell(getPlainCell(String.valueOf(characterPlayer.getLevels().size())));

        return table;
    }
}
