package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.file.ModuleManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves which {@code categorias.txt} files contribute to a given module, shared by every
 * migration tool that needs to walk the "Habilidades" column of that file
 * ({@link CategoryMigrationTool}, {@link SkillMigrationTool}).
 *
 * <p>{@link ModuleManager#CORE} is special-cased to also include the base
 * {@code rolemaster/categorias.txt} file (outside any module folder), which the legacy application
 * always loaded first regardless of which modules were enabled.</p>
 */
final class LegacyCategoriesFiles {

    private static final String CATEGORIES_FILE = "categorias.txt";

    private LegacyCategoriesFiles() {
        // Utility class.
    }

    /**
     * @param englishModuleId English {@link ModuleManager} module id (e.g. {@link ModuleManager#CORE}).
     */
    static List<Path> forModule(String englishModuleId, Path rolemasterDir, Path modulosDir) {
        final List<Path> files = new ArrayList<>();
        if (ModuleManager.CORE.equals(englishModuleId)) {
            addIfExists(files, rolemasterDir.resolve(CATEGORIES_FILE));
        }
        final String sourceFolder = LegacyModules.sourceFolderFor(englishModuleId);
        addIfExists(files, modulosDir.resolve(sourceFolder).resolve(CATEGORIES_FILE));
        return files;
    }

    private static void addIfExists(List<Path> files, Path candidate) {
        if (Files.isRegularFile(candidate)) {
            files.add(candidate);
        }
    }
}
