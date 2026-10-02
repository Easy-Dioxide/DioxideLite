package com.dioxidelite.manager;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（来源开源版）com/dioxidelite/manager/RotationManager.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.AfterRotationEvent;
import com.dioxidelite.event.events.AttackYawEvent;
import com.dioxidelite.event.events.FallFlyingEvent;
import com.dioxidelite.event.events.JumpEvent;
import com.dioxidelite.event.events.KeyboardInputEvent;
import com.dioxidelite.event.events.PacketEvent;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.event.events.RaytraceEvent;
import com.dioxidelite.event.events.RespawnEvent;
import com.dioxidelite.event.events.RotationAnimationEvent;
import com.dioxidelite.event.events.SendPositionEvent;
import com.dioxidelite.event.events.StrafeEvent;
import com.dioxidelite.module.modules.movement.MovementFix;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.util.Mth;

import java.util.function.Function;

/**
 * Server-side (silent) rotation engine. Modules request rotations via
 * {@link #setRotations}; the manager smooths them each player tick and rewrites
 * outgoing {@link SendPositionEvent}s so the server sees the aimed rotation while
 * the client camera stays put.
 */
public final class RotationManager {

    public static final RotationManager INSTANCE = new RotationManager();

    private static final Minecraft mc = Minecraft.getInstance();

    private final Rot2f offset = new Rot2f(0, 0);
    public Rot2f rotations = new Rot2f(0, 0);
    public Rot2f lastRotations = new Rot2f(0, 0);
    public Rot2f targetRotations;
    public Rot2f animationRotation;
    public Rot2f lastAnimationRotation;

    private boolean active;
    private boolean smoothed;
    private double rotationSpeed;
    private Function<Rot2f, Boolean> raytrace;
    private float randomAngle;
    private boolean s08;
    private boolean renderAnimation = true;

    private int priority;
    private Runnable callback;
    private Object transientOwner;

    private RotationManager() {
        EventBus.INSTANCE.subscribe(this);
    }

    public void setRotations(Rot2f rotations, double rotationSpeed) {
        setRotations(rotations, rotationSpeed, null, Priority.Medium, null);
    }

    public void setRotations(Rot2f rotations, double rotationSpeed, Priority priority) {
        setRotations(rotations, rotationSpeed, null, priority, null);
    }

    public void setRotations(
            Rot2f rotations,
            double rotationSpeed,
            Priority priority,
            boolean renderAnimation) {
        setRotations(rotations, rotationSpeed, null, priority, null, renderAnimation);
    }

    public void setRotations(Rot2f rotations, double rotationSpeed, Function<Rot2f, Boolean> raytrace) {
        setRotations(rotations, rotationSpeed, raytrace, Priority.Medium, null);
    }

    public void setRotations(Rot2f rotations, double rotationSpeed, Function<Rot2f, Boolean> raytrace, Priority priority) {
        setRotations(rotations, rotationSpeed, raytrace, priority, null);
    }

    public void setRotations(Rot2f rotations, double rotationSpeed, Function<Rot2f, Boolean> raytrace, Priority priority, Runnable callback) {
        setRotations(rotations, rotationSpeed, raytrace, priority, callback, true);
    }

    /** Applies a rotation model's already-stepped result without smoothing it a second time. */
    public void setRotationsDirect(Rot2f rotations, Priority priority) {
        if (rotations == null || mc.player == null) return;
        if (this.active && priority.priority < this.priority) return;

        if (s08) {
            this.rotations = this.lastRotations = this.targetRotations =
                    new Rot2f(mc.player.getYRot(), mc.player.getXRot());
            this.callback = null;
            this.transientOwner = null;
            s08 = false;
            return;
        }

        this.rotations = rotations;
        this.targetRotations = rotations;
        this.raytrace = null;
        this.priority = priority.priority;
        this.callback = null;
        this.transientOwner = null;
        this.renderAnimation = true;
        this.active = true;
        this.smoothed = true;

        MovementFix movementFix = MovementFix.INSTANCE;
        if (movementFix.isEnabled() && movementFix.shouldChangeLook()) {
            mc.player.setYRot(rotations.getYaw());
            mc.player.setXRot(rotations.getPitch());
            mc.player.yHeadRot = rotations.getYaw();
            mc.player.yBodyRot = rotations.getYaw();
        }
        mc.pick(1.0F);
    }

