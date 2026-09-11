package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.Config;
import com.dioxidelite.client.gui.clickgui.pages.*;
import com.dioxidelite.client.render.skia.DioxideLiteVisuals;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * DioxideLite Minimal ClickGUI theme, v1.8 native render rework.
 *
 * Like the Liquid Glass screen this is now drawn with plain GuiGraphics
 * (fills, hairline outlines, vanilla font) - no Skia/OpenGL anywhere in the
 * ClickGUI. Theme switching never removes or duplicates settings; the
 * presentation is the compact, outline-first minimal visual language.
 */
public final class DioxideLiteMinimalClickGuiScreen extends Screen implements ClickGuiScreen {
    private static final int BG = 0xB0080B10;
    private static final int PANEL = 0xD90C1118;
    private static final int TEXT = 0xFFF4F8FF;
    private static final int MUTED = 0xFF9BA7B7;
    private static final int ACCENT = 0xFF78CFFF;
    private static final float W = 860f;
    private static final float H = 520f;
    private static final float MIN_SCALE = 0.64f;
    private static final float NAV_W = 174f;
    private static final String[] NAV = {"Combat", "Render", "Tools", "Theme", "Optimize", "Misc"};

    private final List<BasePage> pages = new ArrayList<>();
    private final Screen parent;
    private int selected = 1;
    private float open = 0f;
    private float scroll = 0f;
    private float targetScroll = 0f;
    private long openedAt = System.nanoTime();
    private long lastFrameNs = System.nanoTime();
    private boolean draggingContent;
    private float dragStartY;
    private float dragStartScroll;

    public DioxideLiteMinimalClickGuiScreen(Screen parent) {
        super(Component.literal("DioxideLite"));
        this.parent = parent;
        pages.add(new CombatPage());
        pages.add(new RenderPage());
        pages.add(new ToolPage());
        pages.add(new ThemePage());
        pages.add(new OptimizePage());
        pages.add(new MiscPage());
    }

