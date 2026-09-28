package com.dioxidelite.ui.clickgui;

import com.dioxidelite.render.SkijaUi;
import io.github.humbleui.skija.Canvas;

/**
 * Decoration for ClickGUI module rows: a rounded card with a 1 px edge ("包边") plus a faint
 * particle glow drifting around it.
 *
 * <p>Particles are deliberately subtle - four small dots per row, low alpha, slow drift - so a full
 * module list stays readable and cheap to draw. Motion is driven by a shared clock (both ClickGUI
 * screens animate identically) and seeded from the module name, so the pattern is stable per module
 * instead of flickering frame to frame.</p>
 */
public final class GuiEffects {

    /** Particles drawn per row (kept small on purpose - this is a faint accent, not fireworks). */
    private static final int PARTICLES = 4;

    private static float clock;

    private GuiEffects() {
    }

    /** Advances the shared animation clock; call once per frame with the frame delta in seconds. */
    public static void advance(float delta) {
        if (delta > 0.0F && delta < 0.5F) {
            clock += delta;
        }
        if (clock > 4096.0F) {
            clock -= 4096.0F;
        }
    }

    /** Standard ease-out curve used for expand/reveal animations. */
    public static float easeOut(float progress) {
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        float inverse = 1.0F - p;
        return 1.0F - inverse * inverse * inverse;
    }

    /**
     * Draws the rounded card, its 1 px edge and the faint particle glow for one module row.
     *
     * @param cardColor   card fill (mode/theme dependent)
     * @param borderColor rounded edge colour
     * @param glowColor   particle colour, usually the module's category colour
     * @param seed        stable per-module seed (use {@code module.name().hashCode()})
     * @param energy      0..1 - enabled rows strongest, hovered next, idle rows faintest
     * @param alpha       surrounding panel alpha (fades the whole effect)
     */
    public static void moduleAura(Canvas canvas, float x, float y, float width, float height,
                                  float radius, int cardColor, int borderColor, int glowColor,
                                  int seed, float energy, float alpha) {
        float a = Math.max(0.0F, Math.min(1.0F, alpha));
        float e = Math.max(0.0F, Math.min(1.0F, energy));
        if (a <= 0.004F) {
            return;
        }
        // rounded card + wrapped 1px edge
        SkijaUi.rounded(canvas, x, y, width, height, radius, multiplyAlpha(cardColor, a));
        SkijaUi.outline(canvas, x, y, width, height, radius, 1.0F,
                multiplyAlpha(borderColor, a * (0.10F + 0.14F * e)));

        // faint inner edge glow - drawn INSIDE the card so nothing pokes out of the panel
        if (width > 8.0F && height > 6.0F) {
            SkijaUi.outline(canvas, x + 0.7F, y + 0.7F, width - 1.4F, height - 1.4F,
                    Math.max(1.0F, radius - 0.7F), 1.0F,
                    multiplyAlpha(0xFFFFFFFF, a * (0.05F + 0.07F * e)));
        }

        float seedF = (Math.abs(seed) % 997) * 0.113F;
        for (int index = 0; index < PARTICLES; index++) {
            float speed = 0.16F + 0.05F * (index % 3);
            float phase = (clock * speed + seedF + index * 0.271F) % 1.0F;
            float px = x + 3.0F + phase * Math.max(1.0F, width - 6.0F);
            boolean top = ((index + Math.abs(seed)) & 1) == 0;
            float bob = (float) Math.sin(clock * 0.9F + index * 1.9F + seedF) * 0.8F;
            float py = top ? y + 1.0F + bob : y + height - 1.0F - bob;
            if (py < y || py > y + height - 1.0F) {
                py = y + 1.0F;
            }
            float size = 0.55F + 0.35F * Math.abs((float) Math.sin(clock * 0.8F + index * 2.3F + seedF));
            float twinkle = 0.45F + 0.55F * Math.abs((float) Math.sin(clock * 1.5F + index * 1.7F));
            int color = multiplyAlpha(glowColor, a * twinkle * 0.5F * (0.35F + 0.5F * e));
            SkijaUi.rounded(canvas, px, py, size * 2.0F, size * 2.0F, size, color);
        }
    }

    /** A single rounded line segment (capsule) from (x1,y1) to (x2,y2). */
    public static void stroke(Canvas canvas, float x1, float y1, float x2, float y2,
                              float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.01F || thickness <= 0.01F) {
            return;
        }
        float half = thickness * 0.5F;
        canvas.save();
        canvas.translate((x1 + x2) * 0.5F, (y1 + y2) * 0.5F);
        canvas.rotate((float) Math.toDegrees(Math.atan2(dy, dx)));
        SkijaUi.rounded(canvas, -length * 0.5F - half, -half, length + thickness, thickness,
                half, color);
        canvas.restore();
    }

    /**
     * modern expand chevron: two rounded strokes that rotate as the module expands.
     *
     * @param halfWidth  half of the chevron's horizontal span
     * @param drop       how far the tip reaches down from the ends
     * @param rotation   0 = pointing down, 180 = pointing up (fully expanded)
     * @param hoverAlpha 0..1, draws a soft rounded plate behind the chevron when hovered
     */
    public static void chevron(Canvas canvas, float centreX, float centreY, float halfWidth,
                               float drop, float rotation, float thickness, int color,
                               float hoverAlpha) {
        if (hoverAlpha > 0.01F) {
            float plate = Math.max(7.5F, halfWidth + drop * 1.6F);
            SkijaUi.rounded(canvas, centreX - plate, centreY - plate, plate * 2.0F, plate * 2.0F,
                    plate, multiplyAlpha(0xFFFFFFFF, 0.09F * hoverAlpha));
        }
        canvas.save();
        canvas.translate(centreX, centreY);
        canvas.rotate(rotation);
        stroke(canvas, -halfWidth, -drop, 0.0F, drop, thickness, color);
        stroke(canvas, 0.0F, drop, halfWidth, -drop, thickness, color);
        canvas.restore();
    }

    private static int multiplyAlpha(int color, float factor) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, factor)));
        return (color & 0xFFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }
}
