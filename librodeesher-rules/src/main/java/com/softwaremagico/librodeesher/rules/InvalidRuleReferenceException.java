package com.softwaremagico.librodeesher.rules;

/**
 * Thrown when a rule object references another rule id that is not present in the currently enabled
 * module set (for example a race references a category id that no loaded {@code categories.xml}
 * defines).
 */
public class InvalidRuleReferenceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRuleReferenceException(String message) {
        super(message);
    }
}
