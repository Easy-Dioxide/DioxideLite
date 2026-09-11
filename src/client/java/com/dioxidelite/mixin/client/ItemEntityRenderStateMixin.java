package com.dioxidelite.mixin.client;

import com.dioxidelite.client.util.ItemPhysicsRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemPhysicsRenderState {
    @Unique private boolean dioxide_lite$itemPhysicsOnGround;
    @Unique private boolean dioxide_lite$itemPhysicsMoving;
    @Unique private boolean dioxide_lite$itemPhysicsBlockItem;
    @Unique private int dioxide_lite$itemPhysicsSeed;

    @Override
    public void dioxide_lite$setItemPhysics(boolean onGround, boolean moving, boolean blockItem, int seed) {
        this.dioxide_lite$itemPhysicsOnGround = onGround;
        this.dioxide_lite$itemPhysicsMoving = moving;
        this.dioxide_lite$itemPhysicsBlockItem = blockItem;
        this.dioxide_lite$itemPhysicsSeed = seed;
    }

    @Override
    public boolean dioxide_lite$itemPhysicsOnGround() {
        return dioxide_lite$itemPhysicsOnGround;
    }

    @Override
    public boolean dioxide_lite$itemPhysicsMoving() {
        return dioxide_lite$itemPhysicsMoving;
    }

    @Override
    public boolean dioxide_lite$itemPhysicsBlockItem() {
        return dioxide_lite$itemPhysicsBlockItem;
    }

    @Override
    public int dioxide_lite$itemPhysicsSeed() {
        return dioxide_lite$itemPhysicsSeed;
    }
}
