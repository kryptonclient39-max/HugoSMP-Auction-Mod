package dev.hugosmp.auctionhouse.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bundled matcher: case-insensitive containment over the keyword rows of
 * the server table. Returned by {@link MatcherCompiler} whenever no
 * compiled variant is present or the compiled variant fails its probe.
 */
public final class TableMatcher implements LocaleMatcher {
    private final List<String> keywords = new ArrayList<>();

    TableMatcher(List<String> rows) {
        for (String row : rows) {
            if (row != null && !row.isBlank()) {
                keywords.add(row.toLowerCase(Locale.ROOT));
            }
        }
    }

    @Override
    public boolean test(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
