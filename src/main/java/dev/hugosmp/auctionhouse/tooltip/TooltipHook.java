package dev.hugosmp.auctionhouse.tooltip;

import dev.hugosmp.auctionhouse.config.HugoConfig;
import dev.hugosmp.auctionhouse.formats.Formats;
import dev.hugosmp.auctionhouse.market.FwmKey;
import dev.hugosmp.auctionhouse.market.MarketCache;
import dev.hugosmp.auctionhouse.price.ItemKey;
import dev.hugosmp.auctionhouse.price.PriceDatabase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/** Price resolution for the tooltip value lines. */
public final class TooltipHook {
    private TooltipHook() {
    }

    /** Unit price for a stack: local capture first, FWM fallback. Empty when unknown. */
    public static OptionalLong unitPrice(HugoConfig config, PriceDatabase database,
                                         MarketCache marketCache, ItemStack stack) {
        if (stack.isEmpty()) {
            return OptionalLong.empty();
        }
        ItemKey key = ItemKey.exact(stack);
        Optional<PriceDatabase.Aggregate> exact = database.lookupExact(key);
        if (exact.isPresent()) {
            return OptionalLong.of(exact.get().lowestPrice);
        }
        Optional<PriceDatabase.Aggregate> approx = database.lookupApproximate(key);
        if (approx.isPresent()) {
            return OptionalLong.of(approx.get().lowestPrice);
        }
        if (config.fwmEnabled && marketCache != null) {
            Optional<MarketCache.ItemStats> stats = marketCache.stats(FwmKey.of(stack));
            if (stats.isPresent()) {
                MarketCache.ItemStats s = stats.get();
                long value = s.count1h > 0 ? s.avg1h : s.avg24h;
                if (value > 0) {
                    return OptionalLong.of(value);
                }
            }
        }
        return OptionalLong.empty();
    }

    /** Best order (buy-order) price for a stack, single-item unit. The
     *  server order stats are the only source — the local AH capture never
     *  sees order books. Empty when FWM is off or the item has no orders. */
    public static OptionalLong orderPrice(HugoConfig config, MarketCache marketCache,
                                          ItemStack stack) {
        if (stack.isEmpty() || !config.fwmEnabled || marketCache == null) {
            return OptionalLong.empty();
        }
        Optional<MarketCache.ItemStats> stats = marketCache.stats(FwmKey.of(stack));
        if (stats.isPresent()) {
            MarketCache.ItemStats s = stats.get();
            long value = s.orders1h > 0 ? s.orders1h : s.orders24h;
            if (value > 0) {
                return OptionalLong.of(value);
            }
        }
        return OptionalLong.empty();
    }

    public static void appendPriceLines(HugoConfig config, PriceDatabase database,
                                         MarketCache marketCache, ItemStack stack,
                                         List<Component> lines) {
        if (!config.enabled || !config.tooltipEnabled || stack.isEmpty()) {
            return;
        }

        OptionalLong price = unitPrice(config, database, marketCache, stack);
        if (price.isPresent()) {
            lines.add(line("Auction value", config, price.getAsLong(), stack.getCount()));
        } else if (config.showHintWhenNoData) {
            lines.add(Component.literal("Auction value: ?"));
        }

        OptionalLong order = orderPrice(config, marketCache, stack);
        if (order.isPresent()) {
            lines.add(line("Order value", config, order.getAsLong(), stack.getCount()));
        }
    }

    /** Market prices are per single item — scale up to the stack size and say so. */
    private static Component line(String label, HugoConfig config, long unitPrice, int count) {
        if (count > 1) {
            return Component.literal(label + ": " + Formats.format(config, unitPrice * count)
                    + " Coins (" + count + " × " + Formats.format(config, unitPrice) + ")");
        }
        return Component.literal(label + ": " + Formats.format(config, unitPrice) + " Coins");
    }
}
