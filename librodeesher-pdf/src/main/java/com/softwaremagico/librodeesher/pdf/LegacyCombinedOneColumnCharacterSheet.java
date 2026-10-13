package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.legacy.LegacyCharacterSheet;
import com.softwaremagico.librodeesher.pdf.legacy.LegacyCombinedOneColumnSheet;

/**
 * The legacy one-column "Hoja Combinada de Habilidades" export, as a printable sheet.
 *
 * <p>The exact counterpart of {@link LegacyCombinedCharacterSheet} for the variant the legacy
 * application offered with a single skill column per page: it shares its first pages with
 * {@link LegacyCharacterSheet} and hands the combined pages over to
 * {@link LegacyCombinedOneColumnSheet}. Like the two-columns export it draws its own footer as a
 * table row, so no {@link com.softwaremagico.librodeesher.pdf.events.FooterEvent} is installed.</p>
 */
public class LegacyCombinedOneColumnCharacterSheet extends PdfDocument {

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
        new LegacyCombinedOneColumnSheet(characterPlayer).render(document, writer);
    }
}
