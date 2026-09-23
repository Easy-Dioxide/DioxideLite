package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/mixin/MultiPlayerGameModeAccessor.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes vanilla's local block-breaking progress and retry delay. */
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

    @Invoker("startPrediction")
    void dioxidelite$startPrediction(ClientLevel level, PredictiveAction action);

    @Accessor("destroyDelay")
    void dioxidelite$setDestroyDelay(int value);

    @Accessor("destroyProgress")
    float dioxidelite$getDestroyProgress();

    @Accessor("destroyProgress")
    void dioxidelite$setDestroyProgress(float value);
}
