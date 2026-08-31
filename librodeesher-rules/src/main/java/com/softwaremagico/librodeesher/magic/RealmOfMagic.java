package com.softwaremagico.librodeesher.magic;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;

/**
 * A realm of magic (e.g. "Esencia", "Mentalismo"), matching both the legacy
 * {@code hechizos/<Reino>.txt} file names and the tag used throughout the rest of the rulebook data.
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code MagicMigrationTool} to resolve the original Spanish file name.</p>
 */
public enum RealmOfMagic {

    CANALIZATION("Canalización", CharacteristicAbbreviation.INTUITION),
    ESSENCE("Esencia", CharacteristicAbbreviation.EMPATHY),
    MENTALISM("Mentalismo", CharacteristicAbbreviation.PRESENCE),
    PSIONIC("Psiónico", CharacteristicAbbreviation.SELF_DISCIPLINE),
    ARCHANUM("Arcano", CharacteristicAbbreviation.NONE),
    /** Racial spell lists, granted by a race rather than a profession. */
    RACE("Racial", CharacteristicAbbreviation.NONE);

    private final String tag;
    private final CharacteristicAbbreviation characteristic;

    RealmOfMagic(String tag, CharacteristicAbbreviation characteristic) {
        this.tag = tag;
        this.characteristic = characteristic;
    }

    /** Original Spanish tag, e.g. matching the legacy {@code hechizos/<Reino>.txt} file name. */
    public String getTag() {
        return tag;
    }

    /**
     * This realm's "prime" characteristic (e.g. {@link CharacteristicAbbreviation#EMPATHY} for
     * {@link #ESSENCE}), matching the legacy {@code RealmOfMagic#getCharacteristic()} exactly; {@link
     * CharacteristicAbbreviation#NONE} for {@link #ARCHANUM}/{@link #RACE}, which have none.
     */
    public CharacteristicAbbreviation getCharacteristic() {
        return characteristic;
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
