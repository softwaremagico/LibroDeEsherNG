package com.softwaremagico.librodeesher.category;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Progression rule used to compute how much a category costs to develop, as printed in the
 * "Progresión" column of {@code categorias.txt}. The actual numeric progression values are a fixed
 * game rule (not data), so they are not read from XML; they will be attached to this enum once the
 * character point-development logic is ported.
 */
public enum CategoryType {

    STANDARD("Estándar"),
    COMBINED("Combinada"),
    LIMITED("Limitada"),
    SPECIAL("Especial"),
    /** "Desarrollo de Puntos de Poder" (power point development). */
    PPD("DPP"),
    /** "Desarrollo Físico" (physical development). */
    PD("DF");

    private final String tag;

    CategoryType(String tag) {
        this.tag = tag;
    }

    /** Original Spanish tag as printed in the legacy text files, kept for round-tripping the XML. */
    @JsonValue
    public String getTag() {
        return tag;
    }

    @JsonCreator
    public static CategoryType fromTag(String tag) {
        for (final CategoryType type : values()) {
            if (type.tag.equalsIgnoreCase(tag)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown category progression tag '" + tag + "'.");
    }
}
