package mixins;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LivingEntity.class})
public interface LivingEntityAccessor {
   @Accessor("noJumpDelay")
   void setNoJumpDelay(int noJumpDelay);

   // Krs parity (com.instrumentalist.mixin.injector.LivingEntityAccessor): riptide spin-attack state,
   // required by the ported AlwaysRiptide module (Krs widens these fields via its accesswidener).
   @Accessor("autoSpinAttackTicks")
   void setAutoSpinAttackTicks(int autoSpinAttackTicks);

   @Accessor("autoSpinAttackDmg")
   void setAutoSpinAttackDmg(float autoSpinAttackDmg);

   @Accessor("autoSpinAttackItemStack")
   void setAutoSpinAttackItemStack(ItemStack autoSpinAttackItemStack);

   @Invoker("setLivingEntityFlag")
   void invokeSetLivingEntityFlag(int flag, boolean value);

   // Krs parity (RotationManager.updateVanillaRenderBodyYaw reads attackAnim):
   // 26.3 replaced the public LivingEntity#attackAnim field with the private
   // LivingEntity$SwingState#animation value; expose the holder for the exact
   // swing-progress condition (see SwingStateAccessor).
   @Accessor("swingState")
   LivingEntity.SwingState getSwingState();
}
