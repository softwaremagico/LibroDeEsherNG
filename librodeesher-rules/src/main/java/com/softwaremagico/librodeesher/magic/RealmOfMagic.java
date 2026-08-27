package com.softwaremagico.librodeesher.magic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A realm of magic (e.g. "Esencia", "Mentalismo"), matching both the legacy
 * {@code hechizos/<Reino>.txt} file names and the tag used throughout the rest of the rulebook data.
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

    @JsonValue
    public String getTag() {
        return tag;
    }

    @JsonCreator
    public static RealmOfMagic fromTag(String tag) {
        for (final RealmOfMagic realm : values()) {
            if (realm.tag.equalsIgnoreCase(tag)) {
                return realm;
            }
        }
        throw new IllegalArgumentException("Unknown magic realm tag '" + tag + "'.");
    }
}
