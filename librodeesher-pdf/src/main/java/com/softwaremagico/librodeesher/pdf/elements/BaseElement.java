package com.softwaremagico.librodeesher.pdf.elements;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.language.TranslatedText;

/**
 * Small table/cell building helpers shared by every table factory. Gray fills avoid OpenPDF's
 * background colour API because its signature introduces an AWT type into the compiled bytecode.
 */
public class BaseElement {

    public static final float TITLE_FONT_SIZE = 12f;
    public static final float LABEL_FONT_SIZE = 7f;
    public static final float VALUE_FONT_SIZE = 9f;

    protected BaseElement() {
        // Only static helpers.
    }

    public static void setTableProperties(PdfPTable table) {
        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
        table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(4);
    }

    public static PdfPCell getTitleCell(String text, int columnSpan) {
        final Phrase phrase = new Phrase(text == null ? "" : text.toUpperCase(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, TITLE_FONT_SIZE));
        final PdfPCell cell = new PdfPCell(phrase);
        cell.setColspan(columnSpan);
        cell.setBorder(Element.ALIGN_TOP + Element.ALIGN_BOTTOM);
        cell.setGrayFill(220f / 255f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        return cell;
    }

    /** A small, all-caps label cell (e.g. a characteristic's/skill's name), not the value itself. */
    public static PdfPCell getLabelCell(String text) {
        final Phrase phrase = new Phrase(text == null ? "" : text,
                FontFactory.getFont(FontFactory.HELVETICA, LABEL_FONT_SIZE));
        final PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(2);
        return cell;
    }

    /** A value cell (e.g. a characteristic's bonus, a skill's rank), bold and right-aligned. */
    public static PdfPCell getValueCell(String text) {
        final Phrase phrase = new Phrase(text == null ? "" : text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, VALUE_FONT_SIZE));
        final PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(2);
        return cell;
    }

    public static PdfPCell getPlainCell(String text) {
        final Phrase phrase = new Phrase(text == null ? "" : text,
                FontFactory.getFont(FontFactory.HELVETICA, VALUE_FONT_SIZE));
        final PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(2);
        cell.setBorder(0);
        return cell;
    }

    public static Font labelFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, LABEL_FONT_SIZE);
    }

    public static String getText(TranslatedText text) {
        if (text == null) {
            return "";
        }
        return text.getTranslatedText() == null ? "" : text.getTranslatedText();
    }
}
