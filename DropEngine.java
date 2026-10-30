package com.example.mymod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;

/**
 * Drops items by sending the same THROW click that Ctrl+Q on a slot sends (button 1 = whole stack).
 * Everything goes through vanilla click handling, so the server validates it like a normal click.
 */
public final class DropEngine {
    private static final ArrayDeque<Integer> PENDING = new ArrayDeque<>();
    private static int pendingContainerId = -1;

    private DropEngine() {
    }

    public static void start(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        if (menu != player.containerMenu) {
            return;
        }
        QuickDropConfig cfg = QuickDropConfig.get();

        // THROW clicks are ignored by vanilla while something is held on the cursor.
        if (!menu.getCarried().isEmpty()) {
            say(player, cfg, "mymod.msg.cursor_busy");
            return;
        }
        if (cfg.mode == QuickDropConfig.Mode.WHITELIST && cfg.items.isEmpty()) {
            say(player, cfg, "mymod.msg.empty_list");
            return;
        }

        Inventory inventory = player.getInventory();
        boolean storageMenu = isStorageMenu(menu);

        PENDING.clear();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !slot.mayPickup(player)) {
                continue;
            }
            if (slot.container == inventory) {
                if (!cfg.includesInventory()) continue;
                int index = slot.getContainerSlot();
                if (index < 0 || index > 35) continue;           // armor + offhand are never touched
                if (index < 9 && !cfg.includeHotbar) continue;   // hotbar is 0..8, main inventory 9..35
            } else {
                // Only plain storage menus: never touch crafting results, anvil output, trades, ...
                if (!cfg.includesContainer() || !storageMenu) continue;
            }
            if (cfg.shouldDrop(stack)) {
                PENDING.add(i);
            }
        }

        if (PENDING.isEmpty()) {
            say(player, cfg, "mymod.msg.nothing");
            return;
        }
        pendingContainerId = menu.containerId;
        tick(mc); // first batch goes out right now, not on the next tick
    }

    public static void tick(Minecraft mc) {
        if (PENDING.isEmpty()) {
            return;
        }
        LocalPlayer player = mc.player;
        MultiPlayerGameMode gameMode = mc.gameMode;
        if (player == null || gameMode == null || player.containerMenu.containerId != pendingContainerId) {
            PENDING.clear();
            return;
        }
        QuickDropConfig cfg = QuickDropConfig.get();
        AbstractContainerMenu menu = player.containerMenu;
        int budget = Math.max(1, cfg.clicksPerTick);
        while (budget > 0 && !PENDING.isEmpty()) {
            int index = PENDING.poll();
            if (index >= menu.slots.size()) {
                continue;
            }
            // Re-check: the stack may have changed since the click was queued.
            ItemStack stack = menu.slots.get(index).getItem();
            if (stack.isEmpty() || !cfg.shouldDrop(stack)) {
                continue;
            }
            gameMode.handleInventoryMouseClick(pendingContainerId, index, 1, ClickType.THROW, player);
            budget--;
        }
    }

    private static boolean isStorageMenu(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu       // chest, barrel, ender chest, chest boat/minecart
                || menu instanceof ShulkerBoxMenu
                || menu instanceof HopperMenu
                || menu instanceof DispenserMenu; // dispenser + dropper
    }

    private static void say(LocalPlayer player, QuickDropConfig cfg, String key) {
        if (cfg.showMessages) {
            player.sendSystemMessage(Component.translatable(key));
        }
    }
}
