package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.Config;
import com.dioxidelite.client.gui.clickgui.pages.BasePage;
import com.dioxidelite.client.gui.clickgui.pages.CombatPage;
import com.dioxidelite.client.gui.clickgui.pages.MiscPage;
import com.dioxidelite.client.gui.clickgui.pages.OptimizePage;
import com.dioxidelite.client.gui.clickgui.pages.RenderPage;
import com.dioxidelite.client.gui.clickgui.pages.ThemePage;
import com.dioxidelite.client.gui.clickgui.pages.ToolPage;
import com.dioxidelite.client.gui.clickgui.widget.SettingModule;
import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.DioxideLiteVisuals;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * DioxideLite's Signature presentation theme.
 *
 * This is an independent implementation of a radial-to-inspector interaction:
 * generous negative space, thin outlines, compact typography and a smooth
 * morph from category nodes into a settings workspace. No reference-client
 * assets or source code are bundled here.
 */
public final class DioxideLiteSignatureClickGuiScreen extends SkiaScreen {
    private static final float BASE_W = 1100f;
    private static final float BASE_H = 660f;
    private static final float MIN_SCALE = .58f;
    private static final float NODE_RADIUS = 25f;
    private static final float RING_RADIUS = 118f;
    private static final float RAIL_W = 178f;
    private static final float PANEL_GAP = 12f;

    private static final int BG = 0xE8060A0D;
    private static final int SURFACE = 0xD90C1216;
    private static final int SURFACE_2 = 0xB8131A20;
    private static final int OUTLINE = 0x5AFFFFFF;
    private static final int TEXT = 0xFFF2F7F6;
    private static final int MUTED = 0xFF99A6A3;
    private static final int FAINT = 0xFF5D6B68;
    private static final int ACCENT = 0xFF58DDBE;
    private static final int ACCENT_SOFT = 0xFF2BAF96;

    private static final String[] NAV = {"Combat", "Render", "Tools", "Theme", "Optimize", "Misc"};
    private final List<BasePage> pages = new ArrayList<>();
    private final Map<Integer, Float> nodeHover = new java.util.HashMap<>();
    private final Map<SettingModule, Float> moduleHover = new IdentityHashMap<>();
    private final Screen parent;

    private int selected = -1;
    private float intro;
    private float open;
    private float scroll;
    private float targetScroll;
    private float pointerX;
    private float pointerY;
    private float dragStartY;
    private float dragStartScroll;
    private boolean dragging;
    private long lastFrame = System.nanoTime();

    public DioxideLiteSignatureClickGuiScreen(Screen parent) {
        super(Component.literal("DioxideLite Signature"), parent);
        this.parent = parent;
        pages.add(new CombatPage());
        pages.add(new RenderPage());
        pages.add(new ToolPage());
        pages.add(new ThemePage());
        pages.add(new OptimizePage());
        pages.add(new MiscPage());
        for (int i = 0; i < NAV.length; i++) nodeHover.put(i, 0f);
    }

    @Override protected void init() {
        intro = 0f;
        open = 0f;
        scroll = targetScroll = 0f;
        selected = -1;
        dragging = false;
        lastFrame = System.nanoTime();
    }

    @Override protected boolean needsContinuousRedraw() { return true; }

    @Override
    protected void drawSkia(Canvas c, int width, int height, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(.05f, Math.max(.001f, (now - lastFrame) / 1_000_000_000f));
        lastFrame = now;
        intro = approach(intro, 1f, 7.5f, dt);
        open = approach(open, selected >= 0 ? 1f : 0f, 9.5f, dt);
        scroll = approach(scroll, targetScroll, 15f, dt);

        float scale = scale(width, height);
        pointerX = logicalX(mouseX, width, scale);
        pointerY = logicalY(mouseY, height, scale);
        BasePage page = selected >= 0 ? pages.get(selected) : null;
        if (page != null) page.update(dt);

        c.drawColor(BG);
        c.save();
        c.translate(width * .5f, height * .5f);
        c.scale(scale, scale);
        c.translate(-width * .5f, -height * .5f);

        drawAmbient(c, width, height, ease(intro));
        if (selected < 0) {
            drawLauncher(c, width, height, ease(intro));
        } else {
            drawWorkspace(c, width, height, ease(intro), ease(open));
        }
        c.restore();
    }

    private void drawAmbient(Canvas c, int w, int h, float alpha) {
        float cx = w * .5f, cy = h * .5f;
        Paint p = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE);
        p.setStrokeWidth(.65f);
        p.setColor(withAlpha(ACCENT, .055f * alpha));
        c.drawCircle(cx, cy, RING_RADIUS * 2.35f, p);
        p.setColor(withAlpha(0xFFFFFFFF, .025f * alpha));
        c.drawCircle(cx, cy, RING_RADIUS * 3.5f, p);
        c.drawLine(28f, cy, w - 28f, cy, p);
        c.drawLine(cx, 24f, cx, h - 24f, p);
        p.close();

