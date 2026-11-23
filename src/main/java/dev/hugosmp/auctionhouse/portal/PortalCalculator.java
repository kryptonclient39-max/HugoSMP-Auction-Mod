package dev.hugosmp.auctionhouse.portal;

/**
 * Nether portal link math (Java ratio 8:1). Pure functions, no game
 * objects — the command layer feeds coordinates, this layer returns the
 * linked side and the vanilla search windows.
 */
public final class PortalCalculator {
    public static final int RATIO = 8;

    /** Vanilla portal search radius when linking INTO the nether. */
    public static final int NETHER_SEARCH = 16;
    /** ... and when linking back OUT into the overworld. */
    public static final int OVERWORLD_SEARCH = 128;

    /** from = where the player (or the given coords) are, to = linked side. */
    public record Link(double fromX, double fromZ, double toX, double toZ,
                       boolean toNether) {}

    private PortalCalculator() {}

    public static Link fromOverworld(double x, double z) {
        return new Link(x, z, x / RATIO, z / RATIO, true);
    }

    public static Link fromNether(double x, double z) {
        return new Link(x, z, x * RATIO, z * RATIO, false);
    }

    public static int searchRadius(Link link) {
        return link.toNether() ? NETHER_SEARCH : OVERWORLD_SEARCH;
    }

    /** Rounded whole-block coordinate for chat display. */
    public static long coord(double v) {
        return Math.round(v);
    }

    /** Full three-axis link result. Y is never scaled (Java ratio). */
    public record Result(double fromX, double fromY, double fromZ,
                         double toX, double toY, double toZ,
                         boolean toNether) {}

    public static Result convert(double x, double y, double z, boolean toNether) {
        return toNether
                ? new Result(x, y, z, x / RATIO, y, z / RATIO, true)
                : new Result(x, y, z, x * RATIO, y, z * RATIO, false);
    }

    /** Parses user input leniently: negatives and decimals allowed.
     *  Returns null for empty or malformed text. */
    public static Double parse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Double.valueOf(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
