package com.dioxidelite.util.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 移植自 Vape-v4 的 PID 旋转控制器（gg.vape.rotation.AdaptiveRotationController +
 * FixedRotationController）。核心逻辑保留：以目标点/目标角度为 setpoint，
 * 每 tick 计算与当前角度的误差，按 speed * 0.25 * 加速度系数步进，
 * 支持 angle-based / linear / cubic 三种加速度、轴比例缩放、容差、aim jitter。
 * <p>
 * 与 DioxideLite RotationManager 的集成方式：模块每 tick 调用
 * {@link #update()} 后，把 {@link #getCurrentYaw()}/{@link #getCurrentPitch()}
 * 传给 {@code RotationManager.INSTANCE.setRotations(...)} 即可。
 * 这样既保留了 Vape 的 PID 手感，又复用了 DioxideLite 已有的静默转头发包链路。
 */
public final class AdaptiveRotationController {

    private static final Minecraft mc = Minecraft.getInstance();

    public static final float UNSET_ROTATION = -999.0f;

    private float targetYaw = UNSET_ROTATION;
    private float targetPitch = UNSET_ROTATION;
    private Vec3 targetPoint;

    private float currentYaw;
    private float currentPitch;

    private float speed = 1.0f;
    private float tolerance = 0.0f;

    private boolean scaleAxesProportionally = true;
    private boolean angleBasedAcceleration = false;
    private boolean linearAcceleration = false;
    private boolean cubicAcceleration = false;
    private boolean clampStepToRemaining = true;

    private boolean jitterEnabled = false;
    private float yawJitterAmplitude = 0.0f;
    private float pitchJitterAmplitude = 0.0f;
    private float yawJitterPhase = 0.0f;
    private float pitchJitterPhase = 0.0f;
    private float yawJitterSpeed = 0.0f;
    private float pitchJitterSpeed = 0.0f;
    private long lastJitterUpdate = 0L;
    private final java.util.Random random = new java.util.Random();

    private boolean complete = false;

    public AdaptiveRotationController() {
        if (mc.player != null) {
            currentYaw = mc.player.getYRot();
            currentPitch = mc.player.getXRot();
        }
    }

    // ---- target setters ----------------------------------------------------

    public void setTarget(Vec3 point) {
        this.targetPoint = point;
        this.targetYaw = UNSET_ROTATION;
        this.targetPitch = UNSET_ROTATION;
        this.complete = false;
    }

    public void setTargetRotation(float yaw, float pitch) {
        this.targetPoint = null;
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.complete = false;
    }

    public void clearTarget() {
        this.targetPoint = null;
        this.targetYaw = UNSET_ROTATION;
        this.targetPitch = UNSET_ROTATION;
        this.complete = true;
    }

    // ---- config setters ----------------------------------------------------

    public void setSpeed(float speed) { this.speed = speed; }
    public void setTolerance(float tolerance) { this.tolerance = tolerance; }
    public void setScaleAxesProportionally(boolean v) { this.scaleAxesProportionally = v; }
    public void setAngleBasedAcceleration(boolean v) { this.angleBasedAcceleration = v; }
    public void setLinearAcceleration(boolean v) { this.linearAcceleration = v; }
    public void setCubicAcceleration(boolean v) { this.cubicAcceleration = v; }
    public void setClampStepToRemaining(boolean v) { this.clampStepToRemaining = v; }
    public void setJitter(boolean enabled, float yawAmp, float pitchAmp) {
        this.jitterEnabled = enabled;
        this.yawJitterAmplitude = yawAmp;
        this.pitchJitterAmplitude = pitchAmp;
    }

    // ---- query -------------------------------------------------------------

    public float getCurrentYaw() { return currentYaw; }
    public float getCurrentPitch() { return currentPitch; }
    public boolean isComplete() { return complete; }
    public boolean hasTarget() { return targetPoint != null || targetYaw != UNSET_ROTATION; }

    // ---- per-tick update ---------------------------------------------------

    /** 计算目标角度并步进 currentYaw/currentPitch。返回 true 表示已对准。 */
    public boolean update() {
        if (mc.player == null) return true;

        // 解析目标角度
        float tYaw, tPitch;
        if (targetPoint != null) {
            Vec3 eye = mc.player.getEyePosition();
            double dx = targetPoint.x - eye.x;
            double dy = targetPoint.y - eye.y;
            double dz = targetPoint.z - eye.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            tYaw = (float) Math.toDegrees(-Math.atan2(dx, dz));
            tPitch = (float) Math.toDegrees(-Math.atan2(dy, dist));
            tPitch = Mth.clamp(tPitch, -90f, 90f);
        } else if (targetYaw != UNSET_ROTATION) {
            tYaw = targetYaw;
            tPitch = targetPitch;
        } else {
            complete = true;
            return true;
        }

        // jitter
        if (jitterEnabled) updateJitter();
        float jYaw = jitterEnabled ? calcYawJitter() : 0f;
        float jPitch = jitterEnabled ? calcPitchJitter() : 0f;
        tYaw += jYaw;
        tPitch += jPitch;

        float yawError = Mth.wrapDegrees(tYaw - currentYaw);
        float pitchError = Mth.wrapDegrees(tPitch - currentPitch);
        float absYaw = Math.abs(yawError);
        float absPitch = Math.abs(pitchError);

        float rotationPerStep = getMouseScale() * 0.15f;
        boolean yawDone = Math.round(absYaw / rotationPerStep) <= Math.max(Math.round(tolerance / rotationPerStep), 0L);
        boolean pitchDone = Math.round(absPitch / rotationPerStep) <= Math.max(Math.round(tolerance / rotationPerStep), 0L);

        if (yawDone && pitchDone) {
            complete = true;
            return true;
        }

        float step = speed * 0.25f;

        if (!yawDone) {
            float yawStep = step;
            if (scaleAxesProportionally && absPitch > 0.001f) {
                float ratio = absYaw / absPitch;
                if (ratio < 1.0f) yawStep *= ratio;
            }
            yawStep = (float) applyYawAcceleration(yawStep, absYaw);
            if (clampStepToRemaining) {
                float remaining = absYaw / rotationPerStep;
                yawStep = Math.min(yawStep, remaining);
            }
            currentYaw += Math.signum(yawError) * yawStep * rotationPerStep;
        }

        if (!pitchDone) {
            float pitchStep = step;
            if (scaleAxesProportionally && absYaw > 0.001f) {
                float ratio = absPitch / absYaw;
                if (ratio < 1.0f) pitchStep *= ratio;
            }
            pitchStep = (float) applyPitchAcceleration(pitchStep, absPitch);
            if (clampStepToRemaining) {
                float remaining = absPitch / rotationPerStep;
                pitchStep = Math.min(pitchStep, remaining);
            }
            currentPitch += Math.signum(pitchError) * pitchStep * rotationPerStep;
            currentPitch = Mth.clamp(currentPitch, -90f, 90f);
        }

        complete = false;
        return false;
    }

    // ---- helpers (ported from FixedRotationController) ---------------------

    private float getMouseScale() {
        double sens = mc.options.sensitivity().get();
        float base = (float) (sens * 0.6f + 0.2f);
        return base * base * base * 8.0f;
    }

    private double applyYawAcceleration(double step, double absError) {
        if (angleBasedAcceleration) return step * (225.0 + absError) / 180.0;
        if (linearAcceleration) return step + absError * 0.05;
        if (!cubicAcceleration) return step;
        double n = absError / 100.0;
        double s = n + 0.7;
        double m = 0.4 + 2.0 * Math.pow(s, 3.0) + Math.pow(s, 2.0);
        return step * Math.min(Math.max(1.0, m), 4.0);
    }

    private double applyPitchAcceleration(double step, double absError) {
        if (angleBasedAcceleration) return step * (135.0 + absError) / 90.0;
        if (linearAcceleration) return step + absError * 0.05;
        if (!cubicAcceleration) return step;
        double n = absError / 75.0;
        double s = n + 0.7;
        double m = 0.4 + 2.0 * Math.pow(s, 3.0) + Math.pow(s, 2.0);
        return step * Math.max(1.0, m);
    }

    private void updateJitter() {
        long now = System.currentTimeMillis();
        if (now - lastJitterUpdate > 200 + random.nextInt(300)) {
            lastJitterUpdate = now;
            yawJitterSpeed = 0.05f + random.nextFloat() * 0.15f;
            pitchJitterSpeed = 0.04f + random.nextFloat() * 0.12f;
        }
        yawJitterPhase += yawJitterSpeed;
        pitchJitterPhase += pitchJitterSpeed;
        if (yawJitterPhase > Math.PI * 2) yawJitterPhase -= Math.PI * 2;
        if (pitchJitterPhase > Math.PI * 2) pitchJitterPhase -= Math.PI * 2;
    }

    private float calcYawJitter() {
        return (float) (Math.sin(yawJitterPhase) * yawJitterAmplitude
                + Math.sin(yawJitterPhase * 2.7f + 1.3f) * yawJitterAmplitude * 0.3);
    }

    private float calcPitchJitter() {
        return (float) (Math.sin(pitchJitterPhase) * pitchJitterAmplitude
                + Math.sin(pitchJitterPhase * 3.1f + 0.7f) * pitchJitterAmplitude * 0.25);
    }
}
