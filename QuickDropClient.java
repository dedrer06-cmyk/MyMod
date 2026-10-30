package com.example.mymod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

public class QuickDropClient implements ClientModInitializer {
    public static final String MOD_ID = "mymod";

    public static KeyMapping dropKey;
    public static KeyMapping toggleHoveredKey;

    private static long lastToggleMillis;

    @Override
    public void onInitializeClient() {
        QuickDropConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        // Both are rebindable in Options > Controls > Key Binds > "Quick Drop".
        dropKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mymod.drop", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, category));
        toggleHoveredKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mymod.toggle_hovered", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            DropEngine.tick(client);
            QuickDropUi.tick(client);
        });
    }

    /**
     * Called from the container screen's keyPressed (key mappings are NOT polled while a
     * screen is open in vanilla, so we have to hook the screen itself).
     *
     * @return true if the key was consumed
     */
    public static boolean onKey(AbstractContainerScreen<?> screen, KeyEvent event, Slot hovered) {
        if (screen instanceof CreativeModeInventoryScreen) {
            return false;
        }
        // Do not steal keystrokes while the player types (anvil rename, etc.)
        if (screen.getFocused() instanceof EditBox) {
            return false;
        }
        if (matches(dropKey, event)) {
            DropEngine.start(screen);
            return true;
        }
        if (matches(toggleHoveredKey, event)) {
            toggleHovered(hovered);
            return true;
        }
        return false;
    }

    private static boolean matches(KeyMapping mapping, KeyEvent event) {
        return mapping != null && !mapping.isUnbound() && mapping.matches(event);
    }

    private static void toggleHovered(Slot hovered) {
        long now = System.currentTimeMillis();
        if (now - lastToggleMillis < 300) { // ignore key auto-repeat
            return;
        }
        lastToggleMillis = now;
        if (hovered == null || !hovered.hasItem()) {
            return;
        }
        String id = QuickDropConfig.idOf(hovered.getItem());
        boolean added = QuickDropConfig.get().toggle(id);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.translatable(
                    added ? "mymod.msg.added" : "mymod.msg.removed", id));
        }
    }
}
