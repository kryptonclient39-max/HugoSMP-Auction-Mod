package dev.hugosmp.auctionhouse;

import dev.hugosmp.auctionhouse.capture.AuctionCapture;
import dev.hugosmp.auctionhouse.config.HugoConfig;
import dev.hugosmp.auctionhouse.config.screen.HugoConfigScreen;
import dev.hugosmp.auctionhouse.keybinds.KeybindsBridge;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Keybinds: config screen (O) and forced rescan (J). */
public final class Keybinds {
    private static final Logger LOGGER = LoggerFactory.getLogger(Keybinds.class);
    private static final String CATEGORY = "key.categories.hugosmp_auctionhouse";
    private static KeyMapping configKey;
    private static KeyMapping rescanKey;

    private Keybinds() {
    }

    public static void register(HugoConfig configUnused, AuctionCapture capture) {
        // KeyMapping construction is versioned: 1.21.9+ switched the category
        // parameter from String to a KeyMapping.Category record.
        configKey = KeybindsBridge.create(
                "key.hugosmp_auctionhouse.open_config", KeybindsBridge.KEY_CONFIG, CATEGORY);
        rescanKey = KeybindsBridge.create(
                "key.hugosmp_auctionhouse.rescan", KeybindsBridge.KEY_RESCAN, CATEGORY);
        LOGGER.info("keybinds registered: config={}, rescan={}",
                configKey.getName(), rescanKey.getName());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKey.consumeClick()) {
                openConfigScreen();
            }
            while (rescanKey.consumeClick()) {
                capture.forceRescan();
            }
        });
    }

    public static void openConfigScreen() {
        Minecraft client = Minecraft.getInstance();
        // END_CLIENT_TICK fires on the render thread already; tell/execute
        // wrappers were removed because tell() was renamed in 1.21.2 mojmap.
        dev.hugosmp.auctionhouse.gui.GuiBridge.openScreen(client, new HugoConfigScreen(null));
    }
}