package dev.hugosmp.auctionhouse.tooltip;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * g2 bridge: fabric-api 0.121+ tooltip callback signature
 * (ItemStack, Item.TooltipContext, TooltipFlag, List<Component>).
 * The 1.21.5 tooltip rework did NOT change this callback's shape.
 */
public final class TooltipBridge {
    private TooltipBridge() {
    }

    public static void register(BiConsumer<ItemStack, List<Component>> consumer) {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipFlag, lines) ->
                consumer.accept(stack, lines));
    }
}
