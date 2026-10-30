package com.example.mymod;

import com.example.mymod.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Keeps the two in-GUI buttons next to the container frame (the frame moves when the recipe book opens). */
public final class QuickDropUi {
    public static final int BUTTON_WIDTH = 64;
    private static final int GAP = 22;

    private static AbstractContainerScreen<?> host;
    private static Button dropButton;
    private static Button settingsButton;

    private QuickDropUi() {
    }

    public static void attach(AbstractContainerScreen<?> screen, Button drop, Button settings) {
        host = screen;
        dropButton = drop;
        settingsButton = settings;
        reposition();
    }

    public static void tick(Minecraft mc) {
        if (host == null) {
            return;
        }
        if (mc.screen != host) {
            host = null;
            dropButton = null;
            settingsButton = null;
            return;
        }
        reposition();
    }

    private static void reposition() {
        AbstractContainerScreenAccessor frame = (AbstractContainerScreenAccessor) host;
        QuickDropConfig cfg = QuickDropConfig.get();
        int x = frame.mymod$getLeftPos() + frame.mymod$getImageWidth() + cfg.buttonOffsetX;
        x = Math.min(x, host.width - BUTTON_WIDTH - 2);
        int y = frame.mymod$getTopPos() + cfg.buttonOffsetY;
        dropButton.setPosition(x, y);
        settingsButton.setPosition(x, y + GAP);
    }
}
