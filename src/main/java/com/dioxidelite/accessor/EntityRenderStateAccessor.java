package com.dioxidelite.accessor;

import net.minecraft.world.entity.Entity;

/** Carries the source entity alongside vanilla's extracted render state. */
public interface EntityRenderStateAccessor {

    Entity DioxideLite$getEntity();

    void DioxideLite$setEntity(Entity entity);
}
