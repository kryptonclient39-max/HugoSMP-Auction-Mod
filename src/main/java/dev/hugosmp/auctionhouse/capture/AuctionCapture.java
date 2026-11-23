package dev.hugosmp.auctionhouse.capture;

import dev.hugosmp.auctionhouse.HugoSmpAuction;
import dev.hugosmp.auctionhouse.config.HugoConfig;
import dev.hugosmp.auctionhouse.price.ItemKey;
import dev.hugosmp.auctionhouse.price.PriceDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Watches open container screens whose title matches the auction-house regex
 * and records every slot's price from its lore. Scans run on the client tick
 * (bounded by refreshIntervalMs) and never touch the network: the data is
 * exactly what the server already sent to the client for rendering.
 */
public final class AuctionCapture {
    private final HugoConfig config;
    private final PriceDatabase database;
    private long lastScanMs;

    public AuctionCapture(HugoConfig config, PriceDatabase database) {
        this.config = config;
        this.database = database;
    }

    public void tick(Minecraft client) {
        if (!config.enabled) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastScanMs < config.refreshIntervalMs) {
            return;
        }
        lastScanMs = now;

        if (!(dev.hugosmp.auctionhouse.gui.GuiBridge.currentScreen(client) instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        String title = screen.getTitle().getString();
        Pattern titlePattern = config.captureTitlePattern();
        if (titlePattern == null || !titlePattern.matcher(title).find()) {
            return;
        }
        scanMenu(screen.getMenu());
    }

    /** Forces an immediate scan of the currently open screen, bypassing the interval. */
    public void forceRescan() {
        Minecraft client = Minecraft.getInstance();
        if (dev.hugosmp.auctionhouse.gui.GuiBridge.currentScreen(client) instanceof AbstractContainerScreen<?> screen) {
            scanMenu(screen.getMenu());
        }
    }

    private void scanMenu(AbstractContainerMenu menu) {
        // Menu slot counts are small (<= 273 in creative-like GUIs); a hard cap
        // guards against pathological custom menus.
        int bound = Math.min(menu.slots.size(), 500);
        for (int i = 0; i < bound; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            parseStack(stack);
        }
    }

    private void parseStack(ItemStack stack) {
        Long price = LorePriceParser.parse(config, stack);
        if (price == null) {
            return;
        }
        ItemKey key = ItemKey.exact(stack);
        database.record(key, price, stack.getCount());
    }
}
