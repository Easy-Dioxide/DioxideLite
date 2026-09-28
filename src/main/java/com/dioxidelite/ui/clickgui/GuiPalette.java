package com.dioxidelite.ui.clickgui;

import com.dioxidelite.render.SkijaUi;
import io.github.humbleui.skija.Canvas;

/**
 * modern design system for the DioxideLite ClickGUI.
 *
 * <p>Visual language ported from the reference client's right-shift ClickGUI (the panel opened by
 * {@code UiSupport_560}/{@code UiSupport_543} in the the reference client source): a single accent-seeded
 * palette, dark accent-tinted glass surfaces, a thin light accent edge, low-alpha accent
 * highlights for hover/selection and generous rounding. Geometry and typography tokens follow the
 * reference reference values scaled down to DioxideLite's denser panels.</p>
 *
 * <p>Everything is derived from one seed colour (the existing {@code ClickGui.accent} setting),
 * exactly like reference' "Accent / seed colour" setting drives its whole theme.</p>
 */
public final class GuiPalette {

    // ------------------------------------------------------------------
    // geometry / typography tokens
    // ------------------------------------------------------------------

    /** Corner radius of a full panel (reference uses 28 on its 640x420 sheet). */
    public static final float PANEL_RADIUS = 16.0F;
    /** Corner radius of an inner card / module row. */
    public static final float CARD_RADIUS = 8.0F;
    /** Corner radius of small chips (hex field, keybind badge, swatches). */
    public static final float CHIP_RADIUS = 5.0F;
    /** Border thickness of panels and cards. */
    public static final float BORDER = 1.0F;
    /** Blur level used behind panels (reference {@code ThemeSupport_060} level 2 == 3). */
    public static final int BLUR_LEVEL = 3;

    // hover / selection alphas taken from ThemeSupport_059 (0.08 hover, 0.10-0.16 fills, 0.38 mid)
    private static final float ALPHA_HOVER = 0.085F;
    private static final float ALPHA_ACTIVE = 0.16F;
    private static final float ALPHA_EDGE = 0.55F;
    private static final float ALPHA_EDGE_BRIGHT = 0.78F;
    private static final float ALPHA_PANEL = 0.84F;
    private static final float ALPHA_INNER = 0.72F;
    private static final float ALPHA_TEXT = 0.96F;
    private static final float ALPHA_TEXT_DIM = 0.62F;
    private static final float ALPHA_TEXT_FAINT = 0.34F;
    private static final float ALPHA_TRACK = 0.12F;
    private static final float ALPHA_SCROLL = 0.55F;
    private static final float ALPHA_BACKDROP = 0.34F;

    /** reference default seed (0xFF6750A4) used until the accent setting is pushed in. */
    private static int seed = 0xFF6750A4;
    private static float seedHue = 256.4F;
    private static float seedSat = 0.344F;

    private GuiPalette() {
    }

    // ------------------------------------------------------------------
    // palette
    // ------------------------------------------------------------------

