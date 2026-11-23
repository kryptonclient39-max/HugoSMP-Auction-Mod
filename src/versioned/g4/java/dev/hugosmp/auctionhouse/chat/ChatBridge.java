package dev.hugosmp.auctionhouse.chat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** g4 bridge: hotbar=false system messages via displayClientMessage. */
public final class ChatBridge {
    private ChatBridge() {
    }

    public static void message(LocalPlayer player, Component message) {
        player.displayClientMessage(message, false);
    }
}
