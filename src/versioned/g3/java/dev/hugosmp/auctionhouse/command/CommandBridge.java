package dev.hugosmp.auctionhouse.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/** g3 (26.x) bridge: command tree factories. */
public final class CommandBridge {
    private CommandBridge() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return ClientCommands.literal(name);
    }

    public static RequiredArgumentBuilder<FabricClientCommandSource, ?> argument(String name,
            ArgumentType<?> type) {
        return ClientCommands.argument(name, type);
    }
}
