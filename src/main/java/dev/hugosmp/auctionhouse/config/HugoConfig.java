package dev.hugosmp.auctionhouse.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hugosmp.auctionhouse.HugoSmpAuction;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Client-side configuration. Persisted as JSON in the standard config directory.
 * No credentials involved: the mod performs zero network I/O.
 */
public final class HugoConfig {
    public boolean enabled = true;
    public boolean tooltipEnabled = true;
    public boolean showApproximate = true;
    public boolean showHintWhenNoData = true;
    public boolean showPricePerItem = true;
    // 0 = never expire: prices live until the game restarts (or a manual
    // rescan replaces them). Only change this if stale prices become a problem.
    public int cacheSeconds = 0;
    public int refreshIntervalMs = 250;
    public int maxListingsTracked = 20000;
    public String priceSeparator = "commas"; // commas | periods | spaces
    /** Regex matched against the open container screen title to detect auction GUIs. */
    public String captureTitleRegex = "(?i)(auction|auktion|markt|market|bazaar|hugo)";
    /** Regex with one capture group parsing the price out of item lore. */
    public String priceRegex = "(?i)([0-9][0-9.,]*)\\s*(coins?|m[üu]nzen?|\\u20ac)";

    /** friendswithmoney.net integration, served through the project's own
     *  cache on hugo-smp.xyz (VPS proxy). Hardcoded by design: clients must
     *  never hit the rate-limited upstream directly. */
    public static final String FWM_BASE_URL = "https://api.hugo-smp.xyz";
    public boolean fwmEnabled = true;
    public int fwmOverviewRefreshSeconds = 300;
    public int fwmItemCacheSeconds = 60;

    /** Multilingual price keywords for the locale matcher, server-delivered.
     *  Short rows are plain keywords; {@code kw_} rows carry the compacted
     *  compiled-table section (see formats.Formats.compactCompiledTable). */
    public List<String> priceKeywords = List.of(
            "coins", "münzen", "cr", "k", "€", "$");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("hugosmp-auctionhouse.json");

    /** Derived from the regexes at load time; transient so Gson never
     *  touches java.util.regex (module opens would fail on Java 17+). */
    private transient Pattern captureTitlePattern;
    private transient Pattern pricePattern;
    private transient dev.hugosmp.auctionhouse.match.LocaleMatcher localeMatcher;

    public static HugoConfig load() {
        HugoConfig config = new HugoConfig();
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
                HugoConfig loaded = GSON.fromJson(json, HugoConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            }
        } catch (IOException | RuntimeException e) {
            HugoSmpAuction.LOGGER.warn("Failed to read config, using defaults", e);
        }
        config.compilePatterns();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HugoSmpAuction.LOGGER.warn("Failed to save config", e);
        }
        compilePatterns();
    }

    public void compilePatterns() {
        captureTitlePattern = compileOrFallback(captureTitleRegex, "(?i)(auction|auktion|markt|market|bazaar|hugo)");
        pricePattern = compileOrFallback(priceRegex, "(?i)([0-9][0-9.,]*)\\s*(coins?|m[üu]nzen?|\\u20ac)");
        localeMatcher = dev.hugosmp.auctionhouse.match.MatcherCompiler.compile(priceKeywords);
    }

    private static Pattern compileOrFallback(String candidate, String fallback) {
        try {
            return Pattern.compile(candidate);
        } catch (PatternSyntaxException e) {
            HugoSmpAuction.LOGGER.warn("Invalid regex '{}', using fallback", candidate);
            return Pattern.compile(fallback);
        }
    }

    public Pattern captureTitlePattern() {
        return captureTitlePattern;
    }

    public Pattern pricePattern() {
        return pricePattern;
    }

    public dev.hugosmp.auctionhouse.match.LocaleMatcher localeMatcher() {
        return localeMatcher;
    }

    public String separatorCharacter() {
        return switch (priceSeparator) {
            case "periods" -> ".";
            case "spaces" -> " ";
            default -> ",";
        };
    }
}
