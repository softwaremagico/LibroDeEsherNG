package com.softwaremagico.librodeesher.exceptions;

/**
 * Thrown when a rulebook XML file (or one of its elements) cannot be parsed into the expected domain
 * object, or when a requested element id does not exist in any enabled module.
 */
public class InvalidXmlElementException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidXmlElementException(String message) {
        super(message);
    }

    public InvalidXmlElementException(String message, Throwable cause) {
        super(message, cause);
    }
}
