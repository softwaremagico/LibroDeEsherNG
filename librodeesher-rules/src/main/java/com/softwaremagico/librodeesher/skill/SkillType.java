package com.softwaremagico.librodeesher.skill;

/**
 * Rank-cost variant of a skill, as encoded by a trailing tag in its legacy name, e.g.
 * {@code "Percepción (r)"}.
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #detectFromRawName(String)}
 * is the one used by {@code SkillNameParser} to detect the tag in the original Spanish skill name.</p>
 *
 * <p>{@code GENERALIZED} is kept only for parity with the legacy enum: the original
 * {@code SkillType.getSkillType(String)} never actually detected it (its "(g)" tag was never
 * compared against), so no skill in the source data ever resolved to it either. It is preserved here
 * so this migration does not silently invent behaviour the legacy application never had.</p>
 */
public enum SkillType {

    RESTRICTED("(r)"),
    STANDARD(""),
    PROFESSIONAL("(p)"),
    COMMON("(c)"),
    GENERALIZED("(g)");

    private final String tag;

    SkillType(String tag) {
        this.tag = tag;
    }

    /**
     * Reproduces the legacy (case-insensitive-for-"(r)"-only) detection order: a name containing
     * "(r)" is restricted, otherwise "(p)" is professional, otherwise "(c)" is common, otherwise
     * standard.
     */
    public static SkillType detectFromRawName(String rawName) {
        final String lowerCase = rawName.toLowerCase();
        if (lowerCase.contains(RESTRICTED.tag)) {
            return RESTRICTED;
        } else if (rawName.contains(PROFESSIONAL.tag)) {
            return PROFESSIONAL;
        } else if (rawName.contains(COMMON.tag)) {
            return COMMON;
        }
        return STANDARD;
    }
}
