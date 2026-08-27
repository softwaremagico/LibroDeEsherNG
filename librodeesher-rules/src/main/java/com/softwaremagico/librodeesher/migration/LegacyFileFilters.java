package com.softwaremagico.librodeesher.migration;

import java.nio.file.Path;
import java.util.List;

/**
 * Shared filter reproducing {@code RolemasterFolderStructure}'s legacy "ignored files" list: file
 * names that should never be treated as real rule data even though they live alongside it (e.g. an
 * authoring template).
 */
final class LegacyFileFilters {

    private static final List<String> IGNORED_FILE_NAME_PARTS = List.of("plantilla", "costes", "raciales");

    private LegacyFileFilters() {
        // Utility class.
    }

    static boolean isRealDataFile(Path file) {
        final String lowerCaseName = file.getFileName().toString().toLowerCase();
        return IGNORED_FILE_NAME_PARTS.stream().noneMatch(lowerCaseName::contains);
    }
}
