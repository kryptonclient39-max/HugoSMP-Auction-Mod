package dev.hugosmp.auctionhouse.config.screen;

import dev.hugosmp.auctionhouse.portal.PortalCalculator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Nether Portal Calculator section of the config screen. Widget-only by
 * design: every visual is a Button or EditBox, so the class compiles and
 * renders unchanged on every supported version (no render-path types,
 * no per-version copy needed). All math lives in PortalCalculator.
 *
 * Layout adapts to the body column width; source values prefill from the
 * player's current position and the destination updates live on every
 * keystroke or direction flip.
 */
public final class PortalCalculatorTab {
    private static final int MAX_W = 300;
    private static final int LABEL_W = 22;
    private static final int COPY_W = 42;
    private static final int ROW = 24;

    private final Screen host;
    private boolean toNether = true;

    private EditBox xIn;
    private EditBox yIn;
    private EditBox zIn;
    private Button dirButton;
    private Button xOut;
    private Button yOut;
    private Button zOut;

    public PortalCalculatorTab(Screen host) {
        this.host = host;
    }

    public void build(Consumer<Button> addButton, Consumer<EditBox> addBox,
                      int x, int y, Font font, Minecraft mc) {
        int w = Math.max(180, Math.min(MAX_W, host.width - x - 12));
        int fieldW = w - LABEL_W - COPY_W - 8;
        int row = y;

        dirButton = Button.builder(directionLabel(), b -> {
            toNether = !toNether;
            b.setMessage(directionLabel());
            recompute();
        }).bounds(x, row, w, 20).build();
        addButton.accept(dirButton);
        row += ROW + 4;

        addButton.accept(sectionLabel("Source", ChatFormatting.GRAY, x, row, w));
        row += 20;

        xIn = numberField(font, x + LABEL_W, row, fieldW, "x");
        yIn = numberField(font, x + LABEL_W, row + ROW, fieldW, "y");
        zIn = numberField(font, x + LABEL_W, row + 2 * ROW, fieldW, "z");
        addBox.accept(xIn);
        addBox.accept(yIn);
        addBox.accept(zIn);
        addButton.accept(staticLabel("X", x, row, font));
        addButton.accept(staticLabel("Y", x, row + ROW, font));
        addButton.accept(staticLabel("Z", x, row + 2 * ROW, font));
        Runnable re = this::recompute;
        xIn.setResponder(v -> re.run());
        yIn.setResponder(v -> re.run());
        zIn.setResponder(v -> re.run());

        int outY = row + 3 * ROW + 4;
        addButton.accept(sectionLabel("Destination", ChatFormatting.GOLD, x, outY, w));
        outY += 20;

        xOut = resultLabel(x + LABEL_W, outY, fieldW);
        yOut = resultLabel(x + LABEL_W, outY + ROW, fieldW);
        zOut = resultLabel(x + LABEL_W, outY + 2 * ROW, fieldW);
        addButton.accept(xOut);
        addButton.accept(yOut);
        addButton.accept(zOut);
        addButton.accept(staticLabel("X", x, outY, font));
        addButton.accept(staticLabel("Y", x, outY + ROW, font));
        addButton.accept(staticLabel("Z", x, outY + 2 * ROW, font));
        addButton.accept(copyButton("Copy X", x + LABEL_W + fieldW + 4, outY, mc, 0));
        addButton.accept(copyButton("Copy Y", x + LABEL_W + fieldW + 4, outY + ROW, mc, 1));
        addButton.accept(copyButton("Copy Z", x + LABEL_W + fieldW + 4, outY + 2 * ROW, mc, 2));

        int actY = outY + 3 * ROW + 4;
        addButton.accept(Button.builder(Component.literal("Copy all"), b -> {
            String text = plain(xOut) + "  " + plain(yOut) + "  " + plain(zOut);
            try {
                mc.keyboardHandler.setClipboard(text);
            } catch (Throwable ignored) {
            }
        }).bounds(x, actY, (w - 4) / 2, 20).build());
        addButton.accept(Button.builder(Component.literal("Reset"), b -> {
            prefill(mc);
            recompute();
        }).bounds(x + (w - 4) / 2 + 4, actY, (w - 4) / 2, 20).build());

        prefill(mc);
        recompute();
    }

