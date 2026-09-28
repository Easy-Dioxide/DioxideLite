package com.dioxidelite.mixin;

import com.dioxidelite.module.modules.render.advanced.ParticleLimiter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Particle Limiter：按模块的随机判定拦截粒子生成。 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$limitParticles(ParticleOptions options, double x, double y, double z,
                                            double velocityX, double velocityY, double velocityZ,
                                            CallbackInfoReturnable<Particle> cir) {
        ParticleLimiter limiter = ParticleLimiter.INSTANCE;
        if (limiter.isEnabled() && limiter.shouldLimit()) {
            cir.setReturnValue(null);
        }
    }
}
