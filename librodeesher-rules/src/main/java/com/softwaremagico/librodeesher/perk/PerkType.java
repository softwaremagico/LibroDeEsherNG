package com.softwaremagico.librodeesher.perk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Broad category of a perk (or weakness), as printed in the "Tipo" column of {@code talentos.txt}.
 * See {@link PerkGrade} for the fallback behaviour reproduced by {@link #fromTag(String)}.
 */
public enum PerkType {

    PHYSICAL("Físico"),
    MENTAL("Mental"),
    MAGICAL("Capacidad Mágica"),
    SPECIAL("Capacidad Especial"),
    TRAINING("Adiestramiento Especial"),
    /** Non-official/house-rule perks, excluded from random perk selection. */
    OTHER("Otros");

    private final String tag;

    PerkType(String tag) {
        this.tag = tag;
    }

    @JsonValue
    public String getTag() {
        return tag;
    }

    @JsonCreator
    public static PerkType fromTag(String tag) {
        for (final PerkType type : values()) {
            if (type.tag.equalsIgnoreCase(tag)) {
                return type;
            }
        }
        return OTHER;
    }
}
