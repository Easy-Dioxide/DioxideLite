package com.dioxidelite.client.gui.clickgui.widget;

import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.DioxideLiteVisuals;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingCycle extends SettingWidget {

    private final List<String> options;
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;
    private final Paint bgPaint = new Paint();
    private int cachedIndex = Integer.MIN_VALUE;
    private String cachedLabel = "";
    private float cachedTextWidth = 0f;

    private static final int COLOR_BG   = 0xFFF0F0F0;
    private static final int COLOR_TEXT = 0xFF333333;

    public SettingCycle(List<String> options, Supplier<Integer> getter, Consumer<Integer> setter) {
        this.options = options;
        this.getter = getter;
        this.setter = setter;
    }

    @Override public float getWidth() { return 100f; }
    @Override public float getHeight() { return 24f; }

    @Override
    public void draw(Canvas canvas, float x, float y, float alpha) {
        int index = getter.get() % options.size();
        if (index != cachedIndex) {
            cachedIndex = index;
            cachedLabel = options.get(index);
            cachedTextWidth = FontRenderer.measureTextWidth(cachedLabel, 12f);
        }
        bgPaint.setColor(withAlpha(0x101923, alpha * 0.82f));
        canvas.drawRRect(RRect.makeXYWH(x, y, getWidth(), getHeight(), 7f), bgPaint);
        DioxideLiteVisuals.outline(canvas, x, y, getWidth(), getHeight(), 7f, DioxideLiteVisuals.CYAN, alpha * 0.18f, 0.8f);
        FontRenderer.drawText(canvas, cachedLabel, x + (getWidth() - cachedTextWidth) / 2f, y + 16f, 11.5f, DioxideLiteVisuals.text(alpha * 0.92f));
    }

    @Override
    public boolean onClick(float mx, float my, float x, float y, int button) {
        if (button != 0) return false;
        setter.accept((getter.get() + 1) % options.size());
        return true;
    }
}
