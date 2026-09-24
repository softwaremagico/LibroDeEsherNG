package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.random.exceptions.InvalidRandomElementSelectedException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Random;

/** Verifies the cumulative weighted selection of {@link RandomSelector}. */
@Test(groups = "randomSelector")
public class RandomSelectorTest {

    private static final List<String> ELEMENTS = List.of("a", "b", "c", "d");

    @Test
    public void buildsCumulativeWeightedMapWithTrailingSentinel() {
        final RandomSelector<String> selector = new RandomSelector<>(ELEMENTS, value -> value.charAt(0));

        // Weights: a=97, b=98, c=99, d=100. Cumulative keys: 0, 97, 195, 294, plus 394 as null tail.
        Assert.assertEquals(selector.getTotalWeight(), 394);
        Assert.assertEquals(selector.size(), 4);
        Assert.assertEquals(selector.getWeightedElements().keySet().toArray(), new Object[] { 0, 97, 195, 294, 394 });
        Assert.assertEquals(selector.getWeightedElements().get(0), "a");
        Assert.assertEquals(selector.getWeightedElements().get(97), "b");
        Assert.assertEquals(selector.getWeightedElements().get(195), "c");
        Assert.assertEquals(selector.getWeightedElements().get(294), "d");
        Assert.assertNull(selector.getWeightedElements().get(394));
    }

    @Test
    public void dropsElementsWithNonPositiveWeight() throws InvalidRandomElementSelectedException {
        final RandomSelector<String> selector = new RandomSelector<>(List.of("a", "b", "c"),
                value -> "b".equals(value) ? 0 : 1);

        Assert.assertEquals(selector.size(), 2);
        Assert.assertTrue(selector.getWeightedElements().values().contains("a"));
        Assert.assertTrue(selector.getWeightedElements().values().contains("c"));
        Assert.assertFalse(selector.getWeightedElements().values().contains("b"));
    }

    @Test
    public void neverSelectsDroppedElements() throws InvalidRandomElementSelectedException {
        final RandomSelector<String> selector = new RandomSelector<>(List.of("a", "b", "c"),
                value -> "b".equals(value) ? 0 : 1, new Random(1));

        for (int i = 0; i < 200; i++) {
            Assert.assertNotEquals(selector.selectElementByWeight(), "b");
        }
    }

    @Test
    public void selectionIsDeterministicGivenTheSameSeed() throws InvalidRandomElementSelectedException {
        final RandomSelector<String> first = new RandomSelector<>(ELEMENTS, value -> value.charAt(0), new Random(42));
        final RandomSelector<String> second = new RandomSelector<>(ELEMENTS, value -> value.charAt(0), new Random(42));

        for (int i = 0; i < 50; i++) {
            Assert.assertEquals(first.selectElementByWeight(), second.selectElementByWeight());
        }
    }

    @Test
    public void selectionWeightsAreRespected() throws InvalidRandomElementSelectedException {
        // "heavy" carries 98% of the probability mass.
        final RandomSelector<String> selector = new RandomSelector<>(
                List.of("light", "heavy"), value -> "heavy".equals(value) ? 98 : 2, new Random(7));

        int heavy = 0;
        for (int i = 0; i < 1000; i++) {
            if ("heavy".equals(selector.selectElementByWeight())) {
                heavy++;
            }
        }
        Assert.assertTrue(heavy > 900, "expected mostly 'heavy', got " + heavy);
    }

    @Test(expectedExceptions = InvalidRandomElementSelectedException.class)
    public void selectionFromEmptySelectorThrows() throws InvalidRandomElementSelectedException {
        new RandomSelector<>(List.of(), value -> 1).selectElementByWeight();
    }

    @Test(expectedExceptions = InvalidRandomElementSelectedException.class)
    public void selectionWhenEveryWeightIsZeroThrows() throws InvalidRandomElementSelectedException {
        new RandomSelector<>(List.of("a", "b"), value -> 0).selectElementByWeight();
    }

    @Test
    public void selectAnyElementIgnoresWeights() throws InvalidRandomElementSelectedException {
        final RandomSelector<String> selector = new RandomSelector<>(
                List.of("light", "heavy"), value -> "heavy".equals(value) ? 0 : 0, new Random(3));

        for (int i = 0; i < 20; i++) {
            final String selected = selector.selectAnyElement();
            Assert.assertTrue(selected.equals("light") || selected.equals("heavy"));
        }
    }

    @Test(expectedExceptions = InvalidRandomElementSelectedException.class)
    public void selectAnyElementFromEmptySelectorThrows() throws InvalidRandomElementSelectedException {
        new RandomSelector<>(List.of(), value -> 1).selectAnyElement();
    }
}