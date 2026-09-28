package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;

/**
 * 平滑拖尾相机 + 第三人称环绕（移植自 来源客户端 {@code features/render/Camera}，
 * 模块名保持 来源原名 "Camera"；因 Dioxide 已有同名 render 模块，类名用 Camera）。
 *
 * <p>来源 端这份逻辑完全由渲染钩子驱动：</p>
 * <ul>
 *   <li>位置：{@code Camera.L(double,double,double)} 用 nanoTime 帧间隔做指数阻尼，
 *       单帧位移超过 8 格（传送 / 换维度）直接重置，平滑位置与目标之间最多拖尾 6 格；</li>
 *   <li>朝向：{@code Camera.L(Entity)} 按 0 目标衰减 yaw（{@code percent/100*260ms} 阻尼），
 *       pitch 视开关平滑或直接跟手；平滑结果交给 {@code RenderSupport_170}，
 *       由它在渲染期临时替换实体的 rotationYaw / rotationPitch（第三人称环绕）。</li>
 * </ul>
 *
 * <p>本端口把整套状态机 1:1 搬过来（{@link #updatePosition} / {@link #updateRotation}），
 * 并在 Render3DEvent 里按渲染帧推进一次；接入相机钩子后，钩子直接读
 * {@link #cameraX()} / {@link #cameraY()} / {@link #cameraZ()} 与
 * {@link #cameraYaw()} / {@link #cameraPitch()} 即可。</p>
 *
 * <p>设置还原度：{@code Smoothing}（95%）、{@code Distance}（50%）两项来自 来源 构造器，
 * 名称 / 范围 / 步进 / 默认值 1:1。来源的两个设置组 {@code FeatureSupport_247}（位置 + 运动倾斜）
 * 与 {@code FeatureSupport_251}（第三人称旋转平滑）的类文件不在反编译产物里，
 * 成员名与默认值无法还原，这里按调用处语义拆成下面的布尔 / 数值设置。</p>
 */
// PORT-NOTE: 需要 Camera hook（Camera#update / alignWithEntity / setPosition+setRotation 的注入点，Dioxide 现有 CameraMixin 只接了 CameraClip）以及渲染期实体旋转 override（来源 RenderSupport_170：渲染时临时替换 rotationYaw/rotationPitch/rotationYawHead）；本端口只实现了 1:1 设置面、按帧推进的指数阻尼状态机与只读访问器（cameraX/Y/Z、cameraYaw/Pitch、cameraLastYaw/Pitch、thirdPersonDistance）。
public final class Camera extends Module {

    public static final Camera INSTANCE = new Camera();

    /** 来源: NumberSetting("Smoothing", 95.0, 0.0, 95.0, 5.0)，后缀 "%"，越大拖尾越长。 */
    public final DoubleSetting smoothing = add(new DoubleSetting("Smoothing", 95.0, 0.0, 95.0, 5.0)
            .displayAs("Smoothing (%)"));

    /** 来源: NumberSetting("Distance", 50.0, 50.0, 160.0, 5.0)，后缀 "%"；{@link #thirdPersonDistance()} 返回 /100 的值。 */
    public final DoubleSetting distance = add(new DoubleSetting("Distance", 50.0, 50.0, 160.0, 5.0)
            .displayAs("Distance (%)"));

    /** 来源: FeatureSupport_247.D() —— 位置拖尾总开关（设置组名不可考，按调用处语义命名）。 */
    public final BooleanSetting smoothPosition = add(new BooleanSetting("Smooth Position", true));

    /** 来源: FeatureSupport_251.D() —— 第三人称环绕 / 旋转平滑总开关。 */
    public final BooleanSetting thirdPerson = add(new BooleanSetting("Third Person", true));

    /** 来源: FeatureSupport_251 的百分比设置，阻尼时间常数 = percent / 100.0 * 260ms。 */
    public final DoubleSetting rotationSmoothing = add(new DoubleSetting("Rotation Smoothing", 100.0, 0.0, 100.0, 5.0)
            .visibleWhen(thirdPerson::get));

