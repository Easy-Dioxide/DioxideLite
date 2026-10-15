package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventBlock;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.movement.fly.FlyModeManager;
import com.dioxideliteng.module.movement.fly.features.CreativeFly;
import com.dioxideliteng.module.movement.fly.features.Grim19To1181Fly;
import com.dioxideliteng.module.movement.fly.features.MatrixFly;
import com.dioxideliteng.module.movement.fly.features.ModernMMCFly;
import com.dioxideliteng.module.movement.fly.features.NCPBoostFly;
import com.dioxideliteng.module.movement.fly.features.NegativePacketFly;
import com.dioxideliteng.module.movement.fly.features.TwoBTwoTJPControlFastFly;
import com.dioxideliteng.module.movement.fly.features.VanillaFly;
import com.dioxideliteng.module.movement.fly.features.VulcanGlideFly;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.TimerUtil;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.movement.fly.FlyModule)
 * and adapted to the DioxideLiteNG client API. The Fly family keeps the Krs
 * architecture: this feature owns the settings and the FlyModeManager, the
 * 13 modes live in the movement.fly.features package as FlyEvent implementors
 * (not registered features), and mode selection / priority / event flow match
 * Krs exactly.
 *
 * <p>Notable adaptations vs the original:
 *  - Krs ListValue/BooleanValue/FloatValue/IntValue map to ModeSetting/
 *    BooleanSetting/NumberSetting; the Krs visibility predicates map to
 *    Setting#setVisible with the same conditions.
 *  - Krs onUpdate -> Events.UPDATE; Krs onMotion -> Events.PRE_MOTION with the
 *    post phase on Events.POST_MOTION (callInPost); Krs onTick -> Events.TICK;
 *    Krs onSendPacket/onReceivedPacket -> EventPacketSend/EventPacketReceive
 *    (instanceof); Krs onBlock -> EventBlock (instanceof).
 *  - Krs ModuleManager.getModule(FlyModule.class).setState(false) inside
 *    MatrixFly becomes the owner reference (this feature).
 *  - Krs InventoryMove.canMoveFreely() is inlined below: the native
 *    InventoryMove has no canMoveFreely/inventoryOnly, and the Krs EmptyScreen
 *    (imguimc) branch has no client equivalent, so the check reduces to
 *    "no screen open, or InventoryMove enabled with a non-chat screen"
 *    (Krs defaults make inventoryOnly a no-op).
 *  - The "x" suffix of the Grim Timer Speed value has no NumberSetting
 *    equivalent and is dropped (cosmetic only).
 */
public class Fly2 extends Feature {
    public final ModeSetting flyMode = new ModeSetting("Fly Mode", this, "Vanilla",
            new String[]{"Vanilla", "Creative", "Airwalk", "Jetpack", "Float", "NCP Boost",
                    "Negative Packet", "2b2tJP Control Fast", "Modern MMC", "Vulcan Glide",
                    "Verus Jetpack", "Matrix", "Grim 1.9-1.18.1"});

    public final BooleanSetting airViewBobbing = new BooleanSetting("Air View Bobbing", this, false);

    // Vanilla
    public final NumberSetting vanillaHSpeed = new NumberSetting("Horizontal Speed", this, 2.0, 0.1, 10.0, 0.1);
    public final NumberSetting vanillaVSpeed = new NumberSetting("Vertical Speed", this, 1.2, 0.1, 10.0, 0.1);

