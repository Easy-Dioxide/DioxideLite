package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PacketEvent;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 移植自 来源客户端 {@code features/render/Ambience.java}（"Cinematic world lighting and color grading"）。
 * <p>
 * 1:1 对应的设置：
 * <ul>
 *   <li>Time lock（false）/ Time（6000, 0..24000, 100，" ticks"，Time lock 的子项）——客户端时间锁；</li>
 *   <li>Light Color 组（来源 {@code FeatureSupport_306}，默认关闭）：Color（0xFFFFE5C2）、Strength（18, 0..100, 1, "%"）；</li>
 *   <li>Color Grading 组（来源 {@code FeatureSupport_265}，默认关闭）：Saturation（100, 0..180, 5）、
 *       Contrast（100, 60..160, 5）、Vignette（10, 0..80, 5）、Film Grain（0, 0..30, 1），全部是百分比。</li>
 * </ul>
 * 来源的时间锁每 tick 调用 {@code World#setWorldTime}。26.1.2 的世界时间来自
 * {@code ClientClockManager}（时钟状态由 {@code ClientboundSetTimePacket} 同步），因此这里用等价做法：
 * 每 tick 把主世界时钟重新锚定到（服务器日数 + 目标时刻），关闭时把跟踪到的自然时钟写回。
 * Light Color / Color Grading 在 来源 里由 {@code custom_ambience} 后处理着色器完成。
 */
// PORT-NOTE: 需要 GameRenderer/后处理 hook（来源 RenderSupport_167 的 custom_ambience 全屏着色器 +
// uLightColorAndStrength / uGradeParams / uTime uniform，并在 RenderSupport_109 里做主题色混合）；
// 本端口只实现了可做的部分——Time lock（tick 里用 ClientClockManager.handleUpdates 重写主世界时钟）
// 以及 Light Color / Color Grading 的设置面、百分比归一化 getter 与 hasColorEffect() 状态查询。
public final class Ambience extends Module {

    public static final Ambience INSTANCE = new Ambience();

    /** 一个游戏日的 tick 数（与 Level 的日晷周期一致）。 */
    private static final long DAY_TICKS = 24000L;

    // --- Time lock ------------------------------------------------------------

    public final BooleanSetting timeLock = add(new BooleanSetting("Time lock", false));
    public final DoubleSetting time = add(new DoubleSetting("Time", 6000.0, 0.0, 24000.0, 100.0)
            .visibleWhen(timeLock::get));

    // --- Light Color（来源 FeatureSupport_306）--------------------------------

    public final BooleanSetting lightColor = add(new BooleanSetting("Light Color", false));
    public final ColorSetting lightColorValue = add(new ColorSetting("Color", new Color(0xFFFFE5C2, true))
            .visibleWhen(lightColor::get));
    public final DoubleSetting lightStrength = add(new DoubleSetting("Strength", 18.0, 0.0, 100.0, 1.0)
            .visibleWhen(lightColor::get));

    // --- Color Grading（来源 FeatureSupport_265）------------------------------

    public final BooleanSetting colorGrading = add(new BooleanSetting("Color Grading", false));
    public final DoubleSetting saturation = add(new DoubleSetting("Saturation", 100.0, 0.0, 180.0, 5.0)
            .visibleWhen(colorGrading::get));
    public final DoubleSetting contrast = add(new DoubleSetting("Contrast", 100.0, 60.0, 160.0, 5.0)
            .visibleWhen(colorGrading::get));
    public final DoubleSetting vignette = add(new DoubleSetting("Vignette", 10.0, 0.0, 80.0, 5.0)
            .visibleWhen(colorGrading::get));
    public final DoubleSetting filmGrain = add(new DoubleSetting("Film Grain", 0.0, 0.0, 30.0, 1.0)
            .visibleWhen(colorGrading::get));

    /**
     * 服务器完整时钟同步包（仅 {@code ClientboundSetTimePacket} 携带时钟状态的场合）。
     * 包事件在网络线程派发，这里只暂存，真正的时钟操作统一在主线程的 tick 里做。
     */
    private final AtomicReference<ClientboundSetTimePacket> pendingSync = new AtomicReference<>();

    /** 未被时间锁改写时的客户端时钟估计值（tick，含小数），用于锁定锚定与解锁还原。 */
    private double naturalTicks;
    /** 服务器下发的时钟速率（tick / game tick），默认 1。 */
    private float naturalRate = 1.0F;
    /** naturalTicks 对应的 gameTime，用于按 tick 差推进估计值。 */
    private long naturalGameTime = Long.MIN_VALUE;
    private boolean tracking;
    /** 当前是否已经改写过客户端时钟（需要还原）。 */
    private boolean clockOverridden;

    private Ambience() {
        super("Ambience", Category.RENDER);
    }

    /** 来源的 {@code I()}：Light Color 或 Color Grading 打开时后处理着色器才需要运行。 */
    public boolean hasColorEffect() {
        return isEnabled() && (lightColor.get() || colorGrading.get());
    }

    /** RenderSupport_167 的 {@code uLightColorAndStrength}：颜色 + Strength/100。 */
    public int lightColorArgb() {
        return lightColorValue.argb();
    }

    public float lightStrengthFraction() {
        return lightStrength.get().floatValue() / 100.0F;
    }

    /** RenderSupport_167 的 {@code uGradeParams}：(Saturation, Contrast, Vignette, Film Grain) / 100。 */
    public float saturationFraction() {
        return saturation.get().floatValue() / 100.0F;
    }

    public float contrastFraction() {
        return contrast.get().floatValue() / 100.0F;
    }

    public float vignetteFraction() {
        return vignette.get().floatValue() / 100.0F;
    }

    public float filmGrainFraction() {
        return filmGrain.get().floatValue() / 100.0F;
    }

    @Override
    protected void onEnable() {
        // 重新以当前客户端时钟为基准开始跟踪。
        tracking = false;
        clockOverridden = false;
        naturalGameTime = Long.MIN_VALUE;
        pendingSync.set(null);
    }

    @Override
    protected void onDisable() {
        restoreNaturalClock();
        tracking = false;
        pendingSync.set(null);
    }

    @Listen
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.getPacket() instanceof ClientboundSetTimePacket packet)) {
            return;
        }
        if (packet.clockUpdates().isEmpty()) {
            // 20 tick 一次的周期同步不带时钟状态，没有可用的时间基准。
            return;
        }
        pendingSync.set(packet);
    }

    @Listen
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) {
            return;
        }
        Holder<WorldClock> overworld = overworldClock();
        if (overworld == null) {
            return;
        }

        long gameTime = mc.level.getGameTime();
        if (!tracking) {
            tracking = true;
            naturalTicks = mc.level.getOverworldClockTime();
            naturalGameTime = gameTime;
        } else if (gameTime > naturalGameTime) {
            naturalTicks += (gameTime - naturalGameTime) * (double) naturalRate;
            naturalGameTime = gameTime;
        }

        // 服务器完整同步优先：/time set、时钟速率变化、重生、切维度时用它重新对齐估计值。
        ClientboundSetTimePacket sync = pendingSync.getAndSet(null);
        if (sync != null) {
            ClockNetworkState serverState = sync.clockUpdates().get(overworld);
            if (serverState != null) {
                naturalTicks = serverState.totalTicks();
                naturalRate = serverState.rate();
                naturalGameTime = sync.gameTime();
            }
        }

        if (timeLock.get()) {
            long base = (long) naturalTicks;
            long locked = base - Math.floorMod(base, DAY_TICKS)
                    + Math.floorMod(time.get().longValue(), DAY_TICKS);
            mc.level.clockManager().handleUpdates(gameTime, Map.of(overworld,
                    new ClockNetworkState(locked, 0.0F, naturalRate)));
            clockOverridden = true;
        } else if (clockOverridden) {
            restoreNaturalClock();
        }
    }

    /** 把客户端时钟交还给服务器时间轴（解锁、关闭模块时调用）。 */
    private void restoreNaturalClock() {
        if (!clockOverridden) {
            return;
        }
        clockOverridden = false;
        if (mc.level == null) {
            return;
        }
        Holder<WorldClock> overworld = overworldClock();
        if (overworld == null) {
            return;
        }
        mc.level.clockManager().handleUpdates(mc.level.getGameTime(), Map.of(overworld,
                new ClockNetworkState((long) naturalTicks, 0.0F, naturalRate)));
    }

    private Holder<WorldClock> overworldClock() {
        if (mc.level == null) {
            return null;
        }
        return mc.level.registryAccess().get(WorldClocks.OVERWORLD).orElse(null);
    }
}
