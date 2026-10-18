package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.DocumentException;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.persistence.CharacterData;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.CharacterJsonManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Exports a character saved in the JSON persistence format (see {@code
 * com.softwaremagico.librodeesher.persistence}) to a PDF sheet, without re-running character
 * creation: the snapshot is restored through {@link CharacterDataMapper}, so the huge decision
 * history, level bookkeeping and already-approved grants are never replayed or double-counted.
 *
 * <p>A null or blank JSON string is rejected instead of silently producing an empty document, since
 * "no character to export" is a caller contract error.</p>
 */
public final class CharacterPdfExporter {

    private CharacterPdfExporter() {
        // Utility class.
    }

    /** The PDF bytes of {@code sheet} rendered for the character stored in {@code characterJSON}. */
    public static byte[] exportFromJSON(String characterJSON, PdfDocument sheet)
            throws DocumentException, InvalidXmlElementException {
        return sheet.generate(toCharacter(characterJSON));
    }

    /** Writes the PDF directly to a caller-owned stream, suitable for Android storage providers. */
    public static void exportToStreamFromJSON(String characterJSON, PdfDocument sheet, OutputStream outputStream)
            throws DocumentException, InvalidXmlElementException {
        sheet.generateTo(outputStream, toCharacter(characterJSON));
    }

    /** Writes the PDF sheet for {@code characterJSON} to {@code path} (a ".pdf" extension is appended if missing). */
    public static void exportToFileFromJSON(String characterJSON, PdfDocument sheet, Path path)
            throws DocumentException, InvalidXmlElementException, IOException {
        sheet.createFile(toCharacter(characterJSON), path);
    }

    /** Writes the PDF sheet for {@code characterJSON} to a path, without requiring {@link Path} on Android. */
    public static void exportToFileFromJSON(String characterJSON, PdfDocument sheet, String path)
            throws DocumentException, InvalidXmlElementException, IOException {
        sheet.createFile(path, toCharacter(characterJSON));
    }

    private static CharacterPlayer toCharacter(String characterJSON) throws InvalidXmlElementException {
        final CharacterData data = CharacterJsonManager.fromJson(characterJSON);
        if (data == null) {
            throw new IllegalArgumentException("Cannot export an empty character: the JSON is null or blank.");
        }
        return CharacterDataMapper.toCharacter(data);
    }
}