package com.dioxidelite.ui.screen;

import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.ui.SkijaScreen;
import com.dioxidelite.ui.UiTheme;
import io.github.humbleui.skija.Canvas;

import java.util.ArrayList;
import java.util.List;

/**
 * Skija overlay that replaces themed vanilla widget visuals without touching their
 * input logic.
 *
 * <p>Widgets are collected while the vanilla screen renders and re-painted here, in
 * the Skija pass that runs afterwards. Because that pass is always last, any widget
 * registered here has its vanilla pixels suppressed by a matching mixin — which also
 * means each kind has to carry enough state to be drawn completely: a slider needs its
 * value, a tick box needs its checked state. The one exception is the text field,
 * whose caret, selection and scrolling text stay with Minecraft's own painter.</p>
 *
 * <p>Deliberately no backing panel behind the group: vanilla screens lay their widgets
 * out edge to edge (the options grid alone spans the whole screen), so any slab sized
 * to their bounding box degenerates into a stray rectangle rather than reading as
 * contained content. Each widget carries its own chrome instead.</p>
 */
public final class VanillaButtonOverlay implements SkijaScreen {

    public static final VanillaButtonOverlay INSTANCE = new VanillaButtonOverlay();

    private static final int KIND_BUTTON = 0;
    private static final int KIND_SLIDER = 1;
    private static final int KIND_CHECKBOX = 2;
    private static final int KIND_FIELD = 3;
    private static final int KIND_SWITCH = 4;

    private static final List<Widget> PENDING_WIDGETS = new ArrayList<>();
    private static volatile List<Widget> renderedWidgets = List.of();

    private VanillaButtonOverlay() {
    }

    public static synchronized void beginFrame() {
        PENDING_WIDGETS.clear();
    }

    public static synchronized void add(int x, int y, int width, int height, String label,
                                        boolean hovered, boolean active, float alpha) {
        push(KIND_BUTTON, x, y, width, height, label, hovered, active, alpha, 0.0F, false);
    }

    public static synchronized void addSlider(int x, int y, int width, int height, String label,
                                              boolean hovered, boolean active, float alpha,
                                              float value) {
        push(KIND_SLIDER, x, y, width, height, label, hovered, active, alpha, value, false);
    }

    public static synchronized void addCheckbox(int x, int y, int width, int height, String label,
                                                boolean hovered, boolean active, float alpha,
                                                boolean checked) {
        push(KIND_CHECKBOX, x, y, width, height, label, hovered, active, alpha, 0.0F, checked);
    }

    /** Frame only — see the class comment for why the field keeps its vanilla painter. */
    public static synchronized void addField(int x, int y, int width, int height,
                                             boolean focused, boolean active, float alpha) {
        push(KIND_FIELD, x, y, width, height, "", false, active, alpha, 0.0F, focused);
    }

    /**
     * A boolean {@code CycleButton}: same glass row as every other button, plus a
     * trailing switch. The message already spells the state out, but on a settings
     * screen a row of "…: On / …: Off" reads far slower than a column of switches.
     */
    public static synchronized void addSwitch(int x, int y, int width, int height, String label,
                                              boolean hovered, boolean active, float alpha,
                                              boolean on) {
        push(KIND_SWITCH, x, y, width, height, label, hovered, active, alpha, 0.0F, on);
    }

    private static void push(int kind, int x, int y, int width, int height, String label,
                             boolean hovered, boolean active, float alpha, float value,
                             boolean checked) {
        if (width <= 0 || height <= 0) {
            return;
        }
        PENDING_WIDGETS.add(new Widget(kind, x, y, width, height,
                label == null ? "" : label, hovered, active,
                Math.max(0.0F, Math.min(1.0F, alpha)), value, checked));
    }

    public static synchronized void endFrame() {
        renderedWidgets = List.copyOf(PENDING_WIDGETS);
    }

    @Override
    public void renderSkija(Canvas canvas) {
        List<Widget> widgets = renderedWidgets;
        if (widgets.isEmpty()) {
            return;
        }
        for (Widget widget : widgets) {
            switch (widget.kind) {
                case KIND_SLIDER -> drawSlider(canvas, widget);
                case KIND_CHECKBOX -> drawCheckbox(canvas, widget);
                case KIND_FIELD -> drawField(canvas, widget);
                case KIND_SWITCH -> drawSwitch(canvas, widget);
                default -> drawButton(canvas, widget);
            }
        }
    }

