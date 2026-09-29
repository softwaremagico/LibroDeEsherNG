package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A persisted {@link com.softwaremagico.librodeesher.decision.Decision}: the structured
 * {@link DecisionKey} it was filed under, the level it was taken at, and the options the grant
 * offered plus the option(s) that were selected. {@code Decision} itself is a final value object with
 * a private constructor (it validates at build time through its static factories), so it cannot be
 * deserialized directly; the mapper rebuilds it from these fields with the same factories.
 */
public final class DecisionData {

    @JsonProperty("key")
    private DecisionKeyData key = new DecisionKeyData();

    @JsonProperty("recordedAtLevel")
    private int recordedAtLevel;

    @JsonProperty("offeredOptions")
    private List<String> offeredOptions = new ArrayList<>();

    @JsonProperty("selectedOptions")
    private List<String> selectedOptions = new ArrayList<>();

    public DecisionData() {
        // Required by deserialization frameworks.
    }

    public DecisionData(DecisionKeyData key, int recordedAtLevel, List<String> offeredOptions,
                        List<String> selectedOptions) {
        this.key = key;
        this.recordedAtLevel = recordedAtLevel;
        this.offeredOptions = offeredOptions;
        this.selectedOptions = selectedOptions;
    }

    public DecisionKeyData getKey() {
        return key;
    }

    public void setKey(DecisionKeyData key) {
        this.key = key;
    }

    public int getRecordedAtLevel() {
        return recordedAtLevel;
    }

    public void setRecordedAtLevel(int recordedAtLevel) {
        this.recordedAtLevel = recordedAtLevel;
    }

    public List<String> getOfferedOptions() {
        return offeredOptions;
    }

    public void setOfferedOptions(List<String> offeredOptions) {
        this.offeredOptions = offeredOptions;
    }

    public List<String> getSelectedOptions() {
        return selectedOptions;
    }

    public void setSelectedOptions(List<String> selectedOptions) {
        this.selectedOptions = selectedOptions;
    }
}