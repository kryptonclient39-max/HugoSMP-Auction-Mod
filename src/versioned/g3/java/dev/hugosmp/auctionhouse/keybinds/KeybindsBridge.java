package dev.hugosmp.auctionhouse.keybinds;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** g3 bridge (26.x): Category record keyed by Identifier; registration
 *  moved to KeyMappingHelper (KeyBindingHelper no longer exists). */
public final class KeybindsBridge {
    public static final int KEY_CONFIG = org.lwjgl.glfw.GLFW.GLFW_KEY_O;
    public static final int KEY_RESCAN = org.lwjgl.glfw.GLFW.GLFW_KEY_J;
    private static final String MOD_NAMESPACE = "hugosmp_auctionhouse";
    private static KeyMapping.Category category;

    private KeybindsBridge() {
    }

    public static KeyMapping create(String name, int key, String legacyCategory) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, key, category()));
    }

    private static KeyMapping.Category category() {
        if (category == null) {
            category = KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(MOD_NAMESPACE, "main"));
        }
        return category;
    }
}
