package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkBonus;
import com.softwaremagico.librodeesher.perk.PerkBonusKind;
import com.softwaremagico.librodeesher.perk.PerkChoiceGrant;
import com.softwaremagico.librodeesher.perk.PerkChoiceScope;
import com.softwaremagico.librodeesher.perk.PerkGrade;
import com.softwaremagico.librodeesher.perk.PerkType;
import com.softwaremagico.librodeesher.resistance.ResistanceType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * One-shot command line tool that converts the legacy tab-separated {@code talentos/talentos.txt}
 * files into the {@code talentos.xml} files read by {@link com.softwaremagico.librodeesher.perk.PerkFactory}.
 *
 * <h2>Legacy file format</h2>
 * Each non-comment line has 7 tab-separated columns:
 * <pre>Nombre\tCoste\tPermitido\tGrado\tTipo\tBonuses\tDescripcion</pre>
 *
 * <p>Unlike categories and skills, perks do not need any cross-module identity merging: each module's
 * {@code talentos.txt} is self-contained, so this tool simply converts one file into one
 * {@code talentos.xml} per module (only "ManualPersonajes", "NoOficiales" and "RazasSubterraneas"
 * define any perks in the original data).</p>
 */
public final class PerkMigrationTool {

    private static final String PERKS_FOLDER = "talentos";
    private static final String PERKS_FILE = "talentos.txt";
    private static final String OUTPUT_FILE = "perks.xml";

    private PerkMigrationTool() {
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
        final Map<String, String> skillIndex = SkillMigrationTool.buildSkillIndex(sourceRoot);

        int written = 0;
        for (final String module : ModuleManager.getAllModules()) {
            final Path file = modulosDir.resolve(LegacyModules.sourceFolderFor(module)).resolve(PERKS_FOLDER).resolve(PERKS_FILE);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            final List<Perk> perks = readPerksFile(file, idAllocator, categoryIndex, skillIndex);
            XmlMigrationWriter.write(modulesTarget.resolve(module).resolve(OUTPUT_FILE), "perks", "perk", perks);
            written++;
        }
        return written;
    }

