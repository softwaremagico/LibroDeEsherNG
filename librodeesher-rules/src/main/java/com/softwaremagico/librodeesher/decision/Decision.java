package com.softwaremagico.librodeesher.decision;

import java.util.List;

/**
 * A resolved "choose one of {@code offeredOptions}" grant (e.g. a training's category choice, or one
 * of its nested skill choices), validated against what was actually offered.
 *
 * <p>Used to resolve any of the {@code {alt1; alt2; ...}} choose-one constructs found throughout the
 * migrated rule data: {@code ChoiceGroup} (a training's characteristic upgrades and common/
 * professional/restricted skill sections), {@code TrainingCategoryGrant} (choice of category) and
 * {@code TrainingSkillGrant} (choice of skill within a category grant already resolved). A grant
 * that only ever offered a single, fixed option is also represented as a {@code Decision} (via
 * {@link #fixed(List)}), so calling code can treat every grant uniformly instead of special-casing
 * "not really a choice".</p>
 */
public final class Decision {

    private final List<String> offeredOptions;
    private final String selectedOption;

    private Decision(List<String> offeredOptions, String selectedOption) {
        this.offeredOptions = offeredOptions;
        this.selectedOption = selectedOption;
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
        return new Decision(List.copyOf(offeredOptions), selectedOption);
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
        return new Decision(List.copyOf(offeredOptions), offeredOptions.get(0));
    }

    private static void requireNonEmptyOptions(List<String> offeredOptions) {
        if (offeredOptions == null || offeredOptions.isEmpty()) {
            throw new InvalidDecisionException("No options were offered.");
        }
    }

    public String getSelectedOption() {
        return selectedOption;
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
        return selectedOption + " (of " + offeredOptions + ")";
    }
}
