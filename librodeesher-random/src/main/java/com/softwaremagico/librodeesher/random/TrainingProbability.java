package com.softwaremagico.librodeesher.random;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.ElementalTriad;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingRequirement;

import java.util.ArrayList;
import java.util.List;

/**
 * How likely the random character generator is to pick a training right now, replicating the legacy
 * {@code pj.random.TrainingProbability} heuristics on top of the id-based NG model ({@code
 * Training#getId()} replaces the legacy trainings keyed by Spanish name, so the suggested list is
 * keyed by raw ids instead of display names).
 *
 * <p>Id-based adaptations with respect to the legacy class (all documented on their call sites):
 *
 * <ul>
 *   <li>A training the character's profession neither lists nor the training itself prices for that
 *       profession reports {@code null} here (see {@code CharacterPlayer#getTrainingDevelopmentCost});
 *       the legacy model fell back to its {@code INVALID_COST} sentinel (200), which is also always
 *       over the dev-point budget, so both models treat it as unaffordable and score it 0.</li>
 *   <li>{@code CharacterPlayer#getSelectedTrainingIds()}/{@code getCurrentLevel().getTrainings()}
 *       replace the legacy {@code getSelectedTrainings()}/{@code getCurrentLevel().getTrainings()}
 *       split, and {@code getLevels().size()} the legacy {@code getLevelUps().size()}; the NG
 *       {@code LevelUp} list already includes the current level, exactly like the legacy one.</li>
 *   <li>The two legacy favourite/forbidden sources ({@code profession.getTrainingTypes()} and {@code
 *       training.getProfessionPreferences()}) are unified here into {@code
 *       CharacterPlayer#isTrainingFavouredByProfession}/{@code isTrainingForbiddenByProfession},
 *       whose profession-side-first fallback is the exact legacy precedence.</li>
 *   <li>Training requirements (legacy {@code getCharacteristicRequirements()}/{@code
 *       getSkillRequirements()}) are a single {@link TrainingRequirement} list here whose entries the
 *       migration did not resolve; each is classified by looking it up as a characteristic first,
 *       else as a skill. No shipped training has any requirement at all (every legacy "REQUISITOS
 *       PROFESIONALES" section was "Ninguno"), so this branch is untested against real data; its
 *       requirement cost discounts are likewise not applied by {@code
 *       CharacterPlayer#getTrainingDevelopmentCost}.</li>
 * </ul>
 *
 * <p>{@code setRandomCategoryRanks()}, {@code setRandomCharacteristicsUpgrades()} and {@code
 * setRandomObjects()} of the legacy class are not ported here: NG resolves a training's category and
 * skill grants through typed decisions ({@code CharacterPlayer#applyTrainingCategories}/{@code
 * applyTrainingSkillChoices}, see {@code com.softwaremagico.librodeesher.training}), and the
 * characteristic/equipment application belongs to the character generator that consumes this class,
 * mirroring how {@code PerkProbability} defers {@code selectOptions()} and {@code shufflePerks()} to
 * {@code RandomCharacterPlayer}.</p>
 */
public final class TrainingProbability {

    private TrainingProbability() {
        // Utility class.
    }

    /**
     * Every available training id, shuffled, with the generator's suggested trainings and (for an
     * elementalist profession) the elementalist trainings ordered first and the already acquired
     * trainings removed, matching the legacy {@code shuffleTrainings} ordering exactly (including
     * its quirk of reversing the suggested order while inserting them at the front). The input list
     * is left untouched.
     */
    public static List<String> shuffleTrainings(CharacterPlayer characterPlayer, List<String> suggestedTrainings)
            throws InvalidXmlElementException {
        final List<String> ids = new ArrayList<>();
        for (final Training training : RulesCatalog.getInstance().getTrainings()) {
            ids.add(training.getId());
        }
        final List<String> allTrainings = RandomValues.shuffle(ids);
        if (suggestedTrainings != null) {
            allTrainings.removeAll(suggestedTrainings);
        }

        // For elementalist, elementalist trainings first.
        final List<String> elementalistTrainings = new ArrayList<>();
        if (isElementalistProfession(characterPlayer)) {
            for (int i = 0; i < allTrainings.size(); i++) {
                final String training = allTrainings.get(i);
                if (ElementalTriad.isElementalistTraining(training)) {
                    elementalistTrainings.add(training);
                    allTrainings.remove(i);
                    i--;
                }
            }
        }

        // Add preferences to the beginning.
        for (int j = 0; j < elementalistTrainings.size(); j++) {
            allTrainings.add(0, elementalistTrainings.get(j));
        }
        if (suggestedTrainings != null) {
            for (final String training : suggestedTrainings) {
                allTrainings.add(0, training);
            }
        }
        // Remove already acquired trainings.
        allTrainings.removeAll(characterPlayer.getSelectedTrainingIds());

        return allTrainings;
    }

