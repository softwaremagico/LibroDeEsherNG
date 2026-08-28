package com.softwaremagico.librodeesher.character;

import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.background.Background;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.characteristic.Appearance;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.rules.RulesCatalog;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A player character in progress.
 *
 * <p>This is a first, minimal cut of the character model: identity, race/culture/profession
 * selection (by id, resolved on demand through {@link RulesCatalog}) and characteristic values,
 * with the level-up/background/age bookkeeping already ported ({@link LevelUp}, {@link Background},
 * {@link com.softwaremagico.librodeesher.age.AgeRules}) wired in. Skills, training decisions, magic,
 * perks and equipment are future work, layered on top of this once their own character-state
 * equivalents (e.g. a {@code CharacterSkill} tracking bought ranks) are designed.</p>
 */
public class CharacterPlayer {

    private String name;
    private SexType sex = SexType.MALE;

    private String raceId;
    private String cultureId;
    private String professionId;

    private final Map<CharacteristicAbbreviation, Integer> characteristicTemporalValues = new EnumMap<>(CharacteristicAbbreviation.class);
    private final Map<CharacteristicAbbreviation, Integer> characteristicPotentialValues = new EnumMap<>(CharacteristicAbbreviation.class);
    private Appearance appearance = new Appearance();

    private int currentAge = AgeModification.INITIAL_AGE;
    private int finalAge = AgeModification.INITIAL_AGE;

    private final List<LevelUp> levels = new ArrayList<>();
    private final Background background = new Background();

    public CharacterPlayer() {
        for (final CharacteristicAbbreviation abbreviation : allRealCharacteristics()) {
            characteristicTemporalValues.put(abbreviation, Characteristics.INITIAL_CHARACTERISTIC_VALUE);
        }
        // Every character starts at level 1.
        levels.add(new LevelUp());
    }

