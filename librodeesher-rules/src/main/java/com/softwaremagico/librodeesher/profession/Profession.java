package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;

import java.util.Collections;
import java.util.List;

/**
 * A character profession (e.g. "Mago", "Guerrero"), as defined in a rulebook's {@code profesiones.xml}.
 *
 * <p>The legacy {@code Profession} constructor parsed 8 sections from its text file. The most
 * regular ones are modeled as plain data here: characteristic preferences, available magic realms,
 * flat category/skill bonuses, and per-training background point costs. HABILIDADES COMUNES/
 * PROFESIONALES/RESTRINGIDAS (skills a profession makes available, either outright or as a "choose N
 * from a category/list" pick, see {@link #getCommonSkillIds()}/{@link #getCommonSkillChoices()} and
 * their PROFESSIONAL/RESTRICTED siblings) are fully modeled too. One section lists per-category skill
 * development costs with special-cased weapon category handling (HABILIDADES Y CATEGORÍAS DE
 * HABILIDADES), and one lists spell list development costs by character level range (DESARROLLO DE
 * HECHIZOS); these two are significant enough scope on their own (and depend on further
 * {@code Category}/magic-list cross-references) that they are preserved verbatim for now, the same
 * trade-off already applied to {@code Perk#getBonusesRaw()}.</p>
 */
public class Profession extends Element {

    /**
     * Preferred characteristics, in order (first is the primary, second the secondary); empty means
     * the profession is not picky about characteristics ("Indiferente" in the legacy file).
     */
    @JacksonXmlElementWrapper(localName = "characteristicPreferences")
    @JacksonXmlProperty(localName = "characteristic")
    private List<CharacteristicAbbreviation> characteristicPreferences;

    @JacksonXmlElementWrapper(localName = "magicRealms")
    @JacksonXmlProperty(localName = "realmGrant")
    private List<RealmOfMagicGrant> magicRealms;

    @JacksonXmlElementWrapper(localName = "bonuses")
    @JacksonXmlProperty(localName = "bonus")
    private List<ProfessionBonus> bonuses;

    /** Verbatim "HABILIDADES Y CATEGORÍAS DE HABILIDADES" section; see the class javadoc. */
    @JsonProperty("categoryCostsRaw")
    private String categoryCostsRaw;

    /** Skill ids granted outright by "HABILIDADES COMUNES" (no choice involved). */
    @JacksonXmlElementWrapper(localName = "commonSkillIds")
    @JacksonXmlProperty(localName = "commonSkillId")
    private List<String> commonSkillIds;

    /** "Choose N from a category/list" entries of "HABILIDADES COMUNES"; see {@link ProfessionSkillGrant}. */
    @JacksonXmlElementWrapper(localName = "commonSkillChoices")
    @JacksonXmlProperty(localName = "commonSkillGrant")
    private List<ProfessionSkillGrant> commonSkillChoices;

    /** Same as {@link #commonSkillIds}, for "HABILIDADES PROFESIONALES". */
    @JacksonXmlElementWrapper(localName = "professionalSkillIds")
    @JacksonXmlProperty(localName = "professionalSkillId")
    private List<String> professionalSkillIds;

    /** Same as {@link #commonSkillChoices}, for "HABILIDADES PROFESIONALES". */
    @JacksonXmlElementWrapper(localName = "professionalSkillChoices")
    @JacksonXmlProperty(localName = "professionalSkillGrant")
    private List<ProfessionSkillGrant> professionalSkillChoices;

    /** Same as {@link #commonSkillIds}, for "HABILIDADES RESTRINGIDAS". */
    @JacksonXmlElementWrapper(localName = "restrictedSkillIds")
    @JacksonXmlProperty(localName = "restrictedSkillId")
    private List<String> restrictedSkillIds;

    /** Same as {@link #commonSkillChoices}, for "HABILIDADES RESTRINGIDAS". */
    @JacksonXmlElementWrapper(localName = "restrictedSkillChoices")
    @JacksonXmlProperty(localName = "restrictedSkillGrant")
    private List<ProfessionSkillGrant> restrictedSkillChoices;

    /** Verbatim "DESARROLLO DE HECHIZOS" section (only present for spell-casting professions). */
    @JsonProperty("magicCostsRaw")
    private String magicCostsRaw;

    @JacksonXmlElementWrapper(localName = "trainingCosts")
    @JacksonXmlProperty(localName = "trainingCost")
    private List<ProfessionTrainingCost> trainingCosts;

    public Profession() {
        super();
    }

    public Profession(String id) {
        super(id);
    }

    public List<CharacteristicAbbreviation> getCharacteristicPreferences() {
        return characteristicPreferences == null ? Collections.emptyList() : characteristicPreferences;
    }

    public void setCharacteristicPreferences(List<CharacteristicAbbreviation> characteristicPreferences) {
        this.characteristicPreferences = characteristicPreferences;
    }

    /** Whether every characteristic is equally suited for this profession ("Indiferente"). */
    public boolean isIndifferentToCharacteristics() {
        return getCharacteristicPreferences().isEmpty();
    }

