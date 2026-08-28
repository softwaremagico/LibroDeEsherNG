package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.culture.CultureLanguageRank;
import com.softwaremagico.librodeesher.culture.CultureTrainingPrice;
import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.language.LanguageSlot;
import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Converts legacy {@code culturas/*.txt} files into module-level {@code cultures.xml}. */
public final class CultureMigrationTool {

    private static final String CULTURES_FOLDER = "culturas";
    private static final String OUTPUT_FILE = "cultures.xml";
    private static final String ANY_CULTURE_LANGUAGE = "Idioma Regional";

    private CultureMigrationTool() {
        // Utility class.
    }

    public static void main(String[] args) throws IOException {
        final Path sourceRoot = Path.of(args.length > 0 ? args[0] : "../../LibroDeEsher");
        final Path modulesTarget = Path.of(args.length > 1 ? args[1] : "../modules");
        final int written = migrate(sourceRoot, modulesTarget);
        System.out.println("Wrote " + written + " '" + OUTPUT_FILE + "' file(s) under " + modulesTarget.toAbsolutePath());
    }

    public static int migrate(Path sourceRoot, Path modulesTarget) throws IOException {
        final Path modulosDir = sourceRoot.resolve("rolemaster").resolve("modulos");
        final IdAllocator idAllocator = new IdAllocator();
        final Map<String, String> categoryIndex = CategoryMigrationTool.buildCategoryIndex(sourceRoot);
        int written = 0;
        for (final String module : ModuleManager.getAllModules()) {
            final Path culturesDir = modulosDir.resolve(LegacyModules.sourceFolderFor(module)).resolve(CULTURES_FOLDER);
            if (!Files.isDirectory(culturesDir)) {
                continue;
            }
            final List<Culture> cultures = new ArrayList<>();
            try (Stream<Path> files = Files.list(culturesDir)) {
                for (final Path file : files.filter(path -> path.toString().endsWith(".txt"))
                        .filter(LegacyFileFilters::isRealDataFile).sorted().toList()) {
                    cultures.add(readCultureFile(file, idAllocator, categoryIndex));
                }
            }
            if (!cultures.isEmpty()) {
                XmlMigrationWriter.write(modulesTarget.resolve(module).resolve(OUTPUT_FILE), "cultures", "culture", cultures);
                written++;
            }
        }
        return written;
    }

    private static Culture readCultureFile(Path file, IdAllocator idAllocator, Map<String, String> categoryIndex)
            throws IOException {
        final String fileName = file.getFileName().toString();
        final String cultureName = fileName.substring(0, fileName.length() - ".txt".length());
        final SectionCursor cursor = new SectionCursor(Files.readAllLines(file, StandardCharsets.UTF_8));

        final Culture culture = new Culture(idAllocator.idFor(cultureName));
        culture.setName(cultureName, Translations.toEnglish(cultureName));
        culture.setTypicalWeaponIds(parseWeaponIds(cursor.nextSection()));
        culture.setTypicalArmorIds(parseArmorIds(cursor.nextSection()));
        culture.setAdolescenceRanks(parseAdolescenceRanks(cursor.nextSection(), categoryIndex));
        culture.setHobbyRanks(parseOptionalInteger(cursor.nextSection()));
        culture.setHobbyIds(parseHobbyIds(cursor.nextSection()));
        final ParsedCultureLanguages languages = parseLanguageRanks(cursor.nextSection());
        culture.setLanguageMaxRanks(languages.ranks());
        culture.setOptionalLanguages(languages.optionalLanguages());
        culture.setTrainingPrices(parseTrainingPrices(cursor.nextSectionOrEmpty()));
        return culture;
    }

    private static List<String> parseWeaponIds(List<String> lines) {
        final List<String> ids = new ArrayList<>();
        for (final String line : lines) {
            if (isAll(line)) {
                ids.add("all");
                continue;
            }
            if (isNone(line)) {
                continue;
            }
            for (final String token : splitComma(line)) {
                ids.add(Translations.toEnglishId(stripBraces(token)));
            }
        }
        return ids;
    }

    private static List<String> parseArmorIds(List<String> lines) {
        final List<String> ids = new ArrayList<>();
        for (final String line : lines) {
            if (isAll(line)) {
                ids.add("all");
                continue;
            }
            if (isNone(line)) {
                continue;
            }
            for (final String token : splitComma(line)) {
                ids.add(Translations.toEnglishId(token));
            }
        }
        return ids;
    }

    private static List<TrainingCategoryGrant> parseAdolescenceRanks(List<String> lines, Map<String, String> categoryIndex) {
        final List<TrainingCategoryGrant> categories = new ArrayList<>();
        TrainingCategoryGrant current = null;
        for (final String line : lines) {
            if (!line.contains("*")) {
                final String[] columns = line.split("\t");
                final String rawCategory = columns[0].trim();
                current = new TrainingCategoryGrant();
                if (rawCategory.contains("{")) {
                    final String options = rawCategory.substring(rawCategory.indexOf('{') + 1, rawCategory.indexOf('}'));
                    final List<String> optionNames = TrainingMigrationTool.parseChoiceOptions(options);
                    // A single name inside braces is not a literal category but a group prefix (e.g.
                    // "{Conocimiento}" means "any category whose name starts with Conocimiento·"),
                    // exactly like the legacy CategoryFactory.getCategoryByGroup lookup; two or more
                    // names are a literal choice between those exact categories instead.
                    current.setCategoryOptions(optionNames.size() == 1
                            ? resolveCategoryGroup(optionNames.get(0), categoryIndex)
                            : TrainingMigrationTool.resolveCategoryIds(optionNames, categoryIndex));
                } else {
                    current.setCategoryOptions(
                            TrainingMigrationTool.resolveCategoryIds(List.of(rawCategory), categoryIndex));
                }
                current.setRanksGranted(Integer.valueOf(columns[1].trim()));
                current.setMinSkills(0);
                current.setMaxSkills(0);
                current.setRanksToDistribute(0);
                categories.add(current);
            } else if (current != null) {
                final String[] columns = line.replace("*", "").trim().split("\t");
                current.getSkills().add(new TrainingSkillGrant(List.of(columns[0].trim()), Integer.valueOf(columns[1].trim())));
            }
        }
        return categories;
    }

    private static Integer parseOptionalInteger(List<String> section) {
        return section.isEmpty() ? null : Integer.valueOf(section.get(0).trim());
    }

    /**
     * Resolves every category whose Spanish name starts with {@code "groupPrefix·"} (e.g.
     * "Conocimiento" matches "Conocimiento·General", "Conocimiento·Técnico", ...), mirroring the
     * legacy {@code CategoryFactory.getCategoryByGroup} prefix lookup.
     */
    private static List<String> resolveCategoryGroup(String groupPrefix, Map<String, String> categoryIndex) {
        final String prefix = groupPrefix.toLowerCase() + "\u00b7";
        final List<String> ids = new ArrayList<>();
        categoryIndex.forEach((spanishName, id) -> {
            if (spanishName.toLowerCase().startsWith(prefix)) {
                ids.add(id);
            }
        });
        if (ids.isEmpty()) {
            throw new IllegalStateException("No categories found for group prefix '" + groupPrefix + "'.");
        }
        return ids;
    }

    /**
     * Parses the "AFICIONES" section into hobby ids. Note that a bare {@code "Idiomas"} token is
     * dropped, not translated into a marker: the legacy application recognized it (as {@code
     * Spanish.CULTURE_LANGUAGE_TAG}) but never actually implemented spending hobby points on a
     * language ({@code "// TODO select a language"}), so it was silently a no-op there too.
     */
    private static List<String> parseHobbyIds(List<String> lines) {
        final List<String> ids = new ArrayList<>();
        for (final String line : lines) {
            if (isAll(line)) {
                ids.add("all");
                continue;
            }
            if (isNone(line)) {
                continue;
            }
            for (final String token : splitComma(line)) {
                final String cleaned = token.startsWith("-") ? token.substring(1).trim() : token;
                if (cleaned.equalsIgnoreCase("idiomas")) {
                    continue;
                }
                ids.add((token.startsWith("-") ? "exclude:" : "") + Translations.toEnglishId(cleaned));
            }
        }
        return ids;
    }

    /**
     * Parses the "IDIOMAS" section: each line is either a named language ("Nombre\tMáxHabla/Escritura"),
     * the {@code "Todas"}/{@code "all"} marker (every language capped the same way), or the anonymous
     * "Idioma Regional" marker (a language slot the player picks freely, see {@link LanguageSlot}).
     */
    private static ParsedCultureLanguages parseLanguageRanks(List<String> lines) {
        final List<CultureLanguageRank> ranks = new ArrayList<>();
        final List<LanguageSlot> optionalLanguages = new ArrayList<>();
        for (final String line : lines) {
            if (isAll(line)) {
                ranks.add(new CultureLanguageRank("all", 10, 10));
                continue;
            }
            if (isNone(line)) {
                continue;
            }
            final String[] columns = line.split("\t");
            if (columns.length >= 2) {
                final String[] pair = columns[1].trim().split("/");
                final int maxSpeaking = Integer.parseInt(pair[0].trim());
                final int maxWriting = Integer.parseInt(pair[1].trim());
                final String name = columns[0].trim();
                if (ANY_CULTURE_LANGUAGE.equalsIgnoreCase(name)) {
                    optionalLanguages.add(new LanguageSlot(0, 0, maxSpeaking, maxWriting));
                } else {
                    ranks.add(new CultureLanguageRank(Translations.toEnglishId(name), maxSpeaking, maxWriting));
                }
            }
        }
        return new ParsedCultureLanguages(ranks, optionalLanguages);
    }

    /** The result of {@link #parseLanguageRanks(List)}: named language ranks, and anonymous optional slots. */
    private record ParsedCultureLanguages(List<CultureLanguageRank> ranks, List<LanguageSlot> optionalLanguages) {
    }

    private static List<CultureTrainingPrice> parseTrainingPrices(List<String> lines) {
        final List<CultureTrainingPrice> prices = new ArrayList<>();
        for (final String line : lines) {
            if (isNone(line)) {
                continue;
            }
            final String[] columns = line.split("\t");
            if (columns.length >= 2) {
                final String rawValue = columns[1].replace("%", "").replace(".", "").replace(",", "").trim();
                prices.add(new CultureTrainingPrice(Translations.toEnglishId(columns[0].trim()), Double.valueOf(rawValue) / 100d));
            }
        }
        return prices;
    }

    private static List<String> splitComma(String line) {
        final List<String> tokens = new ArrayList<>();
        for (final String token : line.split(",")) {
            if (!token.trim().isEmpty()) {
                tokens.add(token.trim());
            }
        }
        return tokens;
    }

    private static String stripBraces(String value) {
        return value.replace("{", "").replace("}", "").trim();
    }

    private static boolean isAll(String line) {
        return line.equalsIgnoreCase("Todas") || line.equalsIgnoreCase("Todos") || line.equalsIgnoreCase("all");
    }

    private static boolean isNone(String line) {
        return line.toLowerCase().contains("ningun") || line.toLowerCase().contains("none");
    }
}
