package dev.hugosmp.auctionhouse.price;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Identity of an item for matching purposes. Exact identity is the registry id
 * plus the hash of the item's data component patch; approximate identity is the
 * registry id alone. Approximate matching exists so that generic items still
 * get a price when the exact variant has never been listed.
 */
public final class ItemKey {
    public final String id;
    public final int componentHash;
    public final boolean exact;

    /** Package-private: rehydration from persisted listings. */
    ItemKey(String id, int componentHash, boolean exact) {
        this.id = id;
        this.componentHash = componentHash;
        this.exact = exact;
    }

    /** Exact key: id + component patch hash. */
    public static ItemKey exact(ItemStack stack) {
        return new ItemKey(registryId(stack), patchHash(stack), true);
    }

    /** Approximate key: id only. */
    public static ItemKey approximate(ItemStack stack) {
        return new ItemKey(registryId(stack), 0, false);
    }

    private static String registryId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static int patchHash(ItemStack stack) {
        try {
            DataComponentPatch patch = stack.getComponentsPatch();
            return patch == null ? 0 : patch.hashCode();
        } catch (RuntimeException e) {
            // Some exotic stacks refuse patch access; degrade to id-only matching.
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey other)) return false;
        return id.equals(other.id) && componentHash == other.componentHash && exact == other.exact;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, componentHash, exact);
    }

    @Override
    public String toString() {
        return exact ? id + "#" + Integer.toHexString(componentHash) : id + "#~";
    }
}
