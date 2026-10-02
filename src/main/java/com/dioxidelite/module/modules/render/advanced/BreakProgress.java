package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.mixin.MultiPlayerGameModeAccessor;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.util.render.WorldToScreen;
import io.github.humbleui.skija.Canvas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 移植自 OpenOpal BreakProgressModule。
 * 在正在挖掘的方块上显示挖掘进度百分比。
 */
public final class BreakProgress extends Module {

    public static final BreakProgress INSTANCE = new BreakProgress();

    private static final float FONT_SIZE = 10.0F;

    private BreakProgress() {
        super("Break Progress", Category.RENDER);
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        if (mc.gameMode == null) return;
        MultiPlayerGameModeAccessor accessor = (MultiPlayerGameModeAccessor) mc.gameMode;
        float progress = accessor.dioxidelite$getDestroyProgress();
        if (progress <= 0.0F) return;

        BlockPos pos = accessor.dioxidelite$getDestroyPos();
        if (pos == null) return;

        Vec3 worldPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vector3f screen = WorldToScreen.getWorldPositionToScreen(worldPos);
        if (screen == null || screen.z < 0) return;

        int percent = (int) Math.ceil(progress * 100);
        String text = percent + "%";
        Canvas canvas = event.canvas();
        float width = SkijaUi.textWidthWithFallback(text, FONT_SIZE);
        SkijaUi.textWithFallback(canvas, text, screen.x - width * 0.5F, screen.y - FONT_SIZE * 0.5F,
                FONT_SIZE + 2.0F, 0xFFFFFFFF, FONT_SIZE);
    }
}
