package com.softwaremagico.librodeesher.pdf.skills;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds the legacy sheet's category and skill values from the NG character state. */
public final class SkillsTableFactory extends BaseElement {
    private static final float[] WIDTHS = {2f, 3f, 1f, 1f, 1f, 1f, 1f};

    private SkillsTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getSkillsTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        return getSkillsTable(characterPlayer, false);
    }

    /** Builds skills grouped by category, or as one global alphabetical list when requested. */
    public static PdfPTable getSkillsTable(CharacterPlayer characterPlayer, boolean alphabetically)
            throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);
        table.addCell(getTitleCell("Categories and Skills", WIDTHS.length));
        table.addCell(getLabelCell("Category"));
        table.addCell(getLabelCell("Skill"));
        table.addCell(getLabelCell("Category ranks"));
        table.addCell(getLabelCell("Skill ranks"));
        table.addCell(getLabelCell("Total bonus"));
        table.addCell(getLabelCell("Next cost"));
        table.addCell(getLabelCell("Max ranks"));

        final List<Category> categories = new ArrayList<>(RulesCatalog.getInstance().getCategories());
        categories.sort(Comparator.comparing(category -> getText(category.getName())));
        if (alphabetically) {
            addAlphabeticalSkills(table, characterPlayer, categories);
            return table;
        }
        for (final Category category : categories) {
            addCategorySkills(table, characterPlayer, category);
        }
        return table;
    }

    private static void addAlphabeticalSkills(PdfPTable table, CharacterPlayer characterPlayer, List<Category> categories)
            throws InvalidXmlElementException {
        final List<Skill> skills = new ArrayList<>();
        for (final Skill skill : RulesCatalog.getInstance().getSkills()) {
            if (characterPlayer.isSkillEnabled(skill)) {
                skills.add(skill);
            }
        }
        skills.sort(Comparator.comparing(skill -> getText(skill.getName())));
        for (final Skill skill : skills) {
            final Category category = categories.stream().filter(candidate -> candidate.getId().equals(skill.getCategoryId()))
                    .findFirst().orElse(null);
            if (category == null) {
                continue;
            }
            table.addCell(getPlainCell(getText(category.getName())));
            table.addCell(getPlainCell(getText(skill.getName())));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getCategoryTotalRanks(category.getId()))));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getSkillTotalRanks(skill.getId()))));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getSkillTotalBonus(category, skill.getId()))));
            addCategoryDevelopmentCells(table, characterPlayer, category, false);
        }
    }

    private static void addCategorySkills(PdfPTable table, CharacterPlayer characterPlayer, Category category)
            throws InvalidXmlElementException {
        final List<Skill> skills = new ArrayList<>();
        for (final Skill skill : RulesCatalog.getInstance().getSkills()) {
            if (!category.getId().equals(skill.getCategoryId())) {
                continue;
            }
            if (characterPlayer.isSkillEnabled(skill)) {
                skills.add(skill);
            }
        }
        skills.sort(Comparator.comparing(skill -> getText(skill.getName())));

        if (skills.isEmpty()) {
            table.addCell(getPlainCell(getText(category.getName())));
            table.addCell(getPlainCell(""));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getCategoryTotalRanks(category.getId()))));
            table.addCell(getValueCell(""));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getCategoryTotalBonus(category))));
            addCategoryDevelopmentCells(table, characterPlayer, category, true);
            return;
        }

        for (int index = 0; index < skills.size(); index++) {
            final Skill skill = skills.get(index);
            table.addCell(getPlainCell(index == 0 ? getText(category.getName()) : ""));
            table.addCell(getPlainCell(getText(skill.getName())));
            table.addCell(getValueCell(index == 0 ? String.valueOf(characterPlayer.getCategoryTotalRanks(category.getId())) : ""));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getSkillTotalRanks(skill.getId()))));
            table.addCell(getValueCell(String.valueOf(characterPlayer.getSkillTotalBonus(category, skill.getId()))));
            addCategoryDevelopmentCells(table, characterPlayer, category, index == 0);
            for (final String specializationId : characterPlayer.getSkillSpecializations(skill.getId())) {
                table.addCell(getPlainCell(""));
                table.addCell(getPlainCell("  " + specializationId));
                table.addCell(getValueCell(""));
                table.addCell(getValueCell(String.valueOf(characterPlayer.getSpecializedSkillRanks(skill))));
                table.addCell(getValueCell(String.valueOf(characterPlayer.getSpecializedSkillTotalBonus(category, skill.getId()))));
                table.addCell(getValueCell(""));
                table.addCell(getValueCell(""));
            }
        }
    }

    private static void addCategoryDevelopmentCells(PdfPTable table, CharacterPlayer characterPlayer, Category category,
                                                     boolean includeValues) throws InvalidXmlElementException {
        if (!includeValues) {
            table.addCell(getValueCell(""));
            table.addCell(getValueCell(""));
            return;
        }
        final int currentRanks = characterPlayer.getCurrentLevel().getCategoryRanks(category.getId());
        final Integer cost = characterPlayer.getCategoryDevelopmentCost(category.getId(), currentRanks);
        table.addCell(getValueCell(cost == null ? "" : String.valueOf(cost)));
        table.addCell(getValueCell(String.valueOf(characterPlayer.getMaximumCategoryRanksThisLevel(category.getId()))));
    }
}
