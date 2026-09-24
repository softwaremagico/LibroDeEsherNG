package com.softwaremagico.librodeesher.magic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.language.Translations;

import java.util.Collections;
import java.util.List;

/**
 * A spell list belonging to a {@link RealmOfMagic} (e.g. "Law of Fire" in "Essence"), as defined in
 * a rulebook's {@code spells.xml}.
 *
 * <p>{@link #getOwners()} lists which professions and/or trainings grant access to this list; two
 * special pseudo-owner tags are used instead of a real profession/training name: {@value #OPEN_LIST_TAG}
 * (any spell-casting profession of this realm may pick from it freely) and {@value #CLOSED_LIST_TAG}
 * (restricted, must be specifically granted). {@link #isOpenList()} and {@link #isClosedList()}
 * recognize them. Real owner names are resolved to their real {@code Profession}/{@code Training} id
 * by {@code MagicMigrationTool} (professions are tried first); the rare "dark spell"/"elementalist
 * training" pseudo-owner tags that match neither are kept as a readable, non-accented placeholder
 * instead (see {@code MagicMigrationTool#translateOwners}'s javadoc).</p>
 *
 * <p>Because {@link Element#getId()} must be unique across every enabled module for a given factory,
 * and the same list name is (rarely, but validly) reused across different realms, {@link #getId()}
 * is realm-prefixed (e.g. {@code "essenceLawOfFire"}) rather than just the list name's id; use
 * {@link #getName()} for display.</p>
 */
public class MagicSpellList extends Element {

    public static final String OPEN_LIST_TAG = "OpenList";
    public static final String CLOSED_LIST_TAG = "ClosedList";
    private static final String ESSENCE_DARK_LIST_TAG = "essenceMaligna";
    private static final String CANALIZATION_DARK_LIST_TAG = "channelingMaligna";
    private static final String MENTALISM_DARK_LIST_TAG = "mentalismMaligno";

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

    /** Whether this list belongs to the legacy dark-magic owner for its realm. */
    public boolean isDarkList() {
        return switch (realm) {
            case ESSENCE -> getOwners().contains(ESSENCE_DARK_LIST_TAG);
            case CANALIZATION -> getOwners().contains(CANALIZATION_DARK_LIST_TAG);
            case MENTALISM -> getOwners().contains(MENTALISM_DARK_LIST_TAG);
            default -> false;
        };
    }

    /**
     * Builds the realm-prefixed, English-derived id for a spell list, e.g.
     * {@code buildId(ESSENCE, "Ley del Fuego")} -&gt; {@code "essenceLawOfFire"}.
     *
     * <p>Used both by {@code MagicMigrationTool} (through {@link com.softwaremagico.librodeesher.migration.IdAllocator}
     * for collision disambiguation) and directly by tests that need to know a list's id.</p>
     */
    public static String buildId(RealmOfMagic realm, String spanishName) {
        final String base = Translations.toEnglishId(spanishName);
        final String capitalized = base.isEmpty() ? base : Character.toUpperCase(base.charAt(0)) + base.substring(1);
        return realm.name().toLowerCase() + capitalized;
    }
}
