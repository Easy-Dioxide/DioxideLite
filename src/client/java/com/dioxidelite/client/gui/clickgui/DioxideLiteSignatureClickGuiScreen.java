package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.Config;
import com.dioxidelite.client.gui.clickgui.pages.*;
import com.dioxidelite.client.gui.clickgui.widget.SettingModule;
import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * DioxideLite's independent Signature ClickGUI theme.
 *
 * The presentation is an original implementation inspired by the supplied reference:
 * a radial category launcher, staged panel transitions, compact dark surfaces and
 * restrained teal accents. It deliberately reuses DioxideLite's own page/settings model.
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

    private static final String[] NAV = {"Combat", "Render", "Tools", "Theme", "Optimize", "Misc"};
    private final List<BasePage> pages = new ArrayList<>();
    private final Map<Integer, Float> bubbleHover = new java.util.HashMap<>();
    private final Map<SettingModule, Float> moduleHover = new IdentityHashMap<>();

    private final Screen parent;
    private int selected = -1;
    private float intro = 0f;
    private float categoryProgress = 0f;
    private float scroll = 0f;
    private float targetScroll = 0f;
    private float pointerX;
    private float pointerY;
    private float dragStartY;
    private float dragStartScroll;
    private boolean dragging;
    private long lastFrame = System.nanoTime();

    public DioxideLiteSignatureClickGuiScreen(Screen parent) {
        super(Component.literal("DioxideLite"), parent);
        this.parent = parent;
        pages.add(new CombatPage());
        pages.add(new RenderPage());
        pages.add(new ToolPage());
        pages.add(new ThemePage());
        pages.add(new OptimizePage());
        pages.add(new MiscPage());
        for (int i = 0; i < NAV.length; i++) bubbleHover.put(i, 0f);
    }

    @Override protected void init() {
        intro = 0f;
        categoryProgress = 0f;
        scroll = 0f;
        targetScroll = 0f;
        selected = -1;
        dragging = false;
        lastFrame = System.nanoTime();
    }

    @Override protected boolean needsContinuousRedraw() { return true; }

    @Override protected void drawSkia(Canvas c, int width, int height, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(.05f, Math.max(.001f, (now - lastFrame) / 1_000_000_000f));
        lastFrame = now;
        intro = approach(intro, 1f, 8.5f, dt);
        float targetCategory = selected >= 0 ? 1f : 0f;
        categoryProgress = approach(categoryProgress, targetCategory, TRANSITION_SPEED, dt);
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
    }

    private void drawAmbient(Canvas c, int width, int height, float alpha) {
        Paint p = new Paint().setAntiAlias(true);
        p.setMode(PaintMode.STROKE).setStrokeWidth(1f).setColor(withAlpha(0x3ED6B4, .035f * alpha));
        float cx = width / 2f, cy = height / 2f;
        c.drawCircle(cx, cy, RING_RADIUS * 2.35f, p);
        c.drawCircle(cx, cy, RING_RADIUS * 3.15f, p);
        p.close();
    }

    private void drawCenter(Canvas c, int width, int height, float alpha) {
        float cx = width / 2f, cy = height / 2f;
        float collapse = ease(categoryProgress);
        float radius = RING_RADIUS * (1f - .22f * collapse);
        Paint ring = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1f);
        ring.setColor(withAlpha(ACCENT, .58f * alpha * (1f - collapse) + .22f));
        c.drawCircle(cx, cy, radius, ring);
        ring.setStrokeWidth(2f).setColor(withAlpha(ACCENT, .95f * alpha));
        float pulse = (float)Math.sin(System.nanoTime() / 360_000_000.0) * 2f;
        c.drawCircle(cx, cy, 5f + pulse, ring);
        FontRenderer.drawText(c, "DioxideLite", cx - 37f, cy - 7f, 10f, withAlpha(TEXT, alpha));
        FontRenderer.drawText(c, selected < 0 ? "SELECT A CATEGORY" : NAV[selected].toUpperCase(), cx - (selected < 0 ? 48f : 22f), cy + 18f, 7f, withAlpha(MUTED, alpha));
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
            FontRenderer.drawText(c, NAV[i], x - FontRenderer.measureTextWidth(NAV[i], 8f) / 2f, y + 3f, 8f, withAlpha(hv > .5f ? TEXT : MUTED, alpha));
            fill.close();
        }
    }

    private void drawCategoryPanel(Canvas c, int width, int height, float alpha) {
        float t = ease(categoryProgress);
        float slide = (1f - t) * 34f;
        float centerX = width / 2f;
        float leftX = centerX - PANEL_GAP / 2f - PANEL_W - slide;
        float rightX = centerX + PANEL_GAP / 2f + slide;
        float y = height / 2f - PANEL_H / 2f;
        drawPanel(c, leftX, y, PANEL_W, PANEL_H, alpha * t, true);
        drawPanel(c, rightX, y, PANEL_W, PANEL_H, alpha * t, false);

        BasePage page = pages.get(selected);
        float contentX = rightX + 18f;
        float contentY = y + 64f;
        float contentW = PANEL_W - 36f;
        float contentH = PANEL_H - 82f;
        c.save();
        c.clipRect(Rect.makeXYWH(contentX, contentY, contentW, contentH));
        page.draw(c, contentX, contentY, contentW, contentH, alpha * t, scroll);
        c.restore();

        float total = page.getTotalHeight() + 18f;
        if (total > contentH) {
            float max = total - contentH;
            float thumbH = Math.max(24f, contentH * contentH / total);
            float thumbY = contentY + (scroll / Math.max(1f, max)) * (contentH - thumbH);
            rounded(c, rightX + PANEL_W - 9f, thumbY, 2f, thumbH, 1f, withAlpha(ACCENT, .58f * alpha * t));
        }
    }

    private void drawPanel(Canvas c, float x, float y, float w, float h, float alpha, boolean left) {
        rounded(c, x, y, w, h, 8f, withAlpha(SURFACE, alpha));
        Paint border = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1f);
        border.setColor(withAlpha(BORDER, alpha));
        c.drawRRect(RRect.makeXYWH(x, y, w, h, 8f), border);
        border.close();
        if (left) {
            FontRenderer.drawText(c, NAV[selected], x + 18f, y + 29f, 18f, withAlpha(TEXT, alpha));
            FontRenderer.drawText(c, "MODULES", x + 18f, y + 47f, 7f, withAlpha(ACCENT, alpha));
            FontRenderer.drawText(c, "DioxideLite Signature", x + 18f, y + h - 18f, 7f, withAlpha(FAINT, alpha));
            drawModuleRail(c, x + 14f, y + 68f, w - 28f, h - 104f, alpha);
        } else {
            FontRenderer.drawText(c, "SETTINGS", x + 18f, y + 29f, 8f, withAlpha(ACCENT, alpha));
            FontRenderer.drawText(c, "← BACK", x + w - 55f, y + 29f, 7f, withAlpha(MUTED, alpha));
        }
    }

    private void drawModuleRail(Canvas c, float x, float y, float w, float h, float alpha) {
        BasePage page = pages.get(selected);
        float cy = y - scroll;
        for (SettingModule module : page.getModules()) {
            if (!module.isVisible()) continue;
            float mh = module.getTotalHeight();
            if (cy + mh > y && cy < y + h) {
                float hovered = pointIn(pointerX, pointerY, x, cy, w, Math.min(mh, 52f)) ? 1f : 0f;
                float old = moduleHover.getOrDefault(module, 0f);
                float hv = approach(old, hovered, 12f, .016f);
                moduleHover.put(module, hv);
                rounded(c, x, cy, w, Math.min(48f, mh), 5f, withAlpha(hv > .5f ? ROW_HOVER : ROW, alpha));
                FontRenderer.drawText(c, module.title, x + 12f, cy + 20f, 10f, withAlpha(TEXT, alpha));
                if (module.subtitle != null) FontRenderer.drawText(c, module.subtitle, x + 12f, cy + 34f, 6.5f, withAlpha(MUTED, alpha * .82f));
            }
            cy += mh + 8f;
        }
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        float scale = Math.max(MIN_SCALE, Math.min(1f, Math.min(width / W, height / H)));
        float lx = logicalX(event.x(), width), ly = logicalY(event.y(), height);
        float cx = width / 2f, cy = height / 2f;
        if (selected < 0) {
            for (int i = 0; i < NAV.length; i++) {
                double a = -Math.PI / 2d + i * (Math.PI * 2d / NAV.length);
                float x = cx + (float)Math.cos(a) * RING_RADIUS;
                float y = cy + (float)Math.sin(a) * RING_RADIUS;
                if (distance(lx, ly, x, y) <= BUBBLE_RADIUS + 7f) {
                    selected = i;
                    scroll = targetScroll = 0f;
                    return true;
                }
            }
            return true;
        }

        float t = ease(categoryProgress);
        float slide = (1f - t) * 34f;
        float rightX = cx + PANEL_GAP / 2f + slide;
        float panelY = cy - PANEL_H / 2f;
        float contentX = rightX + 18f;
        float contentY = panelY + 64f;
        float contentW = PANEL_W - 36f;
        float contentH = PANEL_H - 82f;
        if (pointIn(lx, ly, contentX, contentY, contentW, contentH)) {
            if (pages.get(selected).onClick(lx, ly, contentX, contentY, contentW, scroll, event.button())) return true;
            dragging = true;
            dragStartY = ly;
            dragStartScroll = scroll;
            return true;
        }
        // Clicking the settings header/back affordance returns to the radial launcher.
        if (pointIn(lx, ly, rightX + PANEL_W - 78f, panelY + 10f, 66f, 28f)) {
            selected = -1;
            return true;
        }
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || selected < 0) return false;
        float ly = logicalY(event.y(), height);
        float contentH = PANEL_H - 82f;
        float max = Math.max(0f, pages.get(selected).getTotalHeight() + 18f - contentH);
        targetScroll = clamp(dragStartScroll + (dragStartY - ly), 0f, max);
        pages.get(selected).onDrag(logicalX(event.x(), width), ly, width / 2f + PANEL_GAP / 2f + 18f, height / 2f - PANEL_H / 2f + 64f, PANEL_W - 36f, scroll);
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
            if (selected >= 0) pages.get(selected).releaseDrag();
            return true;
        }
        return false;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selected < 0) return true;
        float contentH = PANEL_H - 82f;
        float max = Math.max(0f, pages.get(selected).getTotalHeight() + 18f - contentH);
        targetScroll = clamp(targetScroll - (float)verticalAmount * 32f, 0f, max);
        return true;
    }

    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (selected >= 0) { selected = -1; return true; }
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static float logicalX(double x, int width) {
        float scale = Math.max(MIN_SCALE, Math.min(1f, Math.min(width / W, 1f)));
        return width / 2f + ((float)x - width / 2f) / scale;
    }
    private static float logicalY(double y, int height) {
        float scale = Math.max(MIN_SCALE, Math.min(1f, Math.min(height / H, 1f)));
        return height / 2f + ((float)y - height / 2f) / scale;
    }
    private static float approach(float current, float target, float speed, float dt) {
        return current + (target - current) * (1f - (float)Math.exp(-speed * Math.max(.001f, dt)));
    }
    private static float ease(float t) { float x = 1f - clamp(t, 0f, 1f); return 1f - x*x*x; }
    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
    private static float distance(float ax, float ay, float bx, float by) { return (float)Math.hypot(ax - bx, ay - by); }
    private static boolean pointIn(float mx, float my, float x, float y, float w, float h) { return mx >= x && mx <= x+w && my >= y && my <= y+h; }
    private static int withAlpha(int color, float a) { return (Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * a))) << 24) | (color & 0xFFFFFF); }
    private static int argb(int a, int r, int g, int b) { return (clampInt(a) << 24) | (clampInt(r) << 16) | (clampInt(g) << 8) | clampInt(b); }
    private static int clampInt(int v) { return Math.max(0, Math.min(255, v)); }
    private static void rounded(Canvas c, float x, float y, float w, float h, float r, int color) {
        Paint p = new Paint().setAntiAlias(true).setColor(color);
        c.drawRRect(RRect.makeXYWH(x, y, w, h, r), p);
        p.close();
    }
}
