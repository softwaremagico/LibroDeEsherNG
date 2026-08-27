package com.softwaremagico.librodeesher.skill;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Rank-cost variant of a skill, as encoded by a trailing tag in its legacy name, e.g.
 * {@code "Percepción (r)"}.
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

    @JsonValue
    public String getTag() {
        return tag;
    }

    @JsonCreator
    public static SkillType fromTag(String tag) {
        for (final SkillType type : values()) {
            if (type.tag.equals(tag)) {
                return type;
            }
        }
        return STANDARD;
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
