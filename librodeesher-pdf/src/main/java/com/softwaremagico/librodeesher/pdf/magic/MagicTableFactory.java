package com.softwaremagico.librodeesher.pdf.magic;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.MagicSpellList;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds the spell-list section from every list currently available to the character. */
public final class MagicTableFactory extends BaseElement {
    private static final float[] WIDTHS = {3f, 1f};

    private MagicTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getMagicTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);
        table.addCell(getTitleCell("Spell Lists", WIDTHS.length));
        table.addCell(getLabelCell("Spell list"));
        table.addCell(getLabelCell("Realm"));

        final Set<MagicSpellList> availableLists = new LinkedHashSet<>();
        availableLists.addAll(characterPlayer.getOpenSpellLists());
        availableLists.addAll(characterPlayer.getClosedSpellLists());
        availableLists.addAll(characterPlayer.getBasicSpellLists());
        availableLists.addAll(characterPlayer.getTrainingSpellLists());
        availableLists.addAll(characterPlayer.getTriadSpellLists());
        availableLists.addAll(characterPlayer.getComplementaryTriadSpellLists());
        availableLists.addAll(characterPlayer.getOtherProfessionSpellLists());
        availableLists.addAll(characterPlayer.getArchanumSpellLists());
        availableLists.addAll(characterPlayer.getRaceSpellLists());

        final List<MagicSpellList> spellLists = new ArrayList<>(availableLists);
        spellLists.sort(Comparator.comparing(list -> getText(list.getName())));
        for (final MagicSpellList spellList : spellLists) {
            table.addCell(getPlainCell(getText(spellList.getName())));
            table.addCell(getPlainCell(spellList.getRealm().name()));
        }
        return table;
    }
}
