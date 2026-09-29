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
     * {@code modules/} resource copied into the jar by the Maven build (see the module's pom.xml).
     */
    public static final String MODULES_FOLDER = "modules";

    private PathManager() {
        // Utility class.
    }

    /**
     * Returns the classpath folder for a given rulebook module, e.g. {@code "modules/Core/"}.
     *
     * <p>The module folder names are the ids declared in {@link ModuleManager} (which keeps the list
     * in code rather than reading a descriptor file, so a module without any data file — the sample
     * {@code Example} module — needs no folder at all).</p>
     *
     * @param moduleName folder name of the module, as declared in {@link ModuleManager}, or
     *                    {@code null} to get the root modules folder.
     */
    public static String getModulePath(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            return MODULES_FOLDER + "/";
        }
        return MODULES_FOLDER + "/" + moduleName + "/";
    }
}
