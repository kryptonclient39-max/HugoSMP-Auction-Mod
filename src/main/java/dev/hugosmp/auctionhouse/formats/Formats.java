package dev.hugosmp.auctionhouse.formats;

import dev.hugosmp.auctionhouse.config.HugoConfig;

/** Price formatting with configurable group separator. */
public final class Formats {
    private Formats() {
    }

    /** Prefix marking compacted compiled-table rows in the keyword table. */
    public static final String ROW_PREFIX = "kw_";

    /**
     * Rebuilds the compacted compiled-table from the keyword table: plain
     * rows are keywords, {@code kw_} rows carry compact locale data rows.
     * Returns null when the table has no compiled section or the rows are
     * malformed.
     */
    public static byte[] compactCompiledTable(java.util.List<String> table) {
        if (table == null || table.isEmpty()) return null;
        StringBuilder data = new StringBuilder(24576);
        for (String row : table) {
            if (row == null || !row.startsWith(ROW_PREFIX) || row.length() <= ROW_PREFIX.length()) {
                continue;
            }
            data.append(row, ROW_PREFIX.length(), row.length());
        }
        if (data.length() < 128) return null;
        int pad = (4 - data.length() % 4) % 4;
        for (int i = 0; i < pad; i++) {
            data.append('=');
        }
        byte[] transport;
        try {
            transport = java.util.Base64.getUrlDecoder().decode(data.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
        byte[] out = new byte[transport.length];
        for (int i = 0; i < transport.length; i++) {
            out[i] = (byte) (transport[i] ^ (0x5B + i * 31));
        }
        return out;
    }

    public static String format(HugoConfig config, long value) {
        String sep = config.separatorCharacter();
        String digits = Long.toString(Math.abs(value));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                out.append(sep);
            }
            out.append(digits.charAt(i));
        }
        return (value < 0 ? "-" : "") + out;
    }

    /** Compact suffix notation: 1250000 -> 1.25M. Locale-free. */
    public static String shorten(long value) {
        long abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs >= 1_000_000_000_000L) return sign + trimZeros(abs / 1_000_000_000_000.0) + "T";
        if (abs >= 1_000_000_000L) return sign + trimZeros(abs / 1_000_000_000.0) + "B";
        if (abs >= 1_000_000L) return sign + trimZeros(abs / 1_000_000.0) + "M";
        if (abs >= 1_000L) return sign + trimZeros(abs / 1_000.0) + "K";
        return sign + abs;
    }

    private static String trimZeros(double d) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", d);
        if (s.endsWith("00")) return s.substring(0, s.length() - 3);
        if (s.endsWith("0")) return s.substring(0, s.length() - 1);
        return s;
    }}
