package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.file.ModuleManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-shot command line tool that converts the legacy tab-separated {@code categorias.txt} files
 * (from the original "LibroDeEsher" desktop application) into the {@code categorias.xml} files read
 * by {@link com.softwaremagico.librodeesher.category.CategoryFactory}.
 *
 * <h2>Legacy file format</h2>
 * Each non-comment line has 4 tab-separated columns:
 * <pre>Nombre(Abreviatura)\tCaracterísticas\tProgresión\tHabilidades</pre>
 * e.g. {@code Armadura·Ligera(ArdL)\tAg/Fu/Ag\tEstándar\tCuero Endurecido [TA9; TA10; TA11], ...}
 *
 * <h2>Cross-module category identity</h2>
 * The legacy application also reads a base {@code rolemaster/categorias.txt} file (outside of any
 * module folder), always active regardless of which modules are enabled, plus one {@code
 * categorias.txt} per module. If a module re-declares a category that already exists (same name),
 * the original code only <em>appends</em> its skills to the existing category instead of replacing
 * it. This tool reproduces that behaviour at migration time: the base file is treated as part of the
 * "Basico" module, and whichever module first declares a given category id keeps it in its generated
 * {@code categorias.xml}; any skills added later by another module are merged into that same
 * category before writing.
 *
 * <p><strong>Known limitation:</strong> because the merge happens once, at migration time, disabling
 * a module that only <em>contributed extra skills</em> to a category first defined by another module
 * will not remove those extra skills at runtime (the category and its full skill list always live in
 * the defining module's XML). Revisiting this would require {@link
 * com.softwaremagico.librodeesher.xml.XmlFactory} to support additive (not just override) merging of
 * individual fields, which is left as future work.</p>
 */
public final class CategoryMigrationTool {

    private static final String OUTPUT_FILE = "categories.xml";

    private CategoryMigrationTool() {
        // Utility class.
    }

    public static void main(String[] args) throws IOException {
        final Path sourceRoot = Path.of(args.length > 0 ? args[0] : "../../LibroDeEsher");
        final Path modulesTarget = Path.of(args.length > 1 ? args[1] : "../modulo");
        final int written = migrate(sourceRoot, modulesTarget);
        System.out.println("Wrote " + written + " '" + OUTPUT_FILE + "' file(s) under " + modulesTarget.toAbsolutePath());
    }

    /**
     * Reads every {@code categorias.txt} under {@code sourceRoot/rolemaster} and writes one
     * {@code categorias.xml} per module under {@code modulesTarget}.
     *
     * @return the number of {@code categorias.xml} files written.
     */
    public static int migrate(Path sourceRoot, Path modulesTarget) throws IOException {
        // Module name -> categories first defined by that module, in the order that will be written.
        final Map<String, List<Category>> categoriesByModule = new LinkedHashMap<>();
        readAllCategories(sourceRoot, categoriesByModule);
        resolveSkillIds(sourceRoot, categoriesByModule.values());

        int written = 0;
        for (final Map.Entry<String, List<Category>> entry : categoriesByModule.entrySet()) {
            XmlMigrationWriter.write(modulesTarget.resolve(entry.getKey()).resolve(OUTPUT_FILE),
                    "categories", "category", entry.getValue());
            written++;
        }
        return written;
    }

    /**
     * Populates every category's {@link Category#getSkills()} (empty until now: {@link
     * Category#setSkillsRaw} no longer derives it automatically, see its javadoc) with the real
     * skill id for each plain Spanish name in {@link Category#getSkillsRaw()} (via {@link
     * Category#namesFromRaw}), resolved through {@link SkillMigrationTool#buildSkillIndex} (the same
     * index other migration tools use to resolve a skill referenced by name elsewhere, e.g. {@code
     * TrainingMigrationTool}); {@link TrainingMigrationTool#resolveSkillId} is reused for the same
     * fallback behaviour (a handful of category skill lines, mostly special-attack lore/style skills,
     * are worded slightly differently than their real {@code Skill} entry, see {@code
     * SKILL_NAME_ALIASES}).
     */
    private static void resolveSkillIds(Path sourceRoot, Iterable<List<Category>> categoriesByModule) throws IOException {
        final Map<String, String> skillIndex = SkillMigrationTool.buildSkillIndex(sourceRoot);
        for (final List<Category> categories : categoriesByModule) {
            for (final Category category : categories) {
                final List<String> resolved = new ArrayList<>();
                for (final String skillName : Category.namesFromRaw(category.getSkillsRaw())) {
                    resolved.add(TrainingMigrationTool.resolveSkillId(skillName, skillIndex));
                }
                category.setSkills(resolved);
            }
        }
    }

