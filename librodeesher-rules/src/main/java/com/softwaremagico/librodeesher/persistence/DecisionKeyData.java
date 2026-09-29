package com.softwaremagico.librodeesher.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwaremagico.librodeesher.decision.DecisionKey;
import com.softwaremagico.librodeesher.decision.DecisionKind;

/**
 * A persisted {@link DecisionKey}: the same structured parts (kind, owner id, index, optional
 * sub-index and level) as flat, deserialization-friendly fields, exactly like {@link DecisionKey},
 * which itself cannot be built by frameworks because it validates through its factories. The mapper
 * rebuilds the live key from this DTO with {@link #toDecisionKey()}.
 */
public final class DecisionKeyData {

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("ownerId")
    private String ownerId;

    @JsonProperty("index")
    private int index;

    @JsonProperty("subIndex")
    private int subIndex;

    @JsonProperty("level")
    private int level;

    public DecisionKeyData() {
        // Required by deserialization frameworks.
    }

    public DecisionKeyData(DecisionKey key) {
        this.kind = key.getKind().name();
        this.ownerId = key.getOwnerId();
        this.index = key.getIndex();
        this.subIndex = key.getSubIndex();
        this.level = key.getLevel();
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getSubIndex() {
        return subIndex;
    }

    public void setSubIndex(int subIndex) {
        this.subIndex = subIndex;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    /** The live {@link DecisionKey} this DTO represents. */
    public DecisionKey toDecisionKey() {
        final DecisionKind decisionKind = DecisionKind.valueOf(kind);
        if (subIndex == DecisionKey.NO_SUB_INDEX) {
            if (decisionKind.isPerLevel()) {
                return DecisionKey.atLevel(decisionKind, ownerId, index, level);
            }
            return DecisionKey.characterWide(decisionKind, ownerId, index);
        }
        if (decisionKind.isPerLevel()) {
            return DecisionKey.nestedAtLevel(decisionKind, ownerId, index, subIndex, level);
        }
        return DecisionKey.nestedCharacterWide(decisionKind, ownerId, index, subIndex);
    }
}