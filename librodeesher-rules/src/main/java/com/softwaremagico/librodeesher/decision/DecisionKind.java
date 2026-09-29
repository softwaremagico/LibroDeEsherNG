package com.softwaremagico.librodeesher.decision;

/**
 * What a {@link Decision} a character has made actually decides.
 *
 * <p>Every value names one kind of "choose one (or N) of these" grant found in the rule data, so a
 * decision can be identified by structure ({@link DecisionKey}) instead of by a string built at the
 * call site. A kind also declares whether the decision belongs to one level or to the character as
 * a whole, which is what lets the same training be chosen again at a later level.
 * </p>
 */
public enum DecisionKind {

    /** A training's category grant (the "categorías" section), identified by the training and the grant index. */
    TRAINING_CATEGORY(true),

    /** A skill chosen inside one of a training's category grants (nested {@link #TRAINING_CATEGORY}). */
    TRAINING_CATEGORY_SKILL(true),

    /** A training's characteristic upgrade ("aumentos características"). */
    TRAINING_CHARACTERISTIC(true),

    /** A culture's adolescence category grant (the "adolescencia" section). */
    CULTURE_ADOLESCENCE_CATEGORY(true),

    /** A skill chosen inside one of a culture's adolescence category grants. */
    CULTURE_ADOLESCENCE_SKILL(true),

    /** A training's "life style" skill choice. */
    TRAINING_LIFE_SKILL(true),

    /** A training's "common skills" choice. */
    TRAINING_COMMON_SKILL(true),

    /** A training's "professional skills" choice. */
    TRAINING_PROFESSIONAL_SKILL(true),

    /** A training's "restricted skills" choice. */
    TRAINING_RESTRICTED_SKILL(true),

    /** One of a training's special items (equipment or magic item granted by the training). */
    TRAINING_SPECIAL_ITEM(true),

    /** One of a profession's "choose N common skills" grants. */
    PROFESSION_COMMON_SKILL(false),

    /** One of a profession's "choose N professional skills" grants. */
    PROFESSION_PROFESSIONAL_SKILL(false),

    /** One of a profession's "choose N restricted skills" grants. */
    PROFESSION_RESTRICTED_SKILL(false),

    /** One of a profession's realms of magic. */
    PROFESSION_REALM(false),

    /** A target chosen by one of a perk's fixed bonus grants. */
    PERK_CHOICE(false),

    /** A weapon category assigned to one tier of the profession's weapon cost table. */
    WEAPON_COST_TIER(false),

    /** A language filling one of the race's optional language slots. */
    OPTIONAL_RACE_LANGUAGE(false),

    /** A language filling one of the culture's optional language slots. */
    OPTIONAL_CULTURE_LANGUAGE(false),

    /** A language filling one of the background's optional language slots. */
    OPTIONAL_BACKGROUND_LANGUAGE(false),

    /** Which skill an enabling skill unlocks when it grants it (a martial arts style and its chi powers). */
    SKILL_ENABLE(false);

    private final boolean perLevel;

    DecisionKind(boolean perLevel) {
        this.perLevel = perLevel;
    }

    /**
     * Whether a decision of this kind belongs to a single level, so that the same grant can be
     * decided again (differently) at a later level, or whether it belongs to the character as a
     * whole and can only ever be decided once.
     *
     * <p>Per-level kinds are the ones a character resolves while building a level: a training's
     * categories, a culture's adolescence choices, a training's characteristic upgrade. Character-wide
     * kinds are resolved once and stay: the profession, its realms and its skill choices, the perks,
     * the weapon cost tiers, the optional language slots and the skills an enabling skill unlocks.</p>
     */
    public boolean isPerLevel() {
        return perLevel;
    }
}
