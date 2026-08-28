package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.culture.Culture;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Verifies {@link CultureMigrationTool} against a compact but representative culture fixture. */
@Test(groups = "migration")
public class CultureMigrationToolTest {

    @Test
    public void migratesCultureSections() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-culture-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-culture-migration-target");
        try {
            final Path cultureDir = sourceRoot.resolve("rolemaster/modulos/RazasYCulturas/culturas");
            Files.createDirectories(cultureDir);
            Files.writeString(sourceRoot.resolve("rolemaster/categorias.txt"), String.join("\n",
                    "Exteriores·Entorno(ExtE)\tAg/In\tEstándar\tRastrear, Acechar",
                    ""), StandardCharsets.UTF_8);
            Files.writeString(cultureDir.resolve("Rural.txt"), String.join("\n",
                    "# ARMAS TÍPICAS", "####################", "Filo, Arco Largo", "",
                    "# ARMADURAS TÍPICAS", "####################", "Armadura Tipo I, Armadura Tipo II", "",
                    "# RANGOS", "####################", "Exteriores·Entorno\t3", "  *  Rastrear\t2", "",
                    "# PUNTOS AFICIONES", "####################", "10", "",
                    "# AFICIONES", "####################", "Rastrear, -Acechar", "",
                    "# IDIOMAS", "####################", "Habla Común\t8/8", "Idioma Regional\t6/4", "",
                    "# ADIESTAMIENTOS", "####################", "Soldado\t50%", "",
                    "### FIN CULTURA ###"), StandardCharsets.UTF_8);

            final int written = CultureMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1);

            final List<Culture> cultures = readGeneratedFile(targetRoot.resolve("RacesAndCultures/cultures.xml"));
            Assert.assertEquals(cultures.size(), 1);
            final Culture rural = cultures.get(0);

            Assert.assertEquals(rural.getId(), "rural");
            Assert.assertEquals(rural.getName().getSpanish(), "Rural");
            Assert.assertEquals(rural.getTypicalWeaponIds(), List.of("edged", "longBow"));
            Assert.assertEquals(rural.getTypicalArmorIds(), List.of("armorTipoI", "armorTipoIi"));
            Assert.assertEquals(rural.getAdolescenceRanks().size(), 1);
            Assert.assertEquals(rural.getAdolescenceRanks().get(0).getCategoryOptions(), List.of("outdoorEnvironment"));
            Assert.assertEquals(rural.getHobbyRanks(), Integer.valueOf(10));
            Assert.assertEquals(rural.getHobbyIds(), List.of("tracking", "exclude:stalking"));
            Assert.assertEquals(rural.getLanguageMaxRanks().get(0).getLanguageId(), "commonSpeech");
            Assert.assertEquals(rural.getLanguageMaxRanks().get(0).getMaxSpeakingRanks(), Integer.valueOf(8));
            Assert.assertEquals(rural.getOptionalLanguages().size(), 1);
            Assert.assertEquals(rural.getOptionalLanguages().get(0).getMaxSpeakingRanks(), Integer.valueOf(6));
            Assert.assertEquals(rural.getOptionalLanguages().get(0).getMaxWritingRanks(), Integer.valueOf(4));
            Assert.assertEquals(rural.getTrainingPrices().get(0).getTrainingId(), "soldier");
            Assert.assertEquals(rural.getTrainingPrices().get(0).getPrice(), Double.valueOf(0.5d));
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    private static List<Culture> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(Culture.class).readValue(inputStream);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                    // Best effort cleanup.
                }
            });
        }
    }
}
