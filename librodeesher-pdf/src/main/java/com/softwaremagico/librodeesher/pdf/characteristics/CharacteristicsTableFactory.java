package com.softwaremagico.librodeesher.pdf.characteristics;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.Characteristic;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;

/**
 * The character sheet's characteristics table: one row per {@link Characteristics#getCharacteristics()}
 * (the ten real characteristics, excluding Appearance), showing its temporal value and total bonus
 * (see {@link CharacterPlayer#getCharacteristicTemporalValue}/{@link
 * CharacterPlayer#getCharacteristicTotalBonus}).
 */
public class CharacteristicsTableFactory extends BaseElement {

    private static final float[] WIDTHS = {2f, 1f, 1f};

    private CharacteristicsTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getCharacteristicsTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);

        table.addCell(getTitleCell("Characteristics", WIDTHS.length));

        table.addCell(getLabelCell("Characteristic"));
        table.addCell(getLabelCell("Value"));
        table.addCell(getLabelCell("Bonus"));

        for (final Characteristic characteristic : Characteristics.getCharacteristics()) {
            final String name = characteristic.getAbbreviation().name();
            table.addCell(getPlainCell(name.charAt(0) + name.substring(1).toLowerCase().replace('_', ' ')));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getCharacteristicTemporalValue(characteristic.getAbbreviation()))));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getCharacteristicTotalBonus(characteristic.getAbbreviation()))));
        }

        return table;
    }
}
