package com.softwaremagico.librodeesher.pdf;

/**
 * Modern equivalent of the legacy {@code PdfStandardSheet}. It renders every character section in
 * a vertical, automatically paginated layout.
 */
public final class StandardCharacterSheet extends CharacterSheet {
    public StandardCharacterSheet() {
        super();
    }

    public StandardCharacterSheet(boolean alphabeticallySortedSkills) {
        super(alphabeticallySortedSkills);
    }
}