    @Override protected void init() {
        openedAt = System.nanoTime();
        lastFrameNs = openedAt;
        open = 0f;
        scroll = 0f;
        targetScroll = 0f;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int width = this.width;
        int height = this.height;
        long now = System.nanoTime();
        float dt = Math.min(.05f, Math.max(.001f, (now - lastFrameNs) / 1_000_000_000f));
        lastFrameNs = now;
        float t = Math.min(1f, (now - openedAt) / 360_000_000f);
        open = 1f - (float)Math.pow(1f - t, 3);
        scroll += (targetScroll - scroll) * (1f - (float)Math.exp(-16f * dt));

        BasePage page = pages.get(selected);
        page.update(dt);
        float scale = Math.max(MIN_SCALE, Math.min(1.0f, Math.min(width / W, height / H)));
        float x = width / 2f - W / 2f;
        float targetY = height / 2f - H / 2f;
        float y = targetY + (1f - open) * 46f;
        float alpha = .24f + .76f * open;

        g.fill(0, 0, width, height, withAlpha(BG, .76f * open));
        g.pose().pushMatrix();
        g.pose().translate(width / 2f, height / 2f);
        g.pose().scale(scale, scale);
        g.pose().translate(-width / 2f, -height / 2f);

        DioxideLiteVisuals.glassFast(g, Math.round(x), Math.round(y), Math.round(W), Math.round(H), alpha,
                PANEL & 0xFFFFFF, (PANEL >>> 24) / 255f, 0xD7E4F5, 0.16f);
        g.fill(Math.round(x + 1), Math.round(y + 1), Math.round(x + 1 + NAV_W), Math.round(y + H - 1), withAlpha(0xA8080D13, alpha));

        g.drawString(this.font, "DIOXIDE", Math.round(x + 28), Math.round(y + 34), TEXT, false);
        g.drawString(this.font, "MINIMAL", Math.round(x + 28), Math.round(y + 46), ACCENT, false);
        g.drawString(this.font, "DIOXIDELITE", Math.round(x + 28), Math.round(y + H - 30), withAlpha(MUTED, 0.8f), false);

        float localMouseX = width / 2f + (mouseX - width / 2f) / scale;
        float localMouseY = height / 2f + (mouseY - height / 2f) / scale;
        for (int i = 0; i < NAV.length; i++) {
            float ny = y + 96 + i * 44;
            boolean active = i == selected;
            boolean hovered = pointIn(localMouseX, localMouseY, x + 14, ny - 15, NAV_W - 28, 32);
            if (active) {
                g.fill(Math.round(x + 14), Math.round(ny - 15), Math.round(x + 14 + NAV_W - 28), Math.round(ny + 17), 0x2878CFFF);
                g.fill(Math.round(x + 14), Math.round(ny - 15), Math.round(x + 16), Math.round(ny + 17), ACCENT);
            }
            g.drawString(this.font, NAV[i], Math.round(x + 32), Math.round(ny - 4),
                    withAlpha(TEXT, active ? 1f : (hovered ? .76f : .45f)), false);
        }

        float contentX = x + NAV_W + 30f;
        float contentY = y + 92f;
        float contentW = W - NAV_W - 54f;
        float contentH = H - 112f;
        g.drawString(this.font, page.getTitle(), Math.round(contentX), Math.round(y + 34), TEXT, false);
        g.drawString(this.font, page.getSubtitle(), Math.round(contentX), Math.round(y + 48), MUTED, false);
        DioxideLiteVisuals.accentLineFast(g, Math.round(contentX), Math.round(y + 58), Math.round(contentW), 0.5f);

        page.drawFast(g, Math.round(contentX), Math.round(contentY), Math.round(contentW), Math.round(contentH),
                Math.round(alpha * 255f), scroll, Math.round(localMouseX), Math.round(localMouseY));

        // Scroll thumb is intentionally tiny and unobtrusive.
        float total = Math.max(contentH, page.getTotalHeight() + 18f);
        if (total > contentH + 1f) {
            float ratio = contentH / total;
            float thumbH = Math.max(24f, contentH * ratio);
            float maxScroll = Math.max(0f, total - contentH);
            float thumbY = contentY + (maxScroll <= 0 ? 0 : (scroll / maxScroll) * (contentH - thumbH));
            g.fill(Math.round(x + W - 15), Math.round(thumbY), Math.round(x + W - 12), Math.round(thumbY + thumbH), 0x6678CFFF);
        }
        g.pose().popMatrix();
    }

    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        float scale = Math.max(MIN_SCALE, Math.min(1.0f, Math.min(width / W, height / H)));
        float lx = width / 2f + ((float)event.x() - width / 2f) / scale;
        float ly = height / 2f + ((float)event.y() - height / 2f) / scale;
        float x = width / 2f - W / 2f;
        float y = height / 2f - H / 2f + (1f - open) * 46f;

        if (lx >= x + 14 && lx <= x + NAV_W - 14 && ly >= y + 80 && ly <= y + 80 + NAV.length * 48) {
            int idx = Math.round((ly - (y + 105)) / 48f);
            if (idx >= 0 && idx < pages.size()) {
                selected = idx;
                targetScroll = scroll = 0f;
                return true;
            }
        }

        float contentX = x + NAV_W + 30f;
        float contentY = y + 92f;
        float contentW = W - NAV_W - 54f;
        float contentH = H - 112f;
        if (pointIn(lx, ly, contentX, contentY, contentW, contentH)) {
            if (pages.get(selected).onClick(lx, ly, contentX, contentY, contentW, scroll, event.button())) return true;
            draggingContent = true;
            dragStartY = ly;
            dragStartScroll = scroll;
            return true;
        }
        return true;
    }

    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (!draggingContent || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        float scale = Math.max(MIN_SCALE, Math.min(1.0f, Math.min(width / W, height / H)));
        float ly = height / 2f + ((float)event.y() - height / 2f) / scale;
        targetScroll = Math.max(0f, dragStartScroll + (dragStartY - ly));
        return true;
    }

    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingContent = false;
            pages.get(selected).releaseDrag();
            return true;
        }
        return false;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScroll = Math.max(0f, targetScroll - (float)verticalAmount * 34f);
        float contentH = H - 112f;
        float max = Math.max(0f, pages.get(selected).getTotalHeight() + 18f - contentH);
        targetScroll = Math.min(max, targetScroll);
        return true;
    }

    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private static boolean pointIn(float mx, float my, float x, float y, float w, float h) { return mx >= x && mx <= x+w && my >= y && my <= y+h; }
    private static int withAlpha(int color, float a) { return (Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * a))) << 24) | (color & 0xFFFFFF); }
}
