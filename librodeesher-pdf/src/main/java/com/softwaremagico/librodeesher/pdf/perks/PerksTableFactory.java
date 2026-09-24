package com.softwaremagico.librodeesher.pdf.perks;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.rules.RulesCatalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds the perks and weaknesses section selected for a character. */
public final class PerksTableFactory extends BaseElement {
    private PerksTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getPerksTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(1);
        setTableProperties(table);
        table.addCell(getTitleCell("Perks and Weaknesses", 1));

        final List<String> names = new ArrayList<>();
        for (final SelectedPerk selectedPerk : characterPlayer.getSelectedPerks()) {
            names.add(getText(RulesCatalog.getInstance().getPerk(selectedPerk.getPerkId()).getName()));
            if (selectedPerk.getWeaknessId() != null) {
                names.add(getText(RulesCatalog.getInstance().getPerk(selectedPerk.getWeaknessId()).getName()));
            }
        }
        names.sort(Comparator.naturalOrder());
        for (final String name : names) {
            table.addCell(getPlainCell(name));
        }
        return table;
    }
}
