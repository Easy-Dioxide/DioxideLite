package com.dioxidelite.ui.hud;

import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.render.SkijaRenderer;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.ui.UiTheme;
import com.dioxidelite.ui.dioxide.DioxideDynamicIsland;
import io.github.humbleui.skija.Canvas;

/** Debug overlay showing Skija / island render timings, active profile and GPU. */
public final class PerformanceHUD extends EpsilonHudModule {

    public static final PerformanceHUD INSTANCE = new PerformanceHUD();

    private PerformanceHUD() {
        super("Performance HUD", Category.RENDER, 8, 120, 150.0F, 20.0F);
    }

    @Override
    protected void renderHud(Render2DEvent event) {
        if (noPlayer()) return;
        Canvas canvas = event.canvas();
        float s = 1.0F;
        float font = 10.0F * s;
        float line = font * 1.35F;

        double skijaMs = SkijaRenderer.lastOverlayNanos / 1_000_000.0;
        double islandMs = DioxideDynamicIsland.lastIslandRenderNanos / 1_000_000.0;
        String profile = SkijaRenderer.renderProfile().name();
        String gpu = SkijaRenderer.rendererString();

        String[] lines = {
                "DioxideLite Perf",
                String.format("Skija: %.1f ms", skijaMs),
                String.format("Island: %.2f ms", islandMs),
                "Profile: " + profile,
                "GPU: " + gpu
        };

        float x = renderX(event, 150.0F);
        float y = renderY(event, 20.0F);
        int color = UiTheme.rgb(255, 255, 255);
        updateBounds(150.0F, line * lines.length);
        for (int i = 0; i < lines.length; i++) {
            float ly = y + i * line;
            SkijaUi.textWithFallback(canvas, lines[i], x, ly, font, color, font);
        }
    }
}
