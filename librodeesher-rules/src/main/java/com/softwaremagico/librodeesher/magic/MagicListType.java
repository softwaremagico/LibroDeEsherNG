package com.softwaremagico.librodeesher.magic;

/**
 * How a spell list relates to a spell-casting character, as printed as a row label in a profession's
 * "DESARROLLO DE HECHIZOS" section (e.g. {@code "Lista Básica (1-5)"}): this decides which of the
 * profession's development cost brackets applies to a rank bought in that list (see {@link
 * com.softwaremagico.librodeesher.profession.Profession#getMagicCost(MagicListType, int)}).
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code ProfessionMigrationTool} to parse the original Spanish row label (the level
 * range suffix, e.g. {@code "(1-5)"}, is stripped by the caller, see {@link MagicLevelRange}).</p>
 *
 * <p><strong>Currently classified by {@code CharacterPlayer}:</strong> every type except {@link
 * #TRIAD}/{@link #COMPLEMENTARY_TRIAD} (see {@code CharacterPlayer#classifySpellList}). Those two
 * require the legacy "elemental triad" reference table (which trainings share lists with which
 * others, e.g. "Mago del Fuego"/"Mago del Hielo"/"Mago del Agua" forming one triad and "Mago de la
 * Tierra"/"Mago del Aire"/"Mago de la Luz" the other) - a small, fully self-contained mechanic on its
 * own, but only relevant to the 3 "elementalist" trainings actually shipped (of the 6 the legacy
 * table defines), so implementing it is deferred; an elementalist training's own lists still resolve,
 * just as {@link #TRAINING} instead of the more specific {@link #TRIAD}.</p>
 */
public enum MagicListType {

    BASIC("Lista Básica"),
    OPEN("Lista Abierta"),
    CLOSED("Lista Cerrada"),
    OTHER_PROFESSION("Listas Básicas de Otras Profesiones"),
    OTHER_REALM_OPEN("Listas Abiertas de Otros Reinos"),
    OTHER_REALM_CLOSED("Listas Cerradas de Otros Reinos"),
    OTHER_REALM_OTHER_PROFESSION("Listas Básicas de Otros Reinos"),
    ARCHANUM("Listas Abiertas Arcanas"),
    TRIAD("Listas Básicas de la Tríada"),
    COMPLEMENTARY_TRIAD("Listas Básicas Elementales Complementarias"),
    TRAINING("Listas Hechizos de Adiestramiento"),
    OTHER_REALM_TRAINING("Listas Hechizos de Adiestramientos de Otros Reinos");

    private final String tag;

    MagicListType(String tag) {
        this.tag = tag;
    }

    public static MagicListType fromTag(String tag) {
        final String normalized = tag.trim();
        for (final MagicListType type : values()) {
            if (type.tag.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown magic list type tag '" + tag + "'.");
    }
}
