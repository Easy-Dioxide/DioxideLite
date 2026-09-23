package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/mixin/DeltaTrackerTimerAccessor.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives Scaffold reversible access to the client's base milliseconds per tick. */
@Mixin(DeltaTracker.Timer.class)
public interface DeltaTrackerTimerAccessor {

    @Accessor("msPerTick")
    float dioxidelite$getMsPerTick();

    @Mutable
    @Accessor("msPerTick")
    void dioxidelite$setMsPerTick(float milliseconds);
}
