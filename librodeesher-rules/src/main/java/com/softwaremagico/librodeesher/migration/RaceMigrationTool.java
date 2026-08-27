package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.language.TranslatedText;
import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.race.RaceLanguage;
import com.softwaremagico.librodeesher.race.RaceSpecial;

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
import java.util.stream.Stream;

/**
 * One-shot command line tool that converts the legacy {@code razas/*.txt} files (one file per race)
 * into {@code races.xml}, one file per module, read by {@link com.softwaremagico.librodeesher.race.RaceFactory}.
 *
 * <p>The legacy race parser is the largest one in the old application. This migrator keeps the same
 * section order and captures all relevant data, while normalizing cross-references to English ids:
 * professions, cultures, skills and categories are stored as id-like values instead of Spanish names.</p>
 */
public final class RaceMigrationTool {

    private static final String RACES_FOLDER = "razas";
    private static final String OUTPUT_FILE = "races.xml";

    private RaceMigrationTool() {
        // Utility class.
    }

    public static void main(String[] args) throws IOException {
        final Path sourceRoot = Path.of(args.length > 0 ? args[0] : "../../LibroDeEsher");
        final Path modulesTarget = Path.of(args.length > 1 ? args[1] : "../modules");
        final int written = migrate(sourceRoot, modulesTarget);
        System.out.println("Wrote " + written + " '" + OUTPUT_FILE + "' file(s) under " + modulesTarget.toAbsolutePath());
    }

    public static int migrate(Path sourceRoot, Path modulesTarget) throws IOException {
        final Path rolemasterDir = sourceRoot.resolve("rolemaster");
        final Path modulosDir = rolemasterDir.resolve("modulos");
        final Set<String> knownCategoryNames = loadCategoryNames(rolemasterDir, modulosDir);
        final IdAllocator idAllocator = new IdAllocator();

        int written = 0;
        for (final String module : ModuleManager.getAllModules()) {
            final Path racesDir = modulosDir.resolve(LegacyModules.sourceFolderFor(module)).resolve(RACES_FOLDER);
            if (!Files.isDirectory(racesDir)) {
                continue;
            }
            final List<Race> races = new ArrayList<>();
            try (Stream<Path> files = Files.list(racesDir)) {
                for (final Path file : files.filter(path -> path.toString().endsWith(".txt"))
                        .filter(LegacyFileFilters::isRealDataFile).sorted().toList()) {
                    races.add(readRaceFile(file, idAllocator, knownCategoryNames));
                }
            }
            if (races.isEmpty()) {
                continue;
            }
            XmlMigrationWriter.write(modulesTarget.resolve(module).resolve(OUTPUT_FILE), "races", "race", races);
            written++;
        }
        return written;
    }

