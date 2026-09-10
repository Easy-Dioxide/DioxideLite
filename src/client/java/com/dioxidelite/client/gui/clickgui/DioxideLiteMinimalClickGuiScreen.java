package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.Config;
import com.dioxidelite.client.gui.clickgui.pages.*;
import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * DioxideLite Signature ClickGUI theme.
 * It uses the same SettingModule/page model as the original ClickGUI, so theme
 * switching never removes or duplicates settings. The presentation is independently
 * implemented with the compact, outline-first visual language of the supplied reference.
 */
public final class DioxideLiteMinimalClickGuiScreen extends SkiaScreen {
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
        super(Component.literal("DioxideLite"), parent);
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

    @Override protected boolean needsContinuousRedraw() { return true; }

    @Override protected void drawSkia(Canvas c, int width, int height, int mouseX, int mouseY, float delta) {
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

        c.drawColor(withAlpha(BG, .76f * open));
        c.save();
        c.translate(width / 2f, height / 2f);
        c.scale(scale, scale);
        c.translate(-width / 2f, -height / 2f);

        // The GPU-backed Skia screen composites the glass background before this
        // canvas is submitted. Do not start a second blur/capture pass from inside
        // the same canvas; that was one of the largest sources of ClickGUI stalls.
        rounded(c, x, y, W, H, Config.glassRadius, withAlpha(PANEL, alpha));
        rounded(c, x + 1, y + 1, NAV_W, H - 2, 14, withAlpha(0xA8080D13, alpha));

        FontRenderer.drawText(c, "DIOXIDE", x + 28, y + 42, 19, TEXT);
        FontRenderer.drawText(c, "SIGNATURE", x + 28, y + 62, 10, ACCENT);
        FontRenderer.drawText(c, "DIOXIDELITE", x + 28, y + H - 30, 8, MUTED);

        float localMouseX = width / 2f + (mouseX - width / 2f) / scale;
        float localMouseY = height / 2f + (mouseY - height / 2f) / scale;
        for (int i = 0; i < NAV.length; i++) {
            float ny = y + 105 + i * 48;
            boolean active = i == selected;
            boolean hovered = pointIn(localMouseX, localMouseY, x + 14, ny - 17, NAV_W - 28, 34);
            if (active) rounded(c, x + 14, ny - 17, NAV_W - 28, 34, 6, 0x2878CFFF);
            if (active) rounded(c, x + 14, ny - 17, 2, 34, 1, ACCENT);
            FontRenderer.drawText(c, NAV[i], x + 32, ny + 4, 10,
                    withAlpha(TEXT, active ? 1f : (hovered ? .76f : .45f)));
        }

        float contentX = x + NAV_W + 30f;
        float contentY = y + 92f;
        float contentW = W - NAV_W - 54f;
        float contentH = H - 112f;
        FontRenderer.drawText(c, page.getTitle(), contentX, y + 42, 18, TEXT);
        FontRenderer.drawText(c, page.getSubtitle(), contentX, y + 62, 9, MUTED);
        rounded(c, contentX, y + 76, contentW, 1, .5f, 0x4078CFFF);

        c.save();
        c.clipRect(Rect.makeXYWH(contentX, contentY, contentW, contentH), true);
        page.draw(c, contentX, contentY, contentW, contentH, alpha, scroll);
        c.restore();

        // Scroll thumb is intentionally tiny and unobtrusive.
        float total = Math.max(contentH, page.getTotalHeight() + 18f);
        if (total > contentH + 1f) {
            float ratio = contentH / total;
            float thumbH = Math.max(24f, contentH * ratio);
            float maxScroll = Math.max(0f, total - contentH);
            float thumbY = contentY + (maxScroll <= 0 ? 0 : (scroll / maxScroll) * (contentH - thumbH));
            rounded(c, x + W - 15, thumbY, 3, thumbH, 1.5f, 0x6678CFFF);
        }
        c.restore();
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

    private static void rounded(Canvas c, float x, float y, float w, float h, float r, int color) {
        Paint p = new Paint().setAntiAlias(true).setColor(color);
        c.drawRRect(RRect.makeXYWH(x, y, w, h, r), p);
        p.close();
    }
    private static boolean pointIn(float mx, float my, float x, float y, float w, float h) { return mx >= x && mx <= x+w && my >= y && my <= y+h; }
    private static int withAlpha(int color, float a) { return (Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * a))) << 24) | (color & 0xFFFFFF); }
}
