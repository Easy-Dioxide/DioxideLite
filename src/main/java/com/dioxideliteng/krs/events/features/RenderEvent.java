package com.dioxideliteng.krs.events.features;

import com.dioxideliteng.krs.events.EventArgument;
import com.dioxideliteng.krs.events.EventListener;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;

public class RenderEvent extends EventArgument {
    public final PoseStack matrix;
    public final float partialTicks;

    public RenderEvent(PoseStack matrix, float partialTicks) {
        this.matrix = matrix;
        this.partialTicks = partialTicks;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onRender(this);
    }
}
