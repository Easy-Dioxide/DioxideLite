package com.dioxidelite.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

// [DioxideLite 修复] accessor 前缀由 DioxideLite$ 统一为小写 dioxidelite$。
// 原大写形式是机械改名残留，且与移植代码的调用点不一致，会编译失败。
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Invoker("getJumpPower")
    float dioxidelite$invokeGetJumpPower();

    @Accessor("noJumpDelay")
    int dioxidelite$getNoJumpDelay();
}
