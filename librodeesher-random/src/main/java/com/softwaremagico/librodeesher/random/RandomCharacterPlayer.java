package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.character.SexType;
import com.softwaremagico.librodeesher.characteristic.Characteristic;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.RealmOfMagicGrant;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.training.ChoiceGroup;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingCategoryGrant;
import com.softwaremagico.librodeesher.training.TrainingSkillGrant;
import com.softwaremagico.librodeesher.training.TrainingSpecialItem;
import com.softwaremagico.librodeesher.weapon.Weapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Randomly spends a character's development points into category ranks, skill ranks and trainings,
 * and levels it up, matching the legacy {@code RandomCharacterPlayer} exactly for the development
 * core of the generator: every iteration picks random trainings through {@link TrainingProbability},
 * then random category/skill ranks through {@link CategoryProbability}/{@link SkillProbability},
 * until the available development points (or the maximum number of tries) run out.
 *
 * <p>
 * The wrapped character must already have its race, culture and profession selected (typically by a
 * caller building an archetype, or by {@code CharacterPlayer}'s own setters); this class does not
 * roll those, nor the culture ranks or the history points yet (those creation steps belong to the
 * coming generation slices). It does skip no creation step upstream of the development points: the
 * sex, the name, the spell-casting realm with the highest characteristic bonus (a deterministic
 * choice, as in the legacy {@code setMagicRealm}) and the initial temporal values of the
 * profession's preferred characteristics (the legacy {@code setCharacteristics}) are all filled in.
 * </p>
 *
 * <p>
 * Everything this class rolls goes through the seedable {@link RandomValues}, so two runs with the
 * same {@link RandomValues#setRandomSeed(long) seed} produce the exact same character (per-level
 * ranks and trainings, weapon-cost tiers, remaining development points, equipment, name and
 * characteristics), which the accompanying test asserts. The one rule-driven exception is the
 * training characteristic-upgrade roll (legacy {@code TrainingProbability#setRandomCharacteristicsUpgrades}),
 * which is applied by the rules side as an ordinary 2d10 roll that is not seedable yet; it is
 * therefore deferred to a dedicated commit along with the remaining creation steps (culture ranks,
 * apprenticeship/background points and perks).
 * </p>
 */
public class RandomCharacterPlayer {

    /** Maximum number of spend/roll attempts per level before the generator gives up. */
    public static final int MAX_TRIES = 5;

    /** Categories whose next rank costs more than this are never rolled into. */
    private static final int MAX_RANDOM_COST = 20;

    private final CharacterPlayer characterPlayer;
    private final int finalLevel;
    private int specializationLevel;
    private final Map<String, Integer> suggestedSkillsRanks = new HashMap<>();
    private final Map<String, Integer> suggestedCategoriesRanks = new HashMap<>();
    private final List<String> suggestedTrainings = new ArrayList<>();
    private final List<String> suggestedPerks = new ArrayList<>();

    /**
     * Wraps {@code characterPlayer} to spend its currently available development points (the level
     * it is on right now).
     */
    public RandomCharacterPlayer(CharacterPlayer characterPlayer) {
        this(characterPlayer, characterPlayer.getLevels().size());
    }

    /**
     * Wraps {@code characterPlayer} and builds it up to {@code finalLevel} (levels beyond the
     * current one are appended through {@link CharacterPlayer#increaseLevel()}).
     *
     * @param finalLevel the level to generate the character up to; must be at least the character's
     *                   current number of levels.
     */
    public RandomCharacterPlayer(CharacterPlayer characterPlayer, int finalLevel) {
        this.characterPlayer = characterPlayer;
        this.finalLevel = finalLevel;
    }

    /** The character this generator fills in. */
    public CharacterPlayer getCharacterPlayer() {
        return characterPlayer;
    }

    /**
     * The specialization level in the range [-3..3] (0 = fully random, 3 = specialize up to skills,
     * -3 = generalize): recycled by {@link CategoryProbability}/{@link SkillProbability},
     * {@link #sortSkillsBySpecialization} and the per-training category selection.
     */
    public void setSpecializationLevel(Integer specializationLevel) {
        this.specializationLevel = specializationLevel == null ? 0 : specializationLevel;
    }

    public int getSpecializationLevel() {
        return specializationLevel;
    }

    /**
     * Sets the desired total ranks for {@code categoryId} (a value {@code <= 0} forgets the
     * suggestion), recycled by {@link CategoryProbability#rankProbability()} instead of the
     * generic heuristics.
     */
    public void setSuggestedCategoryRanks(String categoryId, int ranks) {
        if (ranks <= 0) {
            suggestedCategoriesRanks.remove(categoryId);
        } else {
            suggestedCategoriesRanks.put(categoryId, ranks);
        }
    }

    public Map<String, Integer> getSuggestedCategoriesRanks() {
        return suggestedCategoriesRanks;
    }

    /**
     * Sets the desired total ranks for {@code skillId} (a value {@code <= 0} forgets the
     * suggestion), recycled by {@link SkillProbability#getRankProbability()} instead of the generic
     * heuristics. Keep in mind only the skill map is actually consumed by the development-core
     * roll; the category map is reserved for the upcoming creation slices.
     */
    public void setSuggestedSkillRanks(String skillId, int ranks) {
        if (ranks <= 0) {
            suggestedSkillsRanks.remove(skillId);
        } else {
            suggestedSkillsRanks.put(skillId, ranks);
        }
    }

    public Map<String, Integer> getSuggestedSkillsRanks() {
        return suggestedSkillsRanks;
    }

    public void setSuggestedTrainings(List<String> suggestedTrainings) {
        this.suggestedTrainings.clear();
        if (suggestedTrainings != null) {
            this.suggestedTrainings.addAll(suggestedTrainings);
        }
    }

    public List<String> getSuggestedTrainings() {
        return suggestedTrainings;
    }

    /**
     * Reserved for the upcoming perk slice: the perk ids to grant first, in that order.
     */
    public void setSuggestedPerks(List<String> suggestedPerks) {
        this.suggestedPerks.clear();
        if (suggestedPerks != null) {
            this.suggestedPerks.addAll(suggestedPerks);
        }
    }

    public List<String> getSuggestedPerks() {
        return suggestedPerks;
    }

    /**
     * Builds the wrapped character up to {@code finalLevel}: assigns the profession's weapon-cost
     * tiers first (if not decided yet), fills in the creation steps upstream of the development
     * points (sex, name, magic realm and initial temporal characteristic values), then spends every
     * level's development points. The race, culture and profession themselves must already be
     * selected on the wrapped character.
     */
    public void createRandomValues() throws InvalidXmlElementException {
        if (!characterPlayer.isWeaponCategoryCostTierAssigned(0)) {
            setWeaponCosts(characterPlayer);
        }
        setCharacterInfo();
        setMagicRealm();
        setRandomCharacteristics(characterPlayer, specializationLevel);
        setRandomCulture();
        setDevelopmentPoints();
        setLevels();
    }

    private void setCharacterInfo() throws InvalidXmlElementException {
        final Race race = characterPlayer.getRace();
        if (race == null) {
            return;
        }
        if (characterPlayer.getSex() == null) {
            characterPlayer.setSex(RandomValues.random() < 0.5 ? SexType.MALE : SexType.FEMALE);
        }
        if (characterPlayer.getName() == null) {
            characterPlayer.setName(race.getRandomName(characterPlayer.getSex(), RandomValues.getRandom()));
        }
    }

    /**
     * Selects the spell-casting realm with the highest characteristic bonus, matching the legacy
     * {@code setMagicRealm()} exactly: for every realm grant of the selected profession, the
     * offered realm whose own characteristic sums the highest total bonus wins (ties keep the last
     * one, as in the legacy {@code >=} comparison). This is a deterministic choice, so it never
     * consumes randomness.
     */
    private void setMagicRealm() throws InvalidXmlElementException {
        final Profession profession = characterPlayer.getProfession();
        if (profession == null) {
            return;
        }
        final List<RealmOfMagicGrant> grants = profession.getMagicRealms();
        if (grants.isEmpty()) {
            return;
        }
        final Map<Integer, RealmOfMagic> realmSelections = new HashMap<>();
        for (int i = 0; i < grants.size(); i++) {
            RealmOfMagic chosenRealm = null;
            int maxCharValue = -100;
            for (final RealmOfMagic realm : grants.get(i).getOptions()) {
                final int charValue = characterPlayer.getCharacteristicTotalBonus(realm.getCharacteristic());
                if (charValue >= maxCharValue) {
                    maxCharValue = charValue;
                    chosenRealm = realm;
                }
            }
            if (chosenRealm != null) {
                realmSelections.put(i, chosenRealm);
            }
        }
        characterPlayer.applyProfessionMagicRealms(realmSelections);
    }

    /**
     * Spends the creation temporal points into the profession's preferred characteristics, matching
     * the legacy {@code setCharacteristics} heuristics exactly: preferred characteristics get a
     * higher (index-driven) raise probability and, through {@code setCharacteristicInitialTemporalValue},
     * are guaranteed to start at least at 90; the raise amount and the current-value cap follow the
     * temporal cost tables defined by {@link Characteristic#getTemporalCost}. Everything is drawn
     * from {@link RandomValues}, so the whole distribution is reproducible seed by seed. Freezes the
     * result in as confirmed (see {@link CharacterPlayer#setCharacteristicsAsConfirmed()}).
     */
    public static void setRandomCharacteristics(CharacterPlayer characterPlayer, int specializationLevel)
            throws InvalidXmlElementException {
        final Profession profession = characterPlayer.getProfession();
        if (profession == null) {
            characterPlayer.setCharacteristicsAsConfirmed();
            return;
        }
        final List<CharacteristicAbbreviation> preferences = profession.getCharacteristicPreferences();
        if (preferences.isEmpty()) {
            characterPlayer.setCharacteristicsAsConfirmed();
            return;
        }
        final List<CharacteristicAbbreviation> reversedPreferences = new ArrayList<>(preferences);
        Collections.reverse(reversedPreferences);
        int loop = 0;
        final int totalPoints = characterPlayer.getCharacteristicsTemporalTotalPoints();
        while (characterPlayer.getCharacteristicsTemporalPointsSpent() < totalPoints && loop < totalPoints / 30) {
            for (int i = 0; i < preferences.size(); i++) {
                final CharacteristicAbbreviation abbreviation = preferences.get(i);
                final int availablePoints = totalPoints - characterPlayer.getCharacteristicsTemporalPointsSpent();
                final int temporalValue = characterPlayer.getCharacteristicTemporalValue(abbreviation);
                // Max probability 90%, preferred characteristics with points.
                final boolean probability = (int) (RandomValues.random() * 100 + 1) < reversedPreferences.indexOf(abbreviation) * 4
                        + loop;
                // Temporal values have a max limit.
                final int temporalLimit = Math.min(Math.max(90 + (totalPoints - Characteristics.TOTAL_CHARACTERISTICS_POINTS) / 20,
                        90 + (specializationLevel - 1) * 6), totalPoints <= Characteristics.TOTAL_CHARACTERISTICS_POINTS ? 101
                        : 90 + totalPoints / 60);
                // Cost affordable.
                final int nextRankCost = Characteristic.getTemporalCost(temporalValue + 1) - Characteristic.getTemporalCost(temporalValue);
                if (probability && temporalValue < temporalLimit && nextRankCost <= availablePoints) {
                    // Increase more than one point depending on the value of the characteristic.
                    int valueToAdd = 1;
                    if (temporalValue < 50) {
                        valueToAdd = Math.min(20 + (specializationLevel + 3) * 5, availablePoints);
                    } else if (temporalValue < 70) {
                        valueToAdd = Math.min(7 + (specializationLevel + 3) * 4, availablePoints);
                    } else if (temporalValue < 83) {
                        valueToAdd = Math.min(2 + (specializationLevel + 3), availablePoints);
                    } else if (temporalValue < 90 - specializationLevel) {
                        valueToAdd = Math.max(1, specializationLevel);
                    }
                    characterPlayer.setCharacteristicInitialTemporalValue(abbreviation, temporalValue + valueToAdd);
                    // Add new points to the same characteristic.
                    if (specializationLevel > 0) {
                        i--;
                    }
                }
            }
            loop++;
        }
        characterPlayer.setCharacteristicsAsConfirmed();
    }

    /**
     * Rolls the currently selected culture's own creation ranks, mirroring the legacy {@code
     * setCulture}: the adolescence category grants (through the same grant machinery a training uses,
     * {@link #applyCultureAdolescenceRanks} on the rules side), with the languages, hobbies and
     * culture spell ranks still pending in the next slice. Cultures without adolescence grants (or
     * characters without a culture selected) are left untouched.
     */
    private void setRandomCulture() throws InvalidXmlElementException {
        final Culture culture = characterPlayer.getCulture();
        if (culture == null) {
            return;
        }
        // The rules side expands the "weapon"/"armor" markers of the migrated data into the
        // culture's typical weapons/armors (see {@code CharacterPlayer#getResolvedAdolescenceRanks}),
        // both here for the cost estimate and in {@code applyCultureAdolescenceRanks}; both sides
        // must see the very same concrete options for the estimate to stay exact.
        final List<TrainingCategoryGrant> grants = characterPlayer.getResolvedAdolescenceRanks(culture);
        if (grants.isEmpty()) {
            return;
        }
        // The adolescence ranks count against the current level's development budget like any other
        // grant (see {@code applyCultureAdolescenceRanks}), so they are applied only when affordable.
        final GrantPicks picks = buildGrantPicks(characterPlayer, grants);
        if (characterPlayer.getRemainingDevelopmentPoints() - picks.estimatedGrantCost < 0) {
            return;
        }
        characterPlayer.applyCultureAdolescenceRanks(culture, picks.categorySelections, picks.skillSelections,
                picks.additionalSkillRanks);
    }

    /**
     * Spends the character's current available development points, repeating the "pick trainings,
     * then pick category/skill ranks" loop up to {@link #MAX_TRIES} times until the budget runs
     * out. Probabilities are cached between tries so previously rolled categories/skills are not
     * re-rolled unless something changed their state.
     */
    public void setDevelopmentPoints() throws InvalidXmlElementException {
        final Map<String, Integer> categoryProbabilityStored = new HashMap<>();
        final Map<String, Integer> skillProbabilityStored = new HashMap<>();
        int tries = 0;
        while (characterPlayer.getRemainingDevelopmentPoints() > 0 && tries <= MAX_TRIES) {
            getRandomTrainings(characterPlayer, specializationLevel, suggestedTrainings, finalLevel);
            setRandomRanks(characterPlayer, specializationLevel, suggestedSkillsRanks, tries, finalLevel,
                    categoryProbabilityStored, skillProbabilityStored);
            tries++;
        }
    }

    private void setLevels() throws InvalidXmlElementException {
        while (characterPlayer.getLevels().size() < finalLevel) {
            characterPlayer.increaseLevel();
            setDevelopmentPoints();
        }
    }

    /**
     * Randomly grabs every currently affordable training whose {@link
     * TrainingProbability#trainingRandomness} roll succeeds, applying each through {@link
     * #setRandomTraining}.
     */
    public static void getRandomTrainings(CharacterPlayer characterPlayer, int specializationLevel,
            List<String> suggestedTrainings, int finalLevel) throws InvalidXmlElementException {
        final List<String> trainings = TrainingProbability.shuffleTrainings(characterPlayer, suggestedTrainings);
        for (final String trainingId : trainings) {
            try {
                final int probability = TrainingProbability.trainingRandomness(characterPlayer, trainingId,
                        specializationLevel, suggestedTrainings, finalLevel);
                if (RandomValues.random() * 100 < probability) {
                    setRandomTraining(characterPlayer, trainingId, specializationLevel);
                }
            } catch (InvalidXmlElementException e) {
                // An unknown or currently-unavailable training is skipped.
            }
        }
    }

    /**
     * Selects {@code trainingId} (when currently available) and randomly resolves every decision it
     * needs: its category grants (category alternative, named skill alternatives and freely
     * distributed skill ranks), its life/common/professional/restricted skill choices, and each of
     * its special background items (rolled against the item's own probability, the same "accepted"
     * discount the legacy applied). The training characteristic upgrade rolls are deliberately not
     * applied yet: they are a rules-side 2d10 roll that is not seedable, and are scheduled for the
     * dedicated roll port (see class javadoc).
     */
    public static void setRandomTraining(CharacterPlayer characterPlayer, String trainingId, int specializationLevel)
            throws InvalidXmlElementException {
        if (!characterPlayer.addTraining(trainingId)) {
            return;
        }
        final Training training = RulesCatalog.getInstance().getTraining(trainingId);
        if (training == null) {
            return;
        }
        final List<TrainingCategoryGrant> grants = training.getCategories();
        if (grants != null && !grants.isEmpty()) {
            // The granted category/skill ranks count against the current level's development budget
            // exactly like every other rank (see {@code CharacterPlayer#getSpentDevelopmentPoints}),
            // so a training whose grants would drive the budget below zero is rolled back untouched:
            // its own cost alone is never enough to tell.
            final GrantPicks picks = buildGrantPicks(characterPlayer, grants);
            if (characterPlayer.getRemainingDevelopmentPoints() - picks.estimatedGrantCost < 0) {
                characterPlayer.removeCurrentLevelTraining(trainingId);
                return;
            }
            characterPlayer.applyTrainingCategories(training, picks.categorySelections, picks.skillSelections,
                    picks.additionalSkillRanks);
        }
        addRandomTrainingSkillChoices(characterPlayer, training);
        addRandomTrainingSpecialItems(characterPlayer, training);
    }

    private static final class GrantPicks {
        private final Map<Integer, String> categorySelections = new HashMap<>();
        private final Map<Integer, List<String>> skillSelections = new HashMap<>();
        private final Map<Integer, Map<String, Integer>> additionalSkillRanks = new HashMap<>();
        private int estimatedGrantCost;
    }

    // Computes the decision picks together with the exact development cost the granted ranks will
    // consume once applied, mirroring {@code CharacterPlayer#getSpentDevelopmentPoints} rank-per-rank.
    private static GrantPicks buildGrantPicks(CharacterPlayer characterPlayer, List<TrainingCategoryGrant> grants)
            throws InvalidXmlElementException {
        final GrantPicks picks = new GrantPicks();
        // Projected per-category and per-skill rank counts for the cost tables (mirroring how
        // {@code getSpentDevelopmentPoints} indexes them rank-per-rank; the character itself is not
        // mutated until {@code applyTrainingCategories} runs, which the estimate mirrors exactly).
        final Map<String, Integer> categoryRanks = new HashMap<>(characterPlayer.getCurrentLevel().getCategoryRanks());
        final Map<String, Integer> skillRanks = new HashMap<>(characterPlayer.getCurrentLevel().getSkillRanks());
        for (int i = 0; i < grants.size(); i++) {
            final TrainingCategoryGrant grant = grants.get(i);
            final List<String> offeredCategories = characterPlayer.expandCategoryWildcards(grant.getCategoryOptions());
            if (offeredCategories.isEmpty()) {
                continue;
            }
            // Choosing between several options happens whenever the grant expands to more than one
            // real category (a wildcard like "allWeaponCategories" expands into every weapon
            // category), not only when the source XML declared an explicit choice: {@code
            // applyCategoryGrant} decides exactly the same way.
            final String categoryId;
            if (offeredCategories.size() > 1) {
                categoryId = offeredCategories.get(RandomValues.nextInt(offeredCategories.size()));
                picks.categorySelections.put(i, categoryId);
            } else {
                categoryId = offeredCategories.get(0);
            }

            final int ranksGranted = grant.getRanksGranted() == null ? 0 : grant.getRanksGranted();
            if (ranksGranted > 0) {
                int start = categoryRanks.getOrDefault(categoryId, 0);
                for (int rank = start; rank < start + ranksGranted; rank++) {
                    final Integer cost = characterPlayer.getCategoryDevelopmentCost(categoryId, rank);
                    if (cost != null) {
                        picks.estimatedGrantCost += cost;
                    }
                }
                categoryRanks.put(categoryId, start + ranksGranted);
            }

            final Set<String> namedSkillIds = new HashSet<>();
            final List<String> selectedSkills = new ArrayList<>();
            final List<TrainingSkillGrant> skillGrants = grant.getSkills();
            if (skillGrants != null) {
                for (final TrainingSkillGrant skillGrant : skillGrants) {
                    // The migrated data occasionally still references a skill id the catalog does
                    // not know (e.g. "instintoUrban" instead of "instintoUrbano"): those options
                    // are dropped, so no grant ever ranks an unresolvable skill.
                    final List<String> options = new ArrayList<>();
                    for (final String option : skillGrant.getSkillOptions()) {
                        if (isResolvableSkill(option)) {
                            options.add(option);
                        }
                    }
                    if (options.isEmpty()) {
                        options.addAll(skillGrant.getSkillOptions());
                    }
                    final String picked = options.get(RandomValues.nextInt(options.size()));
                    selectedSkills.add(picked);
                    namedSkillIds.add(picked);
                    final int namedRanks = skillGrant.getRanksToDistribute() == null ? 0
                            : skillGrant.getRanksToDistribute();
                    if (namedRanks > 0) {
                        estimateRankCost(characterPlayer, skillRanks, picked, namedRanks, picks);
                    }
                }
            }
            if (!selectedSkills.isEmpty()) {
                picks.skillSelections.put(i, selectedSkills);
            }

            final int ranksToDistribute = grant.getRanksToDistribute() == null ? 0 : grant.getRanksToDistribute();
            final int namedRanks = grantRanksOf(skillGrants);
            final int remainingRanks = ranksToDistribute - namedRanks;
            if (remainingRanks > 0) {
                final int minSkills = grant.getMinSkills() == null ? 0 : grant.getMinSkills();
                final int maxSkills = grant.getMaxSkills() == null ? 0 : grant.getMaxSkills();
                final int minAdditional = Math.max(0, minSkills - namedSkillIds.size());
                final int maxAdditional = maxSkills - namedSkillIds.size();
                final List<String> categorySkills = categorySkillIds(characterPlayer, categoryId);
                final List<String> pool = new ArrayList<>();
                for (final String skillId : categorySkills) {
                    if (!namedSkillIds.contains(skillId) && isResolvableSkill(skillId)) {
                        pool.add(skillId);
                    }
                }
                if (minAdditional <= pool.size() && minAdditional <= maxAdditional) {
                    final int limit = Math.min(maxAdditional, pool.size());
                    // At least one skill takes the leftover ranks when none is required.
                    final int low = Math.max(1, minAdditional);
                    if (low <= limit) {
                        final int count = low + RandomValues.nextInt(limit - low + 1);
                        final List<String> picked = RandomValues.shuffle(pool).subList(0, count);
                        picks.additionalSkillRanks.put(i, distributeRanks(picked, remainingRanks));
                        for (final Map.Entry<String, Integer> entry : picks.additionalSkillRanks.get(i).entrySet()) {
                            estimateRankCost(characterPlayer, skillRanks, entry.getKey(), entry.getValue(), picks);
                        }
                    }
                }
            }
        }
        return picks;
    }

    private static int grantRanksOf(List<TrainingSkillGrant> skillGrants) {
        int namedRanks = 0;
        if (skillGrants != null) {
            for (final TrainingSkillGrant skillGrant : skillGrants) {
                namedRanks += skillGrant.getRanksToDistribute() == null ? 0 : skillGrant.getRanksToDistribute();
            }
        }
        return namedRanks;
    }

    private static void estimateRankCost(CharacterPlayer characterPlayer, Map<String, Integer> skillRanks,
            String skillId, int ranksToAdd, GrantPicks picks) throws InvalidXmlElementException {
        final String categoryId = RulesCatalog.getInstance().getSkill(skillId).getCategoryId();
        int start = skillRanks.getOrDefault(skillId, 0);
        for (int rank = start; rank < start + ranksToAdd; rank++) {
            final Integer cost = characterPlayer.getCategoryDevelopmentCost(categoryId, rank);
            if (cost != null) {
                picks.estimatedGrantCost += cost;
            }
        }
        skillRanks.put(skillId, start + ranksToAdd);
    }

    private static void addRandomTrainingSkillChoices(CharacterPlayer characterPlayer, Training training) {
        characterPlayer.applyTrainingSkillChoices(training, randomSkillChoiceSelections(training.getLifeSkills()),
                randomSkillChoiceSelections(training.getCommonSkills()),
                randomSkillChoiceSelections(training.getProfessionalSkills()),
                randomSkillChoiceSelections(training.getRestrictedSkills()));
    }

    private static Map<Integer, String> randomSkillChoiceSelections(List<ChoiceGroup> groups) {
        final Map<Integer, String> selections = new HashMap<>();
        if (groups == null) {
            return selections;
        }
        for (int i = 0; i < groups.size(); i++) {
            final List<String> options = groups.get(i).getOptions();
            if (options.isEmpty()) {
                continue;
            }
            selections.put(i, options.get(RandomValues.nextInt(options.size())));
        }
        return selections;
    }

    private static void addRandomTrainingSpecialItems(CharacterPlayer characterPlayer, Training training) {
        final List<TrainingSpecialItem> items = training.getSpecialItems();
        if (items == null) {
            return;
        }
        int accepted = 1;
        for (int i = 0; i < items.size(); i++) {
            final TrainingSpecialItem item = items.get(i);
            final int probability = item.getProbability() == null ? 100 : item.getProbability();
            if (RandomValues.nextInt(100) < Math.max(0, probability / Math.max(1, accepted))) {
                characterPlayer.applyTrainingSpecialItem(training, i);
                accepted++;
            }
        }
    }

    /** Whether {@code skillId} resolves in the current {@link RulesCatalog}. */
    private static boolean isResolvableSkill(String skillId) {
        try {
            RulesCatalog.getInstance().getSkill(skillId);
            return true;
        } catch (final InvalidXmlElementException e) {
            return false;
        }
    }

    /** The {@code Skill} ids belonging to {@code categoryId} (dynamically-derived for weapon categories). */
    private static List<String> categorySkillIds(CharacterPlayer characterPlayer, String categoryId)
            throws InvalidXmlElementException {
        final Category category = RulesCatalog.getInstance().getCategory(categoryId);
        if (category.hasDynamicSkills()) {
            final List<String> ids = new ArrayList<>();
            for (final Weapon weapon : RulesCatalog.getInstance().getWeapons()) {
                if (categoryId.equals(weapon.getCategoryId())) {
                    ids.add(weapon.getId());
                }
            }
            return ids;
        }
        return category.getSkills();
    }

    /** Spreads {@code totalRanks} over {@code skills} (at least one rank each), randomly but evenly. */
    private static Map<String, Integer> distributeRanks(List<String> skills, int totalRanks) {
        final Map<String, Integer> ranks = new HashMap<>();
        int remaining = totalRanks;
        for (final String skillId : skills) {
            ranks.put(skillId, 1);
            remaining--;
        }
        while (remaining > 0) {
            final String skillId = skills.get(RandomValues.nextInt(skills.size()));
            ranks.put(skillId, ranks.get(skillId) + 1);
            remaining--;
        }
        return ranks;
    }

    /**
     * Randomly spends development points into category and skill ranks, matching the legacy {@code
     * RandomCharacterPlayer#setRandomRanks} exactly: categories are visited cheapest-first, a
     * category rank is added when {@link CategoryProbability#rankProbability()} rolls high enough
     * (plus a "tries" concession), then each of the category's skills is visited in specialization
     * order with the same rule ({@link SkillProbability#getRankProbability()}). With
     * {@code specializationLevel} {@code > 2} a just-ranked skill may additionally get one of its
     * specialities; with {@code specializationLevel} {@code < -2} it may just as well be
     * generalized.
     *
     * <p>
     * The legacy automatically unlocked a prerequisite skill whenever a rank was tried on a
     * disabled skill; the same happens here through {@link CharacterPlayer#enableSkillOption(String,
     * String)} (choosing the disabled skill itself among its enabling skill's options, mirroring the
     * legacy's random pick) before the rank is attempted again.
     * </p>
     *
     * @param suggestedSkillsRanks desired total ranks per skill id, or {@code null}.
     * @param tries the current spend attempt (0-based), shared across levels.
     * @param finalLevel the level the character is being generated up to.
     */
    public static void setRandomRanks(CharacterPlayer characterPlayer, int specializationLevel,
            Map<String, Integer> suggestedSkillsRanks, Integer tries, int finalLevel,
            Map<String, Integer> categoryProbabilityStored, Map<String, Integer> skillProbabilityStored)
            throws InvalidXmlElementException {
        final List<Category> sortedCategoriesByCost = new ArrayList<>(RulesCatalog.getInstance().getCategories());
        sortCategoriesByCost(characterPlayer, sortedCategoriesByCost);
        int developmentPoints = characterPlayer.getRemainingDevelopmentPoints();
        for (final Category category : sortedCategoriesByCost) {
            if (developmentPoints < 1) {
                break;
            }
            final Integer newRankCost = characterPlayer.getCategoryDevelopmentCost(category.getId(),
                    characterPlayer.getCurrentLevelRanks(category));
            if (newRankCost == null || newRankCost > developmentPoints || newRankCost > MAX_RANDOM_COST
                    || !characterPlayer.isCategoryInteresting(category)) {
                continue;
            }

            if (categoryProbabilityStored.get(category.getId()) == null) {
                categoryProbabilityStored.put(category.getId(), new CategoryProbability(characterPlayer, category,
                        suggestedSkillsRanks, specializationLevel, finalLevel).rankProbability());
            }
            final int roll = (int) (RandomValues.random() * 100 + 1);
            final int probability = categoryProbabilityStored.get(category.getId());
            if (probability > 0 && roll < probability + tries * 3) {
                final int currentRanks = characterPlayer.getCurrentLevel().getCategoryRanks(category.getId());
                if (characterPlayer.setCurrentLevelCategoryRanks(category.getId(), currentRanks + 1)) {
                    developmentPoints = characterPlayer.getRemainingDevelopmentPoints();
                    categoryProbabilityStored.remove(category.getId());
                    for (final String skillId : category.getSkills()) {
                        skillProbabilityStored.remove(skillId);
                    }
                }
            }

            final List<Skill> shuffledSkills = new ArrayList<>();
            for (final String skillId : sortSkillsBySpecialization(characterPlayer, category.getSkills(),
                    specializationLevel)) {
                try {
                    shuffledSkills.add(RulesCatalog.getInstance().getSkill(skillId));
                } catch (final InvalidXmlElementException e) {
                    // An id the catalog cannot resolve (legacy data leftovers) is skipped.
                }
            }
            for (int j = 0; j < shuffledSkills.size(); j++) {
                if (developmentPoints < 1) {
                    break;
                }
                final Skill skill = shuffledSkills.get(j);
                final Integer rankCost = characterPlayer.getSkillDevelopmentCost(skill.getId(),
                        characterPlayer.getCurrentLevel().getSkillRanks(skill.getId()));
                if (rankCost == null || rankCost > developmentPoints) {
                    break;
                }
                if (!characterPlayer.isSkillEnabled(skill) && !enableSkill(characterPlayer, skill)) {
                    continue;
                }
                if (characterPlayer.isSkillDisabledByOptions(skill)) {
                    continue;
                }

                if (skillProbabilityStored.get(skill.getId()) == null) {
                    skillProbabilityStored.put(skill.getId(), new SkillProbability(characterPlayer, skill,
                            suggestedSkillsRanks, specializationLevel, finalLevel).getRankProbability());
                }
                final int roll2 = (int) (RandomValues.random() * 100 + 1);
                final int skillProbability = skillProbabilityStored.get(skill.getId());
                if (skillProbability > 0 && roll2 < skillProbability + tries * 3 || tries == MAX_TRIES) {
                    final int currentSkillRanks = characterPlayer.getCurrentLevel().getSkillRanks(skill.getId());
                    if (characterPlayer.setCurrentLevelSkillRanks(skill.getId(), currentSkillRanks + 1)) {
                        for (final String skillToRemove : category.getSkills()) {
                            skillProbabilityStored.remove(skillToRemove);
                        }
                        developmentPoints = characterPlayer.getRemainingDevelopmentPoints();

                        if (specializationLevel > 0) {
                            j--;
                        }

                        if (specializationLevel > 2) {
                            if (!characterPlayer.isSkillRestricted(skill)
                                    && !characterPlayer.isSkillGeneralized(skill.getId())) {
                                for (final String speciality : skill.getSpecialities()) {
                                    if (RandomValues.random() * 100 < (specializationLevel - 2) * 3) {
                                        final Integer specialityCost = characterPlayer
                                                .getSkillDevelopmentCost(skill.getId(),
                                                        characterPlayer.getCurrentLevel().getSkillRanks(skill.getId()));
                                        if (specialityCost != null && specialityCost < developmentPoints) {
                                            characterPlayer.addSkillSpecialization(skill.getId(), speciality);
                                            developmentPoints = characterPlayer.getRemainingDevelopmentPoints();
                                            break;
                                        }
                                    }
                                }
                            }
                        } else if (specializationLevel < -2) {
                            if (!characterPlayer.isSkillRestricted(skill)
                                    && characterPlayer.getSkillSpecializations(skill.getId()).isEmpty()) {
                                if (RandomValues.random() * 100 < (-specializationLevel - 2) * 5) {
                                    characterPlayer.generalizeSkill(skill.getId());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Attempts to unlock {@code skill} through a currently-ranked enabling skill; see {@link #setRandomRanks}. */
    private static boolean enableSkill(CharacterPlayer characterPlayer, Skill skill) throws InvalidXmlElementException {
        for (final Skill candidate : RulesCatalog.getInstance().getSkills()) {
            if (!candidate.getEnableSkills().contains(skill.getId())) {
                continue;
            }
            final Integer candidateRanks = characterPlayer.getSkillTotalRanks(candidate.getId());
            if (candidateRanks == null || candidateRanks <= 0) {
                continue;
            }
            if (candidate.isAllEnabled()) {
                return true;
            }
            characterPlayer.enableSkillOption(candidate.getId(), skill.getId());
            return true;
        }
        return false;
    }

    /**
     * Sorts {@code categories} the way the legacy {@code CategoryComparatorByCost} did: by the next
     * rank cost (spell-list categories discounted, further discounted for casters profiting from a
     * single or a hybrid realm), so cheap categories are visited first.
     */
    private static void sortCategoriesByCost(CharacterPlayer characterPlayer, List<Category> categories)
            throws InvalidXmlElementException {
        Collections.sort(categories, (category1, category2) -> {
            try {
                return Integer.compare(costOfNextRank(characterPlayer, category1),
                        costOfNextRank(characterPlayer, category2));
            } catch (final InvalidXmlElementException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static int costOfNextRank(CharacterPlayer characterPlayer, Category category)
            throws InvalidXmlElementException {
        final Integer cost = characterPlayer.getCategoryDevelopmentCost(category.getId(),
                characterPlayer.getCurrentLevelRanks(category));
        if (cost == null) {
            return Integer.MAX_VALUE;
        }
        if (isSpellCategory(category.getId())) {
            int value = cost - 3;
            final int realms = characterPlayer.getRealmsOfMagic().size();
            if (realms == 1) {
                value -= 2;
            } else if (realms > 1) {
                value -= 4;
            }
            return value;
        }
        return cost;
    }

    private static boolean isSpellCategory(String categoryId) {
        try {
            return RulesCatalog.getInstance().getSpellList(categoryId) != null;
        } catch (InvalidXmlElementException e) {
            return false;
        }
    }

    private static List<String> sortSkillsBySpecialization(CharacterPlayer characterPlayer, List<String> skills,
            int specializationLevel) throws InvalidXmlElementException {
        final List<String> sorted = new ArrayList<>(skills);
        if (specializationLevel == -1) {
            return RandomValues.shuffle(sorted);
        }
        final List<String> shuffled = RandomValues.shuffle(sorted);
        shuffled.sort((skillId1, skillId2) -> {
            final Integer ranks1 = characterPlayer.getSkillTotalRanks(skillId1);
            final Integer ranks2 = characterPlayer.getSkillTotalRanks(skillId2);
            return Integer.compare(ranks1 == null ? 0 : ranks1, ranks2 == null ? 0 : ranks2);
        });
        if (specializationLevel < 0) {
            Collections.reverse(shuffled);
        }
        return shuffled;
    }

    /**
     * Assigns the profession's weapon-cost tiers (cheapest first, see {@link
     * CharacterPlayer#assignWeaponCategoryCostTier(int, String)}) to a shuffled list of weapon
     * categories holding the legacy ordering: first one hand-to-hand category, then one distance (or
     * firearm, when the character allows them) category, then everything else shuffled. The legacy's
     * unintentional fall-through that also collected projectile/throwing categories into the
     * firearm bucket is not reproduced.
     */
    public static void setWeaponCosts(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final int tierCount = characterPlayer.getProfession().getWeaponCategoryCostTiers().size();
        if (tierCount == 0) {
            return;
        }
        final List<String> shuffled = shuffleWeaponCategories(characterPlayer);
        for (int i = 0; i < shuffled.size() && i < tierCount; i++) {
            characterPlayer.assignWeaponCategoryCostTier(i, shuffled.get(i));
        }
    }

    private static List<String> shuffleWeaponCategories(CharacterPlayer characterPlayer) {
        final List<String> handToHand = new ArrayList<>(
                List.of("weaponsBlunt", "weaponsEdged", "weaponsPolearm", "weaponsTwoHanded"));
        final List<String> distance = new ArrayList<>(List.of("weaponsMissile", "weaponsThrown"));
        final List<String> fireArms = new ArrayList<>(
                List.of("weaponsFirearmOneHanded", "weaponsFirearmTwoHanded"));
        final List<String> others = new ArrayList<>(List.of("weaponsSiege"));

        final List<String> shuffledWeapons = new ArrayList<>();
        shuffledWeapons.add(RandomValues.shuffle(handToHand).get(0));
        if (!characterPlayer.isFirearmsAllowed()) {
            shuffledWeapons.add(RandomValues.shuffle(distance).get(0));
        } else {
            shuffledWeapons.add(RandomValues.shuffle(fireArms).get(0));
        }
        handToHand.remove(shuffledWeapons.get(0));
        if (!characterPlayer.isFirearmsAllowed()) {
            distance.remove(shuffledWeapons.get(1));
        } else {
            fireArms.remove(shuffledWeapons.get(1));
        }
        others.addAll(handToHand);
        others.addAll(distance);
        if (characterPlayer.isFirearmsAllowed()) {
            others.addAll(fireArms);
        }
        shuffledWeapons.addAll(RandomValues.shuffle(others));
        return shuffledWeapons;
    }
}