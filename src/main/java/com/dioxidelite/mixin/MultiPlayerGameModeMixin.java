package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/mixin/MultiPlayerGameModeMixin.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.events.AttackEvent;
import com.dioxidelite.module.modules.movement.Scaffold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void dioxidelite$beforeAttack(Player player, Entity target, CallbackInfo ci) {
        if (player == Minecraft.getInstance().player) {
            EventBus.INSTANCE.post(new AttackEvent(player, target));
        }
    }

    @ModifyExpressionValue(
            method = "ensureHasSentCarriedItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I"))
    private int dioxidelite$scaffoldSilentSlot(int original) {
        return Scaffold.INSTANCE.modifyServerSelectedSlot(original);
    }
}
