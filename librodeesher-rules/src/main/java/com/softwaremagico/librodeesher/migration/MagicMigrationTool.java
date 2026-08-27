package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.magic.MagicSpellList;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One-shot command line tool that converts the legacy {@code hechizos/<Reino>.txt} files (one file
 * per {@link RealmOfMagic}) into a single {@code hechizos.xml} per module, read by
 * {@link com.softwaremagico.librodeesher.magic.MagicSpellListFactory}.
 *
 * <h2>Legacy file format</h2>
 * Each line is {@code "Nombre de la Lista\tPropietario1/Propietario2/..."}: a spell list name and the
 * professions/trainings (or the special {@code "Lista Abierta"}/{@code "Lista Cerrada"} pseudo-owner
 * tags) that grant access to it, slash-separated when there is more than one.
 *
 * <p>Like categories and skills, a (realm, list name) pair is a cross-module identity: if a later
 * module lists more owners for a list already defined by an earlier one (e.g. an expansion adding a
 * new profession to an existing spell list), the owners are merged into the list kept by the
 * defining module, instead of creating a duplicate entry.</p>
 */
public final class MagicMigrationTool {

    private static final String SPELLS_FOLDER = "hechizos";
    private static final String OUTPUT_FILE = "spells.xml";

    private MagicMigrationTool() {
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

        final Map<String, MagicSpellList> spellListsById = new LinkedHashMap<>();
        final Map<String, List<MagicSpellList>> spellListsByModule = new LinkedHashMap<>();
        final IdAllocator idAllocator = new IdAllocator();

        for (final String module : ModuleManager.getAllModules()) {
            for (final RealmOfMagic realm : RealmOfMagic.values()) {
                final Path file = modulosDir.resolve(LegacyModules.sourceFolderFor(module)).resolve(SPELLS_FOLDER).resolve(realm.getTag() + ".txt");
                if (!Files.isRegularFile(file) || !LegacyFileFilters.isRealDataFile(file)) {
                    continue;
                }
                readSpellListsFile(file, realm, module, spellListsById, spellListsByModule, idAllocator);
            }
        }

        int written = 0;
        for (final Map.Entry<String, List<MagicSpellList>> entry : spellListsByModule.entrySet()) {
            XmlMigrationWriter.write(modulesTarget.resolve(entry.getKey()).resolve(OUTPUT_FILE),
                    "spells", "spellList", entry.getValue());
            written++;
        }
        return written;
    }

    private static void readSpellListsFile(Path file, RealmOfMagic realm, String module,
                                            Map<String, MagicSpellList> spellListsById,
                                            Map<String, List<MagicSpellList>> spellListsByModule,
                                            IdAllocator idAllocator) throws IOException {
        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            final String[] columns = line.split("\t");
            final String name = columns[0].trim();
            final List<String> owners = translateOwners(columns[1].trim().split("/"));
            // Realm-scoped key: the same list name is (rarely) reused across different realms.
            final String id = idAllocator.idFor(realm.name() + "|" + name, MagicSpellList.buildId(realm, name));

            final MagicSpellList existing = spellListsById.get(id);
            if (existing == null) {
                final MagicSpellList spellList = new MagicSpellList(id);
                spellList.setName(name, Translations.toEnglish(name));
                spellList.setRealm(realm);
                spellList.setOwners(owners);
                spellListsById.put(id, spellList);
                spellListsByModule.computeIfAbsent(module, key -> new ArrayList<>()).add(spellList);
            } else {
                // A later module grants this same list to additional owners: merge, keep order.
                final Set<String> mergedOwners = new LinkedHashSet<>(existing.getOwners());
                mergedOwners.addAll(owners);
                existing.setOwners(new ArrayList<>(mergedOwners));
            }
        }
    }

    /**
     * Translates each owner token to English: the "Lista Abierta"/"Lista Cerrada" pseudo-owner tags
     * become {@link MagicSpellList#OPEN_LIST_TAG}/{@link MagicSpellList#CLOSED_LIST_TAG}; real
     * profession/training names are translated for readability (not yet resolved to their id, see
     * {@link MagicSpellList}'s javadoc).
     */
    private static List<String> translateOwners(String[] rawOwners) {
        final List<String> owners = new ArrayList<>();
        for (final String rawOwner : rawOwners) {
            final String trimmed = rawOwner.trim();
            if (trimmed.equalsIgnoreCase("Lista Abierta")) {
                owners.add(MagicSpellList.OPEN_LIST_TAG);
            } else if (trimmed.equalsIgnoreCase("Lista Cerrada")) {
                owners.add(MagicSpellList.CLOSED_LIST_TAG);
            } else {
                owners.add(Translations.toEnglish(trimmed));
            }
        }
        return owners;
    }
}
