package com.dioxideliteng.module.movement;

/**
 * Krs parity: the MotionEvent payload the Fly/Speed mode managers hand to their
 * modes (Krs com.instrumentalist.krs.events.features.MotionEvent). Field names
 * and semantics are kept identical to Krs so the ported modes stay unchanged:
 * x/y/z, yaw/pitch, onGround, isMoving, callInPost, post and cancel().
 *
 * <p>The mode managers translate this payload to/from the client's
 * Events.PRE_MOTION / Events.POST_MOTION pair, which is the exact analog of the
 * Krs dispatch point (LocalPlayer#sendPosition HEAD, with the post event fired
 * after the move packet when a mode requested callInPost).
 *
 * <p>isMoving is always false in this port: Krs computes it in its rewritten
 * sendPosition from the last-sent position deltas, which the client mixin does
 * not track; no Fly/Speed mode reads the field.
 */
public class MotionEvent {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public boolean onGround;
    public boolean isMoving;
    public boolean callInPost;
    public final boolean post;
    private boolean cancelled;

    public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround,
                       boolean isMoving, boolean callInPost, boolean post) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.isMoving = isMoving;
        this.callInPost = callInPost;
        this.post = post;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }
}
