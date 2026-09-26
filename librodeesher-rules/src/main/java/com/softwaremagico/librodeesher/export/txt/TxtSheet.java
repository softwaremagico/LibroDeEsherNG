package com.softwaremagico.librodeesher.export.txt;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.Characteristic;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.equipment.MagicObject;
import com.softwaremagico.librodeesher.equipment.ObjectBonus;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.language.TranslatedText;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.race.RaceSpecial;
import com.softwaremagico.librodeesher.resistance.ResistanceType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.training.Training;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The whole character sheet as plain text, the legacy {@code TxtSheet}'s standard export
 * ({@code exportSheet/getCharacterStandardSheetAsText}): name/race/culture/profession/trainings,
 * the ten characteristics, the nine resistance rolls, every category and its skills (with ranks and
 * total bonuses, plus specializations), history, talents, race specials and equipment. It renders
 * exactly the same state the PDF sheet does (see {@code
 * com.softwaremagico.librodeesher.pdf.skills.SkillsTableFactory}), in a fixed-width, plain-text,
 * copy-paste and diff friendly layout.
 */
public final class TxtSheet {

	private static final String SEPARATOR = "------------------------------------------------------------------------";

	private TxtSheet() {
		// Utility class.
	}

	/** The character sheet as one plain-text document, or an empty string for a null character. */
	public static String getCharacterStandardSheetAsText(CharacterPlayer character) throws InvalidXmlElementException {
		if (character == null) {
			return "";
		}
		return header(character) + "\n\n" + characteristics(character) + "\n\n" + resistances(character) + "\n\n"
				+ skills(character) + "\n\n" + history(character) + talents(character) + specials(character)
				+ equipment(character);
	}

	/** Writes the character sheet to {@code path} (a ".txt" extension is appended if missing). */
	public static void createFile(CharacterPlayer character, Path path)
			throws InvalidXmlElementException, IOException {
		final Path target = path.toString().endsWith(".txt") ? path : Path.of(path + ".txt");
		Files.writeString(target, getCharacterStandardSheetAsText(character), StandardCharsets.UTF_8);
	}

	private static String header(CharacterPlayer character) throws InvalidXmlElementException {
		final StringBuilder text = new StringBuilder(character.getName() + "\tLevel " + character.getLevel() + "\n");
		text.append(raceName(character)).append(" (").append(cultureName(character)).append(")\n");
		text.append(professionName(character));
		final List<String> trainings = selectedTrainingNames(character);
		if (!trainings.isEmpty()) {
			text.append(" (").append(String.join(", ", trainings)).append(")");
		}
		return text.append("\n").toString();
	}

	private static String characteristics(CharacterPlayer character) throws InvalidXmlElementException {
		final String format = "%1$-8s %2$-8s %3$-8s %4$-8s %5$-8s %6$-8s %7$-8s%n";
		final StringBuilder text = new StringBuilder();
		text.append(String.format(format, "Caract", "Temp", "Pot", "Tot", "Raza", "Esp", "Total"));
		text.append(SEPARATOR).append("\n");
		for (final Characteristic characteristic : Characteristics.getCharacteristics()) {
			final CharacteristicAbbreviation abbreviation = characteristic.getAbbreviation();
			text.append(String.format(format, abbreviation.getCode(),
					value(character.getCharacteristicTemporalValue(abbreviation)),
					value(character.getCharacteristicPotentialValue(abbreviation)),
					value(character.getCharacteristicTemporalBonus(abbreviation)),
					value(character.getCharacteristicRaceBonus(abbreviation)),
					value(character.getPerkCharacteristicBonus(abbreviation)),
					value(character.getCharacteristicTotalBonus(abbreviation))));
		}
		return text.toString();
	}

	private static String resistances(CharacterPlayer character) throws InvalidXmlElementException {
		final String format = "%1$-3s %2$-22s %3$-8s%n";
		final StringBuilder text = new StringBuilder("Resistance rolls\n");
		text.append(SEPARATOR).append("\n");
		for (final ResistanceType type : ResistanceType.values()) {
			text.append(String.format(format, type.getCode(), type.name(),
					value(character.getResistanceTotalBonus(type))));
		}
		return text.toString();
	}

	private static String skills(CharacterPlayer character) throws InvalidXmlElementException {
		final List<Category> categories = new ArrayList<>(RulesCatalog.getInstance().getCategories());
		categories.sort(Comparator.comparing(category -> text(category.getName())));

		final StringBuilder text = new StringBuilder("Categories and skills\n");
		text.append(SEPARATOR).append("\n");
		for (final Category category : categories) {
			text.append(String.format("%1$-36s %2$-10s %3$-10s %4$-8s %5$-8s%n",
					text(category.getName()), "Ranks", "Bonus", "Cost", "Max"));
			text.append(String.format("%1$-36s %2$-10s %3$-10s %4$-8s %5$-8s%n",
					text(category.getName()), value(character.getCategoryTotalRanks(category.getId())),
					value(character.getCategoryTotalBonus(category)), nextCategoryCost(character, category),
					value(character.getMaximumCategoryRanksThisLevel(category.getId()))));
			for (final Skill skill : enabledSkills(character, category)) {
				text.append(String.format("%1$-36s %2$-10s %3$-10s%n", "  *  " + text(skill.getName()),
						value(character.getSkillTotalRanks(skill.getId())),
						value(character.getSkillTotalBonus(category, skill.getId()))));
				for (final String specializationId : character.getSkillSpecializations(skill.getId())) {
					text.append(String.format("%1$-36s %2$-10s %3$-10s%n", "     -> " + specializationId,
							value(character.getSpecializedSkillRanks(skill)),
							value(character.getSpecializedSkillTotalBonus(category, skill.getId()))));
				}
			}
			text.append("\n");
		}
		return text.toString();
	}

