package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.skill.Skill;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies {@link SkillMigrationTool} against small fixture files, focusing on the two behaviours
 * that are easy to get wrong: a skill first seen as an "enableSkills" target inside another skill's
 * {@code {...}} still gets its own proper entry (with the right category) once its own line is read,
 * and that same skill is baked in as disabled by default.
 */
@Test(groups = "migration")
public class SkillMigrationToolTest {

    @Test
    public void migratesSkillsAndComputesEnabledByDefault() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-skill-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-skill-migration-target");
        try {
            final Path rolemasterDir = sourceRoot.resolve("rolemaster");
            Files.createDirectories(rolemasterDir);
            Files.writeString(rolemasterDir.resolve("categorias.txt"), String.join("\n",
                    "#Nombre\tCaracterísticas\tProgresión\tHabilidades",
                    "####################",
                    "Artes Marciales·Maniobras(AmM)\tAg/Rp/Ag\tCombinada\t"
                            + "Estilo de la Grulla {Poderes Chi: Ataque Sin Sombra}",
                    "Autocontrol(Autoc)\tAd/Pr/Ad\tEstándar\tPoderes Chi: Ataque Sin Sombra, Xeno-Conocimientos*",
                    ""), StandardCharsets.UTF_8);

            final int written = SkillMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1);

            final List<Skill> skills = readGeneratedFile(targetRoot.resolve("Core/skills.xml"));
            Assert.assertEquals(skills.size(), 3);

            final Skill style = findById(skills, "Estilo de la Grulla");
            Assert.assertEquals(style.getCategoryId(), "Artes Marciales·Maniobras");
            Assert.assertEquals(style.getEnableSkills(), List.of("Poderes Chi: Ataque Sin Sombra"));
            Assert.assertTrue(style.isEnabledByDefault());

            final Skill chiPower = findById(skills, "Poderes Chi: Ataque Sin Sombra");
            Assert.assertEquals(chiPower.getCategoryId(), "Autocontrol", "the skill's own definition line wins the "
                    + "category, not the earlier line that only referenced it as an unlock target");
            Assert.assertFalse(chiPower.isEnabledByDefault(), "referenced as an enableSkills target, so it must "
                    + "start disabled until unlocked");

            final Skill rareSkill = findById(skills, "Xeno-Conocimientos");
            Assert.assertTrue(rareSkill.isRare());
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    private static List<Skill> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(Skill.class).readValue(inputStream);
        }
    }

    private static Skill findById(List<Skill> skills, String id) {
        return skills.stream().filter(skill -> skill.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Skill '" + id + "' not found"));
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
