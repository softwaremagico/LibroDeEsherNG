package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.characteristics.CharacteristicsTableFactory;
import com.softwaremagico.librodeesher.pdf.info.CharacterBasicsTableFactory;
import com.softwaremagico.librodeesher.pdf.info.DerivedStatsTableFactory;
import com.softwaremagico.librodeesher.pdf.info.ResistancesTableFactory;

/**
 * The character sheet: name/race/culture/profession/level, the ten characteristics, the derived
 * stats (movement/defensive bonus/armour class/power points/points left) and the nine resistance
 * rolls, one {@link com.lowagie.text.pdf.PdfPTable} per section (see {@link PdfDocument}'s own
 * javadoc for why this does not attempt to replicate the legacy hand-drawn sheet layout).
 *
 * <p>Equipment, skills/categories with ranks, spells and perks are not rendered yet: this is a
 * first, real, working slice (the header/characteristics/derived-stats/resistances a player needs
 * at a glance), not the full legacy sheet (which also lists every skill, every spell list known,
 * every perk, and every piece of equipment) - future work, one section/table factory at a time,
 * following the same pattern.</p>
 */
public class CharacterSheet extends PdfDocument {

    @Override
    protected void createContent(Document document, CharacterPlayer characterPlayer)
            throws DocumentException, InvalidXmlElementException {
        document.add(CharacterBasicsTableFactory.getCharacterBasicsTable(characterPlayer));
        document.add(CharacteristicsTableFactory.getCharacteristicsTable(characterPlayer));
        document.add(DerivedStatsTableFactory.getDerivedStatsTable(characterPlayer));
        document.add(ResistancesTableFactory.getResistancesTable(characterPlayer));
    }
}
