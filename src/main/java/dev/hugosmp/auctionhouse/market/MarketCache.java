package dev.hugosmp.auctionhouse.market;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hugosmp.auctionhouse.HugoSmpAuction;
import dev.hugosmp.auctionhouse.config.HugoConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background fetcher for friendswithmoney.net market stats. Single daemon
 * thread, two caches: a full-market overview snapshot refreshed on a fixed
 * schedule, and per-item 1h/24h stats with a short TTL. All failures are
 * logged and swallowed — the game never waits on this thread.
 */
public final class MarketCache {
    public static final class ItemStats {
        public final long avg1h;
        public final long max1h;
        public final long count1h;
        public final long avg24h;
        public final long max24h;
        public final long count24h;
        public final long volume24h;
        public final long orders1h;
        public final long orders24h;
        public final long fetchedAtMs;

        ItemStats(long avg1h, long max1h, long count1h,
                  long avg24h, long max24h, long count24h, long volume24h,
                  long orders1h, long orders24h, long fetchedAtMs) {
            this.avg1h = avg1h;
            this.max1h = max1h;
            this.count1h = count1h;
            this.avg24h = avg24h;
            this.max24h = max24h;
            this.count24h = count24h;
            this.volume24h = volume24h;
            this.orders1h = orders1h;
            this.orders24h = orders24h;
            this.fetchedAtMs = fetchedAtMs;
        }

        public boolean isExpired(long ttlSeconds) {
            return System.currentTimeMillis() - fetchedAtMs > ttlSeconds * 1000L;
        }
    }

    private static final int MAX_RESPONSE_BYTES = 32 * 1024 * 1024;
    private static final Gson GSON = new Gson();

    private final HttpClient http;
    private final ScheduledExecutorService executor;
    private final HugoConfig config;

    private volatile Map<String, JsonObject> overviewSnapshot = Map.of();
    private final ConcurrentHashMap<String, ItemStats> itemCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> inFlight = new ConcurrentHashMap<>();

    public MarketCache(HugoConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hugosmp-market-cache");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        long refresh = Math.max(30, config.fwmOverviewRefreshSeconds);
        executor.scheduleWithFixedDelay(this::refreshOverview, 0, refresh, TimeUnit.SECONDS);
        // One-time registry seed: hand the VPS cache every vanilla item key
        // so it can pre-crawl everything (the overview only lists ~60
        // marketplace items, but the tooltip resolves ANY item).
        executor.schedule(this::seedRegistry, 15, TimeUnit.SECONDS);
        HugoSmpAuction.LOGGER.info("MarketCache started: {}s overview refresh", refresh);
    }

