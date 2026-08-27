package com.softwaremagico.librodeesher.perk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.language.TranslatedText;

import java.util.Collections;
import java.util.List;

/**
 * A character perk or weakness (e.g. "Suerte", "Adicción Ligera"), as defined in a rulebook's
 * {@code talentos.xml}.
 *
 * <p>{@link #getCost()} is negative for a weakness (it grants background points back to the player
 * instead of costing them), matching the legacy convention.</p>
 *
 * <p>The "bonuses" column of the legacy file packs a lot of different, loosely structured
 * information: fixed bonuses to a category/skill/characteristic/resistance, conditional bonuses,
 * extra ranks, and "choose one of these N options" groups (including special tokens like
 * {@code "Cualquier Categoría"}). Interpreting all of that requires cross-referencing
 * {@link com.softwaremagico.librodeesher.category.Category} and
 * {@link com.softwaremagico.librodeesher.skill.Skill}, and is significant enough scope that it is left
 * as dedicated future work (the equivalent of the legacy {@code PerkFactory#addBonuses}). For now
 * {@link #getBonusesRaw()} preserves the column verbatim so no information is lost.</p>
 */
public class Perk extends Element {

    @JsonProperty("cost")
    private Integer cost;

    /**
     * Raw "Permitido" column: either the literal {@code "Todos"} (available to every race/profession)
     * or a semicolon/comma-separated list of race and/or profession names. Not resolved against
     * {@link com.softwaremagico.librodeesher.category.CategoryFactory}-style factories at migration
     * time, since races/professions/perks are migrated independently of each other.
     */
    @JacksonXmlElementWrapper(localName = "availableTo")
    @JacksonXmlProperty(localName = "name")
    private List<String> availableTo;

    @JsonProperty("grade")
    private PerkGrade grade;

    @JsonProperty("type")
    private PerkType type;

    /** Verbatim "bonuses" column, see the class javadoc. */
    @JsonProperty("bonusesRaw")
    private String bonusesRaw;

    @JsonProperty("description")
    private TranslatedText description;

    public Perk() {
        super();
    }

    public Perk(String id) {
        super(id);
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    /** Whether this entry is a weakness (grants points back) rather than a perk (costs points). */
    public boolean isWeakness() {
        return cost != null && cost < 0;
    }

    public List<String> getAvailableTo() {
        return availableTo == null ? Collections.emptyList() : availableTo;
    }

    public void setAvailableTo(List<String> availableTo) {
        this.availableTo = availableTo;
    }

    /** Whether every race and profession can take this perk (the legacy "Todos" marker). */
    public boolean isAvailableToEveryone() {
        return getAvailableTo().size() == 1 && "Todos".equalsIgnoreCase(getAvailableTo().get(0));
    }

    public PerkGrade getGrade() {
        return grade;
    }

    public void setGrade(PerkGrade grade) {
        this.grade = grade;
    }

    public PerkType getType() {
        return type;
    }

    public void setType(PerkType type) {
        this.type = type;
    }

    public String getBonusesRaw() {
        return bonusesRaw;
    }

    public void setBonusesRaw(String bonusesRaw) {
        this.bonusesRaw = bonusesRaw;
    }

    public TranslatedText getDescription() {
        return description;
    }

    public void setDescription(TranslatedText description) {
        this.description = description;
    }

    /** Convenience setter building the {@link TranslatedText} from its two languages. */
    public void setDescription(String spanish, String english) {
        this.description = new TranslatedText(spanish, english);
    }
}
