package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.events.FooterEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for every PDF sheet this module can render, built with OpenPDF (the {@code
 * com.lowagie.text} package), matching the shape of {@code think-machine-pdf}'s own {@code
 * PdfDocument}/{@code CharacterSheet} pair (a sibling Rolemaster-unrelated project by the same
 * author, itself already migrated off iText onto OpenPDF): a document is built once (see {@link
 * #generate()}/{@link #createFile(Path)}), delegating the actual content to {@link
 * #createContent(Document, CharacterPlayer)}, which concrete subclasses (currently just {@link
 * com.softwaremagico.librodeesher.pdf.CharacterSheet}) implement by adding one {@link
 * com.lowagie.text.pdf.PdfPTable} per section (see {@code
 * com.softwaremagico.librodeesher.pdf.elements.BaseElement}).
 *
* <p>Subclasses can still use the plain flow layout (one {@link
     * com.lowagie.text.pdf.PdfPTable} per section) or, like {@link
     * com.softwaremagico.librodeesher.pdf.legacy.LegacyCharacterSheet}, the legacy layout that overlays
     * its tables on top of the hand-drawn background images now bundled as resources (see {@link
     * com.softwaremagico.librodeesher.pdf.legacy.LegacySheetAssets}).</p>
 */
public abstract class PdfDocument {

    private static final int MARGIN = 30;

    protected Document addMetaData(Document document) {
        document.addTitle("Rolemaster Character Sheet");
        document.addAuthor("Software Magico");
        document.addCreator("Libro de Esher");
        document.addSubject("RPG");
        document.addCreationDate();
        return document;
    }

    protected abstract void createContent(Document document, CharacterPlayer characterPlayer)
            throws DocumentException, InvalidXmlElementException;

    /**
     * Renders the content with access to the {@link PdfWriter}. The default implementation ignores it
     * and delegates to {@link #createContent(Document, CharacterPlayer)}; only the legacy sheet, which
     * draws its tables with {@link com.lowagie.text.pdf.PdfPTable#writeSelectedRows}, overrides this.
     */
    protected void createContent(Document document, CharacterPlayer characterPlayer, PdfWriter writer)
            throws DocumentException, InvalidXmlElementException {
        createContent(document, characterPlayer);
    }

    protected Rectangle getPageSize() {
        return PageSize.A4;
    }

    /** The page margin used on all four sides. */
    protected float getMargin() {
        return MARGIN;
    }

    /** Whether {@link FooterEvent} stamps the page number; the legacy sheet draws its own footer instead. */
    protected boolean useFooterEvent() {
        return true;
    }

    private void generatePdf(Document document, PdfWriter writer, CharacterPlayer characterPlayer)
            throws DocumentException, InvalidXmlElementException {
        this.addMetaData(document);
        document.open();
        try {
            this.createContent(document, characterPlayer, writer);
        } finally {
            document.close();
        }
    }

    /** The character sheet as a byte array. Be careful with very large PDFs. */
    public final byte[] generate(CharacterPlayer characterPlayer) throws DocumentException, InvalidXmlElementException {
        final float margin = this.getMargin();
        final Document document = new Document(this.getPageSize(), margin, margin, margin, margin);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        if (this.useFooterEvent()) {
            writer.setPageEvent(new FooterEvent());
        }
        this.generatePdf(document, writer, characterPlayer);
        return outputStream.toByteArray();
    }

    /** Writes the character sheet to {@code path} (a ".pdf" extension is appended if missing). */
    public final void createFile(CharacterPlayer characterPlayer, Path path)
            throws DocumentException, InvalidXmlElementException, IOException {
        final Path target = path.toString().endsWith(".pdf") ? path : Path.of(path + ".pdf");
        final float margin = this.getMargin();
        final Document document = new Document(this.getPageSize(), margin, margin, margin, margin);
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            final PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            if (this.useFooterEvent()) {
                writer.setPageEvent(new FooterEvent());
            }
            this.generatePdf(document, writer, characterPlayer);
        }
    }
}
