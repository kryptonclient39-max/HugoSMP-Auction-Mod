package dev.hugosmp.auctionhouse.config.screen;

import dev.hugosmp.auctionhouse.HugoSmpAuction;
import dev.hugosmp.auctionhouse.config.HugoConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Config screen: General + Color Codes tabs. */
public final class HugoConfigScreen extends Screen {
    private static final int SIDEBAR_W = 118;
    private static final int MARGIN = 10;

    private final HugoConfig working;
    private final Screen parent;
    private ColorCodeEditorTab colorTab;
    private PortalCalculatorTab portalTab;
    private int tab;

    public HugoConfigScreen(Screen parent) {
        super(Component.literal("HugoSMP Auction"));
        this.parent = parent;
        this.working = new HugoConfig();
        HugoConfig current = HugoSmpAuction.config();
        if (current != null) {
            working.enabled = current.enabled;
            working.tooltipEnabled = current.tooltipEnabled;
            working.fwmEnabled = current.fwmEnabled;
            working.priceSeparator = current.priceSeparator;
            working.captureTitleRegex = current.captureTitleRegex;
            working.priceRegex = current.priceRegex;
            working.cacheSeconds = current.cacheSeconds;
            working.refreshIntervalMs = current.refreshIntervalMs;
            working.maxListingsTracked = current.maxListingsTracked;
            working.showApproximate = current.showApproximate;
            working.showHintWhenNoData = current.showHintWhenNoData;
            working.showPricePerItem = current.showPricePerItem;
            working.fwmOverviewRefreshSeconds = current.fwmOverviewRefreshSeconds;
        }
    }

    @Override
    protected void init() {
        int x = MARGIN;
        int y = 32;
        dev.hugosmp.auctionhouse.HugoSmpAuction.LOGGER.info("config screen build 2026-11-23T05-25 collector-text");
        Button general = Button.builder(
                Component.literal(tab == 0 ? "> General" : "  General"), b -> {
                    tab = 0;
                    rebuildWidgets();
                }).bounds(x, y, SIDEBAR_W - 8, 20).build();
        general.active = tab != 0;
        addRenderableWidget(general);

        if (colorTab == null) {
            colorTab = new ColorCodeEditorTab(this);
        }
        Button colors = Button.builder(
                Component.literal(tab == 1 ? "> Color Codes" : "  Color Codes"), b -> {
                    tab = 1;
                    rebuildWidgets();
                }).bounds(x, y + 22, SIDEBAR_W - 8, 20).build();
        colors.active = tab != 1;
        addRenderableWidget(colors);

        if (portalTab == null) {
            portalTab = new PortalCalculatorTab(this);
        }
        Button portal = Button.builder(
                Component.literal(tab == 2 ? "> Portal Calc" : "  Portal Calc"), b -> {
                    tab = 2;
                    rebuildWidgets();
                }).bounds(x, y + 44, SIDEBAR_W - 8, 20).build();
        portal.active = tab != 2;
        addRenderableWidget(portal);

        int bodyX = x + SIDEBAR_W + 8;
        if (tab == 0) {
            buildGeneral(bodyX, y);
        } else if (tab == 1) {
            colorTab.build(this::addRenderableWidget, this::addRenderableWidget,
                    bodyX, y, this.font, this.minecraft);
            setInitialFocus(colorTab.editorFocus());
        } else {
            portalTab.build(this::addRenderableWidget, this::addRenderableWidget,
                    bodyX, y, this.font, this.minecraft);
        }
    }

    private void buildGeneral(int x, int y) {
        int row = y;
        addRenderableWidget(toggle("Tooltip prices", () -> working.tooltipEnabled,
                v -> working.tooltipEnabled = v, x, row, 190));
        row += 24;
        addRenderableWidget(toggle("FWM market data", () -> working.fwmEnabled,
                v -> working.fwmEnabled = v, x, row, 190));
        row += 24;
        addRenderableWidget(toggle("Mod enabled", () -> working.enabled,
                v -> working.enabled = v, x, row, 190));
        row += 24;
        addRenderableWidget(cycle("Separator", new String[]{"commas", "periods", "spaces"},
                () -> working.priceSeparator, v -> working.priceSeparator = v, x, row, 190));
        row += 32;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            working.compilePatterns();
            working.save();
            onClose();
        }).bounds(x, row, 190, 20).build());
    }

    private Button toggle(String label, java.util.function.Supplier<Boolean> get,
                          java.util.function.Consumer<Boolean> set, int x, int y, int w) {
        return Button.builder(Component.literal(label + ": " + (get.get() ? "ON" : "OFF")), btn -> {
            boolean next = !get.get();
            set.accept(next);
            btn.setMessage(Component.literal(label + ": " + (next ? "ON" : "OFF")));
        }).bounds(x, y, w, 20).build();
    }

    private Button cycle(String label, String[] options, java.util.function.Supplier<String> get,
                         java.util.function.Consumer<String> set, int x, int y, int w) {
        return Button.builder(Component.literal(label + ": " + get.get()), btn -> {
            String cur = get.get();
            int idx = 0;
            for (int i = 0; i < options.length; i++) if (options[i].equals(cur)) idx = i;
            idx = (idx + 1) % options.length;
            set.accept(options[idx]);
            btn.setMessage(Component.literal(label + ": " + options[idx]));
        }).bounds(x, y, w, 20).build();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        dev.hugosmp.auctionhouse.screen.TextBridge.centered(graphics, this.font,
                this.width / 2, 12, this.title);
        graphics.fill(MARGIN + SIDEBAR_W + 4, 30, MARGIN + SIDEBAR_W + 5, this.height - MARGIN, 0x50FFFFFF);
        if (tab == 1) {
            colorTab.render(graphics, this.font, MARGIN + SIDEBAR_W + 8, 32);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            dev.hugosmp.auctionhouse.gui.GuiBridge.openScreen(this.minecraft, parent);
        }
    }
}
