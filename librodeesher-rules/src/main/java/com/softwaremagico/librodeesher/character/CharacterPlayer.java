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
import com.softwaremagico.librodeesher.magic.ElementalTriad;
import com.softwaremagico.librodeesher.magic.MagicListType;
import com.softwaremagico.librodeesher.magic.MagicSpellList;
import com.softwaremagico.librodeesher.magic.MagicSpellListFactory;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkBonus;
import com.softwaremagico.librodeesher.perk.PerkBonusKind;
import com.softwaremagico.librodeesher.perk.PerkChoiceGrant;
import com.softwaremagico.librodeesher.perk.PerkChoiceScope;
import com.softwaremagico.librodeesher.perk.PerkGrade;
import com.softwaremagico.librodeesher.perk.SelectedPerk;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.ProfessionMagicCost;
import com.softwaremagico.librodeesher.profession.ProfessionSkillGrant;
import com.softwaremagico.librodeesher.profession.ProfessionTrainingCost;
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
import com.softwaremagico.librodeesher.training.TrainingProfessionCost;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;
import com.softwaremagico.librodeesher.training.TrainingType;
import com.softwaremagico.librodeesher.weapon.Weapon;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
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
		return total - this.getSkillSpecializationsRankCost(skillId);
	}

	/**
	 * Selects {@code specializationId} as one of the "hidden" ranks {@code skillId} unlocks (e.g. one
	 * of a soft/hardened leather armor skill's {@code "TA9"}/{@code "TA10"}/{@code "TA11"} armor
	 * training table numbers, see {@link Skill#getSpecialities()}), recorded on {@link
	 * #getCurrentLevel()}; matches the legacy {@code addSkillSpecialization(Skill, String)}.
	 * Mutually exclusive with {@link #generalizeSkill(String)}: also removes {@code skillId}'s
	 * "generalized" mark, if any.
	 *
	 * @throws IllegalArgumentException if {@code specializationId} is not one of {@code skillId}'s
	 *                                   specialities.
	 */
	public void addSkillSpecialization(String skillId, String specializationId) throws InvalidXmlElementException {
		final Skill skill = RulesCatalog.getInstance().getSkill(skillId);
		if (!skill.getSpecialities().contains(specializationId)) {
			throw new IllegalArgumentException(
					"'" + specializationId + "' is not one of '" + skillId + "''s specialities " + skill.getSpecialities() + ".");
		}
		this.removeSkillGeneralization(skillId);
		this.getCurrentLevel().addSkillSpecialization(specializationId);
	}

	/** Every one of {@code skillId}'s specialities (see {@link Skill#getSpecialities()}) selected so far, across every level. */
	public List<String> getSkillSpecializations(String skillId) throws InvalidXmlElementException {
		final List<String> specialityIds = RulesCatalog.getInstance().getSkill(skillId).getSpecialities();
		if (specialityIds.isEmpty()) {
			return List.of();
		}
		final List<String> selected = new ArrayList<>();
		for (final LevelUp levelUp : this.levels) {
			selected.addAll(levelUp.getSkillSpecializations(specialityIds));
		}
		return selected;
	}

	/**
	 * How many of {@code skillId}'s "hidden" specialization ranks have been selected so far: these
	 * count as ranks bought (they cost the same as a normal rank, see {@code Training}/{@code
	 * Category}'s rank-cost tables) but are not counted towards {@link #getSkillTotalRanks(String)}
	 * itself, matching the legacy {@code getTotalRanks(Skill)} exactly (ranks minus specialities
	 * selected).
	 */
	private int getSkillSpecializationsRankCost(String skillId) {
		final List<String> specialityIds;
		try {
			specialityIds = RulesCatalog.getInstance().getSkill(skillId).getSpecialities();
		} catch (final InvalidXmlElementException e) {
			return 0;
		}
		if (specialityIds.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (final LevelUp levelUp : this.levels) {
			total += levelUp.getRanksSpentInSpecializations(specialityIds);
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
	 * @param additionalSkillRanks
	 *            how many ranks to grant each freely-chosen skill of the resolved
	 *            category, on top of {@code grant}'s named skill grants (if any);
	 *            only meaningful when {@code grant}'s named skill grants do not
	 *            already add up to {@link TrainingCategoryGrant#getRanksToDistribute()}
	 *            (the common case, e.g. "choose 1-3 skills of the Outdoor/Animal
	 *            category and freely distribute 4 ranks among them" leaves no named
	 *            skill grants at all). Ignored otherwise.
	 * @throws IllegalArgumentException if a skill in {@code additionalSkillRanks} is
	 *             not one of the resolved category's skills (or is already one of
	 *             {@code grant}'s named skills), if its size is not between
	 *             {@link TrainingCategoryGrant#getMinSkills()} and
	 *             {@link TrainingCategoryGrant#getMaxSkills()} (discounting the named
	 *             skills), or if its values do not add up to exactly the ranks left
	 *             to distribute.
	 */
	public void applyCategoryGrant(String key, TrainingCategoryGrant grant, String selectedCategoryId,
			List<String> selectedSkillIds, Map<String, Integer> additionalSkillRanks) throws InvalidXmlElementException {
		final List<String> offeredCategories = this.expandCategoryWildcards(grant.getCategoryOptions());
		final Decision categoryDecision = this.decideOrReuse(key,
				() -> offeredCategories.size() > 1
						? Decision.select(offeredCategories, selectedCategoryId)
						: Decision.fixed(offeredCategories));
		final String categoryId = categoryDecision.getSelectedOption();
		this.getCurrentLevel().addCategoryRanks(categoryId, grant.getRanksGranted());

		final List<TrainingSkillGrant> skills = grant.getSkills();
		final Set<String> namedSkillIds = new HashSet<>();
		int namedRanks = 0;
		for (int i = 0; i < skills.size(); i++) {
			final TrainingSkillGrant skillGrant = skills.get(i);
			final String skillKey = key + ":skill:" + i;
			final String selectedSkillId = selectedSkillIds != null && i < selectedSkillIds.size()
					? selectedSkillIds.get(i)
					: null;
			final Decision skillDecision = this.decideOrReuse(skillKey, () -> skillGrant.resolve(selectedSkillId));
			namedSkillIds.add(skillDecision.getSelectedOption());
			namedRanks += skillGrant.getRanksToDistribute();
			// Whether this is a spell skill is not resolved here (it requires
			// cross-referencing the
			// skill's category), see LevelUp#setSkillRanks; future work.
			this.getCurrentLevel().addSkillRanks(skillDecision.getSelectedOption(), skillGrant.getRanksToDistribute(),
					false);
		}

		final int remainingRanks = grant.getRanksToDistribute() == null ? 0 : grant.getRanksToDistribute() - namedRanks;
		if (remainingRanks > 0) {
			this.applyAdditionalCategorySkillRanks(grant, categoryId, namedSkillIds, remainingRanks,
					additionalSkillRanks == null ? Map.of() : additionalSkillRanks);
		}
	}

	/**
	 * Validates and applies the "freely chosen" part of a category grant's skill
	 * distribution (see {@link #applyCategoryGrant}'s {@code additionalSkillRanks}
	 * parameter).
	 */
	private void applyAdditionalCategorySkillRanks(TrainingCategoryGrant grant, String categoryId,
			Set<String> namedSkillIds, int remainingRanks, Map<String, Integer> additionalSkillRanks)
			throws InvalidXmlElementException {
		final int minAdditionalSkills = Math.max(0, grant.getMinSkills() - namedSkillIds.size());
		final int maxAdditionalSkills = grant.getMaxSkills() - namedSkillIds.size();
		if (additionalSkillRanks.size() < minAdditionalSkills || additionalSkillRanks.size() > maxAdditionalSkills) {
			throw new IllegalArgumentException("Expected between " + minAdditionalSkills + " and " + maxAdditionalSkills
					+ " additional skill(s) for category '" + categoryId + "', got " + additionalSkillRanks.size() + ".");
		}
		final Category category = RulesCatalog.getInstance().getCategory(categoryId);
		final List<String> categorySkillIds = category.hasDynamicSkills() ? this.getWeaponSkillIds(categoryId)
				: category.getSkills();
		int sum = 0;
		for (final Map.Entry<String, Integer> entry : additionalSkillRanks.entrySet()) {
			if (namedSkillIds.contains(entry.getKey()) || !categorySkillIds.contains(entry.getKey())) {
				throw new IllegalArgumentException(
						"'" + entry.getKey() + "' is not one of category '" + categoryId + "''s remaining skills.");
			}
			sum += entry.getValue();
		}
		if (sum != remainingRanks) {
			throw new IllegalArgumentException(
					"Expected the additional skill ranks to add up to " + remainingRanks + ", got " + sum + ".");
		}
		for (final Map.Entry<String, Integer> entry : additionalSkillRanks.entrySet()) {
			this.getCurrentLevel().addSkillRanks(entry.getKey(), entry.getValue(), false);
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
	 * @param additionalSkillRanksSelections
	 *            same, but for each grant's {@code additionalSkillRanks} (see
	 *            {@link #applyCategoryGrant}), keyed the same way.
	 */
	public void applyTrainingCategories(Training training, Map<Integer, String> categorySelections,
			Map<Integer, List<String>> skillSelections, Map<Integer, Map<String, Integer>> additionalSkillRanksSelections)
			throws InvalidXmlElementException {
		this.applyCategoryGrants("training:" + training.getId() + ":category", training.getCategories(),
				categorySelections, skillSelections, additionalSkillRanksSelections);
	}

	/**
	 * Applies every adolescence-rank category grant of {@code culture} to the
	 * current level, keying each one's decision as
	 * {@code "culture:" + culture.getId() + ":adolescence:" + <index>}; see
	 * {@link #applyTrainingCategories} for the selection map semantics.
	 */
	public void applyCultureAdolescenceRanks(Culture culture, Map<Integer, String> categorySelections,
			Map<Integer, List<String>> skillSelections, Map<Integer, Map<String, Integer>> additionalSkillRanksSelections)
			throws InvalidXmlElementException {
		this.applyCategoryGrants("culture:" + culture.getId() + ":adolescence", culture.getAdolescenceRanks(),
				categorySelections, skillSelections, additionalSkillRanksSelections);
	}

	private void applyCategoryGrants(String keyPrefix, List<TrainingCategoryGrant> grants,
			Map<Integer, String> categorySelections, Map<Integer, List<String>> skillSelections,
			Map<Integer, Map<String, Integer>> additionalSkillRanksSelections) throws InvalidXmlElementException {
		for (int i = 0; i < grants.size(); i++) {
			final String selectedCategoryId = categorySelections == null ? null : categorySelections.get(i);
			final List<String> selectedSkillIds = skillSelections == null ? null : skillSelections.get(i);
			final Map<String, Integer> additionalSkillRanks = additionalSkillRanksSelections == null ? null
					: additionalSkillRanksSelections.get(i);
			this.applyCategoryGrant(keyPrefix + ":" + i, grants.get(i), selectedCategoryId, selectedSkillIds,
					additionalSkillRanks);
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

	/** The weapon (id, doubling as its skill id) of every {@link Weapon} belonging to {@code categoryId}. */
	private List<String> getWeaponSkillIds(String categoryId) throws InvalidXmlElementException {
		final List<String> ids = new ArrayList<>();
		for (final Weapon weapon : RulesCatalog.getInstance().getWeapons()) {
			if (categoryId.equals(weapon.getCategoryId())) {
				ids.add(weapon.getId());
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
	 * Marks {@code skillId} as "generalized" on {@link #getCurrentLevel()}: a generalized skill
	 * counts fully for its rank multiplier only when it is also common or professional (half
	 * otherwise, see {@link #getSkillRankMultiplier(Skill)}), but no longer distinguishes between its
	 * specialities. Mutually exclusive with {@link #addSkillSpecialization(String, String)}: matches
	 * the legacy {@code addGeneralized(Skill)}, which always calls {@code removeSpecialized(skill)}
	 * first.
	 */
	public void generalizeSkill(String skillId) throws InvalidXmlElementException {
		this.removeSkillSpecializations(skillId);
		this.getCurrentLevel().addGeneralizedSkill(skillId);
	}

	/** Removes every selected specialization of {@code skillId}, across every level. */
	public void removeSkillSpecializations(String skillId) throws InvalidXmlElementException {
		final List<String> specialityIds = RulesCatalog.getInstance().getSkill(skillId).getSpecialities();
		for (final LevelUp levelUp : this.levels) {
			for (final String specialityId : specialityIds) {
				levelUp.removeSkillSpecialization(specialityId);
			}
		}
	}

	/** Removes {@code skillId}'s "generalized" mark, across every level. */
	public void removeSkillGeneralization(String skillId) {
		for (final LevelUp levelUp : this.levels) {
			levelUp.removeGeneralizedSkill(skillId);
		}
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
				|| (profession != null && this.isSkillGrantedByProfessionChoice(skill.getId(),
						PROFESSION_RESTRICTED_SKILLS_SECTION, profession.getRestrictedSkillChoices()))
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
				|| (profession != null && this.isSkillGrantedByProfessionChoice(skill.getId(),
						PROFESSION_COMMON_SKILLS_SECTION, profession.getCommonSkillChoices()))
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
				|| (profession != null && profession.isProfessionalSkill(skill.getId()))
				|| (profession != null && this.isSkillGrantedByProfessionChoice(skill.getId(),
						PROFESSION_PROFESSIONAL_SKILLS_SECTION, profession.getProfessionalSkillChoices()));
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

	/**
	 * Whether {@code perkId} is available to the selected race/profession (its "Permitido" column),
	 * matching the legacy {@code Perk#isPerkAllowed(String, String)} exactly: available to everyone
	 * if it names no race/profession at all, otherwise available if either the selected race or the
	 * selected profession is one of the ones it names (not enforced by {@link #addPerk(String)},
	 * same as every other "is this selection allowed" query in this class - consuming code decides
	 * whether to act on it).
	 */
	public boolean isPerkAllowedForCharacter(String perkId) throws InvalidXmlElementException {
		final Perk perk = RulesCatalog.getInstance().getPerk(perkId);
		if (perk.isAvailableToEveryone()) {
			return true;
		}
		final Race race = this.tryGetSelectedRace();
		if (race != null && perk.getAvailableToRaceIds().contains(race.getId())) {
			return true;
		}
		final Profession profession = this.tryGetSelectedProfession();
		return profession != null && perk.getAvailableToProfessionIds().contains(profession.getId());
	}

	/** {@link #getRace()} without the checked exception, {@code null} on failure; see {@link #tryGetSelectedProfession()}. */
	private Race tryGetSelectedRace() {
		try {
			return this.getRace();
		} catch (final InvalidXmlElementException e) {
			return null;
		}
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

	/** Whether the selected profession can cast spells at all (see {@link Profession#isSpellCaster()}); {@code false} if none is selected. */
	public boolean isSpellCaster() throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		return profession != null && profession.isSpellCaster();
	}

	/**
	 * Every realm of magic the selected profession grants, already resolved (see {@link
	 * #applyProfessionMagicRealms}); empty if no profession is selected, it is not a spell caster, or
	 * a hybrid realm grant has not been resolved yet.
	 */
	public List<RealmOfMagic> getRealmsOfMagic() throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		if (profession == null) {
			return List.of();
		}
		final List<RealmOfMagic> realms = new ArrayList<>();
		for (final String tag : this.getDecidedOptions("profession:" + profession.getId() + ":realm",
				profession.getMagicRealms().size())) {
			realms.add(RealmOfMagic.valueOf(tag));
		}
		return realms;
	}

	/**
	 * Every spell list marked as an "open list" (see {@link MagicSpellList#isOpenList()}) of one of
	 * the character's {@link #getRealmsOfMagic()}: any spell-casting profession of that realm may
	 * freely pick ranks in it (matching the legacy {@code MagicFactory#getOpenLists}).
	 */
	public List<MagicSpellList> getOpenSpellLists() throws InvalidXmlElementException {
		return this.getSpellListsMatching(MagicSpellList::isOpenList);
	}

	/**
	 * Every spell list marked as a "closed list" (see {@link MagicSpellList#isClosedList()}) of one of
	 * the character's {@link #getRealmsOfMagic()}: restricted, must be specifically granted (matching
	 * the legacy {@code MagicFactory#getClosedLists}; that specific grant itself is future work, same
	 * as the general "spend development points on a spell list" mechanic, see {@link
	 * LevelUp#getSpellRankMultiplier}).
	 */
	public List<MagicSpellList> getClosedSpellLists() throws InvalidXmlElementException {
		return this.getSpellListsMatching(MagicSpellList::isClosedList);
	}

	/**
	 * Every spell list of one of the character's {@link #getRealmsOfMagic()} directly owned by the
	 * selected profession, or by the character's own elementalist training if any (its "basic lists";
	 * matching the legacy {@code MagicFactory#getListOfProfession}/{@code
	 * MagicSpellLists#orderSpellListsByCategory}'s own {@code elementalistList} addition to {@code
	 * basicSpells}; the "dark spell" special case is still future work); empty if no profession is
	 * selected and the character has no elementalist training either.
	 */
	public List<MagicSpellList> getBasicSpellLists() throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		final String ownProfessionId = profession == null ? null : profession.getId();
		final String ownElementalistTrainingId = this.getElementalistTrainingId();
		if (ownProfessionId == null && ownElementalistTrainingId == null) {
			return List.of();
		}
		return this.getSpellListsMatching(list -> list.getOwners().contains(ownProfessionId)
				|| list.getOwners().contains(ownElementalistTrainingId));
	}

	/**
	 * Every spell list of one of the character's {@link #getRealmsOfMagic()} directly owned by one of
	 * the character's selected trainings, other than an elementalist one (see {@link
	 * ElementalTriad#isElementalistTraining(String)}: those are classified as {@link
	 * MagicListType#TRIAD}/{@link MagicListType#COMPLEMENTARY_TRIAD} instead, see {@link
	 * #getTriadSpellLists()}/{@link #getComplementaryTriadSpellLists()}), matching the legacy {@code
	 * MagicFactory#getListOfTraining} exactly.
	 */
	public List<MagicSpellList> getTrainingSpellLists() throws InvalidXmlElementException {
		final List<String> trainingIds = this.getNonElementalistTrainingIds();
		return this.getSpellListsMatching(list -> containsAny(list.getOwners(), trainingIds));
	}

	/**
	 * Every spell list of one of the character's {@link #getRealmsOfMagic()} directly owned by an
	 * elementalist training of the same "elemental triad" (see {@link ElementalTriad}) as one of the
	 * character's selected trainings (excluding the character's own training itself, matching the
	 * legacy {@code MagicFactory#getListOfOwnTriad}); empty if the character has no elementalist
	 * training selected.
	 */
	public List<MagicSpellList> getTriadSpellLists() throws InvalidXmlElementException {
		final List<String> sameTriadTrainingIds = this.getSameTriadTrainingIds();
		return this.getSpellListsMatching(list -> containsAny(list.getOwners(), sameTriadTrainingIds));
	}

	/**
	 * Every spell list of one of the character's {@link #getRealmsOfMagic()} directly owned by an
	 * elementalist training of the "elemental triad" (see {@link ElementalTriad}) other than the one
	 * of the character's own selected training (matching the legacy {@code
	 * MagicFactory#getListOfOtherTriad}); empty if the character has no elementalist training
	 * selected.
	 */
	public List<MagicSpellList> getComplementaryTriadSpellLists() throws InvalidXmlElementException {
		final List<String> otherTriadTrainingIds = this.getOtherTriadTrainingIds();
		return this.getSpellListsMatching(list -> containsAny(list.getOwners(), otherTriadTrainingIds));
	}

	/**
	 * Every spell list of one of the character's {@link #getRealmsOfMagic()} directly owned by some
	 * other real profession (neither the character's own, nor a training, nor the open/closed pseudo-
	 * owner tags), matching the legacy {@code MagicFactory#getListOfOtherProfessions}.
	 */
	public List<MagicSpellList> getOtherProfessionSpellLists() throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		final String ownProfessionId = profession == null ? null : profession.getId();
		return this.getSpellListsMatching(
				list -> list.getOwners().stream().anyMatch(owner -> !owner.equals(ownProfessionId) && this.isRealProfessionId(owner)));
	}

	/**
	 * Every spell list marked open, of a realm other than any of the character's {@link
	 * #getRealmsOfMagic()} (matching the legacy {@code MagicFactory#getOtherRealmOpenLists}).
	 */
	public List<MagicSpellList> getOtherRealmOpenSpellLists() throws InvalidXmlElementException {
		return this.getOtherRealmSpellListsMatching(MagicSpellList::isOpenList);
	}

	/**
	 * Every spell list marked closed, of a realm other than any of the character's {@link
	 * #getRealmsOfMagic()} (matching the legacy {@code MagicFactory#getOtherRealmClosedLists}).
	 */
	public List<MagicSpellList> getOtherRealmClosedSpellLists() throws InvalidXmlElementException {
		return this.getOtherRealmSpellListsMatching(MagicSpellList::isClosedList);
	}

	/**
	 * Every spell list of a realm other than any of the character's {@link #getRealmsOfMagic()},
	 * directly owned by one of the character's selected trainings, other than an elementalist one
	 * (matching the legacy {@code MagicFactory#getListOfTrainingOtherRealms}; same simplification as
	 * {@link #getTrainingSpellLists()}).
	 */
	public List<MagicSpellList> getOtherRealmTrainingSpellLists() throws InvalidXmlElementException {
		final List<String> trainingIds = this.getNonElementalistTrainingIds();
		return this.getOtherRealmSpellListsMatching(list -> containsAny(list.getOwners(), trainingIds));
	}

	/**
	 * Every spell list of a realm other than any of the character's {@link #getRealmsOfMagic()},
	 * directly owned by some other real profession (matching the legacy {@code
	 * MagicFactory#getListOfOtherProfessionsOtherRealm}).
	 */
	public List<MagicSpellList> getOtherRealmOtherProfessionSpellLists() throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		final String ownProfessionId = profession == null ? null : profession.getId();
		return this.getOtherRealmSpellListsMatching(
				list -> list.getOwners().stream().anyMatch(owner -> !owner.equals(ownProfessionId) && this.isRealProfessionId(owner)));
	}

	/**
	 * Every spell list marked open of {@link RealmOfMagic#ARCHANUM}: available to any spell caster
	 * regardless of their own {@link #getRealmsOfMagic()} (matching the legacy {@code
	 * MagicFactory#getArchanumOpenLists}, which is not filtered by the character's own realm(s)
	 * either); empty if the character is not a spell caster at all.
	 */
	public List<MagicSpellList> getArchanumSpellLists() throws InvalidXmlElementException {
		if (!this.isSpellCaster()) {
			return List.of();
		}
		final List<MagicSpellList> lists = new ArrayList<>();
		for (final MagicSpellList list : MagicSpellListFactory.getInstance().getSpellLists(RealmOfMagic.ARCHANUM)) {
			if (list.isOpenList()) {
				lists.add(list);
			}
		}
		return lists;
	}

	/**
	 * Every spell list of {@link RealmOfMagic#RACE} owned by the selected race (matching the legacy
	 * {@code MagicFactory#getRaceLists}); empty if no race is selected.
	 */
	public List<MagicSpellList> getRaceSpellLists() throws InvalidXmlElementException {
		final Race race = this.getRace();
		if (race == null) {
			return List.of();
		}
		final List<MagicSpellList> lists = new ArrayList<>();
		for (final MagicSpellList list : MagicSpellListFactory.getInstance().getSpellLists(RealmOfMagic.RACE)) {
			if (list.getOwners().contains(race.getId())) {
				lists.add(list);
			}
		}
		return lists;
	}

	/** Every spell list of one of the character's {@link #getRealmsOfMagic()} matching {@code filter}. */
	private List<MagicSpellList> getSpellListsMatching(Predicate<MagicSpellList> filter)
			throws InvalidXmlElementException {
		final List<MagicSpellList> lists = new ArrayList<>();
		for (final RealmOfMagic realm : this.getRealmsOfMagic()) {
			for (final MagicSpellList list : MagicSpellListFactory.getInstance().getSpellLists(realm)) {
				if (filter.test(list)) {
					lists.add(list);
				}
			}
		}
		return lists;
	}

	/** Every spell list of a realm other than any of the character's {@link #getRealmsOfMagic()} matching {@code filter}. */
	private List<MagicSpellList> getOtherRealmSpellListsMatching(Predicate<MagicSpellList> filter)
			throws InvalidXmlElementException {
		final List<RealmOfMagic> ownRealms = this.getRealmsOfMagic();
		final List<MagicSpellList> lists = new ArrayList<>();
		for (final RealmOfMagic realm : RealmOfMagic.values()) {
			if (ownRealms.contains(realm)) {
				continue;
			}
			for (final MagicSpellList list : MagicSpellListFactory.getInstance().getSpellLists(realm)) {
				if (filter.test(list)) {
					lists.add(list);
				}
			}
		}
		return lists;
	}

	/** Every training id selected so far (see {@link LevelUp#getTrainings()}), across every level. */
	private List<String> getSelectedTrainingIds() {
		final List<String> ids = new ArrayList<>();
		for (final LevelUp levelUp : this.levels) {
			ids.addAll(levelUp.getTrainings());
		}
		return ids;
	}

	/** Every selected training id (see {@link #getSelectedTrainingIds()}) that is not an elementalist training. */
	private List<String> getNonElementalistTrainingIds() {
		final List<String> ids = new ArrayList<>();
		for (final String trainingId : this.getSelectedTrainingIds()) {
			if (!ElementalTriad.isElementalistTraining(trainingId)) {
				ids.add(trainingId);
			}
		}
		return ids;
	}

	/** The character's own elementalist training (see {@link ElementalTriad}), or {@code null} if none is selected. */
	private String getElementalistTrainingId() {
		for (final String trainingId : this.getSelectedTrainingIds()) {
			if (ElementalTriad.isElementalistTraining(trainingId)) {
				return trainingId;
			}
		}
		return null;
	}

	/** {@link ElementalTriad#getSameTriadTrainings}, applied to {@link #getElementalistTrainingId()}. */
	private List<String> getSameTriadTrainingIds() {
		final String own = this.getElementalistTrainingId();
		return own == null ? List.of() : ElementalTriad.getSameTriadTrainings(own);
	}

	/** {@link ElementalTriad#getOtherTriadTrainings}, applied to {@link #getElementalistTrainingId()}. */
	private List<String> getOtherTriadTrainingIds() {
		final String own = this.getElementalistTrainingId();
		return own == null ? List.of() : ElementalTriad.getOtherTriadTrainings(own);
	}

	private static boolean containsAny(List<String> haystack, List<String> needles) {
		return haystack.stream().anyMatch(needles::contains);
	}

	private boolean isRealProfessionId(String id) {
		try {
			RulesCatalog.getInstance().getProfession(id);
			return true;
		} catch (final InvalidXmlElementException e) {
			return false;
		}
	}

	/**
	 * Classifies {@code spellListId} for this character (see {@link
	 * com.softwaremagico.librodeesher.magic.MagicListType}'s javadoc for which classifications are
	 * currently resolved - every one except {@link MagicListType#TRIAD}/{@link
	 * MagicListType#COMPLEMENTARY_TRIAD}), or {@code null} if none applies (including {@link
	 * RealmOfMagic#RACE}'s own lists, which this does not classify: see {@link #getRaceSpellLists()}
	 * instead).
	 *
	 * <p>Priority order (own realm first, then own profession/training before another real
	 * profession's): {@link MagicListType#ARCHANUM} (realm-independent, see {@link
	 * #getArchanumSpellLists()}) &gt; {@link MagicListType#BASIC} &gt; {@link MagicListType#OPEN}
	 * &gt; {@link MagicListType#CLOSED} &gt; {@link MagicListType#TRAINING} &gt; {@link
	 * MagicListType#OTHER_PROFESSION} for one of the character's own realms; {@link
	 * MagicListType#OTHER_REALM_TRAINING} &gt; {@link MagicListType#OTHER_REALM_OTHER_PROFESSION}
	 * &gt; {@link MagicListType#OTHER_REALM_OPEN} &gt; {@link MagicListType#OTHER_REALM_CLOSED} for
	 * every other realm.</p>
	 */
	public MagicListType classifySpellList(String spellListId) throws InvalidXmlElementException {
		final MagicSpellList spellList = MagicSpellListFactory.getInstance().getElement(spellListId);
		if (spellList.getRealm() == RealmOfMagic.ARCHANUM && spellList.isOpenList() && this.isSpellCaster()) {
			return MagicListType.ARCHANUM;
		}
		final Profession profession = this.getProfession();
		final String ownProfessionId = profession == null ? null : profession.getId();
		final List<String> sameTriadTrainingIds = this.getSameTriadTrainingIds();
		final List<String> otherTriadTrainingIds = this.getOtherTriadTrainingIds();
		final List<String> trainingIds = this.getNonElementalistTrainingIds();
		final String ownElementalistTrainingId = this.getElementalistTrainingId();
		if (this.getRealmsOfMagic().contains(spellList.getRealm())) {
			if ((ownProfessionId != null && spellList.getOwners().contains(ownProfessionId))
					|| (ownElementalistTrainingId != null && spellList.getOwners().contains(ownElementalistTrainingId))) {
				return MagicListType.BASIC;
			}
			if (spellList.isOpenList()) {
				return MagicListType.OPEN;
			}
			if (spellList.isClosedList()) {
				return MagicListType.CLOSED;
			}
			if (containsAny(spellList.getOwners(), sameTriadTrainingIds)) {
				return MagicListType.TRIAD;
			}
			if (containsAny(spellList.getOwners(), otherTriadTrainingIds)) {
				return MagicListType.COMPLEMENTARY_TRIAD;
			}
			if (containsAny(spellList.getOwners(), trainingIds)) {
				return MagicListType.TRAINING;
			}
			if (spellList.getOwners().stream().anyMatch(owner -> !owner.equals(ownProfessionId) && this.isRealProfessionId(owner))) {
				return MagicListType.OTHER_PROFESSION;
			}
			return null;
		}
		if (containsAny(spellList.getOwners(), trainingIds)) {
			return MagicListType.OTHER_REALM_TRAINING;
		}
		if (spellList.getOwners().stream().anyMatch(owner -> !owner.equals(ownProfessionId) && this.isRealProfessionId(owner))) {
			return MagicListType.OTHER_REALM_OTHER_PROFESSION;
		}
		if (spellList.isOpenList()) {
			return MagicListType.OTHER_REALM_OPEN;
		}
		if (spellList.isClosedList()) {
			return MagicListType.OTHER_REALM_CLOSED;
		}
		return null;
	}

	/**
	 * The background point cost of a rank bought in {@code spellListId} (see {@link
	 * Profession#getMagicCost(MagicListType, int)}), given it already has {@code currentListRanks}
	 * ranks bought across every level, and {@code ranksBoughtThisLevel} of them were bought at the
	 * current level (0-based, so {@code ranksBoughtThisLevel=0} is the first rank bought this level in
	 * this list); {@code null} if no profession is selected, {@link #classifySpellList(String)} cannot
	 * classify it for this character yet, or the profession has no cost defined for that bracket/rank
	 * index.
	 */
	public Integer getSpellListDevelopmentCost(String spellListId, int currentListRanks, int ranksBoughtThisLevel)
			throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		if (profession == null) {
			return null;
		}
		final MagicListType listType = this.classifySpellList(spellListId);
		if (listType == null) {
			return null;
		}
		final ProfessionMagicCost bracket = profession.getMagicCost(listType, currentListRanks);
		return bracket == null ? null : bracket.getRankCost(ranksBoughtThisLevel);
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

	/**
	 * The selected profession's explicit background point cost/preference for {@code trainingId}
	 * (see {@link Profession#getTrainingCost(String)}), or {@code null} if no profession is selected
	 * or it does not mention that training.
	 */
	public ProfessionTrainingCost getProfessionTrainingCost(String trainingId) throws InvalidXmlElementException {
		final Profession profession = this.getProfession();
		return profession == null ? null : profession.getTrainingCost(trainingId);
	}

	/**
	 * Whether the selected profession forbids {@code trainingId}: either the profession's own
	 * {@link #getProfessionTrainingCost} says so, or {@code trainingId}'s own {@link
	 * Training#getProfessionCost(String)} override does (either side can forbid it, matching legacy's
	 * {@code CharacterPlayer#getAvailableTrainings()}); {@code false} if no profession is selected or
	 * neither side mentions it.
	 */
	public boolean isTrainingForbiddenByProfession(String trainingId) throws InvalidXmlElementException {
		return this.trainingProfessionPreference(trainingId) == TrainingType.FORBIDDEN;
	}

	/** Same as {@link #isTrainingForbiddenByProfession(String)}, for {@link TrainingType#FAVOURITE}. */
	public boolean isTrainingFavouredByProfession(String trainingId) throws InvalidXmlElementException {
		return this.trainingProfessionPreference(trainingId) == TrainingType.FAVOURITE;
	}

	/**
	 * The selected profession's preference for {@code trainingId} (see {@link
	 * #isTrainingForbiddenByProfession}/{@link #isTrainingFavouredByProfession}): the profession's own
	 * side takes priority, falling back to {@code trainingId}'s own override; {@code null} if no
	 * profession is selected or neither side mentions it.
	 */
	private TrainingType trainingProfessionPreference(String trainingId) throws InvalidXmlElementException {
		final ProfessionTrainingCost professionSide = this.getProfessionTrainingCost(trainingId);
		if (professionSide != null) {
			return professionSide.getType();
		}
		final Profession profession = this.getProfession();
		if (profession == null) {
			return null;
		}
		final TrainingProfessionCost trainingSide = RulesCatalog.getInstance().getTraining(trainingId)
				.getProfessionCost(profession.getId());
		return trainingSide == null ? null : trainingSide.getType();
	}

	/**
	 * Whether {@code training} is available to the selected race: either it has no "EXCLUSIVO RAZA"
	 * restriction at all ({@link Training#isAvailableToEveryRace()}), or the selected race's id is
	 * one of {@link Training#getLimitedRaces()}. {@code false} if it is race-restricted and no race
	 * is selected.
	 */
	public boolean isTrainingAvailableForRace(Training training) throws InvalidXmlElementException {
		if (training.isAvailableToEveryRace()) {
			return true;
		}
		final Race race = this.getRace();
		return race != null && training.getLimitedRaces().contains(race.getId());
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
	private static final String GROUP_TOKEN_PREFIX = "group:";

	/**
	 * Whether {@code professionId} matches reference {@code token} from a race's "PROFESIONES
	 * PROHIBIDAS" section: a plain token matches by id; a {@code "group:"} token matches every
	 * profession that casts in that magic realm (matching the legacy {@code
	 * MagicFactory#getSpellCasters(RealmOfMagic)}, resolved here through {@link
	 * Profession#getMagicRealms()} instead, since no magic-list catalog exists).
	 */
	/**
	 * Whether {@code professionId} matches reference {@code token} from a race's "PROFESIONES
	 * PROHIBIDAS" section: a plain token matches by id (even if {@code professionId} does not name a
	 * real, migrated profession - a handful of real race files list one that does not, e.g. a
	 * training name by mistake, always inert); a {@code "group:"} token matches every profession
	 * that casts in that magic realm (matching the legacy {@code
	 * MagicFactory#getSpellCasters(RealmOfMagic)}, resolved here through {@link
	 * Profession#getMagicRealms()} instead, since no magic-list catalog exists), which needs
	 * {@code professionId} to actually resolve (never true otherwise).
	 */
	private boolean professionMatchesReference(String professionId, String token) throws InvalidXmlElementException {
		if (!token.startsWith(GROUP_TOKEN_PREFIX)) {
			return professionId.equals(token);
		}
		final RealmOfMagic realm;
		try {
			realm = RealmOfMagic.valueOf(token.substring(GROUP_TOKEN_PREFIX.length()).toUpperCase());
		} catch (final IllegalArgumentException e) {
			return false;
		}
		final Profession profession;
		try {
			profession = RulesCatalog.getInstance().getProfession(professionId);
		} catch (final InvalidXmlElementException e) {
			return false;
		}
		for (final RealmOfMagicGrant grant : profession.getMagicRealms()) {
			if (grant.getOptions().contains(realm)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether {@code professionId} is restricted by the selected race's "PROFESIONES PROHIBIDAS"
	 * section, or {@code false} if no race is selected. Once {@link Race#getExcludedProfessionIds()}
	 * is non-empty the section's meaning flips (matching the legacy per-file "exception" flag): the
	 * union of {@link Race#getRestrictedProfessionIds()} and {@link Race#getExcludedProfessionIds()}
	 * becomes the only professions the race is <em>allowed</em> to take, everything else restricted.
	 */
	public boolean isProfessionRestrictedByRace(String professionId) throws InvalidXmlElementException {
		final Race race = this.getRace();
		if (race == null) {
			return false;
		}
		if (!race.getExcludedProfessionIds().isEmpty()) {
			for (final String token : concat(race.getRestrictedProfessionIds(), race.getExcludedProfessionIds())) {
				if (this.professionMatchesReference(professionId, token)) {
					return false;
				}
			}
			return true;
		}
		for (final String token : race.getRestrictedProfessionIds()) {
			if (this.professionMatchesReference(professionId, token)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Every profession id the selected race allows (every profession {@link RulesCatalog} knows about,
	 * minus {@link #isProfessionRestrictedByRace(String)}), or every known profession if no race is
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
	 * Whether {@code cultureId} matches reference {@code token} from a race's "CULTURAS DISPONIBLES"
	 * section: {@code "all"} matches every culture; a plain token matches by id; a {@code "group:"}
	 * token matches every culture whose id contains it (case-insensitively), matching the legacy
	 * {@code CultureFactory#getAvailableCulturesSubString(String)}.
	 */
	private boolean cultureMatchesReference(String cultureId, String token) {
		if (token.startsWith(GROUP_TOKEN_PREFIX)) {
			return cultureId.toLowerCase().contains(token.substring(GROUP_TOKEN_PREFIX.length()).toLowerCase());
		}
		return cultureId.equals(token);
	}

	/**
	 * Whether the selected race allows culture {@code cultureId} (its "CULTURAS DISPONIBLES" section,
	 * {@link Race#getCultureIds()}), or {@code false} if no race is selected. Once {@link
	 * Race#getExcludedCultureIds()} is non-empty the section's meaning flips (matching the legacy
	 * per-file "exception" flag): the union of {@link Race#getCultureIds()} and {@link
	 * Race#getExcludedCultureIds()} becomes the cultures <em>excluded</em> from "every culture is
	 * available", instead of being the only ones available.
	 */
	public boolean isCultureAvailableForRace(String cultureId) throws InvalidXmlElementException {
		final Race race = this.getRace();
		if (race == null) {
			return false;
		}
		if (!race.getExcludedCultureIds().isEmpty()) {
			for (final String token : concat(race.getCultureIds(), race.getExcludedCultureIds())) {
				if (this.cultureMatchesReference(cultureId, token)) {
					return false;
				}
			}
			return true;
		}
		for (final String token : race.getCultureIds()) {
			if ("all".equals(token) || this.cultureMatchesReference(cultureId, token)) {
				return true;
			}
		}
		return false;
	}

	private static List<String> concat(List<String> first, List<String> second) {
		final List<String> combined = new ArrayList<>(first);
		combined.addAll(second);
		return combined;
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
			if (this.isCultureAvailableForRace(culture.getId())) {
				ids.add(culture.getId());
			}
		}
		return ids;
	}

	private static final String PROFESSION_SKILL_CHOICE_KEY_PREFIX = "profession:";

	/** Section names used to namespace {@link #applyProfessionSkillGrant} decisions; see there. */
	public static final String PROFESSION_COMMON_SKILLS_SECTION = "commonSkill";
	public static final String PROFESSION_PROFESSIONAL_SKILLS_SECTION = "professionalSkill";
	public static final String PROFESSION_RESTRICTED_SKILLS_SECTION = "restrictedSkill";

	/**
	 * Resolves one of the selected profession's "choose N skills from a category/list" grants
	 * ({@link ProfessionSkillGrant}, one of {@link Profession#getCommonSkillChoices()}/{@link
	 * Profession#getProfessionalSkillChoices()}/{@link Profession#getRestrictedSkillChoices()}):
	 * validates {@code selectedSkillIds} is exactly {@link ProfessionSkillGrant#getRanksToChoose()}
	 * distinct skill ids, all from the grant's pool (every skill of {@link
	 * ProfessionSkillGrant#getCategoryId()} if set, otherwise {@link
	 * ProfessionSkillGrant#getSkillOptions()}), and records the choice; a skill this resolves
	 * classifies the same way as one of {@link Profession#getCommonSkillIds()}/etc. (see {@link
	 * #isSkillCommon}/{@link #isSkillRestricted}/{@link #isSkillProfessional}).
	 *
	 * @param professionId the selected profession this grant belongs to.
	 * @param section       which of the three sections {@code grant} came from: {@link
	 *                      #PROFESSION_COMMON_SKILLS_SECTION}/{@link
	 *                      #PROFESSION_PROFESSIONAL_SKILLS_SECTION}/{@link
	 *                      #PROFESSION_RESTRICTED_SKILLS_SECTION}.
	 * @param grantIndex    the grant's index within that section's list.
	 */
	public List<String> applyProfessionSkillGrant(String professionId, String section, int grantIndex,
												   ProfessionSkillGrant grant, List<String> selectedSkillIds)
			throws InvalidXmlElementException {
		final String key = PROFESSION_SKILL_CHOICE_KEY_PREFIX + professionId + ":" + section + ":" + grantIndex;
		final List<String> pool = this.getProfessionSkillGrantPool(grant);
		final Decision decision = this.decideOrReuse(key,
				() -> Decision.selectMultiple(pool, selectedSkillIds, grant.getRanksToChoose()));
		return decision.getSelectedOptions();
	}

	/** Every skill {@code grant} lets the player pick from: its category's skills, or its explicit list. */
	private List<String> getProfessionSkillGrantPool(ProfessionSkillGrant grant) throws InvalidXmlElementException {
		if (grant.getCategoryId() == null) {
			return grant.getSkillOptions();
		}
		final List<String> ids = new ArrayList<>();
		for (final Skill skill : RulesCatalog.getInstance().getSkills()) {
			if (grant.getCategoryId().equals(skill.getCategoryId())) {
				ids.add(skill.getId());
			}
		}
		return ids;
	}

	/**
	 * Whether an already-resolved {@link #applyProfessionSkillGrant} choice, from {@code section} of
	 * the selected profession, granted {@code skillId}; {@code false} if no profession is selected,
	 * it has no such grants, or none of them have been resolved yet.
	 */
	private boolean isSkillGrantedByProfessionChoice(String skillId, String section, List<ProfessionSkillGrant> grants) {
		final Profession profession = this.tryGetSelectedProfession();
		if (profession == null) {
			return false;
		}
		for (int i = 0; i < grants.size(); i++) {
			final String key = PROFESSION_SKILL_CHOICE_KEY_PREFIX + profession.getId() + ":" + section + ":" + i;
			if (this.decisions.isDecided(key) && this.decisions.get(key).getSelectedOptions().contains(skillId)) {
				return true;
			}
		}
		return false;
	}

	/** {@link #getProfession()} without the checked exception, {@code null} on failure (used where a boolean query cannot fail on a bad selection). */
	private Profession tryGetSelectedProfession() {
		try {
			return this.getProfession();
		} catch (final InvalidXmlElementException e) {
			return null;
		}
	}

	private static final String ENABLE_SKILL_KEY_PREFIX = "skill:enables:";

	/**
	 * Resolves which of {@code enablingSkillId}'s {@link Skill#getEnableSkills()} gets unlocked once
	 * it has ranks bought in it, when {@link Skill#isAllEnabled()} is {@code false} (an "or" choice,
	 * e.g. a martial arts style unlocking one of several Chi Powers): matches the legacy {@code
	 * enableSkillOption(Skill, Skill)}/{@code SkillForEnablingMustBeSelected} requirement. Not needed
	 * when {@link Skill#isAllEnabled()} is {@code true} (every listed skill unlocks at once, see
	 * {@link #isSkillEnabled(Skill)}).
	 */
	public void enableSkillOption(String enablingSkillId, String selectedEnabledSkillId) throws InvalidXmlElementException {
		final Skill enablingSkill = RulesCatalog.getInstance().getSkill(enablingSkillId);
		this.decideOrReuse(ENABLE_SKILL_KEY_PREFIX + enablingSkillId,
				() -> Decision.select(enablingSkill.getEnableSkills(), selectedEnabledSkillId));
	}

	/**
	 * Whether {@code skill} may be developed at all: either it is
	 * {@link Skill#isEnabledByDefault()}, or some other skill that lists it in its own {@link
	 * Skill#getEnableSkills()} has ranks bought in it and either {@link Skill#isAllEnabled()} (every
	 * listed skill unlocks) or {@code skill} is the one {@link #enableSkillOption} resolved for it.
	 */
	public boolean isSkillEnabled(Skill skill) throws InvalidXmlElementException {
		if (skill.isEnabledByDefault()) {
			return true;
		}
		for (final Skill candidate : RulesCatalog.getInstance().getSkills()) {
			if (!candidate.getEnableSkills().contains(skill.getId()) || this.getSkillTotalRanks(candidate.getId()) <= 0) {
				continue;
			}
			if (candidate.isAllEnabled()) {
				return true;
			}
			final String key = ENABLE_SKILL_KEY_PREFIX + candidate.getId();
			if (this.decisions.isDecided(key) && skill.getId().equals(this.decisions.getSelectedOption(key))) {
				return true;
			}
		}
		return false;
	}
}
