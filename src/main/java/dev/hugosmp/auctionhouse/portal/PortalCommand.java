package dev.hugosmp.auctionhouse.portal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.hugosmp.auctionhouse.HugoSmpAuction;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * /hugoauction portal [x z] — nether portal link calculator. Without
 * arguments it uses the player's current position and dimension; with
 * explicit x z it calculates from the player's current dimension.
 * Numbers only, so the output works in every client language.
 */
public final class PortalCommand {
    private PortalCommand() {}

    public static int showCurrent(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        LocalPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        return report(source, player, player.getX(), player.getZ());
    }

    public static int showExplicit(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        LocalPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int z = IntegerArgumentType.getInteger(ctx, "z");
        return report(source, player, x, z);
    }

    private static int report(FabricClientCommandSource source, LocalPlayer player,
                              double x, double z) {
        boolean inNether = isNether(player);
        PortalCalculator.Link link = inNether
                ? PortalCalculator.fromNether(x, z)
                : PortalCalculator.fromOverworld(x, z);
        int radius = PortalCalculator.searchRadius(link);

        dev.hugosmp.auctionhouse.chat.ChatBridge.message(player, header());
        dev.hugosmp.auctionhouse.chat.ChatBridge.message(player, line(
                inNether ? "NE " : "OW ",
                "x=" + PortalCalculator.coord(x) + " z=" + PortalCalculator.coord(z)));
        dev.hugosmp.auctionhouse.chat.ChatBridge.message(player, line(
                link.toNether() ? "NE " : "OW ",
                "x=" + PortalCalculator.coord(link.toX()) + " z=" + PortalCalculator.coord(link.toZ())
                        + "  (y free)"));
        dev.hugosmp.auctionhouse.chat.ChatBridge.message(player, line("search ",
                "+/- " + radius + " blocks around target"));
        HugoSmpAuction.LOGGER.info("portal calc: {} -> {} r={}",
                link.fromX() + "," + link.fromZ(), link.toX() + "," + link.toZ(), radius);
        return 1;
    }

    private static Component header() {
        return Component.literal("[portal calc] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("8:1").withStyle(ChatFormatting.GRAY));
    }

    private static Component line(String label, String value) {
        return Component.literal("  " + label).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static boolean isNether(LocalPlayer player) {
        try {
            // toString() is stable across versions ("...minecraft:nether..."),
            // while location()/identifier() was renamed in 1.21.9 — no bridge needed this way
            String dim = player.level().dimension().toString();
            return dim.contains("nether");
        } catch (Throwable t) {
            return false;
        }
    }
}
