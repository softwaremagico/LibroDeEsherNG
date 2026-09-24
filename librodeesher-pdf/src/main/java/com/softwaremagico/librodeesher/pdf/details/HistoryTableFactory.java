package com.softwaremagico.librodeesher.pdf.details;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;

/** Renders the character history section present on the legacy sheet's detail page. */
public final class HistoryTableFactory extends BaseElement {
    private HistoryTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getHistoryTable(CharacterPlayer characterPlayer) {
        final PdfPTable table = new PdfPTable(1);
        setTableProperties(table);
        table.addCell(getTitleCell("History", 1));
        table.addCell(getPlainCell(characterPlayer.getHistoryText()));
        return table;
    }
}
