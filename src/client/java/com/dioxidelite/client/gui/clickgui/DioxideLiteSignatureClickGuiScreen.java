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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Fast Signature theme. Uses Minecraft's immediate GUI renderer only: no Skia,
 * no off-screen framebuffer, no blur capture and no CPU texture upload.
 */
public final class DioxideLiteSignatureClickGuiScreen extends Screen {
    private static final int BG = 0xFF06090D;
    private static final int PANEL = 0xE50B1116;
    private static final int PANEL_2 = 0xCC0E161C;
    private static final int TEXT = 0xFFF2F7F6;
    private static final int MUTED = 0xFF96A39F;
    private static final int FAINT = 0xFF56635F;
    private static final int ACCENT = 0xFF58DDBE;
    private static final int ACCENT_DIM = 0xFF2D8E7A;
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

    public DioxideLiteSignatureClickGuiScreen(Screen parent) {
        super(Component.literal("DioxideLite Signature"));
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

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(.05f, Math.max(.001f, (now - lastNs) / 1_000_000_000f));
        lastNs = now;
        intro = approach(intro, 1f, 7f, dt);
        open = approach(open, selected >= 0 ? 1f : 0f, 10f, dt);
        scroll = approach(scroll, targetScroll, 16f, dt);

        g.fill(0, 0, width, height, BG);
        drawAmbient(g, mouseX, mouseY, intro);
        if (selected < 0) drawLauncher(g, mouseX, mouseY, intro);
        else drawWorkspace(g, mouseX, mouseY, intro, open, dt);
    }

    private void drawAmbient(GuiGraphics g, int mx, int my, float a) {
        int cx = width / 2, cy = height / 2;
        int aa = Math.round(255 * .06f * a);
        g.renderOutline(Math.max(20, cx - 190), Math.max(20, cy - 190), Math.min(width - 40, 380), Math.min(height - 40, 380), (aa << 24) | ACCENT);
        g.fill(24, cy, width - 24, cy + 1, (Math.round(255 * .025f * a) << 24) | 0xFFFFFF);
        g.fill(cx, 18, cx + 1, height - 18, (Math.round(255 * .025f * a) << 24) | 0xFFFFFF);
        g.drawString(font, "DIOXIDELITE", 22, 20, (Math.round(255 * a) << 24) | TEXT, false);
        g.drawString(font, "SIGNATURE UI", 22, 33, (Math.round(255 * .45f * a) << 24) | ACCENT, false);
        g.drawString(font, "RIGHT SHIFT", width - 92, height - 18, (Math.round(255 * .35f * a) << 24) | FAINT, false);
    }

    private void drawLauncher(GuiGraphics g, int mx, int my, float a) {
        int cx = width / 2, cy = height / 2;
        int r = Math.min(116, Math.min(width, height) / 4);
        int aa = Math.round(255 * .55f * a);
        g.renderOutline(cx - r, cy - r, r * 2, r * 2, (aa << 24) | ACCENT);
        g.renderOutline(cx - r - 10, cy - r - 10, r * 2 + 20, r * 2 + 20, (Math.round(255 * .12f * a) << 24) | 0xFFFFFF);
        g.drawCenteredString(font, "DioxideLite", cx, cy - 10, (Math.round(255 * a) << 24) | TEXT);
        g.drawCenteredString(font, "SIGNATURE", cx, cy + 5, (Math.round(255 * .85f * a) << 24) | ACCENT);
        g.drawCenteredString(font, "SELECT CATEGORY", cx, cy + 20, (Math.round(255 * .42f * a) << 24) | MUTED);
        for (int i = 0; i < NAV.length; i++) {
            double ang = -Math.PI / 2 + i * Math.PI * 2 / NAV.length;
            int x = cx + (int)(Math.cos(ang) * r);
            int y = cy + (int)(Math.sin(ang) * r);
            boolean hover = dist(mx, my, x, y) < 34;
            int size = hover ? 56 : 48;
            int fill = (Math.round(255 * (hover ? .14f : .07f) * a) << 24) | 0x0E181C;
            g.fill(x - size/2, y - size/2, x + size/2, y + size/2, fill);
            g.renderOutline(x - size/2, y - size/2, size, size,
                    (Math.round(255 * (hover ? .62f : .16f) * a) << 24) | (hover ? ACCENT : 0xFFFFFF));
            g.drawCenteredString(font, NAV[i], x, y - 3, (Math.round(255 * (hover ? 1f : .68f) * a) << 24) | (hover ? TEXT : MUTED));
            g.drawCenteredString(font, String.format("%02d", i + 1), x, y + 9, (Math.round(255 * .5f * a) << 24) | (hover ? ACCENT : FAINT));
        }
        g.drawString(font, "ESC  CLOSE", 24, height - 20, (Math.round(255 * .45f * a) << 24) | FAINT, false);
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

        g.drawString(font, "DioxideLite", margin + 18, top + 18, (alpha << 24) | TEXT, false);
        g.drawString(font, "SIGNATURE", margin + 18, top + 32, (alpha << 24) | ACCENT, false);
        for (int i = 0; i < NAV.length; i++) {
            int y = top + 58 + i * 43;
            boolean active = i == selected;
            boolean hover = mx >= margin + 10 && mx <= margin + railW - 10 && my >= y && my <= y + 34;
            if (active || hover) {
                g.fill(margin + 10, y, margin + railW - 10, y + 34,
                        (Math.round(255 * (active ? .14f : .07f) * a) << 24) | (active ? ACCENT_DIM : 0xFFFFFF));
                g.fill(margin + 10, y, margin + 12, y + 34, (alpha << 24) | ACCENT);
            }
            g.drawString(font, String.format("%02d", i + 1), margin + 19, y + 9,
                    (Math.round(255 * .55f * a) << 24) | (active ? ACCENT : FAINT), false);
            g.drawString(font, NAV[i], margin + 44, y + 9,
                    (Math.round(255 * (active ? 1f : .68f) * a) << 24) | (active ? TEXT : MUTED), false);
        }
        g.drawString(font, "ESC", margin + 18, top + h - 18, (Math.round(255 * .5f * a) << 24) | FAINT, false);
        g.drawString(font, "BACK", margin + 44, top + h - 18, (Math.round(255 * .75f * a) << 24) | MUTED, false);

        BasePage page = pages.get(selected);
        page.update(dt);
        g.drawString(font, NAV[selected], panelX + 20, top + 18, (alpha << 24) | TEXT, false);
        g.drawString(font, page.getSubtitle(), panelX + 20, top + 33, (Math.round(255 * .6f * a) << 24) | MUTED, false);
        g.drawString(font, String.format("%02d / %02d", selected + 1, NAV.length), panelX + panelW - 58, top + 18, (Math.round(255 * .42f * a) << 24) | FAINT, false);
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
            int contentX = panelX + 18, contentY = top + 60, contentW = panelW - 36, contentH = h - 76;
            float cy = contentY - scroll;
            BasePage page = pages.get(selected);
            for (SettingModule module : page.getModules()) {
                if (!module.isVisible()) continue;
                float mh = module.getTotalHeight();
                if (my >= cy && my <= cy + mh) {
                    if (page.onClick(mx, my, contentX, contentY, contentW, scroll, event.button())) return true;
                    break;
                }
                cy += mh + 8;
            }
            dragging = true; dragStartY = my; dragStartScroll = scroll; return true;
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
