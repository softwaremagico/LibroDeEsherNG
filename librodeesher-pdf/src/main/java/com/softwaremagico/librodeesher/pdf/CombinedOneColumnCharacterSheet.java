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
import com.softwaremagico.librodeesher.pdf.magic.MagicTableFactory;
import com.softwaremagico.librodeesher.pdf.perks.PerksTableFactory;
import com.softwaremagico.librodeesher.pdf.skills.SkillsTableFactory;

/** Modern one-column equivalent of the legacy {@code PdfCombinedSheet1Column}. */
public final class CombinedOneColumnCharacterSheet extends PdfDocument {

    @Override
    protected void createContent(Document document, CharacterPlayer characterPlayer)
            throws DocumentException, InvalidXmlElementException {
        document.add(CharacterBasicsTableFactory.getCharacterBasicsTable(characterPlayer));
        document.add(CharacteristicsTableFactory.getCharacteristicsTable(characterPlayer));
        document.add(DerivedStatsTableFactory.getDerivedStatsTable(characterPlayer));
        document.add(ResistancesTableFactory.getResistancesTable(characterPlayer));
        document.add(SkillsTableFactory.getSkillsTable(characterPlayer));
        document.add(EquipmentTableFactory.getEquipmentTable(characterPlayer));
        document.add(PerksTableFactory.getPerksTable(characterPlayer));
        document.add(MagicTableFactory.getMagicTable(characterPlayer));
        document.add(CharacterDetailsTableFactory.getCharacterDetailsTable(characterPlayer));
    }
}
