package com.softwaremagico.librodeesher.pdf.perks;

import com.lowagie.text.pdf.PdfPTable;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.decision.Decision;
import com.softwaremagico.librodeesher.decision.DecisionKey;
import com.softwaremagico.librodeesher.decision.DecisionKind;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.pdf.elements.BaseElement;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkBonus;
import com.softwaremagico.librodeesher.perk.PerkBonusKind;
import com.softwaremagico.librodeesher.perk.PerkChoiceGrant;
import com.softwaremagico.librodeesher.perk.PerkChoiceScope;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.rules.RulesCatalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds the perks and weaknesses section selected for a character. */
public final class PerksTableFactory extends BaseElement {
    private static final float[] WIDTHS = {2.2f, 3.2f, 5.6f};

    private PerksTableFactory() {
        // Only static helpers.
    }

    public static PdfPTable getPerksTable(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final PdfPTable table = new PdfPTable(WIDTHS);
        setTableProperties(table);
        table.addCell(getTitleCell("Perks and Weaknesses", WIDTHS.length));
        table.addCell(getLabelCell("Perk / Weakness"));
        table.addCell(getLabelCell("Bonuses"));
        table.addCell(getLabelCell("Description"));

        final List<PerkRow> rows = new ArrayList<>();
        for (final SelectedPerk selected : characterPlayer.getSelectedPerks()) {
            if (selected.getPerkId() != null) {
                rows.add(new PerkRow("Perk", RulesCatalog.getInstance().getPerk(selected.getPerkId())));
            }
            if (selected.getWeaknessId() != null) {
                rows.add(new PerkRow("Weakness", RulesCatalog.getInstance().getPerk(selected.getWeaknessId())));
            }
        }
        rows.sort(Comparator.comparing(row -> getText(row.perk().getName())));
        for (final PerkRow row : rows) {
            table.addCell(getPlainCell(row.perkType() + ": " + getText(row.perk().getName())));
            table.addCell(getPlainCell(getBonusText(characterPlayer, row.perk())));
            table.addCell(getPlainCell(getText(row.perk().getDescription())));
        }
        return table;
    }

    private static String getBonusText(CharacterPlayer character, Perk perk) {
        final List<String> bonuses = new ArrayList<>();
        for (final PerkBonus bonus : perk.getBonuses()) {
            final String target = bonusTarget(bonus);
            if (bonus.getKind() == PerkBonusKind.MAKES_COMMON) {
                bonuses.add(target + " (Common)");
            } else if (bonus.getKind() == PerkBonusKind.MAKES_RESTRICTED) {
                bonuses.add(target + " (Restricted)");
            } else if (bonus.getValue() != null) {
                bonuses.add(formatBonus(bonus.getValue(), bonus.getKind()) + " to " + target);
            }
        }
        for (int i = 0; i < perk.getChoiceGrants().size(); i++) {
            final PerkChoiceGrant grant = perk.getChoiceGrants().get(i);
            if (grant.getValue() == null) {
                continue;
            }
            final Decision decision = character.getDecisions().get(
                    DecisionKey.characterWide(DecisionKind.PERK_CHOICE, perk.getId(), i));
            final List<String> targets = new ArrayList<>();
            if (decision != null) {
                final boolean categories = isCategoryGrant(grant);
                for (final String option : decision.getSelectedOptions()) {
                    targets.add(elementName(option, categories));
                }
            }
            if (targets.isEmpty()) {
                if (grant.getCategoryId() != null) {
                    targets.add(elementName(grant.getCategoryId(), true));
                } else if (grant.getSkillId() != null) {
                    targets.add(elementName(grant.getSkillId(), false));
                } else {
                    targets.add(scopeName(grant.getScope()));
                }
            }
            final String target = targets.size() == 1 ? targets.get(0) : "{" + String.join(" | ", targets) + "}";
            final String count = grant.getOptionsToChoose() == null || grant.getOptionsToChoose() == 1
                    ? "" : " [" + grant.getOptionsToChoose() + "]";
            bonuses.add(formatBonus(grant.getValue(), grant.getKind()) + " to " + target + count);
        }
        return String.join("; ", bonuses);
    }

    private static String formatBonus(Integer value, PerkBonusKind kind) {
        final String formatted = value > 0 ? "+" + value : value.toString();
        if (kind == PerkBonusKind.CONDITIONAL) {
            return formatted + "*";
        }
        if (kind == PerkBonusKind.PER_RANK) {
            return formatted + "/rank";
        }
        return formatted;
    }

    private static String bonusTarget(PerkBonus bonus) {
        if (bonus.getCategoryId() != null) {
            return elementName(bonus.getCategoryId(), true);
        }
        if (bonus.getSkillId() != null) {
            return elementName(bonus.getSkillId(), false);
        }
        if (bonus.getCharacteristic() != null && bonus.getCharacteristic() != CharacteristicAbbreviation.NONE) {
            return bonus.getCharacteristic().getCode();
        }
        if (bonus.getResistanceType() != null) {
            return bonus.getResistanceType().getTag();
        }
        if (bonus.isAppearance()) {
            return "Appearance";
        }
        if (bonus.isArmor()) {
            return "Armor";
        }
        if (bonus.isMovement()) {
            return "Movement";
        }
        return bonus.getUnresolvedTargetId() == null ? "" : bonus.getUnresolvedTargetId();
    }

    private static String elementName(String id, boolean category) {
        if (id == null || id.isBlank()) {
            return "";
        }
        try {
            return getText(category ? RulesCatalog.getInstance().getCategory(id).getName()
                    : RulesCatalog.getInstance().getSkill(id).getName());
        } catch (InvalidXmlElementException e) {
            return id;
        }
    }

    private static boolean isCategoryGrant(PerkChoiceGrant grant) {
        return grant.getCategoryId() != null || grant.getScope() == PerkChoiceScope.ANY_CATEGORY
                || grant.getScope() == PerkChoiceScope.ANY_WEAPON_CATEGORY;
    }

    private static String scopeName(PerkChoiceScope scope) {
        if (scope == null) {
            return "";
        }
        return switch (scope) {
            case ANY_CATEGORY -> "Any category";
            case ANY_WEAPON_CATEGORY -> "Any weapon category";
            case ANY_SKILL -> "Any skill";
            case ANY_WEAPON_SKILL -> "Any weapon skill";
        };
    }

    private record PerkRow(String perkType, Perk perk) {
    }
}
