package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.SkiaScreen;
// [v1.8 ADDITION] brand mark renderer
import com.dioxidelite.client.render.skia.SignatureLogo;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Fast Signature theme. Uses Minecraft's immediate GUI renderer only: no external rasterizer,
 * no off-screen framebuffer, no blur capture and no CPU texture upload.
 */
public final class DioxideLiteSignatureClickGuiScreen extends SkiaScreen {
    private static final float W = 920f;
    private static final float H = 560f;
    private static final float MIN_SCALE = 0.58f;
    private static final float RING_RADIUS = 92f;
    private static final float BUBBLE_RADIUS = 27f;
    private static final float PANEL_W = 390f;
    private static final float PANEL_H = 430f;
    private static final float PANEL_GAP = 16f;
    private static final float TRANSITION_SPEED = 10.5f;

    private static final int BACKDROP = argb(185, 2, 6, 8);
    private static final int SURFACE = argb(244, 12, 17, 19);
    private static final int SURFACE_ALT = argb(235, 18, 25, 27);
    private static final int ROW = argb(80, 255, 255, 255);
    private static final int ROW_HOVER = argb(30, 62, 214, 180);
    private static final int BORDER = argb(180, 53, 65, 67);
    private static final int BORDER_HOVER = argb(225, 73, 88, 90);
    private static final int TEXT = 0xFFF1F6F4;
    private static final int MUTED = 0xFFA6B2AE;
    private static final int FAINT = 0xFF687571;
    private static final int ACCENT = 0xFF3ED6B4;

    // [v1.8 ADDITION] Centre lockup geometry. The mark and the two text lines are stacked
    // around the ring centre so the whole lockup is vertically balanced on cy.
    private static final float CENTRE_MARK_HALF = 15f;   // mark half extent in logical px
    private static final float CENTRE_MARK_DY = -16f;    // mark centre offset from cy
    private static final float CENTRE_BRAND_DY = 17f;    // brand baseline offset from cy
    private static final float CENTRE_STATUS_DY = 31f;   // status baseline offset from cy

    private static final String[] NAV = {"Combat", "Render", "Tools", "Theme", "Optimize", "Misc"};

    private final Screen parent;
    private final List<BasePage> pages = new ArrayList<>();
    private int selected = -1;
    private float open = 0f;
    private float intro = 0f;
    private float scroll = 0f;
    private float targetScroll = 0f;
    private long lastNs = System.nanoTime();
    private boolean dragging;
    private float dragStartY;
    private float dragStartScroll;

    // [v1.8 ADDITION] Redraw gating state. See needsContinuousRedraw().
    private boolean animating = true;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;

    public DioxideLiteSignatureClickGuiScreen(Screen parent) {
        super(FontRenderer.component("DioxideLite Signature"));
        this.parent = parent;
        pages.add(new CombatPage());
        pages.add(new RenderPage());
        pages.add(new ToolPage());
        pages.add(new ThemePage());
        pages.add(new OptimizePage());
        pages.add(new MiscPage());
    }

    @Override protected void init() {
        selected = -1;
        open = 0f;
        intro = 0f;
        scroll = targetScroll = 0f;
        dragging = false;
        lastNs = System.nanoTime();
    }

    /**
     * [v1.8 CHANGED] Originally: return true;
     *
     * That rebuilt a full-window Skija frame on every frame, even when nothing on screen was
     * moving. Each rebuild costs a full-screen CPU raster plus a full-frame CPU to GPU texture
     * upload (see SkiaRenderer), which is why an open ClickGUI was the most expensive state.
     *
     * The screen now redraws only while something is actually animating, plus one extra frame
     * after the pointer moves. When the screen is visually static, SkiaScreen falls back to
     * SkiaRenderer.drawCached(), which is a single cached blit.
     *
     * Revert: put "return true;" back here. No other file depends on this.
     */
    @Override protected boolean needsContinuousRedraw() {
        return animating;
    }

