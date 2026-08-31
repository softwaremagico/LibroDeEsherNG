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

    CANALIZATION("Canalización", CharacteristicAbbreviation.INTUITION, "ppChanneling"),
    ESSENCE("Esencia", CharacteristicAbbreviation.EMPATHY, "ppEssence"),
    MENTALISM("Mentalismo", CharacteristicAbbreviation.PRESENCE, "ppMentalism"),
    PSIONIC("Psiónico", CharacteristicAbbreviation.SELF_DISCIPLINE, "ppPsionic"),
    ARCHANUM("Arcano", CharacteristicAbbreviation.NONE, "ppArcane"),
    /** Racial spell lists, granted by a race rather than a profession. */
    RACE("Racial", CharacteristicAbbreviation.NONE, null);

    private final String tag;
    private final CharacteristicAbbreviation characteristic;
    private final String powerPointProgressionKey;

    RealmOfMagic(String tag, CharacteristicAbbreviation characteristic, String powerPointProgressionKey) {
        this.tag = tag;
        this.characteristic = characteristic;
        this.powerPointProgressionKey = powerPointProgressionKey;
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

    /**
     * This realm's own key into {@link com.softwaremagico.librodeesher.race.Race#getProgressionRankValues()}
     * for looking up how many power points a rank of this realm's Power Point Development skill is
     * worth (e.g. {@code "ppEssence"} for {@link #ESSENCE}), matching the legacy {@code
     * ProgressionCostType#getProgressionCostType(RealmOfMagic)} exactly; {@code null} for {@link
     * #RACE}, which has none (matching the legacy version's own {@code null} for that case).
     */
    public String getPowerPointProgressionKey() {
        return powerPointProgressionKey;
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