    // Matrix
    public final ModeSetting matrixMode = new ModeSetting("Matrix Mode", this, "Normal",
            new String[]{"Normal", "High", "Damage"});
    public final BooleanSetting matrixHighSelfDamage = new BooleanSetting("Matrix High Self Damage", this, false);
    public final NumberSetting matrixHighHeight = new NumberSetting("Matrix Height", this, 1.0, 0.42, 7.0, 0.01);
    public final NumberSetting matrixDamageTimerSpeed = new NumberSetting("Matrix Timer Speed", this, 30.0, 1.0, 100.0, 1.0);
    public final NumberSetting matrixDamageSpeed = new NumberSetting("Matrix Speed", this, 0.07, 0.0, 1.0, 0.01);
    public final BooleanSetting matrixDamageNoSpeed = new BooleanSetting("Matrix No Speed", this, false);
    public final BooleanSetting matrixDamageDetectDamage = new BooleanSetting("Matrix Detect Damage", this, true);
    public final BooleanSetting matrixDamageAutoDisable = new BooleanSetting("Matrix Auto Disable", this, true);
    public final NumberSetting matrixDamageFlyTicks = new NumberSetting("Matrix Fly Ticks", this, 1000.0, 0.0, 1600.0, 1.0);
    public final BooleanSetting matrixDamageSelfDamage = new BooleanSetting("Matrix Damage Self Damage", this, true);
    public final BooleanSetting matrixDamageNewSelfDamage = new BooleanSetting("Matrix New Self Damage", this, false);
    public final BooleanSetting matrixDamagePacket = new BooleanSetting("Matrix Packet", this, true);
    public final ModeSetting matrixDamageMotionY = new ModeSetting("Matrix Motion Y", this, "None",
            new String[]{"None", "Simple", "Multiply"});
    public final NumberSetting matrixDamageMotion = new NumberSetting("Matrix Motion", this, -0.01, -0.3, 0.3, 0.01);

    // Grim 1.9-1.18.1
    public final NumberSetting grimSpeed = new NumberSetting("Grim Speed", this, 0.275, 0.0, 0.32, 0.005);
    public final NumberSetting grimTimerSpeed = new NumberSetting("Grim Timer Speed", this, 1.0, 0.1, 10.0, 0.1);
    public final BooleanSetting grimSlowFall = new BooleanSetting("Grim Slow Fall", this, false);

    // Verus Jetpack
    public final BooleanSetting verusJetpackJumpKeyOnly = new BooleanSetting("Verus Jump Key Only", this, false);

    // NCP Boost
    public final BooleanSetting ncpDamageBoost = new BooleanSetting("NCP Damage Boost", this, true);

    private final FlyModeManager flyModeManager;
    private boolean postMotionArmed;

