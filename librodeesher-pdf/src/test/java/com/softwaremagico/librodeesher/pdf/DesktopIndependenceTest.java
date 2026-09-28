package com.softwaremagico.librodeesher.pdf;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * The library has to stay usable on Android, where the desktop-only parts of {@code java.awt} and
 * Swing are missing, so no source file of any module may import them.
 *
 * <p>{@link com.softwaremagico.librodeesher.pdf.elements.BaseElement} used to import
 * {@code java.awt.Color} to grey out its table headers; it now uses OpenPDF's own {@code RGBColor},
 * which is why this test exists to stop the dependency from creeping back in.</p>
 */
@Test(groups = "pdf")
public class DesktopIndependenceTest {

    /** Packages that only exist on a desktop JVM, never on Android. */
    private static final List<String> FORBIDDEN = List.of(
            "java.awt.", "javax.swing.", "java.beans.", "javax.imageio.");

    @Test
    public void noSourceFileImportsADesktopOnlyPackage() throws IOException {
        final Path root = aggregatorRoot();
        final List<String> offenders = new ArrayList<>();
        int scanned = 0;

        for (final Path sourceFolder : List.of(root.resolve("librodeesher-rules/src/main/java"),
                root.resolve("librodeesher-random/src/main/java"), root.resolve("librodeesher-pdf/src/main/java"))) {
            Assert.assertTrue(Files.isDirectory(sourceFolder), "missing source folder " + sourceFolder);
            try (Stream<Path> files = Files.walk(sourceFolder)) {
                for (final Path file : files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java")).toList()) {
                    scanned++;
                    final List<String> imports = importLinesOf(file);
                    for (final String forbidden : FORBIDDEN) {
                        if (imports.stream().anyMatch(line -> line.contains(forbidden))) {
                            offenders.add(root.relativize(file) + " imports " + forbidden);
                        }
                    }
                }
            }
        }

        Assert.assertTrue(scanned > 100, "only " + scanned + " source files were scanned");
        Assert.assertTrue(offenders.isEmpty(), "desktop only imports found: " + offenders);
    }

    private static List<String> importLinesOf(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(String::strip)
                .filter(line -> line.startsWith("import "))
                // Static imports of a nested type are still an import of the outer package.
                .map(line -> line.replace("import static ", "import "))
                .toList();
    }

    /** Walks up from the module folder until the aggregator pom that declares the modules is found. */
    private static Path aggregatorRoot() {
        Path folder = Path.of("").toAbsolutePath();
        while (folder != null) {
            final Path pom = folder.resolve("pom.xml");
            if (Files.isRegularFile(pom)) {
                try {
                    final String content = Files.readString(pom, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                    if (content.contains("<module>librodeesher-rules</module>")) {
                        return folder;
                    }
                } catch (final IOException e) {
                    throw new IllegalStateException("Cannot read " + pom, e);
                }
            }
            folder = folder.getParent();
        }
        throw new IllegalStateException("The aggregator pom.xml was not found from " + Path.of("").toAbsolutePath());
    }
}