        Paint glow = new Paint().setAntiAlias(true);
        float pulse = .5f + .5f * (float)Math.sin(System.nanoTime() / 1_000_000_000.0 * .7);
        glow.setColor(withAlpha(ACCENT_SOFT, .025f + pulse * .018f));
        c.drawCircle(cx, cy, 170f + pulse * 8f, glow);
        glow.close();
    }

    private void drawLauncher(Canvas c, int w, int h, float alpha) {
        float cx = w * .5f, cy = h * .5f;
        float reveal = ease(intro);
        Paint ring = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE);
        ring.setStrokeWidth(1f).setColor(withAlpha(ACCENT, .55f * reveal));
        c.drawCircle(cx, cy, RING_RADIUS * reveal, ring);
        ring.setStrokeWidth(.55f).setColor(withAlpha(0xFFFFFFFF, .14f * reveal));
        c.drawCircle(cx, cy, RING_RADIUS + 11f, ring);
        ring.close();

        FontRenderer.drawText(c, "DioxideLite", cx - 43f, cy - 8f, 12f, withAlpha(TEXT, reveal));
        FontRenderer.drawText(c, "SIGNATURE", cx - 27f, cy + 14f, 7f, withAlpha(ACCENT, reveal * .85f));
        FontRenderer.drawText(c, "SELECT CATEGORY", cx - 37f, cy + 29f, 6.3f, withAlpha(MUTED, reveal));

        for (int i = 0; i < NAV.length; i++) {
            double angle = -Math.PI / 2d + i * Math.PI * 2d / NAV.length;
            float x = cx + (float)Math.cos(angle) * RING_RADIUS;
            float y = cy + (float)Math.sin(angle) * RING_RADIUS;
            boolean hovered = distance(pointerX, pointerY, x, y) <= NODE_RADIUS + 8f;
            float hv = approach(nodeHover.getOrDefault(i, 0f), hovered ? 1f : 0f, 13f, .016f);
            nodeHover.put(i, hv);
            float r = NODE_RADIUS + hv * 3f;
            int fill = mix(SURFACE, 0xE01B2A2B, hv * .55f);
            DioxideLiteVisuals.card(c, x - r, y - r, r * 2f, r * 2f, r, reveal * (.9f + hv * .1f), hv > .5f, false);
            DioxideLiteVisuals.outline(c, x - r, y - r, r * 2f, r * 2f, r,
                    hv > .5f ? ACCENT : 0xFFFFFF, reveal * (hv > .5f ? .62f : .10f), 1f);
            FontRenderer.drawText(c, NAV[i], x - FontRenderer.measureTextWidth(NAV[i], 8f) / 2f,
                    y + 3f, 8f, withAlpha(hv > .5f ? TEXT : MUTED, reveal));
            FontRenderer.drawText(c, String.valueOf(i + 1), x - 2f, y - 11f, 5.5f,
                    withAlpha(hv > .5f ? ACCENT : FAINT, reveal));
        }

        FontRenderer.drawText(c, "ESC  CLOSE", 24f, h - 22f, 7f, withAlpha(FAINT, reveal));
        FontRenderer.drawText(c, "RIGHT CLICK  EXPAND", w - 112f, h - 22f, 7f, withAlpha(FAINT, reveal));
    }

    private void drawWorkspace(Canvas c, int w, int h, float alpha, float progress) {
        float t = ease(progress);
        float marginX = Math.max(26f, (w - BASE_W) * .5f);
        float top = Math.max(24f, h * .09f);
        float bottom = h - Math.max(30f, h * .07f);
        float railX = marginX;
        float railY = top;
        float railH = bottom - top;
        float panelX = railX + RAIL_W + PANEL_GAP;
        float panelY = top;
        float panelW = w - panelX - marginX;
        float panelH = railH;

        float slide = (1f - t) * 42f;
        drawPanel(c, railX - slide, railY, RAIL_W, railH, alpha * t, true);
        drawPanel(c, panelX + slide, panelY, panelW, panelH, alpha * t, false);

        drawRail(c, railX - slide, railY, railH, alpha * t);
        drawContent(c, panelX + slide, panelY, panelW, panelH, alpha * t);
    }

    private void drawPanel(Canvas c, float x, float y, float w, float h, float alpha, boolean rail) {
        DioxideLiteVisuals.card(c, x, y, w, h, 11f, alpha, false, false);
        DioxideLiteVisuals.outline(c, x, y, w, h, 11f, 0xFFFFFF, alpha * .11f, .8f);
        if (!rail) DioxideLiteVisuals.accentLine(c, x + 18f, y + 18f, Math.min(64f, w * .22f), alpha * .65f);
    }

    private void drawRail(Canvas c, float x, float y, float h, float alpha) {
        FontRenderer.drawText(c, "DioxideLite", x + 18f, y + 27f, 12f, withAlpha(TEXT, alpha));
        FontRenderer.drawText(c, "SIGNATURE", x + 18f, y + 43f, 6.5f, withAlpha(ACCENT, alpha));
        float cy = y + 76f;
        for (int i = 0; i < NAV.length; i++) {
            boolean active = i == selected;
            boolean hovered = pointerX >= x + 10f && pointerX <= x + RAIL_W - 10f
                    && pointerY >= cy && pointerY <= cy + 38f;
            float hv = approach(nodeHover.getOrDefault(100 + i, 0f), hovered ? 1f : 0f, 13f, .016f);
            nodeHover.put(100 + i, hv);
            int fill = active ? withAlpha(ACCENT_SOFT, .18f * alpha) : withAlpha(0xFFFFFF, .035f * alpha);
            if (hv > .01f) fill = withAlpha(0xFFFFFF, (.035f + .07f * hv) * alpha);
            Paint p = new Paint().setAntiAlias(true).setColor(fill);
            c.drawRRect(RRect.makeXYWH(x + 10f, cy, RAIL_W - 20f, 38f, 7f), p);
            p.close();
            if (active) DioxideLiteVisuals.accentLine(c, x + 11f, cy + 8f, 2.5f, alpha);
            FontRenderer.drawText(c, String.format("%02d", i + 1), x + 20f, cy + 24f, 6f,
                    withAlpha(active ? ACCENT : FAINT, alpha));
            FontRenderer.drawText(c, NAV[i], x + 43f, cy + 24f, 9f,
                    withAlpha(active ? TEXT : MUTED, alpha * (active ? 1f : .92f)));
            cy += 44f;
        }
        FontRenderer.drawText(c, "ESC", x + 18f, y + h - 21f, 6.5f, withAlpha(FAINT, alpha));
        FontRenderer.drawText(c, "BACK", x + 43f, y + h - 21f, 6.5f, withAlpha(MUTED, alpha));
    }

    private void drawContent(Canvas c, float x, float y, float w, float h, float alpha) {
        BasePage page = pages.get(selected);
        FontRenderer.drawText(c, NAV[selected], x + 20f, y + 31f, 18f, withAlpha(TEXT, alpha));
        FontRenderer.drawText(c, page.getSubtitle(), x + 20f, y + 49f, 7.5f, withAlpha(MUTED, alpha));
        FontRenderer.drawText(c, String.format("%02d / %02d", selected + 1, NAV.length), x + w - 55f, y + 27f, 6f, withAlpha(FAINT, alpha));
        FontRenderer.drawText(c, "BACK", x + w - 51f, y + 42f, 6.5f, withAlpha(ACCENT, alpha));

        float contentX = x + 18f;
        float contentY = y + 66f;
        float contentW = w - 36f;
        float contentH = h - 82f;
        c.save();
        c.clipRect(Rect.makeXYWH(contentX, contentY, contentW, contentH));
        drawModuleRows(c, page, contentX, contentY, contentW, contentH, alpha);
        c.restore();

        float total = page.getTotalHeight() + 8f;
        if (total > contentH) {
            float max = total - contentH;
            float thumbH = Math.max(26f, contentH * contentH / total);
            float thumbY = contentY + scroll / Math.max(1f, max) * (contentH - thumbH);
            DioxideLiteVisuals.accentLine(c, x + w - 5f, thumbY, 2f, alpha * .75f);
            Paint p = new Paint().setAntiAlias(true).setColor(withAlpha(ACCENT, alpha * .35f));
            c.drawRRect(RRect.makeXYWH(x + w - 5f, thumbY, 2f, thumbH, 1f), p);
            p.close();
        }
    }

    private void drawModuleRows(Canvas c, BasePage page, float x, float y, float w, float h, float alpha) {
        float cy = y - scroll;
        for (SettingModule module : page.getModules()) {
            if (!module.isVisible()) continue;
            float mh = module.getTotalHeight();
            if (cy + mh > y && cy < y + h) {
                boolean hovered = pointerX >= x && pointerX <= x + w && pointerY >= cy && pointerY <= cy + Math.min(56f, mh);
                float hv = approach(moduleHover.getOrDefault(module, 0f), hovered ? 1f : 0f, 12f, .016f);
                moduleHover.put(module, hv);
                module.draw(c, x, cy, w, alpha, y, y + h);
                if (hv > .01f) {
                    DioxideLiteVisuals.outline(c, x, cy, w, Math.min(48f, mh - 8f), 9f,
                            ACCENT, alpha * .10f * hv, 1f);
                }
            }
            cy += mh + 8f;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return false;
        float scale = scale(width, height);
        float x = logicalX(event.x(), width, scale), y = logicalY(event.y(), height, scale);
        float cx = width * .5f, cy = height * .5f;

        if (selected < 0) {
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                for (int i = 0; i < NAV.length; i++) {
                    double angle = -Math.PI / 2d + i * Math.PI * 2d / NAV.length;
                    float nx = cx + (float)Math.cos(angle) * RING_RADIUS;
                    float ny = cy + (float)Math.sin(angle) * RING_RADIUS;
                    if (distance(x, y, nx, ny) <= NODE_RADIUS + 9f) {
                        selected = i;
                        scroll = targetScroll = 0f;
                        return true;
                    }
                }
            }
            return true;
        }

        float marginX = Math.max(26f, (width - BASE_W) * .5f);
        float top = Math.max(24f, height * .09f);
        float railX = marginX;
        float panelX = railX + RAIL_W + PANEL_GAP;
        float panelY = top;
        float panelW = width - panelX - marginX;
        float panelH = height - top - Math.max(30f, height * .07f);

        if (x >= railX + 10f && x <= railX + RAIL_W - 10f && y >= panelY + 70f && y <= panelY + panelH - 10f) {
            int hit = (int)((y - (panelY + 76f)) / 44f);
            if (hit >= 0 && hit < NAV.length && y <= panelY + 76f + NAV.length * 44f) {
                selected = hit;
                scroll = targetScroll = 0f;
                return true;
            }
        }

        if (x >= panelX + 18f && x <= panelX + panelW - 18f && y >= panelY + 66f && y <= panelY + panelH - 16f) {
            float contentX = panelX + 18f;
            float contentY = panelY + 66f;
            float contentW = panelW - 36f;
            if (pages.get(selected).onClick(x, y, contentX, contentY, contentW, scroll, event.button())) return true;
        }

        if (x >= panelX + panelW - 70f && y >= panelY + 10f && y <= panelY + 58f) {
            selected = -1;
            return true;
        }
        if (x >= railX && x <= railX + RAIL_W && y >= panelY + panelH - 42f) {
            selected = -1;
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (selected < 0 || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        float scale = scale(width, height);
        float x = logicalX(event.x(), width, scale), y = logicalY(event.y(), height, scale);
        float top = Math.max(24f, height * .09f);
        float marginX = Math.max(26f, (width - BASE_W) * .5f);
        float panelX = marginX + RAIL_W + PANEL_GAP;
        float panelY = top;
        float panelW = width - panelX - marginX;
        float contentX = panelX + 18f, contentY = panelY + 66f, contentW = panelW - 36f;
        float contentH = height - top - Math.max(30f, height * .07f) - 82f;
        if (!dragging) {
            dragging = true;
            dragStartY = y;
            dragStartScroll = scroll;
        }
        float max = Math.max(0f, pages.get(selected).getTotalHeight() + 8f - contentH);
        targetScroll = clamp(dragStartScroll + (dragStartY - y), 0f, max);
        pages.get(selected).onDrag(x, y, contentX, contentY, contentW, scroll);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
            if (selected >= 0) pages.get(selected).releaseDrag();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selected < 0) return true;
        float top = Math.max(24f, height * .09f);
        float marginX = Math.max(26f, (width - BASE_W) * .5f);
        float panelX = marginX + RAIL_W + PANEL_GAP;
        float panelW = width - panelX - marginX;
        float contentH = height - top - Math.max(30f, height * .07f) - 82f;
        float max = Math.max(0f, pages.get(selected).getTotalHeight() + 8f - contentH);
        targetScroll = clamp(targetScroll - (float)verticalAmount * 32f, 0f, max);
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (selected >= 0) { selected = -1; return true; }
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static float scale(int w, int h) {
        return Math.max(MIN_SCALE, Math.min(1f, Math.min(w / BASE_W, h / BASE_H)));
    }

    private static float logicalX(double x, int width, float scale) {
        return width * .5f + ((float)x - width * .5f) / scale;
    }

    private static float logicalY(double y, int height, float scale) {
        return height * .5f + ((float)y - height * .5f) / scale;
    }

    private static float approach(float current, float target, float speed, float dt) {
        return current + (target - current) * (1f - (float)Math.exp(-speed * Math.max(.001f, dt)));
    }

    private static float ease(float t) {
        float x = 1f - clamp(t, 0f, 1f);
        return 1f - x * x * x;
    }

    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
    private static float distance(float ax, float ay, float bx, float by) { return (float)Math.hypot(ax - bx, ay - by); }

    private static int withAlpha(int color, float alpha) {
        return (Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * alpha))) << 24) | (color & 0xFFFFFF);
    }

    private static int mix(int from, int to, float t) {
        t = clamp(t, 0f, 1f);
        int a = (int)(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * t);
        int r = (int)(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = (int)(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = (int)((from & 255) + ((to & 255) - (from & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
