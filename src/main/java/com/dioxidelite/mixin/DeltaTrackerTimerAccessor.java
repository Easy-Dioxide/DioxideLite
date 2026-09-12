package com.dioxidelite.mixin;

import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives Scaffold reversible access to the client's base milliseconds per tick. */
@Mixin(DeltaTracker.Timer.class)
public interface DeltaTrackerTimerAccessor {

    @Accessor("msPerTick")
    float DioxideLite$getMsPerTick();

    @Mutable
    @Accessor("msPerTick")
    void DioxideLite$setMsPerTick(float milliseconds);
}