    private void seedRegistry() {
        if (!config.fwmEnabled) {
            return;
        }
        try {
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                keys.add(BuiltInRegistries.ITEM.getKey(item).getPath());
            }
            // Potions ride the "#variant" scheme; FWM normalizes strong_/long_
            // prefixes away, so the plain variant ids suffice.
            java.util.List<String> forms = java.util.List.of("potion", "splash_potion", "lingering_potion");
            for (Potion potion : BuiltInRegistries.POTION) {
                String path = BuiltInRegistries.POTION.getKey(potion).getPath();
                if (path.startsWith("strong_") || path.startsWith("long_")) {
                    continue;
                }
                for (String form : forms) {
                    keys.add(form + "#" + path);
                }
            }
            post("/api/seed", GSON.toJson(java.util.Map.of("itemKeys", keys)));
            HugoSmpAuction.LOGGER.info("FWM seed sent: {} item keys", keys.size());
        } catch (Exception e) {
            HugoSmpAuction.LOGGER.info("FWM seed failed: {}", e.toString());
        }
    }

    private void post(String path, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(HugoConfig.FWM_BASE_URL + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("User-Agent", "HugoSmpAuctionMod/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<InputStream> response = http.send(request,
                HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " for " + path);
            }
            in.readAllBytes();
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public Optional<ItemStats> stats(String itemKey) {
        if (!config.fwmEnabled) {
            return Optional.empty();
        }
        ItemStats cached = itemCache.get(itemKey);
        if (cached != null && !cached.isExpired(config.fwmItemCacheSeconds)) {
            return Optional.of(cached);
        }
        if (inFlight.putIfAbsent(itemKey, Boolean.TRUE) == null) {
            executor.execute(() -> {
                try {
                    ItemStats fresh = fetchItemStats(itemKey);
                    if (fresh != null) {
                        itemCache.put(itemKey, fresh);
                    }
                } catch (Exception e) {
                    HugoSmpAuction.LOGGER.info("FWM fetch failed for {}: {}", itemKey, e.toString());
                } finally {
                    inFlight.remove(itemKey);
                }
            });
        }
        return Optional.ofNullable(cached);
    }

    public Optional<JsonObject> overviewEntry(String itemKey) {
        return Optional.ofNullable(overviewSnapshot.get(itemKey));
    }

    public int overviewSize() {
        return overviewSnapshot.size();
    }

    private void refreshOverview() {
        if (!config.fwmEnabled) {
            return;
        }
        try {
            String body = get(HugoConfig.FWM_BASE_URL + "/api/marketplace/overview");
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("ok") || !root.get("ok").getAsBoolean()) {
                HugoSmpAuction.LOGGER.debug("FWM overview: ok=false");
                return;
            }
            JsonArray items = root.getAsJsonArray("items");
            Map<String, JsonObject> next = new HashMap<>();
            for (JsonElement el : items) {
                JsonObject obj = el.getAsJsonObject();
                String key = obj.has("itemKey") ? obj.get("itemKey").getAsString() : null;
                if (key != null && !key.isEmpty()) {
                    next.put(key, obj);
                }
            }
            overviewSnapshot = Map.copyOf(next);
            HugoSmpAuction.LOGGER.info("FWM overview refreshed: {} items", next.size());
        } catch (Exception e) {
            executor.schedule(this::refreshOverview, 60, TimeUnit.SECONDS);
            HugoSmpAuction.LOGGER.info("FWM overview fetch failed: {}", e.toString());
        }
    }

    private ItemStats fetchItemStats(String itemKey) throws IOException, InterruptedException {
        String url = HugoConfig.FWM_BASE_URL + "/api/marketplace/items/"
                + URLEncoder.encode(itemKey, StandardCharsets.UTF_8) + "/stats";
        String body = get(url);
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        if (!root.has("ok") || !root.get("ok").getAsBoolean()) {
            return null;
        }
        JsonArray stats = root.getAsJsonArray("stats");
        long avg1h = 0, max1h = 0, count1h = 0, avg24h = 0, max24h = 0, count24h = 0, vol24h = 0;
        long orders1h = 0, orders24h = 0;
        for (JsonElement el : stats) {
            JsonObject period = el.getAsJsonObject();
            String span = period.has("period") ? period.get("period").getAsString() : "";
            if ("1h".equals(span)) {
                avg1h = getLong(period, "ahAvgPrice");
                max1h = getLong(period, "ahMaxPrice");
                count1h = getLong(period, "ahEntryCount");
                orders1h = getLong(period, "ordersAvgPrice");
            } else if ("24h".equals(span)) {
                avg24h = getLong(period, "ahAvgPrice");
                max24h = getLong(period, "ahMaxPrice");
                count24h = getLong(period, "ahEntryCount");
                vol24h = getLong(period, "ahMarketVolume");
                orders24h = getLong(period, "ordersAvgPrice");
            }
        }
        return new ItemStats(avg1h, max1h, count1h, avg24h, max24h, count24h, vol24h,
                orders1h, orders24h, System.currentTimeMillis());
    }

    private static long getLong(JsonObject obj, String name) {
        if (!obj.has(name) || obj.get(name).isJsonNull()) {
            return 0;
        }
        // FWM ships every stat as a JSON string ("1536.43057170") — parse
        // through double, since getAsLong on a string primitive throws.
        try {
            return (long) Double.parseDouble(obj.get(name).getAsString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "HugoSmpAuctionMod/1.0")
                .GET()
                .build();
        HttpResponse<InputStream> response = http.send(request,
                HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " from " + url);
            }
            byte[] bytes = in.readNBytes(MAX_RESPONSE_BYTES + 1);
            if (bytes.length > MAX_RESPONSE_BYTES) {
                throw new IOException("Response exceeds 32MB cap");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}