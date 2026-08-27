package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.ProfessionBonus;
import com.softwaremagico.librodeesher.profession.ProfessionTrainingCost;
import com.softwaremagico.librodeesher.training.TrainingType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies {@link ProfessionMigrationTool} against a fixture file covering every section: ordered
 * characteristic preferences, magic realms, flat bonuses, the (preserved verbatim) skill/category
 * sections, and per-training costs with a preference marker and a non-magic alternate cost.
 */
@Test(groups = "migration")
public class ProfessionMigrationToolTest {

    @Test
    public void migratesEveryProfessionSection() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-profession-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-profession-migration-target");
        try {
            final Path professionsDir = sourceRoot.resolve("rolemaster/modulos/Basico/profesiones");
            Files.createDirectories(professionsDir);
            Files.writeString(professionsDir.resolve("Mago.txt"), String.join("\n",
                    "#CARACTERÍSTICAS POR ORDEN",
                    "####################################",
                    "Em Ra Ad",
                    "",
                    "#REINOS DE MAGIA",
                    "####################################",
                    "Esencia",
                    "",
                    "#BONIFICACIÓN POR PROFESIÓN",
                    "####################################",
                    "Conocimiento·Mágico\t10",
                    "Desarrollo de Puntos de Poder\t5",
                    "",
                    "#HABILIDADES Y CATEGORÍAS DE HABILIDADES",
                    "####################################",
                    "Armadura·Ligera\t9",
                    "",
                    "# HABILIDADES COMUNES",
                    "####################################",
                    "Sentido del Tiempo, Meditación",
                    "",
                    "#HABILIDADES PROFESIONALES",
                    "####################################",
                    "Ninguna",
                    "",
                    "#HABILIDADES RESTRINGIDAS",
                    "####################################",
                    "Ninguna",
                    "",
                    "#DESARROLLO DE HECHIZOS",
                    "####################################",
                    "Lista Básica (1-5)\t3/3/3",
                    "",
                    "#ADIESTRAMIENTO",
                    "####################################",
                    "Erudito\t19+",
                    "Soldado\t37-\t40",
                    "",
                    "### FIN PROFESION ###",
                    ""), StandardCharsets.UTF_8);

            final int written = ProfessionMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1);

            final List<Profession> professions = readGeneratedFile(targetRoot.resolve("Basico/profesiones.xml"));
            Assert.assertEquals(professions.size(), 1);
            final Profession mago = professions.get(0);

            Assert.assertEquals(mago.getId(), "Mago");
            Assert.assertEquals(mago.getCharacteristicPreferences(), List.of("Em", "Ra", "Ad"));
            Assert.assertFalse(mago.isIndifferentToCharacteristics());
            Assert.assertEquals(mago.getMagicRealms(), List.of("Esencia"));
            Assert.assertTrue(mago.isSpellCaster());

            Assert.assertEquals(mago.getBonuses().size(), 2);
            final ProfessionBonus knowledgeBonus = mago.getBonuses().get(0);
            Assert.assertEquals(knowledgeBonus.getName(), "Conocimiento·Mágico");
            Assert.assertEquals(knowledgeBonus.getBonus(), Integer.valueOf(10));

            Assert.assertTrue(mago.getCategoryCostsRaw().contains("Armadura·Ligera"));
            Assert.assertTrue(mago.getCommonSkillsRaw().contains("Meditación"));
            Assert.assertEquals(mago.getProfessionalSkillsRaw(), "Ninguna");
            Assert.assertTrue(mago.getMagicCostsRaw().contains("Lista Básica"));

            Assert.assertEquals(mago.getTrainingCosts().size(), 2);
            final ProfessionTrainingCost favouredTraining = mago.getTrainingCosts().get(0);
            Assert.assertEquals(favouredTraining.getTrainingName(), "Erudito");
            Assert.assertEquals(favouredTraining.getCost(), Integer.valueOf(19));
            Assert.assertEquals(favouredTraining.getType(), TrainingType.FAVOURITE);

            final ProfessionTrainingCost forbiddenTraining = mago.getTrainingCosts().get(1);
            Assert.assertEquals(forbiddenTraining.getType(), TrainingType.FORBIDDEN);
            Assert.assertEquals(forbiddenTraining.getCost(), Integer.valueOf(37));
            Assert.assertEquals(forbiddenTraining.getCostNotMagic(), Integer.valueOf(40));
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    private static List<Profession> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(Profession.class).readValue(inputStream);
        }
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
