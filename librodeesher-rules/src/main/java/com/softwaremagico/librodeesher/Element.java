package com.softwaremagico.librodeesher;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.language.TranslatedText;

import java.util.Objects;

/**
 * Base class for every rule element read from a rulebook XML file (races, cultures, professions,
 * trainings, weapons, spell lists, skills, categories, perks...).
 *
 * <p>Instances are plain data holders populated by Jackson XML through {@link XmlFactory}: no I/O
 * happens inside this class or its subclasses, unlike the legacy implementation where each domain
 * class read its own text file from its constructor. Separating "what an element is" (this class)
 * from "how it is loaded" ({@link XmlFactory}) is the central refactor requested for this
 * modernization.</p>
 *
 * <p>{@link #getId()} is a stable, language-independent identifier (the original Spanish name, as
 * used throughout the legacy data as a cross-reference key: category ids, skill names,
 * training/profession names...). {@link #getName()} is the human-readable, bilingual display text;
 * every rulebook file is Spanish-only in origin, so the English translation is produced once by the
 * migration tools (see {@code com.softwaremagico.librodeesher.language.Translations}).</p>
 */
public class Element implements Comparable<Element> {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private TranslatedText name;

    /**
     * Optional grouping tag (e.g. weapon category, spell realm). Not every element type uses it.
     */
    @JsonProperty("group")
    private String group;

    /** Folder name of the module (rulebook supplement) this element was loaded from. Not persisted. */
    @JsonIgnore
    private String moduleName;

    protected Element() {
        // Required by Jackson.
    }

    protected Element(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TranslatedText getName() {
        return name;
    }

    public void setName(TranslatedText name) {
        this.name = name;
    }

    /** Convenience setter building the {@link TranslatedText} from its two languages. */
    public void setName(String spanish, String english) {
        this.name = new TranslatedText(spanish, english);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Element element)) {
            return false;
        }
        return Objects.equals(id, element.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(Element other) {
        if (name == null || other.name == null) {
            return 0;
        }
        return name.compareTo(other.name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    }
}
