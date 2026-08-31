package com.softwaremagico.librodeesher.perk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.resistance.ResistanceType;

/**
 * One fixed bonus a perk grants (one entry of the "bonuses" column not wrapped in {@code
 * "{...}"}, see {@link Perk#getBonuses()}), matching the legacy {@code PerkFactory#addDefinedBonus}.
 *
 * <p>Exactly one of {@link #getSkillId()}, {@link #getCategoryId()}, {@link #getCharacteristic()},
 * {@link #getResistanceType()}, {@link #isAppearance()}, {@link #isArmor()}, {@link #isMovement()} or
 * {@link #getUnresolvedTargetId()} identifies the target; {@link #getValue()} is {@code null} when
 * {@link #getKind()} is {@link PerkBonusKind#MAKES_COMMON}/{@link PerkBonusKind#MAKES_RESTRICTED}
 * (which only apply to a skill or category target).</p>
 */
public class PerkBonus {

    @JsonProperty("skillId")
    private String skillId;

    @JsonProperty("categoryId")
    private String categoryId;

    @JsonProperty("characteristic")
    private CharacteristicAbbreviation characteristic;

    @JsonProperty("resistanceType")
    private ResistanceType resistanceType;

    @JsonProperty("appearance")
    private boolean appearance;

    @JsonProperty("armor")
    private boolean armor;

    @JsonProperty("movement")
    private boolean movement;

    /**
     * A translated-but-not-cross-referenced id for a bonus target this tool could not resolve
     * against a real skill/category: in practice, the only shipped value is {@code "realm"} (the
     * generic "TR Reino" resistance-to-your-own-realm-of-magic marker), fully resolved by {@code
     * CharacterPlayer#getResistanceTotalBonus(ResistanceType)}. A spell-list-classification target
     * (e.g. "Listas Básicas de Hechizos") does <strong>not</strong> land here: it resolves as a
     * regular {@link #getCategoryId()} instead, see {@code
     * com.softwaremagico.librodeesher.magic.MagicListType}'s javadoc.
     */
    @JsonProperty("unresolvedTargetId")
    private String unresolvedTargetId;

    @JsonProperty("kind")
    private PerkBonusKind kind;

    @JsonProperty("value")
    private Integer value;

    public PerkBonus() {
        // Required by Jackson.
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public CharacteristicAbbreviation getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(CharacteristicAbbreviation characteristic) {
        this.characteristic = characteristic;
    }

    public ResistanceType getResistanceType() {
        return resistanceType;
    }

    public void setResistanceType(ResistanceType resistanceType) {
        this.resistanceType = resistanceType;
    }

    public boolean isAppearance() {
        return appearance;
    }

    public void setAppearance(boolean appearance) {
        this.appearance = appearance;
    }

    public boolean isArmor() {
        return armor;
    }

    public void setArmor(boolean armor) {
        this.armor = armor;
    }

    public boolean isMovement() {
        return movement;
    }

    public void setMovement(boolean movement) {
        this.movement = movement;
    }

    public String getUnresolvedTargetId() {
        return unresolvedTargetId;
    }

    public void setUnresolvedTargetId(String unresolvedTargetId) {
        this.unresolvedTargetId = unresolvedTargetId;
    }

    public PerkBonusKind getKind() {
        return kind;
    }

    public void setKind(PerkBonusKind kind) {
        this.kind = kind;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
