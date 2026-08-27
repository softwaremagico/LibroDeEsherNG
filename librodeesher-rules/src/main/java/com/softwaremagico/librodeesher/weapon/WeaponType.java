package com.softwaremagico.librodeesher.weapon;

import com.softwaremagico.librodeesher.language.Translations;

/**
 * Broad weapon category, matching both the legacy {@code armas/<Tipo>.txt} file names and the
 * "Armas·&lt;Tipo&gt;" {@link com.softwaremagico.librodeesher.category.Category} (see
 * {@link #getCategoryId()}).
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code WeaponMigrationTool} to resolve the original Spanish file name.</p>
 */
public enum WeaponType {

    EDGE("Filo"),
    BLUNT("Contundentes"),
    THROWING("Arrojadizas"),
    PROJECTILE("Proyectiles"),
    HANDLE("Asta"),
    TWO_HANDS("2manos"),
    SIEGE("Artillería"),
    FIREARM_ONE_HAND("Fuego 1mano"),
    FIREARM_TWO_HANDS("Fuego 2manos");

    private final String tag;

    WeaponType(String tag) {
        this.tag = tag;
    }

    /**
     * Id of the skill category that groups every weapon of this type (see CategoryMigrationTool),
     * computed the same way {@code CategoryMigrationTool} derives every other category id: an
     * English-derived abbreviation of the original Spanish "Armas·&lt;Tipo&gt;" category name.
     */
    public String getCategoryId() {
        return Translations.toEnglishId("Armas·" + tag);
    }

    public static WeaponType fromTag(String tag) {
        final String normalized = tag.trim();
        for (final WeaponType type : values()) {
            if (type.tag.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown weapon type tag '" + tag + "'.");
    }
}
