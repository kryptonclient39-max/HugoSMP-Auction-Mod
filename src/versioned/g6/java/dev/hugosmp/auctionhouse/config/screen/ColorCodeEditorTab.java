package dev.hugosmp.auctionhouse.config.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.function.Consumer;

/**
 * In-game port of the market's color-code editor: compose an auction item
 * name (35 chars, codes count) or sign text (30 chars, codes free) with the
 * sixteen legacy colors, the formatters and full hex colors, mirror it in
 * the framed preview box and copy the finished code.
 *
 * All preview text goes through button labels: on this loader version raw
 * drawString calls made from the screen's render path do not show, while
 * widget-pass labels (the colored code buttons) demonstrably do.
 *
 * Lives inside HugoConfigScreen as a tab; holds its own widgets so text
 * survives tab switches while the menu is open.
 */
final class ColorCodeEditorTab {
    private static final int[] LIMITS = {35, 30};
    private static final int BODY_W = 190;

    private final Screen host;
    private int mode; // 0 = item name, 1 = sign

    private EditBox editor;
    private EditBox hexInput;
    private EditBox importInput;
    private String status = "";
    private long statusUntilMs;
    private int controlsW = BODY_W;
    private int previewX;
    private int previewY;
    private int previewW = 200;

    ColorCodeEditorTab(Screen host) {
        this.host = host;
    }

    void build(Consumer<Button> addButton, Consumer<EditBox> addBox,
               int x, int y, Font font, Minecraft mc) {
        int avail = host.width - x - 12;
        // 854x480 @ scale 2 gives ~279 px here — the preview must fit next to
        // the controls even on small windows, never behind them
        boolean previewRight = avail >= 240;
        controlsW = previewRight ? Math.max(140, Math.min(BODY_W, avail - 110)) : BODY_W;
        int row = y;

        Button modeButton = Button.builder(Component.literal(modeLabel()), b -> {
            mode = 1 - mode;
            b.setMessage(Component.literal(modeLabel()));
        }).bounds(x, row, controlsW, 20).build();
        addButton.accept(modeButton);
        row += 24;

        editor = new EditBox(font, x, row, controlsW, 20, Component.literal("text"));
        editor.setMaxLength(160);
        editor.setHint(Component.literal("Item name / sign text"));
        addBox.accept(editor);
        row += 22;

        row += 12;

        for (int i = 0; i < 16; i++) {
            final char code = "0123456789abcdef".charAt(i);
            ChatFormatting color = LegacyText.byLegacyChar(code);
            Button b = Button.builder(Component.literal(String.valueOf(code).toUpperCase())
                            .withStyle(color), btn -> applyCode("&" + code))
                    .bounds(x + (i % 8) * 24, row + (i / 8) * 22, 22, 20).build();
            addButton.accept(b);
        }
        row += 46;

        String[][] formats = {
                {"l", "Bold"}, {"n", "Underline"}, {"o", "Italic"},
                {"m", "Strike"}, {"k", "Random"}, {"r", "Reset"}};
        int fw = (controlsW - 4) / 3;
        for (int i = 0; i < formats.length; i++) {
            final String code = formats[i][0];
            Button b = Button.builder(
                    Component.literal(formats[i][1] + " ").append(
                            Component.literal("&" + code).withStyle(ChatFormatting.DARK_GRAY)),
                    btn -> applyCode("&" + code))
                    .bounds(x + (i % 3) * (fw + 2), row + (i / 3) * 22, fw, 20).build();
            addButton.accept(b);
        }
        row += 46;

        hexInput = new EditBox(font, x, row, 70, 20, Component.literal("hex"));
        hexInput.setMaxLength(7);
        hexInput.setHint(Component.literal("FF00AA"));
        addBox.accept(hexInput);
        addButton.accept(Button.builder(Component.literal("Apply Hex"), b -> applyHex())
                .bounds(x + 74, row, 70, 20).build());
        row += 24;

        int hw = (controlsW - 8) / 2;
        addButton.accept(Button.builder(Component.literal("Clear"), b -> editor.setValue(""))
                .bounds(x, row, hw, 20).build());
        addButton.accept(Button.builder(Component.literal("Copy"), b -> {
            mc.keyboardHandler.setClipboard(editor.getValue());
            say("copied code");
        }).bounds(x + hw + 8, row, hw, 20).build());
        row += 24;

        importInput = new EditBox(font, x, row, controlsW - 44, 20, Component.literal("import"));
        importInput.setMaxLength(320);
        importInput.setHint(Component.literal("& paste existing code"));
        addBox.accept(importInput);
        addButton.accept(Button.builder(Component.literal("Load"), b -> {
            editor.setValue(importInput.getValue().replace('§', '&'));
            importInput.setValue("");
            say("loaded into editor");
        }).bounds(x + controlsW - 40, row, 40, 20).build());

        // preview column position, drawn in render() before the widget pass
        previewX = previewRight ? x + controlsW + 16 : x;
        previewY = previewRight ? y : Math.min(row + 30, host.height - 118);
        previewW = previewRight ? Math.max(100, host.width - previewX - 8)
                : Math.min(controlsW + 10, host.width - previewX - 12);
    }

