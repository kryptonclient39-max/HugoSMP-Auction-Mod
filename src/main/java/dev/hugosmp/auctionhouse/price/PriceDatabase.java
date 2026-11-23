package dev.hugosmp.auctionhouse.price;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.hugosmp.auctionhouse.HugoSmpAuction;
import dev.hugosmp.auctionhouse.config.HugoConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory cache of observed auction listings, persisted to disk. All reads
 * happen on the render thread from the tooltip hook, so the maps are concurrent
 * and no method here performs I/O on the hot path.
 */
public final class PriceDatabase {
    public static final class Aggregate {
        public long lowestPrice;
        public int listingCount;
        public long updatedAtMs;
    }

    private static final Gson GSON = new GsonBuilder().create();
    private static final Type LISTING_LIST = new TypeToken<List<Listing>>() { }.getType();

    private final HugoConfig config;
    private final Path dataPath =
            FabricLoader.getInstance().getConfigDir().resolve("hugosmp-auctionhouse-data.json");

    private final ConcurrentMap<ItemKey, Aggregate> exact = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Aggregate> approximate = new ConcurrentHashMap<>();
    private final List<Listing> rawListings = new ArrayList<>();

    public PriceDatabase(HugoConfig config) {
        this.config = config;
        load();
    }

    /** Records one observed listing. Called on the client thread during capture. */
    public void record(ItemKey key, long price, int stackCount) {
        if (price <= 0 || stackCount <= 0) {
            return;
        }
        long now = System.currentTimeMillis();

        Aggregate agg = exact.computeIfAbsent(key, k -> new Aggregate());
        synchronized (agg) {
            if (agg.lowestPrice == 0 || price < agg.lowestPrice) {
                agg.lowestPrice = price;
            }
            agg.listingCount++;
            agg.updatedAtMs = now;
        }

        if (config.showApproximate) {
            Aggregate approx = approximate.computeIfAbsent(key.id, k -> new Aggregate());
            synchronized (approx) {
                if (approx.lowestPrice == 0 || price < approx.lowestPrice) {
                    approx.lowestPrice = price;
                }
                approx.listingCount++;
                approx.updatedAtMs = now;
            }
        }

        synchronized (rawListings) {
            rawListings.add(new Listing(key.id, key.componentHash, price, stackCount, now));
            trimRawListings();
        }
    }

    private void trimRawListings() {
        while (rawListings.size() > config.maxListingsTracked) {
            rawListings.remove(0);
        }
    }

    /** Exact lookup, TTL-guarded. */
    public Optional<Aggregate> lookupExact(ItemKey key) {
        return fresh(exact.get(key));
    }

    /** Approximate (id-only) lookup, TTL-guarded. */
    public Optional<Aggregate> lookupApproximate(ItemKey key) {
        if (!config.showApproximate) {
            return Optional.empty();
        }
        return fresh(approximate.get(key.id));
    }

    private Optional<Aggregate> fresh(Aggregate agg) {
        if (agg == null) {
            return Optional.empty();
        }
        // cacheSeconds == 0 means "never expire": prices live until the game
        // restarts or a manual rescan (J) replaces them via record().
        if (config.cacheSeconds <= 0) {
            return Optional.of(copyOf(agg));
        }
        long ttlMs = config.cacheSeconds * 1000L;
        synchronized (agg) {
            if (agg.updatedAtMs == 0 || System.currentTimeMillis() - agg.updatedAtMs > ttlMs) {
                return Optional.empty();
            }
            return Optional.of(copyOf(agg));
        }
    }

    private static Aggregate copyOf(Aggregate agg) {
        synchronized (agg) {
            Aggregate copy = new Aggregate();
            copy.lowestPrice = agg.lowestPrice;
            copy.listingCount = agg.listingCount;
            copy.updatedAtMs = agg.updatedAtMs;
            return copy;
        }
    }


    public int listingCount() {
        synchronized (rawListings) {
            return rawListings.size();
        }
    }

    public int distinctItemCount() {
        return exact.size();
    }

    public String statsLine() {
        return listingCount() + " listings, " + exact.size() + " distinct items cached";
    }

    public void save() {
        try {
            List<Listing> snapshot;
            synchronized (rawListings) {
                snapshot = new ArrayList<>(rawListings);
            }
            String json = GSON.toJson(snapshot, LISTING_LIST);
            Path tmp = dataPath.resolveSibling(dataPath.getFileName() + ".tmp");
            Files.createDirectories(dataPath.getParent());
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            Files.move(tmp, dataPath, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            HugoSmpAuction.LOGGER.warn("Failed to persist listing cache", e);
        }
    }

    private void load() {
        if (!Files.exists(dataPath)) {
            return;
        }
        try {
            String json = Files.readString(dataPath, StandardCharsets.UTF_8);
            List<Listing> listings = GSON.fromJson(json, LISTING_LIST);
            if (listings == null) {
                return;
            }
            long ttlMs = config.cacheSeconds * 1000L;
            long now = System.currentTimeMillis();
            for (Listing listing : listings) {
                if (listing.price <= 0) {
                    continue;
                }
                // cacheSeconds == 0 = never expire: reload everything the
                // last session captured, regardless of age.
                if (ttlMs > 0 && now - listing.seenAtMs > ttlMs) {
                    continue; // stale: drop silently, never re-serve
                }
                ItemKey key = new ItemKey(listing.itemId, listing.componentHash, true);
                record(key, listing.price, listing.stackCount);
            }
            HugoSmpAuction.LOGGER.info("Restored {} listings from cache", listings.size());
        } catch (IOException | RuntimeException e) {
            HugoSmpAuction.LOGGER.warn("Failed to load listing cache, starting fresh", e);
        }
    }
}
