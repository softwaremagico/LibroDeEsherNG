package com.softwaremagico.librodeesher.pdf.legacy;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.equipment.ObjectBonus;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.language.TranslatedText;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkBonus;
import com.softwaremagico.librodeesher.perk.PerkBonusKind;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.race.RaceSpecial;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Spanish prose blocks and name-shortening helpers the legacy {@code PdfStandardSheet} borrowed
 * from the legacy {@code TxtSheet}, ported onto the NG model.
 *
 * <p>The legacy sheet built its back page ({@code equipmentPage}/{@code addSpecialText}) out of four
 * plain-text blocks concatenated in a fixed order, so the NG sheet reproduces those blocks verbatim
 * here rather than rebuilding them as tables: {@link #exportHistory}, {@link #exportPerks}, {@link
 * #exportSpecials} and {@link #exportItems}.</p>
 */
public final class LegacyTextBlocks {

    private static final String SEPARATOR = "--------------------------------------------------";
    private static final int MAX_FAVOURITE_SKILL_NAME_LENGTH = 27;
    private static final int MAX_FAVOURITE_WEAPON_NAME_LENGTH = 25;

    private LegacyTextBlocks() {
        // Utility class.
    }

    /** The "Trasfondo:" block, empty when the character has no history text. */
    public static String exportHistory(CharacterPlayer character) {
        final String history = character.getHistoryText();
        if (history == null || history.isEmpty()) {
            return "";
        }
        return "Trasfondo:\n" + SEPARATOR + "\n" + history + "\n\n";
    }

    /** The "Talentos:" block (perks then weaknesses), empty when the character has none. */
    public static String exportPerks(CharacterPlayer character) throws InvalidXmlElementException {
        final List<Perk> perks = new ArrayList<>();
        final List<Perk> weaknesses = new ArrayList<>();
        for (final SelectedPerk selected : character.getSelectedPerks()) {
            if (selected.getPerkId() != null) {
                perks.add(RulesCatalog.getInstance().getPerk(selected.getPerkId()));
            }
            if (selected.getWeaknessId() != null) {
                weaknesses.add(RulesCatalog.getInstance().getPerk(selected.getWeaknessId()));
            }
        }
        if (perks.isEmpty() && weaknesses.isEmpty()) {
            return "";
        }
        final StringBuilder text = new StringBuilder("Talentos:\n").append(SEPARATOR).append("\n");
        for (final Perk perk : perks) {
            text.append(perk.getName().getSpanish()).append(":\t ").append(perkLongDescription(perk)).append("\n\n");
        }
        for (final Perk weakness : weaknesses) {
            text.append(weakness.getName().getSpanish()).append(":\t ").append(perkLongDescription(weakness))
                    .append("\n\n");
        }
        return text + "\n\n";
    }

    /**
     * The legacy {@code Perk#getLongDescription()}: the perk's flat and conditional category/skill
     * bonuses ("<name> (<value>), ...", "<name> (<value>*)", ... for the conditional ones, each in
     * the legacy order: flat categories, flat skills, conditional categories, conditional skills)
     * followed by ". " and the free-text description.
     */
    private static String perkLongDescription(Perk perk) throws InvalidXmlElementException {
        final List<PerkBonus> flatCategories = new ArrayList<>();
        final List<PerkBonus> flatSkills = new ArrayList<>();
        final List<PerkBonus> conditionalCategories = new ArrayList<>();
        final List<PerkBonus> conditionalSkills = new ArrayList<>();
        for (final PerkBonus bonus : perk.getBonuses()) {
            if (bonus.getValue() == null) {
                continue;
            }
            final boolean conditional = bonus.getKind() != PerkBonusKind.FLAT;
            if (bonus.getCategoryId() != null) {
                (conditional ? conditionalCategories : flatCategories).add(bonus);
            } else {
                (conditional ? conditionalSkills : flatSkills).add(bonus);
            }
        }
        final String bonuses = perkBonusesBlock(flatCategories)
                + perkBonusesBlock(flatSkills)
                + perkBonusesBlock(conditionalCategories)
                + perkBonusesBlock(conditionalSkills);
        String description = name(perk.getDescription());
        if (!bonuses.isEmpty()) {
            description = bonuses + ". " + description;
        }
        return description;
    }

    private static String perkBonusesBlock(List<PerkBonus> bonuses) {
        if (bonuses.isEmpty()) {
            return "";
        }
        final List<String> parts = new ArrayList<>();
        for (final PerkBonus bonus : bonuses) {
            parts.add(bonusTargetName(bonus) + " (" + bonus.getValue()
                    + (bonus.getKind() != PerkBonusKind.FLAT ? "*)" : ")"));
        }
        parts.sort(String::compareTo);
        return String.join(", ", parts);
    }

    private static String bonusTargetName(PerkBonus bonus) {
        if (bonus.getCategoryId() != null) {
            return elementSpanishName(bonus.getCategoryId(), true);
        }
        return elementSpanishName(bonus.getSkillId(), false);
    }

    /** The Spanish name of a skill/category id, or the raw id when the catalog does not know it. */
    private static String elementSpanishName(String id, boolean category) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        try {
            final TranslatedText name = category
                    ? RulesCatalog.getInstance().getCategory(id).getName()
                    : RulesCatalog.getInstance().getSkill(id).getName();
            if (name != null) {
                final String spanish = name.getSpanish();
                if (spanish != null && !spanish.isEmpty()) {
                    return spanish;
                }
            }
        } catch (InvalidXmlElementException e) {
            // Not enabled/defined: print the raw target name.
        }
        return id;
    }

    /** The "Especiales:" block (race specials), empty when the race grants none. */
    public static String exportSpecials(CharacterPlayer character) throws InvalidXmlElementException {
        final Race race = character.getRace();
        final List<RaceSpecial> specials = race == null ? List.of() : race.getSpecials();
        if (specials.isEmpty()) {
            return "";
        }
        final StringBuilder text = new StringBuilder("Especiales:\n").append(SEPARATOR).append("\n");
        for (final RaceSpecial special : specials) {
            text.append(special.getText()).append("\n\n");
        }
        return text.toString().replace("\t", "  ") + "\n\n";
    }

    /** The "Equipo:" block (magic objects then non-magic equipment), empty when the character has none. */
    public static String exportItems(CharacterPlayer character) throws InvalidXmlElementException {
        final List<MagicObject> magicItems = new ArrayList<>(character.getAllMagicItems());
        final List<Equipment> equipment = new ArrayList<>(character.getAllNotMagicEquipment());
        if (magicItems.isEmpty() && equipment.isEmpty()) {
            return "";
        }
        final StringBuilder text = new StringBuilder("Equipo:\n").append(SEPARATOR).append("\n");

        magicItems.sort(Comparator.comparing(item -> name(item.getName())));
        for (final MagicObject item : magicItems) {
            String line = name(item.getName());
            if (item.getDescription() != null && !name(item.getDescription()).isEmpty()) {
                line += " (" + name(item.getDescription()) + ")";
            }
            if (!item.getBonuses().isEmpty()) {
                line += ": ";
            }
            for (int i = 0; i < item.getBonuses().size(); i++) {
                final ObjectBonus bonus = item.getBonuses().get(i);
                line += bonus.getBonus() + " a " + itemBonusTargetName(bonus);
                if (i < item.getBonuses().size() - 1) {
                    line += ", ";
                }
            }
            text.append(line).append("\n\n");
        }
        if (!magicItems.isEmpty()) {
            text.append("\n");
        }

        // Kept as the legacy wrote it: it built and sorted this copy but iterated the character's
        // own list, so equipment printed in acquisition order rather than by name.
        equipment.sort(Comparator.comparing(item -> name(item.getName())));
        for (final Equipment item : character.getAllNotMagicEquipment()) {
            text.append(name(item.getName())).append(" ").append(name(item.getDescription())).append("\n\n");
        }
        if (!equipment.isEmpty()) {
            text.append("\n");
        }
        return text.toString();
    }

    /**
     * The legacy {@code MagicObject} bonus target, printed as the legacy stored it: its Spanish name
     * ("Espada", "Primeros Auxilios", ...), or "Bonificación Defensiva" for the defensive bonus,
     * which has no skill/category target.
     */
    private static String itemBonusTargetName(ObjectBonus bonus) {
        if (bonus.getType() == BonusType.DEFENSIVE_BONUS) {
            return "Bonificación Defensiva";
        }
        return elementSpanishName(bonus.getBonusName(), bonus.getType() == BonusType.CATEGORY);
    }

    /**
     * The legacy {@code TxtSheet#getNameSpecificLength(String, int)}: the name shortened (see {@link
     * #reduceName(String)}) and then padded with spaces, or cut to {@code length - 1} characters
     * when it is too long.
     */
    public static String getNameOfLength(String text, int length) {
        String name = reduceName(text);
        if (length > name.length()) {
            name = name + " ".repeat(length - name.length());
        } else {
            name = name.substring(0, length - 1);
        }
        return name;
    }

    /** {@link #getNameOfLength(String, int)} for a favourite attack name ({@code length} 25). */
    public static String getWeaponNameOfLength(String text) {
        return getNameOfLength(text, MAX_FAVOURITE_WEAPON_NAME_LENGTH);
    }

    /** {@link #getNameOfLength(String, int)} for a favourite skill name ({@code length} 27). */
    public static String getFavouriteSkillNameOfLength(String text) {
        return getNameOfLength(text, MAX_FAVOURITE_SKILL_NAME_LENGTH);
    }

    /**
     * The characteristics a category is developed with, as the short column of the category table
     * showed them: the two-letter abbreviations joined with a slash.
     */
    public static String characteristicTags(List<CharacteristicAbbreviation> characteristics) {
        final StringBuilder tags = new StringBuilder();
        for (final CharacteristicAbbreviation characteristic : characteristics) {
            if (tags.length() > 0) {
                tags.append("/");
            }
            tags.append(characteristic.getTag());
        }
        return tags.toString();
    }

    private static String reduceName(String currentName) {
        if (currentName == null) {
            return null;
        }
        return currentName.replace("Conocimiento", "Con.").replace("Supervivencia", "Superv.")
                .replace("Percepción", "Per.");
    }

    /** The Spanish text of a translated element, as the printed sheet showed it. */
    static String name(TranslatedText text) {
        return text == null ? "" : text.getSpanish();
    }

    static String name(String text) {
        return text == null ? "" : text;
    }
}
