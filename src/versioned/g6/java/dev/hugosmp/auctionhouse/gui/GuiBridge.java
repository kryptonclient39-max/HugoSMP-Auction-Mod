package dev.hugosmp.auctionhouse.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** g6 bridge (26.3): screen management moved from Minecraft to Minecraft#gui. */
public final class GuiBridge {
    private GuiBridge() {
    }

    public static Screen currentScreen(Minecraft client) {
        return client.gui.screen();
    }

    public static void openScreen(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }
}
