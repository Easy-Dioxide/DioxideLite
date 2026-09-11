package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.Config;
import com.dioxidelite.client.gui.clickgui.pages.*;
import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.SkiaRenderer;
import com.dioxidelite.client.render.skia.DioxideLiteVisuals;
import com.dioxidelite.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MINIMAL theme — dedicated compact layout, rewritten from the v1.7.2 shape for
 * full-screen friendliness: a larger adaptive card (up to ~1.15x scale on big
 * monitors), a wide content area, drag-to-scroll and a proper scrollbar.
 * Deliberately cheap to render: no framebuffer blur, plain opaque panel.
 */
public final class DioxideLiteMinimalClickGuiScreen extends SkiaScreen {

    private static final float BASE_W = 900f;
    private static final float BASE_H = 560f;
    private static final float NAV_W = 168f;
    private static final float MARGIN = 16f;
    private static final float HEADER_H = 64f;
    private static final float TAB_H = 36f;
    private static final float TAB_GAP = 2f;
    private static final float CLOSE_H = 32f;

    private final List<BasePage> pages;
    private int selectedTab = 0;
    private int hoveredTab = -1;
    private boolean closeHovered = false;
    private boolean closing = false;
    private float openProgress = 0f;
    private float closeHoverAlpha = 0f;
    private float contentScrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private boolean draggingInContent = false;
    private boolean draggingScrollbar = false;
    private float scrollbarDragOffset = 0f;
    private float cachedTotalH = 0f;
    private float cachedScrollMax = 0f;
    private boolean redrawRequested = true;

    private static final String[] TAB_KEYS_ZH = {"战斗", "视觉", "工具", "主题", "优化", "其他"};
    private static final String[] TAB_KEYS_EN = {"Combat", "Render", "Tools", "Theme", "Optimize", "Misc"};

    public DioxideLiteMinimalClickGuiScreen(Screen parent) {
        super(Component.literal("Settings"), parent);
        pages = new ArrayList<>(List.of(new CombatPage(), new RenderPage(), new ToolPage(), new ThemePage(), new OptimizePage(), new MiscPage()));
    }

    private float getScale(int width, int height) {
        float sx = (width - MARGIN) / BASE_W;
        float sy = (height - MARGIN) / BASE_H;
        return Math.max(0.60f, Math.min(1.15f, Math.min(sx, sy)));
    }

    private float cardX(int width) { return (width - BASE_W * getScale(width, this.height)) / 2f; }
    private float cardY(int height) { return (height - BASE_H * getScale(this.width, height)) / 2f; }

    private float toLayoutX(double x, int width, float scale) {
        float cx = width / 2f;
        return cx + ((float) x - cx) / scale;
    }

    private float toLayoutY(double y, int height, float scale) {
        float cy = height / 2f;
        return cy + ((float) y - cy) / scale;
    }

