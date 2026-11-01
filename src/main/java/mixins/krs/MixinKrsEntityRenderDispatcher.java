package mixins.krs;

import com.dioxideliteng.krs.utils.render.IEntityRenderState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Attaches the entity to the render state it was just extracted into (KRS's approach; one reliable point). */
@Mixin(EntityRenderDispatcher.class)
public abstract class MixinKrsEntityRenderDispatcher {

    @ModifyReturnValue(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity, S extends EntityRenderState> S krs$attachEntity(S state, E entity, float tickDelta) {
        ((IEntityRenderState) state).client$setEntity(entity);
        return state;
    }
}
