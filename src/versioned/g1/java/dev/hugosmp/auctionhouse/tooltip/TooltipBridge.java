package dev.hugosmp.auctionhouse.tooltip;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * g1 bridge: fabric-api <= 0.119.x tooltip callback signature
 * (ItemStack, Item.TooltipContext, TooltipFlag, List&lt;Component&gt;).
 */
public final class TooltipBridge {
    private TooltipBridge() {
    }

    public static void register(BiConsumer<ItemStack, List<Component>> consumer) {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipFlag, lines) ->
                consumer.accept(stack, lines));
    }
}
