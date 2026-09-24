package com.softwaremagico.librodeesher.random;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Verifies the seedable primitives of {@link RandomValues}. */
@Test(groups = "randomValues")
public class RandomValuesTest {

    @Test
    public void chanceFollowsTheLegacyPercentRoll() {
        // A chance of 0 never succeeds and a chance above 100 always succeeds.
        for (int i = 0; i < 200; i++) {
            Assert.assertFalse(RandomValues.chance(0));
            Assert.assertTrue(RandomValues.chance(101));
        }
    }

    @Test
    public void chanceIsMostlySuccessfulAtOneHundredPercent() {
        // The legacy comparison is strict: a percent of 100 still fails roughly one roll in a hundred.
        int successes = 0;
        for (int i = 0; i < 10000; i++) {
            if (RandomValues.chance(100)) {
                successes++;
            }
        }
        Assert.assertTrue(successes > 9000 && successes < 10000,
                "expected ~99% successes at percent 100, got " + successes + "/10000");
    }

    @Test
    public void sequenceIsReproducibleAfterSettingTheSameSeed() {
        final List<Double> first = drawSequence(1234L);
        final List<Double> second = drawSequence(1234L);

        Assert.assertEquals(first, second);
    }

    @Test
    public void sequenceDiffersBetweenDifferentSeeds() {
        final Set<Double> first = new HashSet<>(drawSequence(1L));
        final Set<Double> second = new HashSet<>(drawSequence(2L));

        Assert.assertNotEquals(first, second);
    }

    @Test
    public void randomStaysWithinTheDocumentedBound() {
        RandomValues.setRandomSeed(7);
        for (int i = 0; i < 5000; i++) {
            final double value = RandomValues.random();
            Assert.assertTrue(value >= 0.0 && value < 1.0, "out of bound: " + value);
        }
    }

    @Test
    public void nextIntStaysWithinBounds() {
        RandomValues.setRandomSeed(8);
        for (int i = 0; i < 5000; i++) {
            final int value = RandomValues.nextInt(5);
            Assert.assertTrue(value >= 0 && value < 5, "out of bound: " + value);
        }
    }

    @Test
    public void pickSelectsAnElementOfTheGivenList() {
        RandomValues.setRandomSeed(9);
        final List<String> values = List.of("one", "two", "three");

        final Set<String> picked = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            picked.add(RandomValues.pick(values));
        }
        Assert.assertTrue(picked.stream().allMatch(values::contains));
        Assert.assertTrue(picked.size() > 1, "expected at least two distinct picks, got " + picked.size());
    }

    @Test
    public void shuffleLeavesTheInputUntouched() {
        final List<String> values = new ArrayList<>(List.of("a", "b", "c", "d", "e"));

        final List<String> shuffled = RandomValues.shuffle(values);

        Assert.assertEquals(values, List.of("a", "b", "c", "d", "e"));
        Assert.assertEquals(shuffled.size(), values.size());
        Assert.assertTrue(shuffled.containsAll(values));
    }

    private static List<Double> drawSequence(long seed) {
        RandomValues.setRandomSeed(seed);
        final List<Double> sequence = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            sequence.add(RandomValues.random());
        }
        return sequence;
    }
}