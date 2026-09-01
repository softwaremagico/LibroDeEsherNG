package com.softwaremagico.librodeesher.race;

/**
 * A race's size category, as printed in a rulebook's "TAMAÑO DE RAZA" column, matching the legacy
 * {@code RaceSize} exactly (including its lenient fallback behaviour: an unrecognized/blank tag - a
 * couple of real race files use a bare {@code "M"} shorthand instead of a full word, and one is
 * simply blank - defaults to {@link #M}, never fails migration).
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code RaceMigrationTool} to parse the original Spanish column value.</p>
 */
public enum RaceSize {

    XXS("muy pequeño"),
    XS("pequeño"),
    S("bajo"),
    M("mediano"),
    L("alto"),
    XL("grande"),
    XXL("super grande");

    private final String tag;

    RaceSize(String tag) {
        this.tag = tag;
    }

    /**
     * Resolves a race's size tag, matching the legacy {@code RaceSize#getRaceSize(String)} exactly:
     * besides each constant's own tag, {@code "normal"}/{@code "medio"} resolve to {@link #M} and
     * {@code "enorme"}/{@code "muy grande"} to {@link #XL}; anything else (including a bare {@code
     * "M"} shorthand, or a blank value) defaults to {@link #M}.
     */
    public static RaceSize fromTag(String tag) {
        if (tag == null) {
            return M;
        }
        final String normalized = tag.trim().toLowerCase();
        for (final RaceSize size : values()) {
            if (size.tag.equals(normalized)) {
                return size;
            }
        }
        if (normalized.equals("normal") || normalized.equals("medio")) {
            return M;
        }
        if (normalized.equals("enorme") || normalized.equals("muy grande")) {
            return XL;
        }
        return M;
    }
}