    /**
     * Whether {@code abbreviation} is this profession's primary or secondary preferred characteristic
     * (the first two entries of {@link #getCharacteristicPreferences()}), used e.g. to grant a
     * higher initial characteristic value during character creation.
     */
    public boolean isPreferredCharacteristic(CharacteristicAbbreviation abbreviation) {
        final List<CharacteristicAbbreviation> preferences = getCharacteristicPreferences();
        return preferences.size() > 0 && preferences.get(0) == abbreviation
                || preferences.size() > 1 && preferences.get(1) == abbreviation;
    }

    public List<RealmOfMagicGrant> getMagicRealms() {
        return magicRealms == null ? Collections.emptyList() : magicRealms;
    }

    public void setMagicRealms(List<RealmOfMagicGrant> magicRealms) {
        this.magicRealms = magicRealms;
    }

    /** Whether this profession can cast spells at all. */
    public boolean isSpellCaster() {
        return !getMagicRealms().isEmpty();
    }

    public List<ProfessionBonus> getBonuses() {
        return bonuses == null ? Collections.emptyList() : bonuses;
    }

    public void setBonuses(List<ProfessionBonus> bonuses) {
        this.bonuses = bonuses;
    }

    /**
     * The flat bonus this profession grants to a category or a skill named {@code id} (see
     * {@link ProfessionBonus}), or 0 if this profession grants it none.
     */
    public Integer getBonus(String id) {
        for (final ProfessionBonus bonus : getBonuses()) {
            if (bonus.getName().equals(id)) {
                return bonus.getBonus();
            }
        }
        return 0;
    }

    public String getCategoryCostsRaw() {
        return categoryCostsRaw;
    }

    public void setCategoryCostsRaw(String categoryCostsRaw) {
        this.categoryCostsRaw = categoryCostsRaw;
    }

    public List<String> getCommonSkillIds() { return commonSkillIds == null ? Collections.emptyList() : commonSkillIds; }
    public void setCommonSkillIds(List<String> commonSkillIds) { this.commonSkillIds = commonSkillIds; }
    public List<ProfessionSkillGrant> getCommonSkillChoices() { return commonSkillChoices == null ? Collections.emptyList() : commonSkillChoices; }
    public void setCommonSkillChoices(List<ProfessionSkillGrant> commonSkillChoices) { this.commonSkillChoices = commonSkillChoices; }

    public List<String> getProfessionalSkillIds() { return professionalSkillIds == null ? Collections.emptyList() : professionalSkillIds; }
    public void setProfessionalSkillIds(List<String> professionalSkillIds) { this.professionalSkillIds = professionalSkillIds; }
    public List<ProfessionSkillGrant> getProfessionalSkillChoices() { return professionalSkillChoices == null ? Collections.emptyList() : professionalSkillChoices; }
    public void setProfessionalSkillChoices(List<ProfessionSkillGrant> professionalSkillChoices) { this.professionalSkillChoices = professionalSkillChoices; }

    public List<String> getRestrictedSkillIds() { return restrictedSkillIds == null ? Collections.emptyList() : restrictedSkillIds; }
    public void setRestrictedSkillIds(List<String> restrictedSkillIds) { this.restrictedSkillIds = restrictedSkillIds; }
    public List<ProfessionSkillGrant> getRestrictedSkillChoices() { return restrictedSkillChoices == null ? Collections.emptyList() : restrictedSkillChoices; }
    public void setRestrictedSkillChoices(List<ProfessionSkillGrant> restrictedSkillChoices) { this.restrictedSkillChoices = restrictedSkillChoices; }

    /**
     * Whether this profession grants {@code skillId} outright as one of its "HABILIDADES COMUNES"
     * (only the fixed {@link #getCommonSkillIds()}, not the "choose N" {@link #getCommonSkillChoices()}
     * entries, matching the legacy {@code Profession#isCommon(Skill)} exactly).
     */
    public boolean isCommonSkill(String skillId) {
        return getCommonSkillIds().contains(skillId);
    }

    /** Same as {@link #isCommonSkill(String)}, for "HABILIDADES RESTRINGIDAS" ({@code Profession#isRestricted(Skill)}). */
    public boolean isRestrictedSkill(String skillId) {
        return getRestrictedSkillIds().contains(skillId);
    }

    /** Same as {@link #isCommonSkill(String)}, for "HABILIDADES PROFESIONALES" ({@code Profession#isProfessional(Skill)}). */
    public boolean isProfessionalSkill(String skillId) {
        return getProfessionalSkillIds().contains(skillId);
    }

    public String getMagicCostsRaw() {
        return magicCostsRaw;
    }

    public void setMagicCostsRaw(String magicCostsRaw) {
        this.magicCostsRaw = magicCostsRaw;
    }

    public List<ProfessionTrainingCost> getTrainingCosts() {
        return trainingCosts == null ? Collections.emptyList() : trainingCosts;
    }

    public void setTrainingCosts(List<ProfessionTrainingCost> trainingCosts) {
        this.trainingCosts = trainingCosts;
    }
}
