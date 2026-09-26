package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.race.Race;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Verifies {@link RaceMigrationTool} against a compact but representative race fixture. */
@Test(groups = "migration")
public class RaceMigrationToolTest {

    @Test
    public void migratesRaceSections() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-race-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-race-migration-target");
        try {
            writeLegacyFixtures(sourceRoot);

            final int written = RaceMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1);

            final List<Race> races = readGeneratedFile(targetRoot.resolve("RacesAndCultures/races.xml"));
            Assert.assertEquals(races.size(), 1);

            final Race elf = races.get(0);
            Assert.assertEquals(elf.getId(), "highElf");
            Assert.assertEquals(elf.getName().getSpanish(), "Elfo Alto");
            Assert.assertEquals(elf.getExpectedLifeYears(), Integer.valueOf(500));
            Assert.assertEquals(elf.getAppearanceBonus(), Integer.valueOf(10));
            Assert.assertEquals(elf.getCharacteristicBonuses().get("AGILITY"), Integer.valueOf(2));
            Assert.assertEquals(elf.getResistanceBonuses().get("ESSENCE"), Integer.valueOf(5));
            Assert.assertEquals(elf.getProgressionRankValues().get("physicalDevelopment"), "0/6/4/2/1");
            Assert.assertEquals(elf.getSoulDepartTime(), Integer.valueOf(12));
            Assert.assertEquals(elf.getRaceType(), Integer.valueOf(3));
            Assert.assertEquals(elf.getSize(), com.softwaremagico.librodeesher.race.RaceSize.M);
            Assert.assertEquals(elf.getLanguagePoints(), Integer.valueOf(3));
            Assert.assertEquals(elf.getBackgroundPoints(), Integer.valueOf(5));
            Assert.assertEquals(elf.getRaceLanguages().size(), 2);
            Assert.assertEquals(elf.getOptionalRaceLanguages().size(), 1);
            Assert.assertEquals(elf.getOptionalRaceLanguages().get(0).getStartingSpeakingRanks(), Integer.valueOf(2));
            Assert.assertEquals(elf.getOptionalRaceLanguages().get(0).getStartingWritingRanks(), Integer.valueOf(1));
            Assert.assertEquals(elf.getOptionalRaceLanguages().get(0).getMaxSpeakingRanks(), Integer.valueOf(10));
            Assert.assertEquals(elf.getOptionalRaceLanguages().get(0).getMaxWritingRanks(), Integer.valueOf(6));
            Assert.assertEquals(elf.getBackgroundLanguages().size(), 1);
            Assert.assertEquals(elf.getOptionalBackgroundLanguages().size(), 1);
            Assert.assertEquals(elf.getCommonCategoryIds(), List.of("loreGeneral"));
            Assert.assertEquals(elf.getCommonSkillIds(), List.of("stalking"));
            Assert.assertEquals(elf.getCultureIds(), List.of("rural", "woodland"));
            Assert.assertEquals(elf.getSpecials().get(0).getPoints(), Integer.valueOf(10));
            // The natural armour marker ("Tipo de Armadura 7") is picked up, while words merely
            // containing "ta" (like "hasta 150 m") must not influence it.
            Assert.assertEquals(elf.getNaturalArmorType(), Integer.valueOf(7));
            Assert.assertEquals(elf.getMaleNames(), List.of("Aerendil", "Calen"));
            Assert.assertEquals(elf.getFemaleNames(), List.of("Aerin", "Lúthien"));
            Assert.assertEquals(elf.getFamilyNames(), List.of("Silverbow", "Starleaf"));
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
                "Conocimiento·General(ConGen)\tMe/Ra/Me\tEstándar\tReligión",
                ""), StandardCharsets.UTF_8);

        final Path raceDir = rolemasterDir.resolve("modulos/RazasYCulturas/razas");
        Files.createDirectories(raceDir);
        Files.writeString(raceDir.resolve("Elfo Alto.txt"), String.join("\n",
                "#MODIFICACIÓN A LAS CARACTERÍSTICAS",
                "####################################",
                "Ag\t2", "Co\t0", "Ap\t10", "",
                "# ESPERANZA DE VIDA", "####################################", "500", "",
                "#MODIFICACIÓN A LA TR", "####################################", "Esencia\t5", "Miedo\t-5", "",
                "#PROGRESIÓN", "####################################", "Desarrollo Físico\t0/6/4/2/1", "PP Esencia\t0/7/6/5/4", "",
                "#PROFESIONES RESTRINGIDAS", "####################################", "Ninguna", "",
                "#PARTIDA DEL ALMA", "####################################", "12", "",
                "#TIPO DE RAZA", "####################################", "3", "",
                "#TAMAÑO DE RAZA", "####################################", "Medio", "",
                "#RECUPERACIÓN", "####################################", "1", "",
                "#IDIOMAS", "####################################", "3", "",
                "#HISTORIAL", "####################################", "5", "",
                "#IDIOMAS\tInicial\tMax.Cultura", "####################################", "Élfico Alto\t8/8\t10/10", "Habla Común\t5/0\t8/8",
                "Idioma Racial\t2/1\t10/6", "",
                "#IDIOMAS HISTORIAL", "####################################", "Habla Común\t0/0\t10/10", "Idioma Regional\t0/0\t10/10", "",
                "#HABILIDADES COMUNES", "####################################", "Conocimiento·General, Acechar", "",
                "#HABILIDADES RESTRINGIDAS", "####################################", "Ninguna", "",
                "#CULTURAS", "####################################", "Rural, Silvana", "",
                "#ESPECIALES", "####################################", "Visión nocturna [10]", "Tipo de Armadura 7 [5]", "Visión total hasta 150 m [15]", "",
                "#NOMBRES MASCULINOS", "####################################", "Aerendil, Calen", "",
                "#NOMBRES FEMENINOS", "####################################", "Aerin, Lúthien", "",
                "#APELLIDOS", "####################################", "Silverbow, Starleaf", "",
                "#### FIN RAZA ####"), StandardCharsets.UTF_8);
    }

    private static List<Race> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(Race.class).readValue(inputStream);
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
