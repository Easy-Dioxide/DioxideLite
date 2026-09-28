package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.event.events.RespawnEvent;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;

/**
 * 自由视角（移植自 来源客户端 {@code features/render/Freelook}，说明 "Look around without turning your player"）。
 *
 * <p>来源 逻辑：模块按住期间把原版第三人称视角当作"分离相机"——首次激活时记下玩家 yaw/pitch
 * 作为相机初始朝向，保存并改写 <code>gameSettings.thirdPersonView</code>（Perspective 决定 1/2），
 * 鼠标增量由鼠标钩子喂给 <code>L(dx, dy)</code>：<code>scale = 0.15 * Sensitivity/100</code>，
 * yaw += dx * scale，pitch = clamp(pitch - dy * scale, -90, 90)；渲染时通过
 * <code>RenderSupport_170</code> 用分离角度临时改写实体 rotationYaw/Pitch 并做上一帧插值，
 * 关闭或换世界时恢复原第三人称模式。</p>
 *
 * <p>本端口把可做的部分都做了：按 Perspective 切换相机模式（{@code mc.options.setCameraType}）、
 * 记录分离 yaw/pitch 与上一帧值、换世界时恢复；鼠标增量与相机角度注入留待 hook 调用
 * {@link #handleMouseMove(double, double)} 与 {@link #cameraYaw()}/{@link #cameraPitch()}。</p>
 */
// PORT-NOTE: 需要相机 + 鼠标 hook（Camera#setRotation 或相机角度注入，以及 MouseHandler 鼠标增量拦截；来源 用 RenderSupport_170 临时改写实体 rotationYaw/Pitch 做分离相机，并把鼠标增量直接喂给 Freelook.L(dx,dy)）；本端口只实现了状态机、Perspective 相机模式切换、yaw/pitch 记录与插值取样，暴露 handleMouseMove(dx,dy)/cameraYaw()/cameraPitch()/previousYaw()/previousPitch() 供 hook 驱动。
public final class Freelook extends Module {

    public static final Freelook INSTANCE = new Freelook();

    /** 来源的 FeatureMode_268：分离相机朝哪一边（对应原版第三人称 1/2）。 */
    public enum Perspective {
        BEHIND(CameraType.THIRD_PERSON_BACK),
        FRONT(CameraType.THIRD_PERSON_FRONT);

        private final CameraType cameraType;

        Perspective(CameraType cameraType) {
            this.cameraType = cameraType;
        }

        public CameraType cameraType() {
            return cameraType;
        }
    }

    /** 来源: Sensitivity 100.0 (25.0..200.0, 5.0, "%")。 */
    public final DoubleSetting sensitivity = add(new DoubleSetting("Sensitivity", 100.0, 25.0, 200.0, 5.0));

    /** 来源: EnumSetting "Perspective"，默认取枚举默认常量（第三人称背后）。 */
    public final EnumSetting<Perspective> perspective = add(new EnumSetting<>("Perspective", Perspective.BEHIND));

    /** 来源的鼠标灵敏度基数：0.15 * Sensitivity / 100。 */
    private static final double BASE_SENSITIVITY = 0.15D;

    /** 来源的俯仰钳制范围。 */
    private static final float PITCH_LIMIT = 90.0F;

    /** 来源 成员 e：分离相机当前 yaw。 */
    private float yaw;

    /** 来源 成员 h：分离相机当前 pitch。 */
    private float pitch;

    /** 来源 成员 I/m：上一帧的分离角度（供相机插值）。 */
    private float prevYaw;
    private float prevPitch;

    /** 来源 成员 l：分离相机是否已激活。 */
    private boolean active;

    /** 来源 成员 i：激活前的 <code>thirdPersonView</code>（26.1.2 里为 CameraType）。 */
    private CameraType savedCameraType = CameraType.FIRST_PERSON;

    private Freelook() {
        super("Freelook", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        // 来源的 L()：启用时只清激活标记，真正的分离相机在第一次使用时初始化。
        active = false;
    }

    @Override
    protected void onDisable() {
        deactivate();
    }

    /** 每 tick 保证分离相机已初始化（来源 是在每帧渲染和鼠标回调里惰性初始化）。 */
    @Listen
    private void onTick(TickEvent.Pre event) {
        activateIfNeeded();
    }

    /** 来源的 WorldChangeEvent 处理：换世界 / 重生时恢复原第三人称模式，下一帧重新初始化。 */
    @Listen
    private void onRespawn(RespawnEvent event) {
        deactivate();
    }

    /** 来源的 EventSupport_619 处理：每帧把当前角度存成"上一帧角度"，供相机 hook 插值。 */
    @Listen
    private void onRender3D(Render3DEvent event) {
        beginFrame();
    }

    /**
     * 每帧开始调用：把当前角度记为上一帧角度（来源的 RenderSupport_170 插值输入）。
     * 相机 hook 在注入角度前调用它即可获得与来源 相同的插值数据。
     */
    public void beginFrame() {
        prevYaw = yaw;
        prevPitch = pitch;
    }

    /**
     * 鼠标增量入口（需要 MouseHandler hook 调用；来源 里由鼠标回调直接调用 <code>L(dx, dy)</code>）。
     *
     * @return true 表示该增量已被自由视角消费、不应再转动玩家
     */
    public boolean handleMouseMove(double deltaX, double deltaY) {
        if (!isEnabled() || mc.player == null) {
            return false;
        }
        activateIfNeeded();
        double scale = BASE_SENSITIVITY * (sensitivity.get() / 100.0D);
        yaw += (float) (deltaX * scale);
        pitch = Mth.clamp(pitch - (float) (deltaY * scale), -PITCH_LIMIT, PITCH_LIMIT);
        return true;
    }

    /** 分离相机当前是否生效。 */
    public boolean isActive() {
        return isEnabled() && active;
    }

    /** 分离相机 yaw（hook 注入用）。 */
    public float cameraYaw() {
        return yaw;
    }

    /** 分离相机 pitch（hook 注入用）。 */
    public float cameraPitch() {
        return pitch;
    }

    /** 上一帧的分离 yaw（hook 插值用）。 */
    public float previousYaw() {
        return prevYaw;
    }

    /** 上一帧的分离 pitch（hook 插值用）。 */
    public float previousPitch() {
        return prevPitch;
    }

    /** 来源的 h()：惰性激活——记录玩家视角、保存并切换相机模式。 */
    private void activateIfNeeded() {
        if (active || mc.player == null || mc.options == null) {
            return;
        }
        yaw = prevYaw = mc.player.getYRot();
        pitch = prevPitch = mc.player.getXRot();
        savedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(perspective.get().cameraType());
        active = true;
    }

    /** 来源的 F()：恢复激活前的相机模式。 */
    private void deactivate() {
        if (!active) {
            return;
        }
        active = false;
        if (mc.options != null) {
            mc.options.setCameraType(savedCameraType);
        }
    }
}
