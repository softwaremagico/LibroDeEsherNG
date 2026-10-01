package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.legacy.LegacyCharacterSheet;
import com.softwaremagico.librodeesher.pdf.legacy.LegacySheetAssets;

/**
 * The character sheet: the hand-drawn Rolemaster layout of the legacy application.
 *
 * <p>This renders through {@link LegacyCharacterSheet}, a port of the legacy {@code PdfStandardSheet}
 * onto OpenPDF and the NG model. Each page carries the original full-page drawing as a background
 * (see {@link LegacySheetAssets}) and its values are drawn on top at fixed page coordinates, so the
 * generated sheet matches the printed one instead of being a generic flow of tables. A character-free
 * export still produces the blank version of the same sheet, as the legacy code did.</p>
 *
 * <p>The legacy sheet drew its own footer as a table row on every page, so this class does not install
 * {@link com.softwaremagico.librodeesher.pdf.events.FooterEvent}.</p>
 */
public class CharacterSheet extends PdfDocument {

    private final boolean alphabeticallySortedSkills;

    public CharacterSheet() {
        this(false);
    }

    /** @param alphabeticallySortedSkills whether to list skills globally by name instead of by category. */
    public CharacterSheet(boolean alphabeticallySortedSkills) {
        this.alphabeticallySortedSkills = alphabeticallySortedSkills;
    }

    @Override
    protected float getMargin() {
        return LegacyCharacterSheet.MARGIN;
    }

    @Override
    protected boolean useFooterEvent() {
        return false;
    }

    @Override
    protected Document addMetaData(Document document) {
        document.addTitle("Ficha Personaje Rolemaster");
        document.addAuthor("Software Magico");
        document.addCreator("Libro de Esher - Generador de PJs y PNJs para Rolemaster");
        document.addSubject("Pagina de PJ para Rolemaster");
        document.addKeywords("Rolemaster, PJ, PNJ, Libro de Esher");
        document.addCreationDate();
        return document;
    }

    @Override
    protected void createContent(Document document, CharacterPlayer characterPlayer)
            throws DocumentException, InvalidXmlElementException {
        // Unused: this sheet draws with the PdfWriter, see createContent(Document, CharacterPlayer, PdfWriter).
        throw new UnsupportedOperationException("Use the PdfWriter-aware overload");
    }

    @Override
    protected void createContent(Document document, CharacterPlayer characterPlayer, PdfWriter writer)
            throws InvalidXmlElementException {
        new LegacyCharacterSheet(characterPlayer, alphabeticallySortedSkills).render(document, writer);
    }
}