    /**
     * [v1.8 ADDITION] Pointer movement is the one input not followed by a click or a scroll,
     * so it has to request its own redraw; otherwise hover states would freeze.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            requestRedraw();
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override protected void drawSkia(Canvas c, int width, int height, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(.05f, Math.max(.001f, (now - lastNs) / 1_000_000_000f));
        lastNs = now;
        intro = approach(intro, 1f, 7f, dt);
        open = approach(open, selected >= 0 ? 1f : 0f, 10f, dt);
        scroll = approach(scroll, targetScroll, 16f, dt);

        pointerX = logicalX(mouseX, width);
        pointerY = logicalY(mouseY, height);

        BasePage page = selected >= 0 ? pages.get(selected) : null;
        if (page != null) page.update(dt);

        float scale = Math.max(MIN_SCALE, Math.min(1f, Math.min(width / W, height / H)));
        float ox = width / 2f, oy = height / 2f;
        float alpha = ease(intro);

        c.drawColor(withAlpha(BACKDROP, alpha));
        c.save();
        c.translate(ox, oy);
        c.scale(scale, scale);
        c.translate(-ox, -oy);

        drawAmbient(c, width, height, alpha);
        drawCenter(c, width, height, alpha);
        drawCategories(c, width, height, alpha);
        if (selected >= 0 && categoryProgress > .015f) drawCategoryPanel(c, width, height, alpha);
        c.restore();

        // [v1.8 ADDITION] Decide whether the next frame needs a redraw at all. Every term is a
        // value that has to converge before the picture stops changing.
        animating = intro < 0.999f
                || Math.abs(categoryProgress - (selected >= 0 ? 1f : 0f)) > 0.002f
                || Math.abs(scroll - targetScroll) > 0.05f
                || anySettling(bubbleHover)
                || anySettling(moduleHover);
    }

    /** True while any hover value is still between its two end states. */
    private static boolean anySettling(Map<?, Float> values) {
        for (Float v : values.values()) {
            if (v != null && v > 0.004f && v < 0.996f) return true;
        }
        return false;
    }

    private void drawAmbient(GuiGraphics g, int mx, int my, float a) {
        int cx = width / 2, cy = height / 2;
        int aa = Math.round(255 * .06f * a);
        g.renderOutline(Math.max(20, cx - 190), Math.max(20, cy - 190), Math.min(width - 40, 380), Math.min(height - 40, 380), (aa << 24) | ACCENT);
        g.fill(24, cy, width - 24, cy + 1, (Math.round(255 * .025f * a) << 24) | 0xFFFFFF);
        g.fill(cx, 18, cx + 1, height - 18, (Math.round(255 * .025f * a) << 24) | 0xFFFFFF);
        int scanY = 20 + (int)(((System.nanoTime() / 1_000_000_000d) % 3.0) / 3.0 * Math.max(1, height - 40));
        g.fill(24, scanY, width - 24, scanY + 1, (Math.round(255 * .06f * a) << 24) | ACCENT);
        g.drawString(font, FontRenderer.component("DIOXIDELITE"), 22, 20, (Math.round(255 * a) << 24) | TEXT, false);
        g.drawString(font, FontRenderer.component("SIGNATURE UI"), 22, 33, (Math.round(255 * .45f * a) << 24) | ACCENT, false);
        g.drawString(font, FontRenderer.component("RIGHT SHIFT"), width - 92, height - 18, (Math.round(255 * .35f * a) << 24) | FAINT, false);
    }

