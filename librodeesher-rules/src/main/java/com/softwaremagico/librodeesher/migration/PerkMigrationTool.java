package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkGrade;
import com.softwaremagico.librodeesher.perk.PerkType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One-shot command line tool that converts the legacy tab-separated {@code talentos/talentos.txt}
 * files into the {@code talentos.xml} files read by {@link com.softwaremagico.librodeesher.perk.PerkFactory}.
 *
 * <h2>Legacy file format</h2>
 * Each non-comment line has 7 tab-separated columns:
 * <pre>Nombre\tCoste\tPermitido\tGrado\tTipo\tBonuses\tDescripcion</pre>
 *
 * <p>Unlike categories and skills, perks do not need any cross-module identity merging: each module's
 * {@code talentos.txt} is self-contained, so this tool simply converts one file into one
 * {@code talentos.xml} per module (only "ManualPersonajes", "NoOficiales" and "RazasSubterraneas"
 * define any perks in the original data).</p>
 */
public final class PerkMigrationTool {

    private static final String PERKS_FOLDER = "talentos";
    private static final String PERKS_FILE = "talentos.txt";
    private static final String OUTPUT_FILE = "talentos.xml";

    private PerkMigrationTool() {
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

        int written = 0;
        for (final String module : ModuleManager.getAllModules()) {
            final Path file = modulosDir.resolve(module).resolve(PERKS_FOLDER).resolve(PERKS_FILE);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            final List<Perk> perks = readPerksFile(file);
            XmlMigrationWriter.write(modulesTarget.resolve(module).resolve(OUTPUT_FILE), "talentos", "talento", perks);
            written++;
        }
        return written;
    }

    private static List<Perk> readPerksFile(Path file) throws IOException {
        final List<Perk> perks = new ArrayList<>();
        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            // "-1" keeps a trailing empty "Descripcion" column instead of dropping it.
            final String[] columns = line.split("\t", -1);
            if (columns.length < 7) {
                throw new IllegalStateException(
                        "Malformed perk line in '" + file + "': expected 7 tab-separated columns, got "
                                + columns.length + ": " + line);
            }

            final Perk perk = new Perk(columns[0].trim());
            perk.setName(columns[0].trim());
            perk.setCost(Integer.valueOf(columns[1].trim()));
            perk.setAvailableTo(parseAvailableTo(columns[2]));
            perk.setGrade(PerkGrade.fromTag(columns[3].trim()));
            perk.setType(PerkType.fromTag(columns[4].trim()));
            perk.setBonusesRaw(columns[5].trim());
            // A handful of rows have a stray tab splitting the description in two (a data typo in the
            // legacy files); join everything from column 6 onwards instead of silently dropping it.
            final String description = String.join(" ", Arrays.copyOfRange(columns, 6, columns.length)).trim();
            perk.setDescription(description);
            perks.add(perk);
        }
        return perks;
    }

    private static List<String> parseAvailableTo(String rawColumn) {
        final String[] tokens = rawColumn.replace(";", ",").split(",");
        final List<String> names = new ArrayList<>();
        for (final String token : tokens) {
            final String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return Arrays.asList(names.toArray(new String[0]));
    }
}
