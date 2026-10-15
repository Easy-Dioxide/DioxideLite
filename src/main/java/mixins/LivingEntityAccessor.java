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
}
