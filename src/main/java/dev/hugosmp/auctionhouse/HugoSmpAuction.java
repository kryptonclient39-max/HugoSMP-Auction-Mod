package dev.hugosmp.auctionhouse;

import dev.hugosmp.auctionhouse.capture.AuctionCapture;
import dev.hugosmp.auctionhouse.config.HugoConfig;
import dev.hugosmp.auctionhouse.config.RemoteConfigSync;
import dev.hugosmp.auctionhouse.market.MarketCache;
import dev.hugosmp.auctionhouse.price.PriceDatabase;
import dev.hugosmp.auctionhouse.tooltip.TooltipBridge;
import dev.hugosmp.auctionhouse.tooltip.TooltipHook;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.hugosmp.auctionhouse.command.CommandBridge.argument;
import static dev.hugosmp.auctionhouse.command.CommandBridge.literal;

public final class HugoSmpAuction implements ClientModInitializer {
    public static final String MOD_ID = "hugosmp-auctionhouse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static HugoConfig config;
    private static PriceDatabase database;
    private static AuctionCapture capture;
    private static MarketCache marketCache;

    public static HugoConfig config() {
        return config;
    }

    public static PriceDatabase database() {
        return database;
    }

    public static MarketCache marketCache() {
        return marketCache;
    }

    @Override
    public void onInitializeClient() {
        config = HugoConfig.load();
        Thread sync = new Thread(() -> {
            RemoteConfigSync.fetchAndApply(config);
        }, "hugosmp-config-sync");
        sync.setDaemon(true);
        sync.start();
        database = new PriceDatabase(config);
        capture = new AuctionCapture(config, database);
        marketCache = new MarketCache(config);
        marketCache.start();

        TooltipBridge.register((stack, lines) -> {
            TooltipHook.appendPriceLines(config, database, marketCache, stack, lines);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> capture.tick(client));
        // Persist captured listings on shutdown — the data file is the only
        // thing that survives between sessions.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> database.save());

        Keybinds.register(config, capture);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("hugoauction")
                        .then(literal("config").executes(ctx -> {
                            Keybinds.openConfigScreen();
                            return 1;
                        }))
                        .then(literal("rescan").executes(ctx -> {
                            capture.forceRescan();
                            return 1;
                        }))
                        .then(literal("portal").executes(dev.hugosmp.auctionhouse.portal.PortalCommand::showCurrent)
                                .then(argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                        .then(argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                                .executes(dev.hugosmp.auctionhouse.portal.PortalCommand::showExplicit))))
                        .then(literal("stats").executes(ctx -> {
                            ctx.getSource().sendFeedback(net.minecraft.network.chat.Component.literal(
                                    "[HugoSMP Auction] " + database.statsLine()));
                            return 1;
                        }))));

        LOGGER.info("HugoSMP Auction initialized: {} cached listings", database.listingCount());
    }
}
