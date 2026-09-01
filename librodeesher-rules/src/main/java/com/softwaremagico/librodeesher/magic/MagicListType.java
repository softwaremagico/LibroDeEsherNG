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
 * <p><strong>Classified by {@code CharacterPlayer#classifySpellList}:</strong> every type, including
 * {@link #TRIAD}/{@link #COMPLEMENTARY_TRIAD} (see {@link ElementalTriad}).</p>
 *
 * <p>The legacy application also modeled each of these 12 types as a synthetic {@code Category} (see
 * {@link #getCategoryId()}), purely so a {@code Perk}'s bonus could target "every spell list of this
 * classification" the same way it targets a real category (e.g. the shipped "Capacidad Mágica" perk's
 * +25 to every {@link #BASIC} list); {@code CharacterPlayer#getPerkCategoryBonus(String)} already
 * resolves these bonuses when given {@link #getCategoryId()}.</p>
 */
public enum MagicListType {

    BASIC("Lista Básica", "listsBasicOfSpells"),
    OPEN("Lista Abierta", "listsAbiertasOfSpells"),
    CLOSED("Lista Cerrada", "listsCerradasOfSpells"),
    OTHER_PROFESSION("Listas Básicas de Otras Profesiones", "listsBasicOfOtrasProfesiones"),
    OTHER_REALM_OPEN("Listas Abiertas de Otros Reinos", "listsAbiertasOfOtrosRealms"),
    OTHER_REALM_CLOSED("Listas Cerradas de Otros Reinos", "listsCerradasOfOtrosRealms"),
    OTHER_REALM_OTHER_PROFESSION("Listas Básicas de Otros Reinos", "listsBasicOfOtrosRealms"),
    ARCHANUM("Listas Abiertas Arcanas", "listsAbiertasArcanas"),
    TRIAD("Listas Básicas de la Tríada", "listsBasicOfTheTriad"),
    COMPLEMENTARY_TRIAD("Listas Básicas Elementales Complementarias", "listsBasicElementalesComplementarias"),
    TRAINING("Listas Hechizos de Adiestramiento", "listsSpellsOfTraining"),
    OTHER_REALM_TRAINING("Listas Hechizos de Adiestramientos de Otros Reinos", "listsSpellsOfTrainingsOfOtrosRealms");

    private final String tag;
    private final String categoryId;

    MagicListType(String tag, String categoryId) {
        this.tag = tag;
        this.categoryId = categoryId;
    }

    /** The synthetic category id a {@code Perk} bonus can target for this classification (see this enum's own javadoc). */
    public String getCategoryId() {
        return categoryId;
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
