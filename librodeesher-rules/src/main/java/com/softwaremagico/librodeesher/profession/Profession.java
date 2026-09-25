package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.magic.MagicLevelRange;
import com.softwaremagico.librodeesher.magic.MagicListType;

import java.util.Collections;
import java.util.List;

/**
 * A character profession (e.g. "Mago", "Guerrero"), as defined in a rulebook's {@code profesiones.xml}.
 *
 * <p>The legacy {@code Profession} constructor parsed 8 sections from its text file, every one now
 * modeled as plain data here: characteristic preferences, available magic realms, flat category/skill
 * bonuses, per-training background point costs, HABILIDADES COMUNES/PROFESIONALES/RESTRINGIDAS
 * (skills a profession makes available, either outright or as a "choose N from a category/list" pick,
 * see {@link #getCommonSkillIds()}/{@link #getCommonSkillChoices()} and their PROFESSIONAL/RESTRICTED
 * siblings), HABILIDADES Y CATEGORÍAS DE HABILIDADES (per-category development costs, see {@link
 * #getCategoryCosts()}/{@link #getWeaponCategoryCostTiers()}; see {@code
 * CharacterPlayer#assignWeaponCategoryCostTier} for the player-facing "assign a weapon cost tier to a
 * weapon category" decision it enables), and DESARROLLO DE HECHIZOS (spell list development costs by
 * level range, see {@link #getMagicCost(com.softwaremagico.librodeesher.magic.MagicListType, int)},
 * consumed through {@code CharacterPlayer#classifySpellList}).</p>
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

    /**
     * Per-category background point costs from "HABILIDADES Y CATEGORÍAS DE HABILIDADES" for every
     * real, named category (i.e. every one that does not start with "Armas·", see {@link
     * #getWeaponCategoryCostTiers()} for those).
     */
    @JacksonXmlElementWrapper(localName = "categoryCosts")
    @JacksonXmlProperty(localName = "categoryCost")
    private List<ProfessionCategoryCost> categoryCosts;

    /**
     * The "Armas·Categoría1" through "Armas·Categoría7" cost tiers, sorted cheapest-to-priciest
     * exactly like the legacy {@code Profession.CategoryCostComparator} did (more rank slots first,
     * then ascending by each rank's cost). Unlike {@link #categoryCosts}, these do not name a
     * specific weapon category by themselves: the player freely assigns each tier to one of their
     * available weapon categories at character creation (cheapest tier to their favourite weapon,
     * and so on), a decision-tracking mechanic matching the legacy {@code
     * ProfessionDecisions#setWeaponCost} (see {@code CharacterPlayer#assignWeaponCategoryCostTier}).
     */
    @JacksonXmlElementWrapper(localName = "weaponCategoryCostTiers")
    @JacksonXmlProperty(localName = "tier")
    private List<ProfessionWeaponCostTier> weaponCategoryCostTiers;

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

    /** The "DESARROLLO DE HECHIZOS" section, per-(list type, level range) rank cost table (only
     * present for spell-casting professions). */
    @JacksonXmlElementWrapper(localName = "magicCosts")
    @JacksonXmlProperty(localName = "magicCost")
    private List<ProfessionMagicCost> magicCosts;

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

    /**
     * Whether this profession can cast spells at all: matches the legacy {@code Profession#
     * isSpellCaster()} exactly, which checks for a defined {@link MagicListType#BASIC} development
     * cost rather than {@link #getMagicRealms()} (non-caster professions, e.g. a pure fighter, are
     * still given a {@link RealmOfMagicGrant} of every realm plus an {@link MagicListType#OPEN}-only
     * cost table, so they may occasionally splurge on an open list spell at exorbitant cost - see any
     * shipped profession's own "DESARROLLO DE HECHIZOS" section - which would make {@code
     * !getMagicRealms().isEmpty()} wrongly classify them as a real spell caster).
     */
    public boolean isSpellCaster() {
        return getMagicCost(MagicListType.BASIC, 0) != null;
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

    public List<ProfessionCategoryCost> getCategoryCosts() { return categoryCosts == null ? Collections.emptyList() : categoryCosts; }
    public void setCategoryCosts(List<ProfessionCategoryCost> categoryCosts) { this.categoryCosts = categoryCosts; }

    public List<ProfessionWeaponCostTier> getWeaponCategoryCostTiers() { return weaponCategoryCostTiers == null ? Collections.emptyList() : weaponCategoryCostTiers; }
    public void setWeaponCategoryCostTiers(List<ProfessionWeaponCostTier> weaponCategoryCostTiers) { this.weaponCategoryCostTiers = weaponCategoryCostTiers; }

    /**
     * The background point cost of developing {@code categoryId} (see {@link ProfessionCategoryCost}),
     * or {@code null} if this profession does not mention that category at all (it is a weapon
     * category, handled by {@link #getWeaponCategoryCostTiers()} instead, or simply not developable).
     */
    public ProfessionCategoryCost getCategoryCost(String categoryId) {
        for (final ProfessionCategoryCost cost : getCategoryCosts()) {
            if (cost.getCategoryId().equals(categoryId)) {
                return cost;
            }
        }
        return null;
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

    public List<ProfessionMagicCost> getMagicCosts() {
        return magicCosts == null ? Collections.emptyList() : magicCosts;
    }

    public void setMagicCosts(List<ProfessionMagicCost> magicCosts) {
        this.magicCosts = magicCosts;
    }

    /**
     * This profession's development cost bracket for a spell list classified as {@code listType},
     * given it already has {@code currentListRanks} ranks bought (across every level): resolves via
     * {@link com.softwaremagico.librodeesher.magic.MagicLevelRange#forRanks(int)} applied to {@code
     * currentListRanks + 1} (the rank about to be bought), matching the legacy {@code
     * Profession#getMagicCost(MagicListType, Integer)} exactly (including its own off-by-one quirk:
     * the bracket boundary is one rank earlier than its "(1-5)"-style label suggests, e.g. the 5th
     * rank of a list already falls under the "(6-10)" bracket's cost, not "(1-5)"'s).
     *
     * <p>Returns the bracket itself (as opposed to a single cost), since - like {@link
     * ProfessionCategoryCost}/{@link ProfessionTrainingCost} - how much of it applies still depends on
     * how many ranks of {@code listType} were already bought at the <em>current level</em> (see
     * {@link ProfessionMagicCost#getRankCost(int)}). {@code null} if this profession's magic cost
     * table has no row for that (type, bracket) pair (e.g. a non-caster profession querying a list
     * type only casters develop).</p>
     */
    public ProfessionMagicCost getMagicCost(MagicListType listType, int currentListRanks) {
        final MagicLevelRange levelRange = MagicLevelRange.forRanks(currentListRanks + 1);
        for (final ProfessionMagicCost cost : getMagicCosts()) {
            if (cost.getListType() == listType && cost.getLevelRange() == levelRange) {
                return cost;
            }
        }
        return null;
    }

    public List<ProfessionTrainingCost> getTrainingCosts() {
        return trainingCosts == null ? Collections.emptyList() : trainingCosts;
    }

    public void setTrainingCosts(List<ProfessionTrainingCost> trainingCosts) {
        this.trainingCosts = trainingCosts;
    }

    /**
     * This profession's explicit background point cost/preference (favourite/forbidden) for
     * {@code trainingId} (the "ADIESTRAMIENTO" section), or {@code null} if it does not mention that
     * training at all (its cost then falls back to {@link
     * com.softwaremagico.librodeesher.training.Training}'s own base cost, not modeled yet).
     */
    public ProfessionTrainingCost getTrainingCost(String trainingId) {
        for (final ProfessionTrainingCost cost : getTrainingCosts()) {
            if (cost.getTrainingName().equals(trainingId)) {
                return cost;
            }
        }
        return null;
    }

    /**
     * Whether this profession is the elementalist (the one whose own trainings are the "elemental
     * triad" wizard specializations, see {@link com.softwaremagico.librodeesher.magic.ElementalTriad}).
     *
     * <p>The legacy {@code Profession#isElementalist()} compared the profession's Spanish display
     * name against {@code Spanish.ELEMENTALIST_PROFESSION} ("Elementalista"); here the id-based
     * equivalent of that same single profession is checked instead ({@code "elementalist"}, the only
     * profession the migrated data marks as a spell caster whose training table lists the elemental
     * wizard trainings), matching how the rest of the migrated model resolves by id.</p>
     */
    public boolean isElementalist() {
        return ELEMENTALIST_PROFESSION_ID.equals(this.getId());
    }

    private static final String ELEMENTALIST_PROFESSION_ID = "elementalist";
}
