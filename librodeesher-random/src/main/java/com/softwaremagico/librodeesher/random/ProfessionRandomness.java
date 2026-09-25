package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryType;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.MagicListType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;

import java.util.List;

/**
 * How much the character's profession favours a skill, mirroring the legacy {@code
 * ProfessionRandomness#preferredSkillByProfession} on the id-based NG model. Spell lists are
 * classified exactly as in the legacy {@code MagicListType.BASIC/OPEN/CLOSED} branches (NG
 * {@link MagicListType} via {@link CharacterPlayer#classifySpellList(String)}), weapon/PD/PPD/armour
 * rules reuse the NG category types and ids; the legacy "aimed spells" elementalist special case
 * targeted Spanish skill names without an NG equivalent and is dropped.
 */
public final class ProfessionRandomness {

    private static final String COMMUNICATION_CATEGORY = "communication";

    private ProfessionRandomness() {
        // Utility class.
    }

    static final int NEVER = -100;
    static final int ADRENAL_PENALTY = -50;
    static final int MONK_PENALTY = -100;

    /**
     * Obtains a ponderation of the preference of a skill by profession, in the legacy [-100..100]
     * band.
     */
    public static int preferredSkillByProfession(CharacterPlayer characterPlayer, Skill skill, int specializationLevel)
            throws InvalidXmlElementException {
        final MagicListType listType = classify(characterPlayer, skill);

        // For spellcasters.
        if (characterPlayer.isWizard() && listType != null) {
            if (listType == MagicListType.BASIC) {
                // At least a basic list per level.
                if (spellRanks(characterPlayer, skill) < characterPlayer.getLevels().size() + specializationLevel + 1) {
                    if (skillsWithNewRanksInSpellList(characterPlayer, skill).size() < specializationLevel) {
                        return characterPlayer.isSemiWizard() ? 45 : 30;
                    } else {
                        return characterPlayer.isSemiWizard() ? 25 : 15;
                    }
                } else {
                    return NEVER;
                }
            }
            if (listType == MagicListType.OPEN) {
                if (skillsWithNewRanksInSpellList(characterPlayer, skill).size() < specializationLevel) {
                    if (spellRanks(characterPlayer, skill) < characterPlayer.getLevel() + specializationLevel + 1) {
                        if (skillsWithNewRanksInSpellList(characterPlayer, skill).isEmpty()) {
                            return characterPlayer.isSemiWizard() ? 15 : 5;
                        }
                        return 10;
                    } else {
                        return NEVER;
                    }
                }
            }
            if (listType == MagicListType.CLOSED) {
                if (spellsWithRanks(characterPlayer, skill).size() < specializationLevel) {
                    if (spellRanks(characterPlayer, skill) < characterPlayer.getLevel() + specializationLevel + 1) {
                        if (skillsWithNewRanksInSpellList(characterPlayer, skill).isEmpty()) {
                            return 20;
                        }
                        return 10;
                    } else {
                        return NEVER;
                    }
                }
            }
            if (isPowerPointDevelopment(skill)) {
                if (currentLevelRanks(characterPlayer, skill) == 0) {
                    return 50;
                }
                // Needs enough power points.
                for (final String spellListId : characterPlayer.getAvailableSpellListIds()) {
                    for (final String spellId : characterPlayer.getSpellListSkillIds(spellListId)) {
                        final Skill spell = RulesCatalog.getInstance().getSkill(spellId);
                        if (characterPlayer.getSkillTotalBonus(spell) < characterPlayer.getSkillTotalBonus(skill)) {
                            return 100;
                        }
                    }
                }
                // Add some extra PPD.
                if (realRanks(characterPlayer, skill) + currentLevelRanks(characterPlayer, skill) < characterPlayer.getLevel()) {
                    return 10 * (characterPlayer.getLevel() - characterPlayer.getCategoryTotalRanks(skill.getCategoryId())
                            + currentLevelRanks(characterPlayer, skill));
                }
            }
        }

        // Adrenal movements are for monks only, and non-monks learn at most one martial art per level.
        if (!isMonk(characterPlayer)) {
            if (skill.getId().toLowerCase().contains("adrenal")) {
                return ADRENAL_PENALTY;
            }
            if (skill.getCategoryId().startsWith("martialArts")
                    && skillsWithNewRanksInCategory(characterPlayer, skill).size() > 0
                    && getFirstRankCost(characterPlayer, skill) > 3) {
                return MONK_PENALTY;
            }
        }

        // For warriors.
        if (characterPlayer.isFighter()) {
            if (CharacterPlayer.isWeaponCategoryId(skill.getCategoryId())
                    && characterPlayer.getSkillCurrentLevelRanks(skill.getId()) == 0
                    && getFirstRankCost(characterPlayer, skill) < 2) {
                if (skillsWithNewRanksInCategory(characterPlayer, skill).size() < 2) {
                    return 20;
                }
                if (skillsWithNewRanksInCategory(characterPlayer, skill).size() < 3) {
                    return 10;
                }
            }

            if (isPhysicalDevelopment(skill) && realRanks(characterPlayer, skill) < 10
                    && currentLevelRanks(characterPlayer, skill) == 0) {
                return 20;
            }

            if (isPowerPointDevelopment(skill)) {
                final int maxOpen = characterPlayer.getMaximumMagicListRanksPerLevel(MagicListType.OPEN);
                final int maxClosed = characterPlayer.getMaximumMagicListRanksPerLevel(MagicListType.CLOSED);
                if (characterPlayer.getSkillTotalBonus(skill) < Math.max(maxOpen, maxClosed)) {
                    return Math.min(10 * characterPlayer.getLevel(), 50);
                }
            }

            // Armours with ranks must keep up with the category's total bonus.
            if (isArmour(skill) && characterPlayer.getSkillRealRanks(skill) > 0) {
                final Category category = RulesCatalog.getInstance().getCategory(skill.getCategoryId());
                if ("armorLight".equals(skill.getCategoryId())
                        && characterPlayer.getCategoryTotalBonus(category) < 10 && getFirstRankCost(characterPlayer, skill) < 3) {
                    return 20;
                }
                if ("armorMiddle".equals(skill.getCategoryId())
                        && characterPlayer.getCategoryTotalBonus(category) < 20 && getFirstRankCost(characterPlayer, skill) < 4) {
                    return 20;
                }
                if ("armorHeavy".equals(skill.getCategoryId())
                        && characterPlayer.getCategoryTotalBonus(category) < 30 && getFirstRankCost(characterPlayer, skill) < 4) {
                    return 20;
                }
            }
        }
        return 0;
    }

