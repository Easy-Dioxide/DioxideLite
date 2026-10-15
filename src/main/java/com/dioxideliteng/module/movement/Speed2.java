package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.movement.speed.SpeedModeManager;
import com.dioxideliteng.module.movement.speed.features.BoostSpeed;
import com.dioxideliteng.module.movement.speed.features.FiveBFiveTSpeed;
import com.dioxideliteng.module.movement.speed.features.FlagBoostSpeed;
import com.dioxideliteng.module.movement.speed.features.HypixelNCPHopSpeed;
import com.dioxideliteng.module.movement.speed.features.MinibloxSpeed;
import com.dioxideliteng.module.movement.speed.features.NCPSpeed;
import com.dioxideliteng.module.movement.speed.features.VanillaSpeed;
import com.dioxideliteng.module.movement.speed.features.VerusSpeed;
import com.dioxideliteng.module.movement.speed.features.VulcanOldSpeed;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.TimerUtil;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.movement.speed.SpeedModule)
 * and adapted to the DioxideLiteNG client API. The Speed family keeps the Krs
 * architecture: this feature owns the settings and the SpeedModeManager, the
 * 12 modes live in the movement.speed.features package as SpeedEvent
 * implementors (not registered features), and mode selection / priority /
 * event flow match Krs exactly (including the Fly-enabled gate on every
 * dispatch).
 *
 * <p>Notable adaptations vs the original:
 *  - Krs ListValue/BooleanValue/FloatValue map to ModeSetting/BooleanSetting/
 *    NumberSetting; the Krs visibility predicates map to Setting#setVisible
 *    with the same conditions.
 *  - Krs onUpdate -> Events.UPDATE; Krs onMotion -> Events.PRE_MOTION with the
 *    post phase on Events.POST_MOTION (callInPost); Krs onTick -> Events.TICK;
 *    Krs onSendPacket/onReceivedPacket -> EventPacketSend/EventPacketReceive
 *    (instanceof).
 *  - Krs ModuleManager.getModuleState(FlyModule.class) is resolved through a
 *    Fly2 lookup in FeatureManager (see SpeedModeManager#isFlyEnabled).
 */
public class Speed2 extends Feature {
    public final ModeSetting speedMode = new ModeSetting("Speed Mode", this, "Vanilla",
            new String[]{"Vanilla", "Smooth Vanilla", "Hypixel NCP Hop", "Modern MMC", "5b5t",
                    "Boost", "Flag Boost", "NCP", "Verus", "Vulcan", "Vulcan Old", "Miniblox"});

    // Identify
    public final NumberSetting vanillaSpeed = new NumberSetting("Speed", this, 1.0, 0.1, 10.0, 0.1);

    // Vanilla
    public final BooleanSetting vanillaAutoBHop = new BooleanSetting("Auto BHop", this, true);

    // Vulcan Old
    public final ModeSetting vulcan2Mode = new ModeSetting("Vulcan Old Mode", this, "BHop",
            new String[]{"BHop", "LowHop", "Ground", "YPort"});

    private final SpeedModeManager speedModeManager;
    private boolean postMotionArmed;

    public Speed2() {
        super("Speed2", Category.MOVEMENT);

        // Krs visibility predicates (DisplayableCondition parity).
        this.vanillaSpeed.setVisible(() -> this.speedMode.getValue().equalsIgnoreCase("vanilla")
                || this.speedMode.getValue().equalsIgnoreCase("smooth vanilla")
                || this.speedMode.getValue().equalsIgnoreCase("flag boost"));
        this.vanillaAutoBHop.setVisible(() -> this.speedMode.getValue().equalsIgnoreCase("vanilla"));
        this.vulcan2Mode.setVisible(() -> this.speedMode.getValue().equalsIgnoreCase("vulcan old"));

        this.speedModeManager = new SpeedModeManager(this, this.speedMode);
    }

    // ===================== Krs SpeedModule.onEnableFunctions / onDisableFunctions =====================

    public void onEnableFunctions() {
        if (this.speedModeManager.currentMode instanceof VulcanOldSpeed vulcanOldSpeed)
            vulcanOldSpeed.reset();
    }

    public void onDisableFunctions() {
        if (this.speedModeManager.currentMode instanceof VanillaSpeed) {
            if (mc.player != null)
                MovementUtil.stopMoving();
            VanillaSpeed.cancelledOnce = false;
        } else if (this.speedModeManager.currentMode instanceof FlagBoostSpeed) {
            FlagBoostSpeed.tick1 = 0;
            if (mc.player != null)
                MovementUtil.stopXZ();
        } else if (this.speedModeManager.currentMode instanceof MinibloxSpeed) {
            if (mc.player != null)
                MovementUtil.stopMoving();
        } else if (this.speedModeManager.currentMode instanceof HypixelNCPHopSpeed) {
            HypixelNCPHopSpeed.canLowHop = false;
        } else if (this.speedModeManager.currentMode instanceof VulcanOldSpeed vulcanOldSpeed) {
            if (this.vulcan2Mode.getValue().equalsIgnoreCase("ground") && mc.player != null)
                VulcanOldSpeed.strafe(MovementUtil.getBaseMoveSpeed(0.2873) * 0.6 - 0.02);

            vulcanOldSpeed.reset();
        } else if (this.speedModeManager.currentMode instanceof NCPSpeed) {
            if (mc.player != null)
                MovementUtil.stopMoving();
        } else if (this.speedModeManager.currentMode instanceof VerusSpeed) {
            if (mc.player != null)
                MovementUtil.stopMoving();
        } else if (this.speedModeManager.currentMode instanceof BoostSpeed) {
            if (mc.player != null)
                MovementUtil.stopMoving();
            BoostSpeed.boostSpeed = 0f;
        } else if (this.speedModeManager.currentMode instanceof FiveBFiveTSpeed) {
            if (mc.player != null)
                MovementUtil.stopMoving();
            TimerUtil.reset();
        }

        if (mc.player != null) {
            mc.options.keyJump.setDown(InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyJump.saveString()).getValue()));
        }
    }

    @Override
    public void onEnable() {
        onEnableFunctions();
        this.setSuffix(this.speedMode.getValue());
    }

    @Override
    public void onDisable() {
        onDisableFunctions();
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.UPDATE) {
            this.speedModeManager.onUpdate(Events.UPDATE);
        } else if (event == Events.PRE_MOTION) {
            // Krs parity: the post MotionEvent only fires when a mode set
            // callInPost during the pre phase; re-arm on every pre phase so a
            // suppressed post (cancelled pre) cannot leak into the next tick.
            this.postMotionArmed = false;
            this.speedModeManager.onPreMotion(Events.PRE_MOTION);
            this.postMotionArmed = this.speedModeManager.consumePostRequest();
        } else if (event == Events.POST_MOTION) {
            if (this.postMotionArmed) {
                this.postMotionArmed = false;
                this.speedModeManager.onPostMotion();
            }
        } else if (event == Events.TICK) {
            this.speedModeManager.onTick(Events.TICK);
            this.setSuffix(this.speedMode.getValue());
        } else if (event instanceof EventPacketSend sending) {
            this.speedModeManager.onSendPacket(sending);
        } else if (event instanceof EventPacketReceive receiving) {
            this.speedModeManager.onReceivedPacket(receiving);
        }
    }
}
