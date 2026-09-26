package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.softwaremagico.librodeesher.ObjectMapperFactory;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.level.LevelUp;

/**
 * Exports a character's current level as a self-contained JSON document (see {@link
 * LevelExporter}) and imports it back onto the same character one level behind, mirroring legacy
 * {@code LevelJsonManager}: a game master prepares a character's next level and shares it, and the
 * owner's copy validates that the level belongs to this exact character and is the very next one,
 * so level bookkeeping can never be replayed out of order or doubled.
 */
public final class LevelJsonManager {

    private LevelJsonManager() {
        // Utility class.
    }

    /** Serializes {@code characterPlayer}'s current level, or {@code null} for a null character. */
    public static String toJson(CharacterPlayer characterPlayer) {
        if (characterPlayer == null) {
            return null;
        }
        try {
            return ObjectMapperFactory.getJsonObjectMapper()
                    .writeValueAsString(LevelExporter.forCharacter(characterPlayer));
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize the character's current level.", exception);
        }
    }

    /**
     * Returns the level stored in {@code jsonText} as a {@link LevelUp} ready to be applied, after
     * validating it was exported for this exact character ({@link InvalidCharacterException}) and
     * that it is the target character's next level ({@link InvalidLevelException}). Returns {@code
     * null} for a null character or a null/blank JSON.
     */
    public static LevelUp fromJson(CharacterPlayer characterPlayer, String jsonText)
            throws InvalidLevelException, InvalidCharacterException {
        if (characterPlayer == null || jsonText == null || jsonText.isBlank()) {
            return null;
        }
        final LevelExporter exporter = parse(jsonText);
        if (!exporter.getCharacterComparatorId().equals(CharacterDataMapper.toIdentitySnapshot(characterPlayer))
                || !equals(characterPlayer.getName(), exporter.getCharacterName())
                || !equals(characterPlayer.getRaceId(), exporter.getCharacterRaceId())
                || !equals(characterPlayer.getProfessionId(), exporter.getCharacterProfessionId())) {
            throw new InvalidCharacterException("Invalid level to be imported. Level is defined for '"
                    + exporter.getCharacterName() + "' and the actual character is '" + characterPlayer.getName() + "'");
        }
        if (exporter.getLevelNumber() != characterPlayer.getLevel() + 1) {
            throw new InvalidLevelException("Level invalid to be imported. Level to import is '"
                    + exporter.getLevelNumber() + "' and the character has level '" + characterPlayer.getLevel() + "'.");
        }
        return CharacterDataMapper.toLevelUp(exporter.getLevel());
    }

    private static LevelExporter parse(String jsonText) {
        try {
            return ObjectMapperFactory.getJsonObjectMapper().readValue(jsonText, LevelExporter.class);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize the shared level.", exception);
        }
    }

    private static boolean equals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}