    // ------------------------------------------------------------------ widgets

    private static void drawButton(Canvas canvas, Widget button) {
        int opacity = Math.round(button.alpha * 255.0F);
        int accent = UiTheme.accent();
        boolean enabled = button.active;
        boolean hovered = enabled && button.hovered;

        drawRowSurface(canvas, button.x, button.y, button.width, button.height,
                opacity, accent, enabled, hovered);

        float fontSize = Math.max(7.0F, Math.min(9.0F, button.height * 0.43F));
        String label = fit(button.label, Math.max(0.0F, button.width - 16.0F), fontSize);
        float textWidth = SkijaUi.boldTextWidth(label, fontSize);
        int textColor = !enabled ? UiControls.TEXT_FAINT
                : hovered ? UiControls.TEXT : UiControls.TEXT_MUTED;
        SkijaUi.boldText(canvas, label, button.x + (button.width - textWidth) * 0.5F,
                button.y, button.height, scale(textColor, opacity), fontSize);
    }

    /**
     * The shared glass row. Every widget that keeps a full-width footprint (buttons,
     * switches) sits on this, so a settings screen does not end up with two
     * competing row treatments.
     */
    private static void drawRowSurface(Canvas canvas, float x, float y, float width,
                                       float height, int opacity, int accent,
                                       boolean enabled, boolean hovered) {
        float radius = UiControls.RADIUS_SMALL;
        int stroke = hovered
                ? UiTheme.withAlpha(accent, 150)
                : enabled ? UiControls.STROKE_STRONG : UiControls.STROKE;
        int fill = !enabled ? 0x1A101720 : hovered ? UiControls.CARD_HOVER : UiControls.CARD;

        SkijaUi.rounded(canvas, x, y, width, height, radius, scale(stroke, opacity));
        SkijaUi.rounded(canvas, x + 1.0F, y + 1.0F, Math.max(0.0F, width - 2.0F),
                Math.max(0.0F, height - 2.0F), Math.max(0.0F, radius - 1.0F),
                scale(fill, opacity));
        if (enabled) {
            SkijaUi.rounded(canvas, x + 2.0F, y + 1.0F, Math.max(0.0F, width - 4.0F), 1.0F,
                    0.5F, scale(UiControls.HIGHLIGHT, opacity));
        }
        if (hovered) {
            SkijaUi.rounded(canvas, x + 2.0F, y + 4.0F, 3.0F,
                    Math.max(0.0F, height - 8.0F), 1.5F,
                    UiTheme.withAlpha(accent, opacity));
        }
    }

    /** Glass row with a trailing pill switch, used for boolean option buttons. */
    private static void drawSwitch(Canvas canvas, Widget toggle) {
        float x = toggle.x;
        float y = toggle.y;
        float width = toggle.width;
        float height = toggle.height;
        int opacity = Math.round(toggle.alpha * 255.0F);
        int accent = UiTheme.accent();
        boolean enabled = toggle.active;
        boolean hovered = enabled && toggle.hovered;
        boolean on = toggle.checked;

        drawRowSurface(canvas, x, y, width, height, opacity, accent, enabled, hovered);

        float pillWidth = clamp(height * 1.05F, 18.0F, 30.0F);
        float pillHeight = clamp(height - 10.0F, 8.0F, 14.0F);
        float pillX = x + width - pillWidth - 5.0F;
        float pillY = y + (height - pillHeight) * 0.5F;

        int track = on
                ? (enabled ? UiTheme.withAlpha(accent, hovered ? 240 : 208) : 0x5530363D)
                : 0x4A0C1116;
        SkijaUi.rounded(canvas, pillX, pillY, pillWidth, pillHeight, pillHeight * 0.5F,
                scale(track, opacity));
        SkijaUi.outline(canvas, pillX + 0.5F, pillY + 0.5F, pillWidth - 1.0F, pillHeight - 1.0F,
                pillHeight * 0.5F, 1.0F,
                scale(on ? 0x38FFFFFF : hovered ? 0x5CFFFFFF : 0x38FFFFFF, opacity));

        float knob = pillHeight - 2.0F;
        float knobX = on ? pillX + pillWidth - knob - 1.0F : pillX + 1.0F;
        SkijaUi.rounded(canvas, knobX, pillY + 1.0F, knob, knob, knob * 0.5F,
                scale(enabled ? 0xFFFFFFFF : 0xFF9AA2B0, opacity));

        // Text stays centred in the space the switch leaves free.
        float fontSize = Math.max(7.0F, Math.min(9.0F, height * 0.43F));
        float areaWidth = Math.max(0.0F, pillX - x - 4.0F);
        String label = fit(toggle.label, Math.max(0.0F, areaWidth - 12.0F), fontSize);
        float textWidth = SkijaUi.boldTextWidth(label, fontSize);
        int textColor = !enabled ? UiControls.TEXT_FAINT
                : hovered ? UiControls.TEXT : UiControls.TEXT_MUTED;
        SkijaUi.boldText(canvas, label, x + (areaWidth - textWidth) * 0.5F, y, height,
                scale(textColor, opacity), fontSize);
    }

