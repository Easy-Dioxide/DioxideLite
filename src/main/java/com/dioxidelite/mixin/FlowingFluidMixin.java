package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/mixin/FlowingFluidMixin.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.dioxidelite.module.modules.movement.Velocity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @WrapOperation(
            method = "getFlow",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Iterator;hasNext()Z",
                    ordinal = 0))
    private boolean dioxidelite$cancelWaterPush(Iterator<Direction> iterator, Operation<Boolean> original) {
        if (Velocity.INSTANCE.isEnabled() && Velocity.INSTANCE.waterPush.get()) {
            return false;
        }
        return original.call(iterator);
    }
}
