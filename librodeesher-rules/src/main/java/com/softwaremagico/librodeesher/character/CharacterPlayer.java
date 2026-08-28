package com.softwaremagico.librodeesher.character;

import com.softwaremagico.librodeesher.age.AgeModification;
import com.softwaremagico.librodeesher.age.AgeRules;
import com.softwaremagico.librodeesher.background.Background;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.characteristic.Appearance;
import com.softwaremagico.librodeesher.characteristic.Characteristic;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.CharacteristicRoll;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.culture.CultureLanguageRank;
import com.softwaremagico.librodeesher.decision.Decision;
import com.softwaremagico.librodeesher.decision.Decisions;
import com.softwaremagico.librodeesher.dice.Roll;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.language.LanguageSlot;
import com.softwaremagico.librodeesher.level.LevelUp;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkBonus;
import com.softwaremagico.librodeesher.perk.PerkBonusKind;
import com.softwaremagico.librodeesher.perk.PerkChoiceGrant;
import com.softwaremagico.librodeesher.perk.PerkChoiceScope;
import com.softwaremagico.librodeesher.perk.PerkGrade;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.RealmOfMagicGrant;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.race.RaceLanguage;
import com.softwaremagico.librodeesher.resistance.ResistanceType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
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
 * <p>
 * This is a first, minimal cut of the character model: identity,
 * race/culture/profession selection (by id, resolved on demand through
 * {@link RulesCatalog}) and characteristic values, with the
 * level-up/background/age bookkeeping already ported ({@link LevelUp},
 * {@link Background}, {@link com.softwaremagico.librodeesher.age.AgeRules})
 * wired in. Skills, training decisions, magic, perks and equipment are future
 * work, layered on top of this once their own character-state equivalents (e.g.
 * a {@code CharacterSkill} tracking bought ranks) are designed.
 * </p>
 */
public class CharacterPlayer {

	private String name;
	private SexType sex = SexType.MALE;

	private String raceId;
	private String cultureId;
	private String professionId;

