package com.dioxidelite.mixin;

import com.dioxidelite.ui.screen.GameMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swaps Minecraft's pause menu for the DioxideLite console. The vanilla screen is
 * still the one the game opens (so pausing, singleplayer pausing and the ESC
 * flow are untouched); only its visuals are replaced once it has built itself.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void DioxideLite$replacePauseMenu(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof GameMenuScreen) {
            return;
        }
        client.setScreen(new GameMenuScreen());
    }
}