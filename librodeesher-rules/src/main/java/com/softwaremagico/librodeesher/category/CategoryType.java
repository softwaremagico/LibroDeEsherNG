package com.softwaremagico.librodeesher.category;

/**
 * Progression rule used to compute how much a category costs to develop, as printed in the
 * "Progresión" column of {@code categorias.txt}, and the resulting skill bonus a character gets per
 * rank bought in a category of this type ({@link #getSkillRankBonus(int)}). Both are a fixed game
 * rule (not data), so they are not read from XML.
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code CategoryMigrationTool} to parse the original Spanish column value.</p>
 */
public enum CategoryType {

    /** The common case: skills develop individually, each rank adding to that skill's own bonus. */
    STANDARD("Estándar", true) {
        @Override
        public Integer getSkillRankBonus(int ranks) {
            if (ranks <= 0) {
                return -15;
            }
            if (ranks <= 10) {
                return ranks * 3;
            }
            if (ranks <= 20) {
                return 30 + (ranks - 10) * 2;
            }
            if (ranks <= 30) {
                return 50 + (ranks - 20);
            }
            return 60 + (ranks - 30) / 2;
        }

        @Override
        public Integer getCategoryRankBonus(int ranks) {
            if (ranks <= 0) {
                return -15;
            }
            if (ranks <= 10) {
                return ranks * 2;
            }
            if (ranks <= 20) {
                return 20 + (ranks - 10);
            }
            if (ranks <= 30) {
                return 30 + (ranks - 20) / 2;
            }
            return 35;
        }
    },
    /** Two or more categories combined into a single development track (e.g. two-weapon styles). */
    COMBINED("Combinada", false) {
        @Override
        public Integer getSkillRankBonus(int ranks) {
            if (ranks <= 0) {
                return -30;
            }
            if (ranks <= 10) {
                return ranks * 5;
            }
            if (ranks <= 20) {
                return 50 + (ranks - 10) * 3;
            }
            if (ranks <= 25) {
                return 80 + (ranks - 20) * 2;
            }
            if (ranks <= 30) {
                return 80 + (int) ((ranks - 20) * 1.5);
            }
            return 95 + (ranks - 30) / 2;
        }
    },
    /** Capped, slower-growing progression (e.g. body development-adjacent categories). */
    LIMITED("Limitada", false) {
        @Override
        public Integer getSkillRankBonus(int ranks) {
            if (ranks <= 0) {
                return 0;
            }
            if (ranks <= 20) {
                return ranks;
            }
            if (ranks < 30) {
                return 20 + (ranks - 20) / 2;
            }
            return 25;
        }
    },
    /** Faster-growing, uncapped progression (e.g. some martial styles/maneuvers). */
    SPECIAL("Especial", false) {
        @Override
        public Integer getSkillRankBonus(int ranks) {
            if (ranks <= 0) {
                return 0;
            }
            if (ranks <= 10) {
                return ranks * 6;
            }
            if (ranks <= 20) {
                return 60 + (ranks - 10) * 5;
            }
            if (ranks <= 30) {
                return 110 + (ranks - 20) * 4;
            }
            return 150 + (ranks - 30) * 3;
        }
    },
    /** "Desarrollo de Puntos de Poder" (power point development): ranks grant power points, no bonus. */
    PPD("DPP", false),
    /**
     * "Desarrollo Físico" (physical development): ranks grant hit points, no rank-scaling bonus; see
     * {@link #getFixedBonus()} for its flat +10 bonus instead.
     */
    PD("DF", false) {
        @Override
        public Integer getFixedBonus() {
            return 10;
        }
    };

    private final String tag;
    private final boolean hasRanks;

    CategoryType(String tag, boolean hasRanks) {
        this.tag = tag;
        this.hasRanks = hasRanks;
    }

    /**
     * Whether categories of this type develop through individually-tracked skill ranks ({@code
     * true}, the {@link #STANDARD} case) as opposed to a single category-wide development track.
     */
    public boolean hasRanks() {
        return hasRanks;
    }

    /** The skill bonus granted by having {@code ranks} ranks in a category of this type. */
    public Integer getSkillRankBonus(int ranks) {
        return 0;
    }

    /**
     * The category's own bonus granted by having {@code ranks} ranks directly in the category itself
     * (as opposed to in one of its skills); 0 for every type except {@link #STANDARD}.
     */
    public Integer getCategoryRankBonus(int ranks) {
        return 0;
    }

    /** A flat bonus granted regardless of ranks (only {@link #PD} has one; 0 otherwise). */
    public Integer getFixedBonus() {
        return 0;
    }

    public static CategoryType fromTag(String tag) {
        for (final CategoryType type : values()) {
            if (type.tag.equalsIgnoreCase(tag)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown category progression tag '" + tag + "'.");
    }
}