    void render(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        int limit = LIMITS[mode];
        int cost = LegacyText.cost(editor.getValue(), mode == 1);
        dev.hugosmp.auctionhouse.screen.TextBridge.left(graphics, font, x, y + 48,
                counterLine());
        if (!status.isEmpty() && System.currentTimeMillis() < statusUntilMs) {
            dev.hugosmp.auctionhouse.screen.TextBridge.left(graphics, font,
                    x + controlsW - font.width(status), y + 48,
                    Component.literal(status).withStyle(ChatFormatting.GREEN));
        }
        renderPreview(graphics, font, previewX, previewY, previewW);
    }

    private void renderPreview(GuiGraphicsExtractor graphics, Font font, int x, int y, int boxW) {
        dev.hugosmp.auctionhouse.screen.TextBridge.left(graphics, font, x, y,
                Component.literal("PREVIEW").withStyle(ChatFormatting.GRAY));
        List<net.minecraft.util.FormattedCharSequence> hint = font.split(Component.literal(
                        "Live mirror of your text, signer from your account.")
                        .withStyle(ChatFormatting.DARK_GRAY), boxW);
        int cursorY = y + 11;
        for (net.minecraft.util.FormattedCharSequence line : hint) {
            dev.hugosmp.auctionhouse.screen.TextBridge.seqLeft(graphics, font, x, cursorY, line);
            cursorY += 10;
        }

        int boxY = cursorY + 2;

        // item mode: the formatted name only. sign mode: text plus the
        // signature block, exactly the two things the market page mirrors.
        List<net.minecraft.util.FormattedCharSequence> body =
                new java.util.ArrayList<>(font.split(mirrorLine(), boxW - 8));
        if (mode == 1) {
            body.addAll(font.split(signerLine(), boxW - 8));
        }
        int boxH = 8 + body.size() * 10;

        graphics.fill(x - 1, boxY - 1, x + boxW + 1, boxY + boxH + 1, 0xFFE8E8E8);
        graphics.fill(x, boxY, x + boxW, boxY + boxH, 0xFF101018);

        int lineY = boxY + 4;
        for (net.minecraft.util.FormattedCharSequence line : body) {
            dev.hugosmp.auctionhouse.screen.TextBridge.seqLeft(graphics, font, x + 4, lineY, line);
            lineY += 10;
        }
    }

    private Component counterLine() {
        int limit = LIMITS[mode];
        int cost = LegacyText.cost(editor.getValue(), mode == 1);
        MutableComponent c = Component.literal(cost + " / " + limit
                + (mode == 1 ? " (codes free)" : ""));
        c.withStyle(cost > limit ? ChatFormatting.RED : ChatFormatting.GRAY);
        if (!status.isEmpty() && System.currentTimeMillis() < statusUntilMs) {
            c.append(Component.literal("  - " + status).withStyle(ChatFormatting.GREEN));
        }
        return c;
    }

    /** The mirror: exactly what the editor holds, styled. */
    private Component mirrorLine() {
        List<LegacyText.Segment> segments = LegacyText.segments(editor.getValue());
        if (segments.isEmpty()) {
            return Component.literal("Item-Name").withStyle(ChatFormatting.DARK_GRAY);
        }
        MutableComponent out = Component.empty();
        for (LegacyText.Segment seg : segments) {
            out.append(Component.literal(seg.text()).withStyle(seg.style()));
        }
        return out;
    }

    private Component signerLine() {
        Minecraft mc = Minecraft.getInstance();
        String account = mc != null ? mc.getUser().getName() : "?";
        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        return Component.literal("Signed by ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(account).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" on ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(date).withStyle(ChatFormatting.WHITE));
    }

    private String modeLabel() {
        return mode == 0 ? "Mode: Item Name (35)" : "Mode: Sign Text (30)";
    }

    private void applyHex() {
        String raw = hexInput.getValue().replace("#", "").trim();
        if (raw.length() != 6 || !raw.chars().allMatch(c ->
                (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
            say("invalid hex, e.g. FF00AA");
            return;
        }
        StringBuilder code = new StringBuilder("&x");
        for (char c : raw.toLowerCase().toCharArray()) code.append('&').append(c);
        applyCode(code.toString());
        hexInput.setValue("");
    }

    /** Insert a code around the current selection (or at the caret), keep
     *  the selection live so repeated clicks stack styles like the web tool. */
    private void applyCode(String code) {
        String value = editor.getValue();
        String selected = editor.getHighlighted();
        if (selected.isEmpty()) {
            editor.insertText(code);
            return;
        }
        int cursor = editor.getCursorPosition();
        int n = selected.length();
        int start = cursor >= n && value.regionMatches(cursor - n, selected, 0, n)
                ? cursor - n : cursor;
        String before = value.substring(0, start);
        String mid = value.substring(start, start + n);
        String after = value.substring(start + n);
        editor.setValue(before + code + mid + "&r" + after);
        int midStart = start + code.length();
        editor.setHighlightPos(midStart);
        editor.moveCursorTo(midStart + n, false);
    }

    private void say(String message) {
        status = message;
        statusUntilMs = System.currentTimeMillis() + 2500;
    }

    EditBox editorFocus() {
        return editor;
    }

}
