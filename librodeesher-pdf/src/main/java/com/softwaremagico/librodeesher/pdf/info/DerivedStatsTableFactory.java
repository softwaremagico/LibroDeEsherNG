package com.softwaremagico.librodeesher.pdf.info;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;

/**
 * The character sheet's derived stats table: movement, defensive bonus, armour class, power points
 * and the current level's remaining development/background points.
 */
public class DerivedStatsTableFactory extends BaseElement {

    private static final float[] WIDTHS = {1f, 1f, 1f};

    private DerivedStatsTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getDerivedStatsTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);

        table.addCell(getTitleCell("Stats", WIDTHS.length));

        addStat(table, "Movement", characterPlayer.getMovementCapacity());
        addStat(table, "Defensive Bonus", characterPlayer.getDefensiveBonus());
        addStat(table, "Armour Class", characterPlayer.getArmourClass());
        addStat(table, "Power Points", characterPlayer.getPowerPoints());
        addStat(table, "Development Points Left", characterPlayer.getRemainingDevelopmentPoints());
        addStat(table, "Background Points Left", characterPlayer.getRemainingBackgroundPoints());

        return table;
    }

    private static void addStat(PdfPTable table, String label, int value) {
        table.addCell(getLabelCell(label));
        table.addCell(getValueCell(String.valueOf(value)));
        table.addCell(getPlainCell(""));
    }
}
