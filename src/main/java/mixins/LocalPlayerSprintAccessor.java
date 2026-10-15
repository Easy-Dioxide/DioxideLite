package mixins;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Krs parity (com.instrumentalist.mixin.injector.LocalPlayerSprintAccessor):
 * the two private LocalPlayer sprint-condition methods the ported Sprint2 module
 * reads in canStartSprintingLikeVanilla (Krs Sprint parity).
 */
@Mixin(LocalPlayer.class)
public interface LocalPlayerSprintAccessor {
   @Invoker("isSprintingPossible")
   boolean invokeIsSprintingPossible(boolean isFlying);

   @Invoker("isSlowDueToUsingItem")
   boolean invokeIsSlowDueToUsingItem();
}