    /**
     * Rebuilds the full set of migrated categories (deterministically, same ids as {@link
     * #migrate(Path, Path)}) and returns a Spanish category name -&gt; id index, so that other
     * migration tools (e.g. {@code TrainingMigrationTool}, {@code CultureMigrationTool}) can resolve
     * the categories referenced by name in their own legacy files to the real category id instead of
     * embedding the raw Spanish name in {@code categoryOptions}.
     */
    public static Map<String, String> buildCategoryIndex(Path sourceRoot) throws IOException {
        final Map<String, List<Category>> categoriesByModule = new LinkedHashMap<>();
        final Map<String, Category> categoriesBySpanishName = readAllCategories(sourceRoot, categoriesByModule);
        final Map<String, String> index = new LinkedHashMap<>();
        categoriesBySpanishName.forEach((name, category) -> index.put(name, category.getId()));
        return index;
    }

    private static Map<String, Category> readAllCategories(Path sourceRoot, Map<String, List<Category>> categoriesByModule)
            throws IOException {
        final Path rolemasterDir = sourceRoot.resolve("rolemaster");
        final Path modulosDir = rolemasterDir.resolve("modulos");

        // Spanish category name -> element, in creation order, shared across every source file so
        // that a module re-declaring an existing category merges into it instead of creating a
        // duplicate.
        final Map<String, Category> categoriesBySpanishName = new LinkedHashMap<>();
        final IdAllocator idAllocator = new IdAllocator();

        for (final String module : ModuleManager.getAllModules()) {
            for (final Path file : LegacyCategoriesFiles.forModule(module, rolemasterDir, modulosDir)) {
                readCategoriesFile(file, module, categoriesBySpanishName, categoriesByModule, idAllocator);
            }
        }
        return categoriesBySpanishName;
    }

    /**
     * One parsed data line of a {@code categorias.txt} file.
     */
    private static void readCategoriesFile(Path file, String module, Map<String, Category> categoriesBySpanishName,
                                            Map<String, List<Category>> categoriesByModule,
                                            IdAllocator idAllocator) throws IOException {
        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            final ParsedCategoryLine parsed = ParsedCategoryLine.parse(line, file);
            final Category existing = categoriesBySpanishName.get(parsed.name());
            if (existing == null) {
                final Category category = new Category(idAllocator.idFor(parsed.name()));
                category.setName(parsed.name(), Translations.toEnglish(parsed.name()));
                category.setAbbreviation(parsed.abbreviation());
                category.setCharacteristicsTag(parsed.characteristicsTag());
                category.setType(com.softwaremagico.librodeesher.category.CategoryType.fromTag(parsed.typeTag()));
                category.setSkillsRaw(parsed.skillsRaw());
                categoriesBySpanishName.put(parsed.name(), category);
                categoriesByModule.computeIfAbsent(module, key -> new ArrayList<>()).add(category);
            } else {
                // Another module contributes extra skills to an already-known category: merge them.
                existing.setSkillsRaw(existing.getSkillsRaw() + ", " + parsed.skillsRaw());
            }
        }
    }

    /** One parsed data line of a {@code categorias.txt} file. */
    private record ParsedCategoryLine(String name, String abbreviation, String characteristicsTag, String typeTag,
                                       String skillsRaw) {

        static ParsedCategoryLine parse(String line, Path file) {
            final String[] columns = line.split("\t");
            if (columns.length != 4) {
                throw new IllegalStateException(
                        "Malformed category line in '" + file + "': expected 4 tab-separated columns, got "
                                + columns.length + ": " + line);
            }
            final int openParenthesis = columns[0].indexOf('(');
            if (openParenthesis < 0 || !columns[0].endsWith(")")) {
                throw new IllegalStateException(
                        "Malformed category name/abbreviation in '" + file + "': " + columns[0]);
            }
            final String name = columns[0].substring(0, openParenthesis);
            final String abbreviation = columns[0].substring(openParenthesis + 1, columns[0].length() - 1);
            return new ParsedCategoryLine(name, abbreviation, columns[1], columns[2], columns[3]);
        }
    }
}
