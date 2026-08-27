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
 * <p>Each folder under {@code modulo/} is a rulebook supplement that the original desktop
 * application let the user turn on or off (e.g. play with only the core rules, or add optional
 * splat books). All modules are enabled by default, matching the previous default behaviour.</p>
 *
 * <p>Unlike ThinkMachine-4E, where {@code ModuleManager#resetModules()} hard-codes the list of every
 * factory to reset, factories here register themselves (see {@link #registerResettable(Runnable)}),
 * which is called once from every {@code XmlFactory} constructor. This avoids having to keep a
 * manually maintained list in sync as new factories are added.</p>
 */
public final class ModuleManager {

    public static final String BASICO = "Basico";
    public static final String ARMAS_DE_FUEGO = "ArmasDeFuego";
    public static final String ARTES_MARCIALES = "ArtesMarciales";
    public static final String CANALIZACION = "Canalización";
    public static final String EJEMPLO = "Ejemplo";
    public static final String ESENCIA = "Esencia";
    public static final String FUEGO_Y_HIELO = "FuegoYHielo";
    public static final String GUIA_CANALIZACION = "GuiaCanalizacion";
    public static final String GUIA_ESENCIA = "GuiaEsencia";
    public static final String GUIA_HABILIDADES = "GuiaHabilidades";
    public static final String GUIA_MENTALISMO = "GuiaMentalismo";
    public static final String GUIA_TESOROS = "GuiaTesoros";
    public static final String LA_ARMERIA = "LaArmeria";
    public static final String MANUAL_PERSONAJES = "ManualPersonajes";
    public static final String MENTALISMO = "Mentalismo";
    public static final String MUNDO_SOMBRAS = "MundoSombras";
    public static final String NO_OFICIALES = "NoOficiales";
    public static final String PULP = "Pulp";
    public static final String RAZAS_SUBTERRANEAS = "RazasSubterraneas";
    public static final String RAZAS_Y_CULTURAS = "RazasYCulturas";

    private static final String[] ALL_MODULES = {
            BASICO, ARMAS_DE_FUEGO, ARTES_MARCIALES, CANALIZACION, EJEMPLO, ESENCIA, FUEGO_Y_HIELO,
            GUIA_CANALIZACION, GUIA_ESENCIA, GUIA_HABILIDADES, GUIA_MENTALISMO, GUIA_TESOROS, LA_ARMERIA,
            MANUAL_PERSONAJES, MENTALISMO, MUNDO_SOMBRAS, NO_OFICIALES, PULP, RAZAS_SUBTERRANEAS,
            RAZAS_Y_CULTURAS
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