    /**
     * Re-seeds the whole palette from an accent colour.
     *
     * @param argb accent colour (alpha ignored)
     */
    public static void seed(int argb) {
        int rgb = argb & 0xFFFFFF;
        if (rgb == (seed & 0xFFFFFF)) {
            return;
        }
        seed = 0xFF000000 | rgb;
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta < 1.0E-5F) {
            hue = 0.0F;
        } else if (max == r) {
            hue = 60.0F * (((g - b) / delta) % 6.0F);
        } else if (max == g) {
            hue = 60.0F * (((b - r) / delta) + 2.0F);
        } else {
            hue = 60.0F * (((r - g) / delta) + 4.0F);
        }
        if (hue < 0.0F) {
            hue += 360.0F;
        }
        float lightness = (max + min) * 0.5F;
        float saturation = delta < 1.0E-5F ? 0.0F : delta / (1.0F - Math.abs(2.0F * lightness - 1.0F));
        seedHue = hue;
        seedSat = Math.max(0.18F, Math.min(1.0F, saturation));
    }

    /** The seed colour itself. */
    public static int accent() {
        return seed;
    }

    /** Accent with an explicit alpha (0..255). */
    public static int accent(int alpha) {
        return withAlpha(seed, alpha);
    }

    private static float alpha(float fraction) {
        return Math.max(0.0F, Math.min(1.0F, fraction)) * 255.0F;
    }

    // ------------------------------------------------------------------
    // Material-3 tonal palette, seeded by the accent colour
    //
    // The reference GUI builds its colours from one seed with the Material 3 tonal-palette model
    // (hue + chroma sampled at fixed tones). The tones below are the ones the ClickGUI actually
    // uses: surfaceContainer for the panel, surfaceContainerHigh for cards, primary for
    // highlights, outline for strokes.
    // ------------------------------------------------------------------

    /** A tonal colour: seed hue, chroma = seed saturation x {@code chromaScale}, MD3 tone. */
    private static int tonal(float chromaScale, float tone) {
        return hsla(seedHue, seedSat * chromaScale, tone, 1.0F);
    }

    /** Accent highlight (MD3 primary, tone ~81). */
    public static int primary() {
        return tonal(1.10F, 0.81F);
    }

    /** Text/icon colour on top of the accent. */
    public static int onPrimary() {
        return tonal(0.95F, 0.29F);
    }

    /** Filled accent container (MD3 primaryContainer). */
    public static int primaryContainer() {
        return tonal(0.72F, 0.38F);
    }

    /** Soft accent surface used for "enabled" rows (MD3 secondaryContainer). */
    public static int secondaryContainer() {
        return withAlpha(tonal(0.35F, 0.30F), alpha(0.92F));
    }

    // surfaces ---------------------------------------------------------
    /** Backdrop scrim (MD3 scrim = black, alpha animated by the caller). */
    public static int backdrop(float alphaMultiplier) {
        return withAlpha(0xFF000000, alpha(ALPHA_BACKDROP * alphaMultiplier));
    }

    /** Panel body (MD3 surfaceContainer). */
    public static int panel() {
        return withAlpha(tonal(0.09F, 0.115F), alpha(0.94F));
    }

    /** Cards / rows inside a panel (MD3 surfaceContainerHigh). */
    public static int panelInner() {
        return withAlpha(tonal(0.10F, 0.155F), alpha(0.90F));
    }

    /** Highest elevation surface (MD3 surfaceContainerHighest). */
    public static int section() {
        return withAlpha(tonal(0.10F, 0.195F), alpha(0.86F));
    }

    /** Panel header band. */
    public static int header() {
        return withAlpha(tonal(0.10F, 0.145F), alpha(0.92F));
    }

    /** MD3 outline - the stroke colour used by every GUI surface. */
    public static int outline() {
        return withAlpha(tonal(0.16F, 0.47F), alpha(0.95F));
    }

    /** MD3 outlineVariant - separators and faint strokes. */
    public static int outlineVariant() {
        return withAlpha(tonal(0.14F, 0.26F), alpha(0.85F));
    }

    /** Stroke colour (alias of {@link #outline()}). */
    public static int edge() {
        return outline();
    }

    /** Focused/accent stroke (MD3 primary). */
    public static int edgeBright() {
        return primary();
    }

    /** Hover overlay: onSurface at 8 % (reference UiSupport_545). */
    public static int hover() {
        return withAlpha(onSurface(), alpha(ALPHA_HOVER));
    }

    /** Selected/pressed plate: secondaryContainer. */
    public static int active() {
        return secondaryContainer();
    }

    // text -------------------------------------------------------------
    public static int text() {
        return tonal(0.05F, 0.90F);
    }

    public static int textDim() {
        return withAlpha(tonal(0.07F, 0.78F), alpha(0.92F));
    }

    public static int textFaint() {
        return withAlpha(tonal(0.07F, 0.62F), alpha(0.62F));
    }

    public static int textOff() {
        return withAlpha(tonal(0.07F, 0.52F), alpha(0.48F));
    }

    public static int onSurface() {
        return tonal(0.05F, 0.90F);
    }

    // tracks / bars ----------------------------------------------------
    /** Unfilled slider/toggle track (MD3 surfaceContainerHighest). */
    public static int track() {
        return tonal(0.10F, 0.22F);
    }

    public static int trackFaint() {
        return withAlpha(tonal(0.10F, 0.22F), alpha(0.55F));
    }

    /** Scrollbar thumb (MD3 outlineVariant at 80 %). */
    public static int scroll() {
        return withAlpha(outlineVariant(), alpha(0.85F));
    }

    public static int white(float fraction) {
        return withAlpha(0xFFFFFFFF, alpha(fraction));
    }

    public static int black(float fraction) {
        return withAlpha(0xFF000000, alpha(fraction));
    }

    // light (daylight) variants ---------------------------------------
    public static int lightPanel() {
        return withAlpha(hsla(seedHue, seedSat * 0.10F, 0.965F, 1.0F), alpha(0.94F));
    }

    public static int lightPanelInner() {
        return withAlpha(hsla(seedHue, seedSat * 0.12F, 0.925F, 1.0F), alpha(0.90F));
    }

    public static int lightHover() {
        return withAlpha(0xFF000000, alpha(0.06F));
    }

    public static int lightText() {
        return hsla(seedHue, seedSat * 0.30F, 0.18F, 1.0F);
    }

    public static int lightTextDim() {
        return hsla(seedHue, seedSat * 0.18F, 0.38F, 1.0F);
    }

    public static int lightTextFaint() {
        return hsla(seedHue, seedSat * 0.12F, 0.56F, 1.0F);
    }

    public static int lightTrack() {
        return hsla(seedHue, seedSat * 0.10F, 0.80F, 1.0F);
    }

    // ------------------------------------------------------------------
    // painters
    // ------------------------------------------------------------------

    /** Draws the reference panel shell: accent-tinted glass, thin bright edge. */
    public static void panelShell(Canvas canvas, float x, float y, float width, float height,
                                 float radius, int fill, int edge, float edgeAlpha) {
        SkijaUi.rounded(canvas, x, y, width, height, radius, fill);
        SkijaUi.outline(canvas, x, y, width, height, radius, BORDER, withAlpha(edge, edgeAlpha));
    }

    /** Draws the panel definition after the surface (reference draws a 2-pass soft shadow). */
    public static void panelShadow(Canvas canvas, float x, float y, float width, float height,
                                   float radius) {
        SkijaUi.dropShadowRounded(canvas, x, y, width, height, radius, 2.0F, 6.0F,
                withAlpha(0xFF000000, alpha(0.35F)));
    }

    /** Draws an reference selection/hover plate behind a row. */
    public static void rowPlate(Canvas canvas, float x, float y, float width, float height,
                                float radius, float hover, float active) {
        float selection = Math.max(active, hover * 0.6F);
        if (selection <= 0.004F) {
            return;
        }
        int color = mix(hover(), active(), active);
        SkijaUi.rounded(canvas, x, y, width, height, radius,
                withAlpha(color, alpha(ALPHA_ACTIVE * active + ALPHA_HOVER * hover)));
        if (active > 0.01F) {
            // accent bar on the leading edge, like the reference selected row
            SkijaUi.rounded(canvas, x + 1.0F, y + height * 0.22F, 1.8F, height * 0.56F, 0.9F,
                    withAlpha(primary(), alpha(0.85F * active)));
        }
    }

    /** reference toggle: rounded track, round knob, accent when on. */
    public static void toggle(Canvas canvas, float x, float y, float width, float height,
                              float progress, int knobColor) {
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        SkijaUi.rounded(canvas, x, y, width, height, height * 0.5F, track());
        if (p > 0.004F) {
            SkijaUi.rounded(canvas, x, y, width, height, height * 0.5F,
                    withAlpha(primary(), alpha(p)));
        }
        float knob = height - 4.0F;
        SkijaUi.rounded(canvas, x + 2.0F + p * (width - knob - 4.0F), y + 2.0F, knob, knob,
                knob * 0.5F, knobColor);
    }

    /** reference slider: 2px track, accent fill, round handle. */
    public static void slider(Canvas canvas, float x, float y, float width, float fraction,
                              int fillColor) {
        float f = Math.max(0.0F, Math.min(1.0F, fraction));
        float trackHeight = Math.max(2.0F, width * 0.006F + 2.0F);
        SkijaUi.rounded(canvas, x, y, width, trackHeight, trackHeight * 0.5F, track());
        if (f > 0.001F) {
            SkijaUi.rounded(canvas, x, y, Math.max(trackHeight, width * f), trackHeight,
                    trackHeight * 0.5F, fillColor);
        }
        float handle = trackHeight + 4.0F;
        float handleX = x + width * f - handle * 0.5F;
        SkijaUi.rounded(canvas, Math.max(x - handle * 0.5F, Math.min(x + width - handle * 0.5F, handleX)),
                y + trackHeight * 0.5F - handle * 0.5F, handle, handle, handle * 0.5F, fillColor);
    }

    /** reference input field / chip. */
    public static void chip(Canvas canvas, float x, float y, float width, float height,
                            boolean focused, float radius, int fill) {
        SkijaUi.rounded(canvas, x, y, width, height, radius, fill);
        SkijaUi.outline(canvas, x, y, width, height, radius, BORDER,
                focused ? withAlpha(primary(), alpha(0.95F)) : withAlpha(outlineVariant(), alpha(0.9F)));
    }

    /** reference scrollbar: thin accent thumb on a faint track. */
    public static void scrollbar(Canvas canvas, float x, float y, float width, float height,
                                 float offset, float visibleFraction, int accent) {
        SkijaUi.rounded(canvas, x, y, width, height, width * 0.5F, trackFaint());
        float fraction = Math.max(0.06F, Math.min(1.0F, visibleFraction));
        float thumb = Math.max(10.0F, height * fraction);
        float travel = Math.max(0.0F, height - thumb);
        float position = y + travel * Math.max(0.0F, Math.min(1.0F, offset));
        SkijaUi.rounded(canvas, x, position, width, thumb, width * 0.5F, accent);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    public static int withAlpha(int argb, float alpha255) {
        int a = Math.max(0, Math.min(255, Math.round(alpha255)));
        return (argb & 0xFFFFFF) | (a << 24);
    }

    public static int mix(int from, int to, float t) {
        float f = Math.max(0.0F, Math.min(1.0F, t));
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * f);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** HSL -> ARGB with an explicit alpha fraction (0..1). */
    private static int hsla(float hue, float saturation, float lightness, float alphaFraction) {
        float h = ((hue % 360.0F) + 360.0F) % 360.0F / 360.0F;
        float s = Math.max(0.0F, Math.min(1.0F, saturation));
        float l = Math.max(0.0F, Math.min(1.0F, lightness));
        float c = (1.0F - Math.abs(2.0F * l - 1.0F)) * s;
        float x = c * (1.0F - Math.abs((h * 6.0F) % 2.0F - 1.0F));
        float m = l - c * 0.5F;
        float r;
        float g;
        float b;
        int sector = (int) (h * 6.0F) % 6;
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0.0F; }
            case 1 -> { r = x; g = c; b = 0.0F; }
            case 2 -> { r = 0.0F; g = c; b = x; }
            case 3 -> { r = 0.0F; g = x; b = c; }
            case 4 -> { r = x; g = 0.0F; b = c; }
            default -> { r = c; g = 0.0F; b = x; }
        }
        int ri = Math.round((r + m) * 255.0F);
        int gi = Math.round((g + m) * 255.0F);
        int bi = Math.round((b + m) * 255.0F);
        int ai = Math.round(Math.max(0.0F, Math.min(1.0F, alphaFraction)) * 255.0F);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
}
