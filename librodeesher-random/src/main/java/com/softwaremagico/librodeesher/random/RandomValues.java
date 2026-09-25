package com.softwaremagico.librodeesher.random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Central, seedable randomness source for the random character generator, replacing the bare
 * {@code Math.random()} calls of the legacy {@code pj.random} package so a whole generation can be
 * reproduced in tests.
 *
 * <p>Every helper mirrors the exact legacy expression it replaces, including its off-by-one quirks:
 * {@link #chance(int)} is a roll whose success condition is the legacy {@code Math.random() * 100 + 1
 * < percent} comparison (so a percent of 100 still fails roughly once in a hundred rolls) and {@link
 * #pick(List)} is the legacy {@code (int) (Math.random() * list.size())} index.</p>
 */
public final class RandomValues {

    /** {@code [0, 1)} bound of the legacy {@code Math.random()} expression. */
    private static final int PERCENT_ROLL_BOUND = 100;

    private static final Random RANDOM = new Random();

    private RandomValues() {
        // Utility class.
    }

    /**
     * Fixes the shared generator's seed, making every non-{@link Random}-injected helper call below
     * predictable. Call it before the sequence you want to reproduce.
     */
    public static void setRandomSeed(long seed) {
        RANDOM.setSeed(seed);
    }

    /**
     * The shared generator itself, for seedable helper methods that accept a {@link Random}
     * (e.g. {@code Race#getRandomName(SexType, Random)}); because it is the very same instance the
     * static helpers below draw from, the seed fixes its whole sequence too.
     */
    public static Random getRandom() {
        return RANDOM;
    }

    /** A uniformly distributed double in {@code [0, 1)}, equivalent to {@code Math.random()}. */
    public static double random() {
        return RANDOM.nextDouble();
    }

    /** A uniformly distributed integer in {@code [0, bound)}. */
    public static int nextInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    /**
     * A {@code percent}-out-of-100 success roll, mirroring the legacy {@code Math.random() * 100 + 1
     * < percent} comparison exactly.
     */
    public static boolean chance(int percent) {
        return RANDOM.nextInt(PERCENT_ROLL_BOUND) + 1 < percent;
    }

    /** A uniformly selected element of a non-empty {@code values} list. */
    public static <T> T pick(List<T> values) {
        return values.get(RANDOM.nextInt(values.size()));
    }

    /** A shuffled copy of {@code values} (the input list is left untouched). */
    public static <T> List<T> shuffle(List<T> values) {
        final List<T> shuffled = new ArrayList<>(values);
        Collections.shuffle(shuffled, RANDOM);
        return shuffled;
    }
}