package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.training.ChoiceGroup;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingProfessionCost;
import com.softwaremagico.librodeesher.training.TrainingRequirement;
import com.softwaremagico.librodeesher.training.TrainingSpecialItem;
import com.softwaremagico.librodeesher.training.TrainingType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 *     <li>HABILIDADES: category/skill ranks granted; kept verbatim, see {@link Training}'s javadoc.</li>
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
    private static final String OUTPUT_FILE = "adiestramientos.xml";
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

        int written = 0;
        for (final String module : ModuleManager.getAllModules()) {
            final Path trainingsDir = modulosDir.resolve(module).resolve(TRAININGS_FOLDER);
            if (!Files.isDirectory(trainingsDir)) {
                continue;
            }
            final List<Training> trainings = new ArrayList<>();
            try (Stream<Path> files = Files.list(trainingsDir)) {
                for (final Path file : files.filter(path -> path.toString().endsWith(".txt"))
                        .filter(LegacyFileFilters::isRealDataFile).sorted().toList()) {
                    trainings.add(readTrainingFile(file));
                }
            }
            if (trainings.isEmpty()) {
                continue;
            }
            XmlMigrationWriter.write(modulesTarget.resolve(module).resolve(OUTPUT_FILE),
                    "adiestramientos", "adiestramiento", trainings);
            written++;
        }
        return written;
    }

    private static Training readTrainingFile(Path file) throws IOException {
        final String fileName = file.getFileName().toString();
        final String trainingName = fileName.substring(0, fileName.length() - ".txt".length());
        final Cursor cursor = new Cursor(Files.readAllLines(file, StandardCharsets.UTF_8));

        final Training training = new Training(trainingName);
        training.setName(trainingName);
        training.setTrainingTimeInMonths(Integer.valueOf(cursor.nextSection().get(0).trim()));
        training.setLimitedRaces(parseCommaList(cursor.nextSection()));
        training.setSpecialItems(parseSpecialItems(cursor.nextSection()));
        training.setCategoriesRaw(String.join("\n", cursor.nextSection()));
        training.setCharacteristicUpgrades(parseChoiceGroups(cursor.nextSection()));
        training.setRequirements(parseRequirements(cursor.nextSection()));
        training.setLifeSkills(parseChoiceGroups(cursor.nextSection()));
        training.setCommonSkills(parseChoiceGroups(cursor.nextSection()));
        training.setProfessionalSkills(parseChoiceGroups(cursor.nextSection()));
        training.setRestrictedSkills(parseChoiceGroups(cursor.nextSection()));
        training.setProfessionCosts(parseProfessionCosts(cursor.nextSectionOrEmpty()));
        return training;
    }

    private static List<String> parseCommaList(List<String> sectionLines) {
        final List<String> values = new ArrayList<>();
        for (final String line : sectionLines) {
            if (isNothingMarker(line)) {
                continue;
            }
            for (final String token : line.split(",\\s*")) {
                if (!token.isBlank()) {
                    values.add(token.trim());
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

    /** Walks a training file section by section, mirroring the legacy index-based line scanning. */
    private static final class Cursor {
        private final List<String> lines;
        private int position;

        Cursor(List<String> lines) {
            this.lines = lines;
        }

        /** Skips blank/comment lines, then collects every line up to the next blank line or EOF. */
        List<String> nextSection() {
            skipHeader();
            final List<String> section = new ArrayList<>();
            while (position < lines.size() && !lines.get(position).isBlank() && !lines.get(position).startsWith("#")) {
                section.add(lines.get(position));
                position++;
            }
            return section;
        }

        /** Same as {@link #nextSection()}, but returns an empty list instead of failing at EOF. */
        List<String> nextSectionOrEmpty() {
            if (position >= lines.size()) {
                return List.of();
            }
            return nextSection();
        }

        private void skipHeader() {
            while (position < lines.size() && (lines.get(position).isBlank() || lines.get(position).startsWith("#"))) {
                position++;
            }
        }
    }
}
