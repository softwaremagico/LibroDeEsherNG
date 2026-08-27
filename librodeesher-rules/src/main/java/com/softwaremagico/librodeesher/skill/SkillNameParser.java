package com.softwaremagico.librodeesher.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses a raw skill token as it appears in a legacy rulebook text file (e.g. the "Habilidades"
 * column of {@code categorias.txt}) into its plain components.
 *
 * <p>Faithfully reproduces the parsing previously spread across the legacy {@code Skill} constructor
 * and {@code SkillFactory#createSkill(String)}, so every existing rulebook file keeps working
 * unchanged. See {@link Skill}'s javadoc for the mini-syntax being parsed.</p>
 */
public final class SkillNameParser {

    private static final Pattern ENABLE_SKILLS_PATTERN = Pattern.compile("\\{([^}]*)}");
    private static final Pattern SPECIALITIES_PATTERN = Pattern.compile("\\[([^]]*)]");
    private static final String CHI_PREFIX = "Poderes Chi:";
    private static final String[] FIREARM_SKILL_PREFIXES = {
            "Percepción del Entorno: Munición", "Fuego de Supresión", "Fuego Rápido"
    };

    private SkillNameParser() {
        // Utility class.
    }

    /** Parses {@code rawToken} (already split out of its comma-separated list) into a {@link Skill}. */
    public static Skill parse(String rawToken) {
        final boolean rare = rawToken.contains("*");
        final SkillType skillType = SkillType.detectFromRawName(rawToken);

        String remaining = rawToken;

        final List<String> enableSkills = new ArrayList<>();
        final boolean[] allEnabled = {false};
        remaining = extractBracketedList(remaining, ENABLE_SKILLS_PATTERN, content -> {
            final String[] tokens = content.contains("&") ? content.split("&") : content.split("\\|");
            allEnabled[0] = content.contains("&");
            for (final String token : tokens) {
                enableSkills.add(token.trim());
            }
        });

        final List<String> specialities = new ArrayList<>();
        remaining = extractBracketedList(remaining, SPECIALITIES_PATTERN, content -> {
            final String[] tokens = content.contains(";") ? content.split(";") : content.split(",");
            for (final String token : tokens) {
                specialities.add(token.trim());
            }
        });

        final String name = removeTypeMarkers(remaining).trim();

        final Skill skill = new Skill(name);
        skill.setName(name);
        skill.setRare(rare);
        skill.setSkillType(skillType);
        skill.setSkillGroup(detectGroup(name));
        skill.setEnableSkills(enableSkills.isEmpty() ? Collections.emptyList() : enableSkills);
        skill.setAllEnabled(allEnabled[0]);
        skill.setSpecialities(specialities.isEmpty() ? Collections.emptyList() : specialities);
        return skill;
    }

    /** Removes a single {@code {...}} or {@code [...]} block matched by {@code pattern}, if present. */
    private static String extractBracketedList(String text, Pattern pattern, java.util.function.Consumer<String> onMatch) {
        final var matcher = pattern.matcher(text);
        if (matcher.find()) {
            onMatch.accept(matcher.group(1));
            return text.substring(0, matcher.start()) + text.substring(matcher.end());
        }
        return text;
    }

    /** Strips the "*" rare marker and "(r)"/"(p)"/"(c)" skill-type suffixes from a raw skill name. */
    private static String removeTypeMarkers(String skillName) {
        return skillName.replace("*", "")
                .replace("(R)", "").replace("(r)", "")
                .replace("(C)", "").replace("(c)", "")
                .replace("(P)", "").replace("(p)", "")
                .trim();
    }

    private static SkillGroup detectGroup(String skillName) {
        if (skillName.startsWith(CHI_PREFIX)) {
            return SkillGroup.CHI;
        }
        for (final String prefix : FIREARM_SKILL_PREFIXES) {
            if (skillName.startsWith(prefix)) {
                return SkillGroup.FIREARM;
            }
        }
        return SkillGroup.STANDARD;
    }
}