    /**
     * Centre of the radial launcher.
     *
     * [v1.8 CHANGED] Two problems were fixed here.
     *
     * 1. Off-centre text. The previous implementation centred every string with a
     *    hand-tuned pixel offset. Those constants are only correct for one string at one
     *    size, so "DioxideLite" sat slightly left of the ring axis and the status line
     *    moved sideways as the selected category changed ("COMBAT" vs "OPTIMIZE" have very
     *    different widths). Both lines now use FontRenderer.drawTextCentered(), which
     *    measures the actual string.
     *
     * 2. Missing brand mark. The old centre drew a 5px pulsing accent dot directly on top
     *    of the brand text. That dot is replaced by the DioxideLite mark, centred exactly
     *    on the ring axis, with a configurable glow and opacity.
     */
    private void drawCenter(Canvas c, int width, int height, float alpha) {
        float cx = width / 2f, cy = height / 2f;
        float collapse = ease(categoryProgress);
        float radius = RING_RADIUS * (1f - .22f * collapse);
        Paint ring = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1f);
        ring.setColor(withAlpha(ACCENT, .58f * alpha * (1f - collapse) + .22f));
        c.drawCircle(cx, cy, radius, ring);

        // ---------------------------------------------------------------------------
        // [v1.8 REMOVED] Centre pulse dot. It overlapped the brand text, and the mark now
        // occupies the centre. Original code, kept for reference:
        //
        //     ring.setStrokeWidth(2f).setColor(withAlpha(ACCENT, .95f * alpha));
        //     float pulse = (float)Math.sin(System.nanoTime() / 360_000_000.0) * 2f;
        //     c.drawCircle(cx, cy, 5f + pulse, ring);
        // ---------------------------------------------------------------------------

        // [v1.8 ADDED] Brand mark, centred on the ring axis.
        // Size, glow strength and transparency come from Config.signatureLogo* (Theme page).
        SignatureLogo.draw(c, cx, cy + CENTRE_MARK_DY, CENTRE_MARK_HALF, alpha);

        // ---------------------------------------------------------------------------
        // [v1.8 REPLACED] Hardcoded horizontal offsets -> measured centring.
        // Original code, kept for reference:
        //
        //     FontRenderer.drawText(c, "DioxideLite", cx - 37f, cy - 7f, 10f, withAlpha(TEXT, alpha));
        //     FontRenderer.drawText(c, selected < 0 ? "SELECT A CATEGORY" : NAV[selected].toUpperCase(),
        //             cx - (selected < 0 ? 48f : 22f), cy + 18f, 7f, withAlpha(MUTED, alpha));
        // ---------------------------------------------------------------------------
        FontRenderer.drawTextCentered(c, "DioxideLite", cx, cy + CENTRE_BRAND_DY, 10f,
                withAlpha(TEXT, alpha));
        String status = selected < 0 ? "SELECT A CATEGORY" : NAV[selected].toUpperCase();
        FontRenderer.drawTextCentered(c, status, cx, cy + CENTRE_STATUS_DY, 7f,
                withAlpha(MUTED, alpha));