    private static Set<String> loadCategoryNames(Path rolemasterDir, Path modulosDir) throws IOException {
        final Set<String> names = new LinkedHashSet<>();
        for (final String module : ModuleManager.getAllModules()) {
            for (final Path file : LegacyCategoriesFiles.forModule(module, rolemasterDir, modulosDir)) {
                for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.isBlank() || line.startsWith("#") || !line.contains("(")) {
                        continue;
                    }
                    names.add(line.substring(0, line.indexOf('(')).trim());
                }
            }
        }
        return names;
    }

    private static Race readRaceFile(Path file, IdAllocator idAllocator, Set<String> knownCategoryNames) throws IOException {
        final String fileName = file.getFileName().toString();
        final String raceName = fileName.substring(0, fileName.length() - ".txt".length());
        final SectionCursor cursor = new SectionCursor(Files.readAllLines(file, StandardCharsets.UTF_8));

        final Race race = new Race(idAllocator.idFor(raceName));
        race.setName(raceName, Translations.toEnglish(raceName));
        race.setNaturalArmorType(1);
        race.setCommonSkillIds(new ArrayList<>());
        race.setCommonCategoryIds(new ArrayList<>());
        race.setRestrictedSkillIds(new ArrayList<>());
        race.setRestrictedCategoryIds(new ArrayList<>());
        parseCharacteristics(cursor.nextSection(), race);
        race.setExpectedLifeYears(parseLifeExpectation(cursor.nextSection()));
        race.setResistanceBonuses(parseNumberMap(cursor.nextSection()));
        race.setProgressionRankValues(parseStringMap(cursor.nextSection()));
        race.setRestrictedProfessionIds(parseReferenceIds(cursor.nextSection(), ReferenceKind.PROFESSION));
        parseOtherRaceInformation(cursor, race);
        race.setRaceLanguages(parseLanguages(cursor.nextSection()));
        race.setBackgroundLanguages(parseLanguages(cursor.nextSection()));
        parseSpecialSkillSection(cursor.nextSection(), knownCategoryNames, race.getCommonSkillIds(), race.getCommonCategoryIds());
        parseSpecialSkillSection(cursor.nextSection(), knownCategoryNames, race.getRestrictedSkillIds(), race.getRestrictedCategoryIds());
        race.setCultureIds(parseCultureIds(cursor.nextSection()));
        race.setSpecials(parseSpecials(cursor.nextSection(), race));
        race.setMaleNames(parseNames(cursor.nextSectionOrEmpty()));
        race.setFemaleNames(parseNames(cursor.nextSectionOrEmpty()));
        race.setFamilyNames(parseNames(cursor.nextSectionOrEmpty()));
        return race;
    }

    private static void parseCharacteristics(List<String> lines, Race race) {
        final Map<String, Integer> bonuses = new LinkedHashMap<>();
        int appearance = 0;
        for (final String line : lines) {
            final String[] columns = line.split("\t");
            if (columns.length < 2) {
                continue;
            }
            if ("Ap".equals(columns[0].trim())) {
                appearance = Integer.parseInt(columns[1].trim());
            } else {
                bonuses.put(columns[0].trim(), Integer.parseInt(columns[1].trim()));
            }
        }
        race.setCharacteristicBonuses(bonuses);
        race.setAppearanceBonus(appearance);
    }

    private static Integer parseLifeExpectation(List<String> lines) {
        if (lines.isEmpty()) {
            return null;
        }
        final String value = lines.get(0).trim();
        if (value.equalsIgnoreCase("Inmortales")) {
            return 10000;
        }
        return Integer.valueOf(value);
    }

    private static Map<String, Integer> parseNumberMap(List<String> lines) {
        final Map<String, Integer> result = new LinkedHashMap<>();
        for (final String line : lines) {
            final String[] columns = line.split("\t");
            if (columns.length >= 2) {
                result.put(Translations.toEnglishId(columns[0].trim()), Integer.valueOf(columns[1].trim()));
            }
        }
        return result;
    }

    private static Map<String, String> parseStringMap(List<String> lines) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (final String line : lines) {
            final String[] columns = line.split("\t");
            if (columns.length >= 2) {
                result.put(Translations.toEnglishId(columns[0].trim()), columns[1].trim());
            }
        }
        return result;
    }

    private static void parseOtherRaceInformation(SectionCursor cursor, Race race) {
        race.setSoulDepartTime(parseOptionalInteger(cursor.nextSection()));
        race.setRaceType(parseOptionalInteger(cursor.nextSection()));
        race.setSize(cursor.nextSection().stream().findFirst().map(Translations::toEnglish).orElse(null));
        race.setRestorationTime(parseOptionalDouble(cursor.nextSection()));
        race.setLanguagePoints(parseOptionalInteger(cursor.nextSection()));
        race.setBackgroundPoints(parseOptionalInteger(cursor.nextSection()));
    }

    private static Integer parseOptionalInteger(List<String> section) {
        return section.isEmpty() ? null : Integer.valueOf(section.get(0).trim());
    }

    private static Double parseOptionalDouble(List<String> section) {
        return section.isEmpty() ? null : Double.valueOf(section.get(0).trim().replace(',', '.'));
    }

    private static List<RaceLanguage> parseLanguages(List<String> lines) {
        final List<RaceLanguage> languages = new ArrayList<>();
        for (final String line : lines) {
            if (line.toLowerCase().contains("ningun")) {
                continue;
            }
            final String[] columns = line.split("\t");
            if (columns.length < 3) {
                continue;
            }
            final int[] initial = parseRankPair(columns[1]);
            final int[] max = parseRankPair(columns[2]);
            languages.add(new RaceLanguage(Translations.toEnglish(columns[0].trim()), initial[0], initial[1], max[0], max[1]));
        }
        return languages;
    }

    private static int[] parseRankPair(String value) {
        final String[] parts = value.trim().split("/");
        return new int[] {Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }

    private enum ReferenceKind { PROFESSION, CULTURE }

    private static List<String> parseReferenceIds(List<String> lines, ReferenceKind kind) {
        final List<String> references = new ArrayList<>();
        for (final String line : lines) {
            if (isNoneOrBlank(line)) {
                continue;
            }
            for (final String token : line.split(",")) {
                String value = token.trim();
                if (value.isEmpty()) {
                    continue;
                }
                if (value.startsWith("-")) {
                    value = value.substring(1).trim();
                    references.add("exclude:" + Translations.toEnglishId(stripBraces(value)));
                } else if (value.contains("{")) {
                    references.add("group:" + Translations.toEnglishId(stripBraces(value)));
                } else {
                    references.add(Translations.toEnglishId(value));
                }
            }
        }
        return references;
    }

    private static void parseSpecialSkillSection(List<String> lines, Set<String> knownCategoryNames,
                                                 List<String> skillIds, List<String> categoryIds) {
        for (final String line : lines) {
            if (isNoneOrBlank(line)) {
                continue;
            }
            for (final String token : line.split(",")) {
                final String value = token.trim();
                if (value.isEmpty()) {
                    continue;
                }
                if (knownCategoryNames.contains(value) || value.contains("·")) {
                    categoryIds.add(Translations.toEnglishId(value));
                } else {
                    skillIds.add(Translations.toEnglishId(value));
                }
            }
        }
    }

    private static List<String> parseCultureIds(List<String> lines) {
        if (lines.stream().anyMatch(line -> line.equalsIgnoreCase("Todas"))) {
            return List.of("all");
        }
        return parseReferenceIds(lines, ReferenceKind.CULTURE);
    }

    private static List<RaceSpecial> parseSpecials(List<String> lines, Race race) {
        final List<RaceSpecial> specials = new ArrayList<>();
        for (final String line : lines) {
            if (isNoneOrBlank(line)) {
                continue;
            }
            final Integer points = parseBracketedPoints(line);
            final String cleanText = line.replaceAll("\\s*\\[[^]]+]\\s*$", "").trim();
            if (cleanText.toUpperCase().contains("TA")) {
                parseNaturalArmorType(cleanText, race);
            }
            specials.add(new RaceSpecial(new TranslatedText(cleanText, Translations.toEnglish(cleanText)), points));
        }
        return specials;
    }

    private static Integer parseBracketedPoints(String line) {
        final int open = line.lastIndexOf('[');
        final int close = line.lastIndexOf(']');
        if (open < 0 || close <= open) {
            return null;
        }
        try {
            return Integer.valueOf(line.substring(open + 1, close).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void parseNaturalArmorType(String text, Race race) {
        final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("TA[^0-9]*([0-9]+)").matcher(text.toUpperCase());
        if (matcher.find()) {
            race.setNaturalArmorType(Integer.valueOf(matcher.group(1)));
        }
    }

    private static List<String> parseNames(List<String> lines) {
        final List<String> names = new ArrayList<>();
        for (final String line : lines) {
            for (final String token : line.split(",")) {
                final String value = token.trim();
                if (!value.isEmpty()) {
                    names.add(value);
                }
            }
        }
        return names;
    }

    private static boolean isNoneOrBlank(String line) {
        final String lower = line.toLowerCase();
        return line.isBlank() || lower.contains("ningun") || lower.contains("none") || lower.endsWith(".");
    }

    private static String stripBraces(String value) {
        return value.replace("{", "").replace("}", "").trim();
    }
}
