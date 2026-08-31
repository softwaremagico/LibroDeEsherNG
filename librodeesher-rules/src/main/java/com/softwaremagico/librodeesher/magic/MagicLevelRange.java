package com.softwaremagico.librodeesher.magic;

/**
 * One five-level bracket of a profession's "DESARROLLO DE HECHIZOS" cost table (e.g. every rank
 * bought in a {@link MagicListType#BASIC} list while it has between 6 and 10 ranks costs whatever
 * {@code "Lista Básica (6-10)"} says), matching the legacy {@code MagicLevelRange} exactly (despite
 * its name, keyed by the spell list's own current ranks, not the character's experience level -
 * matching the legacy {@code MagicLevelRange#getLevelRange(Integer currentRanks)}).
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code ProfessionMigrationTool} to parse the original Spanish row label's level range
 * suffix (e.g. {@code "1-5"}).</p>
 */
public enum MagicLevelRange {

    FIRST_FIVE_LEVELS("1-5"),
    SECOND_FIVE_LEVELS("6-10"),
    THIRD_FIVE_LEVELS("11-15"),
    FOURTH_FIVE_LEVELS("16-20"),
    MORE_LEVELS("21+");

    private final String tag;

    MagicLevelRange(String tag) {
        this.tag = tag;
    }

    public static MagicLevelRange fromTag(String tag) {
        final String normalized = tag.trim();
        for (final MagicLevelRange range : values()) {
            if (range.tag.equalsIgnoreCase(normalized)) {
                return range;
            }
        }
        throw new IllegalArgumentException("Unknown magic level range tag '" + tag + "'.");
    }

    /**
     * The bracket a spell list with {@code ranks} ranks bought so far falls into (so its
     * <em>next</em> rank's cost can be looked up), matching the legacy {@code
     * MagicLevelRange#getLevelRange(Integer)} exactly.
     */
    public static MagicLevelRange forRanks(int ranks) {
        if (ranks < 5) {
            return FIRST_FIVE_LEVELS;
        }
        if (ranks < 10) {
            return SECOND_FIVE_LEVELS;
        }
        if (ranks < 15) {
            return THIRD_FIVE_LEVELS;
        }
        if (ranks < 20) {
            return FOURTH_FIVE_LEVELS;
        }
        return MORE_LEVELS;
    }
}