    /**
     * A real track + fill + knob. Sliders used to be routed through the button
     * painter, which left the options screen with controls showing no indication
     * of where the value actually sat.
     */
    private static void drawSlider(Canvas canvas, Widget slider) {
        float x = slider.x;
        float y = slider.y;
        float width = slider.width;
        float height = slider.height;
        int opacity = Math.round(slider.alpha * 255.0F);
        int accent = UiTheme.accent();
        boolean enabled = slider.active;
        boolean hovered = enabled && slider.hovered;
        float value = clamp(slider.value, 0.0F, 1.0F);

        float trackHeight = clamp(height - 10.0F, 5.0F, 11.0F);
        float centerY = y + height * 0.5F;
        float trackY = centerY - trackHeight * 0.5F;
        float trackRadius = trackHeight * 0.5F;

        SkijaUi.rounded(canvas, x, trackY, width, trackHeight, trackRadius,
                scale(enabled ? 0x66090C10 : 0x3C0C1116, opacity));
        SkijaUi.outline(canvas, x + 0.5F, trackY + 0.5F, width - 1.0F, trackHeight - 1.0F,
                trackRadius, 1.0F, scale(0x22FFFFFF, opacity));

        float filled = width * value;
        if (filled > 1.0F) {
            int fillColor = enabled ? UiTheme.withAlpha(accent, hovered ? 240 : 205) : 0x55262B31;
            SkijaUi.rounded(canvas, x, trackY, filled, trackHeight, trackRadius,
                    scale(fillColor, opacity));
        }

        float knob = trackHeight + 4.0F;
        float knobX = clamp(x + filled, x + knob * 0.5F, x + width - knob * 0.5F);
        SkijaUi.rounded(canvas, knobX - knob * 0.5F, centerY - knob * 0.5F + 1.0F, knob, knob,
                knob * 0.5F, scale(0xA0000000, opacity));
        SkijaUi.rounded(canvas, knobX - knob * 0.5F, centerY - knob * 0.5F, knob, knob,
                knob * 0.5F, scale(enabled ? 0xFFF2F5F9 : 0xFF6C747E, opacity));
        if (enabled) {
            SkijaUi.outline(canvas, knobX - knob * 0.5F, centerY - knob * 0.5F, knob, knob,
                    knob * 0.5F, 1.0F, scale(hovered ? 0x66FFFFFF : 0x33FFFFFF, opacity));
        }

        float fontSize = Math.max(7.0F, Math.min(9.0F, height * 0.43F));
        String label = fit(slider.label, Math.max(0.0F, width - 20.0F), fontSize);
        float textWidth = SkijaUi.boldTextWidth(label, fontSize);
        // The label rides on the track, so it needs a shadow to stay legible over
        // whichever half of the fill it happens to cross.
        SkijaUi.boldTextShadow(canvas, label, x + (width - textWidth) * 0.5F, y, height,
                scale(enabled ? UiControls.TEXT : UiControls.TEXT_FAINT, opacity), fontSize);
    }