    private static List<CharacteristicAbbreviation> allRealCharacteristics() {
        final List<CharacteristicAbbreviation> abbreviations = new ArrayList<>();
        for (final CharacteristicAbbreviation abbreviation : CharacteristicAbbreviation.values()) {
            if (abbreviation != CharacteristicAbbreviation.NONE && abbreviation != CharacteristicAbbreviation.REALM_OF_MAGIC) {
                abbreviations.add(abbreviation);
            }
        }
        return abbreviations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SexType getSex() {
        return sex;
    }

    public void setSex(SexType sex) {
        this.sex = sex;
    }

    public String getRaceId() {
        return raceId;
    }

    public void setRaceId(String raceId) {
        this.raceId = raceId;
    }

    /** Resolves the selected race, or {@code null} if none is selected yet. */
    public Race getRace() throws InvalidXmlElementException {
        return raceId == null ? null : RulesCatalog.getInstance().getRace(raceId);
    }

    public String getCultureId() {
        return cultureId;
    }

    public void setCultureId(String cultureId) {
        this.cultureId = cultureId;
    }

    /** Resolves the selected culture, or {@code null} if none is selected yet. */
    public Culture getCulture() throws InvalidXmlElementException {
        return cultureId == null ? null : RulesCatalog.getInstance().getCulture(cultureId);
    }

    public String getProfessionId() {
        return professionId;
    }

    public void setProfessionId(String professionId) {
        this.professionId = professionId;
    }

    /** Resolves the selected profession, or {@code null} if none is selected yet. */
    public Profession getProfession() throws InvalidXmlElementException {
        return professionId == null ? null : RulesCatalog.getInstance().getProfession(professionId);
    }

    public Integer getCharacteristicTemporalValue(CharacteristicAbbreviation abbreviation) {
        return characteristicTemporalValues.getOrDefault(abbreviation, 0);
    }

    public void setCharacteristicTemporalValue(CharacteristicAbbreviation abbreviation, Integer value) {
        characteristicTemporalValues.put(abbreviation, value);
    }

    public Integer getCharacteristicPotentialValue(CharacteristicAbbreviation abbreviation) {
        return characteristicPotentialValues.getOrDefault(abbreviation, 0);
    }

    public void setCharacteristicPotentialValue(CharacteristicAbbreviation abbreviation, Integer value) {
        characteristicPotentialValues.put(abbreviation, value);
    }

    /** Rolls (or re-rolls) the potential value for a characteristic from its current temporal value. */
    public Integer rollCharacteristicPotentialValue(CharacteristicAbbreviation abbreviation) {
        final Integer potential = Characteristics.getPotential(getCharacteristicTemporalValue(abbreviation));
        characteristicPotentialValues.put(abbreviation, potential);
        return potential;
    }

    public Integer getCharacteristicTemporalBonus(CharacteristicAbbreviation abbreviation) {
        return Characteristics.getTemporalBonus(getCharacteristicTemporalValue(abbreviation));
    }

    /** The race's fixed bonus for a characteristic, or 0 if no race is selected or it grants none. */
    public Integer getCharacteristicRaceBonus(CharacteristicAbbreviation abbreviation) throws InvalidXmlElementException {
        final Race race = getRace();
        if (race == null) {
            return 0;
        }
        return race.getCharacteristicBonuses().getOrDefault(abbreviation.name(), 0);
    }

    /**
     * The characteristic's total bonus: its temporal bonus plus the race's fixed bonus.
     *
     * <p>Background/perk/special bonuses are future work, to be added here once ported.
     * {@link CharacteristicAbbreviation#REALM_OF_MAGIC} (used by spell categories) is not resolved
     * here yet either: it requires knowing the caster's current realm of magic, which is future
     * (magic) work; it returns 0 for now.</p>
     */
    public Integer getCharacteristicTotalBonus(CharacteristicAbbreviation abbreviation) throws InvalidXmlElementException {
        if (abbreviation == CharacteristicAbbreviation.NONE || abbreviation == CharacteristicAbbreviation.REALM_OF_MAGIC) {
            return 0;
        }
        return getCharacteristicTemporalBonus(abbreviation) + getCharacteristicRaceBonus(abbreviation);
    }

    public Appearance getAppearance() {
        return appearance;
    }

    public void setAppearance(Appearance appearance) {
        this.appearance = appearance;
    }

    /** The Appearance characteristic's final value, combining the {@link Appearance} roll with Presence. */
    public int getAppearanceTotal() {
        return appearance.getTotal(getCharacteristicPotentialValue(CharacteristicAbbreviation.PRESENCE));
    }

    public int getCurrentAge() {
        return currentAge;
    }

    public void setCurrentAge(int currentAge) {
        this.currentAge = currentAge;
    }

    public int getFinalAge() {
        return finalAge;
    }

    public void setFinalAge(int finalAge) {
        this.finalAge = finalAge;
    }

    public List<LevelUp> getLevels() {
        return levels;
    }

    /** The character's current level (1-based), i.e. how many levels have been added so far. */
    public int getLevel() {
        return levels.size();
    }

    public LevelUp getCurrentLevel() {
        return levels.get(levels.size() - 1);
    }

    /** Adds a new, empty level and returns it, making it the current level. */
    public LevelUp increaseLevel() {
        final LevelUp levelUp = new LevelUp();
        levels.add(levelUp);
        return levelUp;
    }

    /**
     * Total ranks bought directly in a category (as opposed to in one of its skills), across every
     * level so far.
     *
     * <p>This only sums {@link LevelUp#getCategoryRanks(String)}; ranks granted by culture/training
     * selections are future work (they require a decision-resolution layer that does not exist yet,
     * since a grant may offer a choice of categories/skills).</p>
     */
    public Integer getCategoryTotalRanks(String categoryId) {
        int total = 0;
        for (final LevelUp levelUp : levels) {
            total += levelUp.getCategoryRanks(categoryId);
        }
        return total;
    }

    /**
     * Total ranks bought in a skill, across every level so far.
     *
     * <p>Same limitation as {@link #getCategoryTotalRanks(String)}: only sums {@link
     * LevelUp#getSkillRanks(String)}.</p>
     */
    public Integer getSkillTotalRanks(String skillId) {
        int total = 0;
        for (final LevelUp levelUp : levels) {
            total += levelUp.getSkillRanks(skillId);
        }
        return total;
    }

    /**
     * The bonus a category grants from ranks bought directly in it (only non-zero for {@link
     * com.softwaremagico.librodeesher.category.CategoryType#STANDARD} categories) plus its flat bonus
     * (only non-zero for {@link com.softwaremagico.librodeesher.category.CategoryType#PD}).
     *
     * <p>Profession/race/background/perk/item bonuses are future work.</p>
     */
    public Integer getCategoryDevelopmentBonus(Category category) {
        return category.getCategoryRankBonus(getCategoryTotalRanks(category.getId())) + category.getFixedBonus();
    }

    /**
     * A skill's bonus from its own ranks, using its category's progression table.
     *
     * <p>Profession/race/background/perk/item bonuses, the "real ranks" multiplier (restricted/
     * common/professional/generalized skills cost and count differently) and the characteristic
     * bonus are future work.</p>
     */
    public Integer getSkillDevelopmentBonus(Category category, String skillId) {
        return category.getSkillRankBonus(getSkillTotalRanks(skillId));
    }

    public Background getBackground() {
        return background;
    }
}
