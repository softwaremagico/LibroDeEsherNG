package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.ProfessionBonus;
import com.softwaremagico.librodeesher.profession.ProfessionTrainingCost;
import com.softwaremagico.librodeesher.training.TrainingType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * One-shot command line tool that converts the legacy {@code profesiones/*.txt} files (one file per
 * profession) into {@code profesiones.xml}, one per module, read by
 * {@link com.softwaremagico.librodeesher.profession.ProfessionFactory}.
 *
 * <h2>Legacy file format</h2>
 * A fixed sequence of 8 {@code #...}-header sections, blank-line terminated:
 * <ol>
 *     <li>CARACTERÍSTICAS POR ORDEN: "Indiferente", or characteristics abbreviations separated by
 *     spaces, in preference order.</li>
 *     <li>REINOS DE MAGIA: comma-separated magic realm names (possibly empty).</li>
 *     <li>BONIFICACIÓN POR PROFESIÓN: "Nombre\tBonus" flat category/skill bonuses.</li>
 *     <li>HABILIDADES Y CATEGORÍAS DE HABILIDADES: category development costs, with special-cased
 *     weapon category handling; kept verbatim, see {@link Profession}'s javadoc.</li>
 *     <li>HABILIDADES COMUNES / PROFESIONALES / RESTRINGIDAS: three sections mixing plain skill names
 *     with a "choose N from {a;b;c}" / "choose N from category#N" syntax; kept verbatim.</li>
 *     <li>DESARROLLO DE HECHIZOS: spell list development costs by character level range; kept
 *     verbatim.</li>
 *     <li>ADIESTRAMIENTO: "Nombre\tCoste[\tCosteSinMagia]" per-training background point costs, with
 *     an optional "+"/"-" preference marker; fully parsed.</li>
 * </ol>
 */
public final class ProfessionMigrationTool {

    private static final String PROFESSIONS_FOLDER = "profesiones";
    private static final String OUTPUT_FILE = "professions.xml";

    private ProfessionMigrationTool() {
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
            final Path professionsDir = modulosDir.resolve(LegacyModules.sourceFolderFor(module)).resolve(PROFESSIONS_FOLDER);
            if (!Files.isDirectory(professionsDir)) {
                continue;
            }
            final List<Profession> professions = new ArrayList<>();
            try (Stream<Path> files = Files.list(professionsDir)) {
                for (final Path file : files.filter(path -> path.toString().endsWith(".txt"))
                        .filter(LegacyFileFilters::isRealDataFile).sorted().toList()) {
                    professions.add(readProfessionFile(file, idAllocator));
                }
            }
            if (professions.isEmpty()) {
                continue;
            }
            XmlMigrationWriter.write(modulesTarget.resolve(module).resolve(OUTPUT_FILE),
                    "professions", "profession", professions);
            written++;
        }
        return written;
    }

    private static Profession readProfessionFile(Path file, IdAllocator idAllocator) throws IOException {
        final String fileName = file.getFileName().toString();
        final String professionName = fileName.substring(0, fileName.length() - ".txt".length());
        final SectionCursor cursor = new SectionCursor(Files.readAllLines(file, StandardCharsets.UTF_8));

        final Profession profession = new Profession(idAllocator.idFor(professionName));
        profession.setName(professionName, Translations.toEnglish(professionName));
        profession.setCharacteristicPreferences(parseCharacteristicPreferences(cursor.nextSection()));
        profession.setMagicRealms(parseMagicRealms(cursor.nextSection()));
        profession.setBonuses(parseBonuses(cursor.nextSection()));
        profession.setCategoryCostsRaw(String.join("\n", cursor.nextSection()));
        profession.setCommonSkillsRaw(String.join("\n", cursor.nextSection()));
        profession.setProfessionalSkillsRaw(String.join("\n", cursor.nextSection()));
        profession.setRestrictedSkillsRaw(String.join("\n", cursor.nextSection()));
        profession.setMagicCostsRaw(String.join("\n", cursor.nextSectionOrEmpty()));
        profession.setTrainingCosts(parseTrainingCosts(cursor.nextSectionOrEmpty()));
        return profession;
    }

    private static List<String> parseCharacteristicPreferences(List<String> sectionLines) {
        if (sectionLines.isEmpty() || sectionLines.get(0).toLowerCase().contains("indiferente")) {
            return List.of();
        }
        final List<String> preferences = new ArrayList<>();
        for (final String token : sectionLines.get(0).split(" ")) {
            if (!token.isBlank()) {
                preferences.add(token.trim());
            }
        }
        return preferences;
    }

    /**
     * Parses the "REINOS DE MAGIA" section. A token may itself be a "/"-separated hybrid (e.g. a
     * profession choosing between two realms); this is flattened into a plain list of every realm
     * involved, losing the original "choose one of" semantics of that hybrid, which is not modeled
     * yet (left as future work alongside {@code Profession}'s other simplifications).
     */
    private static List<RealmOfMagic> parseMagicRealms(List<String> sectionLines) {
        final List<RealmOfMagic> realms = new ArrayList<>();
        for (final String line : sectionLines) {
            for (final String token : line.split(",\\s*")) {
                for (final String realmTag : token.split("/")) {
                    if (!realmTag.isBlank()) {
                        realms.add(RealmOfMagic.fromTag(realmTag.trim()));
                    }
                }
            }
        }
        return realms;
    }

    private static List<ProfessionBonus> parseBonuses(List<String> sectionLines) {
        final List<ProfessionBonus> bonuses = new ArrayList<>();
        for (final String line : sectionLines) {
            final String[] columns = line.split("\t");
            bonuses.add(new ProfessionBonus(Translations.toEnglishId(columns[0].trim()), Integer.valueOf(columns[1].trim())));
        }
        return bonuses;
    }

    private static List<ProfessionTrainingCost> parseTrainingCosts(List<String> sectionLines) {
        final List<ProfessionTrainingCost> costs = new ArrayList<>();
        for (final String line : sectionLines) {
            final String[] columns = line.split("\t");
            final TrainingType type;
            if (columns[0].contains("+") || columns[1].contains("+")) {
                type = TrainingType.FAVOURITE;
            } else if (columns[0].contains("-") || columns[1].contains("-")) {
                type = TrainingType.FORBIDDEN;
            } else {
                type = TrainingType.STANDARD;
            }
            final String trainingName = columns[0].replace("+", "").replace("-", "").trim();
            final Integer cost = Integer.valueOf(columns[1].replace("+", "").replace("-", "").trim());
            final Integer costNotMagic = columns.length > 2
                    ? Integer.valueOf(columns[2].replace("+", "").replace("-", "").trim())
                    : null;
            costs.add(new ProfessionTrainingCost(Translations.toEnglishId(trainingName), cost, costNotMagic, type));
        }
        return costs;
    }
}
