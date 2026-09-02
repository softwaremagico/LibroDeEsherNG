package com.softwaremagico.librodeesher.pdf.info;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.resistance.ResistanceType;

/**
 * The character sheet's resistance rolls table: one row per {@link ResistanceType}, showing the
 * total bonus (see {@link CharacterPlayer#getResistanceTotalBonus}).
 */
public class ResistancesTableFactory extends BaseElement {

    private static final float[] WIDTHS = {2f, 1f};

    private ResistancesTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getResistancesTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);

        table.addCell(getTitleCell("Resistance Rolls", WIDTHS.length));

        for (final ResistanceType resistanceType : ResistanceType.values()) {
            final String name = resistanceType.name();
            table.addCell(getLabelCell(name.charAt(0) + name.substring(1).toLowerCase()));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getResistanceTotalBonus(resistanceType))));
        }

        return table;
    }
}
