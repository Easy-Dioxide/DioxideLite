package mixins;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Krs parity: RotationManager.updateVanillaRenderBodyYaw reads the swing animation
 * progress. On the Krs mappings this was the public LivingEntity#attackAnim field;
 * 26.3 moved it into the private LivingEntity$SwingState#animation value. Neither
 * LivingEntity#isSwinging() (true one tick early, at swing start) nor
 * LivingEntity#getSwingAnimation(float) (interpolated, one tick late at swing end)
 * reproduces the vanilla body-yaw condition exactly, so the raw value is exposed here.
 */
@Mixin({LivingEntity.SwingState.class})
public interface SwingStateAccessor {
   @Accessor("animation")
   float getSwingAnimationValue();
}
