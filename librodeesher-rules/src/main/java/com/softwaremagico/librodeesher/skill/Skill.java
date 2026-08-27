package com.softwaremagico.librodeesher.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;

import java.util.Collections;
import java.util.List;

/**
 * A character skill (e.g. "Cuero Endurecido", "Percepción (r)", "Hablar [Idioma1; Idioma2]"), as
 * defined in a rulebook's {@code habilidades.xml}.
 *
 * <p>Unlike most other rule elements, the legacy application never gave skills their own text file:
 * every skill was created on the fly the first time its name was found inside a category's
 * "Habilidades" column (see the original {@code SkillFactory#getSkill(String)}). The name itself
 * encoded several pieces of information using an ad-hoc mini-syntax:</p>
 * <ul>
 *     <li>{@code "Nombre*"}: trailing {@code *} marks the skill as {@link #isRare() rare} (excluded
 *     from random character generation).</li>
 *     <li>{@code "Nombre (r)"}/{@code "(p)"}/{@code "(c)"}: marks the {@link #getSkillType() skill type}.</li>
 *     <li>{@code "Nombre [Espec1; Espec2]"}: lists {@link #getSpecialities() specialities} of the skill.</li>
 *     <li>{@code "Nombre {Skill1|Skill2}"} / {@code "{Skill1&Skill2}"}: lists other skills unlocked
 *     ({@link #getEnableSkills()}) once this one is ranked, with OR/AND semantics
 *     ({@link #isAllEnabled()}).</li>
 * </ul>
 * <p>{@link SkillNameParser} extracts all of this into plain fields once, at migration time, so this
 * class itself is a plain data holder with no parsing logic, unlike the legacy {@code Skill}, which
 * parsed its own name in its constructor.</p>
 */
public class Skill extends Element {

    @JacksonXmlElementWrapper(localName = "specialities")
    @JacksonXmlProperty(localName = "speciality")
    private List<String> specialities;

    /** Names of other skills unlocked once this one has at least one rank. */
    @JacksonXmlElementWrapper(localName = "enableSkills")
    @JacksonXmlProperty(localName = "skill")
    private List<String> enableSkills;

    /** When {@code true}, every skill in {@link #enableSkills} is required (AND); otherwise any one (OR). */
    @JsonProperty("allEnabled")
    private boolean allEnabled;

    @JsonProperty("skillType")
    private SkillType skillType = SkillType.STANDARD;

    @JsonProperty("skillGroup")
    private SkillGroup skillGroup = SkillGroup.STANDARD;

    /** Trailing {@code *} in the legacy name: excluded from random character generation. */
    @JsonProperty("rare")
    private boolean rare;

    /**
     * Baked-in snapshot of whether this skill is usable, computed at migration time from every
     * other skill's {@link #enableSkills}: a skill referenced there starts disabled until unlocked
     * by ranking the skill that grants it.
     *
     * <p><strong>Known limitation:</strong> since this is computed once across the full, always-on
     * rule set and stored as plain data, disabling a module whose skills were the only ones granting
     * an "enableSkills" relationship will not re-enable the affected skill at runtime. Revisiting this
     * would require computing the disabled set live from the skills of the currently enabled modules
     * instead of baking it into the XML, left as future work alongside the equivalent limitation in
     * {@code CategoryMigrationTool}.</p>
     */
    @JsonProperty("enabledByDefault")
    private boolean enabledByDefault = true;

    /** Id of the category this skill was first found under. */
    @JsonProperty("categoryId")
    private String categoryId;

    public Skill() {
        super();
    }

    public Skill(String id) {
        super(id);
    }

    public List<String> getSpecialities() {
        return specialities == null ? Collections.emptyList() : specialities;
    }

    public void setSpecialities(List<String> specialities) {
        this.specialities = specialities;
    }

    public List<String> getEnableSkills() {
        return enableSkills == null ? Collections.emptyList() : enableSkills;
    }

    public void setEnableSkills(List<String> enableSkills) {
        this.enableSkills = enableSkills;
    }

    public boolean isAllEnabled() {
        return allEnabled;
    }

    public void setAllEnabled(boolean allEnabled) {
        this.allEnabled = allEnabled;
    }

    public SkillType getSkillType() {
        return skillType;
    }

    public void setSkillType(SkillType skillType) {
        this.skillType = skillType;
    }

    public SkillGroup getSkillGroup() {
        return skillGroup;
    }

    public void setSkillGroup(SkillGroup skillGroup) {
        this.skillGroup = skillGroup;
    }

    public boolean isRare() {
        return rare;
    }

    public void setRare(boolean rare) {
        this.rare = rare;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}
