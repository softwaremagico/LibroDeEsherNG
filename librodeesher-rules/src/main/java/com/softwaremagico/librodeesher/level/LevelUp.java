package com.softwaremagico.librodeesher.level;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.dice.Roll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything a character developed in a single level: category/skill ranks bought, characteristic
 * rolls, age modifications suffered and a handful of per-level bookkeeping sets used to compute
 * development point costs (e.g. the discount for a "generalized" skill, or the multiplier that
 * kicks in after learning too many spell lists in one level).
 *
 * <p>Every reference to a category/skill/training is kept as its plain id (String), the same id used
 * by {@code CategoryFactory}/{@code SkillFactory}/{@code TrainingFactory}, instead of embedding the
 * rule objects themselves; this mirrors the legacy class (which stored plain names) and keeps this
 * class independent from the character-state layer (a future {@code CharacterSkill}/{@code
 * TrainingDecision} equivalent) that has not been ported yet.</p>
 */
public class LevelUp {

    @JsonProperty("categoryRanks")
    private Map<String, Integer> categoryRanks = new HashMap<>();
    @JsonProperty("skillRanks")
    private Map<String, Integer> skillRanks = new HashMap<>();
    @JsonProperty("generalizedSkills")
    private Set<String> generalizedSkills = new HashSet<>();
    /** Spell skill ids leveled up this level, in the order they were first touched (see {@link #getSpellRankMultiplier}). */
    @JsonProperty("spellsUpdated")
    private List<String> spellsUpdated = new ArrayList<>();
    @JsonProperty("trainings")
    private List<String> trainings = new ArrayList<>();
    @JsonProperty("skillSpecializations")
    private Set<String> skillSpecializations = new HashSet<>();
    @JsonProperty("favouriteSkills")
    private Set<String> favouriteSkills = new HashSet<>();
    @JsonProperty("characteristicUpdates")
    private List<CharacteristicRoll> characteristicUpdates = new ArrayList<>();
    @JsonProperty("ageModifications")
    private List<AgeModification> ageModifications = new ArrayList<>();

    public Integer getCategoryRanks(String categoryId) {
        return categoryRanks.getOrDefault(categoryId, 0);
    }

    public void setCategoryRanks(String categoryId, Integer ranks) {
        if (ranks <= 0) {
            categoryRanks.remove(categoryId);
        } else {
            categoryRanks.put(categoryId, ranks);
        }
    }

    /** Adds {@code ranks} to whatever ranks in {@code categoryId} were already bought this level. */
    public void addCategoryRanks(String categoryId, Integer ranks) {
        setCategoryRanks(categoryId, getCategoryRanks(categoryId) + ranks);
    }

    public List<String> getCategoriesWithRanks() {
        return new ArrayList<>(categoryRanks.keySet());
    }

    public Map<String, Integer> getCategoryRanks() {
        return categoryRanks;
    }

    public void setCategoryRanks(Map<String, Integer> categoryRanks) {
        this.categoryRanks = categoryRanks;
    }

    public Integer getSkillRanks(String skillId) {
        return skillRanks.getOrDefault(skillId, 0);
    }

    /**
     * Sets how many ranks were bought this level for a skill. {@code isSpellSkill} must be {@code
     * true} for skills belonging to a spell list category, so they are tracked by {@link
     * #getSpellRankMultiplier} (learning many spell lists in the same level costs progressively more).
     */
    public void setSkillRanks(String skillId, Integer ranks, boolean isSpellSkill) {
        if (ranks <= 0) {
            skillRanks.remove(skillId);
            if (isSpellSkill) {
                spellsUpdated.remove(skillId);
            }
        } else {
            skillRanks.put(skillId, ranks);
            if (isSpellSkill && !spellsUpdated.contains(skillId)) {
                spellsUpdated.add(skillId);
            }
        }
    }

    /** Adds {@code ranks} to whatever ranks in {@code skillId} were already bought this level. */
    public void addSkillRanks(String skillId, Integer ranks, boolean isSpellSkill) {
        setSkillRanks(skillId, getSkillRanks(skillId) + ranks, isSpellSkill);
    }

    public List<String> getSkillsWithRanks() {
        return new ArrayList<>(skillRanks.keySet());
    }

    public Map<String, Integer> getSkillRanks() {
        return skillRanks;
    }

