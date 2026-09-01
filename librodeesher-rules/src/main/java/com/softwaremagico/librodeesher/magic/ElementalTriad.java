package com.softwaremagico.librodeesher.magic;

import java.util.ArrayList;
import java.util.List;

/**
 * The "elemental triad" grouping of the 6 legacy "elementalist" trainings (splatbook trainings that
 * specialize a caster in a single classic element, e.g. {@code "Mago del Fuego"}/"Fire Wizard"),
 * matching the legacy {@code Triad} class exactly: a {@link RealmOfMagic#ESSENCE} spell list one of
 * these trainings owns is shared with the other trainings of its own triad ({@link
 * MagicListType#TRIAD}) and, to a lesser degree, with the other triad ({@link
 * MagicListType#COMPLEMENTARY_TRIAD}).
 *
 * <p>Only 3 of the 6 trainings this table names are actually shipped ({@code "fireWizard"}, {@code
 * "lightWizard"}, {@code "wizardOfTheAir"}); the other 3 ({@code "iceWizard"}, {@code "waterWizard"},
 * {@code "earthWizard"}) are kept anyway, matching legacy verbatim, but can never match a real
 * selected training since no training with that id exists in the migrated data.</p>
 */
public final class ElementalTriad {

    private static final List<String> FIRST_TRIAD = List.of("fireWizard", "iceWizard", "waterWizard");
    private static final List<String> SECOND_TRIAD = List.of("earthWizard", "wizardOfTheAir", "lightWizard");

    private ElementalTriad() {
        // Utility class.
    }

    /** Whether {@code trainingId} is one of the 6 elementalist trainings this table knows about. */
    public static boolean isElementalistTraining(String trainingId) {
        return FIRST_TRIAD.contains(trainingId) || SECOND_TRIAD.contains(trainingId);
    }

    /**
     * Every other elementalist training of {@code trainingId}'s own triad (excluding itself); empty
     * if {@code trainingId} is not one of the 6 elementalist trainings.
     */
    public static List<String> getSameTriadTrainings(String trainingId) {
        final List<String> triad = ownTriadOf(trainingId);
        if (triad == null) {
            return List.of();
        }
        final List<String> others = new ArrayList<>(triad);
        others.remove(trainingId);
        return others;
    }

    /**
     * Every elementalist training of the triad other than {@code trainingId}'s own; empty if {@code
     * trainingId} is not one of the 6 elementalist trainings.
     */
    public static List<String> getOtherTriadTrainings(String trainingId) {
        if (FIRST_TRIAD.contains(trainingId)) {
            return SECOND_TRIAD;
        }
        if (SECOND_TRIAD.contains(trainingId)) {
            return FIRST_TRIAD;
        }
        return List.of();
    }

    private static List<String> ownTriadOf(String trainingId) {
        if (FIRST_TRIAD.contains(trainingId)) {
            return FIRST_TRIAD;
        }
        if (SECOND_TRIAD.contains(trainingId)) {
            return SECOND_TRIAD;
        }
        return null;
    }
}
