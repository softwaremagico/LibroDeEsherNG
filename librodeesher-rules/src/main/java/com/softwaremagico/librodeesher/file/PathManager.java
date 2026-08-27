package com.softwaremagico.librodeesher.file;

/**
 * Computes the classpath-relative paths where rulebook data lives.
 *
 * <p>Every path returned by this class is a <em>classpath resource path</em> (forward slashes, no
 * leading slash), not a filesystem path. This is a deliberate difference from the legacy application
 * (which located an external {@code rolemaster/} folder next to the executable using heuristics
 * based on {@code ProtectionDomain}): resolving data purely through the classpath is what allows the
 * exact same code to run unmodified inside a desktop jar or an Android package (APK/AAR) later on.</p>
 */
public final class PathManager {

    /**
     * Root classpath folder, under which every rulebook module has its own sub-folder. Matches the
     * {@code modulo/} resource copied into the jar by the Maven build (see the module's pom.xml).
     */
    public static final String MODULES_FOLDER = "modulo";

    /** Name of the file describing the available modules (id, display name, folder). */
    public static final String MODULES_DEFINITION_FILE = "modules.xml";

    private PathManager() {
        // Utility class.
    }

    /**
     * Returns the classpath folder for a given rulebook module, e.g. {@code "modulo/Basico/"}.
     *
     * @param moduleName folder name of the module (as declared in {@code modulo/modules.xml}), or
     *                    {@code null} to get the root modules folder.
     */
    public static String getModulePath(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            return MODULES_FOLDER + "/";
        }
        return MODULES_FOLDER + "/" + moduleName + "/";
    }

    /** Returns the classpath path of the module definition file, e.g. {@code "modulo/modules.xml"}. */
    public static String getModulesDefinitionPath() {
        return getModulePath(null) + MODULES_DEFINITION_FILE;
    }
}
