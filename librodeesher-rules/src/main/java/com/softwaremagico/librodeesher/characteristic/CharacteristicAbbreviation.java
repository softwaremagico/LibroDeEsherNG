package com.softwaremagico.librodeesher.characteristic;

/**
 * The ten characteristics used by every character (plus the {@link #NONE} placeholder for "not
 * applicable" fields).
 *
 * <p>Serialized to XML/JSON using the plain (English) enum constant name. The Spanish {@code tag}
 * (e.g. "Ag", "Fu") is only used by {@link #fromTag(String)}, which migration tools call to parse
 * the original two-letter abbreviation column (e.g. a category's "Ag/Fu" characteristics column);
 * it is never used at runtime otherwise. {@link #getCode()} returns the standard English two-letter
 * abbreviation instead (e.g. "AG", "ST"), used wherever a characteristic is displayed.</p>
 */
public enum CharacteristicAbbreviation {

    NONE("", ""),
    AGILITY("Ag", "AG"),
    CONSTITUTION("Co", "CO"),
    MEMORY("Me", "MM"),
    REASONING("Ra", "RE"),
    SELF_DISCIPLINE("Ad", "SD"),
    EMPATHY("Em", "EM"),
    INTUITION("In", "IN"),
    PRESENCE("Pr", "PR"),
    QUICKNESS("Rp", "QU"),
    STRENGTH("Fu", "ST"),
    APPEARANCE("Ap", "AP"),
    /**
     * Placeholder used by spell categories instead of a fixed characteristic: the actual
     * characteristic is whichever one governs the caster's current realm of magic (see
     * {@code RealmOfMagic}), resolved by {@code CharacterPlayer} rather than being a real,
     * fixed characteristic itself.
     */
    REALM_OF_MAGIC("*", "*");

    private final String tag;
    private final String code;

    CharacteristicAbbreviation(String tag, String code) {
        this.tag = tag;
        this.code = code;
    }

    /** The standard English two-letter abbreviation (e.g. "AG" for Agility), used for display. */
    public String getCode() {
        return code;
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