    /**
     * Claims a packet-scoped silent rotation without changing the camera,
     * animation, ray trace, or movement input. Existing rotations of equal or
     * higher priority keep ownership.
     */
    public boolean claimSilentRotation(Object owner, Rot2f rotations, Priority priority) {
        if (owner == null || rotations == null || mc.player == null) return false;
        if (this.active && this.transientOwner != owner && priority.priority <= this.priority) {
            return false;
        }

        if (s08) {
            this.rotations = this.lastRotations = this.targetRotations =
                    new Rot2f(mc.player.getYRot(), mc.player.getXRot());
            this.callback = null;
            this.transientOwner = null;
            s08 = false;
            return false;
        }

        this.rotations = rotations;
        this.targetRotations = rotations;
        this.raytrace = null;
        this.priority = priority.priority;
        this.callback = null;
        this.transientOwner = owner;
        this.renderAnimation = false;
        this.active = true;
        this.smoothed = true;
        return true;
    }

    /** Releases only the packet-scoped rotation claimed by the same owner. */
    public void releaseSilentRotation(Object owner) {
        if (owner == null || this.transientOwner != owner) return;

        this.active = false;
        this.priority = 0;
        this.callback = null;
        this.transientOwner = null;
        this.raytrace = null;
        this.smoothed = false;
        this.renderAnimation = true;
        if (mc.player != null) {
            this.targetRotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());
        }
    }

    private void setRotations(
            Rot2f rotations,
            double rotationSpeed,
            Function<Rot2f, Boolean> raytrace,
            Priority priority,
            Runnable callback,
            boolean renderAnimation) {
        if (rotations == null || mc.player == null) return;

        if (this.active && priority.priority < this.priority) {
            return;
        }

        if (s08) {
            this.rotations = this.lastRotations = this.targetRotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());
            this.callback = null;
            this.transientOwner = null;
            s08 = false;
            return;
        }

        this.targetRotations = rotations;
        this.rotationSpeed = rotationSpeed * 18.0;
        this.raytrace = raytrace;
        this.priority = priority.priority;
        this.callback = callback;
        this.transientOwner = null;
        this.renderAnimation = renderAnimation;
        this.active = true;

        smooth();
    }

    private void smooth() {
        if (mc.player == null) {
            resetState();
            return;
        }

        if (!smoothed) {
            float targetYaw = targetRotations.getYaw();
            float targetPitch = targetRotations.getPitch();

            if (raytrace != null && (Math.abs(targetYaw - rotations.getYaw()) > 5 || Math.abs(targetPitch - rotations.getPitch()) > 5)) {
                final Rot2f trueTargetRotations = new Rot2f(targetRotations.getYaw(), targetRotations.getPitch());
                double speed = (Math.random() * Math.random() * Math.random()) * 20;
                randomAngle += (float) ((20 + (float) (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360)) * (mc.player.tickCount / 10 % 2 == 0 ? -1 : 1));

                offset.set(
                        (float) (offset.getYaw() + -Mth.sin((float) Math.toRadians(randomAngle)) * speed),
                        (float) (offset.getPitch() + Mth.cos((float) Math.toRadians(randomAngle)) * speed)
                );
                targetYaw += offset.getYaw();
                targetPitch += offset.getPitch();

                if (!raytrace.apply(new Rot2f(targetYaw, targetPitch))) {
                    randomAngle = (float) Math.toDegrees(Math.atan2(trueTargetRotations.getYaw() - targetYaw, targetPitch - trueTargetRotations.getPitch())) - 180;
                    targetYaw -= offset.getYaw();
                    targetPitch -= offset.getPitch();
                    offset.set(
                            (float) (offset.getYaw() + -Mth.sin((float) Math.toRadians(randomAngle)) * speed),
                            (float) (offset.getPitch() + Mth.cos((float) Math.toRadians(randomAngle)) * speed)
                    );
                    targetYaw = targetYaw + offset.getYaw();
                    targetPitch = targetPitch + offset.getPitch();
                }

                if (!raytrace.apply(new Rot2f(targetYaw, targetPitch))) {
                    offset.set(0, 0);
                    targetYaw = (float) (targetRotations.getYaw() + Math.random() * 2);
                    targetPitch = (float) (targetRotations.getPitch() + Math.random() * 2);
                }
            }

            // Vape 风格 PID 加速步进（替换原 RotationUtils.smooth 的简单 lerp）
            // - 步长基于鼠标灵敏度（getMouseScale），与真人手搓输入一致，绕过 Grim/Matrix/NCP 的
            //   旋转速度/加速度检测
            // - 加速度模式 angle-based：剩余角度越大步进越快，但不会超过每 tick 上限
            // - 微抖动避免完美瞄准被检测
            rotations = vapeSmooth(targetYaw, targetPitch);
        }

        smoothed = true;

        MovementFix movementFix = MovementFix.INSTANCE;
        if (movementFix.isEnabled() && movementFix.shouldChangeLook()) {
            mc.player.setYRot(rotations.getYaw());
            mc.player.setXRot(rotations.getPitch());
            mc.player.yHeadRot = rotations.getYaw();
            mc.player.yBodyRot = rotations.getYaw();
        }

        mc.pick(1.0f);
    }

    /**
     * Vape 风格旋转步进（移植自 gg.vape.rotation.FixedRotationController）。
     * 每 tick 以「鼠标灵敏度单位」步进，剩余角度越大加速度越高，模拟真人甩枪轨迹。
     * 绕过 Grim（rotation velocity/acceleration）、Matrix（snap/angle 检测）、
     * NCP（rotation speed/head-body consistency）。
     */
    private Rot2f vapeSmooth(float targetYaw, float targetPitch) {
        float curYaw = lastRotations.getYaw();
        float curPitch = lastRotations.getPitch();

        float yawError = Mth.wrapDegrees(targetYaw - curYaw);
        float pitchError = Mth.wrapDegrees(targetPitch - curPitch);
        float absYaw = Math.abs(yawError);
        float absPitch = Math.abs(pitchError);

        // 鼠标灵敏度系数（与游戏内 mouseSensitivity 换算一致）
        double sens = mc.options.sensitivity().get();
        float mouseScale = (float) (sens * 0.6f + 0.2f);
        mouseScale = mouseScale * mouseScale * mouseScale * 8.0f;
        float rotationPerStep = mouseScale * 0.15f;

        float step = (float) (Math.max(rotationSpeed, 1.0) * 0.25);

        // 容差内直接对准（不再步进），避免在目标点抖动
        float tolerance = rotationPerStep * 1.5f;
        if (absYaw <= tolerance && absPitch <= tolerance) {
            return new Rot2f(targetYaw, targetPitch);
        }

        // yaw 步进 + angle-based 加速度
        if (absYaw > tolerance) {
            float yawStep = step;
            // 比例缩放：当 pitch 误差更大时，yaw 步进按比例减小
            if (absPitch > 0.001f) {
                float ratio = absYaw / absPitch;
                if (ratio < 1.0f) yawStep *= ratio;
            }
            // angle-based acceleration：剩余角度越大，步进越大（有上限）
            double accel = (225.0 + absYaw) / 180.0;
            yawStep *= (float) accel;
            // 限制单 tick 最大步数，避免被判定为 snap
            float maxSteps = absYaw / rotationPerStep;
            yawStep = Math.min(yawStep, maxSteps);
            curYaw += Math.signum(yawError) * yawStep * rotationPerStep;
        }

        // pitch 步进
        if (absPitch > tolerance) {
            float pitchStep = step;
            if (absYaw > 0.001f) {
                float ratio = absPitch / absYaw;
                if (ratio < 1.0f) pitchStep *= ratio;
            }
            double accel = (135.0 + absPitch) / 90.0;
            pitchStep *= (float) accel;
            float maxSteps = absPitch / rotationPerStep;
            pitchStep = Math.min(pitchStep, maxSteps);
            curPitch += Math.signum(pitchError) * pitchStep * rotationPerStep;
            curPitch = Mth.clamp(curPitch, -90f, 90f);
        }

        // 微抖动：避免每 tick 完美对准，真人不会每次都正中
        float jitterYaw = (float) ((Math.random() - 0.5) * rotationPerStep * 0.3);
        float jitterPitch = (float) ((Math.random() - 0.5) * rotationPerStep * 0.2);
        curYaw += jitterYaw;
        curPitch = Mth.clamp(curPitch + jitterPitch, -90f, 90f);

        return new Rot2f(curYaw, curPitch);
    }

    private void correctDisabledRotations() {
        if (mc.player == null) return;
        Rot2f rotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());
        Rot2f fixedRotations = RotationUtils.resetRotation(RotationUtils.applySensitivityPatch(rotations, lastRotations));
        mc.player.setYRot(fixedRotations.getYaw());
        mc.player.setXRot(fixedRotations.getPitch());
    }

    public float getYaw() {
        return getRotation().getYaw();
    }

    public float getPitch() {
        return getRotation().getPitch();
    }

    public Rot2f getRotation() {
        if (mc.player == null) return rotations != null ? rotations : new Rot2f(0, 0);
        return active ? rotations : new Rot2f(mc.player.getYRot(), mc.player.getXRot());
    }

    public Rot2f getLastRotation() {
        if (mc.player == null) return lastRotations != null ? lastRotations : new Rot2f(0, 0);
        return lastRotations != null ? lastRotations : new Rot2f(mc.player.yRotO, mc.player.xRotO);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.transientOwner = null;
        }
    }

    /** Whether silent rotations are intentionally mirrored to the third-person model. */
    public boolean isRenderAnimationEnabled() {
        return renderAnimation;
    }

    public boolean isSmoothed() {
        return smoothed;
    }

    public void setSmoothed(boolean smoothed) {
        this.smoothed = smoothed;
    }

    @Listen
    private void onRespawn(RespawnEvent event) {
        resetState();
    }

    private void resetState() {
        offset.set(0, 0);
        rotations = new Rot2f(0, 0);
        lastRotations = new Rot2f(0, 0);
        targetRotations = null;
        animationRotation = null;
        lastAnimationRotation = null;
        active = false;
        priority = 0;
        callback = null;
        transientOwner = null;
        smoothed = false;
        raytrace = null;
        randomAngle = 0;
        s08 = false;
        renderAnimation = true;
    }

    @Listen
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket || event.getPacket() instanceof ClientboundPlayerRotationPacket) {
            s08 = true;
        }
    }

    @Listen
    private void onRaytrace(RaytraceEvent event) {
        if (active && rotations != null) {
            event.setYaw(rotations.getYaw());
            event.setPitch(rotations.getPitch());
        }
    }

    @Listen
    private void onAnimation(RotationAnimationEvent event) {
        if (renderAnimation
                && active
                && animationRotation != null
                && lastAnimationRotation != null) {
            event.setYaw(animationRotation.getYaw());
            event.setLastYaw(lastAnimationRotation.getYaw());
            event.setPitch(animationRotation.getPitch());
            event.setLastPitch(lastAnimationRotation.getPitch());
        }
    }

    @Listen(priority = -1000)
    private void onPlayerTick(PlayerTickEvent.Pre event) {
        if (mc.player == null) {
            resetState();
            return;
        }

        if (!active || rotations == null || lastRotations == null || targetRotations == null) {
            rotations = lastRotations = targetRotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());
        }

        if (active) {
            smooth();
            EventBus.INSTANCE.post(new AfterRotationEvent());

            if (callback != null) {
                callback.run();
                callback = null;
            }
        }
    }

    @Listen(priority = com.dioxidelite.event.Priority.LOWEST)
    private void onSendPosition(SendPositionEvent event) {
        if (mc.player == null) {
            resetState();
            return;
        }

        if (active && rotations != null) {
            float yaw = rotations.getYaw();
            float pitch = rotations.getPitch();

            if (!Float.isNaN(yaw) && !Float.isNaN(pitch)) {
                event.setYaw(yaw);
                event.setPitch(pitch);
            }

            if (Math.abs((rotations.getYaw() - mc.player.getYRot()) % 360) < 1 && Math.abs((rotations.getPitch() - mc.player.getXRot())) < 1) {
                active = false;
                priority = 0;
                callback = null;
                transientOwner = null;
                this.correctDisabledRotations();
            }

            lastRotations = rotations;
        } else {
            lastRotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());
        }

        lastAnimationRotation = animationRotation;
        animationRotation = new Rot2f(event.getYaw(), event.getPitch());
        targetRotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());
        raytrace = null;
        smoothed = false;
    }

    @Listen(priority = com.dioxidelite.event.Priority.HIGH)
    private void onMoveInput(KeyboardInputEvent event) {
        if (mc.player == null) return;
        MovementFix movementFix = MovementFix.INSTANCE;
        if (movementFix.isEnabled()
                && movementFix.shouldFixInput()
                && active
                && rotations != null
                && !mc.player.isFallFlying()) {
            movementFix.fixMovement(event, rotations.getYaw());
        }
    }

    @Listen
    private void onStrafe(StrafeEvent event) {
        if (mc.player == null) return;
        MovementFix movementFix = MovementFix.INSTANCE;
        if (movementFix.isEnabled()
                && movementFix.shouldFixRotationYaw()
                && active
                && rotations != null
                && !mc.player.isFallFlying()) {
            event.setYaw(rotations.getYaw());
        }
    }

    @Listen
    private void onJump(JumpEvent event) {
        if (mc.player == null) return;
        MovementFix movementFix = MovementFix.INSTANCE;
        if (movementFix.isEnabled()
                && movementFix.shouldFixRotationYaw()
                && active
                && rotations != null
                && !mc.player.isFallFlying()) {
            event.setYaw(rotations.getYaw());
        }
    }

    @Listen
    private void onFallFlying(FallFlyingEvent event) {
        MovementFix movementFix = MovementFix.INSTANCE;
        if (movementFix.isEnabled()
                && movementFix.shouldFixRotationYaw()
                && active
                && rotations != null) {
            event.setYaw(rotations.getYaw());
            event.setPitch(rotations.getPitch());
        }
    }

    @Listen
    private void onAttack(AttackYawEvent event) {
        if (rotations != null) {
            event.setYaw(rotations.getYaw());
        }
    }
}
