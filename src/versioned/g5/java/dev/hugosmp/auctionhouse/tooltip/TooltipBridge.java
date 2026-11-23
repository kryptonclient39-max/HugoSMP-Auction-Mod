package dev.hugosmp.auctionhouse.tooltip;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * g4 bridge (1.21.9+): fabric-api 0.141+ keeps the 4-param tooltip callback
 * (ItemStack, Item.TooltipContext, TooltipFlag, List<Component>).
 */
public final class TooltipBridge {
    private TooltipBridge() {
    }

    public static void register(BiConsumer<ItemStack, List<Component>> consumer) {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipFlag, lines) ->
                consumer.accept(stack, lines));
    }
}