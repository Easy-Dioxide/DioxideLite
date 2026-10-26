package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.movement.LongJump;
import com.dioxideliteng.krs.hacks.features.movement.fly.FlyModule;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {

    public AbstractClientPlayerMixin(Level world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Inject(method = "updateBob", at = @At("HEAD"), cancellable = true)
    private void krs$airViewBobbing(CallbackInfo ci) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (!this.shouldAirViewBobbing(player))
            return;

        float bob = Math.min(0.1F, (float) player.getDeltaMovement().horizontalDistance());
        player.avatarState().updateBob(bob);
        ci.cancel();
    }

    @Unique
    private boolean shouldAirViewBobbing(AbstractClientPlayer player) {
        boolean allowState = ModuleManager.getModuleState(FlyModule.class) && FlyModule.airViewBobbing.get() || ModuleManager.getModuleState(LongJump.class) && LongJump.airViewBobbing.get();
        return allowState && player instanceof LocalPlayer && !player.onGround() && !player.isDeadOrDying() && !player.isSwimming();
    }
}
