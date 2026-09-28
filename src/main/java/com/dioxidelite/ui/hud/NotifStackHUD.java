package com.dioxidelite.ui.hud;

import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.notification.NotificationManager;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.setting.settings.BooleanSetting;
import io.github.humbleui.skija.Canvas;

import java.util.List;

/**
 * modern notification stack: slides in from the right edge, rounded dark
 * panel with a left accent stripe, title + description, and a bottom progress
 * bar that drains as the notification ages.
 */
public final class NotifStackHUD extends EpsilonHudModule {

    public static final NotifStackHUD INSTANCE = new NotifStackHUD();

    public enum Anchor { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT }

    private static final float W = 168.0f;
    private static final float H = 32.0f;
    private static final float GAP = 4.0f;

    public final DoubleSetting scale = add(new DoubleSetting("Scale", 0.85, 0.5, 2.0, 0.05));
    public final EnumSetting<Anchor> anchor = add(new EnumSetting<>("Anchor", Anchor.TOP_RIGHT));
    public final IntSetting duration = add(new IntSetting("Hold", 3000, 500, 10000, 100));
    public final IntSetting maxVisible = add(new IntSetting("Max Stack", 5, 1, 10, 1));
    public final BooleanSetting progressBar = add(new BooleanSetting("Progress Bar", true));
    public final BooleanSetting subtitle = add(new BooleanSetting("Subtitle", true));

    private NotifStackHUD() {
        super("Notification Stack", Category.HUD, 0, 0, W, H);
        setEnabled(true);
    }

    @Override
    protected void renderHud(Render2DEvent event) {
        float s = scale.get().floatValue();
        float w = W * s;
        float h = H * s;
        float gap = GAP * s;

        List<NotificationManager.Entry> entries = NotificationManager.INSTANCE.visible(
                duration.get() + 300, maxVisible.get());
        if (entries.isEmpty()) { updateBounds(4, 4); return; }

        updateBounds(w, entries.size() * (h + gap));

        float x = switch (anchor.get()) {
            case TOP_RIGHT, BOTTOM_RIGHT -> event.width() - w - 4.0f;
            case TOP_LEFT, BOTTOM_LEFT -> 4.0f;
        };
        float y = switch (anchor.get()) {
            case TOP_RIGHT, TOP_LEFT -> 4.0f;
            case BOTTOM_RIGHT, BOTTOM_LEFT -> event.height() - entries.size() * (h + gap) - 4.0f;
        };

        long now = System.currentTimeMillis();
        for (int i = 0; i < entries.size(); i++) {
            NotificationManager.Entry e = entries.get(i);
            float ny = y + i * (h + gap);

            float age = (now - e.visibleAt());
            float total = duration.get() + 300;
            float progress = 1.0f - age / total;

            // Slide-in: first 200ms
            float slideT = Math.min(1.0f, age / 200.0f);
            float slideE = easeOut(slideT);
            float slideX = (1.0f - slideE) * (anchor.get() == Anchor.TOP_RIGHT || anchor.get() == Anchor.BOTTOM_RIGHT ? w : -w);

            // Exit fade: last 200ms
            float fade = age > duration.get() ? Math.max(0, 1.0f - (age - duration.get()) / 300.0f) : 1.0f;
            int alpha = Math.round(255 * slideE * fade);

            float dx = x + slideX;

            // Panel background
            SkijaUi.rounded(event.canvas(), dx, ny, w, h, 4.0f * s,
                    withAlpha(0xE014141C, alpha));

            // Left accent stripe
            int accent = switch (e.type()) {
                case SUCCESS -> 0xFF7FE0A8;
                case WARNING -> 0xFFFFC857;
                case ERROR -> 0xFFFF6B6B;
                case INFO -> 0xFF8C6CE0;
            };
            SkijaUi.rounded(event.canvas(), dx, ny, 3.0f * s, h, 1.5f * s, withAlpha(accent, alpha));

            // Title
            float titleSize = 9.0f * s;
            SkijaUi.boldText(event.canvas(), e.title(), dx + 8.0f * s, ny + 5.0f * s,
                    titleSize, withAlpha(0xFFFFFFFF, alpha));

            // Description
            float descSize = 7.5f * s;
            String desc = e.message();
            float maxDescW = w - 14.0f * s;
            if (SkijaUi.textWidth(desc, descSize) > maxDescW) {
                while (desc.length() > 1 && SkijaUi.textWidth(desc + "...", descSize) > maxDescW) {
                    desc = desc.substring(0, desc.length() - 1);
                }
                desc += "...";
            }
            SkijaUi.text(event.canvas(), desc, dx + 8.0f * s, ny + 17.0f * s,
                    descSize, withAlpha(0xFFB0B0C0, alpha));

            // Progress bar
            if (progressBar.get()) {
                float barW = (w - 10.0f * s) * Math.max(0, progress);
                SkijaUi.rounded(event.canvas(), dx + 5.0f * s, ny + h - 3.0f * s,
                        barW, 1.5f * s, 0.75f * s, withAlpha(accent, alpha));
            }
        }
    }

    private static float easeOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }
}
