package com.dioxidelite.mixin;

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
    void DioxideLite$startPrediction(ClientLevel level, PredictiveAction action);

    @Accessor("destroyDelay")
    void DioxideLite$setDestroyDelay(int value);

    @Accessor("destroyProgress")
    float DioxideLite$getDestroyProgress();

    @Accessor("destroyProgress")
    void DioxideLite$setDestroyProgress(float value);
}
