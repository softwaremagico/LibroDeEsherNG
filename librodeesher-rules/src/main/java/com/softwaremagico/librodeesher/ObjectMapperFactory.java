package com.softwaremagico.librodeesher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * Builds and caches the single {@link ObjectMapper} used to read and write the rulebook XML files.
 *
 * <p>Jackson XML (not JAXB, DOM or SAX) is used on purpose: it lets the domain classes stay plain
 * annotated POJOs, keeps the module free of code generation steps, and is fully compatible with
 * Android. This mirrors the approach used by the sibling project ThinkMachine-4E.</p>
 */
public final class ObjectMapperFactory {

    private static XmlMapper xmlObjectMapper;

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
}
