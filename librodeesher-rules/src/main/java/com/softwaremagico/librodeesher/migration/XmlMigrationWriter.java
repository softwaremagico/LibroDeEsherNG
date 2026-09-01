package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.ObjectMapperFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes a list of domain elements as a human-readable XML file, for use by the {@code *MigrationTool}
 * classes only (never at application runtime: {@link com.softwaremagico.librodeesher.xml.XmlFactory}
 * only ever reads).
 *
 * <p>Jackson XML does not know a natural per-item element name when serializing a bare
 * {@code List<T>} (it defaults to the generic {@code <item>} tag). Since {@code XmlFactory} reads
 * lists without relying on the item tag name at all (see its javadoc), the exact item tag is purely
 * cosmetic; this class renames it after serialization so the generated files are pleasant to read
 * and review by a human, e.g. {@code <categoria>} instead of {@code <item>}.</p>
 *
 * <p>The rename only targets <strong>root-level</strong> {@code <item>} tags (indented exactly two
 * spaces, since {@link ObjectMapperFactory#getXmlObjectMapper()} always indents with two spaces per
 * level): a domain class may itself contain a nested {@code List<X>} field that Jackson also
 * serializes with generic {@code <item>} tags (e.g. a list of value objects with no
 * {@code @JacksonXmlProperty} override), and a global, unanchored replace would incorrectly rename
 * those too.</p>
 */
final class XmlMigrationWriter {

    private XmlMigrationWriter() {
        // Utility class.
    }

    /**
     * Serializes {@code elements} to {@code targetFile} as {@code <rootTag><itemTag>...}, creating
     * parent directories as needed.
     */
    static <T> void write(Path targetFile, String rootTag, String itemTag, List<T> elements) throws IOException {
        final String rawXml = ObjectMapperFactory.getXmlObjectMapper().writer()
                .withRootName(rootTag)
                .writeValueAsString(elements);
        final String readableXml = rawXml
                .replaceAll("(?m)^  <item>", "  <" + itemTag + ">")
                .replaceAll("(?m)^  </item>", "  </" + itemTag + ">")
                .replaceAll("(?m)^  <item/>", "  <" + itemTag + "/>");

        Files.createDirectories(targetFile.getParent());
        Files.writeString(targetFile, readableXml, StandardCharsets.UTF_8);
    }
}
