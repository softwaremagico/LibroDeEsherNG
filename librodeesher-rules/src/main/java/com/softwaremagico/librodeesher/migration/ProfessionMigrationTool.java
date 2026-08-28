package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.ProfessionBonus;
import com.softwaremagico.librodeesher.profession.ProfessionCategoryCost;
import com.softwaremagico.librodeesher.profession.ProfessionSkillGrant;
import com.softwaremagico.librodeesher.profession.ProfessionTrainingCost;
import com.softwaremagico.librodeesher.profession.ProfessionWeaponCostTier;
import com.softwaremagico.librodeesher.profession.RealmOfMagicGrant;
import com.softwaremagico.librodeesher.training.TrainingType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
 *     (granted outright) with a "choose N from {a;b;c}" / "choose N from category#N" syntax; fully
 *     parsed, see {@link Profession#getCommonSkillIds()}/{@link Profession#getCommonSkillChoices()}.</li>
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
        final Map<String, String> categoryIndex = CategoryMigrationTool.buildCategoryIndex(sourceRoot);

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
                    professions.add(readProfessionFile(file, idAllocator, categoryIndex));
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

    private static Profession readProfessionFile(Path file, IdAllocator idAllocator, Map<String, String> categoryIndex)
            throws IOException {
        final String fileName = file.getFileName().toString();
        final String professionName = fileName.substring(0, fileName.length() - ".txt".length());
        final SectionCursor cursor = new SectionCursor(Files.readAllLines(file, StandardCharsets.UTF_8));

        final Profession profession = new Profession(idAllocator.idFor(professionName));
        profession.setName(professionName, Translations.toEnglish(professionName));
        profession.setCharacteristicPreferences(parseCharacteristicPreferences(cursor.nextSection()));
        profession.setMagicRealms(parseMagicRealms(cursor.nextSection()));
        profession.setBonuses(parseBonuses(cursor.nextSection()));
        final ParsedCategoryCosts categoryCosts = parseCategoryCosts(cursor.nextSection(), categoryIndex);
        profession.setCategoryCosts(categoryCosts.categoryCosts());
        profession.setWeaponCategoryCostTiers(categoryCosts.weaponCostTiers());
        final ParsedSkillSection common = parseSkillSection(cursor.nextSection(), categoryIndex);
        profession.setCommonSkillIds(common.fixedSkillIds());
        profession.setCommonSkillChoices(common.choices());
        final ParsedSkillSection professional = parseSkillSection(cursor.nextSection(), categoryIndex);
        profession.setProfessionalSkillIds(professional.fixedSkillIds());
        profession.setProfessionalSkillChoices(professional.choices());
        final ParsedSkillSection restricted = parseSkillSection(cursor.nextSection(), categoryIndex);
        profession.setRestrictedSkillIds(restricted.fixedSkillIds());
        profession.setRestrictedSkillChoices(restricted.choices());
        profession.setMagicCostsRaw(String.join("\n", cursor.nextSectionOrEmpty()));
        profession.setTrainingCosts(parseTrainingCosts(cursor.nextSectionOrEmpty()));
        return profession;
    }

    /**
     * A small, fixed set of skill-name prefixes used by the "HABILIDADES COMUNES/PROFESIONALES/
     * RESTRINGIDAS" sections' {@code "{<prefix>}#N"} syntax that do not name an actual category
     * (unlike e.g. {@code "Oficios#1"} or {@code "{Conocimiento·Técnico}#1"}): the legacy application
     * resolved these against every skill whose name starts with {@code <prefix>} ({@code
     * SkillFactory#getSkills(String, String)}). Building the full cross-module skill index this would
     * need in general is significant scope on its own (see {@code SkillMigrationTool}), so this
     * hardcodes the one prefix actually used across every shipped profession instead.
     */
    private static final Map<String, List<String>> SKILL_PREFIX_GROUPS = Map.of(
            "Percepción del Entorno", List.of(
                    "perceptionOfTheEnvironmentCiudades", "perceptionOfTheEnvironmentCombat",
                    "perceptionOfTheEnvironmentDurmiendo", "perceptionOfTheEnvironmentExploration"));

    /**
     * Known typos in the "HABILIDADES COMUNES/PROFESIONALES/RESTRINGIDAS" skill names, fixed before
     * translation (rather than left to translate into an accented, un-migratable id): {@code
     * "Advinación"} (twice, in "GuiaMentalismo/Vidente.txt" and "Astrólogo.txt") for "Adivinación",
     * and {@code "MNatemáticas Basícas"} (a doubly-mistyped "Matemáticas Básicas", also in
     * "Astrólogo.txt").
     *
     * <p>A handful of other typos/nonexistent skill names are left untouched instead (e.g. "Conocimiento
     * de lod Círculos", "Hipnosi", "Ritual Mágica", "Estabilización Adreanl", "Primerio Auxilios",
     * "Maestría de los Hechizo", none of which name a real migrated skill even once corrected, since
     * "Círculos"/"Hipnosis"/etc. are not skills anywhere in the shipped data): the resulting ids
     * (already non-accented, so safe to keep) match no real {@code Skill}, which is a faithful match
     * for the legacy behaviour here (its {@code SkillFactory#getSkill(String)} silently created a new,
     * standalone "skill" for any never-before-seen name instead of failing, so these grants were
     * already effectively inert there too).</p>
     */
    private static final Map<String, String> SKILL_NAME_TYPO_FIXES = Map.of(
            "Advinación", "Adivinación",
            "Conocimiento de la Advinación", "Conocimiento de la Adivinación",
            "MNatemáticas Basícas", "Matemáticas Básicas");

    private static String translateSkillName(String rawName) {
        return Translations.toEnglishId(SKILL_NAME_TYPO_FIXES.getOrDefault(rawName, rawName));
    }

    /**
     * Parses a "HABILIDADES COMUNES"/"PROFESIONALES"/"RESTRINGIDAS" section: comma-separated tokens,
     * each either a plain skill name (granted outright) or a choice (a {@code "<X>#N"} entry, or a
     * bracketed {@code "{alt1;alt2}"} group with no explicit {@code "#N"}, defaulting to {@code N=1}
     * like {@link ChoiceGroup} does), where {@code <X>} is a category name (with or without
     * surrounding braces), an explicit {@code {alt1;alt2}} alternative list, or one of {@link
     * #SKILL_PREFIX_GROUPS}.
     */
    private static ParsedSkillSection parseSkillSection(List<String> sectionLines, Map<String, String> categoryIndex) {
        final List<String> fixedSkillIds = new ArrayList<>();
        final List<ProfessionSkillGrant> choices = new ArrayList<>();
        for (final String line : sectionLines) {
            if (isNone(line)) {
                continue;
            }
            for (final String token : splitTopLevelComma(line)) {
                if (token.contains("#") || token.startsWith("{")) {
                    choices.add(parseSkillChoice(token, categoryIndex));
                } else {
                    fixedSkillIds.add(translateSkillName(token));
                }
            }
        }
        return new ParsedSkillSection(fixedSkillIds, choices);
    }

    private static ProfessionSkillGrant parseSkillChoice(String token, Map<String, String> categoryIndex) {
        final int hashIndex = token.lastIndexOf('#');
        final String content = (hashIndex >= 0 ? token.substring(0, hashIndex) : token)
                .replace("{", "").replace("}", "").trim();
        final Integer ranksToChoose = hashIndex >= 0 ? Integer.valueOf(token.substring(hashIndex + 1).trim()) : 1;

        final String categoryId = categoryIndex.get(content);
        if (categoryId != null) {
            return ProfessionSkillGrant.ofCategory(ranksToChoose, categoryId);
        }
        if (content.contains(";")) {
            final List<String> options = new ArrayList<>();
            for (final String option : content.split(";")) {
                options.add(translateSkillName(option.trim()));
            }
            return ProfessionSkillGrant.ofSkillOptions(ranksToChoose, options);
        }
        final List<String> prefixGroup = SKILL_PREFIX_GROUPS.get(content);
        if (prefixGroup != null) {
            return ProfessionSkillGrant.ofSkillOptions(ranksToChoose, prefixGroup);
        }
        throw new IllegalStateException("Unknown category/skill-prefix in profession skill section: '" + content + "'.");
    }

    private static boolean isNone(String line) {
        final String lower = line.toLowerCase();
        return lower.contains("ninguna") || lower.contains("ninguno") || lower.contains("nothing");
    }

    /** Splits on {@code ", "} like the legacy parser did, without splitting inside a {@code {...}} group. */
    private static List<String> splitTopLevelComma(String line) {
        final List<String> tokens = new ArrayList<>();
        int depth = 0;
        final StringBuilder current = new StringBuilder();
        for (final char c : line.toCharArray()) {
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                tokens.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString().trim());
        }
        return tokens;
    }

    /** The result of {@link #parseSkillSection(List, Map)}. */
    private record ParsedSkillSection(List<String> fixedSkillIds, List<ProfessionSkillGrant> choices) {
    }

    private static List<CharacteristicAbbreviation> parseCharacteristicPreferences(List<String> sectionLines) {
        if (sectionLines.isEmpty() || sectionLines.get(0).toLowerCase().contains("indiferente")) {
            return List.of();
        }
        final List<CharacteristicAbbreviation> preferences = new ArrayList<>();
        for (final String token : sectionLines.get(0).split(" ")) {
            if (!token.isBlank()) {
                final CharacteristicAbbreviation abbreviation = CharacteristicAbbreviation.fromTag(token.trim());
                if (abbreviation == CharacteristicAbbreviation.NONE) {
                    throw new IllegalStateException("Unknown characteristic tag: '" + token.trim() + "'.");
                }
                preferences.add(abbreviation);
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
    /**
     * Parses the "REINOS DE MAGIA" section: a comma-separated list of realm grants, each either a
     * single, fixed realm or a {@code "Realm1/Realm2"} choice between several (a "hybrid" profession),
     * matching {@link RealmOfMagicGrant}'s semantics exactly (unlike the previous flattened
     * representation, which lost the distinction between "grants both of these realms" and "grants
     * one of these realms, player's choice").
     */
    private static List<RealmOfMagicGrant> parseMagicRealms(List<String> sectionLines) {
        final List<RealmOfMagicGrant> grants = new ArrayList<>();
        for (final String line : sectionLines) {
            for (final String token : line.split(",\\s*")) {
                final List<RealmOfMagic> options = new ArrayList<>();
                for (final String realmTag : token.split("/")) {
                    if (!realmTag.isBlank()) {
                        options.add(RealmOfMagic.fromTag(realmTag.trim()));
                    }
                }
                if (!options.isEmpty()) {
                    grants.add(new RealmOfMagicGrant(options));
                }
            }
        }
        return grants;
    }

    private static List<ProfessionBonus> parseBonuses(List<String> sectionLines) {
        final List<ProfessionBonus> bonuses = new ArrayList<>();
        for (final String line : sectionLines) {
            final String[] columns = line.split("\t");
            bonuses.add(new ProfessionBonus(Translations.toEnglishId(columns[0].trim()), Integer.valueOf(columns[1].trim())));
        }
        return bonuses;
    }

    private static final String WEAPON_CATEGORY_PREFIX = "Armas·";

    /**
     * Parses "HABILIDADES Y CATEGORÍAS DE HABILIDADES": every line names a real category (resolved
     * via {@code categoryIndex}) except the "Armas·CategoríaN" ones, which are collected separately
     * and sorted cheapest-to-priciest, matching the legacy {@code CategoryCostComparator} (more rank
     * slots first, then ascending by each rank's cost in turn).
     */
    private static ParsedCategoryCosts parseCategoryCosts(List<String> sectionLines, Map<String, String> categoryIndex) {
        final List<ProfessionCategoryCost> categoryCosts = new ArrayList<>();
        final List<List<Integer>> weaponCostTiers = new ArrayList<>();
        for (final String line : sectionLines) {
            final String[] columns = line.split("\t");
            final String name = columns[0].trim();
            final List<Integer> rankCosts = parseRankCosts(columns[1].trim());
            if (name.startsWith(WEAPON_CATEGORY_PREFIX)) {
                weaponCostTiers.add(rankCosts);
            } else {
                final String categoryId = categoryIndex.get(name);
                if (categoryId == null) {
                    throw new IllegalStateException("Unknown category name in profession category costs: '" + name + "'.");
                }
                categoryCosts.add(new ProfessionCategoryCost(categoryId, rankCosts));
            }
        }
        weaponCostTiers.sort(Comparator
                .<List<Integer>>comparingInt(List::size).reversed()
                .thenComparingInt(costs -> costs.get(0))
                .thenComparingInt(costs -> costs.size() > 1 ? costs.get(1) : 0)
                .thenComparingInt(costs -> costs.size() > 2 ? costs.get(2) : 0));
        final List<ProfessionWeaponCostTier> tiers = new ArrayList<>();
        for (final List<Integer> costs : weaponCostTiers) {
            tiers.add(new ProfessionWeaponCostTier(costs));
        }
        return new ParsedCategoryCosts(categoryCosts, tiers);
    }

    private static List<Integer> parseRankCosts(String costString) {
        final List<Integer> costs = new ArrayList<>();
        for (final String cost : costString.split("/")) {
            costs.add(Integer.valueOf(cost.trim()));
        }
        return costs;
    }

    /** The result of {@link #parseCategoryCosts(List, Map)}. */
    private record ParsedCategoryCosts(List<ProfessionCategoryCost> categoryCosts, List<ProfessionWeaponCostTier> weaponCostTiers) {
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
