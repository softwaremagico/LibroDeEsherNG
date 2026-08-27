package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.weapon.Weapon;
import com.softwaremagico.librodeesher.weapon.WeaponType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * One-shot command line tool that converts the legacy {@code armas/<Tipo>.txt} files (one per
 * {@link WeaponType}, each a plain "Nombre\tAbreviatura" list) into a single {@code armas.xml} per
 * module, read by {@link com.softwaremagico.librodeesher.weapon.WeaponFactory}.
 *
 * <p>Like categories and skills, a weapon name is a global identity: if the same name is defined by
 * more than one module (e.g. a base weapon re-listed by an expansion), the first module to define it
 * (in {@link ModuleManager#getAllModules()} order) keeps it.</p>
 */
public final class WeaponMigrationTool {

    private static final String WEAPONS_FOLDER = "armas";
    private static final String OUTPUT_FILE = "armas.xml";

    private WeaponMigrationTool() {
        // Utility class.
    }

    public static void main(String[] args) throws IOException {
        final Path sourceRoot = Path.of(args.length > 0 ? args[0] : "../../LibroDeEsher");
        final Path modulesTarget = Path.of(args.length > 1 ? args[1] : "../modulo");
        final int written = migrate(sourceRoot, modulesTarget);
        System.out.println("Wrote " + written + " '" + OUTPUT_FILE + "' file(s) under " + modulesTarget.toAbsolutePath());
    }

    public static int migrate(Path sourceRoot, Path modulesTarget) throws IOException {
        final Path modulosDir = sourceRoot.resolve("rolemaster").resolve("modulos");

        final Map<String, Weapon> weaponsById = new LinkedHashMap<>();
        final Map<String, List<Weapon>> weaponsByModule = new LinkedHashMap<>();

        for (final String module : ModuleManager.getAllModules()) {
            final Path weaponsDir = modulosDir.resolve(module).resolve(WEAPONS_FOLDER);
            if (!Files.isDirectory(weaponsDir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(weaponsDir)) {
                for (final Path file : files.filter(path -> path.toString().endsWith(".txt"))
                        .filter(LegacyFileFilters::isRealDataFile).sorted().toList()) {
                    readWeaponsFile(file, module, weaponsById, weaponsByModule);
                }
            }
        }

        int written = 0;
        for (final Map.Entry<String, List<Weapon>> entry : weaponsByModule.entrySet()) {
            XmlMigrationWriter.write(modulesTarget.resolve(entry.getKey()).resolve(OUTPUT_FILE),
                    "armas", "arma", entry.getValue());
            written++;
        }
        return written;
    }

    private static void readWeaponsFile(Path file, String module, Map<String, Weapon> weaponsById,
                                         Map<String, List<Weapon>> weaponsByModule) throws IOException {
        final String fileName = file.getFileName().toString();
        final WeaponType type = WeaponType.fromTag(fileName.substring(0, fileName.length() - ".txt".length()));

        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            final String[] columns = line.split("\t");
            if (columns.length < 2) {
                continue;
            }
            final boolean rare = columns[0].contains("*");
            final String name = columns[0].replace("*", "").trim();
            if (weaponsById.containsKey(name)) {
                continue;
            }

            final Weapon weapon = new Weapon(name);
            weapon.setName(name);
            weapon.setType(type);
            weapon.setAbbreviation(columns[1].trim());
            weapon.setRare(rare);

            weaponsById.put(name, weapon);
            weaponsByModule.computeIfAbsent(module, key -> new ArrayList<>()).add(weapon);
        }
    }
}
