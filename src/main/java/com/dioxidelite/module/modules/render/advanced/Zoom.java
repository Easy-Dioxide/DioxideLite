package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.KeybindSetting;
import com.dioxidelite.util.client.KeybindUtils;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * 平滑按住缩放（移植自 来源客户端 {@code features/render/Zoom}，说明 "Smooth hold-to-zoom camera"）。
 *
 * <p>来源 逻辑：每帧查询一次 FOV，把"是否按住 Zoom Key（默认 V）且没有打开界面"作为目标值 0/1，
 * 用 Animation(ms) 做缓动（<code>1 - (1 - t)^3</code>）推进内部进度 <code>i</code>，
 * 最后 <code>fov = baseFov + (baseFov / Distance - baseFov) * smoothstep(i)</code>。
 * <code>zoomFactor()</code> / <code>isZooming()</code>（来源的 L()/I()，供 FogBlur 的 Disable-on-zoom 查询）同样保留。</p>
 *
 * <p>动画驱动：本端口在 {@link Render3DEvent} 里按帧推进状态机；接入 FOV hook 后由 hook 在取 FOV 前调用
 * {@link #applyFov(float)}（该调用自身也会推进状态机，重复调用只按真实 dt 分摊，不影响结果）。</p>
 */
// PORT-NOTE: 需要 FOV hook（GameRenderer/Camera 的视场角计算注入，来源 由 FOV mixin 调用 Zoom.d(baseFov)）；本端口只实现了按住缩放的状态机与动画，并暴露 applyFov(baseFov)/zoomFactor()/isZooming()，FOV 本身未改动。
public final class Zoom extends Module {

    public static final Zoom INSTANCE = new Zoom();

    /** 来源: KeybindSetting "Zoom Key"，默认 47（LWJGL2 的 KEY_V），GLFW 下 V = 86。 */
    public final KeybindSetting zoomKey = add(new KeybindSetting("Zoom Key", GLFW.GLFW_KEY_V));

    /** 来源: Distance 4.0 (1.5..12.0, 0.5, "x")。 */
    public final DoubleSetting distance = add(new DoubleSetting("Distance", 4.0, 1.5, 12.0, 0.5));

    /** 来源: Animation 180.0 (0.0..500.0, 10.0, " ms")。 */
    public final DoubleSetting animation = add(new DoubleSetting("Animation", 180.0, 0.0, 500.0, 10.0));

    /** 来源的 I()：进度超过 0.02 视为正在缩放。 */
    private static final float MIN_ZOOM_PROGRESS = 0.02F;

    /** 来源的收尾阈值：与目标差小于 0.001 时直接吸附。 */
    private static final float SNAP_EPSILON = 0.001F;

    /** 来源: 单帧 dt 上限 100ms。 */
    private static final float MAX_DELTA_MS = 100.0F;

    /** 来源 成员 i：当前缩放进度 0..1。 */
    private float progress;

    /** 来源 成员 e：上一帧的 nanoTime（0 表示本帧是收帧）。 */
    private long lastFrameNanos;

    private Zoom() {
        super("Zoom", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        reset();
    }

    @Override
    protected void onDisable() {
        // 来源的 D() 只重置时间戳；这里额外把进度清零，重新启用时从 1x 开始。
        reset();
    }

    private void reset() {
        progress = 0.0F;
        lastFrameNanos = 0L;
    }

    /** 没有 FOV hook 时，用每帧事件驱动动画（有 hook 时 hook 调 applyFov() 即可）。 */
    @Listen
    private void onRender3D(Render3DEvent event) {
        update();
    }

    /** 来源的 d(float) 前半段：按 dt 把进度平滑推向"按住 = 1 / 松开 = 0"。 */
    private void update() {
        long now = System.nanoTime();
        float deltaMs = lastFrameNanos == 0L
                ? 0.0F
                : Math.min((now - lastFrameNanos) / 1_000_000.0F, MAX_DELTA_MS);
        lastFrameNanos = now;

        // 来源: enabled && 按键按下 && currentScreen == null。
        float target = isZoomKeyDown() ? 1.0F : 0.0F;
        float animationMs = animation.get().floatValue();
        if (animationMs <= 0.0F) {
            progress = target;
            return;
        }
        if (deltaMs <= 0.0F) {
            return;
        }
        // 来源: t = clamp(dt / animation, 0, 1)，再取 1 - (1 - t)^3 作为本帧步进比例。
        float step = Mth.clamp(deltaMs / animationMs, 0.0F, 1.0F);
        step = 1.0F - (float) Math.pow(1.0F - step, 3.0D);
        progress += (target - progress) * step;
        if (Math.abs(target - progress) < SNAP_EPSILON) {
            progress = target;
        }
    }

    private boolean isZoomKeyDown() {
        return mc.screen == null && KeybindUtils.isPressed(zoomKey.get());
    }

    /**
     * 来源的 I()：是否正在缩放（供外部的 zoom 联动查询，例如 FogBlur 的 Disable on zoom）。
     */
    public boolean isZooming() {
        return isEnabled() && progress > MIN_ZOOM_PROGRESS;
    }

    /**
     * 来源的 L()：当前缩放进度 0..1；未启用时为 0。
     */
    public float zoomFactor() {
        return isEnabled() ? progress : 0.0F;
    }

    /**
     * 把基础 FOV 换算成缩放后的 FOV（来源 由 FOV mixin 调用 <code>d(baseFov)</code>）。
     *
     * @param baseFov 原版当帧 FOV（含疾跑/药水等修正后的值）
     * @return 缩放后的 FOV；未启用时原样返回
     */
    public float applyFov(float baseFov) {
        if (!isEnabled()) {
            return baseFov;
        }
        update();
        float smooth = smoothstep(progress);
        float zoomedFov = baseFov / distance.get().floatValue();
        return baseFov + (zoomedFov - baseFov) * smooth;
    }

    /** 来源的私有 static L(float)：smoothstep(t) = t * t * (3 - 2t)。 */
    private static float smoothstep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
