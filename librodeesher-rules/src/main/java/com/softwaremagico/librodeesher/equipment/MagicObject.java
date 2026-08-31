package com.softwaremagico.librodeesher.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.language.TranslatedText;
import com.softwaremagico.librodeesher.training.TrainingItemType;
import com.softwaremagico.librodeesher.training.TrainingSpecialItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A magic item: a name/description plus a list of bonuses (see {@link ObjectBonus}), matching the
 * legacy {@code MagicObject} exactly minus its persistence concerns. Every shipped instance comes
 * from a {@link TrainingSpecialItem} for which {@link TrainingSpecialItem#isMagic()} is {@code true}
 * (see {@link #forTrainingSpecialItem(TrainingSpecialItem)}); a character can also be given one
 * directly (not tied to any training), matching the legacy {@code CharacterPlayer#addMagicItem}.
 */
public class MagicObject {

    @JsonProperty("name")
    private TranslatedText name;

    @JsonProperty("description")
    private TranslatedText description;

    @JacksonXmlElementWrapper(localName = "bonuses")
    @JacksonXmlProperty(localName = "bonus")
    private List<ObjectBonus> bonuses;

    public MagicObject() {
        // Required by Jackson.
    }

    public MagicObject(TranslatedText name, TranslatedText description, List<ObjectBonus> bonuses) {
        this.name = name;
        this.description = description;
        this.bonuses = bonuses;
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

    public List<ObjectBonus> getBonuses() {
        return bonuses == null ? Collections.emptyList() : bonuses;
    }

    public void setBonuses(List<ObjectBonus> bonuses) {
        this.bonuses = bonuses;
    }

    /** The bonus this object grants to the skill {@code skillId}, or 0 if it grants it none. */
    public int getSkillBonus(String skillId) {
        return getObjectBonus(BonusType.SKILL, skillId);
    }

    /** The bonus this object grants to the category {@code categoryId}, or 0 if it grants it none. */
    public int getCategoryBonus(String categoryId) {
        return getObjectBonus(BonusType.CATEGORY, categoryId);
    }

    /**
     * The flat bonus this object grants of {@code type} (only meaningful for {@link
     * BonusType#DEFENSIVE_BONUS}, which has no name), matching the legacy {@code
     * MagicObject#getObjectBonus(BonusType)} exactly.
     */
    public int getObjectBonus(BonusType type) {
        return getObjectBonus(type, null);
    }

    private int getObjectBonus(BonusType type, String bonusName) {
        for (final ObjectBonus objectBonus : getBonuses()) {
            if (objectBonus.getType() == type
                    && (bonusName == null || bonusName.equals(objectBonus.getBonusName()))) {
                return objectBonus.getBonus() == null ? 0 : objectBonus.getBonus();
            }
        }
        return 0;
    }

    /**
     * Builds the {@link MagicObject} a {@link TrainingSpecialItem} grants (only meaningful when
     * {@link TrainingSpecialItem#isMagic()} is {@code true}), matching the legacy {@code
     * MagicObject#createMagicObjectFor(Skill, TrainingItem)} exactly: its own name/description, and
     * a single {@link BonusType#SKILL}/{@link BonusType#CATEGORY} bonus (matching {@link
     * TrainingSpecialItem#getType()}) to {@link TrainingSpecialItem#getTargetId()}.
     */
    public static MagicObject forTrainingSpecialItem(TrainingSpecialItem item) {
        final BonusType type = item.getType() == TrainingItemType.CATEGORY ? BonusType.CATEGORY : BonusType.SKILL;
        final List<ObjectBonus> bonuses = new ArrayList<>();
        bonuses.add(new ObjectBonus(type, item.getTargetId(), item.getBonus()));
        return new MagicObject(item.getName(), item.getDescription(), bonuses);
    }
}
