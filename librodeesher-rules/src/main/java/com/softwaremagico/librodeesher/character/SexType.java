package com.softwaremagico.librodeesher.character;

/**
 * A character's biological sex, affecting only flavour text (name lists, pronouns), not game rules.
 */
public enum SexType {
    MALE("Varón"),
    FEMALE("Mujer");

    private final String tag;

    SexType(String tag) {
        this.tag = tag;
    }

    /** The Spanish name this value is printed as, matching the legacy {@code SexType#getTag()}. */
    public String getTag() {
        return tag;
    }
}