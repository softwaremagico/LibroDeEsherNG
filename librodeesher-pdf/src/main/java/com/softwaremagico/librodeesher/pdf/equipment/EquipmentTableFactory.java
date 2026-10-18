package com.softwaremagico.librodeesher.pdf.equipment;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.equipment.ObjectBonus;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.rules.RulesCatalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds the equipment section, including the legacy distinction between mundane and magic items. */
public final class EquipmentTableFactory extends BaseElement {
    private static final float[] WIDTHS = {3f, 5f, 1.5f};

    private EquipmentTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getEquipmentTable(CharacterPlayer characterPlayer) {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);
        table.addCell(getTitleCell("Equipment", WIDTHS.length));
        table.addCell(getLabelCell("Item"));
        table.addCell(getLabelCell("Description / Bonuses"));
        table.addCell(getLabelCell("Type"));

        final List<Equipment> equipment = new ArrayList<>(characterPlayer.getAllNotMagicEquipment());
        equipment.sort(Comparator.comparing(item -> getText(item.getName())));
        for (final Equipment item : equipment) {
            table.addCell(getPlainCell(getText(item.getName())));
            table.addCell(getPlainCell(getText(item.getDescription())));
            table.addCell(getPlainCell("Equipment"));
        }

        final List<MagicObject> magicItems = new ArrayList<>(characterPlayer.getAllMagicItems());
        magicItems.sort(Comparator.comparing(item -> getText(item.getName())));
        for (final MagicObject item : magicItems) {
            table.addCell(getPlainCell(getText(item.getName())));
            table.addCell(getPlainCell(magicItemDetails(item)));
            table.addCell(getPlainCell("Magic item"));
        }
        return table;
    }

    private static String magicItemDetails(MagicObject item) {
        final List<String> details = new ArrayList<>();
        final String description = getText(item.getDescription());
        if (!description.isBlank()) {
            details.add(description);
        }
        for (final ObjectBonus bonus : item.getBonuses()) {
            if (bonus.getBonus() != null) {
                details.add((bonus.getBonus() > 0 ? "+" : "") + bonus.getBonus()
                        + " to " + objectBonusTarget(bonus));
            }
        }
        return String.join("; ", details);
    }

    private static String objectBonusTarget(ObjectBonus bonus) {
        if (bonus.getType() == BonusType.DEFENSIVE_BONUS) {
            return "Defensive bonus";
        }
        final String id = bonus.getBonusName();
        if (id == null || id.isBlank()) {
            return "";
        }
        try {
            return getText(bonus.getType() == BonusType.CATEGORY
                    ? RulesCatalog.getInstance().getCategory(id).getName()
                    : RulesCatalog.getInstance().getSkill(id).getName());
        } catch (InvalidXmlElementException e) {
            return id;
        }
    }
}
