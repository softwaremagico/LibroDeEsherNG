package com.softwaremagico.librodeesher.weapon;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.Element;

/**
 * A weapon (e.g. "Espada Larga", "Ballesta Ligera"), as defined in a rulebook's {@code armas.xml}.
 *
 * <p>The legacy application stored one text file per {@link WeaponType} (e.g. {@code armas/Filo.txt}),
 * each just a two-column "Nombre\tAbreviatura" list; this migration consolidates every weapon type of
 * a module into a single {@code armas.xml}, with {@link #getType()} carrying what used to be implicit
 * in the file name.</p>
 */
public class Weapon extends Element {

    @JsonProperty("type")
    private WeaponType type;

    @JsonProperty("abbreviation")
    private String abbreviation;

    /** Trailing {@code *} in the legacy name: a rare weapon, excluded from random equipment selection. */
    @JsonProperty("rare")
    private boolean rare;

    public Weapon() {
        super();
    }

    public Weapon(String id) {
        super(id);
    }

    public WeaponType getType() {
        return type;
    }

    public void setType(WeaponType type) {
        this.type = type;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public boolean isRare() {
        return rare;
    }

    public void setRare(boolean rare) {
        this.rare = rare;
    }

    /** Id of the skill category this weapon's training belongs to, e.g. "Armas·Filo". */
    public String getCategoryId() {
        return type == null ? null : type.getCategoryId();
    }
}
