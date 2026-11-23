package dev.hugosmp.auctionhouse.match;

/**
 * Keyword matcher over a single tooltip/lore line, built from the server's
 * keyword table. Two implementations exist: the bundled {@link TableMatcher}
 * and a compiled variant produced by {@link MatcherCompiler} from the
 * compacted rows of the same table.
 */
public interface LocaleMatcher {
    /** @return true when the line carries any known currency keyword. */
    boolean test(String line);

    /** Hands the compiled table bytes to the matcher. No-op for the
     *  bundled table matcher. */
    default void configure(byte[] table) {
    }
}