    /** 来源: FeatureSupport_251.H —— 关掉时 pitch 直接跟手，只保留 yaw 的衰减平滑。 */
    public final BooleanSetting smoothRotation = add(new BooleanSetting("Smooth Rotation", true)
            .visibleWhen(thirdPerson::get));

    /** 来源: 8.0^2 —— 单帧位移超过 8 格视为传送 / 换维度，重置平滑状态。 */
    private static final double TELEPORT_RESET_DISTANCE_SQ = 64.0D;
    /** 来源: E = 6.0 —— 平滑位置与目标之间的最大拖尾半径（格）。 */
    private static final double MAX_LAG_DISTANCE = 6.0D;
    /** 来源: 第一个采样帧用的兜底帧间隔（毫秒）。 */
    private static final double DEFAULT_FRAME_MS = 16.0D;
    /** 来源: 帧间隔上限 100ms。 */
    private static final double MAX_FRAME_MS = 100.0D;
    /** 来源 旋转分支的阻尼常数形状：percent / 100.0 * 260.0（毫秒）。 */
    private static final double TAU_SCALE_MS = 260.0D;

    private double smoothedX;
    private double smoothedY;
    private double smoothedZ;
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean positionReady;
    private long positionNanos;

    private float smoothedYaw;
    private float smoothedPitch;
    private float previousYaw;
    private float previousPitch;
    private boolean rotationReady;
    private long rotationNanos;

