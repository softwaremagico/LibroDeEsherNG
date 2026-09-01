package com.softwaremagico.librodeesher.perk;

/**
 * Which pool of skills/categories a {@link PerkChoiceGrant} lets the player pick from (the legacy
 * "Cualquier Categoría"/"Cualquier Habilidad"/"Cualquier Arma"/"cualquier Categoría de Armas"
 * special markers). {@code null} on {@link PerkChoiceGrant#getScope()} means an explicit, single
 * category/skill instead (see {@link PerkChoiceGrant#getCategoryId()}/{@link
 * PerkChoiceGrant#getSkillId()}).
 */
public enum PerkChoiceScope {
    /** Any category at all ({@code "Cualquier Categoría"}). */
    ANY_CATEGORY,
    /** Any weapon category ({@code "cualquier Categoría de Armas"}). */
    ANY_WEAPON_CATEGORY,
    /** Any skill at all ({@code "Cualquier Habilidad"}). */
    ANY_SKILL,
    /** Any weapon skill ({@code "Cualquier Arma"}). */
    ANY_WEAPON_SKILL
}
