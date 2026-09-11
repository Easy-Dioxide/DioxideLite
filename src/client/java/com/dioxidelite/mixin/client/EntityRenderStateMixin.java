package com.dioxidelite.mixin.client;

import com.dioxidelite.client.util.NameTagPlayerFilterState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements NameTagPlayerFilterState {
    @Unique private boolean dioxide_lite$nameTagRealPlayer = true;

    @Override
    public void dioxide_lite$setNameTagRealPlayer(boolean realPlayer) {
        dioxide_lite$nameTagRealPlayer = realPlayer;
    }

    @Override
    public boolean dioxide_lite$isNameTagRealPlayer() {
        return dioxide_lite$nameTagRealPlayer;
    }
}
