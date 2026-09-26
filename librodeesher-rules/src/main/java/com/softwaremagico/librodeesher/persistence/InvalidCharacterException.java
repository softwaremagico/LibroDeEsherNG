package com.softwaremagico.librodeesher.persistence;

/**
 * Thrown by {@link LevelJsonManager} when a level exported for one character is imported onto a
 * different character (name, race, profession or identity fingerprint do not match).
 */
public class InvalidCharacterException extends Exception {

    public InvalidCharacterException(String message) {
        super(message);
    }
}