    private static int withAlpha(int color, float alpha) {
        return ((int) (Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | (color & 0x00FFFFFF);
    }

    private float contentTop() { return HEADER_H; }
    private float contentLeft() { return NAV_W + 1f; }
    private float contentW() { return BASE_W - NAV_W - 1f; }
    private float contentH() { return BASE_H - HEADER_H - CLOSE_H - 8f; }

    private float getContentTotalHeight(BasePage page) {
        int visible = 0;
        for (var m : page.getModules()) if (m.isVisible()) visible++;
        return 44f + page.getTotalHeight() + visible * 8f + 12f;
    }

    private void updateScrollCache(BasePage page) {
        float total = getContentTotalHeight(page);
        float area = contentH() - 44f;
        cachedTotalH = total;
        cachedScrollMax = Math.max(0f, total - area);
    }

    @Override
    protected void requestRedraw() {
        redrawRequested = true;
        SkiaRenderer.markRegionDirty();
    }

    @Override
    protected boolean needsContinuousRedraw() {
        if (closing || openProgress < 0.999f) return true;
        if (draggingInContent || draggingScrollbar) return true;
        if (Math.abs(contentScrollOffset - targetScrollOffset) > 0.35f) return true;
        if (closeHoverAlpha > 0.01f || closeHovered) return true;
        return pages.get(selectedTab).hasAnimatingModules();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        float scale = getScale(this.width, this.height);
        float cardW = BASE_W * scale;
        float cardH = BASE_H * scale;
        float bx = (this.width - cardW) / 2f;
        float by = (this.height - cardH) / 2f;

        // Cheap glass backdrop only when the user enabled the Liquid Glass visual style.
        if (Config.visualStyle == Config.VisualStyle.LIQUID_GLASS) {
            com.dioxidelite.client.render.skia.SkiaBlurRenderer.getInstance().render(
                    Minecraft.getInstance(), bx, by, cardW, cardH,
                    Config.glassRadius * scale, 0x30D9ECFF, Config.glassBlur * 0.85f);
        }

        Canvas canvas = SkiaRenderer.begin();
        if (canvas != null) {
            drawSkia(canvas, this.width, this.height, mouseX, mouseY, delta);
        }
        SkiaRenderer.end(graphics, this.width, this.height);
        redrawRequested = false;
    }

    @Override
    protected void drawSkia(Canvas canvas, int width, int height, int mouseX, int mouseY, float delta) {
        float now = System.currentTimeMillis() / 1000f;
        float dt = 0.016f;
        if (closing) {
            openProgress = Math.max(0f, openProgress - dt / 0.14f);
            if (openProgress < 0.005f) {
                super.closing();
                return;
            }
        } else {
            openProgress = Math.min(1f, openProgress + dt / 0.14f);
        }
        float animT = 1f - (1f - openProgress) * (1f - openProgress) * (1f - openProgress);
        float scale = getScale(width, height);
        float cardX = cardX(width);
        float cardY = cardY(height);
        float cardW = BASE_W * scale;
        float cardH = BASE_H * scale;
        float alpha = 1f;

        float layoutMouseX = toLayoutX(mouseX, width, scale);
        float layoutMouseY = toLayoutY(mouseY, height, scale);

        float cx = width / 2f;
        float cy = height / 2f;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        canvas.translate(-cx, -cy);

        float openScale = 0.96f + 0.04f * animT;
        canvas.save();
        canvas.translate(cardX + BASE_W / 2f, cardY + BASE_H / 2f);
        canvas.scale(openScale, openScale);
        canvas.translate(-(cardX + BASE_W / 2f), -(cardY + BASE_H / 2f));

        // Panel — opaque minimal surface.
        Paint panel = new Paint().setAntiAlias(true);
        panel.setColor(withAlpha(0x0E1116, 0.98f));
        canvas.drawRRect(RRect.makeXYWH(cardX, cardY, cardW, cardH, 14f), panel);
        DioxideLiteVisuals.outline(canvas, cardX, cardY, cardW, cardH, 14f, 0x2A3444, 0.55f, 1f);

        // Header
        FontRenderer.drawText(canvas, "DIOXIDE", cardX + 16f, cardY + 24f, 15f, DioxideLiteVisuals.text(alpha));
        FontRenderer.drawText(canvas, "LITE MIN", cardX + 16f, cardY + 38f, 8f, DioxideLiteVisuals.accent(alpha * 0.92f));

        // Nav tabs
        float navX = cardX + 10f;
        float tabStartY = cardY + HEADER_H;
        float tabW = NAV_W - 20f;
        hoveredTab = -1;
        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (TAB_H + TAB_GAP);
            boolean hover = layoutMouseX >= navX && layoutMouseX <= navX + tabW && layoutMouseY >= ty && layoutMouseY <= ty + TAB_H;
            if (hover) hoveredTab = i;
            boolean active = i == selectedTab;
            Paint bg = new Paint().setAntiAlias(true);
            if (active) {
                bg.setColor(withAlpha(DioxideLiteVisuals.CYAN, 0.16f));
                canvas.drawRRect(RRect.makeXYWH(navX, ty, tabW, TAB_H, 6f), bg);
            } else if (hover) {
                bg.setColor(withAlpha(0xB9DFFF, 0.06f));
                canvas.drawRRect(RRect.makeXYWH(navX, ty, tabW, TAB_H, 6f), bg);
            }
            int textColor = active ? DioxideLiteVisuals.text(alpha) : DioxideLiteVisuals.muted(alpha * 0.88f);
            FontRenderer.drawText(canvas, UiText.t(TAB_KEYS_ZH[i], TAB_KEYS_EN[i]), navX + 12f, ty + TAB_H / 2f + 5f, 12f, textColor);
            if (active) {
                DioxideLiteVisuals.accentLine(canvas, navX + 12f, ty + TAB_H - 1f, 24f, alpha * 0.75f);
            }
        }

        // Divider
        canvas.drawRect(Rect.makeXYWH(cardX + NAV_W, cardY + 10f, 1f, cardH - 20f), new Paint().setColor(withAlpha(0xFFFFFF, 0.08f)));

        // Content area
        BasePage page = pages.get(selectedTab);
        updateScrollCache(page);
        targetScrollOffset = Math.min(targetScrollOffset, cachedScrollMax);
        contentScrollOffset += (targetScrollOffset - contentScrollOffset) * Math.min(1f, dt * 18f);

        float contentX = cardX + contentLeft();
        float contentY = cardY + contentTop();
        float contentW = contentW();
        float contentH = contentH();

        FontRenderer.drawText(canvas, page.getTitle(), contentX + 14f, contentY + 22f, 17f, DioxideLiteVisuals.text(alpha));
        FontRenderer.drawText(canvas, page.getSubtitle(), contentX + 14f, contentY + 37f, 9f, DioxideLiteVisuals.muted(alpha * 0.85f));
        DioxideLiteVisuals.accentLine(canvas, contentX + 14f, contentY + 44f, 36f, alpha * 0.7f);

        float clipTop = contentY + 50f;
        float clipBottom = contentY + contentH;
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(contentX, clipTop, contentW, clipBottom - clipTop));

        float moduleStartY = contentY + 50f;
        page.draw(canvas, contentX + 12f, moduleStartY, contentW - 42f, contentH - 50f, alpha, contentScrollOffset);

        // Scrollbar
        if (cachedTotalH > contentH - 50f) {
            float trackX = contentX + contentW - 8f;
            float trackTop = contentY + 56f;
            float trackH = contentH - 56f - 6f;
            float thumbH = Math.max(20f, trackH * (contentH - 50f) / cachedTotalH);
            float progress = Math.min(1f, contentScrollOffset / Math.max(1f, cachedScrollMax));
            float thumbTop = trackTop + (trackH - thumbH) * progress;
            Paint trackPaint = new Paint().setColor(withAlpha(0xE0E0E0, 0.5f));
            canvas.drawRRect(RRect.makeXYWH(trackX, trackTop, 4f, trackH, 2f), trackPaint);
            Paint thumbPaint = new Paint().setColor(withAlpha(0xBBBBBB, 1f));
            canvas.drawRRect(RRect.makeXYWH(trackX, thumbTop, 4f, thumbH, 2f), thumbPaint);
        }
        canvas.restore();

        // Close button
        float closeX = cardX + contentLeft();
        float closeY = cardY + BASE_H - CLOSE_H - 6f;
        boolean closeHover = layoutMouseX >= closeX && layoutMouseX <= closeX + contentW() && layoutMouseY >= closeY && layoutMouseY <= closeY + CLOSE_H;
        closeHovered = closeHover;
        closeHoverAlpha += ((closeHover ? 1f : 0f) - closeHoverAlpha) * Math.min(1f, dt * 12f);
        Paint closeBg = new Paint().setAntiAlias(true);
        closeBg.setColor(withAlpha(0x11151B, 0.85f + closeHoverAlpha * 0.1f));
        canvas.drawRRect(RRect.makeXYWH(closeX, closeY, contentW(), CLOSE_H, 7f), closeBg);
        DioxideLiteVisuals.outline(canvas, closeX, closeY, contentW(), CLOSE_H, 7f, closeHover ? 0xD5ECFF : 0x6E7F93, 0.2f + closeHoverAlpha * 0.2f, 0.7f);
        String closeText = UiText.t("× 关闭", "× Close");
        float cw = FontRenderer.measureTextWidth(closeText, 12f);
        FontRenderer.drawText(canvas, closeText, closeX + (contentW() - cw) / 2f, closeY + 21f, 12f, DioxideLiteVisuals.muted(alpha * (0.9f + closeHoverAlpha * 0.1f)));

        canvas.restore();
        canvas.restore();

        // TEMP FPS probe: visible inside the ClickGUI like the F3 overlay.
        String fpsLine = String.format("FPS: %d", Minecraft.getInstance().getFps());
        float fpsW = FontRenderer.measureTextWidth(fpsLine, 12f);
        FontRenderer.drawText(canvas, fpsLine, cardX + cardW - fpsW - 14f, cardY + 20f, 12f, withAlpha(0xFFE0FF, 1f));
    }

