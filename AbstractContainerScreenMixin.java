package com.example.mymod.mixin;

import com.example.mymod.DropEngine;
import com.example.mymod.QuickDropClient;
import com.example.mymod.QuickDropConfigScreen;
import com.example.mymod.QuickDropUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void mymod$addButtons(CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (self instanceof CreativeModeInventoryScreen) {
            return; // creative inventory uses its own, different click handling
        }

        Button drop = this.addRenderableWidget(
                Button.builder(Component.translatable("mymod.button.drop"), b -> DropEngine.start(self))
                        .bounds(0, 0, QuickDropUi.BUTTON_WIDTH, 20)
                        .tooltip(Tooltip.create(Component.translatable("mymod.button.drop.tooltip")))
                        .build());

        Button settings = this.addRenderableWidget(
                Button.builder(Component.translatable("mymod.button.settings"),
                                b -> Minecraft.getInstance().setScreen(new QuickDropConfigScreen(self)))
                        .bounds(0, 0, QuickDropUi.BUTTON_WIDTH, 20)
                        .tooltip(Tooltip.create(Component.translatable("mymod.button.settings.tooltip")))
                        .build());

        QuickDropUi.attach(self, drop, settings);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void mymod$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) self;
        if (QuickDropClient.onKey(self, event, accessor.mymod$getHoveredSlot())) {
            cir.setReturnValue(true);
        }
    }
}