	private final Map<CharacteristicAbbreviation, Integer> characteristicTemporalValues = new EnumMap<>(
			CharacteristicAbbreviation.class);
	private final Map<CharacteristicAbbreviation, Integer> characteristicPotentialValues = new EnumMap<>(
			CharacteristicAbbreviation.class);
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
			this.characteristicTemporalValues.put(abbreviation, Characteristics.INITIAL_CHARACTERISTIC_VALUE);
		}
		// Every character starts at level 1.
		this.levels.add(new LevelUp());
	}

	private static List<CharacteristicAbbreviation> allRealCharacteristics() {
		final List<CharacteristicAbbreviation> abbreviations = new ArrayList<>();
		for (final CharacteristicAbbreviation abbreviation : CharacteristicAbbreviation.values()) {
			if (abbreviation != CharacteristicAbbreviation.NONE
					&& abbreviation != CharacteristicAbbreviation.REALM_OF_MAGIC) {
				abbreviations.add(abbreviation);
			}
		}
		return abbreviations;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SexType getSex() {
		return this.sex;
	}

	public void setSex(SexType sex) {
		this.sex = sex;
	}

	public String getRaceId() {
		return this.raceId;
	}

	public void setRaceId(String raceId) {
		this.raceId = raceId;
	}

	/** Resolves the selected race, or {@code null} if none is selected yet. */
	public Race getRace() throws InvalidXmlElementException {
		return this.raceId == null ? null : RulesCatalog.getInstance().getRace(this.raceId);
	}

	public String getCultureId() {
		return this.cultureId;
	}

	public void setCultureId(String cultureId) {
		this.cultureId = cultureId;
	}

	/** Resolves the selected culture, or {@code null} if none is selected yet. */
	public Culture getCulture() throws InvalidXmlElementException {
		return this.cultureId == null ? null : RulesCatalog.getInstance().getCulture(this.cultureId);
	}

	public String getProfessionId() {
		return this.professionId;
	}

	public void setProfessionId(String professionId) {
		this.professionId = professionId;
	}

	/**
	 * Resolves the selected profession, or {@code null} if none is selected yet.
	 */
	public Profession getProfession() throws InvalidXmlElementException {
		return this.professionId == null ? null : RulesCatalog.getInstance().getProfession(this.professionId);
	}

	public Integer getCharacteristicTemporalValue(CharacteristicAbbreviation abbreviation) {
		return this.characteristicTemporalValues.getOrDefault(abbreviation, 0);
	}

	public void setCharacteristicTemporalValue(CharacteristicAbbreviation abbreviation, Integer value) {
		this.characteristicTemporalValues.put(abbreviation, value);
	}

	public Integer getCharacteristicPotentialValue(CharacteristicAbbreviation abbreviation) {
		return this.characteristicPotentialValues.getOrDefault(abbreviation, 0);
	}

	public void setCharacteristicPotentialValue(CharacteristicAbbreviation abbreviation, Integer value) {
		this.characteristicPotentialValues.put(abbreviation, value);
	}

	/**
	 * Rolls (or re-rolls) the potential value for a characteristic from its current
	 * temporal value.
	 */
	public Integer rollCharacteristicPotentialValue(CharacteristicAbbreviation abbreviation) {
		final Integer potential = Characteristics.getPotential(this.getCharacteristicTemporalValue(abbreviation));
		this.characteristicPotentialValues.put(abbreviation, potential);
		return potential;
	}

	public Integer getCharacteristicTemporalBonus(CharacteristicAbbreviation abbreviation) {
		return Characteristics.getTemporalBonus(this.getCharacteristicTemporalValue(abbreviation));
	}

	/**
	 * The race's fixed bonus for a characteristic, or 0 if no race is selected or
	 * it grants none.
	 */
	public Integer getCharacteristicRaceBonus(CharacteristicAbbreviation abbreviation)
			throws InvalidXmlElementException {
		final Race race = this.getRace();
		if (race == null) {
			return 0;
		}
		return race.getCharacteristicBonuses().getOrDefault(abbreviation.name(), 0);
	}

	/**
	 * Whether the selected race restricts {@code skillId}, or {@code false} if no
	 * race is selected.
	 */
	public boolean isSkillRestrictedByRace(String skillId) throws InvalidXmlElementException {
		final Race race = this.getRace();
		return race != null && race.getRestrictedSkillIds().contains(skillId);
	}

	/**
	 * Whether the selected race treats {@code skillId} as common, or {@code false}
	 * if no race is selected.
	 */
	public boolean isSkillCommonByRace(String skillId) throws InvalidXmlElementException {
		final Race race = this.getRace();
		return race != null && race.getCommonSkillIds().contains(skillId);
	}

	/**
	 * Whether the selected race restricts {@code categoryId}, or {@code false} if
	 * no race is selected.
	 */
	public boolean isCategoryRestrictedByRace(String categoryId) throws InvalidXmlElementException {
		final Race race = this.getRace();
		return race != null && race.getRestrictedCategoryIds().contains(categoryId);
	}

	/**
	 * Whether the selected race treats {@code categoryId} as common, or
	 * {@code false} if no race is selected.
	 */
	public boolean isCategoryCommonByRace(String categoryId) throws InvalidXmlElementException {
		final Race race = this.getRace();
		return race != null && race.getCommonCategoryIds().contains(categoryId);
	}

	/**
	 * The characteristic's total bonus: its temporal bonus plus the race's fixed
	 * bonus plus every selected perk's flat bonus to it (see
	 * {@link #getPerkCharacteristicBonus(CharacteristicAbbreviation)}).
	 *
	 * <p>
	 * Background/special bonuses are future work.
	 * {@link CharacteristicAbbreviation#REALM_OF_MAGIC} (used by spell
	 * categories) is not resolved here yet either: it requires knowing the caster's
	 * current realm of magic, which is future (magic) work; it returns 0 for now.
	 * </p>
	 */
	public Integer getCharacteristicTotalBonus(CharacteristicAbbreviation abbreviation)
			throws InvalidXmlElementException {
		if (abbreviation == CharacteristicAbbreviation.NONE
				|| abbreviation == CharacteristicAbbreviation.REALM_OF_MAGIC) {
			return 0;
		}
		return this.getCharacteristicTemporalBonus(abbreviation) + this.getCharacteristicRaceBonus(abbreviation)
				+ this.getPerkCharacteristicBonus(abbreviation);
	}

	public Appearance getAppearance() {
		return this.appearance;
	}

	public void setAppearance(Appearance appearance) {
		this.appearance = appearance;
	}

	/**
	 * The Appearance characteristic's final value, combining the {@link Appearance}
	 * roll with Presence, the selected race's flat bonus (see
	 * {@link #getAppearanceRaceBonus()}) and every selected perk's flat bonus to
	 * appearance (see {@link #getPerkAppearanceBonus()}).
	 */
	public int getAppearanceTotal() throws InvalidXmlElementException {
		return this.appearance.getTotal(this.getCharacteristicPotentialValue(CharacteristicAbbreviation.PRESENCE))
				+ this.getAppearanceRaceBonus() + this.getPerkAppearanceBonus();
	}

	/** The selected race's flat appearance bonus, or 0 if no race is selected or it grants none. */
	public int getAppearanceRaceBonus() throws InvalidXmlElementException {
		final Race race = this.getRace();
		return race == null || race.getAppearanceBonus() == null ? 0 : race.getAppearanceBonus();
	}

	public int getCurrentAge() {
		return this.currentAge;
	}

	public void setCurrentAge(int currentAge) {
		this.currentAge = currentAge;
	}

	public int getFinalAge() {
		return this.finalAge;
	}

	public void setFinalAge(int finalAge) {
		this.finalAge = finalAge;
	}

	/**
	 * Advances the character from its current age to {@link #getFinalAge()} one year at a time,
	 * rolling {@link AgeRules#hasCharacteristicDecrease} every year and, whenever it triggers,
	 * recording the resulting {@link AgeModification} on {@link #getCurrentLevel()} and applying it
	 * immediately: the rolled characteristic's temporal value decreases by the full roll, its
	 * potential value by a third of it (rounded down), and the temporal value is then clamped to
	 * never exceed the (now lower) potential value - matching the legacy
	 * {@code AgeRules.increaseAge(CharacterPlayer)}/{@code getCharacteristicTemporalValue}'s "Age
	 * modifications" clamp exactly, except applied once here instead of recomputed on every read.
	 *
	 * <p>Requires a race to be selected, for {@link Race#getExpectedLifeYears()}/{@link
	 * Race#getRaceType()}.</p>
	 */
	public void increaseAge() throws InvalidXmlElementException {
		final Race race = this.getRace();
		while (this.currentAge < this.finalAge) {
			if (AgeRules.hasCharacteristicDecrease(this.currentAge, race.getExpectedLifeYears(),
					this.getCharacteristicTotalBonus(CharacteristicAbbreviation.CONSTITUTION),
					this.getCharacteristicTotalBonus(CharacteristicAbbreviation.SELF_DISCIPLINE))) {
				final AgeModification ageModification = new AgeModification(this.currentAge, race.getRaceType());
				this.getCurrentLevel().addAgeModification(ageModification);

				final CharacteristicAbbreviation abbreviation = ageModification.getCharacteristicAbbreviation();
				final int modification = ageModification.getCharacteristicModification();
				final int newPotentialValue = this.getCharacteristicPotentialValue(abbreviation) - modification / 3;
				this.setCharacteristicPotentialValue(abbreviation, newPotentialValue);
				final int newTemporalValue = this.getCharacteristicTemporalValue(abbreviation) - modification;
				this.setCharacteristicTemporalValue(abbreviation, Math.min(newTemporalValue, newPotentialValue));
			}
			this.currentAge++;
		}
	}

	public List<LevelUp> getLevels() {
		return this.levels;
	}

	/**
	 * The character's current level (1-based), i.e. how many levels have been added
	 * so far.
	 */
	public int getLevel() {
		return this.levels.size();
	}

	public LevelUp getCurrentLevel() {
		return this.levels.get(this.levels.size() - 1);
	}

	/** Adds a new, empty level and returns it, making it the current level. */
	public LevelUp increaseLevel() {
		final LevelUp levelUp = new LevelUp();
		this.levels.add(levelUp);
		return levelUp;
	}

	/**
	 * Total ranks bought directly in a category (as opposed to in one of its
	 * skills), across every level so far.
	 *
	 * <p>
	 * This only sums {@link LevelUp#getCategoryRanks(String)}; ranks granted by
	 * culture/training selections are future work (they require a
	 * decision-resolution layer that does not exist yet, since a grant may offer a
	 * choice of categories/skills).
	 * </p>
	 */
	public Integer getCategoryTotalRanks(String categoryId) {
		int total = 0;
		for (final LevelUp levelUp : this.levels) {
			total += levelUp.getCategoryRanks(categoryId);
		}
		return total;
	}

	/**
	 * Total ranks bought in a skill, across every level so far.
	 *
	 * <p>
	 * Same limitation as {@link #getCategoryTotalRanks(String)}: only sums
	 * {@link LevelUp#getSkillRanks(String)}.
	 * </p>
	 */
	public Integer getSkillTotalRanks(String skillId) {
		int total = 0;
		for (final LevelUp levelUp : this.levels) {
			total += levelUp.getSkillRanks(skillId);
		}
		return total;
	}

	/**
	 * The bonus a category grants from ranks bought directly in it (only non-zero
	 * for {@link com.softwaremagico.librodeesher.category.CategoryType#STANDARD}
	 * categories) plus its flat bonus (only non-zero for
	 * {@link com.softwaremagico.librodeesher.category.CategoryType#PD}) plus the
	 * flat bonus the selected profession grants this category, if any, plus the
	 * background points spent on it (see {@link Background#getCategoryBonus(String)}),
	 * plus every selected perk's flat and {@link PerkBonusKind#CONDITIONAL} bonus to
	 * it (see {@link #getPerkCategoryBonus(String)}/{@link
	 * #getPerkCategoryConditionalBonus(String)} - matching the legacy {@code
	 * getSimpleBonus(Category)} exactly, a conditional bonus is included in the
	 * total unconditionally: consuming code/the player is expected to know when
	 * the condition described in the perk's free-text description applies)
	 * plus its per-rank perk bonus (see {@link #getPerkCategoryRankBonus(String)})
	 * times the ranks bought in it.
	 *
	 * <p>
	 * Race bonuses are future work (no shipped race actually grants one to a real
	 * category, see {@code RaceMigrationTool#parseSpecials}, so there is nothing to
	 * wire yet in practice).
	 * </p>
	 */
	public Integer getCategoryDevelopmentBonus(Category category) throws InvalidXmlElementException {
		final int ranks = this.getCategoryTotalRanks(category.getId());
		return category.getCategoryRankBonus(ranks) + category.getFixedBonus()
				+ this.getProfessionBonus(category.getId()) + this.background.getCategoryBonus(category.getId())
				+ this.getPerkCategoryBonus(category.getId()) + this.getPerkCategoryConditionalBonus(category.getId())
				+ this.getPerkCategoryRankBonus(category.getId()) * ranks;
	}

	/**
	 * A skill's bonus from its own ranks, using its category's progression table,
	 * plus the flat bonus the selected profession grants this skill, if any, plus
	 * the background points spent on it (see {@link Background#getSkillBonus(String)}),
	 * plus every selected perk's flat and {@link PerkBonusKind#CONDITIONAL} bonus to
	 * it (see {@link #getPerkSkillBonus(String)}/{@link
	 * #getPerkSkillConditionalBonus(String)}, included unconditionally, same as
	 * {@link #getCategoryDevelopmentBonus}) plus its per-rank perk bonus (see
	 * {@link #getPerkSkillRankBonus(String)}) times its "real ranks" (see
	 * {@link #getSkillRealRanks(Skill)}), matching the legacy
	 * {@code getSimpleBonus(Skill)} exactly.
	 *
	 * <p>
	 * The race bonus and the characteristic bonus are future work (no shipped
	 * race actually grants a bonus to a real skill either, same as categories).
	 * </p>
	 */
	public Integer getSkillDevelopmentBonus(Category category, String skillId) throws InvalidXmlElementException {
		final Integer perkSkillRankBonus = this.getPerkSkillRankBonus(skillId);
		final int perkRankTerm = perkSkillRankBonus == 0
				? 0
				: perkSkillRankBonus * this.getSkillRealRanks(RulesCatalog.getInstance().getSkill(skillId));
		return category.getSkillRankBonus(this.getSkillTotalRanks(skillId)) + this.getProfessionBonus(skillId)
				+ this.background.getSkillBonus(skillId) + this.getPerkSkillBonus(skillId)
				+ this.getPerkSkillConditionalBonus(skillId) + perkRankTerm;
	}

	/**
	 * The flat bonus the selected profession grants a category or skill named
	 * {@code id}, or 0 if no profession is selected, or it grants that id none.
	 */
	public Integer getProfessionBonus(String id) throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		return profession == null ? 0 : profession.getBonus(id);
	}

	/**
	 * Whether {@code abbreviation} is the selected profession's primary or
	 * secondary preferred characteristic, or {@code false} if no profession is
	 * selected.
	 */
	public boolean isPreferredCharacteristic(CharacteristicAbbreviation abbreviation)
			throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		return profession != null && profession.isPreferredCharacteristic(abbreviation);
	}

	public Background getBackground() {
		return this.background;
	}

	/**
	 * Every choice the player has made so far for a category/skill/characteristic
	 * grant; see {@link Decisions}.
	 */
	public Decisions getDecisions() {
		return this.decisions;
	}

	/**
	 * Applies one of a training's (or culture's adolescence) category grants to the
	 * current level: resolves which category it applies to (reusing an already-made
	 * decision if {@code key} was decided before, otherwise validating
	 * {@code selectedCategoryId} and recording it), adds the grant's ranks to that
	 * category, then does the same for each of its nested skill grants (keyed as
	 * {@code key + ":skill:" + <index>}).
	 *
	 * <p>
	 * {@link #ALL_WEAPON_CATEGORIES}/{@link #ALL_ATTACK_CATEGORIES} wildcard
	 * markers in {@code
	 * grant}'s category options (see
	 * {@code TrainingMigrationTool.resolveCategoryIds}) are expanded into every
	 * real matching category id before resolving the decision, so {@code
	 * selectedCategoryId} must be one of the real ids, not the wildcard marker
	 * itself.
	 * </p>
	 *
	 * <p>
	 * Ranks are added to the <em>current</em> level; call this once per level the
	 * grant actually applies at (typically once, when the training/culture is first
	 * taken).
	 * </p>
	 *
	 * @param key
	 *            a caller-chosen id identifying this specific grant uniquely for
	 *            this character (e.g. {@code "training:soldier:category:1"}),
	 *            reused as-is on subsequent calls once a choice has been made.
	 * @param grant
	 *            the category grant to apply.
	 * @param selectedCategoryId
	 *            the category to use if {@code grant} offers a choice and
	 *            {@code key} has not been decided yet; ignored otherwise (including
	 *            when {@code
	 *                           grant} is not a choice).
	 * @param selectedSkillIds
	 *            the skill to use for each of {@code grant}'s nested skill grants
	 *            that offers a choice and has not been decided yet, in the same
	 *            order as {@link TrainingCategoryGrant#getSkills()}; ignored for
	 *            grants that are not a choice or are already decided.
	 */
	public void applyCategoryGrant(String key, TrainingCategoryGrant grant, String selectedCategoryId,
			List<String> selectedSkillIds) throws InvalidXmlElementException {
		final List<String> offeredCategories = this.expandCategoryWildcards(grant.getCategoryOptions());
		final Decision categoryDecision = this.decideOrReuse(key,
				() -> offeredCategories.size() > 1
						? Decision.select(offeredCategories, selectedCategoryId)
						: Decision.fixed(offeredCategories));
		this.getCurrentLevel().addCategoryRanks(categoryDecision.getSelectedOption(), grant.getRanksGranted());

		final List<TrainingSkillGrant> skills = grant.getSkills();
		for (int i = 0; i < skills.size(); i++) {
			final TrainingSkillGrant skillGrant = skills.get(i);
			final String skillKey = key + ":skill:" + i;
			final String selectedSkillId = selectedSkillIds != null && i < selectedSkillIds.size()
					? selectedSkillIds.get(i)
					: null;
			final Decision skillDecision = this.decideOrReuse(skillKey, () -> skillGrant.resolve(selectedSkillId));
			// Whether this is a spell skill is not resolved here (it requires
			// cross-referencing the
			// skill's category), see LevelUp#setSkillRanks; future work.
			this.getCurrentLevel().addSkillRanks(skillDecision.getSelectedOption(), skillGrant.getRanksToDistribute(),
					false);
		}
	}

	/**
	 * Applies every category grant of {@code training} to the current level, keying
	 * each one's decision as
	 * {@code "training:" + training.getId() + ":category:" + <index>} (and its
	 * nested skill grants as {@code ":skill:" + <index>} under that), reusing
	 * {@link #applyCategoryGrant}.
	 *
	 * @param categorySelections
	 *            the category to use for each grant (by its index in
	 *            {@link Training#getCategories()}) that offers a choice and has not
	 *            been decided yet; entries for grants that are fixed or already
	 *            decided are ignored. May be {@code null} if no grant needs a fresh
	 *            selection.
	 * @param skillSelections
	 *            same, but for each grant's nested skill choices, keyed the same
	 *            way.
	 */
	public void applyTrainingCategories(Training training, Map<Integer, String> categorySelections,
			Map<Integer, List<String>> skillSelections) throws InvalidXmlElementException {
		this.applyCategoryGrants("training:" + training.getId() + ":category", training.getCategories(),
				categorySelections, skillSelections);
	}

	/**
	 * Applies every adolescence-rank category grant of {@code culture} to the
	 * current level, keying each one's decision as
	 * {@code "culture:" + culture.getId() + ":adolescence:" + <index>}; see
	 * {@link #applyTrainingCategories} for the selection map semantics.
	 */
	public void applyCultureAdolescenceRanks(Culture culture, Map<Integer, String> categorySelections,
			Map<Integer, List<String>> skillSelections) throws InvalidXmlElementException {
		this.applyCategoryGrants("culture:" + culture.getId() + ":adolescence", culture.getAdolescenceRanks(),
				categorySelections, skillSelections);
	}

	private void applyCategoryGrants(String keyPrefix, List<TrainingCategoryGrant> grants,
			Map<Integer, String> categorySelections, Map<Integer, List<String>> skillSelections)
			throws InvalidXmlElementException {
		for (int i = 0; i < grants.size(); i++) {
			final String selectedCategoryId = categorySelections == null ? null : categorySelections.get(i);
			final List<String> selectedSkillIds = skillSelections == null ? null : skillSelections.get(i);
			this.applyCategoryGrant(keyPrefix + ":" + i, grants.get(i), selectedCategoryId, selectedSkillIds);
		}
	}

	/**
	 * Resolves every choice in a training's "HABILIDADES DE ESTILO DE
	 * VIDA"/"COMUNES"/ "PROFESIONALES"/"RESTRINGIDAS" sections, recording which
	 * skill each one grants (as opposed to how many ranks: these sections make a
	 * skill available at a favourable cost tier rather than granting ranks
	 * directly, so there is nothing to add to {@link LevelUp} here).
	 *
	 * @param lifeSkillSelections
	 *            the skill id to use for each of {@link Training#getLifeSkills()}
	 *            that offers a choice and has not been decided yet, by index.
	 * @param commonSkillSelections
	 *            same, for {@link Training#getCommonSkills()}.
	 * @param professionalSkillSelections
	 *            same, for {@link Training#getProfessionalSkills()}.
	 * @param restrictedSkillSelections
	 *            same, for {@link Training#getRestrictedSkills()}.
	 */
	public void applyTrainingSkillChoices(Training training, Map<Integer, String> lifeSkillSelections,
			Map<Integer, String> commonSkillSelections, Map<Integer, String> professionalSkillSelections,
			Map<Integer, String> restrictedSkillSelections) {
		final String prefix = "training:" + training.getId();
		this.applyChoiceGroups(prefix + ":lifeSkill", training.getLifeSkills(), lifeSkillSelections);
		this.applyChoiceGroups(prefix + ":commonSkill", training.getCommonSkills(), commonSkillSelections);
		this.applyChoiceGroups(prefix + ":professionalSkill", training.getProfessionalSkills(),
				professionalSkillSelections);
		this.applyChoiceGroups(prefix + ":restrictedSkill", training.getRestrictedSkills(), restrictedSkillSelections);
	}

	/**
	 * The {@code Skill} id granted by each decided choice in
	 * {@link Training#getLifeSkills()}.
	 */
	public List<String> getTrainingLifeSkills(Training training) {
		return this.getDecidedOptions("training:" + training.getId() + ":lifeSkill", training.getLifeSkills().size());
	}

	/**
	 * Same as {@link #getTrainingLifeSkills}, for
	 * {@link Training#getCommonSkills()}.
	 */
	public List<String> getTrainingCommonSkills(Training training) {
		return this.getDecidedOptions("training:" + training.getId() + ":commonSkill",
				training.getCommonSkills().size());
	}

	/**
	 * Same as {@link #getTrainingLifeSkills}, for
	 * {@link Training#getProfessionalSkills()}.
	 */
	public List<String> getTrainingProfessionalSkills(Training training) {
		return this.getDecidedOptions("training:" + training.getId() + ":professionalSkill",
				training.getProfessionalSkills().size());
	}

	/**
	 * Same as {@link #getTrainingLifeSkills}, for
	 * {@link Training#getRestrictedSkills()}.
	 */
	public List<String> getTrainingRestrictedSkills(Training training) {
		return this.getDecidedOptions("training:" + training.getId() + ":restrictedSkill",
				training.getRestrictedSkills().size());
	}

	private void applyChoiceGroups(String keyPrefix, List<ChoiceGroup> groups, Map<Integer, String> selections) {
		for (int i = 0; i < groups.size(); i++) {
			final ChoiceGroup group = groups.get(i);
			final String selectedOption = selections == null ? null : selections.get(i);
			this.decideOrReuse(keyPrefix + ":" + i, () -> group.resolve(selectedOption));
		}
	}

	/**
	 * The selected option of every decision {@code keyPrefix + ":0"} through
	 * {@code keyPrefix + ":" + (count - 1)}.
	 */
	private List<String> getDecidedOptions(String keyPrefix, int count) {
		final List<String> selected = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			final String option = this.decisions.getSelectedOption(keyPrefix + ":" + i);
			if (option != null) {
				selected.add(option);
			}
		}
		return selected;
	}

	/**
	 * Applies one of a training's "AUMENTOS CARACTERÍSTICAS" choices: resolves
	 * which characteristic it applies to (reusing an already-made decision if
	 * {@code key} was decided before), rolls 2d10 and increases that
	 * characteristic's temporal value by
	 * {@link Characteristic#getCharacteristicUpgrade}, recording the roll in the
	 * current level.
	 *
	 * @param key
	 *            a caller-chosen id identifying this specific choice uniquely for
	 *            this character (e.g. {@code "training:scout:characteristic:0"}).
	 * @param group
	 *            the characteristic choice to apply.
	 * @param selectedCharacteristic
	 *            the characteristic to use if {@code group} offers a choice and
	 *            {@code key} has not been decided yet; ignored otherwise.
	 * @return the recorded roll.
	 */
	public CharacteristicRoll applyCharacteristicUpgrade(String key, ChoiceGroup group,
			CharacteristicAbbreviation selectedCharacteristic) {
		final String selectedOption = selectedCharacteristic == null ? null : selectedCharacteristic.name();
		final Decision decision = this.decideOrReuse(key, () -> group.resolve(selectedOption));
		final CharacteristicAbbreviation abbreviation = CharacteristicAbbreviation
				.valueOf(decision.getSelectedOption());

		final Integer temporalValue = this.getCharacteristicTemporalValue(abbreviation);
		final Integer potentialValue = this.getCharacteristicPotentialValue(abbreviation);
		final Roll roll = new Roll();
		final Integer upgrade = Characteristic.getCharacteristicUpgrade(temporalValue, potentialValue, roll);
		this.setCharacteristicTemporalValue(abbreviation, temporalValue + upgrade);

		return this.getCurrentLevel().addCharacteristicUpdate(abbreviation, temporalValue, potentialValue, roll);
	}

	/**
	 * Returns the existing decision for {@code key}, or resolves it via
	 * {@code resolver} and records it.
	 */
	private Decision decideOrReuse(String key, Supplier<Decision> resolver) {
		if (this.decisions.isDecided(key)) {
			return this.decisions.get(key);
		}
		final Decision decision = resolver.get();
		this.decisions.set(key, decision);
		return decision;
	}

	/**
	 * Pseudo-category marker (see {@code TrainingMigrationTool.resolveCategoryIds})
	 * standing for "any weapon category", expanded by
	 * {@link #expandCategoryWildcards(List)} into every category id starting with
	 * "weapons" (matching {@code CategoryMigrationTool}'s id convention for every
	 * "Armas·&lt;Tipo&gt;" category).
	 */
	public static final String ALL_WEAPON_CATEGORIES = "allWeaponCategories";

	/**
	 * Pseudo-category marker standing for "any non-weapon attack category" (martial
	 * arts strikes/ sweeps/combat maneuvers, special attacks), matching the exact
	 * set the legacy application hardcoded in
	 * {@code CategoryFactory.getOthersAttack()}.
	 */
	public static final String ALL_ATTACK_CATEGORIES = "allAttackCategories";

	private static final List<String> ATTACK_CATEGORY_IDS = List.of("martialArtsStrikes", "martialArtsSweeps",
			"martialArtsCombatManeuvers", "specialAttacks");

	private static final String OPTIONAL_RACE_LANGUAGE_PREFIX = "language:optionalRace:";
	private static final String OPTIONAL_BACKGROUND_LANGUAGE_PREFIX = "language:optionalBackground:";
	private static final String OPTIONAL_CULTURE_LANGUAGE_PREFIX = "language:optionalCulture:";

	/**
	 * Expands any {@link #ALL_WEAPON_CATEGORIES}/{@link #ALL_ATTACK_CATEGORIES}
	 * marker in {@code
	 * categoryIds} into the real category ids it stands for (every other id is kept
	 * as-is). The actual set depends on which modules are currently enabled
	 * (through {@link RulesCatalog}), so this cannot be resolved once and for all
	 * at migration time.
	 */
	public List<String> expandCategoryWildcards(List<String> categoryIds) throws InvalidXmlElementException {
		final List<String> expanded = new ArrayList<>();
		for (final String categoryId : categoryIds) {
			if (ALL_WEAPON_CATEGORIES.equals(categoryId)) {
				expanded.addAll(this.getWeaponCategoryIds());
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
	 * Whether {@code skillId} was granted as a "generalized" skill (marked directly
	 * on a level, not derived from anything else) in any level so far.
	 */
	public boolean isSkillGeneralized(String skillId) {
		for (final LevelUp levelUp : this.levels) {
			if (levelUp.getGeneralizedSkills().contains(skillId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether any training taken so far (see {@link LevelUp#getTrainings()})
	 * granted {@code
	 * skillId} through its {@code trainingSkillsGetter} section
	 * (common/professional/restricted).
	 */
	private boolean isSkillGrantedByAnyTraining(String skillId, Function<Training, List<String>> trainingSkillsGetter)
			throws InvalidXmlElementException {
		for (final LevelUp levelUp : this.levels) {
			for (final String trainingId : levelUp.getTrainings()) {
				final Training training = RulesCatalog.getInstance().getTraining(trainingId);
				if (trainingSkillsGetter.apply(training).contains(skillId)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Whether {@code skill} is restricted: either by its own
	 * {@link SkillType#RESTRICTED} tag, because a training taken so far grants it
	 * as one of its restricted skills, because the selected race restricts it
	 * (matched by {@code skill.getId()} against
	 * {@link Race#getRestrictedSkillIds()}), because the selected profession
	 * does ({@link Profession#isRestrictedSkill(String)}), or because a selected
	 * perk does (see {@link #isSkillRestrictedByPerk(String)}).
	 */
	public boolean isSkillRestricted(Skill skill) throws InvalidXmlElementException {
		final Profession profession = getProfession();
		return skill.getSkillType() == SkillType.RESTRICTED
				|| this.isSkillGrantedByAnyTraining(skill.getId(), this::getTrainingRestrictedSkills)
				|| this.isSkillRestrictedByRace(skill.getId())
				|| (profession != null && profession.isRestrictedSkill(skill.getId()))
				|| this.isSkillRestrictedByPerk(skill.getId());
	}

	/**
	 * Same limitation as {@link #isSkillRestricted(Skill)}, for
	 * {@link SkillType#COMMON}.
	 */
	public boolean isSkillCommon(Skill skill) throws InvalidXmlElementException {
		final Profession profession = getProfession();
		return skill.getSkillType() == SkillType.COMMON
				|| this.isSkillGrantedByAnyTraining(skill.getId(), this::getTrainingCommonSkills)
				|| this.isSkillCommonByRace(skill.getId())
				|| (profession != null && profession.isCommonSkill(skill.getId()))
				|| this.isSkillCommonByPerk(skill.getId());
	}

	/**
	 * Same limitation as {@link #isSkillRestricted(Skill)}, for
	 * {@link SkillType#PROFESSIONAL}.
	 */
	public boolean isSkillProfessional(Skill skill) throws InvalidXmlElementException {
		final Profession profession = getProfession();
		return skill.getSkillType() == SkillType.PROFESSIONAL
				|| this.isSkillGrantedByAnyTraining(skill.getId(), this::getTrainingProfessionalSkills)
				|| (profession != null && profession.isProfessionalSkill(skill.getId()));
	}

	/**
	 * The multiplier applied to a skill's bought ranks to get its "real ranks"
	 * (used for skill bonus purposes, as opposed to category-bonus purposes):
	 * restricted skills count for half, professional skills for triple, common
	 * skills for double, and a generalized skill counts fully only if it is also
	 * common or professional (half otherwise); standard skills count for their full
	 * value.
	 */
	public double getSkillRankMultiplier(Skill skill) throws InvalidXmlElementException {
		if (this.isSkillRestricted(skill)) {
			return 0.5;
		}
		final boolean common = this.isSkillCommon(skill);
		final boolean professional = this.isSkillProfessional(skill);
		if (this.isSkillGeneralized(skill.getId())) {
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
	 * A skill's "real ranks": its bought ranks (see
	 * {@link #getSkillTotalRanks(String)}) times {@link #getSkillRankMultiplier}.
	 */
	public int getSkillRealRanks(Skill skill) throws InvalidXmlElementException {
		return (int) (this.getSkillTotalRanks(skill.getId()) * this.getSkillRankMultiplier(skill));
	}

	public List<SelectedPerk> getSelectedPerks() {
		return this.selectedPerks;
	}

	public boolean isPerkSelected(String perkId) {
		return this.findSelectedPerk(perkId) != null;
	}

	/** Selects {@code perkId}, if it was not already selected. */
	public void addPerk(String perkId) {
		if (!this.isPerkSelected(perkId)) {
			this.selectedPerks.add(new SelectedPerk(perkId));
		}
	}

	/** Unselects {@code perkId} and forgets any weakness paired with it. */
	public void removePerk(String perkId) {
		this.selectedPerks.removeIf(selectedPerk -> selectedPerk.getPerkId().equals(perkId));
	}

	/**
	 * Pairs an already-selected perk with a weakness, discounting the perk's
	 * background points cost.
	 */
	public void setWeakness(String perkId, String weaknessPerkId) {
		final SelectedPerk selectedPerk = this.findSelectedPerk(perkId);
		if (selectedPerk != null) {
			selectedPerk.setWeaknessId(weaknessPerkId);
		}
	}

	public boolean hasWeakness(String perkId) {
		final SelectedPerk selectedPerk = this.findSelectedPerk(perkId);
		return selectedPerk != null && selectedPerk.getWeaknessId() != null;
	}

	/**
	 * Marks an already-selected perk as chosen by random character generation (not
	 * player-removable).
	 */
	public void setPerkAsRandom(String perkId, boolean random) {
		final SelectedPerk selectedPerk = this.findSelectedPerk(perkId);
		if (selectedPerk != null) {
			selectedPerk.setRandom(random);
		}
	}

	public boolean isPerkRandom(String perkId) {
		final SelectedPerk selectedPerk = this.findSelectedPerk(perkId);
		return selectedPerk != null && selectedPerk.isRandom();
	}

	private SelectedPerk findSelectedPerk(String perkId) {
		for (final SelectedPerk selectedPerk : this.selectedPerks) {
			if (selectedPerk.getPerkId().equals(perkId)) {
				return selectedPerk;
			}
		}
		return null;
	}

	/**
	 * Total background points spent on every selected perk so far, discounted by
	 * any weakness paired with each one and by whether it was chosen at random (see
	 * {@link PerkGrade#getBackgroundCost}). A perk that is itself a weakness
	 * ({@link Perk#isWeakness()}) costs nothing here, matching the legacy rule (its
	 * points are refunded some other way, not modeled yet).
	 */
	public int getPerksBackgroundPointsCost() throws InvalidXmlElementException {
		int cost = 0;
		for (final SelectedPerk selectedPerk : this.selectedPerks) {
			final Perk perk = RulesCatalog.getInstance().getPerk(selectedPerk.getPerkId());
			if (perk.isWeakness()) {
				continue;
			}
			final Perk weakness = selectedPerk.getWeaknessId() == null
					? null
					: RulesCatalog.getInstance().getPerk(selectedPerk.getWeaknessId());
			cost += perk.getGrade().getBackgroundCost(weakness == null ? null : weakness.getGrade(),
					selectedPerk.isRandom());
		}
		return cost;
	}

	/**
	 * Background points left to spend: the race's total
	 * ({@link Race#getBackgroundPoints()}, 0 without a race selected) minus what
	 * {@link Background} and the selected perks have spent so far.
	 */
	public int getRemainingBackgroundPoints() throws InvalidXmlElementException {
		final Race race = this.getRace();
		final int totalBackgroundPoints = race == null || race.getBackgroundPoints() == null
				? 0
				: race.getBackgroundPoints();
		return totalBackgroundPoints - this.background.getSpentBackgroundPoints() - this.getPerksBackgroundPointsCost();
	}

	/**
	 * Resolves one of the selected profession's realm-of-magic grants: for a fixed
	 * grant, {@code
	 * selectedRealm} is ignored and its single realm is used; for a hybrid choice
	 * (e.g. "Esencia/Canalización"), {@code selectedRealm} must be one of
	 * {@link RealmOfMagicGrant#getOptions()}.
	 *
	 * @param key
	 *            a caller-chosen id identifying this specific grant uniquely for
	 *            this character (e.g. {@code "profession:sorcerer:realm:0"}),
	 *            reused as-is on subsequent calls once a choice has been made.
	 * @param grant
	 *            the realm grant to resolve.
	 * @param selectedRealm
	 *            the realm to use if {@code grant} offers a choice and {@code key}
	 *            has not been decided yet; ignored otherwise.
	 */
	public RealmOfMagic applyMagicRealmChoice(String key, RealmOfMagicGrant grant, RealmOfMagic selectedRealm) {
		final List<String> offeredRealms = new ArrayList<>();
		for (final RealmOfMagic realm : grant.getOptions()) {
			offeredRealms.add(realm.name());
		}
		final String selectedOption = selectedRealm == null ? null : selectedRealm.name();
		final Decision decision = this.decideOrReuse(key,
				() -> grant.isChoice()
						? Decision.select(offeredRealms, selectedOption)
						: Decision.fixed(offeredRealms));
		return RealmOfMagic.valueOf(decision.getSelectedOption());
	}

	/**
	 * Resolves every one of the selected profession's realm-of-magic grants (see
	 * {@link #applyMagicRealmChoice}), keying each one's decision as
	 * {@code "profession:" +
	 * profession.getId() + ":realm:" + <index>}, and returns every resolved realm:
	 * the character is a caster of all of them at once (as opposed to the
	 * alternatives within a single hybrid grant, which are mutually exclusive).
	 * Returns an empty list if no profession is selected.
	 *
	 * @param realmSelections
	 *            the realm to use for each grant (by its index in
	 *            {@link Profession#getMagicRealms()}) that offers a choice and has
	 *            not been decided yet. May be {@code null} if no grant needs a
	 *            fresh selection.
	 */
	public List<RealmOfMagic> applyProfessionMagicRealms(Map<Integer, RealmOfMagic> realmSelections)
			throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		if (profession == null) {
			return List.of();
		}
		final List<RealmOfMagic> realms = new ArrayList<>();
		final List<RealmOfMagicGrant> grants = profession.getMagicRealms();
		for (int i = 0; i < grants.size(); i++) {
			final RealmOfMagic selectedRealm = realmSelections == null ? null : realmSelections.get(i);
			realms.add(this.applyMagicRealmChoice("profession:" + profession.getId() + ":realm:" + i, grants.get(i),
					selectedRealm));
		}
		return realms;
	}

	/**
	 * The fixed speaking ranks a race grants at creation for a language (0 if no
	 * race is selected or it does not grant that language at creation), including a
	 * language assigned to one of the race's optional "Idioma Racial"/"Idioma
	 * Regional" slots (see {@link #assignOptionalRaceLanguage}).
	 */
	public int getRaceLanguageStartingSpeakingRanks(String languageId) throws InvalidXmlElementException {
		final RaceLanguage language = this.findRaceLanguage(languageId, false);
		final int named = language == null || language.getStartingSpeakingRanks() == null
				? 0
				: language.getStartingSpeakingRanks();
		return Math.max(named,
				this.getOptionalRaceLanguageRanks(languageId, LanguageSlot::getStartingSpeakingRanks, false));
	}

	/**
	 * Same as {@link #getRaceLanguageStartingSpeakingRanks(String)}, for writing
	 * ranks.
	 */
	public int getRaceLanguageStartingWritingRanks(String languageId) throws InvalidXmlElementException {
		final RaceLanguage language = this.findRaceLanguage(languageId, false);
		final int named = language == null || language.getStartingWritingRanks() == null
				? 0
				: language.getStartingWritingRanks();
		return Math.max(named,
				this.getOptionalRaceLanguageRanks(languageId, LanguageSlot::getStartingWritingRanks, false));
	}

	/**
	 * The maximum speaking ranks a language can reach for the selected race,
	 * checking both the languages granted at creation and those only available
	 * through background points, plus a language assigned to any optional slot; the
	 * legacy default of 10 applies only if the race mentions the language nowhere
	 * at all (0 without a race selected, since there is no race to ask).
	 */
	public int getRaceLanguageMaxSpeakingRanks(String languageId) throws InvalidXmlElementException {
		if (this.getRace() == null) {
			return 0;
		}
		final RaceLanguage language = this.findRaceLanguage(languageId, true);
		if (language != null && language.getMaxSpeakingRanks() != null) {
			return language.getMaxSpeakingRanks();
		}
		final int fromSlot = this.getOptionalRaceLanguageRanks(languageId, LanguageSlot::getMaxSpeakingRanks, true);
		return fromSlot > 0 ? fromSlot : 10;
	}

	/**
	 * Same as {@link #getRaceLanguageMaxSpeakingRanks(String)}, for writing ranks.
	 */
	public int getRaceLanguageMaxWritingRanks(String languageId) throws InvalidXmlElementException {
		if (this.getRace() == null) {
			return 0;
		}
		final RaceLanguage language = this.findRaceLanguage(languageId, true);
		if (language != null && language.getMaxWritingRanks() != null) {
			return language.getMaxWritingRanks();
		}
		final int fromSlot = this.getOptionalRaceLanguageRanks(languageId, LanguageSlot::getMaxWritingRanks, true);
		return fromSlot > 0 ? fromSlot : 10;
	}

	private RaceLanguage findRaceLanguage(String languageId, boolean includeBackgroundLanguages)
			throws InvalidXmlElementException {
		final Race race = this.getRace();
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
	 * Assigns {@code languageId} to one of the race's optional "IDIOMAS" slots (by
	 * its index in {@link Race#getOptionalRaceLanguages()}), recording the choice
	 * so
	 * {@link #getRaceLanguageStartingSpeakingRanks}/{@link #getRaceLanguageMaxSpeakingRanks}
	 * (and their writing-rank counterparts) pick up that slot's ranks for
	 * {@code languageId}.
	 */
	public void assignOptionalRaceLanguage(int slotIndex, String languageId) {
		this.decisions.set(OPTIONAL_RACE_LANGUAGE_PREFIX + slotIndex, Decision.fixed(List.of(languageId)));
	}

	public String getOptionalRaceLanguageAssignment(int slotIndex) {
		return this.decisions.getSelectedOption(OPTIONAL_RACE_LANGUAGE_PREFIX + slotIndex);
	}

	/**
	 * Same as {@link #assignOptionalRaceLanguage}, but for
	 * {@link Race#getOptionalBackgroundLanguages()}.
	 */
	public void assignOptionalBackgroundLanguage(int slotIndex, String languageId) {
		this.decisions.set(OPTIONAL_BACKGROUND_LANGUAGE_PREFIX + slotIndex, Decision.fixed(List.of(languageId)));
	}

	public String getOptionalBackgroundLanguageAssignment(int slotIndex) {
		return this.decisions.getSelectedOption(OPTIONAL_BACKGROUND_LANGUAGE_PREFIX + slotIndex);
	}

	private int getOptionalRaceLanguageRanks(String languageId, Function<LanguageSlot, Integer> rankGetter,
			boolean includeBackgroundLanguages) throws InvalidXmlElementException {
		final Race race = this.getRace();
		if (race == null) {
			return 0;
		}
		int best = this.matchingSlotRank(race.getOptionalRaceLanguages(), OPTIONAL_RACE_LANGUAGE_PREFIX, languageId,
				rankGetter);
		if (includeBackgroundLanguages) {
			best = Math.max(best, this.matchingSlotRank(race.getOptionalBackgroundLanguages(),
					OPTIONAL_BACKGROUND_LANGUAGE_PREFIX, languageId, rankGetter));
		}
		return best;
	}

	private int matchingSlotRank(List<LanguageSlot> slots, String keyPrefix, String languageId,
			Function<LanguageSlot, Integer> rankGetter) {
		int best = 0;
		for (int i = 0; i < slots.size(); i++) {
			if (languageId.equals(this.decisions.getSelectedOption(keyPrefix + i))) {
				final Integer rank = rankGetter.apply(slots.get(i));
				best = Math.max(best, rank == null ? 0 : rank);
			}
		}
		return best;
	}

	/**
	 * The maximum speaking ranks a language can reach for the selected culture,
	 * resolving the {@code
	 * "all"} marker (a culture that caps every language the same way) as a fallback
	 * when the language is not mentioned by name, plus a language assigned to one
	 * of the culture's optional "Idioma Regional" slots (see
	 * {@link #assignOptionalCultureLanguage}); 0 if no culture is selected or it
	 * does not mention that language anywhere.
	 */
	public int getCultureLanguageMaxSpeakingRanks(String languageId) throws InvalidXmlElementException {
		final CultureLanguageRank rank = this.findCultureLanguageRank(languageId);
		if (rank != null && rank.getMaxSpeakingRanks() != null) {
			return rank.getMaxSpeakingRanks();
		}
		return this.getOptionalCultureLanguageMaxRanks(languageId, LanguageSlot::getMaxSpeakingRanks);
	}

	/**
	 * Same as {@link #getCultureLanguageMaxSpeakingRanks(String)}, for writing
	 * ranks.
	 */
	public int getCultureLanguageMaxWritingRanks(String languageId) throws InvalidXmlElementException {
		final CultureLanguageRank rank = this.findCultureLanguageRank(languageId);
		if (rank != null && rank.getMaxWritingRanks() != null) {
			return rank.getMaxWritingRanks();
		}
		return this.getOptionalCultureLanguageMaxRanks(languageId, LanguageSlot::getMaxWritingRanks);
	}

	private CultureLanguageRank findCultureLanguageRank(String languageId) throws InvalidXmlElementException {
		final Culture culture = this.getCulture();
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

	/**
	 * Assigns {@code languageId} to one of the culture's optional "IDIOMAS" slots;
	 * see {@link #assignOptionalRaceLanguage}.
	 */
	public void assignOptionalCultureLanguage(int slotIndex, String languageId) {
		this.decisions.set(OPTIONAL_CULTURE_LANGUAGE_PREFIX + slotIndex, Decision.fixed(List.of(languageId)));
	}

	public String getOptionalCultureLanguageAssignment(int slotIndex) {
		return this.decisions.getSelectedOption(OPTIONAL_CULTURE_LANGUAGE_PREFIX + slotIndex);
	}

	private int getOptionalCultureLanguageMaxRanks(String languageId, Function<LanguageSlot, Integer> rankGetter)
			throws InvalidXmlElementException {
		final Culture culture = this.getCulture();
		if (culture == null) {
			return 0;
		}
		return this.matchingSlotRank(culture.getOptionalLanguages(), OPTIONAL_CULTURE_LANGUAGE_PREFIX, languageId,
				rankGetter);
	}

	/**
	 * The highest speaking-ranks cap a language can reach, taking the best of what
	 * the selected race and culture allow (the legacy rule takes the highest value
	 * offered by any source that mentions a language, including any assigned
	 * optional-language slot). 0 if neither mentions it.
	 *
	 * <p>
	 * See {@link #setBackgroundLanguageRank(String, int)} for validating background
	 * points spent on a language against this cap.
	 * </p>
	 */
	public int getLanguageMaxSpeakingRanks(String languageId) throws InvalidXmlElementException {
		return Math.max(this.getRaceLanguageMaxSpeakingRanks(languageId),
				this.getCultureLanguageMaxSpeakingRanks(languageId));
	}

	/** Same as {@link #getLanguageMaxSpeakingRanks(String)}, for writing ranks. */
	public int getLanguageMaxWritingRanks(String languageId) throws InvalidXmlElementException {
		return Math.max(this.getRaceLanguageMaxWritingRanks(languageId),
				this.getCultureLanguageMaxWritingRanks(languageId));
	}

	/**
	 * Sets how many background points' worth of ranks were spent on
	 * {@code languageId} (see {@link Background#setHistoryLanguageRank}), rejecting
	 * the change (leaving it at 0) if {@code ranks} alone would exceed
	 * {@link #getLanguageMaxSpeakingRanks(String)}, matching the legacy rule.
	 */
	public void setBackgroundLanguageRank(String languageId, int ranks) throws InvalidXmlElementException {
		this.background.setHistoryLanguageRank(languageId, ranks);
		if (this.background.getHistoryLanguageRank(languageId) > this.getLanguageMaxSpeakingRanks(languageId)) {
			this.background.setHistoryLanguageRank(languageId, 0);
		}
	}

	/**
	 * Sets how many of the selected culture's hobby points were spent on
	 * {@code skillId} (the legacy "AFICIONES" section's free ranks), matching
	 * {@code CultureDecisions#setHobbyRanks} exactly: unlike
	 * {@link #setBackgroundLanguageRank}, the legacy application never validated
	 * this against {@link Culture#getHobbyRanks()} or
	 * {@link Culture#isHobbySkillAllowed(String)} at the model level (only in its
	 * UI), so this does not either; use those two query methods to validate a
	 * choice before calling this, if desired.
	 */
	public void setHobbySkillRank(String skillId, int ranks) {
		if (ranks <= 0) {
			this.hobbySkillRanks.remove(skillId);
		} else {
			this.hobbySkillRanks.put(skillId, ranks);
		}
	}

	public int getHobbySkillRank(String skillId) {
		return this.hobbySkillRanks.getOrDefault(skillId, 0);
	}

	/**
	 * The sum of every hobby rank spent so far, to compare against the selected
	 * culture's {@link Culture#getHobbyRanks()}.
	 */
	public int getTotalHobbySkillRanks() {
		int total = 0;
		for (final int ranks : this.hobbySkillRanks.values()) {
			total += ranks;
		}
		return total;
	}

	/**
	 * The selected culture's discount/markup on {@code trainingId}'s background
	 * points cost (see {@link Culture#getTrainingPricePercentage(String)}), or
	 * {@code 1.0} (no change) if no culture is selected.
	 */
	public double getCultureTrainingPricePercentage(String trainingId) throws InvalidXmlElementException {
		final Culture culture = this.getCulture();
		return culture == null ? 1.0 : culture.getTrainingPricePercentage(trainingId);
	}

	private static final String PERK_CHOICE_KEY_PREFIX = "perk:";

	/**
	 * Resolves one of a selected perk's "choose N" grants ({@link PerkChoiceGrant},
	 * the {@code "{...}"} entries of its "bonuses" column): a grant that already
	 * names a specific category ({@link PerkChoiceGrant#getCategoryId()}) or skill
	 * ({@link PerkChoiceGrant#getSkillId()}) auto-resolves to it (there is nothing
	 * to choose); otherwise {@code selectedTargetId} must be one of {@link
	 * PerkChoiceGrant#getScope()}'s pool (every category/skill, every weapon
	 * category, or every weapon skill, per {@link PerkChoiceScope}).
	 *
	 * <p>{@link PerkChoiceGrant#getOptionsToChoose()} (how many distinct targets a
	 * grant lets the player pick) is not enforced here: every choice grant in the
	 * shipped data effectively resolves to a single target anyway (either the
	 * grant already names one, or its one N&gt;1 case still only has that same
	 * single, fixed category as its "pool"), so a single {@code selectedTargetId}
	 * per grant covers every real perk; future work if a perk ever needs more.</p>
	 *
	 * @param perkId    the selected perk this grant belongs to.
	 * @param grantIndex the grant's index within {@link Perk#getChoiceGrants()}.
	 */
	public String applyPerkChoiceGrant(String perkId, int grantIndex, PerkChoiceGrant grant, String selectedTargetId)
			throws InvalidXmlElementException {
		final String key = PERK_CHOICE_KEY_PREFIX + perkId + ":choice:" + grantIndex;
		final Decision decision = this.decideOrReuse(key, () -> {
			if (grant.getCategoryId() != null) {
				return Decision.fixed(List.of(grant.getCategoryId()));
			}
			if (grant.getSkillId() != null) {
				return Decision.fixed(List.of(grant.getSkillId()));
			}
			try {
				return Decision.select(this.getPerkChoiceScopeOptions(grant.getScope()), selectedTargetId);
			} catch (final InvalidXmlElementException e) {
				throw new IllegalStateException(e);
			}
		});
		return decision.getSelectedOption();
	}

	/** Every category/skill id {@link PerkChoiceScope#getScope()} lets the player pick from. */
	private List<String> getPerkChoiceScopeOptions(PerkChoiceScope scope) throws InvalidXmlElementException {
		final List<String> ids = new ArrayList<>();
		switch (scope) {
			case ANY_CATEGORY -> {
				for (final Category category : RulesCatalog.getInstance().getCategories()) {
					ids.add(category.getId());
				}
			}
			case ANY_WEAPON_CATEGORY -> ids.addAll(this.getWeaponCategoryIds());
			case ANY_SKILL -> {
				for (final Skill skill : RulesCatalog.getInstance().getSkills()) {
					ids.add(skill.getId());
				}
			}
			case ANY_WEAPON_SKILL -> {
				for (final Skill skill : RulesCatalog.getInstance().getSkills()) {
					if (skill.getCategoryId() != null && skill.getCategoryId().startsWith("weapons")) {
						ids.add(skill.getId());
					}
				}
			}
		}
		return ids;
	}

	/** Whether {@code grant}'s resolved target (see {@link #applyPerkChoiceGrant}) is a skill id. */
	private static boolean isPerkChoiceGrantSkillTarget(PerkChoiceGrant grant) {
		return grant.getSkillId() != null || grant.getScope() == PerkChoiceScope.ANY_SKILL
				|| grant.getScope() == PerkChoiceScope.ANY_WEAPON_SKILL;
	}

	/** Whether {@code grant}'s resolved target (see {@link #applyPerkChoiceGrant}) is a category id. */
	private static boolean isPerkChoiceGrantCategoryTarget(PerkChoiceGrant grant) {
		return grant.getCategoryId() != null || grant.getScope() == PerkChoiceScope.ANY_CATEGORY
				|| grant.getScope() == PerkChoiceScope.ANY_WEAPON_CATEGORY;
	}

	/** Every {@link PerkBonus} of every currently selected perk, in selection order. */
	private List<PerkBonus> getSelectedPerkBonuses() throws InvalidXmlElementException {
		final List<PerkBonus> bonuses = new ArrayList<>();
		for (final SelectedPerk selectedPerk : this.selectedPerks) {
			bonuses.addAll(RulesCatalog.getInstance().getPerk(selectedPerk.getPerkId()).getBonuses());
		}
		return bonuses;
	}

	/**
	 * Every already-resolved {@link PerkChoiceGrant} of every currently selected perk (paired with
	 * its resolved target id via {@link #applyPerkChoiceGrant}); a grant nobody has resolved yet
	 * (see {@link #applyPerkChoiceGrant}) contributes nothing until it is.
	 */
	private List<Map.Entry<PerkChoiceGrant, String>> getResolvedPerkChoiceGrants() throws InvalidXmlElementException {
		final List<Map.Entry<PerkChoiceGrant, String>> resolved = new ArrayList<>();
		for (final SelectedPerk selectedPerk : this.selectedPerks) {
			final Perk perk = RulesCatalog.getInstance().getPerk(selectedPerk.getPerkId());
			final List<PerkChoiceGrant> grants = perk.getChoiceGrants();
			for (int i = 0; i < grants.size(); i++) {
				final String key = PERK_CHOICE_KEY_PREFIX + perk.getId() + ":choice:" + i;
				if (this.decisions.isDecided(key)) {
					resolved.add(Map.entry(grants.get(i), this.decisions.get(key).getSelectedOption()));
				}
			}
		}
		return resolved;
	}

	/**
	 * The flat bonus every selected perk grants characteristic {@code abbreviation}, or 0. Only
	 * {@link PerkBonusKind#FLAT} bonuses count: {@link PerkBonusKind#CONDITIONAL} ones only apply
	 * under some condition described in the perk's free-text description, which is not modeled.
	 */
	public Integer getPerkCharacteristicBonus(CharacteristicAbbreviation abbreviation) throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (bonus.getCharacteristic() == abbreviation && bonus.getKind() == PerkBonusKind.FLAT) {
				total += bonus.getValue();
			}
		}
		return total;
	}

	/** Same as {@link #getPerkCharacteristicBonus}, for {@link PerkBonus#isAppearance()}. */
	public int getPerkAppearanceBonus() throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (bonus.isAppearance() && bonus.getKind() == PerkBonusKind.FLAT) {
				total += bonus.getValue();
			}
		}
		return total;
	}

	/** Same as {@link #getPerkCharacteristicBonus}, for {@link PerkBonus#isArmor()}. */
	public int getPerkArmorBonus() throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (bonus.isArmor() && bonus.getKind() == PerkBonusKind.FLAT) {
				total += bonus.getValue();
			}
		}
		return total;
	}

	/** Same as {@link #getPerkCharacteristicBonus}, for {@link PerkBonus#isMovement()}. */
	public int getPerkMovementBonus() throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (bonus.isMovement() && bonus.getKind() == PerkBonusKind.FLAT) {
				total += bonus.getValue();
			}
		}
		return total;
	}

	/** Same as {@link #getPerkCharacteristicBonus}, for {@link PerkBonus#getResistanceType()}. */
	public Integer getPerkResistanceBonus(ResistanceType resistanceType) throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (bonus.getResistanceType() == resistanceType && bonus.getKind() == PerkBonusKind.FLAT) {
				total += bonus.getValue();
			}
		}
		return total;
	}

	/**
	 * The flat bonus every selected perk grants skill {@code skillId}, or 0: either directly
	 * ({@link PerkBonus#getSkillId()}) or through an already-resolved choice grant that targets it
	 * (see {@link #applyPerkChoiceGrant}). {@link PerkBonusKind#PER_RANK} bonuses are not included
	 * here; see {@link #getPerkSkillRankBonus(String)}.
	 */
	public Integer getPerkSkillBonus(String skillId) throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (skillId.equals(bonus.getSkillId()) && bonus.getKind() == PerkBonusKind.FLAT) {
				total += bonus.getValue();
			}
		}
		for (final Map.Entry<PerkChoiceGrant, String> resolved : this.getResolvedPerkChoiceGrants()) {
			final PerkChoiceGrant grant = resolved.getKey();
			if (isPerkChoiceGrantSkillTarget(grant) && skillId.equals(resolved.getValue())
					&& grant.getKind() == PerkBonusKind.FLAT) {
				total += grant.getValue();
			}
		}
		return total;
	}

	/** Same as {@link #getPerkSkillBonus(String)}, for {@link PerkBonus#getCategoryId()}. */
	public Integer getPerkCategoryBonus(String categoryId) throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (categoryId.equals(bonus.getCategoryId()) && bonus.getKind() == PerkBonusKind.FLAT) {
				total += bonus.getValue();
			}
		}
		for (final Map.Entry<PerkChoiceGrant, String> resolved : this.getResolvedPerkChoiceGrants()) {
			final PerkChoiceGrant grant = resolved.getKey();
			if (isPerkChoiceGrantCategoryTarget(grant) && categoryId.equals(resolved.getValue())
					&& grant.getKind() == PerkBonusKind.FLAT) {
				total += grant.getValue();
			}
		}
		return total;
	}

	/**
	 * The sum of every {@link PerkBonusKind#PER_RANK} bonus a selected perk grants skill
	 * {@code skillId} (a bonus applied to every rank bought, e.g. "+4 to every rank of X"): not yet
	 * multiplied into {@link #getSkillDevelopmentBonus} (future work, since that depends on how many
	 * ranks were actually bought).
	 */
	public Integer getPerkSkillRankBonus(String skillId) throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (skillId.equals(bonus.getSkillId()) && bonus.getKind() == PerkBonusKind.PER_RANK) {
				total += bonus.getValue();
			}
		}
		return total;
	}

	/** Same as {@link #getPerkSkillRankBonus(String)}, for {@link PerkBonus#getCategoryId()}. */
	public Integer getPerkCategoryRankBonus(String categoryId) throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (categoryId.equals(bonus.getCategoryId()) && bonus.getKind() == PerkBonusKind.PER_RANK) {
				total += bonus.getValue();
			}
		}
		return total;
	}

	/**
	 * The sum of every {@link PerkBonusKind#CONDITIONAL} bonus a selected perk grants skill
	 * {@code skillId}: not included in {@link #getSkillDevelopmentBonus}, matching the legacy
	 * {@code getConditionalPerkBonus(Skill)} being a separate query from {@code getPerkBonus(Skill)}
	 * (it only applies under some condition described in the perk's free-text description, which
	 * consuming code must check itself).
	 */
	public Integer getPerkSkillConditionalBonus(String skillId) throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (skillId.equals(bonus.getSkillId()) && bonus.getKind() == PerkBonusKind.CONDITIONAL) {
				total += bonus.getValue();
			}
		}
		return total;
	}

	/** Same as {@link #getPerkSkillConditionalBonus(String)}, for {@link PerkBonus#getCategoryId()}. */
	public Integer getPerkCategoryConditionalBonus(String categoryId) throws InvalidXmlElementException {
		int total = 0;
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (categoryId.equals(bonus.getCategoryId()) && bonus.getKind() == PerkBonusKind.CONDITIONAL) {
				total += bonus.getValue();
			}
		}
		return total;
	}

	/** Whether a selected perk (or an already-resolved choice grant) makes {@code skillId} restricted. */
	public boolean isSkillRestrictedByPerk(String skillId) throws InvalidXmlElementException {
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (skillId.equals(bonus.getSkillId()) && bonus.getKind() == PerkBonusKind.MAKES_RESTRICTED) {
				return true;
			}
		}
		for (final Map.Entry<PerkChoiceGrant, String> resolved : this.getResolvedPerkChoiceGrants()) {
			final PerkChoiceGrant grant = resolved.getKey();
			if (isPerkChoiceGrantSkillTarget(grant) && skillId.equals(resolved.getValue())
					&& grant.getKind() == PerkBonusKind.MAKES_RESTRICTED) {
				return true;
			}
		}
		return false;
	}

	/** Same as {@link #isSkillRestrictedByPerk(String)}, for {@link PerkBonusKind#MAKES_COMMON}. */
	public boolean isSkillCommonByPerk(String skillId) throws InvalidXmlElementException {
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (skillId.equals(bonus.getSkillId()) && bonus.getKind() == PerkBonusKind.MAKES_COMMON) {
				return true;
			}
		}
		for (final Map.Entry<PerkChoiceGrant, String> resolved : this.getResolvedPerkChoiceGrants()) {
			final PerkChoiceGrant grant = resolved.getKey();
			if (isPerkChoiceGrantSkillTarget(grant) && skillId.equals(resolved.getValue())
					&& grant.getKind() == PerkBonusKind.MAKES_COMMON) {
				return true;
			}
		}
		return false;
	}

	/** Same as {@link #isSkillRestrictedByPerk(String)}, for {@link PerkBonus#getCategoryId()}. */
	public boolean isCategoryRestrictedByPerk(String categoryId) throws InvalidXmlElementException {
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (categoryId.equals(bonus.getCategoryId()) && bonus.getKind() == PerkBonusKind.MAKES_RESTRICTED) {
				return true;
			}
		}
		for (final Map.Entry<PerkChoiceGrant, String> resolved : this.getResolvedPerkChoiceGrants()) {
			final PerkChoiceGrant grant = resolved.getKey();
			if (isPerkChoiceGrantCategoryTarget(grant) && categoryId.equals(resolved.getValue())
					&& grant.getKind() == PerkBonusKind.MAKES_RESTRICTED) {
				return true;
			}
		}
		return false;
	}

	/** Same as {@link #isCategoryRestrictedByPerk(String)}, for {@link PerkBonusKind#MAKES_COMMON}. */
	public boolean isCategoryCommonByPerk(String categoryId) throws InvalidXmlElementException {
		for (final PerkBonus bonus : this.getSelectedPerkBonuses()) {
			if (categoryId.equals(bonus.getCategoryId()) && bonus.getKind() == PerkBonusKind.MAKES_COMMON) {
				return true;
			}
		}
		for (final Map.Entry<PerkChoiceGrant, String> resolved : this.getResolvedPerkChoiceGrants()) {
			final PerkChoiceGrant grant = resolved.getKey();
			if (isPerkChoiceGrantCategoryTarget(grant) && categoryId.equals(resolved.getValue())
					&& grant.getKind() == PerkBonusKind.MAKES_COMMON) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The selected race's fixed bonus/penalty to {@code resistanceType}'s resistance roll, or 0 if no
	 * race is selected or it grants none.
	 */
	public Integer getResistanceRaceBonus(ResistanceType resistanceType) throws InvalidXmlElementException {
		final Race race = this.getRace();
		return race == null ? 0 : race.getResistanceBonuses().getOrDefault(resistanceType.name(), 0);
	}

	/**
	 * {@code resistanceType}'s total resistance bonus: the selected race's fixed bonus plus every
	 * selected perk's flat bonus to it (see {@link #getPerkResistanceBonus(ResistanceType)}).
	 */
	public Integer getResistanceTotalBonus(ResistanceType resistanceType) throws InvalidXmlElementException {
		return this.getResistanceRaceBonus(resistanceType) + this.getPerkResistanceBonus(resistanceType);
	}

	/**
	 * Whether the selected race restricts profession {@code professionId} (the "PROFESIONES
	 * PROHIBIDAS" section), or {@code false} if no race is selected.
	 */
	public boolean isProfessionRestrictedByRace(String professionId) throws InvalidXmlElementException {
		final Race race = this.getRace();
		return race != null && race.getRestrictedProfessionIds().contains(professionId);
	}

	/**
	 * Every profession id the selected race allows (every profession {@link RulesCatalog} knows about,
	 * minus {@link Race#getRestrictedProfessionIds()}), or every known profession if no race is
	 * selected, matching the legacy {@code Race#getAvailableProfessions()}.
	 */
	public List<String> getAvailableProfessionIds() throws InvalidXmlElementException {
		final List<String> ids = new ArrayList<>();
		for (final Profession profession : RulesCatalog.getInstance().getProfessions()) {
			if (!this.isProfessionRestrictedByRace(profession.getId())) {
				ids.add(profession.getId());
			}
		}
		return ids;
	}

	/**
	 * Whether the selected race allows culture {@code cultureId} (its "CULTURAS DISPONIBLES"
	 * section, {@link Race#getCultureIds()}), or {@code false} if no race is selected.
	 */
	public boolean isCultureAvailableForRace(String cultureId) throws InvalidXmlElementException {
		final Race race = this.getRace();
		return race != null && race.getCultureIds().contains(cultureId);
	}

	/**
	 * Every culture id the selected race allows, restricted to cultures {@link RulesCatalog} actually
	 * knows about (a race may list a culture belonging to a module that is not currently enabled),
	 * matching the legacy {@code Race#getAvailableCultures()}; empty if no race is selected.
	 */
	public List<String> getAvailableCultureIds() throws InvalidXmlElementException {
		final Race race = this.getRace();
		if (race == null) {
			return List.of();
		}
		final List<String> ids = new ArrayList<>();
		for (final Culture culture : RulesCatalog.getInstance().getCultures()) {
			if (race.getCultureIds().contains(culture.getId())) {
				ids.add(culture.getId());
			}
		}
		return ids;
	}
}