    private static List<Perk> readPerksFile(Path file, IdAllocator idAllocator, Map<String, String> categoryIndex,
                                             Map<String, String> skillIndex) throws IOException {
        final List<Perk> perks = new ArrayList<>();
        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            // "-1" keeps a trailing empty "Descripcion" column instead of dropping it.
            final String[] columns = line.split("\t", -1);
            if (columns.length < 7) {
                throw new IllegalStateException(
                        "Malformed perk line in '" + file + "': expected 7 tab-separated columns, got "
                                + columns.length + ": " + line);
            }

            final String spanishName = columns[0].trim();
            final Perk perk = new Perk(idAllocator.idFor(spanishName));
            perk.setName(spanishName, Translations.toEnglish(spanishName));
            perk.setCost(Integer.valueOf(columns[1].trim()));
            perk.setAvailableTo(parseAvailableTo(columns[2]));
            perk.setGrade(PerkGrade.fromTag(columns[3].trim()));
            perk.setType(PerkType.fromTag(columns[4].trim()));
            final ParsedBonuses bonuses = parseBonuses(columns[5].trim(), categoryIndex, skillIndex);
            perk.setBonuses(bonuses.bonuses());
            perk.setChoiceGrants(bonuses.choiceGrants());
            // A handful of rows have a stray tab splitting the description in two (a data typo in the
            // legacy files); join everything from column 6 onwards instead of silently dropping it.
            final String description = String.join(" ", Arrays.copyOfRange(columns, 6, columns.length)).trim();
            perk.setDescription(description, Translations.toEnglish(description));
            perks.add(perk);
        }
        return perks;
    }

    private static List<String> parseAvailableTo(String rawColumn) {
        final String[] tokens = rawColumn.replace(";", ",").split(",");
        final List<String> names = new ArrayList<>();
        for (final String token : tokens) {
            final String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                names.add(Translations.toEnglish(trimmed));
            }
        }
        return Arrays.asList(names.toArray(new String[0]));
    }

    /** The result of {@link #parseBonuses(String, Map, Map)}. */
    private record ParsedBonuses(List<PerkBonus> bonuses, List<PerkChoiceGrant> choiceGrants) {
    }

    /**
     * Parses the "bonuses" column: comma-space-separated entries, each either a {@code "{...}"}
     * choose-N group (see {@link #parseChoiceGrant}) or a single fixed bonus (see {@link
     * #parseDefinedBonus}), matching the legacy {@code PerkFactory#addBonuses} exactly (including its
     * top-level {@code ", "} splitting, safe here since no shipped perk's bonus column has a comma
     * inside a {@code "{...}"} group).
     */
    private static ParsedBonuses parseBonuses(String bonusesColumn, Map<String, String> categoryIndex, Map<String, String> skillIndex) {
        final List<PerkBonus> bonuses = new ArrayList<>();
        final List<PerkChoiceGrant> choiceGrants = new ArrayList<>();
        for (final String entry : bonusesColumn.split(", ")) {
            if (entry.contains("{")) {
                choiceGrants.add(parseChoiceGrant(entry, categoryIndex, skillIndex));
            } else {
                final PerkBonus bonus = parseDefinedBonus(entry, categoryIndex, skillIndex);
                if (bonus != null) {
                    bonuses.add(bonus);
                }
            }
        }
        return new ParsedBonuses(bonuses, choiceGrants);
    }

    /**
     * Parses one {@code "{scope} (bonus)[N]"} entry into a {@link PerkChoiceGrant}, matching the
     * legacy {@code PerkFactory#addListToChooseBonus}: {@code scope} is one of four special markers
     * (any category/weapon category/skill/weapon) or a single, specific category/skill name (see
     * {@link PerkChoiceGrant}'s class javadoc for the one simplification made here).
     */
    private static PerkChoiceGrant parseChoiceGrant(String entry, Map<String, String> categoryIndex, Map<String, String> skillIndex) {
        final String[] set = entry.split("\\(", 2);
        final String scopeText = set[0].replace("{", "").replace("}", "").trim();
        final String rest = set[1]; // e.g. "20)[1]" or "3r)[3]" or "Común)[1]"

        final int optionsToChoose = Integer.parseInt(rest.substring(rest.indexOf('[') + 1, rest.indexOf(']')).trim());
        final String bonusString = rest.substring(0, rest.indexOf(')')).trim();

        final PerkChoiceGrant grant = new PerkChoiceGrant();
        grant.setOptionsToChoose(optionsToChoose);

        final String lowerScope = scopeText.toLowerCase();
        if (lowerScope.contains("cualquier categoría de armas") || lowerScope.contains("cualquier categoria de armas")) {
            grant.setScope(PerkChoiceScope.ANY_WEAPON_CATEGORY);
        } else if (lowerScope.contains("cualquier categoría") || lowerScope.contains("cualquier categoria")) {
            grant.setScope(PerkChoiceScope.ANY_CATEGORY);
        } else if (lowerScope.contains("cualquier arma")) {
            grant.setScope(PerkChoiceScope.ANY_WEAPON_SKILL);
        } else if (lowerScope.contains("cualquier habilidad")) {
            grant.setScope(PerkChoiceScope.ANY_SKILL);
        } else {
            final String categoryId = categoryIndex.get(scopeText);
            final String skillId = skillIndex.get(scopeText);
            if (categoryId != null) {
                grant.setCategoryId(categoryId);
            } else if (skillId != null) {
                grant.setSkillId(skillId);
            } else {
                throw new IllegalStateException("Unknown perk choice scope: '" + scopeText + "'.");
            }
        }

        applyBonusValue(grant::setKind, grant::setValue, bonusString);
        return grant;
    }

    /**
     * Parses one fixed "{@code target (bonus)}" entry into a {@link PerkBonus}, matching the legacy
     * {@code PerkFactory#addDefinedBonus}; {@code null} for the {@code "Ninguno(a)"} marker (a
     * description-only perk with no mechanical bonus at all).
     */
    private static PerkBonus parseDefinedBonus(String entry, Map<String, String> categoryIndex, Map<String, String> skillIndex) {
        final String[] parts = entry.split("\\(", 2);
        final String targetName = parts[0].trim();
        if (targetName.toLowerCase().contains("ningun")) {
            return null;
        }
        final String bonusString = parts[1].substring(0, parts[1].indexOf(')')).trim();

        final PerkBonus bonus = new PerkBonus();
        final String skillId = skillIndex.get(targetName);
        final String categoryId = categoryIndex.get(targetName);
        if (skillId != null) {
            bonus.setSkillId(skillId);
        } else if (categoryId != null) {
            bonus.setCategoryId(categoryId);
        } else if (targetName.startsWith("TR ")) {
            final String realmOrType = targetName.substring("TR ".length()).trim();
            final ResistanceType resistanceType = ResistanceType.fromTag(realmOrType);
            if (resistanceType != null) {
                bonus.setResistanceType(resistanceType);
            } else {
                // E.g. "TR Reino": resistance to the character's own realm of magic, which requires
                // knowing that realm at runtime; not modeled yet (see PerkBonus#getUnresolvedTargetId()).
                bonus.setUnresolvedTargetId(Translations.toEnglishId(realmOrType));
            }
        } else {
            final CharacteristicAbbreviation characteristic = CharacteristicAbbreviation.fromTag(targetName);
            if (characteristic != CharacteristicAbbreviation.NONE) {
                bonus.setCharacteristic(characteristic);
            } else if (targetName.toLowerCase().contains("apariencia")) {
                bonus.setAppearance(true);
            } else if (targetName.equalsIgnoreCase("TA")) {
                bonus.setArmor(true);
            } else if (targetName.toLowerCase().contains("movimiento")) {
                bonus.setMovement(true);
            } else {
                throw new IllegalStateException("Unknown perk bonus target: '" + targetName + "'.");
            }
        }

        applyBonusValue(bonus::setKind, bonus::setValue, bonusString);
        return bonus;
    }

    /**
     * Resolves a bonus value string (e.g. {@code "8"}, {@code "10*"} conditional, {@code "+4/r"}
     * per-rank, or {@code "Común"}/{@code "Restringida"}) into a {@link PerkBonusKind}/value pair,
     * shared by {@link #parseDefinedBonus} and {@link #parseChoiceGrant}. A bare "ranks" marker with
     * no "/" (the legacy {@code extraRanks} case, e.g. a category's "grant N ranks of its skills"
     * flavour, distinct from "N points per rank bought") never appears in the shipped data, so it is
     * treated the same as the "/"-marked per-rank case rather than modeled separately.
     */
    private static void applyBonusValue(java.util.function.Consumer<PerkBonusKind> kindSetter, java.util.function.Consumer<Integer> valueSetter,
                                         String bonusString) {
        final String lower = bonusString.toLowerCase();
        if (lower.contains("común") || lower.contains("comun")) {
            kindSetter.accept(PerkBonusKind.MAKES_COMMON);
            return;
        }
        if (lower.contains("restringid")) {
            kindSetter.accept(PerkBonusKind.MAKES_RESTRICTED);
            return;
        }
        final boolean conditional = bonusString.contains("*");
        final boolean perRank = bonusString.contains("r");
        final int value = Integer.parseInt(bonusString.replace("*", "").replace("r", "").replace("/", "").replace("+", "").trim());
        kindSetter.accept(conditional ? PerkBonusKind.CONDITIONAL : perRank ? PerkBonusKind.PER_RANK : PerkBonusKind.FLAT);
        valueSetter.accept(value);
    }
}
