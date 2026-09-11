package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.Config;
import com.dioxidelite.client.Version;
import com.dioxidelite.client.gui.clickgui.pages.*;
import com.dioxidelite.client.ResetManager;
import com.dioxidelite.client.render.skia.DioxideLiteVisuals;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Liquid Glass settings screen, v1.8 native render rework.
 *
 * The whole ClickGUI now renders through plain Minecraft {@link GuiGraphics}
 * (fills, hairline outlines and the vanilla font) instead of the Skia/OpenGL
 * path. This is identical on Windows / Linux / macOS, removes the per-frame
 * Skia surface submission and the GL framebuffer capture that caused blank
 * ClickGUI panels on Windows, and costs a fraction of the old CPU cost.
 */
public class NewSettingsScreen extends Screen implements ClickGuiScreen {

    private final List<BasePage> pages;

    private static final String[] TAB_KEYS_ZH = {"战斗", "视觉", "工具", "主题", "优化", "其他"};
    private static final String[] TAB_KEYS_EN = {"Combat", "Render", "Tools", "Theme", "Optimize", "Misc"};

    private int selectedTab = 0;
    private int hoveredTab = -1;
    private boolean closeHovered = false;
    private boolean resetHovered = false;
    private boolean resetConfirm = false;
    private boolean closing = false;

    private final float[] tabHoverAlpha = new float[TAB_KEYS_ZH.length];
    private float closeHoverAlpha = 0f;
    private float resetHoverAlpha = 0f;
    private float indicatorY = -1f;
    private float openProgress = 0f;
    private float pageTransition = 1f;
    private int transitionFromTab = 0;
    private long lastRenderMs = 0;

    private float contentScrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private boolean draggingInContent = false;
    private boolean draggingScrollbar = false;
    private float scrollbarDragOffset = 0f;
    private boolean redrawRequested = true;
    private static final float OPEN_DURATION = 0.16f;
    private static final float BASE_CARD_W = 740f;
    private static final float BASE_CARD_H = 500f;
    private static final float SCREEN_MARGIN = 24f;
    private String cachedResetText = "";
    private int cachedResetTextWidth = 0;
    private String cachedCloseText = "";
    private int cachedCloseTextWidth = 0;
    private BasePage cachedScrollPage = null;
    private float cachedScrollContentH = Float.NaN;
    private float cachedContentTotalHeight = 0f;
    private float cachedScrollAreaHeight = 0f;
    private float cachedScrollMax = 0f;
    private int lastHoverSignature = Integer.MIN_VALUE;
    private double debugLastDrawMs = 0.0;
    private double debugLastUpdateMs = 0.0;
    private int debugVisibleModules = 0;
    private int debugExpandedModules = 0;
    private int debugAnimatingModules = 0;

    public NewSettingsScreen(Screen parent) {
        super(Component.literal("Settings"));
        this.pages = new ArrayList<>(List.of(new CombatPage(), new RenderPage(), new ToolPage(), new ThemePage(), new OptimizePage(), new MiscPage()));
        this.parent = parent;
    }

    private final Screen parent;

    private float[] layout(int width, int height) {
        float cardW = BASE_CARD_W;
        float cardH = BASE_CARD_H;
        float cardX = (width - cardW) / 2f;
        float cardY = (height - cardH) / 2f;
        float sidebarW = 190f;
        float tabStartY = cardY + 96f;
        float tabH = 34f;
        float tabGap = 2f;
        float tabW = sidebarW - 24f;
        float closeH = 32f;
        float resetH = 32f;
        float closeY = cardY + cardH - 46f;
        float resetY = closeY - resetH - 8f;
        float closeX = cardX + 12f;
        float contentX = cardX + sidebarW + 1f;
        float contentW = cardW - sidebarW - 1f;
        float contentY = cardY + 60f;
        float contentH = cardH - 60f - 12f;
        return new float[]{
                cardX, cardY, cardW, cardH,
                sidebarW, tabStartY, tabH, tabGap, tabW,
                closeX, closeY, closeH, resetY, resetH,
                contentX, contentY, contentW, contentH
        };
    }

