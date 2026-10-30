package com.example.mymod;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Settings + item list editor. Built only from vanilla widgets (Button / EditBox / StringWidget)
 * so it does not depend on any custom rendering code.
 */
public class QuickDropConfigScreen extends Screen {
    private static final int ROWS = 5;

    private final Screen parent;
    private int page;
    private String draft = "";
    private Component status = Component.empty();
    private EditBox input;

    public QuickDropConfigScreen(Screen parent) {
        super(Component.translatable("mymod.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QuickDropConfig cfg = QuickDropConfig.get();
        int cx = this.width / 2;
        int left = cx - 155;
        int right = cx + 5;
        int top = 36;

        this.addRenderableWidget(new StringWidget(left, 14, 310, 12, this.getTitle(), this.font));

        // ---- left column: settings ----------------------------------------
        this.addRenderableWidget(Button.builder(modeLabel(cfg), b -> {
                    cfg.mode = cfg.mode == QuickDropConfig.Mode.WHITELIST
                            ? QuickDropConfig.Mode.BLACKLIST : QuickDropConfig.Mode.WHITELIST;
                    cfg.changed();
                    b.setMessage(modeLabel(cfg));
                })
                .bounds(left, top, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("mymod.config.mode.tooltip")))
                .build());

        this.addRenderableWidget(Button.builder(scopeLabel(cfg), b -> {
                    QuickDropConfig.Scope[] all = QuickDropConfig.Scope.values();
                    cfg.scope = all[(cfg.scope.ordinal() + 1) % all.length];
                    cfg.changed();
                    b.setMessage(scopeLabel(cfg));
                })
                .bounds(left, top + 24, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("mymod.config.scope.tooltip")))
                .build());

        this.addRenderableWidget(Button.builder(onOff("mymod.config.hotbar", cfg.includeHotbar), b -> {
                    cfg.includeHotbar = !cfg.includeHotbar;
                    cfg.changed();
                    b.setMessage(onOff("mymod.config.hotbar", cfg.includeHotbar));
                })
                .bounds(left, top + 48, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("mymod.config.hotbar.tooltip")))
                .build());

        this.addRenderableWidget(Button.builder(onOff("mymod.config.messages", cfg.showMessages), b -> {
                    cfg.showMessages = !cfg.showMessages;
                    cfg.changed();
                    b.setMessage(onOff("mymod.config.messages", cfg.showMessages));
                })
                .bounds(left, top + 72, 150, 20)
                .build());

        // ---- right column: item list --------------------------------------
        int pages = Math.max(1, (cfg.items.size() + ROWS - 1) / ROWS);
        this.page = Math.max(0, Math.min(this.page, pages - 1));

        for (int row = 0; row < ROWS; row++) {
            int index = this.page * ROWS + row;
            if (index >= cfg.items.size()) {
                break;
            }
            String entry = cfg.items.get(index);
            int y = top + row * 22;

            Button label = Button.builder(Component.literal(entry), b -> { })
                    .bounds(right, y, 126, 20)
                    .build();
            label.active = false;
            this.addRenderableWidget(label);

            this.addRenderableWidget(Button.builder(
                            Component.literal("X").withStyle(ChatFormatting.RED), b -> {
                                cfg.remove(entry);
                                refresh();
                            })
                    .bounds(right + 130, y, 20, 20)
                    .build());
        }

        int pagerY = top + ROWS * 22;
        Button prev = Button.builder(Component.literal("<"), b -> {
                    this.page--;
                    refresh();
                })
                .bounds(right, pagerY, 40, 20)
                .build();
        prev.active = this.page > 0;
        this.addRenderableWidget(prev);

        Button pageLabel = Button.builder(Component.literal((this.page + 1) + "/" + pages), b -> { })
                .bounds(right + 44, pagerY, 62, 20)
                .build();
        pageLabel.active = false;
        this.addRenderableWidget(pageLabel);

        Button next = Button.builder(Component.literal(">"), b -> {
                    this.page++;
                    refresh();
                })
                .bounds(right + 110, pagerY, 40, 20)
                .build();
        next.active = this.page < pages - 1;
        this.addRenderableWidget(next);

        int inputY = pagerY + 24;
        this.input = new EditBox(this.font, right, inputY, 94, 20, Component.translatable("mymod.config.hint"));
        this.input.setMaxLength(128);
        this.input.setHint(Component.translatable("mymod.config.hint"));
        this.input.setValue(this.draft);
        this.addRenderableWidget(this.input);

        this.addRenderableWidget(Button.builder(Component.translatable("mymod.config.add"), b -> addEntry())
                .bounds(right + 98, inputY, 52, 20)
                .build());

        this.addRenderableWidget(new StringWidget(right, inputY + 24, 150, 10, this.status, this.font));

        // ---- bottom -----------------------------------------------------------
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(cx - 100, Math.min(216, this.height - 24), 200, 20)
                .build());
    }

    private void addEntry() {
        QuickDropConfig cfg = QuickDropConfig.get();
        String normalized = QuickDropConfig.normalize(this.input.getValue());
        if (normalized == null) {
            this.status = Component.translatable("mymod.config.invalid").withStyle(ChatFormatting.RED);
            refresh(); // keeps the typed text so it can be fixed
            return;
        }
        cfg.add(normalized);
        this.input.setValue("");
        this.status = Component.translatable("mymod.config.added", normalized).withStyle(ChatFormatting.GREEN);
        this.page = Integer.MAX_VALUE; // clamped to the last page in init()
        refresh();
    }

    private void refresh() {
        this.draft = this.input == null ? "" : this.input.getValue();
        this.rebuildWidgets();
    }

    private static Component modeLabel(QuickDropConfig cfg) {
        return Component.translatable("mymod.config.mode",
                Component.translatable("mymod.mode." + cfg.mode.name().toLowerCase(Locale.ROOT)));
    }

    private static Component scopeLabel(QuickDropConfig cfg) {
        return Component.translatable("mymod.config.scope",
                Component.translatable("mymod.scope." + cfg.scope.name().toLowerCase(Locale.ROOT)));
    }

    private static Component onOff(String key, boolean value) {
        return Component.translatable(key,
                Component.translatable(value ? "mymod.config.on" : "mymod.config.off"));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }
}
