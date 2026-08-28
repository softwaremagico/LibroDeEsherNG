package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.language.LanguageSlot;
import com.softwaremagico.librodeesher.language.TranslatedText;
import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.race.RaceLanguage;
import com.softwaremagico.librodeesher.race.RaceSpecial;
import com.softwaremagico.librodeesher.resistance.ResistanceType;

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
    private static final String ANY_RACE_LANGUAGE = "Idioma Racial";
    private static final String ANY_CULTURE_LANGUAGE = "Idioma Regional";

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
        race.setResistanceBonuses(parseResistanceBonuses(cursor.nextSection()));
        race.setProgressionRankValues(parseStringMap(cursor.nextSection()));
        final ParsedReferenceIds restrictedProfessions = parseReferenceIds(cursor.nextSection());
        race.setRestrictedProfessionIds(restrictedProfessions.ids());
        race.setExcludedProfessionIds(restrictedProfessions.excluded());
        parseOtherRaceInformation(cursor, race);
        final ParsedLanguages raceLanguages = parseLanguages(cursor.nextSection());
        race.setRaceLanguages(raceLanguages.languages());
        race.setOptionalRaceLanguages(raceLanguages.optionalLanguages());
        final ParsedLanguages backgroundLanguages = parseLanguages(cursor.nextSection());
        race.setBackgroundLanguages(backgroundLanguages.languages());
        race.setOptionalBackgroundLanguages(backgroundLanguages.optionalLanguages());
        parseSpecialSkillSection(cursor.nextSection(), knownCategoryNames, race.getCommonSkillIds(), race.getCommonCategoryIds());
        parseSpecialSkillSection(cursor.nextSection(), knownCategoryNames, race.getRestrictedSkillIds(), race.getRestrictedCategoryIds());
        final ParsedReferenceIds cultures = parseCultureIds(cursor.nextSection());
        race.setCultureIds(cultures.ids());
        race.setExcludedCultureIds(cultures.excluded());
        race.setSpecials(parseSpecials(cursor.nextSection(), race));
        race.setMaleNames(parseNames(cursor.nextSectionOrEmpty()));
        race.setFemaleNames(parseNames(cursor.nextSectionOrEmpty()));
        race.setFamilyNames(parseNames(cursor.nextSectionOrEmpty()));
        return race;
    }

    /**
     * Parses the "MODIFICACIÓN A LAS CARACTERÍSTICAS" section, keyed by {@link
     * CharacteristicAbbreviation} constant name (resolved from the legacy two-letter Spanish tag via
     * {@link CharacteristicAbbreviation#fromTag(String)}) instead of the raw tag itself, so the
     * generated XML never embeds Spanish text as a map key/element name.
     */
    private static void parseCharacteristics(List<String> lines, Race race) {
        final Map<String, Integer> bonuses = new LinkedHashMap<>();
        int appearance = 0;
        for (final String line : lines) {
            final String[] columns = line.split("\t");
            if (columns.length < 2) {
                continue;
            }
            final CharacteristicAbbreviation abbreviation = CharacteristicAbbreviation.fromTag(columns[0].trim());
            if (abbreviation == CharacteristicAbbreviation.APPEARANCE) {
                appearance = Integer.parseInt(columns[1].trim());
            } else if (abbreviation != CharacteristicAbbreviation.NONE) {
                bonuses.put(abbreviation.name(), Integer.parseInt(columns[1].trim()));
            } else {
                throw new IllegalStateException("Unknown characteristic tag: '" + columns[0].trim() + "'.");
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

    /**
     * Parses the "MODIFICACIÓN A LA TR" section into resistance bonuses, keyed by {@link
     * ResistanceType} constant name instead of a translated Spanish column name (unlike a generic
     * {@code parseNumberMap}, since a resistance type is a fixed, already-known set of values, not
     * free text to translate).
     */
    private static Map<String, Integer> parseResistanceBonuses(List<String> lines) {
        final Map<String, Integer> result = new LinkedHashMap<>();
        for (final String line : lines) {
            final String[] columns = line.split("\t");
            if (columns.length >= 2) {
                final ResistanceType type = ResistanceType.fromTag(columns[0].trim());
                if (type == null) {
                    throw new IllegalStateException("Unknown resistance type: '" + columns[0].trim() + "'.");
                }
                result.put(type.name(), Integer.valueOf(columns[1].trim()));
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

    /**
     * Parses a race's "IDIOMAS"/"IDIOMAS DE TRASFONDO" section: each line is either a named language
     * ("Nombre\tInicialHabla/Escritura\tMáxHabla/Escritura") or the anonymous "Idioma Racial"/"Idioma
     * Regional" marker (a language slot the player picks freely, see {@link LanguageSlot}).
     */
    private static ParsedLanguages parseLanguages(List<String> lines) {
        final List<RaceLanguage> languages = new ArrayList<>();
        final List<LanguageSlot> optionalLanguages = new ArrayList<>();
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
            final String name = columns[0].trim();
            if (ANY_RACE_LANGUAGE.equalsIgnoreCase(name) || ANY_CULTURE_LANGUAGE.equalsIgnoreCase(name)) {
                optionalLanguages.add(new LanguageSlot(initial[0], initial[1], max[0], max[1]));
            } else {
                languages.add(new RaceLanguage(Translations.toEnglishId(name), initial[0], initial[1], max[0], max[1]));
            }
        }
        return new ParsedLanguages(languages, optionalLanguages);
    }

    /** The result of {@link #parseLanguages(List)}: named languages, and anonymous optional slots. */
    private record ParsedLanguages(List<RaceLanguage> languages, List<LanguageSlot> optionalLanguages) {
    }

    private static int[] parseRankPair(String value) {
        final String[] parts = value.trim().split("/");
        return new int[] {Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }

    /**
     * Parses a comma-separated race/culture-profession reference section ("PROFESIONES PROHIBIDAS"/
     * "CULTURAS DISPONIBLES"): a plain token resolves to a real id, a {@code "{Group}"} token to a
     * {@code "group:"}-prefixed marker (resolved against the live catalog at runtime, e.g. every
     * culture whose id contains it, or every profession of that magic realm), and a leading "-"
     * moves the (otherwise identically resolved) token to {@link ParsedReferenceIds#excluded()}
     * instead of {@link ParsedReferenceIds#ids()}, matching the legacy per-file "exception" flag
     * that flips the whole section's meaning once any entry uses it.
     */
    private static ParsedReferenceIds parseReferenceIds(List<String> lines) {
        final List<String> ids = new ArrayList<>();
        final List<String> excluded = new ArrayList<>();
        for (final String line : lines) {
            if (isNoneOrBlank(line)) {
                continue;
            }
            for (final String token : line.split(",")) {
                String value = token.trim();
                if (value.isEmpty()) {
                    continue;
                }
                final boolean isExcluded = value.startsWith("-");
                if (isExcluded) {
                    value = value.substring(1).trim();
                }
                final String resolved = value.contains("{")
                        ? "group:" + Translations.toEnglishId(stripBraces(value))
                        : Translations.toEnglishId(value);
                (isExcluded ? excluded : ids).add(resolved);
            }
        }
        return new ParsedReferenceIds(ids, excluded);
    }

    /** The result of {@link #parseReferenceIds(List)}: normal mentions, and "-"-marked ones. */
    private record ParsedReferenceIds(List<String> ids, List<String> excluded) {
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

    private static ParsedReferenceIds parseCultureIds(List<String> lines) {
        if (lines.stream().anyMatch(line -> line.equalsIgnoreCase("Todas"))) {
            return new ParsedReferenceIds(List.of("all"), List.of());
        }
        return parseReferenceIds(lines);
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