    public Fly2() {
        super("Fly2", Category.MOVEMENT);

        // Krs visibility predicates (DisplayableCondition parity).
        this.vanillaHSpeed.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("vanilla"));
        this.vanillaVSpeed.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("vanilla"));
        this.matrixMode.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix"));
        this.matrixHighSelfDamage.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("high"));
        this.matrixHighHeight.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("high"));
        this.matrixDamageTimerSpeed.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage"));
        this.matrixDamageSpeed.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage"));
        this.matrixDamageNoSpeed.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage"));
        this.matrixDamageDetectDamage.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage"));
        this.matrixDamageAutoDisable.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage"));
        this.matrixDamageFlyTicks.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage")
                && this.matrixDamageDetectDamage.getValue()
                && !this.matrixDamageAutoDisable.getValue());
        this.matrixDamageSelfDamage.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage"));
        this.matrixDamageNewSelfDamage.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage"));
        this.matrixDamagePacket.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage"));
        this.matrixDamageMotionY.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage"));
        this.matrixDamageMotion.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("matrix")
                && this.matrixMode.getValue().equalsIgnoreCase("damage")
                && !this.matrixDamageMotionY.getValue().equalsIgnoreCase("none"));
        this.grimSpeed.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("grim 1.9-1.18.1"));
        this.grimTimerSpeed.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("grim 1.9-1.18.1"));
        this.grimSlowFall.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("grim 1.9-1.18.1"));
        this.verusJetpackJumpKeyOnly.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("verus jetpack"));
        this.ncpDamageBoost.setVisible(() -> this.flyMode.getValue().equalsIgnoreCase("ncp boost"));

        this.flyModeManager = new FlyModeManager(this, this.flyMode);
    }

    // ===================== Krs FlyModule.onEnableFunctions / onDisableFunctions =====================

    public void onEnableFunctions() {
        if (this.flyModeManager.currentMode instanceof MatrixFly matrixFly) {
            matrixFly.reset();
        } else if (this.flyModeManager.currentMode instanceof Grim19To1181Fly grim19To1181Fly) {
            grim19To1181Fly.reset();
        }
    }

    public void onDisableFunctions() {
        if (this.flyModeManager.currentMode instanceof VanillaFly) {
            if (mc.player != null) {
                MovementUtil.stopMoving();
                MovementUtil.setVelocityY(0.0);
            }
        } else if (this.flyModeManager.currentMode instanceof TwoBTwoTJPControlFastFly) {
            if (mc.player != null) {
                MovementUtil.stopMoving();
                MovementUtil.setVelocityY(0.0);
            }
            TimerUtil.reset();
            TwoBTwoTJPControlFastFly.tick = 0;
            TwoBTwoTJPControlFastFly.phasing = false;
            TwoBTwoTJPControlFastFly.boostSpeed = 1f;
        } else if (this.flyModeManager.currentMode instanceof ModernMMCFly) {
            MovementUtil.stopMoving();
            MovementUtil.setVelocityY(0.0);
            ModernMMCFly.tick = 0;
        } else if (this.flyModeManager.currentMode instanceof CreativeFly) {
            if (mc.player != null) {
                if (mc.player.isSpectator() || !mc.player.isCreative())
                    mc.player.getAbilities().mayfly = false;
                if (!mc.player.isCreative())
                    mc.player.getAbilities().flying = false;
            }
        } else if (this.flyModeManager.currentMode instanceof NegativePacketFly) {
            NegativePacketFly.motion = Vec3.ZERO;
        } else if (this.flyModeManager.currentMode instanceof MatrixFly matrixFly) {
            matrixFly.disable();
        } else if (this.flyModeManager.currentMode instanceof Grim19To1181Fly grim19To1181Fly) {
            grim19To1181Fly.disable();
        } else if (this.flyModeManager.currentMode instanceof VulcanGlideFly) {
            VulcanGlideFly.ticks = 0;
        } else if (this.flyModeManager.currentMode instanceof NCPBoostFly) {
            NCPBoostFly.moveSpeed = 0;
            if (NCPBoostFly.timer) {
                TimerUtil.reset();
                NCPBoostFly.timer = false;
            }
            if (mc.player != null) {
                MovementUtil.stopMoving();
            }
        }
    }

    /**
     * Krs parity: InventoryMove.canMoveFreely(). See the class javadoc for the
     * adaptation notes (no EmptyScreen / inventoryOnly equivalents).
     */
    public static boolean canMoveFreely() {
        Screen screen = mc.gui.screen();
        return screen == null || (isInventoryMoveEnabled() && !(screen instanceof ChatScreen));
    }

    private static boolean isInventoryMoveEnabled() {
        for (Feature feature : FeatureManager.getModules()) {
            if (feature instanceof InventoryMove inventoryMove)
                return inventoryMove.isEnabled();
        }
        return false;
    }

    @Override
    public void onEnable() {
        onEnableFunctions();
        this.setSuffix(this.flyMode.getValue());
    }

    @Override
    public void onDisable() {
        onDisableFunctions();
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.UPDATE) {
            this.flyModeManager.onUpdate(Events.UPDATE);
        } else if (event == Events.PRE_MOTION) {
            // Krs parity: the post MotionEvent only fires when a mode set
            // callInPost during the pre phase; re-arm on every pre phase so a
            // suppressed post (cancelled pre) cannot leak into the next tick.
            this.postMotionArmed = false;
            this.flyModeManager.onPreMotion(Events.PRE_MOTION);
            this.postMotionArmed = this.flyModeManager.consumePostRequest();
        } else if (event == Events.POST_MOTION) {
            if (this.postMotionArmed) {
                this.postMotionArmed = false;
                this.flyModeManager.onPostMotion();
            }
        } else if (event == Events.TICK) {
            this.flyModeManager.onTick(Events.TICK);
            this.setSuffix(this.flyMode.getValue());
        } else if (event instanceof EventPacketSend sending) {
            this.flyModeManager.onSendPacket(sending);
        } else if (event instanceof EventPacketReceive receiving) {
            this.flyModeManager.onReceivedPacket(receiving);
        } else if (event instanceof EventBlock block) {
            this.flyModeManager.onBlock(block);
        }
    }
}
