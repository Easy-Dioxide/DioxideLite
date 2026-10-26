package com.dioxideliteng.krs.screen;

import com.dioxideliteng.krs.utils.IMinecraft;
import com.dioxideliteng.krs.utils.nanovg.NanoVGManager;

/**
 * KRS 主菜单标题界面的精简替代。
 *
 * <p>KRS 原版的 {@code CustomTitleScreen} 会接管游戏主菜单，与 DioxideLite NG 自己的
 * {@code DioxideLiteNGTitleScreen} 冲突，因此这里不搬入完整界面，只保留被
 * {@link NanoVGClickGuiScreen} 与背景渲染引用的<b>背景视差状态</b>，
 * 保证 KRS 的 ClickGui 视觉行为与原版一致。</p>
 */
public final class CustomTitleScreen implements IMinecraft {

    public static float bgOffsetX = 0f;
    public static float bgOffsetY = 0f;

    private static float bgParallaxScreenWidth = -1f;
    private static float bgParallaxScreenHeight = -1f;
    private static boolean bgParallaxHoldCenter = true;
    private static float bgParallaxHoldMouseX = 0f;
    private static float bgParallaxHoldMouseY = 0f;

    private CustomTitleScreen() {
    }

    /** 与 KRS 原实现逐行一致的背景视差插值。 */
    public static void stepBackgroundParallax(float targetX, float targetY, float amount) {
        if (!Float.isFinite(targetX) || !Float.isFinite(targetY) || !Float.isFinite(amount)) {
            return;
        }

        float screenWidth = NanoVGManager.getScaledScreenWidth();
        float screenHeight = NanoVGManager.getScaledScreenHeight();
        if (!(screenWidth > 0f) || !(screenHeight > 0f)) {
            return;
        }

        if (screenWidth != bgParallaxScreenWidth || screenHeight != bgParallaxScreenHeight
                || !Float.isFinite(bgOffsetX) || !Float.isFinite(bgOffsetY)) {
            bgParallaxScreenWidth = screenWidth;
            bgParallaxScreenHeight = screenHeight;
            bgOffsetX = screenWidth / 2f;
            bgOffsetY = screenHeight / 2f;
            bgParallaxHoldCenter = true;
            bgParallaxHoldMouseX = targetX;
            bgParallaxHoldMouseY = targetY;
            return;
        }

        if (bgParallaxHoldCenter) {
            if (Math.abs(targetX - bgParallaxHoldMouseX) < 0.5f && Math.abs(targetY - bgParallaxHoldMouseY) < 0.5f) {
                targetX = screenWidth / 2f;
                targetY = screenHeight / 2f;
            } else {
                bgParallaxHoldCenter = false;
            }
        }

        bgOffsetX += (targetX - bgOffsetX) * amount;
        bgOffsetY += (targetY - bgOffsetY) * amount;
    }

    /** 主菜单音乐由 NG 自己管理，这里保持空实现以兼容调用点。 */
    public static void stopMainMenuMusic() {
    }

    /** KRS 启动加载条只属于 KRS 主菜单；NG 有独立加载界面，故恒为不可见。 */
    public static boolean isStartupLoadBarVisible() {
        return false;
    }

    /** 同上：不绘制 KRS 的启动加载条。 */
    public static void renderStartupLoadBarIfNeeded(NanoVGManager nanoVgManager) {
    }
}
