package com.softwaremagico.librodeesher.equipment;

/**
 * What a {@link MagicObject}'s {@link ObjectBonus} applies to, matching the legacy {@code
 * BonusType} exactly: a {@link #SKILL} or {@link #CATEGORY} bonus (identified by
 * {@link ObjectBonus#getBonusName()}, a real skill/category id) or a flat {@link #DEFENSIVE_BONUS}
 * (unnamed, only one such bonus makes sense per item).
 */
public enum BonusType {
    CATEGORY,
    SKILL,
    DEFENSIVE_BONUS
}
