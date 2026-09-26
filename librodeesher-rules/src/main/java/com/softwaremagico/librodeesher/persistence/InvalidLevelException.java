package com.softwaremagico.librodeesher.persistence;

/**
 * Thrown by {@link LevelJsonManager} when the level to import is not the very next one for the
 * target character (importing level 3 onto a character currently at level 2 is valid; anything
 * else is rejected to prevent out-of-order or doubled level bookkeeping).
 */
public class InvalidLevelException extends Exception {

    public InvalidLevelException(String message) {
        super(message);
    }
}