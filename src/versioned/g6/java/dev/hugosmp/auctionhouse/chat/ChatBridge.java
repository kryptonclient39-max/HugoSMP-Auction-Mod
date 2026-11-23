package dev.hugosmp.auctionhouse.chat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** g6 bridge (26.3): displayClientMessage was replaced by sendSystemMessage. */
public final class ChatBridge {
    private ChatBridge() {
    }

    public static void message(LocalPlayer player, Component message) {
        player.sendSystemMessage(message);
    }
}
