package dev.hugosmp.auctionhouse.capture;

import dev.hugosmp.auctionhouse.config.HugoConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts a price from an item's lore (or hover name) using the configured regex. */
public final class LorePriceParser {
    private LorePriceParser() {
    }

    /** @return parsed price in the server's currency unit, or null if the stack shows none. */
    public static Long parse(HugoConfig config, ItemStack stack) {
        Pattern pattern = config.pricePattern();
        if (pattern == null) {
            return null;
        }

        Long fromLore = fromComponents(pattern, stack);
        if (fromLore != null) {
            return fromLore;
        }
        // fallback hover-name matching requires a known currency keyword:
        // cuts false prices from item names that merely look numeric
        String name = stack.getHoverName().getString();
        dev.hugosmp.auctionhouse.match.LocaleMatcher matcher = config.localeMatcher();
        if (matcher != null && !matcher.test(name)) {
            return null;
        }
        return fromString(pattern, name);
    }

    private static Long fromComponents(Pattern pattern, ItemStack stack) {
        try {
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) {
                return null;
            }
            List<Component> lines = lore.lines();
            for (Component line : lines) {
                Long price = fromString(pattern, line.getString());
                if (price != null) {
                    return price;
                }
            }
        } catch (RuntimeException e) {
            // Component API drift guard: fall back to nothing rather than crash.
        }
        return null;
    }

    private static Long fromString(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1).replace(",", "").replace(".", "").replace(" ", "");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
