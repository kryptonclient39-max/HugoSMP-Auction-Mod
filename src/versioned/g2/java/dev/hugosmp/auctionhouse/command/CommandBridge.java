package dev.hugosmp.auctionhouse.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/** g2 (1.21.5-1.21.7) bridge: command tree factories. */
public final class CommandBridge {
    private CommandBridge() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return ClientCommandManager.literal(name);
    }

    public static RequiredArgumentBuilder<FabricClientCommandSource, ?> argument(String name,
            ArgumentType<?> type) {
        return ClientCommandManager.argument(name, type);
    }
}
