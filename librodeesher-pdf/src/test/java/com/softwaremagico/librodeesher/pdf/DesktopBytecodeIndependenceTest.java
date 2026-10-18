package com.softwaremagico.librodeesher.pdf;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * The compiled library has to stay loadable on Android, where the desktop-only parts of
 * {@code java.awt} and Swing are missing.
 *
 * <p>{@link DesktopIndependenceTest} only reads the sources, which a {@code import} removal could
 * satisfy while the reference survives in the bytecode (a fully qualified name, a static import of a
 * nested type, or a type that leaks through a third-party signature). This test scans class names
 * and descriptors in the constant pool of every compiled class, so the guarantee covers what is
 * actually shipped.
 * </p>
 *
 * <p>No exception is allowed: OpenPDF's background colour API leaks an AWT type into callers'
 * bytecode, so table fills use the gray-scale API instead.</p>
 */
@Test(groups = "pdf")
public class DesktopBytecodeIndependenceTest {

    /** Desktop-only packages, as the internal names a class file uses. */
    private static final List<String> FORBIDDEN = List.of(
            "java/awt/", "javax/swing/", "java/beans/", "javax/imageio/");

    @Test
    public void noCompiledClassReferencesADesktopOnlyPackage() throws IOException {
        final Path root = aggregatorRoot();
        final List<String> offenders = new ArrayList<>();
        int scanned = 0;

        for (final Path classesFolder : List.of(root.resolve("librodeesher-rules/target/classes"),
                root.resolve("librodeesher-random/target/classes"), root.resolve("librodeesher-pdf/target/classes"))) {
            Assert.assertTrue(Files.isDirectory(classesFolder), "missing compiled folder " + classesFolder
                    + " (the module has to be compiled before this test runs)");
            try (Stream<Path> files = Files.walk(classesFolder)) {
                for (final Path file : files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".class")).toList()) {
                    scanned++;
                    final String className = root.relativize(file).toString();
                    for (final String referenced : desktopReferencesIn(file)) {
                        offenders.add(className + " references " + referenced.replace('/', '.'));
                    }
                }
            }
        }

        Assert.assertTrue(scanned > 100, "only " + scanned + " compiled classes were scanned");
        Assert.assertTrue(offenders.isEmpty(), "desktop only references found in the bytecode: " + offenders);
    }

    /**
     * Every internal type name in the class file's constant pool that lives in a desktop-only
     * package. Only {@code CONSTANT_Class} entries name types, so a UTF-8 entry that merely spells a
     * package (a message, a debug string) is not reported.
     */
    private static List<String> desktopReferencesIn(Path classFile) throws IOException {
        try (InputStream input = Files.newInputStream(classFile);
             DataInputStream data = new DataInputStream(new BufferedInputStream(input))) {
            if (data.readInt() != 0xCAFEBABE) {
                throw new IOException("not a class file: " + classFile);
            }
            data.readUnsignedShort();
            data.readUnsignedShort();
            final int constantPoolCount = data.readUnsignedShort();
            final String[] utf8 = new String[constantPoolCount];
            final int[] classNameIndexes = new int[constantPoolCount];

            for (int index = 1; index < constantPoolCount; index++) {
                final int tag = data.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf8[index] = data.readUTF();
                    case 7 -> classNameIndexes[index] = data.readUnsignedShort();
                    case 8, 16, 19, 20 -> data.readUnsignedShort();
                    case 15 -> {
                        data.readUnsignedByte();
                        data.readUnsignedShort();
                    }
                    case 3, 4, 9, 10, 11, 12, 17, 18 -> data.readInt();
                    case 5, 6 -> {
                        data.readLong();
                        // Eight byte constants take two constant pool slots.
                        index++;
                    }
                    default -> throw new IOException("unknown constant pool tag " + tag + " in " + classFile);
                }
            }

            final List<String> references = new ArrayList<>();
            for (final int nameIndex : classNameIndexes) {
                if (nameIndex == 0) {
                    continue;
                }
                final String name = utf8[nameIndex];
                if (name == null) {
                    continue;
                }
                if (FORBIDDEN.stream().anyMatch(name::startsWith)) {
                    references.add(name);
                }
            }
            for (final String name : utf8) {
                if (name != null && FORBIDDEN.stream().anyMatch(name::contains) && !references.contains(name)) {
                    references.add(name);
                }
            }
            return references;
        }
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
