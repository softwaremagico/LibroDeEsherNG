package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.character.CharacterPlayer;

/**
 * A single character level, exported for sharing (the legacy {@code LevelExporter}): the level's
 * own gains ({@code level}) plus the metadata needed to know exactly which character and which
 * level number it belongs to. A game master prepares a character's next level, exports it, and a
 * player imports it (see {@link LevelJsonManager}) — which validates both the character identity
 * and that the level is the very next one, so level progression can never be replayed or skipped.
 */
public final class LevelExporter {

    @JsonProperty("level")
    private LevelData level;

    @JsonProperty("characterName")
    private String characterName;

    @JsonProperty("characterRaceId")
    private String characterRaceId;

    @JsonProperty("characterProfessionId")
    private String characterProfessionId;

    @JsonProperty("levelNumber")
    private int levelNumber;

    @JsonProperty("characterComparatorId")
    private String characterComparatorId;

    public LevelExporter() {
        // Required by deserialization frameworks.
    }

    /** Snapshots {@code character}'s current level for export. */
    public static LevelExporter forCharacter(CharacterPlayer character) {
        final CharacterData snapshot = CharacterDataMapper.toData(character);
        final LevelExporter exporter = new LevelExporter();
        exporter.level = snapshot.getLevels().get(snapshot.getLevels().size() - 1);
        exporter.characterName = character.getName();
        exporter.characterRaceId = character.getRaceId();
        exporter.characterProfessionId = character.getProfessionId();
        exporter.levelNumber = character.getLevel();
        exporter.characterComparatorId = CharacterDataMapper.toIdentitySnapshot(character);
        return exporter;
    }

    public LevelData getLevel() {
        return level;
    }

    public void setLevel(LevelData level) {
        this.level = level;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getCharacterRaceId() {
        return characterRaceId;
    }

    public void setCharacterRaceId(String characterRaceId) {
        this.characterRaceId = characterRaceId;
    }

    public String getCharacterProfessionId() {
        return characterProfessionId;
    }

    public void setCharacterProfessionId(String characterProfessionId) {
        this.characterProfessionId = characterProfessionId;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public void setLevelNumber(int levelNumber) {
        this.levelNumber = levelNumber;
    }

    public String getCharacterComparatorId() {
        return characterComparatorId;
    }

    public void setCharacterComparatorId(String characterComparatorId) {
        this.characterComparatorId = characterComparatorId;
    }
}