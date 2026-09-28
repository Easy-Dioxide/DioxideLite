package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.util.render.WorldToScreen;
import io.github.humbleui.skija.Canvas;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows the remaining fuse time of primed TNT above each block.
 * <p>
 * Ported from 来源客户端 {@code TNTTimer}: entities are interpolated and projected
 * during the world-space 3D pass, then the "0.0s" pill (rounded background +
 * centred text, same 8px radius / 14px height / colours) is drawn on the shared
 * Skija canvas in the 2D pass.
 */
public final class TNTTimer extends Module {

    public static final TNTTimer INSTANCE = new TNTTimer();

    /** 来源客户端 background pill: 0xB8141519. */
    private static final int BACKGROUND = 0xB8141519;
    /** Vertical offset above the TNT entity position, in blocks. */
    private static final double LABEL_OFFSET = 1.25;
    /** Vanilla default fuse when the entity has not been primed with a value. */
    private static final int DEFAULT_FUSE = 80;
    private static final float BOX_HEIGHT = 14.0F;
    private static final float BOX_RADIUS = 8.0F;
    private static final float TEXT_PADDING = 8.0F;

    public final ColorSetting color = add(new ColorSetting("Color", new Color(255, 92, 92)));

    private final List<Label> labels = new ArrayList<>();

    private TNTTimer() {
        super("TNTTimer", Category.RENDER);
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        labels.clear();
        if (noPlayer()) {
            return;
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        for (Entity candidate : mc.level.entitiesForRendering()) {
            if (!(candidate instanceof PrimedTnt tnt)) {
                continue;
            }

            double x = Mth.lerp(partialTick, tnt.xOld, tnt.getX());
            double y = Mth.lerp(partialTick, tnt.yOld, tnt.getY()) + LABEL_OFFSET;
            double z = Mth.lerp(partialTick, tnt.zOld, tnt.getZ());

            Vector3f projected = WorldToScreen.getWorldPositionToScreen(new Vec3(x, y, z));
            if (projected == null || projected.z < 0.0F || projected.z > 1.0F) {
                continue;
            }

            int fuse = tnt.getFuse();
            if (fuse < 0) {
                fuse = DEFAULT_FUSE;
            }
            float seconds = Math.max(0.0F, (fuse - partialTick) / 20.0F);
            labels.add(new Label(projected.x, projected.y,
                    String.format(Locale.ROOT, "%.1fs", seconds)));
        }
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        if (labels.isEmpty()) {
            return;
        }

        Canvas canvas = event.canvas();
        int textColor = color.argb();
        for (Label label : labels) {
            float textWidth = SkijaUi.textWidth(label.text());
            float boxWidth = textWidth + TEXT_PADDING;
            float left = label.x() - boxWidth / 2.0F;
            float top = label.y() - BOX_HEIGHT / 2.0F;

            SkijaUi.rounded(canvas, left, top, boxWidth, BOX_HEIGHT, BOX_RADIUS, BACKGROUND);
            SkijaUi.text(canvas, label.text(), label.x() - textWidth / 2.0F, top, BOX_HEIGHT, textColor);
        }
    }

    private record Label(float x, float y, String text) {
    }
}