    /**
     * Probability of a random generator picking {@code trainingId} as its next training, matching the
     * legacy {@code trainingRandomness} exactly: unaffordable/unknown trainings and unmet
     * characteristic or skill requirements score 0 first, a suggested and affordable training (while
     * the current level is still empty, or while more trainings remain than levels left) short
     * circuits to 100, then the base formula {@code (28 - cost) * 1.5 + levels - (selected +
     * specialization) * 25}, the profession/culture favourites, the "at least one per ten levels"
     * floor, the forbidden veto and the elementalist's first-training requirement, divided by
     * {@code currentLevelTrainings + 1}.
     *
     * @param suggestedTrainings training ids the generator is instructed to pick this level (legacy
     *                           suggested-training names, lifted from a pre-generated character),
     *                           {@code null} when no target profile applies; the caller's list is
     *                           left untouched.
     */
    public static int trainingRandomness(CharacterPlayer characterPlayer, String trainingId, int specialization,
            List<String> suggestedTrainings, int finalLevel) throws InvalidXmlElementException {
        final Integer cost = characterPlayer.getTrainingDevelopmentCost(trainingId);
        // No too expensive take, and no take of a training the profession does not price at all.
        if (cost == null || cost > characterPlayer.getRemainingDevelopmentPoints()) {
            return 0;
        }

        // Has not the characteristics requirements.
        for (final TrainingRequirement requirement : RulesCatalog.getInstance().getTraining(trainingId).getRequirements()) {
            final CharacteristicAbbreviation characteristic = CharacteristicAbbreviation.fromTag(requirement.getName());
            if (characteristic != CharacteristicAbbreviation.NONE) {
                if (requirement.getValue() > characterPlayer.getCharacteristicTemporalValue(characteristic)) {
                    return 0;
                }
            } else {
                // Not a characteristic: a skill requirement (or an unknown target, which fails closed).
                final Skill skill;
                try {
                    skill = RulesCatalog.getInstance().getSkill(requirement.getName());
                } catch (final InvalidXmlElementException e) {
                    return 0;
                }
                if (requirement.getValue() > characterPlayer.getSkillRealRanks(skill)) {
                    return 0;
                }
            }
        }

        // Suggested training.
        final List<String> remainingSuggestions = suggestedTrainings == null ? null
                : new ArrayList<>(suggestedTrainings);
        if (remainingSuggestions != null) {
            remainingSuggestions.removeAll(characterPlayer.getSelectedTrainingIds());
            if (remainingSuggestions.contains(trainingId)
                    && characterPlayer.getTrainingDevelopmentCost(trainingId) <= characterPlayer.getRemainingDevelopmentPoints()) {
                // At least one training per level.
                if (characterPlayer.getCurrentLevel().getTrainings().isEmpty()) {
                    return 100;
                } else if (remainingSuggestions.size() > finalLevel - characterPlayer.getLevels().size()) {
                    return 100;
                }
            }
        }

        int probability = (int) ((28 - cost) * 1.5 + characterPlayer.getLevels().size()
                - ((characterPlayer.getSelectedTrainingIds().size() + specialization) * 25));

        if (characterPlayer.isTrainingFavouredByProfession(trainingId)) {
            probability += 15;
        }

        // Culture has a favourite training.
        if (characterPlayer.getCulture() != null && characterPlayer.getCulture().getTrainingPricePercentage(trainingId) > 0) {
            probability += 10;
        }

        if (probability < 1 && characterPlayer.getSelectedTrainingIds().size() < characterPlayer.getLevels().size() / 10) {
            probability = 1;
        }

        if (characterPlayer.isTrainingForbiddenByProfession(trainingId)) {
            probability -= 1500;
        }

        // Elementalist must take an elementalist training first.
        if (isElementalistProfession(characterPlayer) && ElementalTriad.isElementalistTraining(trainingId)
                && characterPlayer.getSelectedTrainingIds().isEmpty()) {
            probability = 1000;
        }
        return probability / (characterPlayer.getCurrentLevel().getTrainings().size() + 1);
    }

    private static boolean isElementalistProfession(CharacterPlayer characterPlayer) throws InvalidXmlElementException {
        final Profession profession = characterPlayer.getProfession();
        return profession != null && profession.isElementalist();
    }
}