package com.softwaremagico.librodeesher.level;

/** Rolemaster's minimum accumulated experience required for each character level. */
public final class Experience {

    private Experience() {
        // Utility class.
    }

    public static int getMinimumExperienceForLevel(int level) {
        if (level < 5) {
            return level * 10_000;
        }
        if (level < 10) {
            return (level - 5) * 20_000 + 50_000;
        }
        if (level < 15) {
            return (level - 10) * 30_000 + 150_000;
        }
        if (level < 20) {
            return (level - 15) * 40_000 + 300_000;
        }
        return (level - 20) * 50_000 + 500_000;
    }
}
