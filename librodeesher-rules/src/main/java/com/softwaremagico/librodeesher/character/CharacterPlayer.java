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
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkGrade;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.profession.RealmOfMagicGrant;
import com.softwaremagico.librodeesher.race.RaceLanguage;
import com.softwaremagico.librodeesher.culture.CultureLanguageRank;
import com.softwaremagico.librodeesher.language.LanguageSlot;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.skill.SkillType;
import com.softwaremagico.librodeesher.training.ChoiceGroup;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    private final List<SelectedPerk> selectedPerks = new ArrayList<>();
    private final Map<String, Integer> hobbySkillRanks = new LinkedHashMap<>();

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

    /** Whether the selected race restricts {@code skillId}, or {@code false} if no race is selected. */
    public boolean isSkillRestrictedByRace(String skillId) throws InvalidXmlElementException {
        final Race race = getRace();
        return race != null && race.getRestrictedSkillIds().contains(skillId);
    }

    /** Whether the selected race treats {@code skillId} as common, or {@code false} if no race is selected. */
    public boolean isSkillCommonByRace(String skillId) throws InvalidXmlElementException {
        final Race race = getRace();
        return race != null && race.getCommonSkillIds().contains(skillId);
    }

    /** Whether the selected race restricts {@code categoryId}, or {@code false} if no race is selected. */
    public boolean isCategoryRestrictedByRace(String categoryId) throws InvalidXmlElementException {
        final Race race = getRace();
        return race != null && race.getRestrictedCategoryIds().contains(categoryId);
    }

    /** Whether the selected race treats {@code categoryId} as common, or {@code false} if no race is selected. */
    public boolean isCategoryCommonByRace(String categoryId) throws InvalidXmlElementException {
        final Race race = getRace();
        return race != null && race.getCommonCategoryIds().contains(categoryId);
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
     * <p>{@link #ALL_WEAPON_CATEGORIES}/{@link #ALL_ATTACK_CATEGORIES} wildcard markers in {@code
     * grant}'s category options (see {@code TrainingMigrationTool.resolveCategoryIds}) are expanded
     * into every real matching category id before resolving the decision, so {@code
     * selectedCategoryId} must be one of the real ids, not the wildcard marker itself.</p>
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
    public void applyCategoryGrant(String key, TrainingCategoryGrant grant, String selectedCategoryId, List<String> selectedSkillIds)
            throws InvalidXmlElementException {
        final List<String> offeredCategories = expandCategoryWildcards(grant.getCategoryOptions());
        final Decision categoryDecision = decideOrReuse(key,
                () -> offeredCategories.size() > 1 ? Decision.select(offeredCategories, selectedCategoryId) : Decision.fixed(offeredCategories));
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
                                         Map<Integer, List<String>> skillSelections) throws InvalidXmlElementException {
        applyCategoryGrants("training:" + training.getId() + ":category", training.getCategories(), categorySelections, skillSelections);
    }

    /**
     * Applies every adolescence-rank category grant of {@code culture} to the current level, keying
     * each one's decision as {@code "culture:" + culture.getId() + ":adolescence:" + <index>}; see
     * {@link #applyTrainingCategories} for the selection map semantics.
     */
    public void applyCultureAdolescenceRanks(Culture culture, Map<Integer, String> categorySelections,
                                              Map<Integer, List<String>> skillSelections) throws InvalidXmlElementException {
        applyCategoryGrants("culture:" + culture.getId() + ":adolescence", culture.getAdolescenceRanks(), categorySelections, skillSelections);
    }

    private void applyCategoryGrants(String keyPrefix, List<TrainingCategoryGrant> grants, Map<Integer, String> categorySelections,
                                      Map<Integer, List<String>> skillSelections) throws InvalidXmlElementException {
        for (int i = 0; i < grants.size(); i++) {
            final String selectedCategoryId = categorySelections == null ? null : categorySelections.get(i);
            final List<String> selectedSkillIds = skillSelections == null ? null : skillSelections.get(i);
            applyCategoryGrant(keyPrefix + ":" + i, grants.get(i), selectedCategoryId, selectedSkillIds);
        }
    }

    /**
     * Resolves every choice in a training's "HABILIDADES DE ESTILO DE VIDA"/"COMUNES"/
     * "PROFESIONALES"/"RESTRINGIDAS" sections, recording which skill each one grants (as opposed to
     * how many ranks: these sections make a skill available at a favourable cost tier rather than
     * granting ranks directly, so there is nothing to add to {@link LevelUp} here).
     *
     * <p>Skill names here are not yet resolved to {@code Skill} ids (see {@link Training}'s class
     * javadoc), so {@code selectedXxxSkillId} values and the results of {@link
     * #getTrainingCommonSkills}/etc. are still the plain (Spanish) skill name.</p>
     *
     * @param lifeSkillSelections         the skill to use for each of {@link Training#getLifeSkills()}
     *                                    that offers a choice and has not been decided yet, by index.
     * @param commonSkillSelections       same, for {@link Training#getCommonSkills()}.
     * @param professionalSkillSelections same, for {@link Training#getProfessionalSkills()}.
     * @param restrictedSkillSelections   same, for {@link Training#getRestrictedSkills()}.
     */
    public void applyTrainingSkillChoices(Training training, Map<Integer, String> lifeSkillSelections,
                                           Map<Integer, String> commonSkillSelections,
                                           Map<Integer, String> professionalSkillSelections,
                                           Map<Integer, String> restrictedSkillSelections) {
        final String prefix = "training:" + training.getId();
        applyChoiceGroups(prefix + ":lifeSkill", training.getLifeSkills(), lifeSkillSelections);
        applyChoiceGroups(prefix + ":commonSkill", training.getCommonSkills(), commonSkillSelections);
        applyChoiceGroups(prefix + ":professionalSkill", training.getProfessionalSkills(), professionalSkillSelections);
        applyChoiceGroups(prefix + ":restrictedSkill", training.getRestrictedSkills(), restrictedSkillSelections);
    }

    public List<String> getTrainingLifeSkills(Training training) {
        return getDecidedOptions("training:" + training.getId() + ":lifeSkill", training.getLifeSkills().size());
    }

    public List<String> getTrainingCommonSkills(Training training) {
        return getDecidedOptions("training:" + training.getId() + ":commonSkill", training.getCommonSkills().size());
    }

    public List<String> getTrainingProfessionalSkills(Training training) {
        return getDecidedOptions("training:" + training.getId() + ":professionalSkill", training.getProfessionalSkills().size());
    }

    public List<String> getTrainingRestrictedSkills(Training training) {
        return getDecidedOptions("training:" + training.getId() + ":restrictedSkill", training.getRestrictedSkills().size());
    }

    private void applyChoiceGroups(String keyPrefix, List<ChoiceGroup> groups, Map<Integer, String> selections) {
        for (int i = 0; i < groups.size(); i++) {
            final ChoiceGroup group = groups.get(i);
            final String selectedOption = selections == null ? null : selections.get(i);
            decideOrReuse(keyPrefix + ":" + i, () -> group.resolve(selectedOption));
        }
    }

    /** The selected option of every decision {@code keyPrefix + ":0"} through {@code keyPrefix + ":" + (count - 1)}. */
    private List<String> getDecidedOptions(String keyPrefix, int count) {
        final List<String> selected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final String option = decisions.getSelectedOption(keyPrefix + ":" + i);
            if (option != null) {
                selected.add(option);
            }
        }
        return selected;
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

    /**
     * Pseudo-category marker (see {@code TrainingMigrationTool.resolveCategoryIds}) standing for
     * "any weapon category", expanded by {@link #expandCategoryWildcards(List)} into every category
     * id starting with "weapons" (matching {@code CategoryMigrationTool}'s id convention for every
     * "Armas·&lt;Tipo&gt;" category).
     */
    public static final String ALL_WEAPON_CATEGORIES = "allWeaponCategories";

    /**
     * Pseudo-category marker standing for "any non-weapon attack category" (martial arts strikes/
     * sweeps/combat maneuvers, special attacks), matching the exact set the legacy application
     * hardcoded in {@code CategoryFactory.getOthersAttack()}.
     */
    public static final String ALL_ATTACK_CATEGORIES = "allAttackCategories";

    private static final List<String> ATTACK_CATEGORY_IDS = List.of(
            "martialArtsStrikes", "martialArtsSweeps", "martialArtsCombatManeuvers", "specialAttacks");

    private static final String OPTIONAL_RACE_LANGUAGE_PREFIX = "language:optionalRace:";
    private static final String OPTIONAL_BACKGROUND_LANGUAGE_PREFIX = "language:optionalBackground:";
    private static final String OPTIONAL_CULTURE_LANGUAGE_PREFIX = "language:optionalCulture:";

    /**
     * Expands any {@link #ALL_WEAPON_CATEGORIES}/{@link #ALL_ATTACK_CATEGORIES} marker in {@code
     * categoryIds} into the real category ids it stands for (every other id is kept as-is). The
     * actual set depends on which modules are currently enabled (through {@link RulesCatalog}), so
     * this cannot be resolved once and for all at migration time.
     */
    public List<String> expandCategoryWildcards(List<String> categoryIds) throws InvalidXmlElementException {
        final List<String> expanded = new ArrayList<>();
        for (final String categoryId : categoryIds) {
            if (ALL_WEAPON_CATEGORIES.equals(categoryId)) {
                expanded.addAll(getWeaponCategoryIds());
            } else if (ALL_ATTACK_CATEGORIES.equals(categoryId)) {
                expanded.addAll(ATTACK_CATEGORY_IDS);
            } else {
                expanded.add(categoryId);
            }
        }
        return expanded;
    }

    private List<String> getWeaponCategoryIds() throws InvalidXmlElementException {
        final List<String> ids = new ArrayList<>();
        for (final Category category : RulesCatalog.getInstance().getCategories()) {
            if (category.getId().startsWith("weapons")) {
                ids.add(category.getId());
            }
        }
        return ids;
    }

    /**
     * Whether {@code skillName} was granted as a "generalized" skill (marked directly on a level, not
     * derived from anything else) in any level so far.
     */
    public boolean isSkillGeneralized(String skillName) {
        for (final LevelUp levelUp : levels) {
            if (levelUp.getGeneralizedSkills().contains(skillName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether any training taken so far (see {@link LevelUp#getTrainings()}) granted {@code
     * skillName} through its {@code trainingSkillsGetter} section (common/professional/restricted).
     */
    private boolean isSkillGrantedByAnyTraining(String skillName, Function<Training, List<String>> trainingSkillsGetter)
            throws InvalidXmlElementException {
        for (final LevelUp levelUp : levels) {
            for (final String trainingId : levelUp.getTrainings()) {
                final Training training = RulesCatalog.getInstance().getTraining(trainingId);
                if (trainingSkillsGetter.apply(training).contains(skillName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether {@code skill} is restricted: either by its own {@link SkillType#RESTRICTED} tag,
     * because a training taken so far grants it as one of its restricted skills, or because the
     * selected race restricts it (matched by {@code skill.getId()} against {@link
     * Race#getRestrictedSkillIds()}).
     *
     * <p>The legacy rule also considers profession and perk classifications; those are future work
     * (they need {@code Profession}'s not-yet-parsed skill sections, and perk skill classification,
     * respectively).</p>
     */
    public boolean isSkillRestricted(Skill skill) throws InvalidXmlElementException {
        return skill.getSkillType() == SkillType.RESTRICTED
                || isSkillGrantedByAnyTraining(skill.getName().getSpanish(), this::getTrainingRestrictedSkills)
                || isSkillRestrictedByRace(skill.getId());
    }

    /** Same limitation as {@link #isSkillRestricted(Skill)}, for {@link SkillType#COMMON}. */
    public boolean isSkillCommon(Skill skill) throws InvalidXmlElementException {
        return skill.getSkillType() == SkillType.COMMON
                || isSkillGrantedByAnyTraining(skill.getName().getSpanish(), this::getTrainingCommonSkills)
                || isSkillCommonByRace(skill.getId());
    }

    /** Same limitation as {@link #isSkillRestricted(Skill)}, for {@link SkillType#PROFESSIONAL}. */
    public boolean isSkillProfessional(Skill skill) throws InvalidXmlElementException {
        return skill.getSkillType() == SkillType.PROFESSIONAL
                || isSkillGrantedByAnyTraining(skill.getName().getSpanish(), this::getTrainingProfessionalSkills);
    }

    /**
     * The multiplier applied to a skill's bought ranks to get its "real ranks" (used for skill bonus
     * purposes, as opposed to category-bonus purposes): restricted skills count for half, professional
     * skills for triple, common skills for double, and a generalized skill counts fully only if it is
     * also common or professional (half otherwise); standard skills count for their full value.
     */
    public double getSkillRankMultiplier(Skill skill) throws InvalidXmlElementException {
        if (isSkillRestricted(skill)) {
            return 0.5;
        }
        final boolean common = isSkillCommon(skill);
        final boolean professional = isSkillProfessional(skill);
        if (isSkillGeneralized(skill.getName().getSpanish())) {
            return common || professional ? 1 : 0.5;
        }
        if (professional) {
            return 3;
        }
        if (common) {
            return 2;
        }
        return 1;
    }

    /**
     * A skill's "real ranks": its bought ranks times {@link #getSkillRankMultiplier}.
     *
     * <p>Ranks are looked up by {@code skill.getName().getSpanish()}, not {@code skill.getId()}: a
     * training's skill grants are not yet resolved to real {@code Skill} ids (see {@code Training}'s
     * class javadoc), so {@link #getSkillTotalRanks(String)} is keyed by the raw Spanish skill name
     * for skills granted this way; future work once that cross-reference is resolved.</p>
     */
    public int getSkillRealRanks(Skill skill) throws InvalidXmlElementException {
        return (int) (getSkillTotalRanks(skill.getName().getSpanish()) * getSkillRankMultiplier(skill));
    }

    public List<SelectedPerk> getSelectedPerks() {
        return selectedPerks;
    }

    public boolean isPerkSelected(String perkId) {
        return findSelectedPerk(perkId) != null;
    }

    /** Selects {@code perkId}, if it was not already selected. */
    public void addPerk(String perkId) {
        if (!isPerkSelected(perkId)) {
            selectedPerks.add(new SelectedPerk(perkId));
        }
    }

    /** Unselects {@code perkId} and forgets any weakness paired with it. */
    public void removePerk(String perkId) {
        selectedPerks.removeIf(selectedPerk -> selectedPerk.getPerkId().equals(perkId));
    }

    /** Pairs an already-selected perk with a weakness, discounting the perk's background points cost. */
    public void setWeakness(String perkId, String weaknessPerkId) {
        final SelectedPerk selectedPerk = findSelectedPerk(perkId);
        if (selectedPerk != null) {
            selectedPerk.setWeaknessId(weaknessPerkId);
        }
    }

    public boolean hasWeakness(String perkId) {
        final SelectedPerk selectedPerk = findSelectedPerk(perkId);
        return selectedPerk != null && selectedPerk.getWeaknessId() != null;
    }

    /** Marks an already-selected perk as chosen by random character generation (not player-removable). */
    public void setPerkAsRandom(String perkId, boolean random) {
        final SelectedPerk selectedPerk = findSelectedPerk(perkId);
        if (selectedPerk != null) {
            selectedPerk.setRandom(random);
        }
    }

    public boolean isPerkRandom(String perkId) {
        final SelectedPerk selectedPerk = findSelectedPerk(perkId);
        return selectedPerk != null && selectedPerk.isRandom();
    }

    private SelectedPerk findSelectedPerk(String perkId) {
        for (final SelectedPerk selectedPerk : selectedPerks) {
            if (selectedPerk.getPerkId().equals(perkId)) {
                return selectedPerk;
            }
        }
        return null;
    }

    /**
     * Total background points spent on every selected perk so far, discounted by any weakness paired
     * with each one and by whether it was chosen at random (see {@link PerkGrade#getBackgroundCost}).
     * A perk that is itself a weakness ({@link Perk#isWeakness()}) costs nothing here, matching the
     * legacy rule (its points are refunded some other way, not modeled yet).
     */
    public int getPerksBackgroundPointsCost() throws InvalidXmlElementException {
        int cost = 0;
        for (final SelectedPerk selectedPerk : selectedPerks) {
            final Perk perk = RulesCatalog.getInstance().getPerk(selectedPerk.getPerkId());
            if (perk.isWeakness()) {
                continue;
            }
            final Perk weakness = selectedPerk.getWeaknessId() == null ? null
                    : RulesCatalog.getInstance().getPerk(selectedPerk.getWeaknessId());
            cost += perk.getGrade().getBackgroundCost(weakness == null ? null : weakness.getGrade(), selectedPerk.isRandom());
        }
        return cost;
    }

    /**
     * Background points left to spend: the race's total ({@link Race#getBackgroundPoints()}, 0
     * without a race selected) minus what {@link Background} and the selected perks have spent so far.
     */
    public int getRemainingBackgroundPoints() throws InvalidXmlElementException {
        final Race race = getRace();
        final int totalBackgroundPoints = race == null || race.getBackgroundPoints() == null ? 0 : race.getBackgroundPoints();
        return totalBackgroundPoints - background.getSpentBackgroundPoints() - getPerksBackgroundPointsCost();
    }

    /**
     * Resolves one of the selected profession's realm-of-magic grants: for a fixed grant, {@code
     * selectedRealm} is ignored and its single realm is used; for a hybrid choice (e.g.
     * "Esencia/Canalización"), {@code selectedRealm} must be one of {@link RealmOfMagicGrant#getOptions()}.
     *
     * @param key           a caller-chosen id identifying this specific grant uniquely for this
     *                      character (e.g. {@code "profession:sorcerer:realm:0"}), reused as-is on
     *                      subsequent calls once a choice has been made.
     * @param grant         the realm grant to resolve.
     * @param selectedRealm the realm to use if {@code grant} offers a choice and {@code key} has not
     *                      been decided yet; ignored otherwise.
     */
    public RealmOfMagic applyMagicRealmChoice(String key, RealmOfMagicGrant grant, RealmOfMagic selectedRealm) {
        final List<String> offeredRealms = new ArrayList<>();
        for (final RealmOfMagic realm : grant.getOptions()) {
            offeredRealms.add(realm.name());
        }
        final String selectedOption = selectedRealm == null ? null : selectedRealm.name();
        final Decision decision = decideOrReuse(key,
                () -> grant.isChoice() ? Decision.select(offeredRealms, selectedOption) : Decision.fixed(offeredRealms));
        return RealmOfMagic.valueOf(decision.getSelectedOption());
    }

    /**
     * Resolves every one of the selected profession's realm-of-magic grants (see {@link
     * #applyMagicRealmChoice}), keying each one's decision as {@code "profession:" +
     * profession.getId() + ":realm:" + <index>}, and returns every resolved realm: the character is a
     * caster of all of them at once (as opposed to the alternatives within a single hybrid grant,
     * which are mutually exclusive). Returns an empty list if no profession is selected.
     *
     * @param realmSelections the realm to use for each grant (by its index in {@link
     *                        Profession#getMagicRealms()}) that offers a choice and has not been
     *                        decided yet. May be {@code null} if no grant needs a fresh selection.
     */
    public List<RealmOfMagic> applyProfessionMagicRealms(Map<Integer, RealmOfMagic> realmSelections) throws InvalidXmlElementException {
        final Profession profession = getProfession();
        if (profession == null) {
            return List.of();
        }
        final List<RealmOfMagic> realms = new ArrayList<>();
        final List<RealmOfMagicGrant> grants = profession.getMagicRealms();
        for (int i = 0; i < grants.size(); i++) {
            final RealmOfMagic selectedRealm = realmSelections == null ? null : realmSelections.get(i);
            realms.add(applyMagicRealmChoice("profession:" + profession.getId() + ":realm:" + i, grants.get(i), selectedRealm));
        }
        return realms;
    }

    /**
     * The fixed speaking ranks a race grants at creation for a language (0 if no race is selected or
     * it does not grant that language at creation), including a language assigned to one of the
     * race's optional "Idioma Racial"/"Idioma Regional" slots (see {@link #assignOptionalRaceLanguage}).
     */
    public int getRaceLanguageStartingSpeakingRanks(String languageId) throws InvalidXmlElementException {
        final RaceLanguage language = findRaceLanguage(languageId, false);
        final int named = language == null || language.getStartingSpeakingRanks() == null ? 0 : language.getStartingSpeakingRanks();
        return Math.max(named, getOptionalRaceLanguageRanks(languageId, LanguageSlot::getStartingSpeakingRanks, false));
    }

    /** Same as {@link #getRaceLanguageStartingSpeakingRanks(String)}, for writing ranks. */
    public int getRaceLanguageStartingWritingRanks(String languageId) throws InvalidXmlElementException {
        final RaceLanguage language = findRaceLanguage(languageId, false);
        final int named = language == null || language.getStartingWritingRanks() == null ? 0 : language.getStartingWritingRanks();
        return Math.max(named, getOptionalRaceLanguageRanks(languageId, LanguageSlot::getStartingWritingRanks, false));
    }

    /**
     * The maximum speaking ranks a language can reach for the selected race, checking both the
     * languages granted at creation and those only available through background points, plus a
     * language assigned to any optional slot; the legacy default of 10 applies only if the race
     * mentions the language nowhere at all (0 without a race selected, since there is no race to ask).
     */
    public int getRaceLanguageMaxSpeakingRanks(String languageId) throws InvalidXmlElementException {
        if (getRace() == null) {
            return 0;
        }
        final RaceLanguage language = findRaceLanguage(languageId, true);
        if (language != null && language.getMaxSpeakingRanks() != null) {
            return language.getMaxSpeakingRanks();
        }
        final int fromSlot = getOptionalRaceLanguageRanks(languageId, LanguageSlot::getMaxSpeakingRanks, true);
        return fromSlot > 0 ? fromSlot : 10;
    }

    /** Same as {@link #getRaceLanguageMaxSpeakingRanks(String)}, for writing ranks. */
    public int getRaceLanguageMaxWritingRanks(String languageId) throws InvalidXmlElementException {
        if (getRace() == null) {
            return 0;
        }
        final RaceLanguage language = findRaceLanguage(languageId, true);
        if (language != null && language.getMaxWritingRanks() != null) {
            return language.getMaxWritingRanks();
        }
        final int fromSlot = getOptionalRaceLanguageRanks(languageId, LanguageSlot::getMaxWritingRanks, true);
        return fromSlot > 0 ? fromSlot : 10;
    }

    private RaceLanguage findRaceLanguage(String languageId, boolean includeBackgroundLanguages) throws InvalidXmlElementException {
        final Race race = getRace();
        if (race == null) {
            return null;
        }
        for (final RaceLanguage language : race.getRaceLanguages()) {
            if (language.getLanguageId().equals(languageId)) {
                return language;
            }
        }
        if (includeBackgroundLanguages) {
            for (final RaceLanguage language : race.getBackgroundLanguages()) {
                if (language.getLanguageId().equals(languageId)) {
                    return language;
                }
            }
        }
        return null;
    }

    /**
     * Assigns {@code languageId} to one of the race's optional "IDIOMAS" slots (by its index in
     * {@link Race#getOptionalRaceLanguages()}), recording the choice so {@link
     * #getRaceLanguageStartingSpeakingRanks}/{@link #getRaceLanguageMaxSpeakingRanks} (and their
     * writing-rank counterparts) pick up that slot's ranks for {@code languageId}.
     */
    public void assignOptionalRaceLanguage(int slotIndex, String languageId) {
        decisions.set(OPTIONAL_RACE_LANGUAGE_PREFIX + slotIndex, Decision.fixed(List.of(languageId)));
    }

    public String getOptionalRaceLanguageAssignment(int slotIndex) {
        return decisions.getSelectedOption(OPTIONAL_RACE_LANGUAGE_PREFIX + slotIndex);
    }

    /** Same as {@link #assignOptionalRaceLanguage}, but for {@link Race#getOptionalBackgroundLanguages()}. */
    public void assignOptionalBackgroundLanguage(int slotIndex, String languageId) {
        decisions.set(OPTIONAL_BACKGROUND_LANGUAGE_PREFIX + slotIndex, Decision.fixed(List.of(languageId)));
    }

    public String getOptionalBackgroundLanguageAssignment(int slotIndex) {
        return decisions.getSelectedOption(OPTIONAL_BACKGROUND_LANGUAGE_PREFIX + slotIndex);
    }

    private int getOptionalRaceLanguageRanks(String languageId, Function<LanguageSlot, Integer> rankGetter,
                                              boolean includeBackgroundLanguages) throws InvalidXmlElementException {
        final Race race = getRace();
        if (race == null) {
            return 0;
        }
        int best = matchingSlotRank(race.getOptionalRaceLanguages(), OPTIONAL_RACE_LANGUAGE_PREFIX, languageId, rankGetter);
        if (includeBackgroundLanguages) {
            best = Math.max(best, matchingSlotRank(race.getOptionalBackgroundLanguages(), OPTIONAL_BACKGROUND_LANGUAGE_PREFIX,
                    languageId, rankGetter));
        }
        return best;
    }

    private int matchingSlotRank(List<LanguageSlot> slots, String keyPrefix, String languageId, Function<LanguageSlot, Integer> rankGetter) {
        int best = 0;
        for (int i = 0; i < slots.size(); i++) {
            if (languageId.equals(decisions.getSelectedOption(keyPrefix + i))) {
                final Integer rank = rankGetter.apply(slots.get(i));
                best = Math.max(best, rank == null ? 0 : rank);
            }
        }
        return best;
    }

    /**
     * The maximum speaking ranks a language can reach for the selected culture, resolving the {@code
     * "all"} marker (a culture that caps every language the same way) as a fallback when the language
     * is not mentioned by name, plus a language assigned to one of the culture's optional "Idioma
     * Regional" slots (see {@link #assignOptionalCultureLanguage}); 0 if no culture is selected or it
     * does not mention that language anywhere.
     */
    public int getCultureLanguageMaxSpeakingRanks(String languageId) throws InvalidXmlElementException {
        final CultureLanguageRank rank = findCultureLanguageRank(languageId);
        if (rank != null && rank.getMaxSpeakingRanks() != null) {
            return rank.getMaxSpeakingRanks();
        }
        return getOptionalCultureLanguageMaxRanks(languageId, LanguageSlot::getMaxSpeakingRanks);
    }

    /** Same as {@link #getCultureLanguageMaxSpeakingRanks(String)}, for writing ranks. */
    public int getCultureLanguageMaxWritingRanks(String languageId) throws InvalidXmlElementException {
        final CultureLanguageRank rank = findCultureLanguageRank(languageId);
        if (rank != null && rank.getMaxWritingRanks() != null) {
            return rank.getMaxWritingRanks();
        }
        return getOptionalCultureLanguageMaxRanks(languageId, LanguageSlot::getMaxWritingRanks);
    }

    private CultureLanguageRank findCultureLanguageRank(String languageId) throws InvalidXmlElementException {
        final Culture culture = getCulture();
        if (culture == null) {
            return null;
        }
        CultureLanguageRank allLanguagesRank = null;
        for (final CultureLanguageRank rank : culture.getLanguageMaxRanks()) {
            if (rank.getLanguageId().equals(languageId)) {
                return rank;
            }
            if ("all".equals(rank.getLanguageId())) {
                allLanguagesRank = rank;
            }
        }
        return allLanguagesRank;
    }

    /** Assigns {@code languageId} to one of the culture's optional "IDIOMAS" slots; see {@link #assignOptionalRaceLanguage}. */
    public void assignOptionalCultureLanguage(int slotIndex, String languageId) {
        decisions.set(OPTIONAL_CULTURE_LANGUAGE_PREFIX + slotIndex, Decision.fixed(List.of(languageId)));
    }

    public String getOptionalCultureLanguageAssignment(int slotIndex) {
        return decisions.getSelectedOption(OPTIONAL_CULTURE_LANGUAGE_PREFIX + slotIndex);
    }

    private int getOptionalCultureLanguageMaxRanks(String languageId, Function<LanguageSlot, Integer> rankGetter) throws InvalidXmlElementException {
        final Culture culture = getCulture();
        if (culture == null) {
            return 0;
        }
        return matchingSlotRank(culture.getOptionalLanguages(), OPTIONAL_CULTURE_LANGUAGE_PREFIX, languageId, rankGetter);
    }

    /**
     * The highest speaking-ranks cap a language can reach, taking the best of what the selected race
     * and culture allow (the legacy rule takes the highest value offered by any source that mentions
     * a language, including any assigned optional-language slot). 0 if neither mentions it.
     *
     * <p>See {@link #setBackgroundLanguageRank(String, int)} for validating background points spent
     * on a language against this cap.</p>
     */
    public int getLanguageMaxSpeakingRanks(String languageId) throws InvalidXmlElementException {
        return Math.max(getRaceLanguageMaxSpeakingRanks(languageId), getCultureLanguageMaxSpeakingRanks(languageId));
    }

    /** Same as {@link #getLanguageMaxSpeakingRanks(String)}, for writing ranks. */
    public int getLanguageMaxWritingRanks(String languageId) throws InvalidXmlElementException {
        return Math.max(getRaceLanguageMaxWritingRanks(languageId), getCultureLanguageMaxWritingRanks(languageId));
    }

    /**
     * Sets how many background points' worth of ranks were spent on {@code languageId} (see {@link
     * Background#setHistoryLanguageRank}), rejecting the change (leaving it at 0) if {@code ranks}
     * alone would exceed {@link #getLanguageMaxSpeakingRanks(String)}, matching the legacy rule.
     */
    public void setBackgroundLanguageRank(String languageId, int ranks) throws InvalidXmlElementException {
        background.setHistoryLanguageRank(languageId, ranks);
        if (background.getHistoryLanguageRank(languageId) > getLanguageMaxSpeakingRanks(languageId)) {
            background.setHistoryLanguageRank(languageId, 0);
        }
    }

    /**
     * Sets how many of the selected culture's hobby points were spent on {@code skillId} (the
     * legacy "AFICIONES" section's free ranks), matching {@code CultureDecisions#setHobbyRanks}
     * exactly: unlike {@link #setBackgroundLanguageRank}, the legacy application never validated
     * this against {@link Culture#getHobbyRanks()} or {@link Culture#isHobbySkillAllowed(String)} at
     * the model level (only in its UI), so this does not either; use those two query methods to
     * validate a choice before calling this, if desired.
     */
    public void setHobbySkillRank(String skillId, int ranks) {
        if (ranks <= 0) {
            hobbySkillRanks.remove(skillId);
        } else {
            hobbySkillRanks.put(skillId, ranks);
        }
    }

    public int getHobbySkillRank(String skillId) {
        return hobbySkillRanks.getOrDefault(skillId, 0);
    }

    /** The sum of every hobby rank spent so far, to compare against the selected culture's {@link Culture#getHobbyRanks()}. */
    public int getTotalHobbySkillRanks() {
        int total = 0;
        for (final int ranks : hobbySkillRanks.values()) {
            total += ranks;
        }
        return total;
    }

    /**
     * The selected culture's discount/markup on {@code trainingId}'s background points cost (see
     * {@link Culture#getTrainingPricePercentage(String)}), or {@code 1.0} (no change) if no culture
     * is selected.
     */
    public double getCultureTrainingPricePercentage(String trainingId) throws InvalidXmlElementException {
        final Culture culture = getCulture();
        return culture == null ? 1.0 : culture.getTrainingPricePercentage(trainingId);
    }
}
