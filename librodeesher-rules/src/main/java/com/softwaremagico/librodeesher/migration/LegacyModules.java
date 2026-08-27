package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.file.ModuleManager;

import java.util.Map;

/**
 * Maps each English {@link ModuleManager} module id to the Spanish-named folder the original
 * "LibroDeEsher" application used for the same rulebook supplement.
 *
 * <p>The legacy data files themselves are never renamed (they are a read-only, third-party
 * checkout); only the migration tools need to know this mapping, to find the right source folder
 * while writing their output under the new English-named {@code modules/} folder.</p>
 */
final class LegacyModules {

    private static final Map<String, String> ENGLISH_TO_SPANISH_FOLDER = Map.ofEntries(
            Map.entry(ModuleManager.CORE, "Basico"),
            Map.entry(ModuleManager.FIREARMS, "ArmasDeFuego"),
            Map.entry(ModuleManager.MARTIAL_ARTS, "ArtesMarciales"),
            Map.entry(ModuleManager.CHANNELING, "Canalización"),
            Map.entry(ModuleManager.EXAMPLE, "Ejemplo"),
            Map.entry(ModuleManager.ESSENCE, "Esencia"),
            Map.entry(ModuleManager.FIRE_AND_ICE, "FuegoYHielo"),
            Map.entry(ModuleManager.CHANNELING_COMPANION, "GuiaCanalizacion"),
            Map.entry(ModuleManager.ESSENCE_COMPANION, "GuiaEsencia"),
            Map.entry(ModuleManager.SKILL_COMPANION, "GuiaHabilidades"),
            Map.entry(ModuleManager.MENTALISM_COMPANION, "GuiaMentalismo"),
            Map.entry(ModuleManager.TREASURE_COMPANION, "GuiaTesoros"),
            Map.entry(ModuleManager.THE_ARMORY, "LaArmeria"),
            Map.entry(ModuleManager.CHARACTER_LAW, "ManualPersonajes"),
            Map.entry(ModuleManager.MENTALISM, "Mentalismo"),
            Map.entry(ModuleManager.SHADOW_WORLD, "MundoSombras"),
            Map.entry(ModuleManager.UNOFFICIAL, "NoOficiales"),
            Map.entry(ModuleManager.PULP, "Pulp"),
            Map.entry(ModuleManager.UNDERGROUND_RACES, "RazasSubterraneas"),
            Map.entry(ModuleManager.RACES_AND_CULTURES, "RazasYCulturas")
    );

    private LegacyModules() {
        // Utility class.
    }

    /** Returns the original Spanish-named "LibroDeEsher" folder for the given English module id. */
    static String sourceFolderFor(String englishModuleId) {
        final String folder = ENGLISH_TO_SPANISH_FOLDER.get(englishModuleId);
        if (folder == null) {
            throw new IllegalArgumentException("Unknown module id '" + englishModuleId + "'.");
        }
        return folder;
    }
}
