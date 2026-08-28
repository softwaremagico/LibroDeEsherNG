package com.softwaremagico.librodeesher.decision;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * A resolved "choose one (or N) of {@code offeredOptions}" grant (e.g. a training's category choice,
 * or a profession's "choose N skills from this category" grant), validated against what was actually
 * offered.
 *
 * <p>Used to resolve any of the {@code {alt1; alt2; ...}} choose-one constructs found throughout the
 * migrated rule data: {@code ChoiceGroup} (a training's characteristic upgrades and common/
 * professional/restricted skill sections), {@code TrainingCategoryGrant} (choice of category) and
 * {@code TrainingSkillGrant} (choice of skill within a category grant already resolved). A grant
 * that only ever offered a single, fixed option is also represented as a {@code Decision} (via
 * {@link #fixed(List)}), so calling code can treat every grant uniformly instead of special-casing
 * "not really a choice".</p>
 *
 * <p>{@link #selectMultiple(List, List, int)} additionally supports "choose N of M" grants (e.g.
 * {@code ProfessionSkillGrant}, where {@code N} may be greater than 1); {@link #getSelectedOption()}
 * remains the single-selection accessor (every existing choose-one caller keeps working unchanged),
 * while {@link #getSelectedOptions()} exposes the full list for "choose N" callers.</p>
 */
public final class Decision {

    private final List<String> offeredOptions;
    private final List<String> selectedOptions;

    private Decision(List<String> offeredOptions, List<String> selectedOptions) {
        this.offeredOptions = offeredOptions;
        this.selectedOptions = selectedOptions;
    }

    /**
     * Resolves a choice by validating {@code selectedOption} is one of {@code offeredOptions}.
     *
     * @throws InvalidDecisionException if {@code offeredOptions} is empty, {@code selectedOption} is
     *                                  null/blank, or it is not one of {@code offeredOptions}.
     */
    public static Decision select(List<String> offeredOptions, String selectedOption) {
        requireNonEmptyOptions(offeredOptions);
        if (selectedOption == null || selectedOption.isBlank()) {
            throw new InvalidDecisionException("A selection is required among " + offeredOptions + ".");
        }
        if (!offeredOptions.contains(selectedOption)) {
            throw new InvalidDecisionException(
                    "'" + selectedOption + "' is not one of the offered options " + offeredOptions + ".");
        }
        return new Decision(List.copyOf(offeredOptions), List.of(selectedOption));
    }

    /**
     * Resolves a "choose {@code count} of {@code offeredOptions}" grant: {@code selectedOptions} must
     * have exactly {@code count} distinct entries, every one of them among {@code offeredOptions}.
     *
     * @throws InvalidDecisionException if {@code offeredOptions} is empty, {@code selectedOptions} is
     *                                  null, does not have exactly {@code count} distinct entries, or
     *                                  any of them is not one of {@code offeredOptions}.
     */
    public static Decision selectMultiple(List<String> offeredOptions, List<String> selectedOptions, int count) {
        requireNonEmptyOptions(offeredOptions);
        if (selectedOptions == null) {
            throw new InvalidDecisionException("A selection of " + count + " is required among " + offeredOptions + ".");
        }
        final List<String> distinct = List.copyOf(new LinkedHashSet<>(selectedOptions));
        if (distinct.size() != selectedOptions.size()) {
            throw new InvalidDecisionException("Duplicate selections are not allowed: " + selectedOptions + ".");
        }
        if (distinct.size() != count) {
            throw new InvalidDecisionException(
                    "Expected " + count + " selection(s), got " + distinct.size() + ": " + selectedOptions + ".");
        }
        for (final String selected : distinct) {
            if (!offeredOptions.contains(selected)) {
                throw new InvalidDecisionException(
                        "'" + selected + "' is not one of the offered options " + offeredOptions + ".");
            }
        }
        return new Decision(List.copyOf(offeredOptions), distinct);
    }

    /**
     * Auto-resolves a grant that only ever offered a single, fixed option (not really a choice).
     *
     * @throws InvalidDecisionException if {@code offeredOptions} does not have exactly one option.
     */
    public static Decision fixed(List<String> offeredOptions) {
        requireNonEmptyOptions(offeredOptions);
        if (offeredOptions.size() > 1) {
            throw new InvalidDecisionException(
                    "Not a fixed grant, a selection is required among " + offeredOptions + ".");
        }
        return new Decision(List.copyOf(offeredOptions), List.of(offeredOptions.get(0)));
    }

    private static void requireNonEmptyOptions(List<String> offeredOptions) {
        if (offeredOptions == null || offeredOptions.isEmpty()) {
            throw new InvalidDecisionException("No options were offered.");
        }
    }

    /** The single selected option; the first one, if this decision actually holds several (see {@link #getSelectedOptions()}). */
    public String getSelectedOption() {
        return selectedOptions.get(0);
    }

    /** Every selected option, in selection order (a single-element list for an ordinary choose-one decision). */
    public List<String> getSelectedOptions() {
        return selectedOptions;
    }

    public List<String> getOfferedOptions() {
        return offeredOptions;
    }

    /** Whether this grant actually offered more than one option (as opposed to a fixed grant). */
    public boolean isChoice() {
        return offeredOptions.size() > 1;
    }

    @Override
    public String toString() {
        return selectedOptions + " (of " + offeredOptions + ")";
    }
}
