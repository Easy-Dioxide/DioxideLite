package com.dioxideliteng.krs.utils.render;

import net.minecraft.world.entity.Entity;

/**
 * Lets render-state hooks know which entity a render state was extracted from. Implemented on
 * {@code EntityRenderState} by {@code mixins.krs.MixinKrsEntityRenderState}.
 *
 * <p>Deliberately outside the {@code mixins} package: Mixin refuses to load plain (non-mixin) classes from its own
 * package, and interfaces implemented by mixins have to be loadable from ordinary code.
 */
public interface IEntityRenderState {
    Entity client$getEntity();

    void client$setEntity(Entity entity);
}
