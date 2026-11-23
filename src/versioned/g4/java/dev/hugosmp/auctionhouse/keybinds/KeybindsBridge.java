package dev.hugosmp.auctionhouse.keybinds;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** g4 bridge (1.21.11): Category record keyed by Identifier. */
public final class KeybindsBridge {
    public static final int KEY_CONFIG = org.lwjgl.glfw.GLFW.GLFW_KEY_O;
    public static final int KEY_RESCAN = org.lwjgl.glfw.GLFW.GLFW_KEY_J;
    private static final String MOD_NAMESPACE = "hugosmp_auctionhouse";
    private static KeyMapping.Category category;

    private KeybindsBridge() {
    }

    public static KeyMapping create(String name, int key, String legacyCategory) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(name, key, category()));
    }

    private static KeyMapping.Category category() {
        if (category == null) {
            category = KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(MOD_NAMESPACE, "main"));
        }
        return category;
    }
}
