package com.softwaremagico.librodeesher.pdf;

import com.lowagie.text.DocumentException;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.persistence.CharacterData;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.CharacterJsonManager;

import java.io.IOException;
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

    /** Writes the PDF sheet for {@code characterJSON} to {@code path} (a ".pdf" extension is appended if missing). */
    public static void exportToFileFromJSON(String characterJSON, PdfDocument sheet, Path path)
            throws DocumentException, InvalidXmlElementException, IOException {
        sheet.createFile(toCharacter(characterJSON), path);
    }

    private static CharacterPlayer toCharacter(String characterJSON) {
        final CharacterData data = CharacterJsonManager.fromJson(characterJSON);
        if (data == null) {
            throw new IllegalArgumentException("Cannot export an empty character: the JSON is null or blank.");
        }
        return CharacterDataMapper.toCharacter(data);
    }
}