package com.softwaremagico.librodeesher.decision;

/**
 * Thrown when resolving a {@link Decision} fails: the selected option is not one of the offered
 * options, or a required selection is missing/blank.
 */
public class InvalidDecisionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidDecisionException(String message) {
        super(message);
    }
}
