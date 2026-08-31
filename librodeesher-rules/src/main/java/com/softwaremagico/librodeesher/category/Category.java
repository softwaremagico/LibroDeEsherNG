package com.softwaremagico.librodeesher.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A skill category (e.g. "Armadura·Ligera", "Ataques Especiales"), as defined in a rulebook's
 * {@code categorias.xml}.
 *
 * <p>This replaces the legacy {@code Category}/{@code StandardCategory}/{@code CombinedCategory}/...
 * class hierarchy: instead of one Java subclass per progression type with hard-coded development
 * costs, the progression type is now plain data ({@link CategoryType}); the point-cost tables
 * themselves are a game rule, not something read from a file, and will be attached separately once
 * character development is ported (see {@code librodeesher-rules} roadmap).</p>
 *
 * <p>The {@code skills} column of the legacy text files could look like:</p>
 * <pre>Cuero Endurecido [TA9; TA10; TA11], Cuero Blando [TA5; TA6; TA7]</pre>
 * <p>The part in brackets described which "hidden" skill ranks unlock which armor/weapon training
 * tables. That bracket syntax is not parsed yet: {@link #getSkillsRaw()} preserves it verbatim so no
 * information is lost, while {@link #getSkills()} exposes the real skill ids (resolved by {@code
 * CategoryMigrationTool} via {@code SkillMigrationTool#buildSkillIndex}) for immediate use.</p>
 */
public class Category extends Element {

    /** Marks a category whose skills are not fixed, but derived from another file (e.g. weapons). */
    private static final String DYNAMIC_SKILLS_MARKER = "noimporta";

    @JsonProperty("abbreviation")
    private String abbreviation;

    @JacksonXmlElementWrapper(localName = "characteristics")
    @JacksonXmlProperty(localName = "characteristic")
    private List<CharacteristicAbbreviation> characteristics;

    @JsonProperty("type")
    private CategoryType type;

    /** Plain skill names, parsed from {@link #skillsRaw}. Empty when {@link #skillsRaw} is "noimporta". */
    @JacksonXmlElementWrapper(localName = "skills")
    @JacksonXmlProperty(localName = "skill")
    private List<String> skills;

    /** Verbatim "Habilidades" column from the legacy file, kept for full fidelity/debugging. */
    @JsonProperty("skillsRaw")
    private String skillsRaw;

    public Category() {
        super();
    }

    public Category(String id) {
        super(id);
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public List<CharacteristicAbbreviation> getCharacteristics() {
        return characteristics == null ? Collections.emptyList() : characteristics;
    }

    public void setCharacteristics(List<CharacteristicAbbreviation> characteristics) {
        this.characteristics = characteristics;
    }

    /**
     * Convenience method parsing the legacy "Ag/Fu/Ag" characteristics tag into {@link #characteristics},
     * resolving each two-letter Spanish tag to its {@link CharacteristicAbbreviation} instead of
     * keeping the raw tag, so the generated XML never embeds Spanish text. Deliberately not named
     * {@code setCharacteristics} to avoid a same-name overload with a different argument type, which
     * confuses Jackson's property-setter resolution during XML deserialization.
     */
    public void setCharacteristicsTag(String characteristicsTag) {
        final List<CharacteristicAbbreviation> resolved = new ArrayList<>();
        if (!"Ninguna".equalsIgnoreCase(characteristicsTag.trim()) && !"Ninguno".equalsIgnoreCase(characteristicsTag.trim())) {
            for (final String tag : characteristicsTag.split("/")) {
                final CharacteristicAbbreviation abbreviation = CharacteristicAbbreviation.fromTag(tag);
                if (abbreviation == CharacteristicAbbreviation.NONE) {
                    throw new IllegalStateException("Unknown characteristic tag: '" + tag + "'.");
                }
                resolved.add(abbreviation);
            }
        }
        this.characteristics = resolved;
    }

    public CategoryType getType() {
        return type;
    }

    /** The skill bonus granted by having {@code ranks} ranks in a skill of this category; see {@link CategoryType}. */
    public Integer getSkillRankBonus(int ranks) {
        return type.getSkillRankBonus(ranks);
    }

    /** The category's own bonus granted by having {@code ranks} ranks directly in it; see {@link CategoryType}. */
    public Integer getCategoryRankBonus(int ranks) {
        return type.getCategoryRankBonus(ranks);
    }

    /** A flat bonus granted regardless of ranks (only {@link CategoryType#PD} has one); see {@link CategoryType}. */
    public Integer getFixedBonus() {
        return type.getFixedBonus();
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public List<String> getSkills() {
        return skills == null ? Collections.emptyList() : skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getSkillsRaw() {
        return skillsRaw;
    }

    /**
     * Sets the raw skills column. Does <strong>not</strong> derive {@link #getSkills()} from it (see
     * {@link #namesFromRaw}/{@link #setSkills}, resolved separately by {@code CategoryMigrationTool}
     * once a skill index is available): the two fields are independent, both read directly from the
     * generated XML, so that Jackson's per-element deserialization order (this setter's old
     * side-effect used to run after {@link #setSkills} deserialized the real ids from {@code
     * <skills>}, silently overwriting them back to plain Spanish names) cannot resurrect the
     * unresolved names.
     */
    public void setSkillsRaw(String skillsRaw) {
        this.skillsRaw = skillsRaw;
    }

    /** Whether this category's skills come from another data source (e.g. weapon files) instead of a fixed list. */
    public boolean hasDynamicSkills() {
        return DYNAMIC_SKILLS_MARKER.equalsIgnoreCase(skillsRaw == null ? "" : skillsRaw.trim());
    }

    /**
     * Splits the raw "Habilidades" column into plain Spanish skill names, dropping the "[...]" unlock
     * hints, so {@code CategoryMigrationTool} can resolve each one to a real skill id (see
     * {@link #getSkills()}). Top-level commas separate skills; brackets never contain a comma in the
     * source data (they use semicolons), so a naive split on commas is safe here.
     */
    public static List<String> namesFromRaw(String raw) {
        final List<String> names = new ArrayList<>();
        if (raw == null || raw.isBlank() || DYNAMIC_SKILLS_MARKER.equalsIgnoreCase(raw.trim())) {
            return names;
        }
        for (final String entry : raw.split(",")) {
            String name = entry.trim();
            final int bracketIndex = name.indexOf('[');
            if (bracketIndex >= 0) {
                name = name.substring(0, bracketIndex).trim();
            }
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
}
