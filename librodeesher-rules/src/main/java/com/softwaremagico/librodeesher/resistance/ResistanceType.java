package com.softwaremagico.librodeesher.resistance;

/**
 * The nine resistance rolls (RR) a character can have.
 *
 * <p>Serialized to XML/JSON using the plain (English) enum constant name. The Spanish {@code tag}
 * (e.g. "Veneno") is only used by {@link #fromTag(String)}, which migration tools call to parse the
 * original column name (e.g. a race's "MODIFICACIÓN A LA TR" section); it is never used at runtime
 * otherwise. {@link #getCode()} returns our own English two-letter code instead (e.g. "PO" for
 * Poison), used wherever a resistance type is displayed.</p>
 */
public enum ResistanceType {

    CHANNELING("Canalización", "CH"),
    ESSENCE("Esencia", "ES"),
    MENTALISM("Mentalismo", "MN"),
    PSIONIC("Psiónico", "PS"),
    POISON("Veneno", "PO"),
    DISEASE("Enfermedad", "DI"),
    COLD("Frío", "CO"),
    HEAT("Calor", "HE"),
    FEAR("Miedo", "FE");

    private final String tag;
    private final String code;

    ResistanceType(String tag, String code) {
        this.tag = tag;
        this.code = code;
    }

    /** Our own English two-letter code (e.g. "PO" for Poison), used for display. */
    public String getCode() {
        return code;
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

    public static boolean isResistanceCode(String code) {
        for (final ResistanceType type : values()) {
            if (type.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
