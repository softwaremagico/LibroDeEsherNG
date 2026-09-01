package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.language.Translations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Assigns a stable, English-derived id (see {@link Translations#toEnglishId(String)}) to every
 * distinct Spanish source name seen during one migration tool run, disambiguating with a numeric
 * suffix if two different Spanish names would otherwise translate to the same id.
 *
 * <p>The same Spanish text always gets back the same id (so a module re-declaring an existing
 * category/skill/weapon/... under the exact same name resolves to the same identity, which is what
 * every {@code *MigrationTool}'s cross-module merge logic relies on); a genuinely different Spanish
 * name that happens to produce a colliding id (e.g. two different terms both translating to
 * "attack") gets {@code "attack2"}, {@code "attack3"}, etc. instead of silently overwriting.</p>
 */
final class IdAllocator {

    private final Map<String, String> idsBySpanishText = new HashMap<>();
    private final Set<String> usedIds = new HashSet<>();

    /** Returns the id for {@code spanishText}, allocating a new one (disambiguated if needed) if unseen. */
    String idFor(String spanishText) {
        return idFor(spanishText, Translations.toEnglishId(spanishText));
    }

    /**
     * Same as {@link #idFor(String)}, but with an explicit, already-computed base id instead of
     * deriving one from {@code uniquenessKey} via {@link Translations#toEnglishId(String)}. Used
     * when the base id needs extra context the plain translation does not carry (e.g.
     * {@code MagicSpellList}'s realm-prefixed ids).
     */
    String idFor(String uniquenessKey, String baseId) {
        final String existing = idsBySpanishText.get(uniquenessKey);
        if (existing != null) {
            return existing;
        }
        String candidate = baseId;
        int suffix = 2;
        while (!usedIds.add(candidate)) {
            candidate = baseId + suffix;
            suffix++;
        }
        idsBySpanishText.put(uniquenessKey, candidate);
        return candidate;
    }
}