    private float getUiScale(int width, int height) {
        float scaleX = Math.max(0.1f, (width - SCREEN_MARGIN) / BASE_CARD_W);
        float scaleY = Math.max(0.1f, (height - SCREEN_MARGIN) / BASE_CARD_H);
        return Math.min(1f, Math.min(scaleX, scaleY));
    }

    private float getVisualScale(int width, int height) {
        return getUiScale(width, height) * (0.9f + 0.1f * easeOutCubic(openProgress));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        drawNative(graphics, this.width, this.height, mouseX, mouseY, delta);
        redrawRequested = false;
    }

    private float toLayoutX(double x, int width, float scale) {
        float cx = width / 2f;
        return cx + ((float) x - cx) / scale;
    }

    private float toLayoutY(double y, int height, float scale) {
        float cy = height / 2f;
        return cy + ((float) y - cy) / scale;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(t, 1f);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float easeOutCubic(float t) {
        float x = 1f - clamp01(t);
        return 1f - x * x * x;
    }

    private static int withAlpha(int color, float alpha) {
        return DioxideLiteVisuals.withAlpha(color, alpha);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(ar+(br-ar)*t) << 16) | ((int)(ag+(bg-ag)*t) << 8) | (int)(ab+(bb-ab)*t);
    }

    private float getContentTotalHeight(BasePage page) {
        return 54f + page.getTotalHeight() + getVisibleModuleGapTotal(page) + 12f;
    }

    private float getVisibleModuleGapTotal(BasePage page) {
        int visibleCount = 0;
        for (var m : page.getModules()) if (m.isVisible()) visibleCount++;
        return visibleCount * 8f;
    }

    private void updateScrollCache(BasePage page, float contentH) {
        float contentTotalHeight = getContentTotalHeight(page);
        if (cachedScrollPage == page
                && cachedScrollContentH == contentH
                && Math.abs(cachedContentTotalHeight - contentTotalHeight) < 0.01f) {
            return;
        }
        cachedScrollPage = page;
        cachedScrollContentH = contentH;
        cachedContentTotalHeight = contentTotalHeight;
        cachedScrollAreaHeight = contentH - 54f;
        cachedScrollMax = Math.max(0f, cachedContentTotalHeight - cachedScrollAreaHeight);
    }

    private void requestRegionRedraw() {
        redrawRequested = true;
        cachedScrollPage = null;
    }

    private void updateDebugStats(BasePage page) {
        int visible = 0;
        int expanded = 0;
        int animating = 0;
        for (var module : page.getModules()) {
            if (!module.isVisible()) continue;
            visible++;
            if (module.getTotalHeight() > 56.5f) expanded++;
            if (module.isAnimating()) animating++;
        }
        debugVisibleModules = visible;
        debugExpandedModules = expanded;
        debugAnimatingModules = animating;
    }

    private void drawDebugOverlay(GuiGraphics g, int cardX, int cardY, int cardW, float alpha) {
        if (!Version.DEBUG) return;
        String line1 = String.format("ClickGUI %.2fms draw %.2fms update", debugLastDrawMs, debugLastUpdateMs);
        String line2 = String.format("modules %d visible %d expanded %d anim", debugVisibleModules, debugExpandedModules, debugAnimatingModules);
        String line3 = String.format("scroll %.1f/%.1f full=%s", contentScrollOffset, cachedScrollMax, Config.fullMode ? "Y" : "N");
        int padding = 8;
        int w = Math.max(this.font.width(line1), Math.max(this.font.width(line2), this.font.width(line3))) + padding * 2;
        int h = 34;
        int x = cardX + cardW - w - 14;
        int y = cardY + 14;
        g.fill(x, y, x + w, y + h, withAlpha(0xF7F7F8, alpha));
        g.drawString(this.font, line1, x + padding, y + 6, withAlpha(0x444444, alpha), false);
        g.drawString(this.font, line2, x + padding, y + 17, withAlpha(0x666666, alpha), false);
        g.drawString(this.font, line3, x + padding, y + 26, withAlpha(0x666666, alpha), false);
    }

