package com.dioxidelite.ui.hud;

import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.ui.UiTheme;
import io.github.humbleui.skija.Canvas;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;
import java.util.Comparator;

/**
 * Real proximity-driven Target HUD. It deliberately does not require an attack/automation module.
 */
public final class TargetHud extends EpsilonHudModule {
    public static final TargetHud INSTANCE = new TargetHud();

    public final DoubleSetting searchRange = add(new DoubleSetting("Search Range", 6.0, 1.0, 32.0, 0.5));
    public final DoubleSetting scale = add(new DoubleSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    public final BooleanSetting showHealth = add(new BooleanSetting("Show Health", true));
    public final BooleanSetting showDistance = add(new BooleanSetting("Show Distance", true));
    public final ColorSetting background = add(new ColorSetting("Background", new Color(12, 16, 24, 220), true));
    public final ColorSetting accent = add(new ColorSetting("Accent", new Color(92, 190, 255, 255), true));

    private Player target;
    private float visibility;
    private long lastFrame;

    private TargetHud() { super("Target HUD", 8, 55, 150, 46); }

    @Override
    protected void renderHud(Render2DEvent event) {
        updateTarget();
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.05f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        visibility += (((target != null) ? 1f : 0f) - visibility) * Math.min(1f, dt * 14f);
        if (visibility < 0.01f || target == null) return;

        float s = scale.get().floatValue();
        float width = 150f * s;
        float height = 46f * s;
        float x = renderX(event, width), y = renderY(event, height);
        updateBounds(width, height);
        int alpha = Math.round(255f * visibility * opacityFactor());
        int bg = (alpha << 24) | (background.argb() & 0x00FFFFFF);
        HudRenderUtil.coloredSurface(event.canvas(), x, y, width, height, 9f * s, bg, HudFusionManager.Edges.NONE);
        String name = target.getName().getString();
        SkijaUi.boldText(event.canvas(), name, x + 9f*s, y + 6f*s, 12f*s,
                (alpha << 24) | (UiTheme.TEXT & 0x00FFFFFF), 8.5f*s);
        float hp = Math.max(0f, Math.min(1f, target.getHealth() / Math.max(1f, target.getMaxHealth())));
        if (showHealth.get()) {
            HudRenderUtil.progress(event.canvas(), x + 9f*s, y + 23f*s, width - 18f*s, 5f*s, hp,
                    (alpha << 24) | (accent.argb() & 0x00FFFFFF));
            String health = String.format(java.util.Locale.ROOT, "%.1f HP", target.getHealth());
            SkijaUi.text(event.canvas(), health, x + 9f*s, y + 30f*s, 10f*s,
                    (alpha << 24) | (UiTheme.TEXT_FAINT & 0x00FFFFFF), 6.5f*s);
        }
        if (showDistance.get()) {
            String distance = String.format(java.util.Locale.ROOT, "%.1fm", mc.player.distanceTo(target));
            float tw = SkijaUi.textWidth(distance, 6.5f*s);
            SkijaUi.text(event.canvas(), distance, x + width - 9f*s - tw, y + 30f*s, 10f*s,
                    (alpha << 24) | (UiTheme.TEXT_FAINT & 0x00FFFFFF), 6.5f*s);
        }
    }

    private void updateTarget() {
        if (noPlayer()) { target = null; return; }
        double maxSq = searchRange.get() * searchRange.get();
        target = mc.level.players().stream()
                .filter(p -> p != mc.player && !p.isRemoved() && !p.isSpectator())
                .filter(p -> p.distanceToSqr(mc.player) <= maxSq)
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player)))
                .orElse(null);
    }

    @Override public int editorColor() { return UiTheme.accent(); }
    @Override public boolean supportsHudFusion() { return true; }
    @Override public boolean hudFusionBorderEnabled() { return true; }
    @Override public boolean hudFusionBackgroundEnabled() { return true; }
}
