package com.softwaremagico.librodeesher.perk;

/**
 * How {@link PerkBonus#getValue()}/{@link PerkChoiceGrant#getValue()} applies, mirroring the legacy
 * "bonuses" column's {@code "*"} (conditional) and {@code "/r"} (per-rank) markers.
 */
public enum PerkBonusKind {
    /** A flat, always-on bonus. */
    FLAT,
    /** Only applies under some condition described in the perk's free-text description (the {@code "*"} marker). */
    CONDITIONAL,
    /** A bonus applied to every rank bought (the {@code "/r"} marker, e.g. "+4 to every rank of X"). */
    PER_RANK,
    /** Makes the target skill/category common instead of granting a numeric bonus (the {@code "(Común)"} marker). */
    MAKES_COMMON,
    /** Makes the target skill/category restricted instead of granting a numeric bonus (the {@code "(Restringida)"} marker). */
    MAKES_RESTRICTED
}