    private Camera() {
        super("Camera", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        reset();
    }

    @Override
    protected void onDisable() {
        reset();
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            reset();
            return;
        }

        // Dioxide 没有"相机前"的帧事件：这里按渲染帧推进状态机（与来源 钩子内的
        // nanoTime 帧间隔一致）。接入相机钩子后，可把这两行整体搬进钩子。
        if (smoothPosition.get()) {
            updatePosition(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        }
        if (thirdPerson.get()) {
            updateRotation(mc.player.getYRot(), mc.player.getXRot());
        }
    }

    /**
     * 来源 {@code Camera.L(double,double,double)}：指数阻尼跟随目标位置，
     * 8 格以上的跳变直接重置，并把拖尾半径限制在 6 格以内。
     */
    public void updatePosition(double x, double y, double z) {
        long now = System.nanoTime();
        double deltaMs = positionNanos == 0L
                ? DEFAULT_FRAME_MS
                : Math.min((now - positionNanos) / 1.0E6D, MAX_FRAME_MS);
        positionNanos = now;

        double dx = x - lastX;
        double dy = y - lastY;
        double dz = z - lastZ;
        boolean teleported = dx * dx + dy * dy + dz * dz > TELEPORT_RESET_DISTANCE_SQ;
        lastX = x;
        lastY = y;
        lastZ = z;

        if (!positionReady || teleported) {
            positionReady = true;
            smoothedX = x;
            smoothedY = y;
            smoothedZ = z;
            return;
        }

        double factor = damping(deltaMs, lagTauMs());
        smoothedX += (x - smoothedX) * factor;
        smoothedY += (y - smoothedY) * factor;
        smoothedZ += (z - smoothedZ) * factor;

        double lagX = x - smoothedX;
        double lagY = y - smoothedY;
        double lagZ = z - smoothedZ;
        double lagSq = lagX * lagX + lagY * lagY + lagZ * lagZ;
        if (lagSq > MAX_LAG_DISTANCE * MAX_LAG_DISTANCE) {
            double scale = MAX_LAG_DISTANCE / Math.sqrt(lagSq);
            smoothedX = x - lagX * scale;
            smoothedY = y - lagY * scale;
            smoothedZ = z - lagZ * scale;
        }
    }

    /**
     * 相机朝向跟随玩家视角：角速度阻尼，把玩家的 yaw / pitch 平滑成"跟手但带拖尾"的
     * 相机角度（记录上一帧值供插值使用）。pitch 按 "Smooth Rotation" 平滑或直接跟手。
     * <p>
     * 注意：这里返回的是**绝对**角度（相机直接使用），不是相对 0 的偏移量 —— 早先按
     * 来源的"偏移量向 0 衰减"写法实现，接到 {@code Camera#setRotation} 上会把相机锁死在
     * yaw=0，表现就是"开了 Camera 之后不能移动视角"。</p>
     */
    public void updateRotation(float yaw, float pitch) {
        long now = System.nanoTime();
        double deltaMs = rotationNanos == 0L
                ? DEFAULT_FRAME_MS
                : Math.min((now - rotationNanos) / 1.0E6D, MAX_FRAME_MS);
        rotationNanos = now;

        float target = wrapDegrees(yaw);
        if (!rotationReady) {
            rotationReady = true;
            smoothedYaw = target;
            previousYaw = target;
            smoothedPitch = pitch;
            previousPitch = pitch;
            return;
        }

        previousYaw = smoothedYaw;
        previousPitch = smoothedPitch;

        float factor = (float) damping(deltaMs, rotationSmoothing.get() / 100.0D * TAU_SCALE_MS);
        smoothedYaw = wrapDegrees(smoothedYaw + wrapDegrees(target - smoothedYaw) * factor);
        if (smoothRotation.get()) {
            smoothedPitch += (pitch - smoothedPitch) * factor;
        } else {
            smoothedPitch = pitch;
        }
    }

    /** 平滑后的相机 X（世界坐标）。 */
    public double cameraX() {
        return smoothedX;
    }

    /** 平滑后的相机 Y（世界坐标）。 */
    public double cameraY() {
        return smoothedY;
    }

    /** 平滑后的相机 Z（世界坐标）。 */
    public double cameraZ() {
        return smoothedZ;
    }

    /** 平滑后的相机 yaw（绝对角度，直接交给相机钩子）。 */
    public float cameraYaw() {
        return smoothedYaw;
    }

    /** 平滑后的 pitch（对应 来源的 RenderSupport_170 的当前 pitch）。 */
    public float cameraPitch() {
        return smoothedPitch;
    }

    /** 上一帧的相机 yaw（供插值/调试）。 */
    public float cameraLastYaw() {
        return previousYaw;
    }

    /** 上一帧的 pitch（RenderSupport_170 的 prevPitch）。 */
    public float cameraLastPitch() {
        return previousPitch;
    }

    /** 来源 {@code Camera.i()}：Distance 百分比换算出的第三人称距离倍率。 */
    public double thirdPersonDistance() {
        return distance.get() / 100.0D;
    }

    /** 位置平滑状态是否已初始化。 */
    public boolean isPositionReady() {
        return positionReady;
    }

    /** 旋转平滑状态是否已初始化。 */
    public boolean isRotationReady() {
        return rotationReady;
    }

    private double lagTauMs() {
        return smoothing.get() / 100.0D * TAU_SCALE_MS;
    }

    /** 指数阻尼系数：{@code 1 - exp(-dt / tau)}；tau 或 dt 为 0 时直接返回 1（不平滑）。 */
    private static double damping(double deltaMs, double tauMs) {
        if (deltaMs <= 0.0D || tauMs <= 0.0D) {
            return 1.0D;
        }
        return 1.0D - Math.exp(-deltaMs / tauMs);
    }

    /** 来源的角度归一化（静态方法 {@code Camera.L(float)}）：折到 [-180, 180)。 */
    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    private void reset() {
        positionReady = false;
        rotationReady = false;
        positionNanos = 0L;
        rotationNanos = 0L;
        smoothedX = 0.0D;
        smoothedY = 0.0D;
        smoothedZ = 0.0D;
        lastX = 0.0D;
        lastY = 0.0D;
        lastZ = 0.0D;
        smoothedYaw = 0.0F;
        smoothedPitch = 0.0F;
        previousYaw = 0.0F;
        previousPitch = 0.0F;
    }
}