    private int computeHoverSignature(double mouseX, double mouseY) {
        float visualScale = getVisualScale(this.width, this.height);
        float mx = toLayoutX(mouseX, this.width, visualScale);
        float my = toLayoutY(mouseY, this.height, visualScale);
        float[] l = layout(this.width, this.height);
        float cardX = l[0];
        float tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float resetY = l[12], resetH = l[13];

        int hovered = -1;
        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap);
            if (mx >= cardX + 12f && mx <= cardX + 12f + tabW && my >= ty && my <= ty + tabH) {
                hovered = i;
                break;
            }
        }

        boolean close = mx >= closeX && mx <= closeX + tabW && my >= closeY && my <= closeY + closeH;
        boolean reset = mx >= closeX && mx <= closeX + tabW && my >= resetY && my <= resetY + resetH;

        int signature = hovered + 2;
        if (close) signature |= 1 << 8;
        if (reset) signature |= 1 << 9;
        return signature;
    }

    private void drawNative(GuiGraphics g, int width, int height, int mouseX, int mouseY, float delta) {
        long debugDrawStartNs = Version.DEBUG ? System.nanoTime() : 0L;
        long now = System.currentTimeMillis();
        float dt = lastRenderMs == 0 ? 0.016f : Math.min((now - lastRenderMs) / 1000f, 0.033f);
        lastRenderMs = now;

        if (closing) {
            openProgress = clamp01(openProgress - dt / OPEN_DURATION);
            if (openProgress < 0.005f) {
                if (this.minecraft != null) this.minecraft.setScreen(parent);
                return;
            }
        } else {
            openProgress = clamp01(openProgress + dt / OPEN_DURATION);
        }

        float animT = easeOutCubic(openProgress);

        float visualScale = getUiScale(width, height) * (0.9f + 0.1f * animT);
        float layoutMouseX = toLayoutX(mouseX, width, visualScale);
        float layoutMouseY = toLayoutY(mouseY, height, visualScale);
        float[] l = layout(width, height);
        BasePage currentPage = pages.get(selectedTab);
        updateScrollCache(currentPage, l[17]);
        targetScrollOffset = Math.min(targetScrollOffset, cachedScrollMax);
        contentScrollOffset = lerp(contentScrollOffset, targetScrollOffset, dt * 18f);
        float cardX = l[0], cardY = l[1], cardW = l[2], cardH = l[3];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11], resetY = l[12], resetH = l[13];
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];

        hoveredTab = -1;
        closeHovered = false;
        resetHovered = false;
        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap);
            if (layoutMouseX >= cardX + 12f && layoutMouseX <= cardX + 12f + tabW && layoutMouseY >= ty && layoutMouseY <= ty + tabH)
                hoveredTab = i;
        }
        if (layoutMouseX >= closeX && layoutMouseX <= closeX + tabW && layoutMouseY >= closeY && layoutMouseY <= closeY + closeH)
            closeHovered = true;
        if (layoutMouseX >= closeX && layoutMouseX <= closeX + tabW && layoutMouseY >= resetY && layoutMouseY <= resetY + resetH)
            resetHovered = true;

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float target = (i == hoveredTab && i != selectedTab) ? 1f : 0f;
            tabHoverAlpha[i] = lerp(tabHoverAlpha[i], target, dt * 12f);
        }
        closeHoverAlpha = lerp(closeHoverAlpha, closeHovered ? 1f : 0f, dt * 12f);
        resetHoverAlpha = lerp(resetHoverAlpha, resetHovered ? 1f : 0f, dt * 12f);

        float targetIndicatorY = tabStartY + selectedTab * (tabH + tabGap);
        if (indicatorY < 0f) indicatorY = targetIndicatorY;
        indicatorY = lerp(indicatorY, targetIndicatorY, dt * 12f);

        BasePage page = currentPage;
        long debugUpdateStartNs = Version.DEBUG ? System.nanoTime() : 0L;
        page.update(dt);
        pageTransition = lerp(pageTransition, 1f, Math.min(1f, dt * 11f));
        if (Version.DEBUG) {
            debugLastUpdateMs = (System.nanoTime() - debugUpdateStartNs) / 1_000_000.0;
            updateDebugStats(page);
        }

        float alpha = 1f;
        float cx = width / 2f;
        float cy = height / 2f;

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(visualScale, visualScale);
        g.pose().translate(-cx, -cy);

        // Glass panel: translucent fill + hairline border, no framebuffer blur.
        DioxideLiteVisuals.glassFast(g,
                Math.round(cardX), Math.round(cardY), Math.round(cardW), Math.round(cardH), alpha,
                0x111827, Config.glassOpacity, 0xD7E4F5, 0.16f);

        int sidebarBase = switch (Config.visualStyle) {
            case AURORA -> 0x102238;
            case RISE_CLEAN -> 0x10151D;
            case MINIMAL -> 0x080B10;
            default -> 0x0B121A;
        };
        g.fill(Math.round(cardX), Math.round(cardY), Math.round(cardX + sidebarW), Math.round(cardY + cardH),
                withAlpha(sidebarBase, (Config.visualStyle == Config.VisualStyle.LIQUID_GLASS ? 0.34f : 0.48f) * alpha));
        g.fill(Math.round(cardX + sidebarW), Math.round(cardY + 14f), Math.round(cardX + sidebarW + 1f), Math.round(cardY + cardH - 14f),
                withAlpha(0xFFFFFF, 0.10f * alpha));

        g.drawString(this.font, "DIOXIDE", Math.round(cardX + 18f), Math.round(cardY + 26f), DioxideLiteVisuals.text(alpha), false);
        g.drawString(this.font, "LITE", Math.round(cardX + 76f), Math.round(cardY + 26f), DioxideLiteVisuals.accent(alpha * 0.92f), false);
        drawDebugOverlay(g, Math.round(cardX), Math.round(cardY), Math.round(cardW), alpha);
        g.drawString(this.font, UiText.t("视觉 / 交互 / 性能", "VISUALS / INTERACTION / PERFORMANCE"),
                Math.round(cardX + 18f), Math.round(cardY + 40f), DioxideLiteVisuals.muted(alpha * 0.82f), false);

        g.fill(Math.round(cardX + 12f), Math.round(indicatorY), Math.round(cardX + 12f + tabW), Math.round(indicatorY + tabH),
                withAlpha(DioxideLiteVisuals.CYAN, 0.10f * alpha));
        DioxideLiteVisuals.accentLineFast(g, Math.round(cardX + 12f), Math.round(indicatorY + tabH - 2f), 34, alpha * 0.75f);

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float tabY = tabStartY + i * (tabH + tabGap);
            if (tabHoverAlpha[i] > 0.01f) {
                g.fill(Math.round(cardX + 12f), Math.round(tabY), Math.round(cardX + 12f + tabW), Math.round(tabY + tabH),
                        withAlpha(0xB9DFFF, 0.055f * alpha * tabHoverAlpha[i]));
            }
            boolean active = i == selectedTab;
            int textColor = active ? DioxideLiteVisuals.text(alpha) : DioxideLiteVisuals.muted(alpha * 0.88f);
            g.drawString(this.font, UiText.t(TAB_KEYS_ZH[i], TAB_KEYS_EN[i]),
                    Math.round(cardX + 18f), Math.round(tabY + tabH / 2f - 4f), textColor, false);
        }

        int closeTextColor = lerpColor(0xFF000000 | DioxideLiteVisuals.MUTED, 0xFFB9DFFF, closeHoverAlpha);
        int resetTextColor = lerpColor(0xFF000000 | DioxideLiteVisuals.MUTED, 0xFFB9DFFF, resetHoverAlpha);
        g.fill(Math.round(closeX), Math.round(resetY), Math.round(closeX + tabW), Math.round(resetY + resetH),
                withAlpha(0x0B1119, alpha * (0.72f + resetHoverAlpha * 0.12f)));
        DioxideLiteVisuals.outlineFast(g, Math.round(closeX), Math.round(resetY), Math.round(tabW), Math.round(resetH),
                DioxideLiteVisuals.CYAN, alpha * (0.10f + resetHoverAlpha * 0.20f));
        String resetText = resetConfirm ? UiText.t("再次点击以确认", "Click Again to Confirm") : UiText.t("重置所有设置", "Reset All Settings");
        if (!resetText.equals(cachedResetText)) {
            cachedResetText = resetText;
            cachedResetTextWidth = this.font.width(resetText);
        }
        g.drawString(this.font, resetText, Math.round(closeX + (tabW - cachedResetTextWidth) / 2f), Math.round(resetY + resetH / 2f - 4f),
                withAlpha(resetTextColor, alpha), false);

        g.fill(Math.round(closeX), Math.round(closeY), Math.round(closeX + tabW), Math.round(closeY + closeH),
                withAlpha(0x0B1119, alpha * (0.72f + closeHoverAlpha * 0.12f)));
        DioxideLiteVisuals.outlineFast(g, Math.round(closeX), Math.round(closeY), Math.round(tabW), Math.round(closeH),
                closeHovered ? 0xD5ECFF : 0x6E7F93, alpha * (0.14f + closeHoverAlpha * 0.20f));
        String closeText = UiText.t("× 关闭", "× Close");
        if (!closeText.equals(cachedCloseText)) {
            cachedCloseText = closeText;
            cachedCloseTextWidth = this.font.width(closeText);
        }
        g.drawString(this.font, closeText, Math.round(closeX + (tabW - cachedCloseTextWidth) / 2f), Math.round(closeY + closeH / 2f - 4f),
                withAlpha(closeTextColor, alpha), false);

        float pageAlpha = clamp01(pageTransition);
        float pageSlide = (1f - pageAlpha) * 10f;
        g.drawString(this.font, page.getTitle(), Math.round(contentX + 18f + pageSlide), Math.round(contentY + 18f),
                DioxideLiteVisuals.text(alpha * pageAlpha), false);
        g.drawString(this.font, page.getSubtitle(), Math.round(contentX + 18f + pageSlide), Math.round(contentY + 31f),
                DioxideLiteVisuals.muted(alpha * pageAlpha), false);
        DioxideLiteVisuals.accentLineFast(g, Math.round(contentX + 18f + pageSlide), Math.round(contentY + 42f), 44, alpha * pageAlpha * 0.72f);

        float clipTop = contentY + 54f;
        float clipBottom = contentY + contentH;

        float moduleStartY = contentY + 54f;
        page.drawFast(g, Math.round(contentX + 10f), Math.round(moduleStartY), Math.round(contentW - 40f), Math.round(contentH - 54f),
                Math.round(alpha * 255f), contentScrollOffset, Math.round(layoutMouseX), Math.round(layoutMouseY));
        drawScrollbar(g, page, contentX, contentY, contentW, contentH, alpha);

        g.pose().popMatrix();

        if (Version.DEBUG) {
            debugLastDrawMs = (System.nanoTime() - debugDrawStartNs) / 1_000_000.0;
        }
    }

    private void drawScrollbar(GuiGraphics g, BasePage page, float contentX, float contentY, float contentW, float contentH, float alpha) {
        updateScrollCache(page, contentH);
        if (cachedContentTotalHeight <= cachedScrollAreaHeight) return;

        float trackX = contentX + contentW - 8f;
        float trackTop = contentY + 60f;
        float trackH = contentH - 60f - 8f;
        float thumbH = Math.max(20f, trackH * cachedScrollAreaHeight / cachedContentTotalHeight);
        float maxScroll = Math.max(1f, cachedScrollMax);
        float progress = Math.min(1f, contentScrollOffset / maxScroll);
        float thumbTop = trackTop + (trackH - thumbH) * progress;
        thumbTop = Math.min(thumbTop, trackTop + trackH - thumbH);

        g.fill(Math.round(trackX), Math.round(trackTop), Math.round(trackX + 4f), Math.round(trackTop + trackH),
                withAlpha(0xE0E0E0, alpha * 0.5f));
        g.fill(Math.round(trackX), Math.round(thumbTop), Math.round(trackX + 4f), Math.round(thumbTop + thumbH),
                withAlpha(0xBBBBBB, alpha));
    }

    private boolean hasScrollbar(BasePage page, float contentH) {
        updateScrollCache(page, contentH);
        return cachedContentTotalHeight > cachedScrollAreaHeight;
    }

    private float scrollbarTrackX(float contentX, float contentW) {
        return contentX + contentW - 8f;
    }

    private float scrollbarTrackTop(float contentY) {
        return contentY + 60f;
    }

    private float scrollbarTrackH(float contentH) {
        return contentH - 60f - 8f;
    }

    private float scrollbarThumbH(BasePage page, float contentH) {
        updateScrollCache(page, contentH);
        return Math.max(20f, scrollbarTrackH(contentH) * cachedScrollAreaHeight / cachedContentTotalHeight);
    }

    private float scrollbarThumbTop(BasePage page, float contentY, float contentH) {
        updateScrollCache(page, contentH);
        float trackTop = scrollbarTrackTop(contentY);
        float trackH = scrollbarTrackH(contentH);
        float thumbH = scrollbarThumbH(page, contentH);
        float maxScroll = Math.max(1f, cachedScrollMax);
        float progress = Math.min(1f, targetScrollOffset / maxScroll);
        return Math.min(trackTop + (trackH - thumbH) * progress, trackTop + trackH - thumbH);
    }

    private boolean isInScrollbar(float mx, float my, float contentX, float contentY, float contentW, float contentH) {
        float x = scrollbarTrackX(contentX, contentW) - 6f;
        float top = scrollbarTrackTop(contentY);
        float h = scrollbarTrackH(contentH);
        return mx >= x && mx <= x + 16f && my >= top && my <= top + h;
    }

    private void setScrollFromScrollbar(BasePage page, float my, float contentY, float contentH) {
        updateScrollCache(page, contentH);
        float maxScroll = cachedScrollMax;
        float trackTop = scrollbarTrackTop(contentY);
        float trackH = scrollbarTrackH(contentH);
        float thumbH = scrollbarThumbH(page, contentH);
        float available = Math.max(1f, trackH - thumbH);
        float thumbTop = Math.max(trackTop, Math.min(my - scrollbarDragOffset, trackTop + available));
        targetScrollOffset = maxScroll * ((thumbTop - trackTop) / available);
    }

    @Override public void onClose() { closing = true; }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int hoverSignature = computeHoverSignature(mouseX, mouseY);
        if (hoverSignature != lastHoverSignature) {
            lastHoverSignature = hoverSignature;
            requestRegionRedraw();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (closing) return false;
        int button = event.button();

        float visualScale = getVisualScale(this.width, this.height);
        float mx = toLayoutX(event.x(), this.width, visualScale);
        float my = toLayoutY(event.y(), this.height, visualScale);
        float[] l = layout(this.width, this.height);
        float cardX = l[0];
        float sidebarW = l[4], tabStartY = l[5], tabH = l[6], tabGap = l[7], tabW = l[8];
        float closeX = l[9], closeY = l[10], closeH = l[11];
        float resetY = l[12], resetH = l[13];
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];
        BasePage page = pages.get(selectedTab);

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (tabH + tabGap);
            if (mx >= cardX + 12f && mx <= cardX + 12f + tabW && my >= ty && my <= ty + tabH) {
                if (button == 0) {
                    if (selectedTab != i) { transitionFromTab = selectedTab; pageTransition = 0f; }
                    selectedTab = i; targetScrollOffset = 0f; contentScrollOffset = 0f; requestRegionRedraw();
                }
                return true;
            }
        }

        if (button == 0 && mx >= closeX && mx <= closeX + tabW && my >= closeY && my <= closeY + closeH) {
            closing = true;
            requestRegionRedraw();
            return true;
        }

        if (button == 0 && mx >= closeX && mx <= closeX + tabW && my >= resetY && my <= resetY + resetH) {
            if (resetConfirm) {
                ResetManager.resetAll();
                resetConfirm = false;
                pages.set(selectedTab, switch (selectedTab) {
                    case 0 -> new CombatPage();
                    case 1 -> new RenderPage();
                    case 2 -> new ToolPage();
                    case 3 -> new ThemePage();
                    case 4 -> new OptimizePage();
                    default -> new MiscPage();
                });
            } else {
                resetConfirm = true;
            }
            requestRegionRedraw();
            return true;
        }

        resetConfirm = false;

        if (mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH) {
            if (hasScrollbar(page, contentH) && isInScrollbar(mx, my, contentX, contentY, contentW, contentH)) {
                draggingScrollbar = true;
                float thumbTop = scrollbarThumbTop(page, contentY, contentH);
                float thumbH = scrollbarThumbH(page, contentH);
                scrollbarDragOffset = my >= thumbTop && my <= thumbTop + thumbH ? my - thumbTop : thumbH * 0.5f;
                setScrollFromScrollbar(page, my, contentY, contentH);
                contentScrollOffset = targetScrollOffset;
                requestRegionRedraw();
                return true;
            }
            float moduleStartY = contentY + 54f;
            boolean hit = page.onClick(mx, my, contentX + 10f, moduleStartY, contentW - 40f, contentScrollOffset, button);
            if (hit) {
                draggingInContent = true;
                requestRegionRedraw();
            }
            return hit;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar) {
            float visualScale = getVisualScale(this.width, this.height);
            float my = toLayoutY(event.y(), this.height, visualScale);
            float[] l = layout(this.width, this.height);
            BasePage page = pages.get(selectedTab);
            setScrollFromScrollbar(page, my, l[15], l[17]);
            contentScrollOffset = targetScrollOffset;
            requestRegionRedraw();
            return true;
        }
        if (draggingInContent) {
            float visualScale = getVisualScale(this.width, this.height);
            float mx = toLayoutX(event.x(), this.width, visualScale);
            float my = toLayoutY(event.y(), this.height, visualScale);
            float[] l = layout(this.width, this.height);
            float contentX = l[14], contentY = l[15], contentW = l[16];
            float moduleStartY = contentY + 54f;
            pages.get(selectedTab).onDrag(mx, my, contentX + 10f, moduleStartY, contentW - 40f, contentScrollOffset);
            requestRegionRedraw();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingInContent = false;
        draggingScrollbar = false;
        pages.get(selectedTab).releaseDrag();
        requestRegionRedraw();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        float visualScale = getVisualScale(this.width, this.height);
        float layoutMx = toLayoutX(mx, this.width, visualScale);
        float layoutMy = toLayoutY(my, this.height, visualScale);
        float[] l = layout(this.width, this.height);
        float contentX = l[14], contentY = l[15], contentW = l[16], contentH = l[17];

        if (layoutMx >= contentX && layoutMx <= contentX + contentW && layoutMy >= contentY && layoutMy <= contentY + contentH) {
            BasePage page = pages.get(selectedTab);
            updateScrollCache(page, contentH);
            targetScrollOffset = Math.max(0f, Math.min(cachedScrollMax, targetScrollOffset + (float)(-vScroll * 16f)));
            requestRegionRedraw();
            return true;
        }
        return false;
    }

    @Override
    protected void renderBlurredBackground(GuiGraphics guiGraphics) {}

    @Override
    protected void renderMenuBackground(GuiGraphics guiGraphics) {}

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {}
}
