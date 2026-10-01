package com.softwaremagico.librodeesher.decision;

import java.util.Objects;

/**
 * Identifies which specific grant a {@link Decision} resolves, as structured data instead of a
 * string assembled at the call site.
 *
 * <p>A key is made of its parts: the {@link DecisionKind kind} of grant, the {@code ownerId} of the
 * rule element that offers it (a training, a culture, a profession, a perk...), the {@code index} of
 * that grant among its owner's grants, an optional {@code subIndex} for grants nested inside another
 * one (the skills offered by a category grant), and the {@code level} the decision belongs to.</p>
 *
 * <p>The level is part of the key's identity, so the same grant decided at two different levels
 * yields two independent decisions instead of the second one silently reusing the first. Only
 * {@link DecisionKind#isPerLevel() per-level} kinds use it; character-wide kinds use
 * {@link #CHARACTER_WIDE}. For every kind, {@link Decision#getRecordedAtLevel()} additionally states
 * the level at which the decision was actually taken.</p>
 */
public final class DecisionKey {

    /** The level marker of a decision that belongs to the character as a whole, not to one level. */
    public static final int CHARACTER_WIDE = 0;

    /** The {@code subIndex} of a grant that is not nested inside another one. */
    public static final int NO_SUB_INDEX = -1;

    /**
     * Reserved offset applied to the sub-index of a nested <em>spell list</em> grant, so that it can
     * never collide with the sub-index of a nested skill grant of the same owner grant. Large enough to
     * stay clear of any realistic number of skill grants.
     */
    private static final int SPELL_LIST_SUB_INDEX_OFFSET = 1_000_000;

    private final DecisionKind kind;
    private final String ownerId;
    private final int index;
    private final int subIndex;
    private final int level;

    private DecisionKey(DecisionKind kind, String ownerId, int index, int subIndex, int level) {
        this.kind = Objects.requireNonNull(kind, "A decision needs a kind.");
        this.ownerId = Objects.requireNonNull(ownerId, "A decision needs an owner id (empty if it has no owner).");
        if (index < 0) {
            throw new IllegalArgumentException("A decision index cannot be negative: " + index + ".");
        }
        if (subIndex < NO_SUB_INDEX) {
            throw new IllegalArgumentException("A decision sub-index is either nested or " + NO_SUB_INDEX
                    + ", not " + subIndex + ".");
        }
        if (kind.isPerLevel() && level < 1) {
            throw new IllegalArgumentException(kind + " decisions belong to a level, but got " + level + ".");
        }
        if (!kind.isPerLevel() && level != CHARACTER_WIDE) {
            throw new IllegalArgumentException(kind + " decisions belong to the whole character, so they cannot be "
                    + "bound to level " + level + ".");
        }
        this.index = index;
        this.subIndex = subIndex;
        this.level = level;
    }

    /**
     * The key of a {@link DecisionKind#isPerLevel() per-level} grant.
     *
     * @param kind    what the grant decides.
     * @param ownerId the rule element offering it (a training, a culture...), or {@code ""} if it
     *                has no owner.
     * @param index   which grant of that owner this is.
     * @param level   the level it belongs to (1-based).
     */
    public static DecisionKey atLevel(DecisionKind kind, String ownerId, int index, int level) {
        return new DecisionKey(kind, ownerId, index, NO_SUB_INDEX, level);
    }

    /**
     * The key of a grant nested inside another one (a skill offered by a category grant).
     *
     * @param kind    what the nested grant decides.
     * @param ownerId the rule element offering it, or {@code ""} if it has no owner.
     * @param index   which grant of that owner contains this one.
     * @param subIndex which of the nested grants this is.
     * @param level   the level it belongs to (1-based).
     */
    public static DecisionKey nestedAtLevel(DecisionKind kind, String ownerId, int index, int subIndex, int level) {
        return new DecisionKey(kind, ownerId, index, subIndex, level);
    }

    /**
     * The key of a {@link DecisionKind#isPerLevel() character-wide} grant: the whole character
     * shares it, so it is recorded once ({@link #CHARACTER_WIDE}) whatever the level.
     */
    public static DecisionKey characterWide(DecisionKind kind, String ownerId, int index) {
        return new DecisionKey(kind, ownerId, index, NO_SUB_INDEX, CHARACTER_WIDE);
    }

    /** The key of a character-wide grant nested inside another one. */
    public static DecisionKey nestedCharacterWide(DecisionKind kind, String ownerId, int index, int subIndex) {
        return new DecisionKey(kind, ownerId, index, subIndex, CHARACTER_WIDE);
    }

    public DecisionKind getKind() {
        return kind;
    }

    /** The rule element offering this grant (a training, a culture...), or {@code ""} if it has no owner. */
    public String getOwnerId() {
        return ownerId;
    }

    /** Which grant of {@link #getOwnerId()} this is. */
    public int getIndex() {
        return index;
    }

    /** Which of the nested grants this is, or {@link #NO_SUB_INDEX} if it is not nested. */
    public int getSubIndex() {
        return subIndex;
    }

    /** Whether this grant is nested inside another one. */
    public boolean isNested() {
        return subIndex != NO_SUB_INDEX;
    }

    /** The level this decision belongs to, or {@link #CHARACTER_WIDE} for a character-wide one. */
    public int getLevel() {
        return level;
    }

    /** Whether this decision belongs to {@code candidateLevel} (character-wide ones never do). */
    public boolean isAtLevel(int candidateLevel) {
        return level == candidateLevel;
    }

    /** The same key one level further down, or {@code null} for a character-wide kind, which is the
     *  same at every level. */
    public DecisionKey atNextLevel() {
        if (!kind.isPerLevel()) {
            return null;
        }
        return new DecisionKey(kind, ownerId, index, subIndex, level + 1);
    }

    /**
     * The nested grant {@code subIndex} under the same owner-grant-level as this key, with the same
     * kind (a category grant's nested skill choices are identified by their sub-index within the
     * category grant's own key).
     */
    public DecisionKey nested(int subIndex) {
        return new DecisionKey(kind, ownerId, index, subIndex, level);
    }

    /**
     * The nested <em>spell list</em> grant {@code subIndex} of the same owner-grant-level as this key.
     * Distinct from {@link #nested(int)}, which yields the nested <em>skill</em> grant of the same
     * sub-index: a category grant may offer both, and their decisions must not alias each other.
     */
    public DecisionKey nestedSpellList(int subIndex) {
        return new DecisionKey(kind, ownerId, index, SPELL_LIST_SUB_INDEX_OFFSET + subIndex, level);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DecisionKey)) {
            return false;
        }
        final DecisionKey that = (DecisionKey) other;
        return index == that.index && subIndex == that.subIndex && level == that.level && kind == that.kind
                && ownerId.equals(that.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, ownerId, index, subIndex, level);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(kind.name()).append('[').append(ownerId).append('#').append(index);
        if (isNested()) {
            builder.append('/').append(subIndex);
        }
        return builder.append(level == CHARACTER_WIDE ? "]" : "@L" + level + "]").toString();
    }
}
