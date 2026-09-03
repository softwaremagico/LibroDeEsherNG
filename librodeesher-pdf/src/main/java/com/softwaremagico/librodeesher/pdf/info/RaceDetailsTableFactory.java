package com.softwaremagico.librodeesher.pdf.info;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.race.Race;

/** Builds the legacy sheet's race and physical-profile values. */
public final class RaceDetailsTableFactory extends BaseElement {
    private static final float[] WIDTHS = {2f, 1f};

    private RaceDetailsTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getRaceDetailsTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);
        table.addCell(getTitleCell("Physical Profile", WIDTHS.length));

        final Race race = characterPlayer.getRace();
        addValue(table, "Appearance", characterPlayer.getAppearanceTotal());
        addValue(table, "Natural armor", race == null ? "" : race.getNaturalArmorType());
        addValue(table, "Restoration time", race == null ? "" : race.getRestorationTime());
        addValue(table, "Soul departure", race == null ? "" : race.getSoulDepartTime());
        return table;
    }

    private static void addValue(PdfPTable table, String label, Object value) {
        table.addCell(getLabelCell(label));
        table.addCell(getValueCell(value == null ? "" : String.valueOf(value)));
    }
}
