package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkGrade;
import com.softwaremagico.librodeesher.perk.PerkType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies {@link PerkMigrationTool} against a small fixture file reproducing the tricky bits of the
 * real data: a weakness (negative cost), a swapped grade/type column pair (a real data typo present
 * in "ManualPersonajes"), and a row whose description was accidentally split by a stray tab.
 */
@Test(groups = "migration")
public class PerkMigrationToolTest {

    @Test
    public void migratesPerksForEveryModuleThatHasThem() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-perk-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-perk-migration-target");
        try {
            final Path perksDir = sourceRoot.resolve("rolemaster").resolve("modulos")
                    .resolve("ManualPersonajes").resolve("talentos");
            Files.createDirectories(perksDir);
            Files.writeString(perksDir.resolve("talentos.txt"), String.join("\n",
                    "# Nombre\tCoste\tPermitido\tGrado\tTipo\tBonus\tDescripcion",
                    "#########\t#########\t#########\t#######\t#########\t#########\t#########",
                    "Suerte\t30\tTodos\tMáximo\tCapacidad Especial\tNinguno\tTiradas abiertas a 93-100.",
                    "Adicción Ligera\t-20\tTodos\tMáximo\tMental\tNinguno\tConsulta la página 90.",
                    // Swapped grade/type columns, exactly as found in the real data.
                    "Promesa (Máximo)\t-20\tTodos\tMental\tMáximo\tNinguno\tUna promesa.",
                    // Description accidentally split across two tab-separated columns.
                    "Duplicación\t30\tTodos\tMáximo\tCapacidad Especial\tNinguno\tGenera una copia.\tConsulta la página 70.",
                    ""), StandardCharsets.UTF_8);

            final int written = PerkMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1);

            final List<Perk> perks = readGeneratedFile(targetRoot.resolve("ManualPersonajes/talentos.xml"));
            Assert.assertEquals(perks.size(), 4);

            final Perk suerte = findById(perks, "Suerte");
            Assert.assertEquals(suerte.getCost(), Integer.valueOf(30));
            Assert.assertFalse(suerte.isWeakness());
            Assert.assertTrue(suerte.isAvailableToEveryone());
            Assert.assertEquals(suerte.getGrade(), PerkGrade.MAXIMUM);
            Assert.assertEquals(suerte.getType(), PerkType.SPECIAL);

            final Perk addiction = findById(perks, "Adicción Ligera");
            Assert.assertTrue(addiction.isWeakness());
            Assert.assertEquals(addiction.getType(), PerkType.MENTAL);

            // Legacy behaviour preserved: an unrecognized grade tag ("Mental") falls back to MAXIMUM,
            // and an unrecognized type tag ("Máximo") falls back to OTHER.
            final Perk swappedColumns = findById(perks, "Promesa (Máximo)");
            Assert.assertEquals(swappedColumns.getGrade(), PerkGrade.MAXIMUM);
            Assert.assertEquals(swappedColumns.getType(), PerkType.OTHER);

            final Perk duplication = findById(perks, "Duplicación");
            Assert.assertEquals(duplication.getDescription(), "Genera una copia. Consulta la página 70.");
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    private static List<Perk> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(Perk.class).readValue(inputStream);
        }
    }

    private static Perk findById(List<Perk> perks, String id) {
        return perks.stream().filter(perk -> perk.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Perk '" + id + "' not found"));
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
