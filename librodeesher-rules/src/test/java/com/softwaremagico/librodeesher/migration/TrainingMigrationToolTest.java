package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.training.ChoiceGroup;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;
import com.softwaremagico.librodeesher.training.TrainingSpecialItem;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies {@link TrainingMigrationTool} against a fixture file covering every section of the legacy
 * format: training time, race restriction, special items, category/skill ranks granted (including a
 * choice of categories and a choice of skills), a characteristic upgrade choice group, a professional
 * requirement and the four skill sections.
 */
@Test(groups = "migration")
public class TrainingMigrationToolTest {

    @Test
    public void migratesEveryTrainingSection() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-training-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-training-migration-target");
        try {
            final Path trainingsDir = sourceRoot.resolve("rolemaster/modulos/Basico/adiestramientos");
            Files.createDirectories(trainingsDir);
            Files.writeString(trainingsDir.resolve("Explorador.txt"), String.join("\n",
                    "# TIEMPO (meses)",
                    "####################################",
                    "12",
                    "",
                    "# EXCLUSIVO RAZA",
                    "####################################",
                    "Ninguno",
                    "",
                    "#ESPECIAL",
                    "####################################",
                    "Arma cuerpo a cuerpo\t30\t10\tArma Cuerpo a Cuerpo",
                    "Amigos\t20",
                    "",
                    "#HABILIDADES",
                    "####################################",
                    "Rastrear\t2\t1\t1\t2",
                    "  *  Rastrear\t2",
                    "{Armas·2manos; Armas·Filo}\t1\t1\t1\t3",
                    "  *  {Espada; Hacha}\t-3",
                    "",
                    "# AUMENTOS CARACTERÍSTICAS",
                    "####################################",
                    "{Ag; Ra}",
                    "",
                    "# REQUISITOS PROFESIONALES",
                    "####################################",
                    "Religión (10) (-3)",
                    "",
                    "#HABILIDADES DE ESTILO DE VIDA",
                    "####################################",
                    "Ninguna",
                    "",
                    "#HABILIDADES COMUNES",
                    "####################################",
                    "Cazar, {Rastrear; Acechar}",
                    "",
                    "#HABILIDADES PROFESIONALES",
                    "####################################",
                    "Ninguna",
                    "",
                    "#HABILIDADES RESTRINGIDAS",
                    "####################################",
                    "Ninguna",
                    "",
                    "### FIN ADIESTRAMIENTO ###",
                    ""), StandardCharsets.UTF_8);

            final int written = TrainingMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1);

            final List<Training> trainings = readGeneratedFile(targetRoot.resolve("Core/trainings.xml"));
            Assert.assertEquals(trainings.size(), 1);
            final Training explorador = trainings.get(0);

            Assert.assertEquals(explorador.getId(), "Explorador");
            Assert.assertEquals(explorador.getTrainingTimeInMonths(), Integer.valueOf(12));
            Assert.assertTrue(explorador.isAvailableToEveryRace());

            Assert.assertEquals(explorador.getSpecialItems().size(), 2);
            final TrainingSpecialItem meleeWeapon = explorador.getSpecialItems().get(0);
            Assert.assertEquals(meleeWeapon.getName(), "Arma cuerpo a cuerpo");
            Assert.assertEquals(meleeWeapon.getProbability(), Integer.valueOf(30));
            Assert.assertEquals(meleeWeapon.getBonus(), Integer.valueOf(10));
            Assert.assertEquals(meleeWeapon.getSkillName(), "Arma Cuerpo a Cuerpo");
            final TrainingSpecialItem friends = explorador.getSpecialItems().get(1);
            Assert.assertNull(friends.getBonus());
            Assert.assertNull(friends.getSkillName());

            Assert.assertEquals(explorador.getCategories().size(), 2);
            final TrainingCategoryGrant trackingCategory = explorador.getCategories().get(0);
            Assert.assertEquals(trackingCategory.getCategoryOptions(), List.of("Rastrear"));
            Assert.assertFalse(trackingCategory.isChoice());
            Assert.assertEquals(trackingCategory.getRanksGranted(), Integer.valueOf(2));
            Assert.assertEquals(trackingCategory.getMinSkills(), Integer.valueOf(1));
            Assert.assertEquals(trackingCategory.getMaxSkills(), Integer.valueOf(1));
            Assert.assertEquals(trackingCategory.getRanksToDistribute(), Integer.valueOf(2));
            Assert.assertEquals(trackingCategory.getSkills().size(), 1);
            final TrainingSkillGrant trackingSkill = trackingCategory.getSkills().get(0);
            Assert.assertEquals(trackingSkill.getSkillOptions(), List.of("Rastrear"));
            Assert.assertFalse(trackingSkill.isChoice());
            Assert.assertEquals(trackingSkill.getRanksToDistribute(), Integer.valueOf(2));

            final TrainingCategoryGrant weaponCategory = explorador.getCategories().get(1);
            Assert.assertEquals(weaponCategory.getCategoryOptions(), List.of("Armas·2manos", "Armas·Filo"));
            Assert.assertTrue(weaponCategory.isChoice());
            Assert.assertEquals(weaponCategory.getRanksGranted(), Integer.valueOf(1));
            Assert.assertEquals(weaponCategory.getRanksToDistribute(), Integer.valueOf(3));
            final TrainingSkillGrant weaponSkillChoice = weaponCategory.getSkills().get(0);
            Assert.assertEquals(weaponSkillChoice.getSkillOptions(), List.of("Espada", "Hacha"));
            Assert.assertTrue(weaponSkillChoice.isChoice());
            Assert.assertEquals(weaponSkillChoice.getRanksToDistribute(), Integer.valueOf(3));

            Assert.assertEquals(explorador.getCharacteristicUpgrades().size(), 1);
            Assert.assertEquals(explorador.getCharacteristicUpgrades().get(0).getOptions(), List.of("Ag", "Ra"));
            Assert.assertFalse(explorador.getCharacteristicUpgrades().get(0).isFixed());

            Assert.assertEquals(explorador.getRequirements().size(), 1);
            Assert.assertEquals(explorador.getRequirements().get(0).getName(), "Religión");
            Assert.assertEquals(explorador.getRequirements().get(0).getValue(), Integer.valueOf(10));
            Assert.assertEquals(explorador.getRequirements().get(0).getCostModification(), Integer.valueOf(-3));

            Assert.assertTrue(explorador.getLifeSkills().isEmpty());
            final List<ChoiceGroup> commonSkills = explorador.getCommonSkills();
            Assert.assertEquals(commonSkills.size(), 2);
            Assert.assertTrue(commonSkills.get(0).isFixed());
            Assert.assertEquals(commonSkills.get(0).getOptions(), List.of("Cazar"));
            Assert.assertEquals(commonSkills.get(1).getOptions(), List.of("Rastrear", "Acechar"));

            Assert.assertTrue(explorador.getProfessionCosts().isEmpty());
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    private static List<Training> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(Training.class).readValue(inputStream);
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
