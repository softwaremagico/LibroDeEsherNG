package com.softwaremagico.librodeesher.characteristic;

/**
 * The ten characteristics used by every character (plus the {@link #NONE} placeholder for "not
 * applicable" fields), each with its two-letter abbreviation as printed on the character sheet.
 *
 * <p>Serialized to XML/JSON using the plain (English) enum constant name; {@link #fromTag(String)}
 * is only used by migration tools that need to parse the original two-letter abbreviation column
 * (e.g. a category's "Ag/Fu" characteristics column).</p>
 */
public enum CharacteristicAbbreviation {

    NONE(""),
    AGILITY("Ag"),
    CONSTITUTION("Co"),
    MEMORY("Me"),
    REASONING("Ra"),
    SELF_DISCIPLINE("Ad"),
    EMPATHY("Em"),
    INTUITION("In"),
    PRESENCE("Pr"),
    QUICKNESS("Rp"),
    STRENGTH("Fu"),
    APPEARANCE("Ap");

    private final String tag;

    CharacteristicAbbreviation(String tag) {
        this.tag = tag;
    }

    /** The two-letter abbreviation printed on the character sheet (e.g. "Ag" for Agility). */
    public String getTag() {
        return tag;
    }

    /** Resolves the abbreviation used by the legacy {@code categorias.txt}/{@code razas/*.txt} files. */
    public static CharacteristicAbbreviation fromTag(String tag) {
        if (tag == null) {
            return NONE;
        }
        final String trimmed = tag.trim();
        for (final CharacteristicAbbreviation abbreviation : values()) {
            if (abbreviation.tag.equalsIgnoreCase(trimmed)) {
                return abbreviation;
            }
        }
        return NONE;
    }
}
