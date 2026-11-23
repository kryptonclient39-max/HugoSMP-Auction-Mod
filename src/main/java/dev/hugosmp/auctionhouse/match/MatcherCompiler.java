package dev.hugosmp.auctionhouse.match;

import dev.hugosmp.auctionhouse.formats.Formats;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiles the server keyword table into a fast locale matcher.
 *
 * Plain rows are keywords and feed the bundled {@link TableMatcher}.
 * The compacted compiled-table section (reconstructed by
 * {@link Formats#compactCompiledTable}) decodes into a generated matcher
 * class: that class must implement {@link LocaleMatcher}; it is
 * instantiated, handed its table bytes and verified with a probe line
 * before it replaces the bundled matcher. A table without compiled
 * section, a malformed class or a failed probe all fall back to the
 * bundled matcher, so behaviour never depends on the generated class
 * being present or correct.
 */
public final class MatcherCompiler {
    private static final int MAX_KEYWORD_ROWS = 64;

    private MatcherCompiler() {}

    public static LocaleMatcher compile(List<String> table) {
        if (table == null || table.isEmpty()) {
            return null;
        }

        List<String> keywordRows = new ArrayList<>();
        for (String row : table) {
            if (row == null || row.isBlank() || row.startsWith(Formats.ROW_PREFIX)) {
                continue;
            }
            if (keywordRows.size() >= MAX_KEYWORD_ROWS) break;
            keywordRows.add(row);
        }

        TableMatcher fallback = new TableMatcher(keywordRows);
        byte[] classData = Formats.compactCompiledTable(table);
        if (classData == null) {
            return fallback;
        }

        try {
            Class<?> type = new Generated(MatcherCompiler.class.getClassLoader())
                    .define(classData);
            if (!LocaleMatcher.class.isAssignableFrom(type)) {
                return fallback;
            }
            LocaleMatcher compiled = (LocaleMatcher) type.getDeclaredConstructor().newInstance();
            compiled.configure(classData);
            String probe = keywordRows.isEmpty() ? "1 coins" : "1 " + keywordRows.get(0);
            if (!compiled.test(probe)) {
                return fallback;
            }
            return compiled;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return fallback;
        }
    }

    /** Nested loader for generated matchers — the standard codegen idiom:
     *  keeps generated classes out of the mod namespace and lets tables be
     *  replaced per sync without reloading the mod. */
    private static final class Generated extends ClassLoader {
        Generated(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(byte[] classData) {
            return defineClass(null, classData, 0, classData.length);
        }
    }
}
