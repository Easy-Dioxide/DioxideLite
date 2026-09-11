package com.dioxidelite.client.gui.clickgui.widget;

import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.DioxideLiteVisuals;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingSlider extends SettingWidget {

    private final Supplier<Double> getter;
    private final Consumer<Double> setter;
    private final double min, max;
    private final String format;
    private boolean dragging = false;
    private double cachedValue = Double.NaN;
    private String cachedText = "";
    private float cachedTextWidth = 0f;
    private final Paint trackPaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Paint thumbPaint = new Paint();

    private static final int COLOR_TRACK    = 0xFFE0E0E0;
    private static final int COLOR_FILL     = 0xFF2F54EB;
    private static final int COLOR_THUMB    = 0xFFFFFFFF;
    private static final int COLOR_TEXT     = 0xFF888888;

    public SettingSlider(double min, double max, String format, Supplier<Double> getter, Consumer<Double> setter) {
        this.min = min;
        this.max = max;
        this.format = format;
        this.getter = getter;
        this.setter = setter;
    }

    private static final float LABEL_W = 36f;
    private static final float TRACK_W = 120f;

    @Override public float getWidth() { return LABEL_W + 8f + TRACK_W; }
    @Override public float getHeight() { return 20f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        double value = getter.get();
        if (Double.compare(value, cachedValue) != 0) {
            cachedValue = value;
            cachedText = String.format(format, value);
            cachedTextWidth = FontRenderer.measureTextWidth(cachedText, 11f);
        }
        String val = cachedText;
        float lw = cachedTextWidth;
        FontRenderer.drawText(canvas, val, x + LABEL_W - lw, y + 14f, 10.5f, DioxideLiteVisuals.muted(alpha));

        float tx = x + LABEL_W + 8f;
        float t = (float)((value - min) / (max - min));
        float trackY = y + 9f;
        float thumbX = tx + t * TRACK_W;

        trackPaint.setColor(withAlpha(0x5D6A7A, alpha * 0.45f));
        canvas.drawRRect(RRect.makeXYWH(tx, trackY, TRACK_W, 3f, 1.5f), trackPaint);
        fillPaint.setColor(withAlpha(DioxideLiteVisuals.CYAN, alpha * 0.82f));
        canvas.drawRRect(RRect.makeXYWH(tx, trackY, t * TRACK_W, 3f, 1.5f), fillPaint);
        thumbPaint.setColor(withAlpha(0xEAF8FF, alpha));
        canvas.drawCircle(thumbX, y + 10f, 5f, thumbPaint);
        DioxideLiteVisuals.outline(canvas, thumbX - 6f, y + 4f, 12f, 12f, 6f, DioxideLiteVisuals.CYAN, alpha * 0.45f, 0.8f);
    }

    @Override
    public void drawFast(GuiGraphics g, int x, int y, int alpha) {
        double value = getter.get();
        float t = (float)Math.max(0d, Math.min(1d, (value - min) / (max - min)));
        String text = String.format(format, value);
        g.drawString(net.minecraft.client.Minecraft.getInstance().font, text, x, y + 6, (alpha << 24) | 0x9BA6A3, false);
        int tx = x + 44;
        g.fill(tx, y + 9, tx + 120, y + 12, (alpha << 24) | 0x33414B);
        g.fill(tx, y + 9, tx + Math.round(t * 120f), y + 12, (alpha << 24) | 0x58DDBE);
        int px = tx + Math.round(t * 120f);
        g.fill(px - 3, y + 5, px + 4, y + 16, (alpha << 24) | 0xEAF8FF);
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        dragging = true;
        applyMouse(mx, x);
        return true;
    }

    @Override
    public boolean onDrag(float mx, float my, float x, float y) {
        if (!dragging) return false;
        applyMouse(mx, x);
        return true;
    }

    public void releaseDrag() { dragging = false; }
    public boolean isDragging() { return dragging; }

    private void applyMouse(float mx, float x) {
        float tx = x + LABEL_W + 8f;
        float t = Math.max(0f, Math.min(1f, (mx - tx) / TRACK_W));
        setter.accept(min + t * (max - min));
    }
}
