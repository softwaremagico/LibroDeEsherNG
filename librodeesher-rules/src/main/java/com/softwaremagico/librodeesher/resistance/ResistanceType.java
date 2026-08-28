package com.softwaremagico.librodeesher.resistance;

/**
 * The nine resistance rolls (RR) a character can have, each with its two-letter abbreviation as
 * printed on the character sheet.
 *
 * <p>Serialized to XML/JSON using the plain (English) enum constant name; {@link #fromTag(String)}
 * is only used by migration tools that need to parse the original Spanish column name (e.g. a race's
 * "MODIFICACIÓN A LA TR" section).</p>
 */
public enum ResistanceType {

    CHANNELING("Canalización", "Cn"),
    ESSENCE("Esencia", "Es"),
    MENTALISM("Mentalismo", "Me"),
    PSIONIC("Psiónico", "Ps"),
    POISON("Veneno", "Vn"),
    DISEASE("Enfermedad", "Ef"),
    COLD("Frío", "Fr"),
    HEAT("Calor", "Ca"),
    FEAR("Miedo", "Mi");

    private final String tag;
    private final String abbreviation;

    ResistanceType(String tag, String abbreviation) {
        this.tag = tag;
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    /** Resolves the resistance type from the original Spanish column name (e.g. "Veneno"). */
    public static ResistanceType fromTag(String tag) {
        if (tag == null) {
            return null;
        }
        final String trimmed = tag.trim();
        for (final ResistanceType type : values()) {
            if (type.tag.equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isResistanceAbbreviation(String abbreviation) {
        for (final ResistanceType type : values()) {
            if (type.abbreviation.equals(abbreviation)) {
                return true;
            }
        }
        return false;
    }
}
