package com.softwaremagico.librodeesher.perk;

/**
 * Broad category of a perk (or weakness), as printed in the "Tipo" column of {@code talentos.txt}.
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code PerkMigrationTool} to parse the original Spanish column value. See
 * {@link PerkGrade} for the fallback behaviour reproduced by {@link #fromTag(String)}.</p>
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

    public static PerkType fromTag(String tag) {
        for (final PerkType type : values()) {
            if (type.tag.equalsIgnoreCase(tag)) {
                return type;
            }
        }
        return OTHER;
    }
}