    /**
     * Cancelling the vanilla widget chrome also cancels the tick box, which left
     * every checkbox in the options screens looking identical whether it was on
     * or off.
     */
    private static void drawCheckbox(Canvas canvas, Widget checkbox) {
        float x = checkbox.x;
        float y = checkbox.y;
        float width = checkbox.width;
        float height = checkbox.height;
        int opacity = Math.round(checkbox.alpha * 255.0F);
        int accent = UiTheme.accent();
        boolean enabled = checkbox.active;
        boolean hovered = enabled && checkbox.hovered;

        float box = clamp(height - 6.0F, 9.0F, 14.0F);
        float boxX = x + 3.0F;
        float boxY = y + (height - box) * 0.5F;
        float boxRadius = 3.5F;

        if (checkbox.checked) {
            SkijaUi.rounded(canvas, boxX, boxY, box, box, boxRadius,
                    scale(enabled ? UiTheme.withAlpha(accent, 235) : 0x6630363D, opacity));
            SkijaUi.outline(canvas, boxX + 0.5F, boxY + 0.5F, box - 1.0F, box - 1.0F,
                    boxRadius, 1.0F, scale(enabled ? 0x66FFFFFF : 0x22FFFFFF, opacity));
            int tick = enabled ? UiControls.onAccent(accent) : 0x99C6CECC;
            SkijaUi.line(canvas, boxX + box * 0.26F, boxY + box * 0.53F,
                    boxX + box * 0.44F, boxY + box * 0.71F, 1.7F, scale(tick, opacity));
            SkijaUi.line(canvas, boxX + box * 0.44F, boxY + box * 0.71F,
                    boxX + box * 0.76F, boxY + box * 0.30F, 1.7F, scale(tick, opacity));
        } else {
            SkijaUi.rounded(canvas, boxX, boxY, box, box, boxRadius, scale(0x14FFFFFF, opacity));
            SkijaUi.outline(canvas, boxX + 0.5F, boxY + 0.5F, box - 1.0F, box - 1.0F,
                    boxRadius, 1.0F, scale(hovered ? 0x5CFFFFFF : 0x38FFFFFF, opacity));
        }

        float fontSize = Math.max(7.0F, Math.min(9.0F, height * 0.43F));
        float textX = boxX + box + 7.0F;
        String label = fit(checkbox.label, Math.max(0.0F, x + width - textX - 4.0F), fontSize);
        int textColor = !enabled ? UiControls.TEXT_FAINT
                : hovered ? UiControls.TEXT : UiControls.TEXT_MUTED;
        SkijaUi.boldText(canvas, label, textX, y, height, scale(textColor, opacity), fontSize);
    }

    /** Frame only: the caret, selection and text stay with Minecraft's own painter. */
    private static void drawField(Canvas canvas, Widget field) {
        float x = field.x;
        float y = field.y;
        float width = field.width;
        float height = field.height;
        int opacity = Math.round(field.alpha * 255.0F);
        int accent = UiTheme.accent();
        boolean focused = field.checked;
        float radius = UiControls.RADIUS_SMALL;

        if (focused) {
            SkijaUi.rounded(canvas, x + 1.0F, y + 1.0F, Math.max(0.0F, width - 2.0F),
                    Math.max(0.0F, height - 2.0F), Math.max(0.0F, radius - 1.0F),
                    scale(UiTheme.withAlpha(accent, 26), opacity));
        }
        // Straddles the vanilla hairline, which sits half a screen unit further out.
        SkijaUi.outline(canvas, x - 0.5F, y - 0.5F, width + 1.0F, height + 1.0F,
                radius + 0.5F, 2.0F,
                scale(focused ? UiTheme.withAlpha(accent, 215) : 0x2EFFFFFF, opacity));
    }

    /** Multiplies a colour's alpha by the widget's own fade level. */
    private static int scale(int color, int opacity) {
        int alpha = ((color >>> 24) & 0xFF) * opacity / 255;
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String fit(String text, float maxWidth, float fontSize) {
        if (SkijaUi.boldTextWidth(text, fontSize) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int end = text.length();
        while (end > 0 && SkijaUi.boldTextWidth(text.substring(0, end) + suffix, fontSize) > maxWidth) {
            end = text.offsetByCodePoints(end, -1);
        }
        return end == 0 ? "" : text.substring(0, end) + suffix;
    }

    private record Widget(int kind, float x, float y, float width, float height, String label,
                          boolean hovered, boolean active, float alpha, float value,
                          boolean checked) {
    }
}
