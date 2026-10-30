package com.example.mymod.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int mymod$getLeftPos();

    @Accessor("topPos")
    int mymod$getTopPos();

    @Accessor("imageWidth")
    int mymod$getImageWidth();

    @Accessor("hoveredSlot")
    Slot mymod$getHoveredSlot();
}
