package dev.hugosmp.auctionhouse.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** g6 bridge (26.3): GuiGraphics became GuiGraphicsExtractor; text() is the
 *  recording call. Centered = manual offset, the extractor has no center api. */
public final class TextBridge {
    private TextBridge() {}

    public static void left(GuiGraphicsExtractor graphics, Font font, int x, int y, Component text) {
        graphics.text(font, text, x, y, 0xFFFFFF);
    }

    public static void seqLeft(GuiGraphicsExtractor graphics, Font font, int x, int y, FormattedCharSequence text) {
        graphics.text(font, text, x, y, 0xFFFFFF);
    }

    public static void centered(GuiGraphicsExtractor graphics, Font font, int centerX, int y, Component text) {
        graphics.text(font, text, centerX - font.width(text) / 2, y, 0xFFFFFF);
    }
}
