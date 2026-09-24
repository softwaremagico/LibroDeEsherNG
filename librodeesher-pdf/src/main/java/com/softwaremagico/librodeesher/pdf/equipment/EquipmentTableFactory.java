package com.softwaremagico.librodeesher.pdf.equipment;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds the equipment section, including the legacy distinction between mundane and magic items. */
public final class EquipmentTableFactory extends BaseElement {
    private static final float[] WIDTHS = {1f, 1f};

    private EquipmentTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getEquipmentTable(CharacterPlayer characterPlayer) {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);
        table.addCell(getTitleCell("Equipment", WIDTHS.length));
        table.addCell(getLabelCell("Item"));
        table.addCell(getLabelCell("Type"));

        final List<String> equipment = new ArrayList<>();
        for (final Equipment item : characterPlayer.getAllNotMagicEquipment()) {
            equipment.add(getText(item.getName()));
        }
        equipment.sort(Comparator.naturalOrder());
        for (final String item : equipment) {
            table.addCell(getPlainCell(item));
            table.addCell(getPlainCell("Equipment"));
        }

        final List<MagicObject> magicItems = new ArrayList<>(characterPlayer.getAllMagicItems());
        magicItems.sort(Comparator.comparing(item -> getText(item.getName())));
        for (final MagicObject item : magicItems) {
            table.addCell(getPlainCell(getText(item.getName())));
            table.addCell(getPlainCell("Magic item"));
        }
        return table;
    }
}
