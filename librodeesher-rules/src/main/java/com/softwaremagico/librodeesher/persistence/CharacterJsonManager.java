package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwaremagico.librodeesher.ObjectMapperFactory;

/**
 * Reads and writes {@link CharacterData} snapshots as JSON, using the shared deterministic JSON
 * mapper. The JSON text produced for the same snapshot is byte-stable (alphabetical properties and
 * key-sorted maps), and both {@code null} input and a JSON {@code "null"} / empty text round-trip to
 * {@code null}.
 */
public final class CharacterJsonManager {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getJsonObjectMapper();

    private CharacterJsonManager() {
        // Utility class.
    }

    /** Serializes {@code data} to pretty-printed JSON, or {@code null} if {@code data} is null. */
    public static String toJson(CharacterData data) {
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize character data.", exception);
        }
    }

    /** Deserializes {@code json} to a {@link CharacterData}, or {@code null} if it is null or empty. */
    public static CharacterData fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, CharacterData.class);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize character data.", exception);
        }
    }
}