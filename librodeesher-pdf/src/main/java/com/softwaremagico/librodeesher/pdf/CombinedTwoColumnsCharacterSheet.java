package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.characteristics.CharacteristicsTableFactory;
import com.softwaremagico.librodeesher.pdf.details.CharacterDetailsTableFactory;
import com.softwaremagico.librodeesher.pdf.details.HistoryTableFactory;
import com.softwaremagico.librodeesher.pdf.equipment.EquipmentTableFactory;
import com.softwaremagico.librodeesher.pdf.info.CharacterBasicsTableFactory;
import com.softwaremagico.librodeesher.pdf.info.DerivedStatsTableFactory;
import com.softwaremagico.librodeesher.pdf.info.ResistancesTableFactory;
import com.softwaremagico.librodeesher.pdf.info.RaceDetailsTableFactory;
import com.softwaremagico.librodeesher.pdf.magic.MagicTableFactory;
import com.softwaremagico.librodeesher.pdf.perks.PerksTableFactory;
import com.softwaremagico.librodeesher.pdf.skills.SkillsTableFactory;
import com.softwaremagico.librodeesher.pdf.skills.FavouriteSkillsTableFactory;

/** Modern two-column equivalent of the legacy {@code PdfCombinedSheet2Columns}. */
public class CombinedTwoColumnsCharacterSheet extends PdfDocument {

    @Override
    protected void createContent(Document document, CharacterPlayer characterPlayer)
            throws DocumentException, InvalidXmlElementException {
        document.add(CharacterBasicsTableFactory.getCharacterBasicsTable(characterPlayer));

        final PdfPTable columns = new PdfPTable(new float[]{1f, 1f});
        columns.setWidthPercentage(100);
        columns.addCell(contentCell(SkillsTableFactory.getSkillsTable(characterPlayer)));
        columns.addCell(contentCell(createCharacterDetails(characterPlayer)));
        document.add(columns);
    }

    private PdfPTable createCharacterDetails(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable details = new PdfPTable(1);
        details.setWidthPercentage(100);
        details.addCell(contentCell(CharacteristicsTableFactory.getCharacteristicsTable(characterPlayer)));
        details.addCell(contentCell(FavouriteSkillsTableFactory.getFavouriteSkillsTable(characterPlayer)));
        details.addCell(contentCell(DerivedStatsTableFactory.getDerivedStatsTable(characterPlayer)));
        details.addCell(contentCell(ResistancesTableFactory.getResistancesTable(characterPlayer)));
        details.addCell(contentCell(RaceDetailsTableFactory.getRaceDetailsTable(characterPlayer)));
        details.addCell(contentCell(EquipmentTableFactory.getEquipmentTable(characterPlayer)));
        details.addCell(contentCell(PerksTableFactory.getPerksTable(characterPlayer)));
        details.addCell(contentCell(MagicTableFactory.getMagicTable(characterPlayer)));
        details.addCell(contentCell(CharacterDetailsTableFactory.getCharacterDetailsTable(characterPlayer)));
        details.addCell(contentCell(HistoryTableFactory.getHistoryTable(characterPlayer)));
        return details;
    }

    private PdfPCell contentCell(PdfPTable content) {
        final PdfPCell cell = new PdfPCell(content);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }
}
