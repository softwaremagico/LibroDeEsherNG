package com.softwaremagico.librodeesher.magic;

/**
 * A realm of magic (e.g. "Esencia", "Mentalismo"), matching both the legacy
 * {@code hechizos/<Reino>.txt} file names and the tag used throughout the rest of the rulebook data.
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code MagicMigrationTool} to resolve the original Spanish file name.</p>
 */
public enum RealmOfMagic {

    CANALIZATION("Canalización"),
    ESSENCE("Esencia"),
    MENTALISM("Mentalismo"),
    PSIONIC("Psiónico"),
    ARCHANUM("Arcano"),
    /** Racial spell lists, granted by a race rather than a profession. */
    RACE("Racial");

    private final String tag;

    RealmOfMagic(String tag) {
        this.tag = tag;
    }

    /** Original Spanish tag, e.g. matching the legacy {@code hechizos/<Reino>.txt} file name. */
    public String getTag() {
        return tag;
    }

    public static RealmOfMagic fromTag(String tag) {
        for (final RealmOfMagic realm : values()) {
            if (realm.tag.equalsIgnoreCase(tag)) {
                return realm;
            }
        }
        throw new IllegalArgumentException("Unknown magic realm tag '" + tag + "'.");
    }
}
