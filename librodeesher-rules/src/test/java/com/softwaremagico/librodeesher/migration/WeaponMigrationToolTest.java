package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.weapon.Weapon;
import com.softwaremagico.librodeesher.weapon.WeaponType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies {@link WeaponMigrationTool} against small fixture files: one weapon type file per module,
 * a rare weapon marker, and a weapon name repeated across two modules (first module wins).
 */
@Test(groups = "migration")
public class WeaponMigrationToolTest {

    @Test
    public void migratesWeaponsAndKeepsFirstModuleOnNameCollision() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-weapon-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-weapon-migration-target");
        try {
            final Path basicoWeapons = sourceRoot.resolve("rolemaster/modulos/Basico/armas");
            Files.createDirectories(basicoWeapons);
            Files.writeString(basicoWeapons.resolve("Filo.txt"), String.join("\n",
                    "#Generalizaciones:",
                    "Daga\tda",
                    "Espada Corta*\tec",
                    ""), StandardCharsets.UTF_8);

            final Path laArmeriaWeapons = sourceRoot.resolve("rolemaster/modulos/LaArmeria/armas");
            Files.createDirectories(laArmeriaWeapons);
            Files.writeString(laArmeriaWeapons.resolve("Filo.txt"), String.join("\n",
                    "Daga\tda",
                    "Espada Ropera\ter",
                    ""), StandardCharsets.UTF_8);

            final int written = WeaponMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 2);

            final List<Weapon> basicoWeaponList = readGeneratedFile(targetRoot.resolve("Core/weapons.xml"));
            Assert.assertEquals(basicoWeaponList.size(), 2);

            final Weapon dagger = findById(basicoWeaponList, "Daga");
            Assert.assertEquals(dagger.getAbbreviation(), "da");
            Assert.assertEquals(dagger.getType(), WeaponType.EDGE);
            Assert.assertEquals(dagger.getCategoryId(), "Armas·Filo");
            Assert.assertFalse(dagger.isRare());

            final Weapon shortSword = findById(basicoWeaponList, "Espada Corta");
            Assert.assertTrue(shortSword.isRare());

            final List<Weapon> laArmeriaWeaponList = readGeneratedFile(targetRoot.resolve("TheArmory/weapons.xml"));
            Assert.assertEquals(laArmeriaWeaponList.size(), 1, "'Daga' was already defined by 'Basico'");
            Assert.assertEquals(laArmeriaWeaponList.get(0).getId(), "Espada Ropera");
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    private static List<Weapon> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(Weapon.class).readValue(inputStream);
        }
    }

    private static Weapon findById(List<Weapon> weapons, String id) {
        return weapons.stream().filter(weapon -> weapon.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Weapon '" + id + "' not found"));
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
