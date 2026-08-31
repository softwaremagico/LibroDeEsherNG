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
 * <p><strong>Currently classified by {@code CharacterPlayer}:</strong> {@link #BASIC} (a list owned
 * by the character's own profession), {@link #OPEN} and {@link #CLOSED} (any list of the character's
 * own realm(s) marked as such). The other 8 types (lists of another profession/realm, the
 * "tríada"/elementalist-training/dark-spell special cases) are not resolved yet: doing so requires
 * porting the legacy {@code MagicFactory}'s remaining classification helpers, which is significant
 * scope on its own (see {@code MagicSpellList}'s javadoc); {@code Profession#getMagicCost} still
 * exposes their cost table verbatim, in case a caller resolves the classification itself.</p>
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