	private static String history(CharacterPlayer character) {
		final String historyText = character.getHistoryText();
		if (historyText == null || historyText.isBlank()) {
			return "";
		}
		return "History\n" + SEPARATOR + "\n" + historyText + "\n\n";
	}

	private static String talents(CharacterPlayer character) throws InvalidXmlElementException {
		final List<SelectedPerk> selectedPerks = character.getSelectedPerks();
		if (selectedPerks.isEmpty()) {
			return "";
		}
		final StringBuilder text = new StringBuilder("Talents\n");
		text.append(SEPARATOR).append("\n");
		for (final SelectedPerk selectedPerk : selectedPerks) {
			final Perk perk = RulesCatalog.getInstance().getPerk(selectedPerk.getPerkId());
			if (perk == null) {
				continue;
			}
			text.append(text(perk.getName())).append(": ")
					.append(text(perk.getDescription())).append("\n");
			if (selectedPerk.getWeaknessId() != null) {
				final Perk weakness = RulesCatalog.getInstance().getPerk(selectedPerk.getWeaknessId());
				text.append("   (weakness: ").append(weakness == null ? selectedPerk.getWeaknessId()
						: text(weakness.getName())).append(")\n");
			}
		}
		return text.append("\n").toString();
	}

	private static String specials(CharacterPlayer character) throws InvalidXmlElementException {
		final Race race = RulesCatalog.getInstance().getRace(character.getRaceId());
		if (race == null || race.getSpecials().isEmpty()) {
			return "";
		}
		final StringBuilder text = new StringBuilder("Race specials\n");
		text.append(SEPARATOR).append("\n");
		for (final RaceSpecial special : race.getSpecials()) {
			text.append(text(special.getText()));
			if (special.getPoints() != null && special.getPoints() != 0) {
				text.append(" (").append(special.getPoints()).append(" points)");
			}
			text.append("\n\n");
		}
		return text.append("\n").toString();
	}

	private static String equipment(CharacterPlayer character) {
		if (character.getAllMagicItems().isEmpty() && character.getAllNotMagicEquipment().isEmpty()) {
			return "";
		}
		final StringBuilder text = new StringBuilder("Equipment\n");
		text.append(SEPARATOR).append("\n");
		final List<MagicObject> magicItems = new ArrayList<>(character.getAllMagicItems());
		magicItems.sort(Comparator.comparing(item -> text(item.getName())));
		for (final MagicObject magicItem : magicItems) {
			text.append(text(magicItem.getName()));
			if (magicItem.getDescription() != null && !text(magicItem.getDescription()).isEmpty()) {
				text.append(" (").append(text(magicItem.getDescription())).append(")");
			}
			if (!magicItem.getBonuses().isEmpty()) {
				text.append(": ");
			}
			final List<String> bonuses = new ArrayList<>();
			for (final ObjectBonus bonus : magicItem.getBonuses()) {
				bonuses.add("+" + bonus.getBonus() + " to " + (bonus.getBonusName() == null
						? bonus.getType().name() : bonus.getBonusName()));
			}
			text.append(String.join(", ", bonuses));
			text.append("\n\n");
		}
		final List<Equipment> equipment = new ArrayList<>(character.getAllNotMagicEquipment());
		equipment.sort(Comparator.comparing(item -> text(item.getName())));
		for (final Equipment item : equipment) {
			text.append(text(item.getName()));
			if (item.getDescription() != null && !text(item.getDescription()).isEmpty()) {
				text.append(" ").append(text(item.getDescription()));
			}
			text.append("\n\n");
		}
		return text.append("\n").toString();
	}

	private static List<Skill> enabledSkills(CharacterPlayer character, Category category)
			throws InvalidXmlElementException {
		final List<Skill> skills = new ArrayList<>();
		for (final Skill skill : RulesCatalog.getInstance().getSkills()) {
			if (category.getId().equals(skill.getCategoryId()) && character.isSkillEnabled(skill)) {
				skills.add(skill);
			}
		}
		skills.sort(Comparator.comparing(skill -> text(skill.getName())));
		return skills;
	}

	private static List<String> selectedTrainingNames(CharacterPlayer character) throws InvalidXmlElementException {
		final List<String> names = new ArrayList<>();
		for (final String trainingId : character.getSelectedTrainingIds()) {
			final Training training = RulesCatalog.getInstance().getTraining(trainingId);
			if (training != null) {
				names.add(text(training.getName()));
			}
		}
		return names;
	}

	private static String nextCategoryCost(CharacterPlayer character, Category category)
			throws InvalidXmlElementException {
		final int currentRanks = character.getCurrentLevel().getCategoryRanks(category.getId());
		final Integer cost = character.getCategoryDevelopmentCost(category.getId(), currentRanks);
		return cost == null ? "" : "/" + cost;
	}

	private static String raceName(CharacterPlayer character) throws InvalidXmlElementException {
		final Race race = RulesCatalog.getInstance().getRace(character.getRaceId());
		return race == null ? "Unknown Race" : text(race.getName());
	}

	private static String cultureName(CharacterPlayer character) throws InvalidXmlElementException {
		final Culture culture = RulesCatalog.getInstance().getCulture(character.getCultureId());
		return culture == null ? "Unknown Culture" : text(culture.getName());
	}

	private static String professionName(CharacterPlayer character) throws InvalidXmlElementException {
		final Profession profession = RulesCatalog.getInstance().getProfession(character.getProfessionId());
		return profession == null ? "Unknown profession" : text(profession.getName());
	}

	private static String value(Integer value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static String text(TranslatedText translatedText) {
		if (translatedText == null) {
			return "";
		}
		final String translated = translatedText.getTranslatedText();
		return translated == null ? "" : translated;
	}
}