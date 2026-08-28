package com.softwaremagico.librodeesher.race;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.language.LanguageSlot;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A playable race/species (e.g. Human, Elf, Dwarf), migrated from the legacy one-file-per-race
 * {@code razas/*.txt} format.
 *
 * <p>The source files contain many small tabular sections. Stable numeric/rule data is modeled as
 * typed fields. References to other rule elements (professions, cultures, categories, skills) are
 * stored by id, not by display name, following the English id standard used by the rest of the new
 * XML data.</p>
 */
public class Race extends Element {

    @JsonProperty("appearanceBonus")
    private Integer appearanceBonus;

    @JsonProperty("expectedLifeYears")
    private Integer expectedLifeYears;

    @JsonProperty("soulDepartTime")
    private Integer soulDepartTime;

    @JsonProperty("raceType")
    private Integer raceType;

    @JsonProperty("size")
    private String size;

    @JsonProperty("restorationTime")
    private Double restorationTime;

    @JsonProperty("languagePoints")
    private Integer languagePoints;

    @JsonProperty("backgroundPoints")
    private Integer backgroundPoints;

    @JsonProperty("naturalArmorType")
    private Integer naturalArmorType;

    /** Keyed by {@code com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation} constant name. */
    @JsonProperty("characteristicBonuses")
    private Map<String, Integer> characteristicBonuses;

    /** Keyed by {@code com.softwaremagico.librodeesher.resistance.ResistanceType} constant name. */
    @JsonProperty("resistanceBonuses")
    private Map<String, Integer> resistanceBonuses;

    @JsonProperty("progressionRankValues")
    private Map<String, String> progressionRankValues;

    @JacksonXmlElementWrapper(localName = "restrictedProfessions")
    @JacksonXmlProperty(localName = "professionId")
    private List<String> restrictedProfessionIds;

    /**
     * Profession ids/{@code "group:"} tokens explicitly marked with a "-" in the "PROFESIONES
     * PROHIBIDAS" section: when this is non-empty, the whole section flips meaning (matching the
     * legacy {@code Race#setRestrictedProfessions}'s per-file "exception" flag) - every profession
     * matching {@link #getRestrictedProfessionIds()} or this list becomes the only ones the race is
     * allowed to take, instead of the ones it forbids.
     */
    @JacksonXmlElementWrapper(localName = "excludedRestrictedProfessions")
    @JacksonXmlProperty(localName = "excludedProfessionId")
    private List<String> excludedProfessionIds;

    @JacksonXmlElementWrapper(localName = "raceLanguages")
    @JacksonXmlProperty(localName = "raceLanguage")
    private List<RaceLanguage> raceLanguages;

    /**
     * Anonymous "Idioma Racial"/"Idioma Regional" slots found in the "IDIOMAS" section (a language
     * the player picks freely, as opposed to the fixed languages in {@link #raceLanguages}); see
     * {@link com.softwaremagico.librodeesher.language.LanguageSlot}.
     */
    @JacksonXmlElementWrapper(localName = "optionalRaceLanguages")
    @JacksonXmlProperty(localName = "optionalRaceLanguageSlot")
    private List<LanguageSlot> optionalRaceLanguages;

    @JacksonXmlElementWrapper(localName = "backgroundLanguages")
    @JacksonXmlProperty(localName = "backgroundLanguage")
    private List<RaceLanguage> backgroundLanguages;

    /** Same as {@link #optionalRaceLanguages}, but for the "IDIOMAS DE TRASFONDO" section. */
    @JacksonXmlElementWrapper(localName = "optionalBackgroundLanguages")
    @JacksonXmlProperty(localName = "optionalBackgroundLanguageSlot")
    private List<LanguageSlot> optionalBackgroundLanguages;

    /** Ids of skills (including weapon skills, see {@code SkillMigrationTool}) this race treats as common (developed at a favourable cost tier). */
    @JacksonXmlElementWrapper(localName = "commonSkills")
    @JacksonXmlProperty(localName = "commonSkillId")
    private List<String> commonSkillIds;

    @JacksonXmlElementWrapper(localName = "commonCategories")
    @JacksonXmlProperty(localName = "commonCategoryId")
    private List<String> commonCategoryIds;

    /** Same as {@link #commonSkillIds}, but for skills this race restricts. */
    @JacksonXmlElementWrapper(localName = "restrictedSkills")
    @JacksonXmlProperty(localName = "restrictedSkillId")
    private List<String> restrictedSkillIds;

    @JacksonXmlElementWrapper(localName = "restrictedCategories")
    @JacksonXmlProperty(localName = "restrictedCategoryId")
    private List<String> restrictedCategoryIds;

    @JacksonXmlElementWrapper(localName = "cultures")
    @JacksonXmlProperty(localName = "cultureId")
    private List<String> cultureIds;

    /**
     * Culture ids/{@code "group:"} tokens explicitly marked with a "-" in the "CULTURAS DISPONIBLES"
     * section: when this is non-empty, the whole section flips meaning (matching the legacy {@code
     * Race#setCultures}'s per-file "exception" flag) - every culture matching {@link
     * #getCultureIds()} or this list becomes excluded from "every culture is available", instead of
     * being the only ones available.
     */
    @JacksonXmlElementWrapper(localName = "excludedCultures")
    @JacksonXmlProperty(localName = "excludedCultureId")
    private List<String> excludedCultureIds;

    @JacksonXmlElementWrapper(localName = "specials")
    @JacksonXmlProperty(localName = "special")
    private List<RaceSpecial> specials;

    @JacksonXmlElementWrapper(localName = "maleNames")
    @JacksonXmlProperty(localName = "maleName")
    private List<String> maleNames;

    @JacksonXmlElementWrapper(localName = "femaleNames")
    @JacksonXmlProperty(localName = "femaleName")
    private List<String> femaleNames;

    @JacksonXmlElementWrapper(localName = "familyNames")
    @JacksonXmlProperty(localName = "familyName")
    private List<String> familyNames;

    public Race() {
        super();
    }

    public Race(String id) {
        super(id);
    }

    public Integer getAppearanceBonus() { return appearanceBonus; }
    public void setAppearanceBonus(Integer appearanceBonus) { this.appearanceBonus = appearanceBonus; }
    public Integer getExpectedLifeYears() { return expectedLifeYears; }
    public void setExpectedLifeYears(Integer expectedLifeYears) { this.expectedLifeYears = expectedLifeYears; }
    public Integer getSoulDepartTime() { return soulDepartTime; }
    public void setSoulDepartTime(Integer soulDepartTime) { this.soulDepartTime = soulDepartTime; }
    public Integer getRaceType() { return raceType; }
    public void setRaceType(Integer raceType) { this.raceType = raceType; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Double getRestorationTime() { return restorationTime; }
    public void setRestorationTime(Double restorationTime) { this.restorationTime = restorationTime; }
    public Integer getLanguagePoints() { return languagePoints; }
    public void setLanguagePoints(Integer languagePoints) { this.languagePoints = languagePoints; }
    public Integer getBackgroundPoints() { return backgroundPoints; }
    public void setBackgroundPoints(Integer backgroundPoints) { this.backgroundPoints = backgroundPoints; }
    public Integer getNaturalArmorType() { return naturalArmorType; }
    public void setNaturalArmorType(Integer naturalArmorType) { this.naturalArmorType = naturalArmorType; }
    public Map<String, Integer> getCharacteristicBonuses() { return characteristicBonuses == null ? Collections.emptyMap() : characteristicBonuses; }
    public void setCharacteristicBonuses(Map<String, Integer> characteristicBonuses) { this.characteristicBonuses = characteristicBonuses; }
    public Map<String, Integer> getResistanceBonuses() { return resistanceBonuses == null ? Collections.emptyMap() : resistanceBonuses; }
    public void setResistanceBonuses(Map<String, Integer> resistanceBonuses) { this.resistanceBonuses = resistanceBonuses; }
    public Map<String, String> getProgressionRankValues() { return progressionRankValues == null ? Collections.emptyMap() : progressionRankValues; }
    public void setProgressionRankValues(Map<String, String> progressionRankValues) { this.progressionRankValues = progressionRankValues; }
    public List<String> getRestrictedProfessionIds() { return restrictedProfessionIds == null ? Collections.emptyList() : restrictedProfessionIds; }
    public void setRestrictedProfessionIds(List<String> restrictedProfessionIds) { this.restrictedProfessionIds = restrictedProfessionIds; }
    public List<String> getExcludedProfessionIds() { return excludedProfessionIds == null ? Collections.emptyList() : excludedProfessionIds; }
    public void setExcludedProfessionIds(List<String> excludedProfessionIds) { this.excludedProfessionIds = excludedProfessionIds; }
    public List<RaceLanguage> getRaceLanguages() { return raceLanguages == null ? Collections.emptyList() : raceLanguages; }
    public void setRaceLanguages(List<RaceLanguage> raceLanguages) { this.raceLanguages = raceLanguages; }
    public List<LanguageSlot> getOptionalRaceLanguages() { return optionalRaceLanguages == null ? Collections.emptyList() : optionalRaceLanguages; }
    public void setOptionalRaceLanguages(List<LanguageSlot> optionalRaceLanguages) { this.optionalRaceLanguages = optionalRaceLanguages; }
    public List<RaceLanguage> getBackgroundLanguages() { return backgroundLanguages == null ? Collections.emptyList() : backgroundLanguages; }
    public void setBackgroundLanguages(List<RaceLanguage> backgroundLanguages) { this.backgroundLanguages = backgroundLanguages; }
    public List<LanguageSlot> getOptionalBackgroundLanguages() { return optionalBackgroundLanguages == null ? Collections.emptyList() : optionalBackgroundLanguages; }
    public void setOptionalBackgroundLanguages(List<LanguageSlot> optionalBackgroundLanguages) { this.optionalBackgroundLanguages = optionalBackgroundLanguages; }
    public List<String> getCommonSkillIds() { return commonSkillIds == null ? Collections.emptyList() : commonSkillIds; }
    public void setCommonSkillIds(List<String> commonSkillIds) { this.commonSkillIds = commonSkillIds; }
    public List<String> getCommonCategoryIds() { return commonCategoryIds == null ? Collections.emptyList() : commonCategoryIds; }
    public void setCommonCategoryIds(List<String> commonCategoryIds) { this.commonCategoryIds = commonCategoryIds; }
    public List<String> getRestrictedSkillIds() { return restrictedSkillIds == null ? Collections.emptyList() : restrictedSkillIds; }
    public void setRestrictedSkillIds(List<String> restrictedSkillIds) { this.restrictedSkillIds = restrictedSkillIds; }
    public List<String> getRestrictedCategoryIds() { return restrictedCategoryIds == null ? Collections.emptyList() : restrictedCategoryIds; }
    public void setRestrictedCategoryIds(List<String> restrictedCategoryIds) { this.restrictedCategoryIds = restrictedCategoryIds; }
    public List<String> getCultureIds() { return cultureIds == null ? Collections.emptyList() : cultureIds; }
    public void setCultureIds(List<String> cultureIds) { this.cultureIds = cultureIds; }
    public List<String> getExcludedCultureIds() { return excludedCultureIds == null ? Collections.emptyList() : excludedCultureIds; }
    public void setExcludedCultureIds(List<String> excludedCultureIds) { this.excludedCultureIds = excludedCultureIds; }
    public List<RaceSpecial> getSpecials() { return specials == null ? Collections.emptyList() : specials; }
    public void setSpecials(List<RaceSpecial> specials) { this.specials = specials; }
    public List<String> getMaleNames() { return maleNames == null ? Collections.emptyList() : maleNames; }
    public void setMaleNames(List<String> maleNames) { this.maleNames = maleNames; }
    public List<String> getFemaleNames() { return femaleNames == null ? Collections.emptyList() : femaleNames; }
    public void setFemaleNames(List<String> femaleNames) { this.femaleNames = femaleNames; }
    public List<String> getFamilyNames() { return familyNames == null ? Collections.emptyList() : familyNames; }
    public void setFamilyNames(List<String> familyNames) { this.familyNames = familyNames; }
}
