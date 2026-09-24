package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.random.exceptions.InvalidRandomElementSelectedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.ToIntFunction;

/**
 * Selects one element out of a collection proportionally to an integer weight per element,
 * reproducing the cumulative-keyed weighted selection of the ThinkMachine 4E {@code RandomSelector}.
 *
 * <p>Elements with a non-positive weight carry no probability mass and are dropped. The weighted map
 * stores each element under the cumulative sum of the previous weights (plus a trailing {@code null}
 * sentinel for the last element's tail), so picking a uniformly distributed index and taking the
 * highest key at or below it yields each element with probability proportional to its weight.</p>
 *
 * <p>{@link #selectElementByWeight()} uses the generator injected at construction time (a fresh
 * {@link Random} by default), so tests and callers that want a reproducible sequence simply pass a
 * seeded {@link Random}.</p>
 *
 * @param <E> the type of the selectable elements.
 */
public final class RandomSelector<E> {

    private final List<E> allElements;
    private final TreeMap<Integer, E> weightedElements;
    private final int totalWeight;
    private final Random random;

    public RandomSelector(Collection<E> elements, ToIntFunction<E> weightFunction) {
        this(elements, weightFunction, new Random());
    }

    public RandomSelector(Collection<E> elements, ToIntFunction<E> weightFunction, Random random) {
        this.allElements = new ArrayList<>(elements);
        final TreeMap<Integer, E> calculatedWeight = new TreeMap<>();
        int count = 0;
        for (final E element : elements) {
            final int weight = weightFunction.applyAsInt(element);
            if (weight > 0) {
                calculatedWeight.put(count, element);
                count += weight;
            }
        }
        // Last element probability tail.
        if (!calculatedWeight.isEmpty()) {
            calculatedWeight.put(count, null);
        }
        this.weightedElements = calculatedWeight;
        this.totalWeight = count;
        this.random = random;
    }

    /** Whether no element carries a positive weight. */
    public boolean isEmpty() {
        return weightedElements.isEmpty() || weightedElements.size() <= 1;
    }

    /** How many elements carry a positive weight. */
    public int size() {
        return Math.max(0, weightedElements.size() - 1);
    }

    /** The sum of every positive weight. */
    public int getTotalWeight() {
        return totalWeight;
    }

    /**
     * Selects an element with probability proportional to its weight, matching the ThinkMachine {@code
     * RandomSelector#selectElementByWeight()}.
     *
     * @throws InvalidRandomElementSelectedException if no element carries a positive weight.
     */
    public E selectElementByWeight() throws InvalidRandomElementSelectedException {
        if (isEmpty()) {
            throw new InvalidRandomElementSelectedException("No elements to select");
        }
        final int value = random.nextInt(totalWeight);
        final SortedMap<Integer, E> head = weightedElements.headMap(value, true);
        E selected;
        try {
            selected = head.get(head.lastKey());
        } catch (NoSuchElementException nse) {
            // If the weight of the first element is greater than 1, the drawn value can be below its
            // own cumulative key, leaving 'head' empty: select the first element by default.
            selected = weightedElements.values().iterator().next();
        }
        if (selected == null) {
            throw new InvalidRandomElementSelectedException("No elements to select");
        }
        return selected;
    }

    /**
     * Picks a uniformly random element without applying weights, as a controlled fallback when
     * weighting produces no candidate.
     */
    public E selectAnyElement() throws InvalidRandomElementSelectedException {
        if (allElements.isEmpty()) {
            throw new InvalidRandomElementSelectedException("No elements to select");
        }
        return allElements.get(random.nextInt(allElements.size()));
    }

    /** The cumulative weight -> element map (trailing {@code null} sentinel for the last tail). */
    public TreeMap<Integer, E> getWeightedElements() {
        return weightedElements;
    }
}