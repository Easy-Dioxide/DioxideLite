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

    /**
     * 没有任何模块再请求旋转后，多久自动释放（纳秒）。
     * <p>
     * <b>这是"关闭模块后还会转头"的修复点。</b>以前 {@link #active} 只能靠
     * {@link #onSendPosition} 里"旋转已经追上玩家视角"这个条件退出，而静默旋转的目标通常
     * 和玩家视角差着几十度，那个条件永远不会成立 —— 于是模块关掉之后
     * {@code active} 一直是 true，{@link #smooth()} 继续跑、{@code SendPositionEvent} 继续改包、
     * {@code LivingEntityMixin} 继续把头/身强行拧到那个过期的目标上。
     * 现在只要连续 250ms（5 tick）没有任何模块请求，就自动释放。
     */
    private static final long IDLE_RELEASE_NANOS = 250_000_000L;

    /**
     * yaw 目标在左右之间反复横跳时的步长折扣（典型的搭路"摇头"）。
     * 检测到本 tick 的误差方向和上一 tick 相反时，把步长压到 35%，
     * 让头部平缓地停在中间，而不是被甩来甩去。
     */
    private static final float REVERSAL_DAMPING = 0.35F;

    private final Rot2f offset = new Rot2f(0, 0);
    public Rot2f rotations = new Rot2f(0, 0);
    public Rot2f lastRotations = new Rot2f(0, 0);
    /** The angle we are continuously smoothing toward; only changed by setRotations / s08-apply. */
    public Rot2f targetRotations;
    public Rot2f animationRotation;
    public Rot2f lastAnimationRotation;

    private boolean active;
    private double rotationSpeed;
    private Function<Rot2f, Boolean> raytrace;
    /** Bounded random angle used only while a raytrace offset is active. */
    private float randomAngle;
    /** True when a server correction packet arrived; the next tick will apply it. */
    private boolean s08;
    private boolean renderAnimation = true;

    private int priority;
    private Runnable callback;
    private Object transientOwner;

    /** 最近一次收到旋转请求的时间戳；用于 {@link #IDLE_RELEASE_NANOS} 的自动释放。 */
    private long lastRequestNanos;
    /** 反抖状态：上一次统计误差方向时的 tick，保证同一 tick 内重复调用 smooth() 结果一致。 */
    private int lastDampTick = -1;
    /** 上一次的 yaw 误差方向（-1 / 0 / 1）。 */
    private float lastYawErrorSign;
    /** 本 tick 是否命中"目标反向"从而需要压低步长。 */
    private boolean dampYawThisTick;

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

    /** 记录一次请求：只要还有模块在请求，旋转就不会被自动释放。 */
    private void markRequest() {
        this.lastRequestNanos = System.nanoTime();
    }

    /** Applies a rotation model's already-stepped result without smoothing it a second time. */
    public void setRotationsDirect(Rot2f rotations, Priority priority) {
        if (rotations == null || mc.player == null) return;
        if (this.active && priority.priority < this.priority) return;

        markRequest();

        if (s08) {
            // Server just corrected our position; align current view but keep target so
            // subsequent smoother calls don't snap from a stale baseline.
            float curYaw = mc.player.getYRot();
            float curPitch = mc.player.getXRot();
            this.rotations = new Rot2f(curYaw, curPitch);
            this.lastRotations = new Rot2f(curYaw, curPitch);
            s08 = false;
        }

        this.rotations = rotations;
        this.targetRotations = rotations.copy();
        this.raytrace = null;
        this.priority = priority.priority;
        this.callback = null;
        this.transientOwner = null;
        this.renderAnimation = true;
        this.active = true;

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

        markRequest();

        if (s08) {
            float curYaw = mc.player.getYRot();
            float curPitch = mc.player.getXRot();
            this.rotations = new Rot2f(curYaw, curPitch);
            this.lastRotations = new Rot2f(curYaw, curPitch);
            this.targetRotations = new Rot2f(curYaw, curPitch);
            this.callback = null;
            this.transientOwner = null;
            s08 = false;
            return false;
        }

        this.rotations = rotations.copy();
        this.targetRotations = rotations.copy();
        this.raytrace = null;
        this.priority = priority.priority;
        this.callback = null;
        this.transientOwner = owner;
        this.renderAnimation = false;
        this.active = true;
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
        this.renderAnimation = true;
        this.lastRequestNanos = 0L;
        this.lastDampTick = -1;
        this.lastYawErrorSign = 0.0F;
        this.dampYawThisTick = false;
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

        markRequest();

        // If a server correction just arrived, align current view to its position
        // before applying our new target, so we don't snap from a stale baseline.
        if (s08) {
            float curYaw = mc.player.getYRot();
            float curPitch = mc.player.getXRot();
            this.rotations = new Rot2f(curYaw, curPitch);
            this.lastRotations = new Rot2f(curYaw, curPitch);
            this.targetRotations = new Rot2f(curYaw, curPitch);
            this.randomAngle = 0;
            s08 = false;
        }

        this.targetRotations = rotations.copy();
        this.rotationSpeed = rotationSpeed * 18.0;
        this.raytrace = raytrace;
        this.priority = priority.priority;
        this.callback = callback;
        this.transientOwner = null;
        this.renderAnimation = renderAnimation;
        this.active = true;

        smooth();
    }

    /**
     * Step the rotation one tick toward {@link #targetRotations}.
     * Must be called every active tick so the result stays in sync with both
     * {@link #lastRotations} (updated in onSendPosition) and {@link #rotations}
     * (read by other modules).
     */
    private void smooth() {
        if (mc.player == null) {
            resetState();
            return;
        }

        // Apply any pending server correction first.
        if (s08) {
            float curYaw = mc.player.getYRot();
            float curPitch = mc.player.getXRot();
            this.rotations = new Rot2f(curYaw, curPitch);
            this.lastRotations = new Rot2f(curYaw, curPitch);
            if (this.targetRotations == null) {
                this.targetRotations = new Rot2f(curYaw, curPitch);
            }
            this.randomAngle = 0;
            s08 = false;
        }

        float targetYaw = targetRotations.getYaw();
        float targetPitch = targetRotations.getPitch();

        // Raytrace offset: only computed once per setRotations call (when raytrace is non-null).
        // Bounded randomAngle prevents long-term drift from unbounded accumulation.
        if (raytrace != null && (Math.abs(targetYaw - rotations.getYaw()) > 5 || Math.abs(targetPitch - rotations.getPitch()) > 5)) {
            final Rot2f trueTarget = new Rot2f(targetRotations.getYaw(), targetRotations.getPitch());
            double speed = (Math.random() * Math.random() * Math.random()) * 20;

            // Clamp randomAngle to [-360, 360] to prevent floating-point precision loss over time
            randomAngle = ((randomAngle % 720f) + 720f) % 720f - 360f;
            randomAngle += (float) ((20 + (float) (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360))
                    * (mc.player.tickCount / 10 % 2 == 0 ? -1 : 1));
            randomAngle = ((randomAngle % 720f) + 720f) % 720f - 360f;

            offset.set(
                    (float) (offset.getYaw() + -Mth.sin((float) Math.toRadians(randomAngle)) * speed),
                    (float) (offset.getPitch() + Mth.cos((float) Math.toRadians(randomAngle)) * speed)
            );
            targetYaw += offset.getYaw();
            targetPitch += offset.getPitch();

            if (!raytrace.apply(new Rot2f(targetYaw, targetPitch))) {
                randomAngle = (float) Math.toDegrees(Math.atan2(trueTarget.getYaw() - targetYaw, targetPitch - trueTarget.getPitch())) - 180;
                targetYaw -= offset.getYaw();
                targetPitch -= offset.getPitch();
                offset.set(
                        (float) (offset.getYaw() + -Mth.sin((float) Math.toRadians(randomAngle)) * speed),
                        (float) (offset.getPitch() + Mth.cos((float) Math.toRadians(randomAngle)) * speed)
                );
                targetYaw += offset.getYaw();
                targetPitch += offset.getPitch();
            }

            if (!raytrace.apply(new Rot2f(targetYaw, targetPitch))) {
                offset.set(0, 0);
                targetYaw = (float) (trueTarget.getYaw() + Math.random() * 2);
                targetPitch = (float) (trueTarget.getPitch() + Math.random() * 2);
            }
        } else {
            // Raytrace done or not needed — reset offset so it doesn't bleed into next call.
            offset.set(0, 0);
        }

        rotations = vapeSmooth(targetYaw, targetPitch);

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

        // [反抖] 目标在左右之间反复横跳时压低本 tick 步长。
        // 搭路时目标 yaw 常在相邻档位之间来回切，若每 tick 都全力追，
        // 头/身体就会被甩成"摇头"。这里只要发现误差方向与上一 tick 相反，
        // 本 tick 就只走 REVERSAL_DAMPING，让头部停在中间平缓摆动。
        // 用 tickCount 做闸门：smooth() 一个 tick 内可能被调用两次
        // （setRotations 里一次 + onPlayerTick 里一次），必须保证两次结果一致。
        int tick = mc.player == null ? -1 : mc.player.tickCount;
        if (tick != lastDampTick) {
            lastDampTick = tick;
            float sign = (float) Math.signum(yawError);
            dampYawThisTick = sign != 0.0F && lastYawErrorSign != 0.0F && sign != lastYawErrorSign;
            if (sign != 0.0F) {
                lastYawErrorSign = sign;
            }
        }
        float damping = dampYawThisTick ? REVERSAL_DAMPING : 1.0F;

        // yaw 步进 + angle-based 加速度
        if (absYaw > tolerance) {
            float yawStep = step * damping;
            // 比例缩放：当 pitch 误差更大时，yaw 步进按比例减小
            if (absPitch > 0.001f) {
                float ratio = absYaw / absPitch;
                if (ratio < 1.0f) yawStep *= ratio;
            }
            // angle-based acceleration：剩余角度越大，步进越大。
            // 原公式 base 是 225/180 = 1.25，等于"已经贴近目标"时反而把步长放大 25%，
            // 目标稍有晃动就会过冲 → 来回抖。改成从 1.0 起步，
            // 小误差不再被放大，而大幅度甩枪的速度几乎不变。
            double accel = 1.0 + Math.min(absYaw, 180.0F) / 180.0 * 0.8;
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
            double accel = 1.0 + Math.min(absPitch, 90.0F) / 90.0 * 0.8;
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
        if (active) {
            markRequest();
            this.active = true;
            return;
        }
        stop();
    }

    /**
     * 立即释放静默旋转。
     * <p>
     * <b>只清内部状态，绝不碰玩家自己的视角</b> —— 所以调用它（或等到空闲自动释放）之后，
     * 头部/身体会立刻回到玩家自己的朝向，不会再"关闭模块还转头"。
     *
     * <p>与 {@link #resetState()} 的区别：{@code resetState()} 会把 rotations 归零（用于换世界 / 重生），
     * 这里则把基准对齐到玩家当前视角，方便下一次请求从当前视角平滑起步。
     */
    public void stop() {
        boolean wasActive = active;

        active = false;
        priority = 0;
        callback = null;
        transientOwner = null;
        raytrace = null;
        renderAnimation = true;
        offset.set(0, 0);
        randomAngle = 0;
        s08 = false;
        lastRequestNanos = 0L;
        lastDampTick = -1;
        lastYawErrorSign = 0.0F;
        dampYawThisTick = false;

        if (mc.player == null) {
            return;
        }

        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        targetRotations = new Rot2f(yaw, pitch);
        if (wasActive) {
            rotations = new Rot2f(yaw, pitch);
            lastRotations = new Rot2f(yaw, pitch);
        }
    }

    /** Whether silent rotations are intentionally mirrored to the third-person model. */
    public boolean isRenderAnimationEnabled() {
        return renderAnimation;
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
        raytrace = null;
        randomAngle = 0;
        s08 = false;
        renderAnimation = true;
        lastRequestNanos = 0L;
        lastDampTick = -1;
        lastYawErrorSign = 0.0F;
        dampYawThisTick = false;
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

        // Initialise stale nulls without overwriting an active target.
        if (rotations == null) rotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());
        if (lastRotations == null) lastRotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());
        if (targetRotations == null) targetRotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());

        if (active) {
            // 没有任何模块再请求旋转（例如模块刚被关闭）→ 立刻释放，
            // 否则 smooth() / 发包 / 头身同步会一直沿用过期目标，表现为"关了模块还在转头"。
            if (lastRequestNanos != 0L && System.nanoTime() - lastRequestNanos > IDLE_RELEASE_NANOS) {
                stop();
                return;
            }

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
                // 静默旋转已经追平玩家自己的视角：本次使命完成，直接释放，
                // 停止继续改包 / 同步头身（走 stop() 以保证状态清理一致）。
                stop();
                this.correctDisabledRotations();
            }

            lastRotations = rotations;
        } else {
            lastRotations = new Rot2f(mc.player.getYRot(), mc.player.getXRot());
        }

        lastAnimationRotation = animationRotation;
        animationRotation = new Rot2f(event.getYaw(), event.getPitch());
        // Preserve targetRotations — do NOT reset it here; let setRotations or
        // the tick null-guard decide what to aim for.
        raytrace = null;
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
