package com.softwaremagico.librodeesher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * Builds and caches the shared {@link ObjectMapper}s used by the rulebook XML files and by the
 * character JSON persistence format.
 *
 * <p>Jackson (not JAXB, DOM or SAX) is used on purpose: it lets the domain classes stay plain
 * annotated POJOs, keeps the module free of code generation steps, and is fully compatible with
 * Android.</p>
 */
public final class ObjectMapperFactory {

    private static XmlMapper xmlObjectMapper;
    private static ObjectMapper jsonObjectMapper;

    private ObjectMapperFactory() {
        // Utility class.
    }

    /**
     * Returns the shared {@link ObjectMapper} configured for XML.
     *
     * <ul>
     *     <li>Unknown XML elements do not fail deserialization, so a rulebook file can contain extra
     *     tags added by a newer version of the schema without breaking older readers.</li>
     *     <li>Empty/absent collections and nulls are omitted when writing (used by the migration
     *     tool that generates the XML files from the legacy tab-separated data).</li>
     * </ul>
     */
    public static synchronized ObjectMapper getXmlObjectMapper() {
        if (xmlObjectMapper == null) {
            xmlObjectMapper = XmlMapper.builder()
                    .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
                    .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                    .serializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .build();
        }
        return xmlObjectMapper;
    }

    /**
     * Returns the shared {@link ObjectMapper} configured for the character JSON persistence format.
     *
     * <ul>
     *     <li>Properties are emitted alphabetically and map entries are ordered by key, so two saves
     *     of the same character produce byte-identical JSON (the persistence layer relies on this
     *     for stable diffs and tests).</li>
     *     <li>Unknown fields do not fail deserialization, keeping readers forward-compatible with
     *     characters saved by newer versions of the app.</li>
     *     <li>Null values are omitted, keeping empty characters compact.</li>
     * </ul>
     */
    public static synchronized ObjectMapper getJsonObjectMapper() {
        if (jsonObjectMapper == null) {
            jsonObjectMapper = new ObjectMapper();
            jsonObjectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            jsonObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            jsonObjectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            jsonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return jsonObjectMapper;
    }
}
