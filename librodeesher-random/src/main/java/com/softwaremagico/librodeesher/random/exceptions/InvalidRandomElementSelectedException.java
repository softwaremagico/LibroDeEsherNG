package com.softwaremagico.librodeesher.random.exceptions;

/** Thrown when a {@code RandomSelector} has no candidate element to pick from. */
public class InvalidRandomElementSelectedException extends Exception {

    public InvalidRandomElementSelectedException(String message) {
        super(message);
    }

    public InvalidRandomElementSelectedException(String message, Throwable cause) {
        super(message, cause);
    }
}