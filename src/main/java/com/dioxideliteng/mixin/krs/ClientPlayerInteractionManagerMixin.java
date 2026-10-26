package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.features.AttackEvent;
import com.dioxideliteng.krs.events.features.CancelBlockBreakingEvent;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.level.NoBreakCooldown;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import com.dioxideliteng.krs.hacks.features.level.ItemDropChanger;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Shadow
    public int destroyDelay;

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    public int noBreakCooldown(MultiPlayerGameMode clientPlayerInteractionManager) {
        if (ModuleManager.getModuleState(NoBreakCooldown.class))
            return 0;

        return this.destroyDelay;
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    public void attackEntity(Player player, Entity entity, CallbackInfo ci) {
        if (KrsClient.eventManager == null || !KrsClient.eventManager.hasListeners(AttackEvent.class))
            return;

        AttackEvent event = new AttackEvent(entity);
        KrsClient.eventManager.call(event);
        if (event.isCancelled())
            ci.cancel();
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
    public void onStopDestroyBlock(CallbackInfo ci) {
        if (KrsClient.eventManager == null || !KrsClient.eventManager.hasListeners(CancelBlockBreakingEvent.class))
            return;

        CancelBlockBreakingEvent event = new CancelBlockBreakingEvent();
        KrsClient.eventManager.call(event);
        if (event.isCancelled())
            ci.cancel();
    }

    // Since 26.3 the drop key goes through dropItem, which only plays the local swing.
    @WrapOperation(method = "dropItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"))
    private boolean dropItemHook(LocalPlayer instance, InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity, Operation<Boolean> original) {
        if (ItemDropChanger.hookDropItemSwing(hand))
            return original.call(instance, hand, animation, sendToSwingingEntity);

        return false;
    }
}
