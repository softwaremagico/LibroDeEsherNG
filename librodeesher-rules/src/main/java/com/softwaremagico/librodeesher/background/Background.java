package com.softwaremagico.librodeesher.background;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.dice.Roll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A character's background: a handful of "background points" the player can spend before play
 * starts on a few extra category/skill ranks, characteristic rolls and history languages.
 *
 * <p>Categories and skills are referenced by their plain id (String, the same id used by {@code
 * CategoryFactory}/{@code SkillFactory}), instead of embedding the rule objects themselves; see
 * {@code LevelUp} for why. The race's optional language slots this background spends points on are
 * tracked by {@code CharacterPlayer}'s decision layer instead of here (see {@code
 * CharacterPlayer#assignOptionalBackgroundLanguage}), unlike the legacy {@code
 * Background#optionalRaceLanguageSelection}.</p>
 */
public class Background {

    private static final int SKILL_BONUS = 10;
    private static final int CATEGORY_BONUS = 5;

    @JsonProperty("categoryIds")
    private List<String> categoryIds = new ArrayList<>();
    @JsonProperty("skillIds")
    private List<String> skillIds = new ArrayList<>();
    @JsonProperty("characteristicUpdates")
    private List<CharacteristicRoll> characteristicUpdates = new ArrayList<>();
    @JsonProperty("languageRanks")
    private Map<String, Integer> languageRanks = new HashMap<>();

    public void setSkillPoint(String skillId, boolean selected) {
        if (selected) {
            if (!isSkillPointSelected(skillId)) {
                skillIds.add(skillId);
            }
        } else {
            skillIds.remove(skillId);
        }
    }

    public void setCategoryPoint(String categoryId, boolean selected) {
        if (selected) {
            if (!isCategoryPointSelected(categoryId)) {
                categoryIds.add(categoryId);
            }
        } else {
            categoryIds.remove(categoryId);
        }
    }

    public boolean isCategoryPointSelected(String categoryId) {
        return categoryIds.contains(categoryId);
    }

    public boolean isSkillPointSelected(String skillId) {
        return skillIds.contains(skillId);
    }

    public Integer getSkillBonus(String skillId) {
        return skillIds.contains(skillId) ? SKILL_BONUS : 0;
    }

    public Integer getCategoryBonus(String categoryId) {
        return categoryIds.contains(categoryId) ? CATEGORY_BONUS : 0;
    }

    public Integer getSpentBackgroundPoints() {
        return skillIds.size() + categoryIds.size() + getCharacteristicUpdatesPoints() + getLanguagesPointCost();
    }

    public CharacteristicRoll addCharacteristicUpdate(CharacteristicAbbreviation abbreviation, Integer currentTemporalValue,
                                                       Integer currentPotentialValue, Roll roll) {
        final CharacteristicRoll characteristicRoll = new CharacteristicRoll(abbreviation, currentTemporalValue,
                currentPotentialValue, roll);
        characteristicUpdates.add(characteristicRoll);
        return characteristicRoll;
    }

    private Integer getCharacteristicUpdatesPoints() {
        return characteristicUpdates.size();
    }

    public List<CharacteristicRoll> getCharacteristicUpdates(CharacteristicAbbreviation abbreviation) {
        final List<CharacteristicRoll> matching = new ArrayList<>();
        for (final CharacteristicRoll characteristicRoll : characteristicUpdates) {
            if (characteristicRoll.getCharacteristicAbbreviation() == abbreviation) {
                matching.add(characteristicRoll);
            }
        }
        return matching;
    }

    public List<CharacteristicRoll> getCharacteristicUpdates() {
        return characteristicUpdates;
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public List<String> getSkillIds() {
        return skillIds;
    }

    public void setSkillIds(List<String> skillIds) {
        this.skillIds = skillIds;
    }

    public void setHistoryLanguageRank(String languageId, int ranks) {
        if (ranks <= 0) {
            languageRanks.remove(languageId);
        } else {
            languageRanks.put(languageId, ranks);
        }
    }

    public int getHistoryLanguageRank(String languageId) {
        return languageRanks.getOrDefault(languageId, 0);
    }

    public int getLanguagesTotalRanksAdded() {
        int total = 0;
        for (final int ranks : languageRanks.values()) {
            total += ranks;
        }
        return total;
    }

    public int getLanguagesPointCost() {
        return (int) Math.ceil(getLanguagesTotalRanksAdded() / 20f);
    }

    public Map<String, Integer> getLanguageRanks() {
        return languageRanks;
    }
}
