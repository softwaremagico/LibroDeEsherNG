package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;

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
 * <p>Unlike the legacy {@code PdfStandardSheet}/{@code PdfCombinedSheet1Column}/{@code
 * PdfCombinedSheet2Columns}, this does not overlay tables on top of a hand-drawn character sheet
 * background image (those PNG assets are not part of either repository, legacy or NG): every
 * section is a plain bordered/titled table instead, simpler to build and maintain, at the cost of
 * not visually matching the original printed sheet pixel-for-pixel.</p>
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

    protected Rectangle getPageSize() {
        return PageSize.A4;
    }

    private void generatePdf(Document document, CharacterPlayer characterPlayer)
            throws DocumentException, InvalidXmlElementException {
        this.addMetaData(document);
        document.open();
        try {
            this.createContent(document, characterPlayer);
        } finally {
            document.close();
        }
    }

    /** The character sheet as a byte array. Be careful with very large PDFs. */
    public final byte[] generate(CharacterPlayer characterPlayer) throws DocumentException, InvalidXmlElementException {
        final Document document = new Document(this.getPageSize(), MARGIN, MARGIN, MARGIN, MARGIN);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);
        this.generatePdf(document, characterPlayer);
        return outputStream.toByteArray();
    }

    /** Writes the character sheet to {@code path} (a ".pdf" extension is appended if missing). */
    public final void createFile(CharacterPlayer characterPlayer, Path path)
            throws DocumentException, InvalidXmlElementException, IOException {
        final Path target = path.toString().endsWith(".pdf") ? path : Path.of(path + ".pdf");
        final Document document = new Document(this.getPageSize(), MARGIN, MARGIN, MARGIN, MARGIN);
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            PdfWriter.getInstance(document, outputStream);
            this.generatePdf(document, characterPlayer);
        }
    }
}
