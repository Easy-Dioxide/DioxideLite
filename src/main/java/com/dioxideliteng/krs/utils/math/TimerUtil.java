package com.dioxideliteng.krs.utils.math;

import com.dioxideliteng.util.TimerController;

/**
 * KRS 的计时倍速开关，同时是两套计时系统的桥。
 *
 * <p>背景：DioxideLite NG 自己有一套计时倍速（{@code MixinTimer} 以
 * {@code @Overwrite} 重写 {@code DeltaTracker.Timer#advanceGameTime}，读
 * {@code TimerController}）；KRS 原版则是在同一方法里以 {@code @At("FIELD")}
 * 二次缩放 {@code deltaTicks}。两者作用于同一个字段，同时存在会互相覆盖，且
 * Mixin 注入点会因 {@code @Overwrite} 而找不到目标。</p>
 *
 * <p>因此移植版<b>不搬</b> KRS 的 {@code RenderTickCounterDynamicMixin}，
 * 改为把 KRS 侧的写入统一转到 NG 的 {@code TimerController}：KRS 的
 * Timer / Spider / LongJump / TuckMod 等模块仍然只写 {@link #timerSpeed}，
 * 行为与 KRS 原版一致（{@code 0} 表示不干预），最终由 NG 的单一实现生效。</p>
 */
public class TimerUtil {

    /** KRS 语义：0 或负数表示「不修改计时」，>0 时为倍速。 */
    public static float timerSpeed = 0f;

    /**
     * KRS 模块改写倍速后调用，把值同步给 NG 的计时控制器。
     *
     * <p>为了让现有 {@code TimerUtil.timerSpeed = x} 的直接赋值也能生效，
     * 这里额外由 {@code TimerSpeedSyncMixin} 在每个游戏刻把字段推给 NG，
     * 双保险且不改变 KRS 模块的调用方式。</p>
     */
    public static void applyToGameTimer() {
        TimerController.setMultiplier(timerSpeed > 0f ? timerSpeed : 1.0f);
    }

    public static void reset() {
        timerSpeed = 0f;
        TimerController.setMultiplier(1.0f);
    }
}
