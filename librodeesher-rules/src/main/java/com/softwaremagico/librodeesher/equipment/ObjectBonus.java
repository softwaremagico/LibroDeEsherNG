package com.softwaremagico.librodeesher.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One bonus a {@link MagicObject} grants, matching the legacy {@code ObjectBonus} exactly. Unlike
 * the legacy version (whose {@code bonusName} was free display text, e.g. a skill's Spanish name),
 * {@link #getBonusName()} here is a real {@code Skill}/{@code Category} id (resolved once, at
 * migration time, against the same skill/category indices every other migration tool uses), so it
 * can be looked up directly against {@code RulesCatalog} without any further translation; {@code
 * null} for {@link BonusType#DEFENSIVE_BONUS}, which has no name (only one makes sense per item).
 */
public class ObjectBonus {

    @JsonProperty("type")
    private BonusType type;

    @JsonProperty("bonusName")
    private String bonusName;

    @JsonProperty("bonus")
    private Integer bonus;

    public ObjectBonus() {
        // Required by Jackson.
    }

    public ObjectBonus(BonusType type, String bonusName, Integer bonus) {
        this.type = type;
        this.bonusName = bonusName;
        this.bonus = bonus;
    }

    public BonusType getType() {
        return type;
    }

    public void setType(BonusType type) {
        this.type = type;
    }

    public String getBonusName() {
        return bonusName;
    }

    public void setBonusName(String bonusName) {
        this.bonusName = bonusName;
    }

    public Integer getBonus() {
        return bonus;
    }

    public void setBonus(Integer bonus) {
        this.bonus = bonus;
    }
}
