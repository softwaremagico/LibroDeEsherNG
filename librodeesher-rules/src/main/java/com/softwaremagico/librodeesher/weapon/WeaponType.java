package com.softwaremagico.librodeesher.weapon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Broad weapon category, matching both the legacy {@code armas/<Tipo>.txt} file names and the
 * "Armas·&lt;Tipo&gt;" {@link com.softwaremagico.librodeesher.category.Category} ids (see
 * {@link #getCategoryId()}).
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

    @JsonValue
    public String getTag() {
        return tag;
    }

    /** Id of the skill category that groups every weapon of this type (see CategoryMigrationTool). */
    public String getCategoryId() {
        return "Armas·" + tag;
    }

    @JsonCreator
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
