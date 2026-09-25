package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A persisted {@link com.softwaremagico.librodeesher.decision.Decision}: the options the grant
 * offered and the option(s) that were selected. {@code Decision} itself is a final value object with
 * a private constructor (it validates at build time through its static factories), so it cannot be
 * deserialized directly; the mapper rebuilds it from these two lists with the same factories.
 */
public final class DecisionData {

    @JsonProperty("offeredOptions")
    private List<String> offeredOptions = new ArrayList<>();

    @JsonProperty("selectedOptions")
    private List<String> selectedOptions = new ArrayList<>();

    public DecisionData() {
        // Required by deserialization frameworks.
    }

    public DecisionData(List<String> offeredOptions, List<String> selectedOptions) {
        this.offeredOptions = offeredOptions;
        this.selectedOptions = selectedOptions;
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