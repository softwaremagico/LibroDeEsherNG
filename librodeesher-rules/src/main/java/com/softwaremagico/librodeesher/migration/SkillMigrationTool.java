package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.skill.SkillNameParser;
import com.softwaremagico.librodeesher.weapon.WeaponType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * One-shot command line tool that builds the global skill catalog and writes it as {@code
 * skills.xml}, one file per module, read by {@link com.softwaremagico.librodeesher.skill.SkillFactory}.
 *
 * <p>The legacy application never stored skills in their own text file: every skill was created the
 * first time its name was encountered inside a category's "Habilidades" column (see the original
 * {@code SkillFactory#getSkill(String)}). This tool walks the exact same {@code categorias.txt} files
 * as {@link CategoryMigrationTool} (in the same module order, so cross-module identity matches), and
 * for every skill token found:</p>
 * <ul>
 *     <li>the first module to mention a given skill name "owns" it (keeps it in its generated
 *     {@code skills.xml}), mirroring the category id/skill-name merge rule;</li>
 *     <li>the {@code noimporta} marker (used by weapon categories, whose skills come from the weapon
 *     files instead) contributes no skill.</li>
 * </ul>
 *
 * <p>Every weapon is also a skill (the legacy application's {@code
 * CategoryFactory#addWeaponsAsSkills()} converted each one via {@code SkillFactory#getSkill(weapon.getName())}
 * and assigned it to its weapon category): this tool reproduces that by walking every {@code
 * armas/<Tipo>.txt} file the same way {@link WeaponMigrationTool} does, after every category-derived
 * skill, contributing one more skill (assigned to {@link WeaponType#getCategoryId()}) for each weapon
 * name not already present. A race/perk/training referencing a weapon name is therefore expected to
 * resolve against this same catalog, not a separate one.</p>
 *
 * <p>Ids are English-derived (see {@link IdAllocator}), assigned in a second pass once every skill's
 * Spanish name is known, so that {@link Skill#getEnableSkills()} (which references other skills by
 * name) can be rewritten from Spanish names to the final ids.</p>
 *
 * <p>Once every skill has been read, this tool reproduces the original {@code
 * SkillFactory#updateDisabledSkills()} pass: any skill referenced by another skill's "enableSkills"
 * is baked in as {@link Skill#isEnabledByDefault()} = {@code false}. See {@link Skill} for the
 * documented limitation of computing this once, at migration time.</p>
 */
public final class SkillMigrationTool {

    private static final String DYNAMIC_SKILLS_MARKER = "noimporta";
    private static final String OUTPUT_FILE = "skills.xml";
    private static final String WEAPONS_FOLDER = "armas";

    private SkillMigrationTool() {
        // Utility class.
    }

    public static void main(String[] args) throws IOException {
        final Path sourceRoot = Path.of(args.length > 0 ? args[0] : "../../LibroDeEsher");
        final Path modulesTarget = Path.of(args.length > 1 ? args[1] : "../modulo");
        final int written = migrate(sourceRoot, modulesTarget);
        System.out.println("Wrote " + written + " '" + OUTPUT_FILE + "' file(s) under " + modulesTarget.toAbsolutePath());
    }

    public static int migrate(Path sourceRoot, Path modulesTarget) throws IOException {
        final Path rolemasterDir = sourceRoot.resolve("rolemaster");
        final Path modulosDir = rolemasterDir.resolve("modulos");

        // Spanish skill name -> parsed skill (id not assigned yet), in creation order.
        final Map<String, Skill> skillsBySpanishName = new LinkedHashMap<>();
        final Map<String, List<Skill>> skillsByModule = new LinkedHashMap<>();

        for (final String module : ModuleManager.getAllModules()) {
            for (final Path file : LegacyCategoriesFiles.forModule(module, rolemasterDir, modulosDir)) {
                readSkillsFromCategoriesFile(file, module, skillsBySpanishName, skillsByModule);
            }
        }

        for (final String module : ModuleManager.getAllModules()) {
            final Path weaponsDir = modulosDir.resolve(LegacyModules.sourceFolderFor(module)).resolve(WEAPONS_FOLDER);
            if (!Files.isDirectory(weaponsDir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(weaponsDir)) {
                for (final Path file : files.filter(path -> path.toString().endsWith(".txt"))
                        .filter(LegacyFileFilters::isRealDataFile).sorted().toList()) {
                    readWeaponSkillsFromFile(file, module, skillsBySpanishName, skillsByModule);
                }
            }
        }

        assignIdsAndRewriteEnableSkills(skillsBySpanishName);
        applyDisabledByEnableSkills(skillsBySpanishName.values());

        int written = 0;
        for (final Map.Entry<String, List<Skill>> entry : skillsByModule.entrySet()) {
            XmlMigrationWriter.write(modulesTarget.resolve(entry.getKey()).resolve(OUTPUT_FILE),
                    "skills", "skill", entry.getValue());
            written++;
        }
        return written;
    }

    private static void readSkillsFromCategoriesFile(Path file, String module, Map<String, Skill> skillsBySpanishName,
                                                      Map<String, List<Skill>> skillsByModule) throws IOException {
        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            final String[] columns = line.split("\t");
            if (columns.length != 4) {
                continue;
            }
            final String categoryName = columns[0].substring(0, columns[0].indexOf('('));
            for (final String rawToken : columns[3].split(",")) {
                final String trimmed = rawToken.trim();
                if (trimmed.isEmpty() || DYNAMIC_SKILLS_MARKER.equalsIgnoreCase(trimmed)) {
                    continue;
                }
                final Skill parsed = SkillNameParser.parse(trimmed);
                final String spanishName = parsed.getId();
                if (skillsBySpanishName.containsKey(spanishName)) {
                    continue;
                }
                parsed.setCategoryId(Translations.toEnglishId(categoryName));
                skillsBySpanishName.put(spanishName, parsed);
                skillsByModule.computeIfAbsent(module, key -> new ArrayList<>()).add(parsed);
            }
        }
    }

    /**
     * Reads one {@code armas/<Tipo>.txt} file (see {@link WeaponMigrationTool}'s class javadoc for
     * its format) and contributes one skill per weapon name not already present, assigned to that
     * weapon type's category id.
     */
    private static void readWeaponSkillsFromFile(Path file, String module, Map<String, Skill> skillsBySpanishName,
                                                  Map<String, List<Skill>> skillsByModule) throws IOException {
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
            final Skill parsed = SkillNameParser.parse(columns[0].trim());
            final String spanishName = parsed.getId();
            if (skillsBySpanishName.containsKey(spanishName)) {
                continue;
            }
            parsed.setCategoryId(type.getCategoryId());
            skillsBySpanishName.put(spanishName, parsed);
            skillsByModule.computeIfAbsent(module, key -> new ArrayList<>()).add(parsed);
        }
    }

    /**
     * Replaces every skill's temporary Spanish-name id with its final English-derived id, then
     * rewrites {@link Skill#getEnableSkills()} (Spanish names referencing other skills) into the
     * same final ids.
     */
    private static void assignIdsAndRewriteEnableSkills(Map<String, Skill> skillsBySpanishName) {
        final IdAllocator idAllocator = new IdAllocator();
        final Map<String, String> idBySpanishName = new LinkedHashMap<>();
        for (final Map.Entry<String, Skill> entry : skillsBySpanishName.entrySet()) {
            final String id = idAllocator.idFor(entry.getKey());
            idBySpanishName.put(entry.getKey(), id);
            entry.getValue().setId(id);
        }
        for (final Skill skill : skillsBySpanishName.values()) {
            if (skill.getEnableSkills().isEmpty()) {
                continue;
            }
            final List<String> rewritten = new ArrayList<>();
            for (final String spanishReference : skill.getEnableSkills()) {
                rewritten.add(idBySpanishName.getOrDefault(spanishReference, Translations.toEnglishId(spanishReference)));
            }
            skill.setEnableSkills(rewritten);
        }
    }

    private static void applyDisabledByEnableSkills(java.util.Collection<Skill> skills) {
        final Set<String> disabledIds = new HashSet<>();
        for (final Skill skill : skills) {
            disabledIds.addAll(skill.getEnableSkills());
        }
        for (final Skill skill : skills) {
            if (disabledIds.contains(skill.getId())) {
                skill.setEnabledByDefault(false);
            }
        }
    }
}
