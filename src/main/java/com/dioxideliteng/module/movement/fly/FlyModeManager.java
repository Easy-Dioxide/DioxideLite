package com.dioxideliteng.module.movement.fly;

import com.dioxideliteng.event.impl.EventBlock;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventPreMotion;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.Fly2;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.fly.features.AirwalkFly;
import com.dioxideliteng.module.movement.fly.features.CreativeFly;
import com.dioxideliteng.module.movement.fly.features.FloatFly;
import com.dioxideliteng.module.movement.fly.features.Grim19To1181Fly;
import com.dioxideliteng.module.movement.fly.features.JetpackFly;
import com.dioxideliteng.module.movement.fly.features.MatrixFly;
import com.dioxideliteng.module.movement.fly.features.ModernMMCFly;
import com.dioxideliteng.module.movement.fly.features.NCPBoostFly;
import com.dioxideliteng.module.movement.fly.features.NegativePacketFly;
import com.dioxideliteng.module.movement.fly.features.TwoBTwoTJPControlFastFly;
import com.dioxideliteng.module.movement.fly.features.VanillaFly;
import com.dioxideliteng.module.movement.fly.features.VerusJetpackFly;
import com.dioxideliteng.module.movement.fly.features.VulcanGlideFly;
import com.dioxideliteng.setting.ModeSetting;
import java.util.Locale;

/**
 * Krs parity: FlyModeManager (mode registry + event fan-out of the Krs Fly
 * family). The switch table, the mode-switch detection in onTick and the event
 * forwarding order are kept identical to Krs.
 *
 * <p>Adaptations: the Krs static FlyModule hooks (onEnableFunctions /
 * onDisableFunctions) are instance methods on the owning Fly2 feature, reached
 * through the owner reference; the Krs MotionEvent dispatch (pre at
 * LocalPlayer#sendPosition HEAD, post after the move packet when callInPost was
 * set) is translated to Events.PRE_MOTION / Events.POST_MOTION.
 */
public class FlyModeManager {
    private final Fly2 owner;
    private final ModeSetting flyMode;
    public FlyEvent currentMode;
    private MotionEvent lastPreMotion;
    private boolean callInPost;

    public FlyModeManager(Fly2 owner, ModeSetting flyMode) {
        this.owner = owner;
        this.flyMode = flyMode;
        updateCurrentMode();
    }

    private void updateCurrentMode() {
        switch (this.flyMode.getValue().toLowerCase(Locale.ROOT)) {
            case "vanilla":
                currentMode = new VanillaFly(this.owner);
                break;
            case "jetpack":
                currentMode = new JetpackFly(this.owner);
                break;
            case "matrix":
                currentMode = new MatrixFly(this.owner);
                break;
            case "grim 1.9-1.18.1":
                currentMode = new Grim19To1181Fly(this.owner);
                break;
            case "creative":
                currentMode = new CreativeFly(this.owner);
                break;
            case "airwalk":
                currentMode = new AirwalkFly(this.owner);
                break;
            case "negative packet":
                currentMode = new NegativePacketFly(this.owner);
                break;
            case "2b2tjp control fast":
                currentMode = new TwoBTwoTJPControlFastFly(this.owner);
                break;
            case "modern mmc":
                currentMode = new ModernMMCFly(this.owner);
                break;
            case "vulcan glide":
                currentMode = new VulcanGlideFly(this.owner);
                break;
            case "verus jetpack":
                currentMode = new VerusJetpackFly(this.owner);
                break;
            case "float":
                currentMode = new FloatFly(this.owner);
                break;
            case "ncp boost":
                currentMode = new NCPBoostFly(this.owner);
                break;
            default:
                currentMode = null;
        }
    }

    public void onUpdate(EventUpdate event) {
        FlyEvent mode = currentMode;
        if (mode != null)
            mode.onUpdate(event);
    }

    /**
     * Krs parity: MotionEvent pre phase. The payload is translated to the
     * client's PRE_MOTION event (same injection point and same mutable fields as
     * the Krs dispatch: x/y/z, onGround, horizontalCollision; cancel suppresses
     * the outgoing move packet).
     */
    public void onPreMotion(EventPreMotion event) {
        FlyEvent mode = currentMode;
        if (mode == null) return;

        MotionEvent motion = new MotionEvent(
                event.getX(), event.getY(), event.getZ(),
                event.getYaw(), event.getPitch(),
                event.isOnGround(), false, false, false
        );
        mode.onMotion(motion);

        if (motion.isCancelled())
            event.setCancelled(true);
        event.setX(motion.x);
        event.setY(motion.y);
        event.setZ(motion.z);
        event.setOnGround(motion.onGround);

        this.lastPreMotion = motion;
        this.callInPost = motion.callInPost;
    }

    /** Krs parity: MotionEvent post phase, dispatched only when a mode set callInPost. */
    public void onPostMotion() {
        FlyEvent mode = currentMode;
        if (mode == null || lastPreMotion == null) return;

        MotionEvent post = new MotionEvent(
                lastPreMotion.x, lastPreMotion.y, lastPreMotion.z,
                lastPreMotion.yaw, lastPreMotion.pitch,
                lastPreMotion.onGround, false, false, true
        );
        mode.onMotion(post);
    }

    public boolean consumePostRequest() {
        boolean requested = this.callInPost;
        this.callInPost = false;
        return requested;
    }

    public void onTick(EventTick event) {
        FlyEvent mode = currentMode;
        if (mode == null || !this.flyMode.getValue().equals(mode.getName())) {
            this.owner.onDisableFunctions();
            updateCurrentMode();
            this.owner.onEnableFunctions();
            mode = currentMode;
        }

        if (mode != null)
            mode.onTick(event);
    }

    public void onSendPacket(EventPacketSend event) {
        FlyEvent mode = currentMode;
        if (mode != null)
            mode.onSendPacket(event);
    }

    public void onReceivedPacket(EventPacketReceive event) {
        FlyEvent mode = currentMode;
        if (mode != null)
            mode.onReceivedPacket(event);
    }

    public void onBlock(EventBlock event) {
        FlyEvent mode = currentMode;
        if (mode != null)
            mode.onBlock(event);
    }
}
