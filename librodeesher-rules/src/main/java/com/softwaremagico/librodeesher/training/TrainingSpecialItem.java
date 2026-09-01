package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.language.TranslatedText;

/**
 * One entry of a training's "ESPECIAL" (background items) section: a chance of starting play with a
 * given item, optionally with a bonus to a skill/category (in which case it is magic, see
 * {@link #isMagic()}), matching the legacy {@code TrainingItem} exactly.
 *
 * <p>{@link #getTargetId()} is a real {@code Skill}/{@code Category} id when {@link #getType()} is
 * {@link TrainingItemType#SKILL}/{@link TrainingItemType#CATEGORY} (resolved once, at migration
 * time, against the same skill/category indices every other migration tool uses); for every other
 * {@link #getType()} it is the item's own raw (translated, non-cross-referenced) tag instead, since
 * there is nothing to resolve it against (no per-weapon/per-armor item bonus mechanic).</p>
 */
public class TrainingSpecialItem {

    @JsonProperty("name")
    private TranslatedText name;

    @JsonProperty("description")
    private TranslatedText description;

    @JsonProperty("probability")
    private Integer probability;

    @JsonProperty("bonus")
    private Integer bonus;

    @JsonProperty("type")
    private TrainingItemType type;

    @JsonProperty("targetId")
    private String targetId;

    public TrainingSpecialItem() {
        // Required by Jackson.
    }

    public TrainingSpecialItem(TranslatedText name, TranslatedText description, Integer probability, Integer bonus,
                                TrainingItemType type, String targetId) {
        this.name = name;
        this.description = description;
        this.probability = probability;
        this.bonus = bonus;
        this.type = type;
        this.targetId = targetId;
    }

    public TranslatedText getName() {
        return name;
    }

    public void setName(TranslatedText name) {
        this.name = name;
    }

    public TranslatedText getDescription() {
        return description;
    }

    public void setDescription(TranslatedText description) {
        this.description = description;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer getBonus() {
        return bonus == null ? 0 : bonus;
    }

    public void setBonus(Integer bonus) {
        this.bonus = bonus;
    }

    public TrainingItemType getType() {
        return type == null ? TrainingItemType.UNKNOWN : type;
    }

    public void setType(TrainingItemType type) {
        this.type = type;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /**
     * Whether this item grants an actual bonus (as opposed to being flavor-only), matching the
     * legacy {@code TrainingItem#isMagic()} exactly: a non-zero bonus and a resolved (non-{@link
     * TrainingItemType#UNKNOWN}) target.
     */
    public boolean isMagic() {
        return getBonus() != 0 && getType() != TrainingItemType.UNKNOWN;
    }
}