    private boolean inScrollbar(float mx, float my, float contentX, float contentY, float contentW, float contentH) {
        float x = contentX + contentW - 8f;
        return mx >= x - 6f && mx <= x + 10f && my >= contentY + 56f && my <= contentY + contentH;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        requestRedraw();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (closing) return false;
        int button = event.button();
        float scale = getScale(this.width, this.height);
        float mx = toLayoutX(event.x(), this.width, scale);
        float my = toLayoutY(event.y(), this.height, scale);
        float cardX = cardX(this.width);
        float cardY = cardY(this.height);
        float navX = cardX + 10f;
        float tabStartY = cardY + HEADER_H;
        float tabW = NAV_W - 20f;
        BasePage page = pages.get(selectedTab);
        float contentX = cardX + contentLeft();
        float contentY = cardY + contentTop();
        float contentW = contentW();
        float contentH = contentH();

        for (int i = 0; i < TAB_KEYS_ZH.length; i++) {
            float ty = tabStartY + i * (TAB_H + TAB_GAP);
            if (button == 0 && mx >= navX && mx <= navX + tabW && my >= ty && my <= ty + TAB_H) {
                if (selectedTab != i) { selectedTab = i; targetScrollOffset = 0f; contentScrollOffset = 0f; }
                requestRedraw();
                return true;
            }
        }

        float closeY = cardY + BASE_H - CLOSE_H - 6f;
        if (button == 0 && mx >= contentX && mx <= contentX + contentW && my >= closeY && my <= closeY + CLOSE_H) {
            closing = true;
            requestRedraw();
            return true;
        }

        if (mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH) {
            if (inScrollbar(mx, my, contentX, contentY, contentW, contentH)) {
                draggingScrollbar = true;
                float trackTop = contentY + 56f;
                float trackH = contentH - 56f - 6f;
                float thumbH = Math.max(20f, trackH * (contentH - 50f) / cachedTotalH);
                scrollbarDragOffset = (my >= trackTop && my <= trackTop + thumbH) ? my - trackTop : thumbH * 0.5f;
                float available = Math.max(1f, trackH - thumbH);
                float thumbTop = Math.max(trackTop, Math.min(my - scrollbarDragOffset, trackTop + available));
                targetScrollOffset = cachedScrollMax * ((thumbTop - trackTop) / available);
                contentScrollOffset = targetScrollOffset;
                requestRedraw();
                return true;
            }
            boolean hit = page.onClick(mx, my, contentX + 12f, contentY + 50f, contentW - 42f, contentScrollOffset, button);
            if (hit) draggingInContent = true;
            requestRedraw();
            return hit;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        float scale = getScale(this.width, this.height);
        float my = toLayoutY(event.y(), this.height, scale);
        float mx = toLayoutX(event.x(), this.width, scale);
        if (draggingScrollbar) {
            float contentX = cardX(this.width) + contentLeft();
            float contentY = cardY(this.height) + contentTop();
            float contentH = contentH();
            float trackTop = contentY + 56f;
            float trackH = contentH - 56f - 6f;
            float thumbH = Math.max(20f, trackH * (contentH - 50f) / cachedTotalH);
            float available = Math.max(1f, trackH - thumbH);
            float thumbTop = Math.max(trackTop, Math.min(my - scrollbarDragOffset, trackTop + available));
            targetScrollOffset = cachedScrollMax * ((thumbTop - trackTop) / available);
            contentScrollOffset = targetScrollOffset;
            requestRedraw();
            return true;
        }
        if (draggingInContent) {
            pages.get(selectedTab).onDrag(mx, my, cardX(this.width) + contentLeft() + 12f, cardY(this.height) + contentTop() + 50f, contentW() - 42f, contentScrollOffset);
            requestRedraw();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingInContent = false;
        draggingScrollbar = false;
        pages.get(selectedTab).releaseDrag();
        requestRedraw();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        float scale = getScale(this.width, this.height);
        float layoutMx = toLayoutX(mx, this.width, scale);
        float layoutMy = toLayoutY(my, this.height, scale);
        float contentX = cardX(this.width) + contentLeft();
        float contentY = cardY(this.height) + contentTop();
        if (layoutMx >= contentX && layoutMx <= contentX + contentW() && layoutMy >= contentY && layoutMy <= contentY + contentH()) {
            updateScrollCache(pages.get(selectedTab));
            targetScrollOffset = Math.max(0f, Math.min(cachedScrollMax, targetScrollOffset + (float) (-vScroll * 16f)));
            requestRedraw();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        closing = true;
    }
}
