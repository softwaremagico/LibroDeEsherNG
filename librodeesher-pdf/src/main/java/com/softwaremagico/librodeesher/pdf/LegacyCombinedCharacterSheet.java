package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.legacy.LegacyCharacterSheet;
import com.softwaremagico.librodeesher.pdf.legacy.LegacyCombinedSheet;

/**
 * The legacy two-columns "Hoja Combinada de Habilidades" export, as a printable sheet.
 *
 * <p>It shares its first pages with {@link LegacyCharacterSheet}, so only the combined pages are new
 * here; the layout itself lives in {@link LegacyCombinedSheet}. This is the printable form of the
 * old {@code PdfCombinedSheet} and nothing more: {@link CombinedTwoColumnsCharacterSheet} is the
 * modern two-columns sheet, whose rows repeat in every category and whose code is unrelated to this
 * one, so the two are deliberately not merged.</p>
 *
 * <p>The legacy sheet drew its own footer as a table row on every page, so this class does not install
 * {@link com.softwaremagico.librodeesher.pdf.events.FooterEvent}.</p>
 */
public class LegacyCombinedCharacterSheet extends PdfDocument {

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
        new LegacyCombinedSheet(characterPlayer).render(document, writer);
    }
}