    private void prefill(Minecraft mc) {
        LocalPlayer player = mc == null ? null : mc.player;
        if (player == null) return;
        // prefill matches the source side of the current direction
        boolean playerInNether = isPlayerInNether(player);
        if (playerInNether == !toNether) {
            toNether = playerInNether;
            if (dirButton != null) dirButton.setMessage(directionLabel());
        }
        xIn.setValue(fmt(player.getX()));
        yIn.setValue(fmt(player.getY()));
        zIn.setValue(fmt(player.getZ()));
    }

    private static boolean isPlayerInNether(LocalPlayer player) {
        try {
            return player.level().dimension().toString().contains("nether");
        } catch (Throwable t) {
            return false;
        }
    }

    private void recompute() {
        Double x = PortalCalculator.parse(xIn.getValue());
        Double y = PortalCalculator.parse(yIn.getValue());
        Double z = PortalCalculator.parse(zIn.getValue());
        if (x == null || y == null || z == null) {
            invalid();
            return;
        }
        PortalCalculator.Result r = PortalCalculator.convert(x, y, z, toNether);
        result(xOut, "X", r.toX());
        result(yOut, "Y", r.toY());
        result(zOut, "Z", r.toZ());
    }

    private void result(Button slot, String axis, double value) {
        slot.setMessage(Component.literal(axis + " " + Math.round(value))
                .withStyle(ChatFormatting.WHITE));
    }

    private void invalid() {
        for (Button b : new Button[] {xOut, yOut, zOut}) {
            b.setMessage(Component.literal("—").withStyle(ChatFormatting.RED));
        }
    }

    private Component directionLabel() {
        net.minecraft.network.chat.MutableComponent from =
                Component.literal(toNether ? "Overworld" : "Nether")
                        .withStyle(toNether ? ChatFormatting.GREEN : ChatFormatting.GOLD);
        Component to = Component.literal(toNether ? "Nether" : "Overworld")
                .withStyle(toNether ? ChatFormatting.GOLD : ChatFormatting.GREEN);
        return from.append(Component.literal("  \u2192  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(to)
                .append(Component.literal(toNether ? "   \u00f7 8" : "   \u00d7 8")
                        .withStyle(ChatFormatting.GRAY));
    }

    private EditBox numberField(Font font, int x, int y, int w, String axis) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.literal(axis));
        box.setMaxLength(16);
        box.setHint(Component.literal(axis));
        return box;
    }

    private Button sectionLabel(String text, ChatFormatting color, int x, int y, int w) {
        Button b = Button.builder(Component.literal(text).withStyle(color), btn -> { })
                .bounds(x, y, w, 12).build();
        b.active = false;
        return b;
    }

    private Button staticLabel(String axis, int x, int y, Font font) {
        Button b = Button.builder(Component.literal(axis).withStyle(ChatFormatting.GRAY),
                btn -> { }).bounds(x, y, LABEL_W, 20).build();
        b.active = false;
        return b;
    }

    private Button resultLabel(int x, int y, int w) {
        Button b = Button.builder(Component.literal("—"), btn -> { }).bounds(x, y, w, 20).build();
        b.active = false;
        return b;
    }

    private Button copyButton(String label, int x, int y, Minecraft mc, int axis) {
        return Button.builder(Component.literal(label).withStyle(ChatFormatting.GRAY), b -> {
            Button slot = switch (axis) {
                case 1 -> yOut;
                case 2 -> zOut;
                default -> xOut;
            };
            try {
                mc.keyboardHandler.setClipboard(plain(slot));
            } catch (Throwable ignored) {
            }
        }).bounds(x, y, COPY_W, 20).build();
    }

    private static String plain(Button b) {
        return b.getMessage().getString();
    }

    private static String fmt(double v) {
        double r = Math.round(v * 100) / 100.0;
        if (r == Math.rint(r)) {
            return Long.toString((long) r);
        }
        return Double.toString(r);
    }
}
