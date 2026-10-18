package com.softwaremagico.librodeesher.pdf.legacy;

import com.lowagie.text.pdf.PdfPCell;
import com.softwaremagico.librodeesher.character.CharacterPlayer;

/** The two-column skills sheet format used by legacy 2.5.3 character exports. */
public final class LegacyCombinedSkillsSheet extends LegacyCombinedSheet {

    public LegacyCombinedSkillsSheet(CharacterPlayer characterPlayer) {
        super(characterPlayer);
    }

    @Override
    protected String sheetTitle() {
        return "Hoja de Habilidades";
    }

    @Override
    protected String footerVersion() {
        return "2.5.3";
    }

    @Override
    protected void applyTitleBackground(PdfPCell cell) {
        cell.setGrayFill(0f);
    }
}
