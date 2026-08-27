package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;

import java.util.Collections;
import java.util.List;

/**
 * A character background training/package (e.g. "Soldado", "Mercader"), as defined in a rulebook's
 * {@code adiestramientos.xml}.
 *
 * <p>The legacy {@code Training} constructor parsed a fixed sequence of 9 sections from its text
 * file: training time, race restriction, special starting items, category/skill ranks granted
 * ({@link #getCategories()}), characteristic upgrade choices, professional requirements, four kinds
 * of extra skills (life/common/professional/restricted), and an (in practice always empty in the
 * shipped data) per-profession cost override.</p>
 */
public class Training extends Element {

    @JsonProperty("trainingTimeInMonths")
    private Integer trainingTimeInMonths;

    @JacksonXmlElementWrapper(localName = "limitedRaces")
    @JacksonXmlProperty(localName = "race")
    private List<String> limitedRaces;

    @JacksonXmlElementWrapper(localName = "specialItems")
    @JacksonXmlProperty(localName = "specialItem")
    private List<TrainingSpecialItem> specialItems;

    /** Category/skill ranks granted by this training; see {@link TrainingCategoryGrant}. */
    @JacksonXmlElementWrapper(localName = "categories")
    @JacksonXmlProperty(localName = "categoryGrant")
    private List<TrainingCategoryGrant> categories;

    @JacksonXmlElementWrapper(localName = "characteristicUpgrades")
    @JacksonXmlProperty(localName = "upgradeChoice")
    private List<ChoiceGroup> characteristicUpgrades;

    @JacksonXmlElementWrapper(localName = "requirements")
    @JacksonXmlProperty(localName = "requirement")
    private List<TrainingRequirement> requirements;

    @JacksonXmlElementWrapper(localName = "lifeSkills")
    @JacksonXmlProperty(localName = "lifeSkillChoice")
    private List<ChoiceGroup> lifeSkills;

    @JacksonXmlElementWrapper(localName = "commonSkills")
    @JacksonXmlProperty(localName = "commonSkillChoice")
    private List<ChoiceGroup> commonSkills;

    @JacksonXmlElementWrapper(localName = "professionalSkills")
    @JacksonXmlProperty(localName = "professionalSkillChoice")
    private List<ChoiceGroup> professionalSkills;

    @JacksonXmlElementWrapper(localName = "restrictedSkills")
    @JacksonXmlProperty(localName = "restrictedSkillChoice")
    private List<ChoiceGroup> restrictedSkills;

    @JacksonXmlElementWrapper(localName = "professionCosts")
    @JacksonXmlProperty(localName = "professionCost")
    private List<TrainingProfessionCost> professionCosts;

    public Training() {
        super();
    }

    public Training(String id) {
        super(id);
    }

    public Integer getTrainingTimeInMonths() {
        return trainingTimeInMonths;
    }

    public void setTrainingTimeInMonths(Integer trainingTimeInMonths) {
        this.trainingTimeInMonths = trainingTimeInMonths;
    }

    public List<String> getLimitedRaces() {
        return limitedRaces == null ? Collections.emptyList() : limitedRaces;
    }

    public void setLimitedRaces(List<String> limitedRaces) {
        this.limitedRaces = limitedRaces;
    }

    /** Whether every race can take this training (no "EXCLUSIVO RAZA" restriction). */
    public boolean isAvailableToEveryRace() {
        return getLimitedRaces().isEmpty();
    }

    public List<TrainingSpecialItem> getSpecialItems() {
        return specialItems == null ? Collections.emptyList() : specialItems;
    }

    public void setSpecialItems(List<TrainingSpecialItem> specialItems) {
        this.specialItems = specialItems;
    }

    public List<TrainingCategoryGrant> getCategories() {
        return categories == null ? Collections.emptyList() : categories;
    }

    public void setCategories(List<TrainingCategoryGrant> categories) {
        this.categories = categories;
    }

    public List<ChoiceGroup> getCharacteristicUpgrades() {
        return characteristicUpgrades == null ? Collections.emptyList() : characteristicUpgrades;
    }

    public void setCharacteristicUpgrades(List<ChoiceGroup> characteristicUpgrades) {
        this.characteristicUpgrades = characteristicUpgrades;
    }

    public List<TrainingRequirement> getRequirements() {
        return requirements == null ? Collections.emptyList() : requirements;
    }

    public void setRequirements(List<TrainingRequirement> requirements) {
        this.requirements = requirements;
    }

    public List<ChoiceGroup> getLifeSkills() {
        return lifeSkills == null ? Collections.emptyList() : lifeSkills;
    }

    public void setLifeSkills(List<ChoiceGroup> lifeSkills) {
        this.lifeSkills = lifeSkills;
    }

    public List<ChoiceGroup> getCommonSkills() {
        return commonSkills == null ? Collections.emptyList() : commonSkills;
    }

    public void setCommonSkills(List<ChoiceGroup> commonSkills) {
        this.commonSkills = commonSkills;
    }

    public List<ChoiceGroup> getProfessionalSkills() {
        return professionalSkills == null ? Collections.emptyList() : professionalSkills;
    }

    public void setProfessionalSkills(List<ChoiceGroup> professionalSkills) {
        this.professionalSkills = professionalSkills;
    }

    public List<ChoiceGroup> getRestrictedSkills() {
        return restrictedSkills == null ? Collections.emptyList() : restrictedSkills;
    }

    public void setRestrictedSkills(List<ChoiceGroup> restrictedSkills) {
        this.restrictedSkills = restrictedSkills;
    }

    public List<TrainingProfessionCost> getProfessionCosts() {
        return professionCosts == null ? Collections.emptyList() : professionCosts;
    }

    public void setProfessionCosts(List<TrainingProfessionCost> professionCosts) {
        this.professionCosts = professionCosts;
    }
}
