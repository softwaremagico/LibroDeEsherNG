package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.characteristics.CharacteristicsTableFactory;
import com.softwaremagico.librodeesher.pdf.details.CharacterDetailsTableFactory;
import com.softwaremagico.librodeesher.pdf.equipment.EquipmentTableFactory;
import com.softwaremagico.librodeesher.pdf.info.CharacterBasicsTableFactory;
import com.softwaremagico.librodeesher.pdf.info.DerivedStatsTableFactory;
import com.softwaremagico.librodeesher.pdf.info.ResistancesTableFactory;
import com.softwaremagico.librodeesher.pdf.info.RaceDetailsTableFactory;
import com.softwaremagico.librodeesher.pdf.magic.MagicTableFactory;
import com.softwaremagico.librodeesher.pdf.perks.PerksTableFactory;
import com.softwaremagico.librodeesher.pdf.skills.SkillsTableFactory;

/**
 * The character sheet: name/race/culture/profession/level, the ten characteristics, the derived
 * stats (movement/defensive bonus/armour class/power points/points left) and the nine resistance
 * rolls, one {@link com.lowagie.text.pdf.PdfPTable} per section (see {@link PdfDocument}'s own
 * javadoc for why this does not attempt to replicate the legacy hand-drawn sheet layout).
 *
 * <p>The sections intentionally use flow-layout tables instead of the old, image-overlay pages:
 * this preserves the data and lets OpenPDF paginate longer skill and spell-list collections.
 * Each section is isolated in a factory, following ThinkMachine4E's modern PDF structure.</p>
 */
public class CharacterSheet extends PdfDocument {

    @Override
    protected void createContent(Document document, CharacterPlayer characterPlayer)
            throws DocumentException, InvalidXmlElementException {
        document.add(CharacterBasicsTableFactory.getCharacterBasicsTable(characterPlayer));
        document.add(CharacteristicsTableFactory.getCharacteristicsTable(characterPlayer));
        document.add(DerivedStatsTableFactory.getDerivedStatsTable(characterPlayer));
        document.add(ResistancesTableFactory.getResistancesTable(characterPlayer));
        document.add(RaceDetailsTableFactory.getRaceDetailsTable(characterPlayer));
        document.add(SkillsTableFactory.getSkillsTable(characterPlayer));
        document.add(EquipmentTableFactory.getEquipmentTable(characterPlayer));
        document.add(PerksTableFactory.getPerksTable(characterPlayer));
        document.add(MagicTableFactory.getMagicTable(characterPlayer));
        document.add(CharacterDetailsTableFactory.getCharacterDetailsTable(characterPlayer));
    }
}
