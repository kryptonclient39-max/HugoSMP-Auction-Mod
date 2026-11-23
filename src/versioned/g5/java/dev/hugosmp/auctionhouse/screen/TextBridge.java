package dev.hugosmp.auctionhouse.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Text drawing for this fabric/mc era: legacy immediate-mode calls. */
public final class TextBridge {
    private TextBridge() {}

    public static void left(GuiGraphics graphics, Font font, int x, int y, Component text) {
        graphics.drawString(font, text, x, y, 0xFFFFFF);
    }

    public static void seqLeft(GuiGraphics graphics, Font font, int x, int y, FormattedCharSequence text) {
        graphics.drawString(font, text, x, y, 0xFFFFFF);
    }

    public static void centered(GuiGraphics graphics, Font font, int centerX, int y, Component text) {
        graphics.drawCenteredString(font, text, centerX, y, 0xFFFFFF);
    }
}
