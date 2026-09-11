package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.Config;
import com.dioxidelite.client.Version;
import com.dioxidelite.client.gui.clickgui.pages.*;
import com.dioxidelite.client.ResetManager;
import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.SkiaRenderer;
import com.dioxidelite.client.render.skia.LiquidGlassRenderer;
import com.dioxidelite.client.render.skia.DioxideLiteVisuals;
import net.minecraft.client.Minecraft;
import com.dioxidelite.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class NewSettingsScreen extends SkiaScreen {

    private final List<BasePage> pages;

    private static final String[] TAB_ICONS = {"\uE903", "\uE901", "\uE026", "\uE3A9", "\uE121", "\uE900"};
    private static final String[] TAB_ICON_FONTS = {FontRenderer.ICON, FontRenderer.ICON, FontRenderer.MATERIAL_SYMBOLS, FontRenderer.MATERIAL_SYMBOLS, FontRenderer.MATERIAL_SYMBOLS, FontRenderer.ICON};
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
    private int cachedRegionX = 0;
    private int cachedRegionY = 0;
    private int cachedRegionW = 0;
    private int cachedRegionH = 0;
    private static final float OPEN_DURATION = 0.16f;
    private static final float BASE_CARD_W = 740f;
    private static final float BASE_CARD_H = 500f;
    private static final float SCREEN_MARGIN = 24f;
    private final Paint cardPaint = new Paint();
    private final Paint sidebarPaint = new Paint();
    private final Paint dividerPaint = new Paint();
    private final Paint indicatorPaint = new Paint();
    private final Paint hoverPaint = new Paint();
    private final Paint resetBgPaint = new Paint();
    private final Paint closeBgPaint = new Paint();
    private final Paint scrollbarTrackPaint = new Paint();
    private final Paint scrollbarThumbPaint = new Paint();
    private final float resetIconWidth = FontRenderer.measureTextWidth("\uE042", 13f, FontRenderer.MATERIAL_SYMBOLS);
    private String cachedResetText = "";
    private float cachedResetTextWidth = 0f;
    private String cachedCloseText = "";
    private float cachedCloseTextWidth = 0f;
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
        super(Component.literal("Settings"), parent);
        pages = new ArrayList<>(List.of(new CombatPage(), new RenderPage(), new ToolPage(), new ThemePage(), new OptimizePage(), new MiscPage()));
    }

    /**
     * Pure transparent-glass mode (GLASS theme). Restores the v1.7 Liquid Glass
     * material on top of the GL path: the panel itself is drawn with the real
     * glass shader surface (blur + translucency + rim) instead of the opaque
     * dark base used by ORIGINAL under the GPU backend.
     */
    protected boolean usePureGlass() {
        return false;
    }

    protected void drawGlass(Canvas canvas, float cardX, float cardY, float cardW, float cardH, float alpha) {
        if (usePureGlass() || !SkiaRenderer.isDrawing()) {
            LiquidGlassRenderer.drawSurface(canvas, null,
                    cardX, cardY, cardW, cardH, Config.glassRadius, alpha);
        } else {
            Paint glassBase = new Paint().setAntiAlias(true);
            glassBase.setColor(withAlpha(0x111827, Config.glassOpacity * alpha));
            canvas.drawRRect(RRect.makeXYWH(cardX, cardY, cardW, cardH, Config.glassRadius), glassBase);
            glassBase.close();
        }
    }

    private float[] layout(int width, int height) {
        float cardW = BASE_CARD_W;
        float cardH = BASE_CARD_H;
        float cardX = (width - cardW) / 2f;
        float cardY = (height - cardH) / 2f;
        float sidebarW = 190f;
        float tabStartY = cardY + 110f;
        float tabH = 38f;
        float tabGap = 2f;
        float tabW = sidebarW - 24f;
        float closeH = 34f;
        float resetH = 34f;
        float closeY = cardY + cardH - 48f;
        float resetY = closeY - resetH - 8f;
        float closeX = cardX + 12f;
        float contentX = cardX + sidebarW + 1f;
        float contentW = cardW - sidebarW - 1f;
        float contentY = cardY + 66f;
        float contentH = cardH - 66f - 12f;
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
        float visualScale = getVisualScale(this.width, this.height);
        float[] l = layout(this.width, this.height);
        float cx = this.width / 2f;
        float cy = this.height / 2f;
        float pad = 28f;
        int regionX = (int) Math.floor(cx + (l[0] - cx) * visualScale - pad);
        int regionY = (int) Math.floor(cy + (l[1] - cy) * visualScale - pad);
        int regionW = (int) Math.ceil(l[2] * visualScale + pad * 2f);
        int regionH = (int) Math.ceil(l[3] * visualScale + pad * 2f);
        regionX = Math.max(0, regionX);
        regionY = Math.max(0, regionY);
        regionW = Math.min(this.width - regionX, Math.max(1, regionW));
        regionH = Math.min(this.height - regionY, Math.max(1, regionH));

        // v1.7.1: render ClickGUI directly to the active GL framebuffer.
        // The previous raster-region path uploaded a CPU Skia surface every
        // animation frame and then performed another framebuffer blur pass.
        // On fast GPUs that synchronization could collapse FPS to ~1.
        if (Config.visualStyle != Config.VisualStyle.MINIMAL) {
            float cx2 = this.width / 2f, cy2 = this.height / 2f;
            float bx = cx2 + (l[0] - cx2) * visualScale;
            float by = cy2 + (l[1] - cy2) * visualScale;
            float bw = l[2] * visualScale;
            float bh = l[3] * visualScale;
            com.dioxidelite.client.render.skia.SkiaBlurRenderer.getInstance().render(
                    Minecraft.getInstance(), bx, by, bw, bh,
                    Config.glassRadius * visualScale, 0x30D9ECFF, Config.glassBlur * 0.85f);
        }

        Canvas canvas = SkiaRenderer.begin();
        if (canvas != null) {
            drawSkia(canvas, this.width, this.height, mouseX, mouseY, delta);
        }
        SkiaRenderer.end(graphics, this.width, this.height);
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
        return ((int)(alpha * 255) << 24) | (color & 0x00FFFFFF);
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
        SkiaRenderer.markRegionDirty();
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

    private void drawDebugOverlay(Canvas canvas, float cardX, float cardY, float cardW, float alpha) {
        if (!Version.DEBUG) return;
        String line1 = String.format("ClickGUI %.2fms draw %.2fms update", debugLastDrawMs, debugLastUpdateMs);
        String line2 = String.format("modules %d visible %d expanded %d anim", debugVisibleModules, debugExpandedModules, debugAnimatingModules);
        String line3 = String.format("scroll %.1f/%.1f full=%s", contentScrollOffset, cachedScrollMax, Config.fullMode ? "Y" : "N");
        float padding = 10f;
        float textSize = 10f;
        float w = Math.max(FontRenderer.measureTextWidth(line1, textSize), Math.max(FontRenderer.measureTextWidth(line2, textSize), FontRenderer.measureTextWidth(line3, textSize))) + padding * 2f;
        float h = 42f;
        float x = cardX + cardW - w - 14f;
        float y = cardY + 14f;
        Paint bg = new Paint();
        bg.setColor(withAlpha(0xF7F7F8, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 8f), bg);
        FontRenderer.drawText(canvas, line1, x + padding, y + 13f, textSize, withAlpha(0x444444, alpha));
        FontRenderer.drawText(canvas, line2, x + padding, y + 25f, textSize, withAlpha(0x666666, alpha));
        FontRenderer.drawText(canvas, line3, x + padding, y + 37f, textSize, withAlpha(0x666666, alpha));
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

    @Override
    protected boolean needsContinuousRedraw() {
        if (closing) return true;
        if (openProgress < 0.999f) return true;
        if (pageTransition < 0.999f) return true;
        if (draggingInContent || draggingScrollbar) return true;
        if (Math.abs(contentScrollOffset - targetScrollOffset) > 0.35f) return true;
        if (closeHoverAlpha > 0.01f || closeHovered) return true;
        if (resetHoverAlpha > 0.01f || resetHovered) return true;
        if (indicatorY < 0f) return true;
        float[] l = layout(this.width, this.height);
        float targetIndicatorY = l[5] + selectedTab * (l[6] + l[7]);
        if (Math.abs(indicatorY - targetIndicatorY) > 0.35f) return true;
        for (float alpha : tabHoverAlpha) if (alpha > 0.01f && alpha < 0.99f) return true;
        return pages.get(selectedTab).hasAnimatingModules();
    }

    @Override
    protected void drawSkia(Canvas canvas, int width, int height, int mouseX, int mouseY, float delta) {
        long debugDrawStartNs = Version.DEBUG ? System.nanoTime() : 0L;
        long now = System.currentTimeMillis();
        float dt = lastRenderMs == 0 ? 0.016f : Math.min((now - lastRenderMs) / 1000f, 0.033f);
        lastRenderMs = now;

        if (closing) {
            openProgress = clamp01(openProgress - dt / OPEN_DURATION);
            if (openProgress < 0.005f) {
                super.closing();
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

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(visualScale, visualScale);
        canvas.translate(-cx, -cy);

        if (!SkiaRenderer.isDrawing() || usePureGlass()) {
            drawGlass(canvas, cardX, cardY, cardW, cardH, alpha);
        } else {
            // The panel blur was already composited on the native GL path before
            // Skia started drawing. Keep only the glass material layers here.
            Paint glassBase = new Paint().setAntiAlias(true);
            glassBase.setColor(withAlpha(0x111827, Config.glassOpacity * alpha));
            canvas.drawRRect(RRect.makeXYWH(cardX, cardY, cardW, cardH, Config.glassRadius), glassBase);
            glassBase.close();
        }

        int sidebarBase = switch (Config.visualStyle) {
            case AURORA -> 0x102238;
            case RISE_CLEAN -> 0x10151D;
            case MINIMAL -> 0x080B10;
            default -> 0x0B121A;
        };
        sidebarPaint.setColor(withAlpha(sidebarBase,
                (Config.visualStyle == Config.VisualStyle.LIQUID_GLASS ? 0.34f : 0.48f) * alpha));
        canvas.save();
        canvas.clipRRect(RRect.makeXYWH(cardX, cardY, sidebarW, cardH, 16f));
        canvas.drawRect(Rect.makeXYWH(cardX, cardY, sidebarW, cardH), sidebarPaint);
        canvas.restore();

        dividerPaint.setColor(withAlpha(0xFFFFFF, 0.10f * alpha));
        canvas.drawRect(Rect.makeXYWH(cardX + sidebarW, cardY + 14f, 1f, cardH - 28f), dividerPaint);

        FontRenderer.drawText(canvas, "DIOXIDE", cardX + 18f, cardY + 35f, 15f, DioxideLiteVisuals.text(alpha));
        FontRenderer.drawText(canvas, "LITE", cardX + 18f, cardY + 50f, 8.5f, DioxideLiteVisuals.accent(alpha * 0.92f));
        drawDebugOverlay(canvas, cardX, cardY, cardW, alpha);
        FontRenderer.drawText(canvas, UiText.t("视觉 / 交互 / 性能", "VISUALS / INTERACTION / PERFORMANCE"), cardX + 18f, cardY + 66f, 8.5f, DioxideLiteVisuals.muted(alpha * 0.82f));

        indicatorPaint.setColor(withAlpha(DioxideLiteVisuals.CYAN, 0.10f * alpha));
        canvas.drawRRect(RRect.makeXYWH(cardX + 12f, indicatorY, tabW, tabH, 7f), indicatorPaint);
        DioxideLiteVisuals.accentLine(canvas, cardX + 12f, indicatorY + tabH - 1.5f, 34f, alpha * 0.75f);

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float tabY = tabStartY + i * (tabH + tabGap);
            if (tabHoverAlpha[i] > 0.01f) {
                hoverPaint.setColor(withAlpha(0xB9DFFF, 0.055f * alpha * tabHoverAlpha[i]));
                canvas.drawRRect(RRect.makeXYWH(cardX + 12f, tabY, tabW, tabH, 7f), hoverPaint);
            }
            boolean active = i == selectedTab;
            int iconColor = active ? DioxideLiteVisuals.accent(alpha) : DioxideLiteVisuals.muted(alpha * 0.82f);
            int textColor = active ? DioxideLiteVisuals.text(alpha) : DioxideLiteVisuals.muted(alpha * 0.88f);
            FontRenderer.drawText(canvas, TAB_ICONS[i], cardX + 18f, tabY + tabH / 2f + 6f, 13f, iconColor, TAB_ICON_FONTS[i]);
            FontRenderer.drawText(canvas, UiText.t(TAB_KEYS_ZH[i], TAB_KEYS_EN[i]), cardX + 38f, tabY + tabH / 2f + 6f, 13f, textColor);
        }

        int closeTextColor = lerpColor(0xFF000000 | DioxideLiteVisuals.MUTED, 0xFFB9DFFF, closeHoverAlpha);
        int resetTextColor = lerpColor(0xFF000000 | DioxideLiteVisuals.MUTED, 0xFFB9DFFF, resetHoverAlpha);
        resetBgPaint.setColor(withAlpha(0x0B1119, alpha * (0.72f + resetHoverAlpha * 0.12f)));
        canvas.drawRRect(RRect.makeXYWH(closeX, resetY, tabW, resetH, 7f), resetBgPaint);
        DioxideLiteVisuals.outline(canvas, closeX, resetY, tabW, resetH, 7f, DioxideLiteVisuals.CYAN, alpha * (0.10f + resetHoverAlpha * 0.20f), 0.7f);
        String resetText = resetConfirm ? UiText.t("再次点击以确认", "Click Again to Confirm") : UiText.t("重置所有设置", "Reset All Settings");
        String resetIcon = "\uE042";
        if (!resetText.equals(cachedResetText)) {
            cachedResetText = resetText;
            cachedResetTextWidth = FontRenderer.measureTextWidth(resetText, 12f);
        }
        float resetTotalW = resetIconWidth + 6f + cachedResetTextWidth;
        float resetStartX = closeX + (tabW - resetTotalW) / 2f;
        FontRenderer.drawText(canvas, resetIcon, resetStartX, resetY + 22f, 13f, withAlpha(resetTextColor, alpha), FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, resetText, resetStartX + resetIconWidth + 6f, resetY + 22f, 12f, withAlpha(resetTextColor, alpha));

        closeBgPaint.setColor(withAlpha(0x0B1119, alpha * (0.72f + closeHoverAlpha * 0.12f)));
        canvas.drawRRect(RRect.makeXYWH(closeX, closeY, tabW, closeH, 7f), closeBgPaint);
        DioxideLiteVisuals.outline(canvas, closeX, closeY, tabW, closeH, 7f, closeHovered ? 0xD5ECFF : 0x6E7F93, alpha * (0.14f + closeHoverAlpha * 0.20f), 0.7f);
        String closeText = UiText.t("× 关闭", "× Close");
        if (!closeText.equals(cachedCloseText)) {
            cachedCloseText = closeText;
            cachedCloseTextWidth = FontRenderer.measureTextWidth(closeText, 12f);
        }
        FontRenderer.drawText(canvas, closeText, closeX + (tabW - cachedCloseTextWidth) / 2f, closeY + 22f, 12f, withAlpha(closeTextColor, alpha));

        float pageAlpha = clamp01(pageTransition);
        float pageSlide = (1f - pageAlpha) * 10f;
        FontRenderer.drawText(canvas, page.getTitle(), contentX + 18f + pageSlide, contentY + 25f, 18f, DioxideLiteVisuals.text(alpha * pageAlpha));
        FontRenderer.drawText(canvas, page.getSubtitle(), contentX + 18f + pageSlide, contentY + 41f, 9.5f, DioxideLiteVisuals.muted(alpha * pageAlpha));
        DioxideLiteVisuals.accentLine(canvas, contentX + 18f + pageSlide, contentY + 49f, 44f, alpha * pageAlpha * 0.72f);

        float clipTop = contentY + 54f;
        float clipBottom = contentY + contentH;
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(contentX, clipTop, contentW, clipBottom - clipTop));

        float moduleStartY = contentY + 54f;
        page.draw(canvas, contentX + 10f, moduleStartY, contentW - 40f, contentH - 54f, alpha, contentScrollOffset);
        drawScrollbar(canvas, page, contentX, contentY, contentW, contentH, alpha);

        canvas.restore();

        canvas.restore();

        if (Version.DEBUG) {
            debugLastDrawMs = (System.nanoTime() - debugDrawStartNs) / 1_000_000.0;
        }

        // TEMP FPS probe: shows render frame-rate inside the ClickGUI (like the F3 overlay).
        String fpsLine = String.format("FPS: %d", Minecraft.getInstance().getFps());
        float fpsW = FontRenderer.measureTextWidth(fpsLine, 12f);
        FontRenderer.drawText(canvas, fpsLine, cardX + cardW - fpsW - 14f, cardY + 20f, 12f, withAlpha(0xFFE0FF, 1f));
    }

    private void drawScrollbar(Canvas canvas, BasePage page, float contentX, float contentY, float contentW, float contentH, float alpha) {
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

        scrollbarTrackPaint.setColor(withAlpha(0xE0E0E0, alpha * 0.5f));
        canvas.drawRRect(RRect.makeXYWH(trackX, trackTop, 4f, trackH, 2f), scrollbarTrackPaint);
        scrollbarThumbPaint.setColor(withAlpha(0xBBBBBB, alpha));
        canvas.drawRRect(RRect.makeXYWH(trackX, thumbTop, 4f, thumbH, 2f), scrollbarThumbPaint);
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
}
