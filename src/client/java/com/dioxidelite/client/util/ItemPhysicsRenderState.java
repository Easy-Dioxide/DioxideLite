package com.dioxidelite.client.util;

public interface ItemPhysicsRenderState {
    void dioxide_lite$setItemPhysics(boolean onGround, boolean moving, boolean blockItem, int seed);

    boolean dioxide_lite$itemPhysicsOnGround();

    boolean dioxide_lite$itemPhysicsMoving();

    boolean dioxide_lite$itemPhysicsBlockItem();

    int dioxide_lite$itemPhysicsSeed();
}
