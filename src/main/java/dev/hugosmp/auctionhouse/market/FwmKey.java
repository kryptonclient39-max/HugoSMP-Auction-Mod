package dev.hugosmp.auctionhouse.market;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.Optional;

/**
 * Maps an ItemStack to friendswithmoney.net's itemKey scheme. FWM keys are
 * registry paths without namespace ("tnt"), with "#variant" for potions
 * ("splash_potion#strength"). FWM normalizes strong_/long_ potion prefixes
 * away, so the same mapping is applied here.
 */
public final class FwmKey {
    private FwmKey() {
    }

    public static String of(ItemStack stack) {
        String base = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String variant = potionVariant(stack);
        return variant == null ? base : base + "#" + variant;
    }

    private static String potionVariant(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return null;
        }
        Optional<Holder<Potion>> potion = contents.potion();
        if (potion.isEmpty()) {
            return null;
        }
        Optional<net.minecraft.resources.ResourceKey<Potion>> key = potion.get().unwrapKey();
        if (key.isEmpty()) {
            return null;
        }
        // toString() ("ResourceKey[ns:reg / ns:path]") is stable across every
        // target version; location()/identifier() were renamed in 1.21.9.
        String id = key.get().toString();
        int slash = id.lastIndexOf('/');
        int colon = id.indexOf(':', slash);
        String path = id.substring(colon + 1, id.endsWith("]") ? id.length() - 1 : id.length());
        if (path.startsWith("strong_")) {
            path = path.substring("strong_".length());
        } else if (path.startsWith("long_")) {
            path = path.substring("long_".length());
        }
        return path;
    }
}
