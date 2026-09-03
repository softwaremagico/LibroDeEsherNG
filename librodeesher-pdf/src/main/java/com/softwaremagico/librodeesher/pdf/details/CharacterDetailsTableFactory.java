package com.softwaremagico.librodeesher.pdf.details;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.race.RaceSpecial;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.training.Training;

/** Detailed textual appendix for legacy perks, racial specials, and equipment notes. */
public final class CharacterDetailsTableFactory extends BaseElement {
    private CharacterDetailsTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getCharacterDetailsTable(CharacterPlayer characterPlayer)
            throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(1);
        setTableProperties(table);
        table.addCell(getTitleCell("Character Details", 1));

        addPerks(table, characterPlayer);
        addTrainings(table, characterPlayer);
        addRaceSpecials(table, characterPlayer);
        addEquipment(table, characterPlayer);
        return table;
    }

    private static void addTrainings(PdfPTable table, CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        for (final String trainingId : characterPlayer.getSelectedTrainingIds()) {
            final Training training = RulesCatalog.getInstance().getTraining(trainingId);
            addDetail(table, "Training", getText(training.getName()), "");
        }
    }

    private static void addPerks(PdfPTable table, CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        for (final SelectedPerk selected : characterPlayer.getSelectedPerks()) {
            addDetail(table, "Perk", getText(RulesCatalog.getInstance().getPerk(selected.getPerkId()).getName()),
                    getText(RulesCatalog.getInstance().getPerk(selected.getPerkId()).getDescription()));
            if (selected.getWeaknessId() != null) {
                addDetail(table, "Weakness", getText(RulesCatalog.getInstance().getPerk(selected.getWeaknessId()).getName()),
                        getText(RulesCatalog.getInstance().getPerk(selected.getWeaknessId()).getDescription()));
            }
        }
    }

    private static void addRaceSpecials(PdfPTable table, CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        if (characterPlayer.getRace() == null) {
            return;
        }
        for (final RaceSpecial special : characterPlayer.getRace().getSpecials()) {
            addDetail(table, "Racial special", getText(special.getText()), "");
        }
    }

    private static void addEquipment(PdfPTable table, CharacterPlayer characterPlayer) {
        for (final Equipment item : characterPlayer.getAllNotMagicEquipment()) {
            addDetail(table, "Equipment", getText(item.getName()), getText(item.getDescription()));
        }
        for (final MagicObject item : characterPlayer.getAllMagicItems()) {
            addDetail(table, "Magic item", getText(item.getName()), getText(item.getDescription()));
        }
    }

    private static void addDetail(PdfPTable table, String type, String name, String description) {
        table.addCell(getLabelCell(type));
        table.addCell(getPlainCell(description.isBlank() ? name : name + ": " + description));
    }
}
