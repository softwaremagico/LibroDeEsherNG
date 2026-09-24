package com.softwaremagico.librodeesher.pdf.events;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/** Shared footer with generator attribution and page number. */
public final class FooterEvent extends PdfPageEventHelper {
    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        final String text = "Libro de Esher - Rolemaster character sheet - " + writer.getPageNumber();
        ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 7)),
                document.getPageSize().getWidth() / 2, 18, 0);
    }
}
