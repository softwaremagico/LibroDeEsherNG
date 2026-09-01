package com.softwaremagico.librodeesher.dice;

import java.util.Random;

/** Rolls dice of an arbitrary number of faces, used by every random game rule (characteristics, age). */
public final class Dice {

    private static final Random GENERATOR = new Random();

    private Dice() {
        // Utility class.
    }

    /** Rolls {@code dices} dice of {@code faces} faces each and returns their sum. */
    public static int getRoll(int dices, int faces) {
        int total = 0;
        for (int i = 0; i < dices; i++) {
            total += getRoll(faces);
        }
        return total;
    }

    /** Rolls a single die of {@code faces} faces. */
    public static int getRoll(int faces) {
        return GENERATOR.nextInt(faces) + 1;
    }
}