    /** Categories with preferred skills also have a bonus. */
    public static int preferredCategoryByProfession(CharacterPlayer characterPlayer, Category category,
            int specializationLevel) throws InvalidXmlElementException {
        for (final String skillId : category.getSkills()) {
            final Skill skill;
            try {
                skill = RulesCatalog.getInstance().getSkill(skillId);
            } catch (final InvalidXmlElementException e) {
                continue;
            }
            if (preferredSkillByProfession(characterPlayer, skill, specializationLevel) > 0) {
                return 20;
            }
        }
        return 0;
    }

    private static MagicListType classify(CharacterPlayer characterPlayer, Skill skill) {
        if (!characterPlayer.isSpellSkill(skill.getId())) {
            return null;
        }
        try {
            return characterPlayer.classifySpellList(skill.getCategoryId());
        } catch (final InvalidXmlElementException e) {
            return null;
        }
    }

    /**
     * Ranks of the skill bought so far: a spell ranks up as its whole list; single-track categories
     * (PD, PPD, skill-less) as the category itself; anything else as a regular skill.
     */
    private static int realRanks(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        if (characterPlayer.isSpellSkill(skill.getId())) {
            return characterPlayer.getSpellListTotalRanks(skill.getCategoryId());
        }
        if (isSingleTrackCategory(skill)) {
            return characterPlayer.getCategoryTotalRanks(skill.getCategoryId());
        }
        return characterPlayer.getSkillRealRanks(skill);
    }

    private static int currentLevelRanks(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        if (characterPlayer.isSpellSkill(skill.getId())) {
            return characterPlayer.getCurrentLevel().getSpellListRanks(skill.getCategoryId());
        }
        if (isSingleTrackCategory(skill)) {
            return characterPlayer.getCurrentLevelCategoryRanks(skill.getCategoryId());
        }
        return characterPlayer.getSkillCurrentLevelRanks(skill.getId());
    }

    private static boolean isSingleTrackCategory(Skill skill) throws InvalidXmlElementException {
        final CategoryType type = RulesCatalog.getInstance().getCategory(skill.getCategoryId()).getType();
        return type == CategoryType.PD || type == CategoryType.PPD;
    }

    private static int spellRanks(CharacterPlayer characterPlayer, Skill skill) {
        return characterPlayer.getSpellListTotalRanks(skill.getCategoryId());
    }

    private static List<String> skillsWithNewRanksInSpellList(CharacterPlayer characterPlayer, Skill skill)
            throws InvalidXmlElementException {
        if (characterPlayer.getCurrentLevel().getSpellListRanks(skill.getCategoryId()) > 0) {
            return characterPlayer.getSpellListSkillIds(skill.getCategoryId());
        }
        return List.of();
    }

    private static List<String> spellsWithRanks(CharacterPlayer characterPlayer, Skill skill)
            throws InvalidXmlElementException {
        if (characterPlayer.getSpellListTotalRanks(skill.getCategoryId()) > 0) {
            return characterPlayer.getSpellListSkillIds(skill.getCategoryId());
        }
        return List.of();
    }

    private static List<String> skillsWithNewRanksInCategory(CharacterPlayer characterPlayer, Skill skill)
            throws InvalidXmlElementException {
        final Category category = RulesCatalog.getInstance().getCategory(skill.getCategoryId());
        return characterPlayer.getCategorySkillsWithNewRanks(category);
    }

    private static Integer getFirstRankCost(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        return characterPlayer.getCategoryDevelopmentCost(skill.getCategoryId(), 0);
    }

    private static boolean isPowerPointDevelopment(Skill skill) throws InvalidXmlElementException {
        return RulesCatalog.getInstance().getCategory(skill.getCategoryId()).getType() == CategoryType.PPD;
    }

    private static boolean isPhysicalDevelopment(Skill skill) throws InvalidXmlElementException {
        return RulesCatalog.getInstance().getCategory(skill.getCategoryId()).getType() == CategoryType.PD;
    }

    private static boolean isArmour(Skill skill) {
        return "armorLight".equals(skill.getCategoryId()) || "armorMiddle".equals(skill.getCategoryId())
                || "armorHeavy".equals(skill.getCategoryId());
    }

    private static boolean isMonk(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final var profession = characterPlayer.getProfession();
        return profession != null && profession.getId().toLowerCase().contains("monk");
    }
}