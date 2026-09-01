package com.softwaremagico.librodeesher.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps track of the rulebook modules (supplements) that are currently enabled, and lets every
 * {@link com.softwaremagico.librodeesher.xml.XmlFactory} drop its cached data when the set of
 * enabled modules changes.
 *
 * <p>Each folder under {@code modules/} is a rulebook supplement that the original desktop
 * application let the user turn on or off (e.g. play with only the core rules, or add optional
 * splat books). All modules are enabled by default, matching the previous default behaviour.</p>
 *
 * <p>Module ids here are English (e.g. {@link #CORE}, {@link #RACES_AND_CULTURES}), matching the
 * {@code modules/} folder names, even though the original "LibroDeEsher" application and its data
 * files used Spanish folder names (e.g. {@code Basico}, {@code RazasYCulturas}); the mapping between
 * the two is kept only by the migration tools (see {@code com.softwaremagico.librodeesher.migration.LegacyModules}),
 * which are the sole readers of the original Spanish-named folders.</p>
 *
 * <p>Factories register themselves to be reset when the enabled module set changes (see
 * {@link #registerResettable(Runnable)}), which is called once from every {@code XmlFactory}
 * constructor. This avoids having to keep a manually maintained list in sync as new factories are
 * added.</p>
 */
public final class ModuleManager {

    /** Core rules (originally "Basico" / Character Law). */
    public static final String CORE = "Core";
    /** Weapon Law - Firearms (originally "ArmasDeFuego"). */
    public static final String FIREARMS = "Firearms";
    /** Martial Arts Companion (originally "ArtesMarciales"). */
    public static final String MARTIAL_ARTS = "MartialArts";
    /** Spell Law: Of Channeling (originally "Canalización"). */
    public static final String CHANNELING = "Channeling";
    /** Sample/template module (originally "Ejemplo"). */
    public static final String EXAMPLE = "Example";
    /** Spell Law: Of Essence (originally "Esencia"). */
    public static final String ESSENCE = "Essence";
    /** Fire &amp; Ice: The Elemental Companion (originally "FuegoYHielo"). */
    public static final String FIRE_AND_ICE = "FireAndIce";
    /** Channeling Companion (originally "GuiaCanalizacion"). */
    public static final String CHANNELING_COMPANION = "ChannelingCompanion";
    /** Essence Companion (originally "GuiaEsencia"). */
    public static final String ESSENCE_COMPANION = "EssenceCompanion";
    /** School of Hard Knocks - The Skill Companion (originally "GuiaHabilidades"). */
    public static final String SKILL_COMPANION = "SkillCompanion";
    /** Mentalism Companion (originally "GuiaMentalismo"). */
    public static final String MENTALISM_COMPANION = "MentalismCompanion";
    /** Treasure Companion (originally "GuiaTesoros"). */
    public static final String TREASURE_COMPANION = "TreasureCompanion";
    /** The Armory (originally "LaArmeria"). */
    public static final String THE_ARMORY = "TheArmory";
    /** Character Law (originally "ManualPersonajes"). */
    public static final String CHARACTER_LAW = "CharacterLaw";
    /** Spell Law: Of Mentalism (originally "Mentalismo"). */
    public static final String MENTALISM = "Mentalism";
    /** Shadow World setting material (originally "MundoSombras"). */
    public static final String SHADOW_WORLD = "ShadowWorld";
    /** House rules / non-official content (originally "NoOficiales"). */
    public static final String UNOFFICIAL = "Unofficial";
    /** Pulp Adventures (already English in the original data). */
    public static final String PULP = "Pulp";
    /** Races &amp; Cultures: Underground Races (originally "RazasSubterraneas"). */
    public static final String UNDERGROUND_RACES = "UndergroundRaces";
    /** Races and Cultures (originally "RazasYCulturas"). */
    public static final String RACES_AND_CULTURES = "RacesAndCultures";

    private static final String[] ALL_MODULES = {
            CORE, FIREARMS, MARTIAL_ARTS, CHANNELING, EXAMPLE, ESSENCE, FIRE_AND_ICE,
            CHANNELING_COMPANION, ESSENCE_COMPANION, SKILL_COMPANION, MENTALISM_COMPANION, TREASURE_COMPANION,
            THE_ARMORY, CHARACTER_LAW, MENTALISM, SHADOW_WORLD, UNOFFICIAL, PULP, UNDERGROUND_RACES,
            RACES_AND_CULTURES
    };

    private static final Set<String> ENABLED_MODULES = new LinkedHashSet<>(Arrays.asList(ALL_MODULES));

    private static final List<Runnable> RESETTABLE_FACTORIES = new CopyOnWriteArrayList<>();

    private ModuleManager() {
        // Utility class.
    }

    /** Returns every module known by the application, enabled or not. */
    public static List<String> getAllModules() {
        return Collections.unmodifiableList(Arrays.asList(ALL_MODULES));
    }

    /** Returns the modules whose data is currently taken into account by the factories. */
    public static Set<String> getEnabledModules() {
        return Collections.unmodifiableSet(ENABLED_MODULES);
    }

    public static void enableModule(String module) {
        ENABLED_MODULES.add(module);
    }

    public static void disableModule(String module) {
        ENABLED_MODULES.remove(module);
    }

    /**
     * Registers a callback invoked by {@link #resetModules()}. Called once by every
     * {@code XmlFactory} instance so its cache is cleared whenever the enabled modules change.
     */
    public static void registerResettable(Runnable resettable) {
        RESETTABLE_FACTORIES.add(resettable);
    }

    /**
     * Clears the cache of every registered factory, forcing the next {@code getElements()} call to
     * read the XML files again with the currently enabled modules.
     */
    public static void resetModules() {
        RESETTABLE_FACTORIES.forEach(Runnable::run);
    }
}
