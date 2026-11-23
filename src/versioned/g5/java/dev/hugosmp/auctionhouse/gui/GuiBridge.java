package dev.hugosmp.auctionhouse.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** g5 bridge: screen field + setScreen live on Minecraft directly. */
public final class GuiBridge {
    private GuiBridge() {
    }

    public static Screen currentScreen(Minecraft client) {
        return client.screen;
    }

    public static void openScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }
}
