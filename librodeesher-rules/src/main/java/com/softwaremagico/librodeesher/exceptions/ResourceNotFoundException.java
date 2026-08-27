package com.softwaremagico.librodeesher.exceptions;

/**
 * Thrown when a rulebook XML file cannot be located, either on the classpath (bundled inside the jar
 * under {@code modulo/}) or as an external file next to the application.
 */
public class ResourceNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
