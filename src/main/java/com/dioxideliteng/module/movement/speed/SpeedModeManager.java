package com.dioxideliteng.module.movement.speed;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventPreMotion;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.Fly2;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.speed.features.BoostSpeed;
import com.dioxideliteng.module.movement.speed.features.FiveBFiveTSpeed;
import com.dioxideliteng.module.movement.speed.features.FlagBoostSpeed;
import com.dioxideliteng.module.movement.speed.features.HypixelNCPHopSpeed;
import com.dioxideliteng.module.movement.speed.features.MinibloxSpeed;
import com.dioxideliteng.module.movement.speed.features.ModernMMCSpeed;
import com.dioxideliteng.module.movement.speed.features.NCPSpeed;
import com.dioxideliteng.module.movement.speed.features.SmoothVanillaSpeed;
import com.dioxideliteng.module.movement.speed.features.VanillaSpeed;
import com.dioxideliteng.module.movement.speed.features.VerusSpeed;
import com.dioxideliteng.module.movement.speed.features.VulcanOldSpeed;
import com.dioxideliteng.module.movement.speed.features.VulcanSpeed;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.ModeSetting;
import java.util.Locale;

/**
 * Krs parity: SpeedModeManager (mode registry + event fan-out of the Krs Speed
 * family). The switch table, the mode-switch detection in onTick and the event
 * forwarding order are kept identical to Krs, including the Fly-enabled gate on
 * every dispatch (Krs ModuleManager.getModuleState(FlyModule.class)).
 *
 * <p>Adaptations: the Krs static SpeedModule hooks (onEnableFunctions /
 * onDisableFunctions) are instance methods on the owning Speed2 feature,
 * reached through the owner reference; the Krs MotionEvent dispatch is
 * translated to Events.PRE_MOTION / Events.POST_MOTION; the Fly-enabled check
 * looks the Fly2 feature up in FeatureManager (no static field exists for it,
 * and FeatureManager must not be modified for this port).
 */
public class SpeedModeManager {
    private final Speed2 owner;
    private final ModeSetting speedMode;
    public SpeedEvent currentMode;
    private MotionEvent lastPreMotion;
    private boolean callInPost;

    public SpeedModeManager(Speed2 owner, ModeSetting speedMode) {
        this.owner = owner;
        this.speedMode = speedMode;
        updateCurrentMode();
    }

    private void updateCurrentMode() {
        switch (this.speedMode.getValue().toLowerCase(Locale.ROOT)) {
            case "vanilla":
                currentMode = new VanillaSpeed(this.owner);
                break;
            case "smooth vanilla":
                currentMode = new SmoothVanillaSpeed(this.owner);
                break;
            case "hypixel ncp hop":
                currentMode = new HypixelNCPHopSpeed(this.owner);
                break;
            case "modern mmc":
                currentMode = new ModernMMCSpeed(this.owner);
                break;
            case "5b5t":
                currentMode = new FiveBFiveTSpeed(this.owner);
                break;
            case "boost":
                currentMode = new BoostSpeed(this.owner);
                break;
            case "ncp":
                currentMode = new NCPSpeed(this.owner);
                break;
            case "flag boost":
                currentMode = new FlagBoostSpeed(this.owner);
                break;
            case "verus":
                currentMode = new VerusSpeed(this.owner);
                break;
            case "vulcan":
                currentMode = new VulcanSpeed(this.owner);
                break;
            case "vulcan old":
                currentMode = new VulcanOldSpeed(this.owner);
                break;
            case "miniblox":
                currentMode = new MinibloxSpeed(this.owner);
                break;
            default:
                currentMode = null;
        }
    }

    /** Krs parity: ModuleManager.getModuleState(FlyModule.class). */
    private boolean isFlyEnabled() {
        for (Feature feature : FeatureManager.getModules()) {
            if (feature instanceof Fly2 fly2)
                return fly2.isEnabled();
        }
        return false;
    }

    public void onUpdate(EventUpdate event) {
        SpeedEvent mode = currentMode;
        if (!isFlyEnabled() && mode != null)
            mode.onUpdate(event);
    }

    /**
     * Krs parity: MotionEvent pre phase. The payload is translated to the
     * client's PRE_MOTION event (same injection point and same mutable fields as
     * the Krs dispatch: x/y/z, onGround, horizontalCollision; cancel suppresses
     * the outgoing move packet).
     */
    public void onPreMotion(EventPreMotion event) {
        SpeedEvent mode = currentMode;
        if (mode == null || isFlyEnabled()) return;

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
        SpeedEvent mode = currentMode;
        if (mode == null || lastPreMotion == null || isFlyEnabled()) return;

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
        SpeedEvent mode = currentMode;
        if (mode == null || !this.speedMode.getValue().equals(mode.getName())) {
            this.owner.onDisableFunctions();
            updateCurrentMode();
            this.owner.onEnableFunctions();
            mode = currentMode;
        }

        if (!isFlyEnabled() && mode != null)
            mode.onTick(event);
    }

    public void onSendPacket(EventPacketSend event) {
        SpeedEvent mode = currentMode;
        if (!isFlyEnabled() && mode != null)
            mode.onSendPacket(event);
    }

    public void onReceivedPacket(EventPacketReceive event) {
        SpeedEvent mode = currentMode;
        if (!isFlyEnabled() && mode != null)
            mode.onReceivedPacket(event);
    }
}
