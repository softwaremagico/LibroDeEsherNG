package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.magic.MagicSpellList;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies {@link MagicMigrationTool} against fixture files covering: a plain single-owner list, a
 * multi-owner ("/"-separated) list, the open/closed pseudo-owner tags, and a cross-module owner merge
 * for the same (realm, list name) pair.
 */
@Test(groups = "migration")
public class MagicMigrationToolTest {

    @Test
    public void migratesSpellListsAndMergesOwnersAcrossModules() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-magic-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-magic-migration-target");
        try {
            final Path esenciaDir = sourceRoot.resolve("rolemaster/modulos/Esencia/hechizos");
            Files.createDirectories(esenciaDir);
            Files.writeString(esenciaDir.resolve("Esencia.txt"), String.join("\n",
                    "Barrera Contra Hechizos\tLista Abierta",
                    "Bridas de los Hechizos\tLista Cerrada",
                    "Ley del Fuego\tMago/Mago del Fuego",
                    ""), StandardCharsets.UTF_8);

            final Path guiaTesorosDir = sourceRoot.resolve("rolemaster/modulos/GuiaTesoros/hechizos");
            Files.createDirectories(guiaTesorosDir);
            Files.writeString(guiaTesorosDir.resolve("Esencia.txt"), String.join("\n",
                    // Grants an extra owner to a list already defined by "Esencia": must merge.
                    "Ley del Fuego\tHechicero de Fuego",
                    ""), StandardCharsets.UTF_8);

            final int written = MagicMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1, "the list was already defined by 'Esencia', so 'GuiaTesoros' "
                    + "contributes no new list of its own");

            final List<MagicSpellList> lists = readGeneratedFile(targetRoot.resolve("Essence/spells.xml"));
            Assert.assertEquals(lists.size(), 3);

            final MagicSpellList openList = findByName(lists, "Barrera Contra Hechizos");
            Assert.assertTrue(openList.isOpenList());
            Assert.assertFalse(openList.isClosedList());
            Assert.assertEquals(openList.getRealm(), RealmOfMagic.ESSENCE);
            Assert.assertEquals(openList.getId(), "essenceBarrierAgainstSpells");

            final MagicSpellList closedList = findByName(lists, "Bridas de los Hechizos");
            Assert.assertTrue(closedList.isClosedList());

            final MagicSpellList fireLaw = findByName(lists, "Ley del Fuego");
            Assert.assertEquals(fireLaw.getOwners(), List.of("Wizard", "Fire Wizard", "Fire Sorcerer"));
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    private static List<MagicSpellList> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(MagicSpellList.class).readValue(inputStream);
        }
    }

    private static MagicSpellList findByName(List<MagicSpellList> lists, String name) {
        return lists.stream().filter(list -> list.getName().getSpanish().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("Spell list '" + name + "' not found"));
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
