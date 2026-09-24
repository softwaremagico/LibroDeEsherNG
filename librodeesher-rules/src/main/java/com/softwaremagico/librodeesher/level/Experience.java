package com.softwaremagico.librodeesher.level;

/**
 * The Rolemaster experience table: the minimum accumulated experience points required to reach a
 * given level. Each consecutive five-level band raises the per-level cost, mirroring the legacy
 * {@code Experience} class.
 */
public final class Experience {

    private static final int COST_PER_LEVEL = 10000;

    private Experience() {
        // Utility class.
    }

    public static int getMinimumExperienceForLevel(int level) {
        if (level < 5) {
            return level * COST_PER_LEVEL;
        }
        if (level < 10) {
            return (level - 5) * 20000 + 50000;
        }
        if (level < 15) {
            return (level - 10) * 30000 + 150000;
        }
        if (level < 20) {
            return (level - 15) * 40000 + 300000;
        }
        return (level - 20) * 50000 + 500000;
    }
}