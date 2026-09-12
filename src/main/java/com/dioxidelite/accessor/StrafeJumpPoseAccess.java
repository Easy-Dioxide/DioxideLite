package com.dioxidelite.accessor;

/** Render-only view of the virtual pose reported by Speed's 45-degree mode. */
public interface StrafeJumpPoseAccess {

    float DioxideLite$getVisualBodyOffset(float partialTick);

    float DioxideLite$getVisualHeadOffset(float partialTick);

    boolean DioxideLite$isSynchronizedStrafeTick();
}
