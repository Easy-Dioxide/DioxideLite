package mixins.krs;

import com.dioxideliteng.krs.hacks.features.movement.EntityControl;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TamableAnimal.class)
public abstract class MixinKrsTamableAnimal {

    @ModifyReturnValue(method = "isTame", at = @At("RETURN"))
    private boolean entityControlTameHook(boolean original) {
        return original || EntityControl.shouldTreatAsTamed((TamableAnimal) (Object) this);
    }
}
