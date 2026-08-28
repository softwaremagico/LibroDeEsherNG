package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
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
            Files.createDirectories(sourceRoot.resolve("rolemaster"));
            Files.writeString(sourceRoot.resolve("rolemaster/categorias.txt"), String.join("\n",
                    "Armadura·Ligera(ArdL)\tAg/Fu/Ag\tEstándar\tCuero Blando",
                    ""), StandardCharsets.UTF_8);

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

            final List<Profession> professions = readGeneratedFile(targetRoot.resolve("Core/professions.xml"));
            Assert.assertEquals(professions.size(), 1);
            final Profession mago = professions.get(0);

            Assert.assertEquals(mago.getId(), "wizard");
            Assert.assertEquals(mago.getCharacteristicPreferences(),
                    List.of(CharacteristicAbbreviation.EMPATHY, CharacteristicAbbreviation.REASONING, CharacteristicAbbreviation.SELF_DISCIPLINE));
            Assert.assertFalse(mago.isIndifferentToCharacteristics());
            Assert.assertEquals(mago.getMagicRealms().size(), 1);
            Assert.assertEquals(mago.getMagicRealms().get(0).getOptions(), List.of(RealmOfMagic.ESSENCE));
            Assert.assertFalse(mago.getMagicRealms().get(0).isChoice());
            Assert.assertTrue(mago.isSpellCaster());

            Assert.assertEquals(mago.getBonuses().size(), 2);
            final ProfessionBonus knowledgeBonus = mago.getBonuses().get(0);
            Assert.assertEquals(knowledgeBonus.getName(), "loreArcane");
            Assert.assertEquals(knowledgeBonus.getBonus(), Integer.valueOf(10));

            Assert.assertEquals(mago.getCategoryCosts().size(), 1);
            Assert.assertEquals(mago.getCategoryCost("armorLight").getRankCosts(), List.of(9));
            Assert.assertNull(mago.getCategoryCost("armorMiddle"));
            Assert.assertTrue(mago.getWeaponCategoryCostTiers().isEmpty());
            Assert.assertEquals(mago.getCommonSkillIds(), List.of("senseOfTiempo", "meditation"));
            Assert.assertTrue(mago.getCommonSkillChoices().isEmpty());
            Assert.assertTrue(mago.getProfessionalSkillIds().isEmpty());
            Assert.assertTrue(mago.getProfessionalSkillChoices().isEmpty());
            Assert.assertTrue(mago.getMagicCostsRaw().contains("Lista Básica"));

            Assert.assertEquals(mago.getTrainingCosts().size(), 2);
            final ProfessionTrainingCost favouredTraining = mago.getTrainingCosts().get(0);
            Assert.assertEquals(favouredTraining.getTrainingName(), "scholar");
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

    @Test
    public void migratesSkillChoiceSyntax() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-profession-choice-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-profession-choice-target");
        try {
            Files.createDirectories(sourceRoot.resolve("rolemaster"));
            Files.writeString(sourceRoot.resolve("rolemaster/categorias.txt"), String.join("\n",
                    "Oficios(Ofi)\tAg/Ra/Ag\tEstándar\tCocina, Costura",
                    ""), StandardCharsets.UTF_8);

            final Path professionsDir = sourceRoot.resolve("rolemaster/modulos/Basico/profesiones");
            Files.createDirectories(professionsDir);
            Files.writeString(professionsDir.resolve("Explorador.txt"), String.join("\n",
                    "#CARACTERÍSTICAS POR ORDEN",
                    "####################################",
                    "Indiferente",
                    "",
                    "#REINOS DE MAGIA",
                    "####################################",
                    "Esencia",
                    "",
                    "#BONIFICACIÓN POR PROFESIÓN",
                    "####################################",
                    "Oficios\t5",
                    "",
                    "#HABILIDADES Y CATEGORÍAS DE HABILIDADES",
                    "####################################",
                    "Oficios\t2/5",
                    "",
                    "# HABILIDADES COMUNES",
                    "####################################",
                    "Sentido del Tiempo, Oficios#1, {Boxeo; Lucha Libre}#1, {Percepción del Entorno}#1",
                    "",
                    "#HABILIDADES PROFESIONALES",
                    "####################################",
                    "{Montar; Forrajear; Predicción del Clima}",
                    "",
                    "#HABILIDADES RESTRINGIDAS",
                    "####################################",
                    "Ninguna",
                    "",
                    "#DESARROLLO DE HECHIZOS",
                    "####################################",
                    "",
                    "#ADIESTRAMIENTO",
                    "####################################",
                    "",
                    "### FIN PROFESION ###",
                    ""), StandardCharsets.UTF_8);

            final int written = ProfessionMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1);

            final Profession explorador = readGeneratedFile(targetRoot.resolve("Core/professions.xml")).get(0);

            Assert.assertEquals(explorador.getCommonSkillIds(), List.of("senseOfTiempo"));
            Assert.assertEquals(explorador.getCommonSkillChoices().size(), 3);
            Assert.assertEquals(explorador.getCommonSkillChoices().get(0).getCategoryId(), "crafts");
            Assert.assertEquals(explorador.getCommonSkillChoices().get(0).getRanksToChoose(), Integer.valueOf(1));
            Assert.assertEquals(explorador.getCommonSkillChoices().get(1).getSkillOptions(), List.of("boxeo", "luchaLibre"));
            Assert.assertEquals(explorador.getCommonSkillChoices().get(2).getSkillOptions(),
                    List.of("perceptionOfTheEnvironmentCiudades", "perceptionOfTheEnvironmentCombat",
                            "perceptionOfTheEnvironmentDurmiendo", "perceptionOfTheEnvironmentExploration"));

            Assert.assertTrue(explorador.getProfessionalSkillIds().isEmpty());
            Assert.assertEquals(explorador.getProfessionalSkillChoices().size(), 1);
            Assert.assertEquals(explorador.getProfessionalSkillChoices().get(0).getRanksToChoose(), Integer.valueOf(1));
            Assert.assertEquals(explorador.getProfessionalSkillChoices().get(0).getSkillOptions(),
                    List.of("montar", "forrajear", "predictionOfTheClima"));
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