    public void setSkillRanks(Map<String, Integer> skillRanks) {
        this.skillRanks = skillRanks;
    }

    /**
     * If a player learns more than 5 spell lists in one level, the cost of the next one is doubled;
     * more than 10, quadrupled.
     *
     * @param skillId the spell skill about to be bought (or already bought) this level.
     */
    public Integer getSpellRankMultiplier(String skillId) {
        int spellLists = spellsUpdated.indexOf(skillId);
        // If not in the list yet, this would be a new list acquired this level. This is useful for
        // random characters, which check the cost of a rank before deciding whether to add it.
        if (spellLists < 0) {
            spellLists = spellsUpdated.size();
        }
        if (spellLists < 5) {
            return 1;
        } else if (spellLists < 10) {
            return 2;
        }
        return 4;
    }

    public List<String> getSpellsUpdated() {
        return spellsUpdated;
    }

    public void setSpellsUpdated(List<String> spellsUpdated) {
        this.spellsUpdated = spellsUpdated;
    }

    public List<String> getTrainings() {
        return trainings;
    }

    public void addTraining(String trainingId) {
        trainings.add(trainingId);
    }

    public void removeTraining(String trainingId) {
        trainings.remove(trainingId);
    }

    public void setTrainings(List<String> trainings) {
        this.trainings = trainings;
    }

    public Set<String> getGeneralizedSkills() {
        return generalizedSkills;
    }

    public void setGeneralizedSkills(Set<String> generalizedSkills) {
        this.generalizedSkills = generalizedSkills;
    }

    /** Of {@code allSpecialityIds} (every speciality of some skill), the ones selected this level. */
    public List<String> getSkillSpecializations(List<String> allSpecialityIds) {
        final List<String> selected = new ArrayList<>();
        for (final String specialityId : allSpecialityIds) {
            if (skillSpecializations.contains(specialityId)) {
                selected.add(specialityId);
            }
        }
        return selected;
    }

    public Set<String> getSkillSpecializations() {
        return skillSpecializations;
    }

    public void addSkillSpecialization(String specializationId) {
        skillSpecializations.add(specializationId);
    }

    public void setSkillSpecializations(Set<String> skillSpecializations) {
        this.skillSpecializations = skillSpecializations;
    }

    /** How many of {@code allSpecialityIds} (every speciality of some skill) were selected this level. */
    public Integer getRanksSpentInSpecializations(List<String> allSpecialityIds) {
        int total = 0;
        for (final String specialityId : allSpecialityIds) {
            if (skillSpecializations.contains(specialityId)) {
                total++;
            }
        }
        return total;
    }

    public CharacteristicRoll getCharacteristicUpdate(CharacteristicAbbreviation abbreviation) {
        for (final CharacteristicRoll characteristicRoll : characteristicUpdates) {
            if (characteristicRoll.getCharacteristicAbbreviation() == abbreviation) {
                return characteristicRoll;
            }
        }
        return null;
    }

    public CharacteristicRoll addCharacteristicUpdate(CharacteristicAbbreviation abbreviation, Integer currentTemporalValue,
                                                       Integer currentPotentialValue, Roll roll) {
        final CharacteristicRoll characteristicRoll = new CharacteristicRoll(abbreviation, currentTemporalValue,
                currentPotentialValue, roll);
        characteristicUpdates.add(characteristicRoll);
        return characteristicRoll;
    }

    public void updateCharacteristicRoll(CharacteristicAbbreviation abbreviation, int temporalValue, int potentialValue) {
        final CharacteristicRoll characteristicRoll = getCharacteristicUpdate(abbreviation);
        characteristicRoll.setCharacteristicTemporalValue(temporalValue);
        characteristicRoll.setCharacteristicPotentialValue(potentialValue);
    }

    public List<CharacteristicRoll> getCharacteristicUpdates() {
        return characteristicUpdates;
    }

    public Set<String> getFavouriteSkills() {
        return favouriteSkills;
    }

    public void setFavouriteSkills(Set<String> favouriteSkills) {
        this.favouriteSkills = favouriteSkills;
    }

    public List<AgeModification> getAgeModifications() {
        return ageModifications;
    }

    public void addAgeModification(AgeModification ageModification) {
        ageModifications.add(ageModification);
    }
}
