package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;

import java.util.Collections;
import java.util.List;

/**
 * One realm of magic a profession grants: either a single, fixed realm, or a choice between several
 * (a "hybrid" profession, e.g. "Esencia/Canalización"), matching the legacy "REINOS DE MAGIA"
 * section's {@code "Realm1/Realm2"} syntax. A profession granting more than one {@code
 * RealmOfMagicGrant} (comma-separated in the legacy file) is a caster of every one of them at once,
 * as opposed to the "/" alternatives within a single grant, which are mutually exclusive.
 */
public class RealmOfMagicGrant {

    @JacksonXmlElementWrapper(localName = "options")
    @JacksonXmlProperty(localName = "realm")
    private List<RealmOfMagic> options;

    public RealmOfMagicGrant() {
        // Required by Jackson.
    }

    public RealmOfMagicGrant(List<RealmOfMagic> options) {
        this.options = options;
    }

    public List<RealmOfMagic> getOptions() {
        return options == null ? Collections.emptyList() : options;
    }

    public void setOptions(List<RealmOfMagic> options) {
        this.options = options;
    }

    /** Whether the player must choose one realm among several, rather than getting a fixed one. */
    public boolean isChoice() {
        return getOptions().size() > 1;
    }
}
