package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/mixin/KeyboardInputMixin.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.events.KeyboardInputEvent;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link KeyboardInputEvent} after raw key state is resolved, then rebuilds
 * the player's {@link Input} and move vector from the (possibly modified) values.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void dioxidelite$onInputTick(CallbackInfo ci) {
        Input original = this.keyPresses;
        KeyboardInputEvent event = EventBus.INSTANCE.post(new KeyboardInputEvent(
                original.forward(), original.backward(), original.left(), original.right(),
                original.jump(), original.shift(), original.sprint()));
        this.keyPresses = event.toInput();
        this.moveVector = new Vec2(event.getStrafe(), event.getForward()).normalized();
    }
}
