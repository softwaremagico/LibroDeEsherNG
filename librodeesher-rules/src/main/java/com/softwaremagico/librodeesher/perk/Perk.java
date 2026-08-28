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
 * information: fixed bonuses to a category/skill/characteristic/resistance/appearance/armor/movement
 * (see {@link #getBonuses()}, matching the legacy {@code PerkFactory#addDefinedBonus}), and "choose
 * one of these N options" groups (see {@link #getChoiceGrants()}, matching {@code
 * PerkFactory#addListToChooseBonus}). A few bonus targets are spell-list groupings the magic system
 * (not modeled yet) would need to resolve; see {@link PerkBonus#getUnresolvedTargetId()}/{@link
 * PerkChoiceGrant#getUnresolvedScopeId()}.</p>
 */
public class Perk extends Element {

    @JsonProperty("cost")
    private Integer cost;

    /**
     * "Permitido" column, translated: either the literal {@code "Everyone"} (the legacy "Todos"
     * marker, available to every race/profession) or a list of race and/or profession names,
     * translated to English but not resolved to a {@code Race}/{@code Profession} id at migration
     * time, since races/professions/perks are migrated independently of each other.
     */
    @JacksonXmlElementWrapper(localName = "availableTo")
    @JacksonXmlProperty(localName = "entry")
    private List<String> availableTo;

    @JsonProperty("grade")
    private PerkGrade grade;

    @JsonProperty("type")
    private PerkType type;

    /** Fixed bonuses; see the class javadoc and {@link PerkBonus}. */
    @JacksonXmlElementWrapper(localName = "bonuses")
    @JacksonXmlProperty(localName = "bonus")
    private List<PerkBonus> bonuses;

    /** "Choose N of..." grants; see the class javadoc and {@link PerkChoiceGrant}. */
    @JacksonXmlElementWrapper(localName = "choiceGrants")
    @JacksonXmlProperty(localName = "choiceGrant")
    private List<PerkChoiceGrant> choiceGrants;

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

    /** Whether every race and profession can take this perk (the legacy "Todos" marker, translated to "Everyone"). */
    public boolean isAvailableToEveryone() {
        return getAvailableTo().size() == 1 && "Everyone".equalsIgnoreCase(getAvailableTo().get(0));
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

    public List<PerkBonus> getBonuses() {
        return bonuses == null ? Collections.emptyList() : bonuses;
    }

    public void setBonuses(List<PerkBonus> bonuses) {
        this.bonuses = bonuses;
    }

    public List<PerkChoiceGrant> getChoiceGrants() {
        return choiceGrants == null ? Collections.emptyList() : choiceGrants;
    }

    public void setChoiceGrants(List<PerkChoiceGrant> choiceGrants) {
        this.choiceGrants = choiceGrants;
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
