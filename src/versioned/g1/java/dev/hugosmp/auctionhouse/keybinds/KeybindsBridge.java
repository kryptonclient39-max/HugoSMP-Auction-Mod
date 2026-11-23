package dev.hugosmp.auctionhouse.keybinds;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/** g1 bridge (<=1.21.4): String category, KeyBindingHelper registration. */
public final class KeybindsBridge {
    public static final int KEY_CONFIG = org.lwjgl.glfw.GLFW.GLFW_KEY_O;
    public static final int KEY_RESCAN = org.lwjgl.glfw.GLFW.GLFW_KEY_J;
    private KeybindsBridge() {
    }

    public static KeyMapping create(String name, int key, String category) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(name, key, category));
    }
}
