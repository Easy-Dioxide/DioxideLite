package mixins.krs;

import com.dioxideliteng.krs.utils.render.IEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Stores the source entity on every entity render state. */
@Mixin(EntityRenderState.class)
public abstract class MixinKrsEntityRenderState implements IEntityRenderState {
    @Unique
    private Entity krs$entity;

    @Override
    public Entity client$getEntity() {
        return krs$entity;
    }

    @Override
    public void client$setEntity(Entity entity) {
        this.krs$entity = entity;
    }
}
