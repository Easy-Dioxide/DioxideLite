package com.dioxidelite.mixin;

import com.dioxidelite.accessor.EntityRenderStateAccessor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Adds a back-reference to the source entity on the render state. */
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateAccessor {

    @Unique
    private Entity DioxideLite$entity;

    @Override
    public Entity DioxideLite$getEntity() {
        return DioxideLite$entity;
    }

    @Override
    public void DioxideLite$setEntity(Entity entity) {
        this.DioxideLite$entity = entity;
    }
}
