package com.softwaremagico.librodeesher.perk;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One perk (or weakness) a character has taken, as tracked by {@code CharacterPlayer}: which {@link
 * Perk} (by id), optionally paired with a weakness (also by id, discounting its background points
 * cost), and whether it was picked by random character generation (slightly cheaper, and not
 * removable by the player once set).
 */
public class SelectedPerk {

    @JsonProperty("perkId")
    private String perkId;

    @JsonProperty("weaknessId")
    private String weaknessId;

    @JsonProperty("random")
    private boolean random;

    public SelectedPerk() {
        // Required by deserialization frameworks.
    }

    public SelectedPerk(String perkId) {
        this.perkId = perkId;
    }

    public String getPerkId() {
        return perkId;
    }

    public void setPerkId(String perkId) {
        this.perkId = perkId;
    }

    public String getWeaknessId() {
        return weaknessId;
    }

    public void setWeaknessId(String weaknessId) {
        this.weaknessId = weaknessId;
    }

    public boolean isRandom() {
        return random;
    }

    public void setRandom(boolean random) {
        this.random = random;
    }
}
