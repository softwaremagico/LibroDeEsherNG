package com.softwaremagico.librodeesher.decision;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every {@link Decision} a character has made so far, keyed by an arbitrary caller-chosen string
 * identifying which specific grant it resolves (e.g. {@code "training:soldier:category:1"} for the
 * second category grant of the "soldier" training, or {@code "training:soldier:category:1:skill:0"}
 * for the first nested skill choice within it). The key format is entirely up to the caller; this
 * class only stores and retrieves by it.
 */
public class Decisions {

    private final Map<String, Decision> decisions = new LinkedHashMap<>();

    /** Records (or replaces) the decision for {@code key}. */
    public void set(String key, Decision decision) {
        decisions.put(key, decision);
    }

    /** The decision for {@code key}, or {@code null} if it has not been resolved yet. */
    public Decision get(String key) {
        return decisions.get(key);
    }

    public boolean isDecided(String key) {
        return decisions.containsKey(key);
    }

    /** The selected option for {@code key}, or {@code null} if it has not been resolved yet. */
    public String getSelectedOption(String key) {
        final Decision decision = decisions.get(key);
        return decision == null ? null : decision.getSelectedOption();
    }

    public void remove(String key) {
        decisions.remove(key);
    }

    public Map<String, Decision> getAll() {
        return decisions;
    }
}
