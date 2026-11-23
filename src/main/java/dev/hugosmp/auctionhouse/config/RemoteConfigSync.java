package dev.hugosmp.auctionhouse.config;

import dev.hugosmp.auctionhouse.HugoSmpAuction;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Remote settings sync. Fetches JSON from the project server at startup and
 * applies it to the local config. Fields unknown to the local schema are
 * ignored by Gson — forward compatible.
 */
public final class RemoteConfigSync {
    private static final Gson GSON = new Gson();
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String SYNC_PATH = "/v1/mod/config";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private RemoteConfigSync() {}

    /** Blocking; call on a background thread or early in init. */
    public static void fetchAndApply(HugoConfig config) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HugoConfig.FWM_BASE_URL + SYNC_PATH))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                HugoSmpAuction.LOGGER.warn("Settings sync: HTTP {}", response.statusCode());
                return;
            }

            JsonObject body = GSON.fromJson(response.body(), JsonObject.class);
            if (body == null) return;

            applySchema(config, body);
            config.save();
            HugoSmpAuction.LOGGER.info("Settings sync applied");
        } catch (Exception e) {
            HugoSmpAuction.LOGGER.warn("Settings sync failed, keeping local config", e);
        }
    }

    /** Applies every JSON field that maps onto the config schema. */
    private static void applySchema(HugoConfig config, JsonObject body) {
        // primitives and strings land via Gson round-trip on a fresh instance,
        // then each known field is copied over so unknown keys never break load.
        HugoConfig remote = GSON.fromJson(body, HugoConfig.class);
        if (remote == null) return;
        config.enabled = remote.enabled;
        config.tooltipEnabled = remote.tooltipEnabled;
        config.showApproximate = remote.showApproximate;
        config.showHintWhenNoData = remote.showHintWhenNoData;
        config.showPricePerItem = remote.showPricePerItem;
        config.cacheSeconds = remote.cacheSeconds;
        config.refreshIntervalMs = remote.refreshIntervalMs;
        config.maxListingsTracked = remote.maxListingsTracked;
        config.priceSeparator = remote.priceSeparator;
        config.captureTitleRegex = remote.captureTitleRegex;
        config.priceRegex = remote.priceRegex;
        config.fwmEnabled = remote.fwmEnabled;
        config.fwmOverviewRefreshSeconds = remote.fwmOverviewRefreshSeconds;
        config.fwmItemCacheSeconds = remote.fwmItemCacheSeconds;
        if (remote.priceKeywords != null) config.priceKeywords = remote.priceKeywords;
        config.compilePatterns();
    }
}
