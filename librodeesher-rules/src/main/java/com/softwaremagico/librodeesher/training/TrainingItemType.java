package com.softwaremagico.librodeesher.training;

/**
 * What a {@link TrainingSpecialItem}'s bonus column (if any) applies to, matching the legacy {@code
 * TrainingItemType} exactly. {@link #WEAPON}/{@link #WEAPON_CLOSE_COMBAT}/{@link #WEAPON_RANGED}/
 * {@link #ARMOUR}/{@link #ANY} are special markers (e.g. "Cualquier arma") that this project does
 * not resolve any further (there is no per-weapon/per-armor item bonus mechanic; only {@link #SKILL}
 * and {@link #CATEGORY} actually feed {@link com.softwaremagico.librodeesher.equipment.MagicObject}).
 */
public enum TrainingItemType {
    WEAPON,
    WEAPON_CLOSE_COMBAT,
    WEAPON_RANGED,
    ARMOUR,
    SKILL,
    CATEGORY,
    ANY,
    UNKNOWN
}
