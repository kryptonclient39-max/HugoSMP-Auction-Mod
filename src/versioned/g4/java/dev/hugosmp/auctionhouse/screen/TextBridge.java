package dev.hugosmp.auctionhouse.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Text drawing for this fabric/mc era: retained ActiveTextCollector path.
 *  Legacy drawString calls are dropped by the 1.21.9+ renderer. */
public final class TextBridge {
    private TextBridge() {}

    public static void left(GuiGraphics graphics, Font font, int x, int y, Component text) {
        graphics.textRenderer().accept(TextAlignment.LEFT, x, y, text);
    }

    public static void seqLeft(GuiGraphics graphics, Font font, int x, int y, FormattedCharSequence text) {
        graphics.textRenderer().accept(TextAlignment.LEFT, x, y, text);
    }

    public static void centered(GuiGraphics graphics, Font font, int centerX, int y, Component text) {
        graphics.textRenderer().accept(TextAlignment.CENTER, centerX, y, text);
    }
}
