package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.features.combat.Velocity;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.movement.MovementFix;
import com.dioxideliteng.krs.hacks.features.player.LookTP;
import com.dioxideliteng.krs.hacks.features.player.Scaffold;
import com.dioxideliteng.krs.hacks.features.combat.WTap;
import com.dioxideliteng.krs.utils.GuiInputBlocker;
import net.minecraft.client.KeyMapping;
import com.dioxideliteng.krs.utils.move.InputUtil;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void syncNanoVGClickGuiMovementKeys(CallbackInfo ci) {
        if (GuiInputBlocker.shouldSyncNanoVGClickGuiMovementKeys())
            KeyMapping.setAll();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void fixMovementAndStopWhenImguiCapturesKeyboard(CallbackInfo ci) {
        ClientInput input = (ClientInput) (Object) this;
        LookTP lookTP = ModuleManager.getModule(LookTP.class);

        if (lookTP != null && lookTP.shouldStopClientInput()) {
            InputUtil.stop(input);
            MovementFix.updateInput(input);
            Scaffold.hookEdgeSafeSneakInput(input);
            return;
        }

        if (!GuiInputBlocker.shouldBlockGameMovement()) {
            MovementFix.updateInput(input);
            Scaffold.hookEdgeSafeSneakInput(input);
            Velocity.hookJumpResetInput(input);
            Scaffold.hookTellyJumpInput(input);
            Scaffold.hookAutoInputFix(input);
            WTap.hookWTapInput(input);
            return;
        }

        GuiInputBlocker.releaseMovementKeys();
        InputUtil.stop(input);
        MovementFix.updateInput(input);
        Scaffold.hookEdgeSafeSneakInput(input);
    }
}
