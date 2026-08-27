package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies {@link CategoryMigrationTool} against small, hand-written fixture files that reproduce the
 * two tricky behaviours of the legacy format: parsing "Nombre(Abreviatura)" and merging skills that a
 * second module contributes to a category already defined by an earlier one. Uses a JVM temporary
 * directory so it never depends on (or mutates) the real "modulo" data.
 */
@Test(groups = "migration")
public class CategoryMigrationToolTest {

    @Test
    public void migratesAndMergesCategoriesAcrossModules() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-migration-target");
        try {
            writeLegacyFixtures(sourceRoot);

            final int written = CategoryMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 2, "expected one categorias.xml for 'Basico' and one for 'Esencia'");

            final List<Category> basicoCategories = readGeneratedFile(targetRoot.resolve("Core/categories.xml"));
            Assert.assertEquals(basicoCategories.size(), 2);

            final Category armaduraLigera = findById(basicoCategories, "ArmaduraLigera");
            Assert.assertEquals(armaduraLigera.getAbbreviation(), "ArdL");
            Assert.assertEquals(armaduraLigera.getCharacteristics(), List.of("Ag", "Fu", "Ag"));
            Assert.assertEquals(armaduraLigera.getType(), CategoryType.STANDARD);
            Assert.assertEquals(armaduraLigera.getSkills(), List.of("Cuero Endurecido", "Cuero Blando"));

            final Category ataquesEspeciales = findById(basicoCategories, "AtaquesEspeciales");
            Assert.assertEquals(ataquesEspeciales.getType(), CategoryType.COMBINED);
            // The base "AtaquesEspeciales" skill plus the one contributed later by the "Esencia" module.
            Assert.assertEquals(ataquesEspeciales.getSkills(), List.of("Pelea", "Ataque Mágico"));

            final List<Category> esenciaCategories = readGeneratedFile(targetRoot.resolve("Essence/categories.xml"));
            Assert.assertEquals(esenciaCategories.size(), 1, "'AtaquesEspeciales' was merged into Basico, "
                    + "only the category first defined by Esencia should remain here");
            Assert.assertEquals(esenciaCategories.get(0).getId(), "ConocimientoMagico");
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    private static void writeLegacyFixtures(Path sourceRoot) throws IOException {
        final Path rolemasterDir = sourceRoot.resolve("rolemaster");
        Files.createDirectories(rolemasterDir);
        Files.writeString(rolemasterDir.resolve("categorias.txt"), String.join("\n",
                "#Nombre\tCaracterísticas\tProgresión\tHabilidades",
                "####################",
                "ArmaduraLigera(ArdL)\tAg/Fu/Ag\tEstándar\tCuero Endurecido, Cuero Blando",
                "AtaquesEspeciales(AtaEsp)\tFu/Ag/Ad\tCombinada\tPelea",
                ""), StandardCharsets.UTF_8);

        final Path esenciaDir = rolemasterDir.resolve("modulos").resolve("Esencia");
        Files.createDirectories(esenciaDir);
        Files.writeString(esenciaDir.resolve("categorias.txt"), String.join("\n",
                "#Nombre\tCaracterísticas\tProgresión\tHabilidades",
                "####################",
                // Adds a skill to a category already defined in the base file: must be merged into
                // Basico's category, not duplicated as a new "Esencia" category.
                "AtaquesEspeciales(AtaEsp)\tFu/Ag/Ad\tCombinada\tAtaque Mágico",
                "ConocimientoMagico(ConMag)\tMe/Ra/Me\tEstándar\tConocimiento de los Hechizos",
                ""), StandardCharsets.UTF_8);
    }

    private static List<Category> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(Category.class).readValue(inputStream);
        }
    }

    private static Category findById(List<Category> categories, String id) {
        return categories.stream().filter(category -> category.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Category '" + id + "' not found"));
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (final IOException e) {
                    // Best effort cleanup of a temporary test directory.
                }
            });
        }
    }
}
