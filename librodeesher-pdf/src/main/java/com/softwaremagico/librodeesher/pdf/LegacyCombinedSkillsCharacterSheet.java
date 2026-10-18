package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.legacy.LegacyCombinedSkillsSheet;

/** The two-column {@code Hoja de Habilidades} layout from legacy 2.5.3 exports. */
public final class LegacyCombinedSkillsCharacterSheet extends LegacyCombinedCharacterSheet {

    @Override
    protected void createContent(Document document, CharacterPlayer characterPlayer, PdfWriter writer)
            throws InvalidXmlElementException {
        new LegacyCombinedSkillsSheet(characterPlayer).render(document, writer);
    }
}
