package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.AttackEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 移植自 OpenOpal AttackEffectsModule。
 * 控制攻击时的粒子（暴击/锋利）与各类攻击音效开关。
 */
public final class AttackEffects extends Module {

    public static final AttackEffects INSTANCE = new AttackEffects();

    // 粒子
    public final BooleanSetting criticalParticle = add(new BooleanSetting("Critical Particle", false));
    public final BooleanSetting sharpnessParticle = add(new BooleanSetting("Sharpness Particle", true));

    // 音效
    public final BooleanSetting soundCrit = add(new BooleanSetting("Sound Crit", false));
    public final BooleanSetting soundKnockback = add(new BooleanSetting("Sound Knockback", false));
    public final BooleanSetting soundStrong = add(new BooleanSetting("Sound Strong", false));
    public final BooleanSetting soundSweep = add(new BooleanSetting("Sound Sweep", false));
    public final BooleanSetting soundWeak = add(new BooleanSetting("Sound Weak", false));
    public final BooleanSetting soundNoDamage = add(new BooleanSetting("Sound No Damage", false));

    private final Map<SoundEvent, Supplier<Boolean>> soundValues = Map.of(
            SoundEvents.PLAYER_ATTACK_CRIT, soundCrit::get,
            SoundEvents.PLAYER_ATTACK_KNOCKBACK, soundKnockback::get,
            SoundEvents.PLAYER_ATTACK_STRONG, soundStrong::get,
            SoundEvents.PLAYER_ATTACK_SWEEP, soundSweep::get,
            SoundEvents.PLAYER_ATTACK_WEAK, soundWeak::get,
            SoundEvents.PLAYER_ATTACK_NODAMAGE, soundNoDamage::get
    );

    private AttackEffects() {
        super("Attack Effects", Category.RENDER);
    }

    @Listen
    private void onAttack(AttackEvent event) {
        if (mc.player == null || mc.level == null) return;
        Entity target = event.getTarget();
        if (criticalParticle.get()) {
            mc.level.addParticle(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    0.0D, 0.0D, 0.0D);
        }
        if (sharpnessParticle.get()) {
            mc.level.addParticle(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    /** 由 LevelMixin 调用：若该攻击音效被禁用则返回 true（应取消播放）。 */
    public boolean shouldCancelSound(SoundEvent sound) {
        if (!isEnabled()) return false;
        Supplier<Boolean> supplier = soundValues.get(sound);
        return supplier != null && !supplier.get();
    }
}
