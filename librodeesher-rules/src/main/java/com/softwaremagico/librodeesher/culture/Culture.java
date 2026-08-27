package com.softwaremagico.librodeesher.culture;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;

import java.util.Collections;
import java.util.List;

/**
 * A cultural background (e.g. Rural, Urban Upper Class) migrated from the legacy
 * {@code culturas/*.txt} files.
 */
public class Culture extends Element {

    @JacksonXmlElementWrapper(localName = "typicalWeapons")
    @JacksonXmlProperty(localName = "weaponId")
    private List<String> typicalWeaponIds;

    @JacksonXmlElementWrapper(localName = "typicalArmors")
    @JacksonXmlProperty(localName = "armor")
    private List<String> typicalArmors;

    @JacksonXmlElementWrapper(localName = "adolescenceRanks")
    @JacksonXmlProperty(localName = "categoryGrant")
    private List<TrainingCategoryGrant> adolescenceRanks;

    @JsonProperty("hobbyRanks")
    private Integer hobbyRanks;

    @JacksonXmlElementWrapper(localName = "hobbies")
    @JacksonXmlProperty(localName = "hobbyId")
    private List<String> hobbyIds;

    @JacksonXmlElementWrapper(localName = "languageMaxRanks")
    @JacksonXmlProperty(localName = "languageRank")
    private List<CultureLanguageRank> languageMaxRanks;

    @JacksonXmlElementWrapper(localName = "trainingPrices")
    @JacksonXmlProperty(localName = "trainingPrice")
    private List<CultureTrainingPrice> trainingPrices;

    public Culture() {
        super();
    }

    public Culture(String id) {
        super(id);
    }

    public List<String> getTypicalWeaponIds() { return typicalWeaponIds == null ? Collections.emptyList() : typicalWeaponIds; }
    public void setTypicalWeaponIds(List<String> typicalWeaponIds) { this.typicalWeaponIds = typicalWeaponIds; }
    public List<String> getTypicalArmors() { return typicalArmors == null ? Collections.emptyList() : typicalArmors; }
    public void setTypicalArmors(List<String> typicalArmors) { this.typicalArmors = typicalArmors; }
    public List<TrainingCategoryGrant> getAdolescenceRanks() { return adolescenceRanks == null ? Collections.emptyList() : adolescenceRanks; }
    public void setAdolescenceRanks(List<TrainingCategoryGrant> adolescenceRanks) { this.adolescenceRanks = adolescenceRanks; }
    public Integer getHobbyRanks() { return hobbyRanks; }
    public void setHobbyRanks(Integer hobbyRanks) { this.hobbyRanks = hobbyRanks; }
    public List<String> getHobbyIds() { return hobbyIds == null ? Collections.emptyList() : hobbyIds; }
    public void setHobbyIds(List<String> hobbyIds) { this.hobbyIds = hobbyIds; }
    public List<CultureLanguageRank> getLanguageMaxRanks() { return languageMaxRanks == null ? Collections.emptyList() : languageMaxRanks; }
    public void setLanguageMaxRanks(List<CultureLanguageRank> languageMaxRanks) { this.languageMaxRanks = languageMaxRanks; }
    public List<CultureTrainingPrice> getTrainingPrices() { return trainingPrices == null ? Collections.emptyList() : trainingPrices; }
    public void setTrainingPrices(List<CultureTrainingPrice> trainingPrices) { this.trainingPrices = trainingPrices; }
}
