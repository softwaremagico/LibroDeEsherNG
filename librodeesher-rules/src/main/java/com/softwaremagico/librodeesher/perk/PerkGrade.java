package com.softwaremagico.librodeesher.perk;

/**
 * How impactful a perk (or weakness) is, as printed in the "Grado" column of {@code talentos.txt}.
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code PerkMigrationTool} to parse the original Spanish column value.</p>
 *
 * <p>The legacy {@code PerkGrade#getPerkCategory(String)} silently defaulted to {@code MAXIMUM} for
 * any unrecognized tag instead of failing. A handful of rows in the real "ManualPersonajes" data have
 * their "Grado" and "Tipo" columns swapped (e.g. a row with grade "Mental", which is actually a
 * {@link PerkType} tag), which under the legacy parser silently produced grade {@code MAXIMUM} for
 * those rows. {@link #fromTag(String)} reproduces that same fallback here, so this migration does not
 * change the effective (if buggy) behaviour of existing data; fixing the swapped columns in the
 * source data is left as a separate content-correction task.</p>
 */
public enum PerkGrade {

    MINIMUM("mínimo", 0),
    MINOR("menor", 1),
    MAJOR("mayor", 2),
    MAXIMUM("máximo", 3);

    private final String tag;
    private final int level;

    PerkGrade(String tag, int level) {
        this.tag = tag;
        this.level = level;
    }

    /** Ordinal weight, lowest to highest impact; used to compare grades (e.g. perk vs. its weakness). */
    public int getLevel() {
        return level;
    }

    public static PerkGrade fromTag(String tag) {
        for (final PerkGrade grade : values()) {
            if (grade.tag.equalsIgnoreCase(tag)) {
                return grade;
            }
        }
        return MAXIMUM;
    }
}
