package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;

import java.util.Collections;
import java.util.List;

/**
 * A character profession (e.g. "Mago", "Guerrero"), as defined in a rulebook's {@code profesiones.xml}.
 *
 * <p>The legacy {@code Profession} constructor parsed 8 sections from its text file. The most
 * regular ones are modeled as plain data here: characteristic preferences, available magic realms,
 * flat category/skill bonuses, and per-training background point costs. Three sections mix plain
 * skill names with a "choose N from {a;b;c}" / "choose N from category#N" syntax
 * (HABILIDADES COMUNES/PROFESIONALES/RESTRINGIDAS), one lists per-category skill development costs
 * with special-cased weapon category handling (HABILIDADES Y CATEGORÍAS DE HABILIDADES), and one
 * lists spell list development costs by character level range (DESARROLLO DE HECHIZOS); these are
 * significant enough scope on their own (and depend on {@code Category}/{@code Skill}/magic-list
 * cross-references) that they are preserved verbatim for now, the same trade-off already applied to
 * {@code Perk#getBonusesRaw()}.</p>
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
    @JacksonXmlProperty(localName = "realm")
    private List<RealmOfMagic> magicRealms;

    @JacksonXmlElementWrapper(localName = "bonuses")
    @JacksonXmlProperty(localName = "bonus")
    private List<ProfessionBonus> bonuses;

    /** Verbatim "HABILIDADES Y CATEGORÍAS DE HABILIDADES" section; see the class javadoc. */
    @JsonProperty("categoryCostsRaw")
    private String categoryCostsRaw;

    /** Verbatim "HABILIDADES COMUNES" section; see the class javadoc. */
    @JsonProperty("commonSkillsRaw")
    private String commonSkillsRaw;

    /** Verbatim "HABILIDADES PROFESIONALES" section; see the class javadoc. */
    @JsonProperty("professionalSkillsRaw")
    private String professionalSkillsRaw;

    /** Verbatim "HABILIDADES RESTRINGIDAS" section; see the class javadoc. */
    @JsonProperty("restrictedSkillsRaw")
    private String restrictedSkillsRaw;

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

    public List<RealmOfMagic> getMagicRealms() {
        return magicRealms == null ? Collections.emptyList() : magicRealms;
    }

    public void setMagicRealms(List<RealmOfMagic> magicRealms) {
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

    public String getCommonSkillsRaw() {
        return commonSkillsRaw;
    }

    public void setCommonSkillsRaw(String commonSkillsRaw) {
        this.commonSkillsRaw = commonSkillsRaw;
    }

    public String getProfessionalSkillsRaw() {
        return professionalSkillsRaw;
    }

    public void setProfessionalSkillsRaw(String professionalSkillsRaw) {
        this.professionalSkillsRaw = professionalSkillsRaw;
    }

    public String getRestrictedSkillsRaw() {
        return restrictedSkillsRaw;
    }

    public void setRestrictedSkillsRaw(String restrictedSkillsRaw) {
        this.restrictedSkillsRaw = restrictedSkillsRaw;
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
