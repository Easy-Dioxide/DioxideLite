package com.dioxidelite.client.gui.clickgui.widget;

import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.DioxideLiteVisuals;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

public class SettingButton extends SettingWidget {
    private final Supplier<String> label;
    private final Runnable action;
    private float pressT = 0f;
    private final Paint bgPaint = new Paint();
    private String cachedText = "";
    private float cachedTextWidth = 0f;

    public SettingButton(String label, Runnable action) {
        this(() -> label, action);
    }

    public SettingButton(Supplier<String> label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    @Override public float getWidth() { return 100f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        pressT += (0f - pressT) * 0.18f;
        bgPaint.setColor(withAlpha(0x10283A, alpha * (0.86f + 0.12f * pressT)));
        canvas.drawRRect(RRect.makeXYWH(x, y, getWidth(), getHeight(), 8f), bgPaint);
        DioxideLiteVisuals.outline(canvas, x, y, getWidth(), getHeight(), 8f, DioxideLiteVisuals.CYAN, alpha * (0.26f + 0.34f * pressT), 0.8f);
        String text = label.get();
        if (!text.equals(cachedText)) {
            cachedText = text;
            cachedTextWidth = FontRenderer.measureTextWidth(text, 11f);
        }
        FontRenderer.drawText(canvas, text, x + (getWidth() - cachedTextWidth) * 0.5f, y + 15.5f, 11f, DioxideLiteVisuals.text(alpha));
    }

    @Override
    public void drawFast(GuiGraphics g, int x, int y, int alpha) {
        g.fill(x, y, x + 100, y + 24, (alpha << 24) | 0x10283A);
        g.renderOutline(x, y, 100, 24, (alpha << 24) | 0x58DDBE);
        String text = label.get();
        g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, text, x + 50, y + 8, (alpha << 24) | 0xF2F7F6);
    }

    @Override
    public boolean isAnimating() {
        return pressT > 0.01f;
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        if (mx < x || mx > x + getWidth() || my < y || my > y + getHeight()) return false;
        pressT = 1f;
        action.run();
        return true;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(ar+(br-ar)*t) << 16) | ((int)(ag+(bg-ag)*t) << 8) | (int)(ab+(bb-ab)*t);
    }
}
