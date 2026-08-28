package com.softwaremagico.librodeesher.character;

import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.background.Background;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.characteristic.Appearance;
import com.softwaremagico.librodeesher.characteristic.Characteristic;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.decision.Decision;
import com.softwaremagico.librodeesher.decision.Decisions;
import com.softwaremagico.librodeesher.dice.Roll;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.training.ChoiceGroup;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
    private final Decisions decisions = new Decisions();

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
     * (only non-zero for {@link com.softwaremagico.librodeesher.category.CategoryType#PD}) plus the
     * flat bonus the selected profession grants this category, if any.
     *
     * <p>Race/background/perk/item bonuses are future work.</p>
     */
    public Integer getCategoryDevelopmentBonus(Category category) throws InvalidXmlElementException {
        return category.getCategoryRankBonus(getCategoryTotalRanks(category.getId())) + category.getFixedBonus()
                + getProfessionBonus(category.getId());
    }

    /**
     * A skill's bonus from its own ranks, using its category's progression table, plus the flat
     * bonus the selected profession grants this skill, if any.
     *
     * <p>Race/background/perk/item bonuses, the "real ranks" multiplier (restricted/common/
     * professional/generalized skills cost and count differently) and the characteristic bonus are
     * future work.</p>
     */
    public Integer getSkillDevelopmentBonus(Category category, String skillId) throws InvalidXmlElementException {
        return category.getSkillRankBonus(getSkillTotalRanks(skillId)) + getProfessionBonus(skillId);
    }

    /**
     * The flat bonus the selected profession grants a category or skill named {@code id}, or 0 if no
     * profession is selected or it grants that id none.
     */
    public Integer getProfessionBonus(String id) throws InvalidXmlElementException {
        final Profession profession = getProfession();
        return profession == null ? 0 : profession.getBonus(id);
    }

    /**
     * Whether {@code abbreviation} is the selected profession's primary or secondary preferred
     * characteristic, or {@code false} if no profession is selected.
     */
    public boolean isPreferredCharacteristic(CharacteristicAbbreviation abbreviation) throws InvalidXmlElementException {
        final Profession profession = getProfession();
        return profession != null && profession.isPreferredCharacteristic(abbreviation);
    }

    public Background getBackground() {
        return background;
    }

    /** Every choice the player has made so far for a category/skill/characteristic grant; see {@link Decisions}. */
    public Decisions getDecisions() {
        return decisions;
    }

    /**
     * Applies one of a training's (or culture's adolescence) category grants to the current level:
     * resolves which category it applies to (reusing an already-made decision if {@code key} was
     * decided before, otherwise validating {@code selectedCategoryId} and recording it), adds the
     * grant's ranks to that category, then does the same for each of its nested skill grants (keyed
     * as {@code key + ":skill:" + <index>}).
     *
     * <p>Ranks are added to the <em>current</em> level; call this once per level the grant actually
     * applies at (typically once, when the training/culture is first taken).</p>
     *
     * @param key                a caller-chosen id identifying this specific grant uniquely for this
     *                           character (e.g. {@code "training:soldier:category:1"}), reused as-is
     *                           on subsequent calls once a choice has been made.
     * @param grant              the category grant to apply.
     * @param selectedCategoryId the category to use if {@code grant} offers a choice and {@code key}
     *                           has not been decided yet; ignored otherwise (including when {@code
     *                           grant} is not a choice).
     * @param selectedSkillIds   the skill to use for each of {@code grant}'s nested skill grants that
     *                           offers a choice and has not been decided yet, in the same order as
     *                           {@link TrainingCategoryGrant#getSkills()}; ignored for grants that are
     *                           not a choice or are already decided.
     */
    public void applyCategoryGrant(String key, TrainingCategoryGrant grant, String selectedCategoryId, List<String> selectedSkillIds) {
        final Decision categoryDecision = decideOrReuse(key, () -> grant.resolve(selectedCategoryId));
        getCurrentLevel().addCategoryRanks(categoryDecision.getSelectedOption(), grant.getRanksGranted());

        final List<TrainingSkillGrant> skills = grant.getSkills();
        for (int i = 0; i < skills.size(); i++) {
            final TrainingSkillGrant skillGrant = skills.get(i);
            final String skillKey = key + ":skill:" + i;
            final String selectedSkillId = selectedSkillIds != null && i < selectedSkillIds.size() ? selectedSkillIds.get(i) : null;
            final Decision skillDecision = decideOrReuse(skillKey, () -> skillGrant.resolve(selectedSkillId));
            // Whether this is a spell skill is not resolved here (it requires cross-referencing the
            // skill's category), see LevelUp#setSkillRanks; future work.
            getCurrentLevel().addSkillRanks(skillDecision.getSelectedOption(), skillGrant.getRanksToDistribute(), false);
        }
    }

    /**
     * Applies every category grant of {@code training} to the current level, keying each one's
     * decision as {@code "training:" + training.getId() + ":category:" + <index>} (and its nested
     * skill grants as {@code ":skill:" + <index>} under that), reusing {@link #applyCategoryGrant}.
     *
     * @param categorySelections the category to use for each grant (by its index in {@link
     *                           Training#getCategories()}) that offers a choice and has not been
     *                           decided yet; entries for grants that are fixed or already decided are
     *                           ignored. May be {@code null} if no grant needs a fresh selection.
     * @param skillSelections    same, but for each grant's nested skill choices, keyed the same way.
     */
    public void applyTrainingCategories(Training training, Map<Integer, String> categorySelections,
                                         Map<Integer, List<String>> skillSelections) {
        applyCategoryGrants("training:" + training.getId() + ":category", training.getCategories(), categorySelections, skillSelections);
    }

    /**
     * Applies every adolescence-rank category grant of {@code culture} to the current level, keying
     * each one's decision as {@code "culture:" + culture.getId() + ":adolescence:" + <index>}; see
     * {@link #applyTrainingCategories} for the selection map semantics.
     */
    public void applyCultureAdolescenceRanks(Culture culture, Map<Integer, String> categorySelections,
                                              Map<Integer, List<String>> skillSelections) {
        applyCategoryGrants("culture:" + culture.getId() + ":adolescence", culture.getAdolescenceRanks(), categorySelections, skillSelections);
    }

    private void applyCategoryGrants(String keyPrefix, List<TrainingCategoryGrant> grants, Map<Integer, String> categorySelections,
                                      Map<Integer, List<String>> skillSelections) {
        for (int i = 0; i < grants.size(); i++) {
            final String selectedCategoryId = categorySelections == null ? null : categorySelections.get(i);
            final List<String> selectedSkillIds = skillSelections == null ? null : skillSelections.get(i);
            applyCategoryGrant(keyPrefix + ":" + i, grants.get(i), selectedCategoryId, selectedSkillIds);
        }
    }

    /**
     * Applies one of a training's "AUMENTOS CARACTERÍSTICAS" choices: resolves which characteristic
     * it applies to (reusing an already-made decision if {@code key} was decided before), rolls 2d10
     * and increases that characteristic's temporal value by {@link Characteristic#getCharacteristicUpgrade},
     * recording the roll in the current level.
     *
     * @param key                   a caller-chosen id identifying this specific choice uniquely for
     *                              this character (e.g. {@code "training:scout:characteristic:0"}).
     * @param group                 the characteristic choice to apply.
     * @param selectedCharacteristic the characteristic to use if {@code group} offers a choice and
     *                              {@code key} has not been decided yet; ignored otherwise.
     * @return the recorded roll.
     */
    public CharacteristicRoll applyCharacteristicUpgrade(String key, ChoiceGroup group, CharacteristicAbbreviation selectedCharacteristic) {
        final String selectedOption = selectedCharacteristic == null ? null : selectedCharacteristic.name();
        final Decision decision = decideOrReuse(key, () -> group.resolve(selectedOption));
        final CharacteristicAbbreviation abbreviation = CharacteristicAbbreviation.valueOf(decision.getSelectedOption());

        final Integer temporalValue = getCharacteristicTemporalValue(abbreviation);
        final Integer potentialValue = getCharacteristicPotentialValue(abbreviation);
        final Roll roll = new Roll();
        final Integer upgrade = Characteristic.getCharacteristicUpgrade(temporalValue, potentialValue, roll);
        setCharacteristicTemporalValue(abbreviation, temporalValue + upgrade);

        return getCurrentLevel().addCharacteristicUpdate(abbreviation, temporalValue, potentialValue, roll);
    }

    /** Returns the existing decision for {@code key}, or resolves it via {@code resolver} and records it. */
    private Decision decideOrReuse(String key, Supplier<Decision> resolver) {
        if (decisions.isDecided(key)) {
            return decisions.get(key);
        }
        final Decision decision = resolver.get();
        decisions.set(key, decision);
        return decision;
    }
}
