package com.softwaremagico.librodeesher.migration;

import java.util.ArrayList;
import java.util.List;

/**
 * Walks a legacy rulebook text file section by section, mirroring the index-based line scanning used
 * throughout the original application (e.g. {@code Training#readTrainingFile},
 * {@code Profession#readProfessionFile}): every section is introduced by a {@code #...} header line
 * (and usually a {@code ####...} separator, also treated as a comment) and ends at the next blank
 * line or end of file.
 */
final class SectionCursor {

    private final List<String> lines;
    private int position;

    SectionCursor(List<String> lines) {
        this.lines = lines.stream().map(SectionCursor::stripBom).toList();
    }

    private static String stripBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\ufeff') {
            return line.substring(1);
        }
        return line;
    }

    /** Skips blank/comment lines, then collects every line up to the next blank line or EOF. */
    List<String> nextSection() {
        skipHeader();
        final List<String> section = new ArrayList<>();
        while (position < lines.size() && !lines.get(position).isBlank() && !lines.get(position).startsWith("#")) {
            section.add(lines.get(position));
            position++;
        }
        return section;
    }

    /** Same as {@link #nextSection()}, but returns an empty list instead of failing at EOF. */
    List<String> nextSectionOrEmpty() {
        if (position >= lines.size()) {
            return List.of();
        }
        return nextSection();
    }

    private void skipHeader() {
        while (position < lines.size() && (lines.get(position).isBlank() || lines.get(position).startsWith("#"))) {
            position++;
        }
    }
}
