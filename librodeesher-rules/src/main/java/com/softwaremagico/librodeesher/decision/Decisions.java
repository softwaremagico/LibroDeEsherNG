package com.softwaremagico.librodeesher.decision;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Every {@link Decision} a character has made so far, keyed by a structured {@link DecisionKey}
 * (what kind of grant it resolves, which rule element offered it, which grant of it, and the level it
 * belongs to) instead of a string assembled by the caller.
 *
 * <p>Because the level is part of the key, the same grant resolved at two different levels yields
 * two independent decisions rather than the second one silently reusing the first. Decisions can be
 * listed back per level with {@link #getDecisionsAtLevel(int)}, which is what reconstructs "the
 * options this character had chosen by level N".</p>
 */
public class Decisions {

    private final Map<DecisionKey, Decision> decisions = new LinkedHashMap<>();

    /**
     * Records (or replaces) the decision for {@code key}.
     *
     * <p>The decision must already be stamped with the level it was taken at (see
     * {@link Decision#recordedAtLevel(int)}); for a per-level key both must agree, so a decision can
     * never be filed under a level it was not taken at. A character-wide key does not constrain the
     * decision's level: the character-wide choice was still taken at some concrete level, which the
     * decision carries.</p>
     *
     * @throws IllegalArgumentException if a per-level key and the decision's level disagree.
     */
    public void set(DecisionKey key, Decision decision) {
        if (key.getLevel() != DecisionKey.CHARACTER_WIDE && !decision.isRecordedAtLevel(key.getLevel())) {
            throw new IllegalArgumentException("Key " + key + " does not match the level its decision was taken at ("
                    + decision.getRecordedAtLevel() + ").");
        }
        decisions.put(key, decision);
    }

    /** The decision for {@code key}, or {@code null} if it has not been resolved yet. */
    public Decision get(DecisionKey key) {
        return decisions.get(key);
    }

    public boolean isDecided(DecisionKey key) {
        return decisions.containsKey(key);
    }

    /** The selected option for {@code key}, or {@code null} if it has not been resolved yet. */
    public String getSelectedOption(DecisionKey key) {
        final Decision decision = decisions.get(key);
        return decision == null ? null : decision.getSelectedOption();
    }

    public void remove(DecisionKey key) {
        decisions.remove(key);
    }

    /** Every recorded decision, keyed by its {@link DecisionKey}. */
    public Map<DecisionKey, Decision> getAll() {
        return decisions;
    }

    /**
     * Every decision belonging to {@code kind} and {@code ownerId}, whatever the level, ordered by
     * level and then by grant: a training's category/skill choices, a profession's realm choices,
     * and so on.
     */
    public List<DecisionKey> getKeysFor(DecisionKind kind, String ownerId) {
        return decisions.keySet().stream()
                .filter(key -> key.getKind() == kind && key.getOwnerId().equals(ownerId))
                .sorted(Comparator.comparingInt(DecisionKey::getLevel).thenComparingInt(DecisionKey::getIndex)
                        .thenComparingInt(DecisionKey::getSubIndex))
                .collect(Collectors.toList());
    }

    /** The {@code index}-th decision of {@code kind} for {@code ownerId}, whatever the level, or {@code null}. */
    public Decision getFor(DecisionKind kind, String ownerId, int index) {
        for (final Map.Entry<DecisionKey, Decision> entry : decisions.entrySet()) {
            if (entry.getKey().getKind() == kind && entry.getKey().getOwnerId().equals(ownerId)
                    && entry.getKey().getIndex() == index) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Whether the {@code index}-th decision of {@code kind} for {@code ownerId} was taken, whatever the level. */
    public boolean isDecidedFor(DecisionKind kind, String ownerId, int index) {
        return getFor(kind, ownerId, index) != null;
    }

    /** The selected option of the {@code index}-th decision of {@code kind} for {@code ownerId}, or {@code null}. */
    public String getSelectedOptionFor(DecisionKind kind, String ownerId, int index) {
        final Decision decision = getFor(kind, ownerId, index);
        return decision == null ? null : decision.getSelectedOption();
    }

    /** The selected options of the {@code index}-th decision of {@code kind} for {@code ownerId}, or an empty list. */
    public List<String> getSelectedOptionsFor(DecisionKind kind, String ownerId, int index) {
        final Decision decision = getFor(kind, ownerId, index);
        return decision == null ? List.of() : decision.getSelectedOptions();
    }

    /**
     * Every decision taken at {@code level} (1-based), in the order they were recorded, so the
     * options chosen while building that level can be listed back.
     */
    public List<Map.Entry<DecisionKey, Decision>> getDecisionsAtLevel(int level) {
        return decisions.entrySet().stream()
                .filter(entry -> entry.getValue().isRecordedAtLevel(level))
                .collect(Collectors.toList());
    }

    /** The selected options of every decision taken at {@code level} (1-based). */
    public List<String> getSelectedOptionsAtLevel(int level) {
        return getDecisionsAtLevel(level).stream()
                .map(entry -> entry.getValue().getSelectedOptions())
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /** Unmodifiable view of every decision, for reporting. */
    public Map<DecisionKey, Decision> asUnmodifiableMap() {
        return Collections.unmodifiableMap(decisions);
    }
}