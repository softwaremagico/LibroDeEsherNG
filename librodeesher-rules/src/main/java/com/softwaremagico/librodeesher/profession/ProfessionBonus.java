package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A flat bonus granted by a profession to a category or a skill (the "BONIFICACIÓN POR PROFESIÓN"
 * section). Whether {@link #getName()} refers to a category or a skill is not resolved at migration
 * time (it requires cross-referencing both factories); consuming code should look it up in both.
 */
public class ProfessionBonus {

    @JsonProperty("name")
    private String name;

    @JsonProperty("bonus")
    private Integer bonus;

    public ProfessionBonus() {
        // Required by Jackson.
    }

    public ProfessionBonus(String name, Integer bonus) {
        this.name = name;
        this.bonus = bonus;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getBonus() {
        return bonus;
    }

    public void setBonus(Integer bonus) {
        this.bonus = bonus;
    }
}
