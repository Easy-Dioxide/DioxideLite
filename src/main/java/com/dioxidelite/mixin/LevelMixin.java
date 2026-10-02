package com.dioxidelite.mixin;

import com.dioxidelite.module.modules.render.advanced.AttackEffects;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 配合 AttackEffects 模块：取消被禁用的玩家攻击音效。
 */
@Mixin(Level.class)
public class LevelMixin {

    @Inject(
            method = "playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void DioxideLite$cancelAttackSounds(Player player, double x, double y, double z,
                                                SoundEvent sound, SoundSource source, float volume, float pitch,
                                                CallbackInfo ci) {
        if (AttackEffects.INSTANCE.shouldCancelSound(sound)) {
            ci.cancel();
        }
    }
}