        ring.close();
    }

    private void drawCategories(Canvas c, int width, int height, float alpha) {
        float cx = width / 2f, cy = height / 2f;
        float collapse = ease(categoryProgress);
        for (int i = 0; i < NAV.length; i++) {
            double a = -Math.PI / 2d + i * (Math.PI * 2d / NAV.length);
            float distance = RING_RADIUS * (1f - .68f * collapse);
            float x = cx + (float)Math.cos(a) * distance;
            float y = cy + (float)Math.sin(a) * distance;
            float hoverTarget = selected < 0 && distance(pointerX, pointerY, x, y) < BUBBLE_RADIUS + 8f ? 1f : 0f;
            float hv = approach(bubbleHover.getOrDefault(i, 0f), hoverTarget, 12f, .016f);
            bubbleHover.put(i, hv);
            float r = BUBBLE_RADIUS + hv * 4f;
            Paint fill = new Paint().setAntiAlias(true);
            fill.setColor(withAlpha(SURFACE, alpha * (.92f + .08f * hv)));
            c.drawCircle(x, y, r, fill);
            fill.setMode(PaintMode.STROKE).setStrokeWidth(1f).setColor(withAlpha(hv > .5f ? BORDER_HOVER : BORDER, alpha));
            c.drawCircle(x, y, r, fill);
            // [v1.8 CHANGED] was already measured-centred; routed through the shared helper
            // so every centre label uses the same code path.
            FontRenderer.drawTextCentered(c, NAV[i], x, y + 3f, 8f,
                    withAlpha(hv > .5f ? TEXT : MUTED, alpha));
            fill.close();
        }
        g.drawString(font, FontRenderer.component("ESC  CLOSE"), 24, height - 20, (Math.round(255 * .45f * a) << 24) | FAINT, false);
    }

    private void drawWorkspace(GuiGraphics g, int mx, int my, float a, float p, float dt) {
        int margin = Math.max(24, (width - 1040) / 2);
        int top = Math.max(22, (int)(height * .08));
        int h = height - top - Math.max(26, (int)(height * .07));
        int railW = 172;
        int gap = 12;
        int panelX = margin + railW + gap;
        int panelW = width - panelX - margin;
        int alpha = Math.round(255 * ease(p) * a);

        g.fill(margin, top, margin + railW, top + h, (Math.round(255 * .88f * a) << 24) | (PANEL & 0xFFFFFF));
        g.fill(panelX, top, panelX + panelW, top + h, (Math.round(255 * .82f * a) << 24) | (PANEL_2 & 0xFFFFFF));
        g.renderOutline(margin, top, railW, h, (Math.round(255 * .22f * a) << 24) | 0x71807C);
        g.renderOutline(panelX, top, panelW, h, (Math.round(255 * .18f * a) << 24) | 0x71807C);

        g.drawString(font, FontRenderer.component("DioxideLite"), margin + 18, top + 18, (alpha << 24) | TEXT, false);
        g.drawString(font, FontRenderer.component("SIGNATURE"), margin + 18, top + 32, (alpha << 24) | ACCENT, false);
        for (int i = 0; i < NAV.length; i++) {
            int y = top + 58 + i * 43;
            boolean active = i == selected;
            boolean hover = mx >= margin + 10 && mx <= margin + railW - 10 && my >= y && my <= y + 34;
            if (active || hover) {
                g.fill(margin + 10, y, margin + railW - 10, y + 34,
                        (Math.round(255 * (active ? .14f : .07f) * a) << 24) | (active ? ACCENT_DIM : 0xFFFFFF));
                g.fill(margin + 10, y, margin + 12, y + 34, (alpha << 24) | ACCENT);
            }
            g.drawString(font, FontRenderer.component(String.format("%02d", i + 1)), margin + 19, y + 9,
                    (Math.round(255 * .55f * a) << 24) | (active ? ACCENT : FAINT), false);
            g.drawString(font, FontRenderer.component(NAV[i]), margin + 44, y + 9,
                    (Math.round(255 * (active ? 1f : .68f) * a) << 24) | (active ? TEXT : MUTED), false);
        }
        g.drawString(font, FontRenderer.component("ESC"), margin + 18, top + h - 18, (Math.round(255 * .5f * a) << 24) | FAINT, false);
        g.drawString(font, FontRenderer.component("BACK"), margin + 44, top + h - 18, (Math.round(255 * .75f * a) << 24) | MUTED, false);

        BasePage page = pages.get(selected);
        page.update(dt);
        g.drawString(font, FontRenderer.component(NAV[selected]), panelX + 20, top + 18, (alpha << 24) | TEXT, false);
        g.drawString(font, FontRenderer.component(page.getSubtitle()), panelX + 20, top + 33, (Math.round(255 * .6f * a) << 24) | MUTED, false);
        g.drawString(font, FontRenderer.component(String.format("%02d / %02d", selected + 1, NAV.length)), panelX + panelW - 58, top + 18, (Math.round(255 * .42f * a) << 24) | FAINT, false);
        g.fill(panelX + 20, top + 47, panelX + panelW - 20, top + 48, (Math.round(255 * .20f * a) << 24) | ACCENT);

        int contentX = panelX + 18;
        int contentY = top + 60;
        int contentW = panelW - 36;
        int contentH = h - 76;
        g.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        page.drawFast(g, contentX, contentY, contentW, contentH, alpha, scroll, mx, my);
        g.disableScissor();

        float total = page.getTotalHeight() + 8f;
        if (total > contentH) {
            float max = total - contentH;
            int thumbH = Math.max(24, (int)(contentH * contentH / total));
            int thumbY = contentY + (int)(Math.max(0, Math.min(max, scroll)) / max * (contentH - thumbH));
            g.fill(panelX + panelW - 5, thumbY, panelX + panelW - 3, thumbY + thumbH, (Math.round(255 * .5f * a) << 24) | ACCENT);
        }
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return false;
        float mx = (float)event.x(), my = (float)event.y();
        if (selected < 0) {
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                int cx = width / 2, cy = height / 2;
                int r = Math.min(116, Math.min(width, height) / 4);
                for (int i = 0; i < NAV.length; i++) {
                    double ang = -Math.PI / 2 + i * Math.PI * 2 / NAV.length;
                    int x = cx + (int)(Math.cos(ang) * r), y = cy + (int)(Math.sin(ang) * r);
                    if (dist(mx, my, x, y) < 34) { selected = i; scroll = targetScroll = 0; return true; }
                }
            }
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { onClose(); return true; }
            return true;
        }

        int margin = Math.max(24, (width - 1040) / 2), top = Math.max(22, (int)(height * .08));
        int h = height - top - Math.max(26, (int)(height * .07));
        int railW = 172, panelX = margin + railW + 12, panelW = width - panelX - margin;
        for (int i = 0; i < NAV.length; i++) {
            int y = top + 58 + i * 43;
            if (mx >= margin + 10 && mx <= margin + railW - 10 && my >= y && my <= y + 34) { selected = i; scroll = targetScroll = 0; return true; }
        }
        if (mx >= panelX + 18 && mx <= panelX + panelW - 18 && my >= top + 60 && my <= top + h - 16) {
            int contentX = panelX + 18, contentY = top + 60, contentW = panelW - 36;
            BasePage page = pages.get(selected);
            // Single source of truth for hit-testing, including right-click expansion.
            if (page.onClick(mx, my, contentX, contentY, contentW, scroll, event.button())) return true;
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                dragging = true;
                dragStartY = my;
                dragStartScroll = scroll;
            }
            return true;
        }
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (selected >= 0) {
            int margin = Math.max(24, (width - 1040) / 2), top = Math.max(22, (int)(height * .08));
            int h = height - top - Math.max(26, (int)(height * .07));
            int panelX = margin + 172 + 12, panelW = width - panelX - margin;
            int contentX = panelX + 18, contentY = top + 60, contentW = panelW - 36, contentH = h - 76;
            if (pages.get(selected).onDrag((float)event.x(), (float)event.y(), contentX, contentY, contentW, scroll)) return true;
        }
        if (!dragging) return false;
        targetScroll = Math.max(0f, dragStartScroll + (dragStartY - (float)event.y()));
        float max = Math.max(0f, pages.get(selected).getTotalHeight() + 8f - (height - Math.max(22, (int)(height * .08)) - Math.max(26, (int)(height * .07)) - 76));
        targetScroll = Math.min(targetScroll, max);
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) { dragging = false; if (selected >= 0) pages.get(selected).releaseDrag(); return true; }
        return false;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selected < 0) return false;
        float contentH = height - Math.max(22, (int)(height * .08)) - Math.max(26, (int)(height * .07)) - 76;
        float max = Math.max(0f, pages.get(selected).getTotalHeight() + 8f - contentH);
        targetScroll = Math.max(0f, Math.min(max, targetScroll - (float)verticalAmount * 34f));
        return true;
    }

    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { if (selected >= 0) { selected = -1; return true; } onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private static float approach(float current, float target, float speed, float dt) {
        return current + (target - current) * Math.min(1f, speed * dt);
    }
    private static float ease(float x) { x = Math.max(0f, Math.min(1f, x)); return 1f - (float)Math.pow(1f - x, 3); }
    private static float dist(float x1, float y1, float x2, float y2) { return (float)Math.hypot(x1 - x2, y1 - y2); }
}
