package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/mixin/ClientInputMixin.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.dioxidelite.module.modules.movement.Scaffold;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientInput.class)
public abstract class ClientInputMixin {

    @Shadow
    protected Vec2 moveVector;

    @ModifyReturnValue(method = "hasForwardImpulse", at = @At("RETURN"))
    private boolean dioxidelite$scaffoldOmnidirectionalSprint(boolean original) {
        if (Scaffold.INSTANCE.shouldSprintOmnidirectionally()) {
            return Math.abs(moveVector.x) > 1.0E-5F || Math.abs(moveVector.y) > 1.0E-5F;
        }
        return original;
    }
}
