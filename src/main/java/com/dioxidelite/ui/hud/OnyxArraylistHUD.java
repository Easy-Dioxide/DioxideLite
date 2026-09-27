package com.dioxidelite.ui.hud;

import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.module.ModuleManager;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import io.github.humbleui.skija.Canvas;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Onyx-style right-aligned module list, ported 1:1 from OpenOnyx's ArrayListModule.
 * Three row styles: Accent Bar / Pills / Text. Six colour modes: Accent / Static /
 * Fade / Gradient / Rainbow / Category.
 */
public final class OnyxArraylistHUD extends EpsilonHudModule {

    public static final OnyxArraylistHUD INSTANCE = new OnyxArraylistHUD();

    public enum Style { ACCENT_BAR, PILLS, TEXT }
    public enum ColorMode { ACCENT, STATIC, FADE, GRADIENT, RAINBOW, CATEGORY }
    public enum Sort { WIDTH, ALPHA }

    // Onyx default: first colour 0xFF7B5FEF (purple), second 0xFF4030A0 (deep purple)
    public final DoubleSetting scale = add(new DoubleSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    public final EnumSetting<Style> style = add(new EnumSetting<>("Style", Style.ACCENT_BAR));
    public final EnumSetting<ColorMode> colorMode = add(new EnumSetting<>("Colors", ColorMode.ACCENT));
    public final ColorSetting firstColor = add(new ColorSetting("First Color",
            new Color(0x7B, 0x5F, 0xEF), false).visibleWhen(() ->
            colorMode.get() == ColorMode.GRADIENT || colorMode.get() == ColorMode.FADE));
    public final ColorSetting secondColor = add(new ColorSetting("Second Color",
            new Color(0x40, 0x30, 0xA0), false).visibleWhen(() ->
            colorMode.get() == ColorMode.GRADIENT || colorMode.get() == ColorMode.FADE));
    public final DoubleSetting colorSpeed = add(new DoubleSetting("Color Speed", 1.0, 0.1, 5.0, 0.1)
            .visibleWhen(() -> colorMode.get() == ColorMode.FADE || colorMode.get() == ColorMode.RAINBOW));
    public final DoubleSetting colorSpread = add(new DoubleSetting("Color Spread", 12.0, 0.0, 60.0, 1.0)
            .visibleWhen(() -> colorMode.get() == ColorMode.GRADIENT || colorMode.get() == ColorMode.RAINBOW));
    public final EnumSetting<Sort> sort = add(new EnumSetting<>("Sort", Sort.WIDTH));
    public final BooleanSetting suffix = add(new BooleanSetting("Suffix", true));
    public final BooleanSetting lowercase = add(new BooleanSetting("Lowercase", false));
    public final BooleanSetting background = add(new BooleanSetting("Background", false)
            .visibleWhen(() -> style.get() != Style.TEXT));
    public final BooleanSetting blur = add(new BooleanSetting("Blur", false)
            .visibleWhen(() -> style.get() != Style.TEXT && background.get()));
    public final BooleanSetting textShadow = add(new BooleanSetting("Text Shadow", true)
            .visibleWhen(() -> style.get() == Style.TEXT));
    public final BooleanSetting outline = add(new BooleanSetting("Outline", false)
            .visibleWhen(() -> style.get() == Style.PILLS));
    public final DoubleSetting rowSpacing = add(new DoubleSetting("Row Spacing", 0.0, 0.0, 6.0, 0.5));
    public final DoubleSetting animSpeed = add(new DoubleSetting("Animation Speed", 1.0, 0.25, 3.0, 0.05));

    private final Map<Module, Float> anim = new IdentityHashMap<>();

    private OnyxArraylistHUD() {
        super("Onyx ArrayList", Category.HUD, 88, 5, 120, 80);
        setEnabled(true);
    }

    @Override
    protected void renderHud(Render2DEvent event) {
        float s = scale.get().floatValue();
        float fontSize = 9.0f * s;
        float rowH = fontSize + 4.0f * s;
        float gap = rowSpacing.get().floatValue() * s;

        List<Module> active = new ArrayList<>();
        for (Module m : ModuleManager.INSTANCE.modules()) {
            if (m.isEnabled() && !m.isHidden() && m != this) active.add(m);
        }

        if (sort.get() == Sort.WIDTH) {
            active.sort(Comparator.comparingDouble(
                    (Module m) -> -SkijaUi.boldTextWidth(displayName(m), fontSize)));
        } else {
            active.sort(Comparator.comparing(m -> displayName(m), String.CASE_INSENSITIVE_ORDER));
        }

        if (active.isEmpty()) { updateBounds(4, 4); return; }

        float maxW = 0;
        for (Module m : active) {
            float w = SkijaUi.boldTextWidth(displayName(m), fontSize) + 8.0f * s;
            maxW = Math.max(maxW, w);
        }

        float totalH = active.size() * rowH + (active.size() - 1) * gap;
        updateBounds(maxW, totalH);

        float x = event.width() - maxW - 4.0f;
        float y = 4.0f;
        long now = System.currentTimeMillis();
        int rainbowTick = (int)(now / 50L);

        for (int i = 0; i < active.size(); i++) {
            Module m = active.get(i);
            float ry = y + i * (rowH + gap);

            float progress = anim.merge(m, 0.02f * animSpeed.get().floatValue(),
                    (old, delta) -> Math.min(1.0f, old + delta));
            if (progress <= 0.01f) continue;

            float slide = (1.0f - progress) * 20.0f * s;
            float drawX = x + slide;
            int alpha = Math.round(255 * progress);
            String name = displayName(m);

            int rowColor = rowColor(i, now);

            switch (style.get()) {
                case ACCENT_BAR -> {
                    if (background.get()) {
                        SkijaUi.rounded(event.canvas(), drawX, ry, maxW, rowH, 2.0f * s,
                                withAlpha(0xC00A0A12, alpha));
                    }
                    SkijaUi.rounded(event.canvas(), drawX + maxW - 2.0f * s, ry, 2.0f * s, rowH,
                            1.0f * s, withAlpha(rowColor, alpha));
                    drawText(event.canvas(), name, drawX + 4.0f * s, ry, rowH, fontSize,
                            withAlpha(0xFFFFFFFF, alpha), s);
                }
                case PILLS -> {
                    float pillW = SkijaUi.boldTextWidth(name, fontSize) + 10.0f * s;
                    SkijaUi.rounded(event.canvas(), drawX, ry, pillW, rowH, rowH / 2,
                            withAlpha(0xC00A0A12, alpha));
                    if (outline.get()) {
                        SkijaUi.outline(event.canvas(), drawX, ry, pillW, rowH, rowH / 2,
                                1.0f, withAlpha(rowColor, alpha));
                    }
                    drawText(event.canvas(), name, drawX + 5.0f * s, ry, rowH, fontSize,
                            withAlpha(rowColor, alpha), s);
                }
                case TEXT -> {
                    drawText(event.canvas(), name, drawX, ry, rowH, fontSize,
                            withAlpha(rowColor, alpha), s);
                }
            }
        }
    }

    private String displayName(Module m) {
        String n = m.name();
        if (lowercase.get()) n = n.toLowerCase();
        return n;
    }

    private int rowColor(int index, long now) {
        return switch (colorMode.get()) {
            case ACCENT -> 0xFF7B5FEF;
            case STATIC -> 0xFFFFFFFF;
            case FADE -> {
                float t = (float) (Math.sin(now * 0.001 * colorSpeed.get().floatValue()) * 0.5 + 0.5);
                yield lerpColor(firstColor.get().getRGB(), secondColor.get().getRGB(), t);
            }
            case GRADIENT -> {
                float t = Math.min(1.0f, index * colorSpread.get().floatValue() / 100f);
                yield lerpColor(firstColor.get().getRGB(), secondColor.get().getRGB(), t);
            }
            case RAINBOW -> {
                int hue = (int) ((now * 0.05 * colorSpeed.get().floatValue() + index * colorSpread.get().floatValue()) % 360);
                yield java.awt.Color.HSBtoRGB(hue / 360f, 0.7f, 1f) | 0xFF000000;
            }
            case CATEGORY -> 0xFF7B5FEF;
        };
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private void drawText(Canvas canvas, String text, float x, float y, float rowH,
                          float fontSize, int color, float s) {
        float ty = y + (rowH - fontSize) / 2;
        if (textShadow.get() && style.get() == Style.TEXT) {
            SkijaUi.boldTextShadow(canvas, text, x, ty, fontSize, color, fontSize);
        } else {
            SkijaUi.boldText(canvas, text, x, ty, fontSize, color);
        }
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }
}
