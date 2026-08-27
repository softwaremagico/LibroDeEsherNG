package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.training.ChoiceGroup;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingProfessionCost;
import com.softwaremagico.librodeesher.training.TrainingRequirement;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;
import com.softwaremagico.librodeesher.training.TrainingSpecialItem;
import com.softwaremagico.librodeesher.training.TrainingType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * One-shot command line tool that converts the legacy {@code adiestramientos/*.txt} files (one file
 * per training/background package) into {@code adiestramientos.xml}, one per module, read by
 * {@link com.softwaremagico.librodeesher.training.TrainingFactory}.
 *
 * <h2>Legacy file format</h2>
 * A fixed sequence of 9 sections, each introduced by a {@code #...} header line and a
 * {@code ####...} separator, blank-line terminated:
 * <ol>
 *     <li>TIEMPO (meses): a single integer.</li>
 *     <li>EXCLUSIVO RAZA: "Ninguno" or a comma-separated race list.</li>
 *     <li>ESPECIAL: "Nombre\tProbabilidad\tBonus\tHabilidad" lines (bonus/skill optional).</li>
 *     <li>HABILIDADES: category/skill ranks granted, e.g. "Influencia\t3\t2\t2\t4" followed by
 *     "  *  Seducción\t3"; see {@link TrainingCategoryGrant} for the full syntax.</li>
 *     <li>AUMENTOS CARACTERÍSTICAS: "Ninguno", a comma list of fixed grants, or {@code {Ab1;Ab2}}
 *     choice groups.</li>
 *     <li>REQUISITOS PROFESIONALES: "Ninguno" or "Nombre (valor) (modificador), ..." entries.</li>
 *     <li>HABILIDADES DE ESTILO DE VIDA / COMUNES / PROFESIONALES / RESTRINGIDAS: four sections
 *     sharing the same "Ninguna", plain name, or {@code {alt1;alt2}} choice syntax.</li>
 *     <li>An optional trailing per-profession cost override (unused by every real training file).</li>
 * </ol>
 */
public final class TrainingMigrationTool {

    private static final String TRAININGS_FOLDER = "adiestramientos";
    private static final String OUTPUT_FILE = "trainings.xml";
    private static final String NOTHING_MARKER = "ningun";

    private TrainingMigrationTool() {
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
        final IdAllocator idAllocator = new IdAllocator();

        int written = 0;
        for (final String module : ModuleManager.getAllModules()) {
            final Path trainingsDir = modulosDir.resolve(LegacyModules.sourceFolderFor(module)).resolve(TRAININGS_FOLDER);
            if (!Files.isDirectory(trainingsDir)) {
                continue;
            }
            final List<Training> trainings = new ArrayList<>();
            try (Stream<Path> files = Files.list(trainingsDir)) {
                for (final Path file : files.filter(path -> path.toString().endsWith(".txt"))
                        .filter(LegacyFileFilters::isRealDataFile).sorted().toList()) {
                    trainings.add(readTrainingFile(file, idAllocator));
                }
            }
            if (trainings.isEmpty()) {
                continue;
            }
            XmlMigrationWriter.write(modulesTarget.resolve(module).resolve(OUTPUT_FILE),
                    "trainings", "training", trainings);
            written++;
        }
        return written;
    }

    private static Training readTrainingFile(Path file, IdAllocator idAllocator) throws IOException {
        final String fileName = file.getFileName().toString();
        final String trainingName = fileName.substring(0, fileName.length() - ".txt".length());
        final SectionCursor cursor = new SectionCursor(Files.readAllLines(file, StandardCharsets.UTF_8));

        final Training training = new Training(idAllocator.idFor(trainingName));
        training.setName(trainingName, Translations.toEnglish(trainingName));
        training.setTrainingTimeInMonths(Integer.valueOf(cursor.nextSection().get(0).trim()));
        training.setLimitedRaces(parseCommaList(cursor.nextSection()));
        training.setSpecialItems(parseSpecialItems(cursor.nextSection()));
        training.setCategories(parseCategories(cursor.nextSection()));
        training.setCharacteristicUpgrades(parseChoiceGroups(cursor.nextSection()));
        training.setRequirements(parseRequirements(cursor.nextSection()));
        training.setLifeSkills(parseChoiceGroups(cursor.nextSection()));
        training.setCommonSkills(parseChoiceGroups(cursor.nextSection()));
        training.setProfessionalSkills(parseChoiceGroups(cursor.nextSection()));
        training.setRestrictedSkills(parseChoiceGroups(cursor.nextSection()));
        training.setProfessionCosts(parseProfessionCosts(cursor.nextSectionOrEmpty()));
        return training;
    }

    /**
     * Parses the "HABILIDADES" section into category grants, attaching each "  *  Skill..." line
     * that follows to the category grant declared immediately above it (a line belongs to a skill,
     * not a category, precisely when it contains a "*", exactly like the legacy parser).
     */
    private static List<TrainingCategoryGrant> parseCategories(List<String> sectionLines) {
        final List<TrainingCategoryGrant> categories = new ArrayList<>();
        TrainingCategoryGrant currentCategory = null;
        for (final String rawLine : sectionLines) {
            if (!rawLine.contains("*")) {
                currentCategory = parseCategoryLine(rawLine);
                categories.add(currentCategory);
            } else {
                if (currentCategory == null) {
                    throw new IllegalStateException("Skill line without a preceding category: '" + rawLine + "'.");
                }
                currentCategory.getSkills().add(parseSkillLine(rawLine));
            }
        }
        return categories;
    }

    /**
     * Parses one category line: either {@code "Nombre\tRanks\tMin\tMax\tDistribute"} or
     * {@code "{Cat1; Cat2}\tRanks\tMin\tMax\tDistribute"} (player chooses one of the listed
     * categories).
     */
    private static TrainingCategoryGrant parseCategoryLine(String rawLine) {
        final List<String> categoryOptions;
        final String[] numberColumns;
        if (rawLine.contains("{")) {
            final String[] parts = rawLine.trim().split("}", 2);
            categoryOptions = parseChoiceOptions(parts[0].substring(parts[0].indexOf('{') + 1));
            numberColumns = onlyNonBlank(parts[1].split("\t"));
        } else {
            final String[] columns = rawLine.split("\t");
            categoryOptions = List.of(columns[0].trim());
            numberColumns = Arrays.copyOfRange(columns, 1, columns.length);
        }

        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(categoryOptions);
        grant.setRanksGranted(Integer.valueOf(numberColumns[0].trim()));
        grant.setMinSkills(Integer.valueOf(numberColumns[1].trim()));
        grant.setMaxSkills(Integer.valueOf(numberColumns[2].trim()));
        grant.setRanksToDistribute(Integer.valueOf(numberColumns[3].trim()));
        return grant;
    }

    /**
     * Parses one "  *  Skill..." line: either {@code "Nombre\tRanks"} or
     * {@code "{Skill1; Skill2}\t-Ranks"} (player chooses one of the listed skills; the leading "-" is
     * a purely cosmetic legacy marker for "choice" and is stripped).
     */
    private static TrainingSkillGrant parseSkillLine(String rawLine) {
        final String withoutMarker = rawLine.replace("*", "").trim();
        final List<String> skillOptions;
        final String ranksColumn;
        if (withoutMarker.contains("{")) {
            final String[] parts = withoutMarker.split("}", 2);
            skillOptions = parseChoiceOptions(parts[0].substring(parts[0].indexOf('{') + 1));
            ranksColumn = parts[1];
        } else {
            final String[] columns = withoutMarker.split("\t");
            skillOptions = List.of(columns[0].trim());
            ranksColumn = columns.length > 1 ? columns[1] : "0";
        }
        final Integer ranks = Integer.valueOf(ranksColumn.replace("-", "").replace("\t", "").trim());
        return new TrainingSkillGrant(skillOptions, ranks);
    }

    /** Splits a {@code "a; b"} or {@code "a, b"} choice list (without its surrounding braces) and trims each option. */
    private static List<String> parseChoiceOptions(String content) {
        final List<String> options = new ArrayList<>();
        for (final String option : content.replace(";", ",").split(",")) {
            if (!option.isBlank()) {
                options.add(option.trim());
            }
        }
        return options;
    }

    private static String[] onlyNonBlank(String[] tokens) {
        return Arrays.stream(tokens).filter(token -> !token.isBlank()).toArray(String[]::new);
    }

    private static List<String> parseCommaList(List<String> sectionLines) {
        final List<String> values = new ArrayList<>();
        for (final String line : sectionLines) {
            if (isNothingMarker(line)) {
                continue;
            }
            for (final String token : line.split(",\\s*")) {
                if (!token.isBlank()) {
                    values.add(Translations.toEnglish(token.trim()));
                }
            }
        }
        return values;
    }

    private static List<TrainingSpecialItem> parseSpecialItems(List<String> sectionLines) {
        final List<TrainingSpecialItem> items = new ArrayList<>();
        for (final String line : sectionLines) {
            final String[] columns = line.split("\t");
            final Integer bonus = columns.length > 2 ? Integer.valueOf(columns[2].trim()) : null;
            final String skillName = columns.length > 3 ? columns[3].trim() : null;
            items.add(new TrainingSpecialItem(columns[0].trim(), Integer.valueOf(columns[1].trim()), bonus, skillName));
        }
        return items;
    }

    /**
     * Parses the shared "Ninguno(a)" / plain comma list / {@code {alt1;alt2}} choice syntax used by
     * the characteristics-upgrade section and the four skill sections.
     */
    private static List<ChoiceGroup> parseChoiceGroups(List<String> sectionLines) {
        final List<ChoiceGroup> groups = new ArrayList<>();
        for (final String rawLine : sectionLines) {
            if (isNothingMarker(rawLine)) {
                continue;
            }
            for (final String entry : rawLine.split(",\\s*")) {
                final String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("{")) {
                    final String content = trimmed.substring(1, trimmed.length() - 1);
                    final List<String> options = new ArrayList<>();
                    for (final String option : content.split(";")) {
                        options.add(option.trim());
                    }
                    groups.add(new ChoiceGroup(options));
                } else {
                    groups.add(new ChoiceGroup(List.of(trimmed)));
                }
            }
        }
        return groups;
    }

    private static List<TrainingRequirement> parseRequirements(List<String> sectionLines) {
        final List<TrainingRequirement> requirements = new ArrayList<>();
        for (final String line : sectionLines) {
            if (isNothingMarker(line)) {
                continue;
            }
            for (final String entry : line.split(",\\s*")) {
                // "Religión (10) (-3)"
                final String[] parts = entry.trim().split("\\(");
                final String name = parts[0].trim();
                final Integer value = Integer.valueOf(parts[1].replace(")", "").trim());
                final Integer costModification = Integer.valueOf(parts[2].replace(")", "").trim());
                requirements.add(new TrainingRequirement(name, value, costModification));
            }
        }
        return requirements;
    }

    private static List<TrainingProfessionCost> parseProfessionCosts(List<String> sectionLines) {
        final List<TrainingProfessionCost> costs = new ArrayList<>();
        for (final String line : sectionLines) {
            if (isNothingMarker(line)) {
                continue;
            }
            final String[] columns = line.split("\t");
            final TrainingType type;
            if (columns[0].contains("+") || columns[1].contains("+")) {
                type = TrainingType.FAVOURITE;
            } else if (columns[0].contains("-") || columns[1].contains("-")) {
                type = TrainingType.FORBIDDEN;
            } else {
                type = TrainingType.STANDARD;
            }
            final String profession = columns[0].replace("+", "").replace("-", "").trim();
            final Integer cost = Integer.valueOf(columns[1].replace("+", "").replace("-", "").trim());
            costs.add(new TrainingProfessionCost(profession, cost, type));
        }
        return costs;
    }

    private static boolean isNothingMarker(String line) {
        return line.toLowerCase().contains(NOTHING_MARKER);
    }
}
