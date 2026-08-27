package com.softwaremagico.librodeesher.magic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;

import java.util.Collections;
import java.util.List;

/**
 * A spell list belonging to a {@link RealmOfMagic} (e.g. "Ley del Fuego" in "Esencia"), as defined in
 * a rulebook's {@code hechizos.xml}.
 *
 * <p>{@link #getOwners()} lists which professions and/or trainings grant access to this list; two
 * special pseudo-owner tags are used by the legacy data instead of a real profession/training name:
 * {@value #OPEN_LIST_TAG} (any spell-casting profession of this realm may pick from it freely) and
 * {@value #CLOSED_LIST_TAG} (restricted, must be specifically granted). {@link #isOpenList()} and
 * {@link #isClosedList()} recognize them.</p>
 *
 * <p>Because {@link Element#getId()} must be unique across every enabled module for a given factory,
 * and the same list name is (rarely, but validly) reused across different realms, {@link #getId()}
 * here is {@code "<realm>|<name>"} rather than the bare list name; use {@link #getName()} for display.</p>
 */
public class MagicSpellList extends Element {

    public static final String OPEN_LIST_TAG = "Lista Abierta";
    public static final String CLOSED_LIST_TAG = "Lista Cerrada";

    @JsonProperty("realm")
    private RealmOfMagic realm;

    @JacksonXmlElementWrapper(localName = "owners")
    @JacksonXmlProperty(localName = "owner")
    private List<String> owners;

    public MagicSpellList() {
        super();
    }

    public MagicSpellList(String id) {
        super(id);
    }

    public RealmOfMagic getRealm() {
        return realm;
    }

    public void setRealm(RealmOfMagic realm) {
        this.realm = realm;
    }

    public List<String> getOwners() {
        return owners == null ? Collections.emptyList() : owners;
    }

    public void setOwners(List<String> owners) {
        this.owners = owners;
    }

    public boolean isOpenList() {
        return getOwners().contains(OPEN_LIST_TAG);
    }

    public boolean isClosedList() {
        return getOwners().contains(CLOSED_LIST_TAG);
    }

    /** Builds the realm-qualified id used to keep this list unique across every enabled module. */
    public static String buildId(RealmOfMagic realm, String name) {
        return realm.getTag() + "|" + name;
    }
}
