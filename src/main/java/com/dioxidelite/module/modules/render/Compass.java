package com.dioxidelite.module.modules.render;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.manager.FriendManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.render.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Draws nearby players as directional arrows around the screen centre. */
public final class Compass extends Module {

    public static final Compass INSTANCE = new Compass();

    private static final float RADIUS = 55.0F;
    private static final float ARROW_SIZE = 7.0F;
    private static final float TEXT_SIZE = 9.0F;
    private static final double MAX_DISTANCE = 128.0;
    private static final double CLOSE_ENEMY_DISTANCE = 15.0;
    private static final int COLOR = 0xE6FFFFFF;
    private static final int CLOSE_ENEMY_COLOR = 0xF2FF3B30;

    private Paint arrowPaint;
    private Path arrowShape;

    private Compass() {
        super("Compass", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        arrowPaint = new Paint().setAntiAlias(true).setColor(COLOR);
        try (PathBuilder builder = new PathBuilder()) {
            builder.moveTo(0.0F, -ARROW_SIZE);
            builder.lineTo(-ARROW_SIZE * 0.65F, ARROW_SIZE * 0.8F);
            builder.lineTo(ARROW_SIZE * 0.65F, ARROW_SIZE * 0.8F);
            builder.close();
            arrowShape = builder.detach();
        }
    }

    @Override
    protected void onDisable() {
        if (arrowPaint != null) {
            arrowPaint.close();
            arrowPaint = null;
        }
        if (arrowShape != null) {
            arrowShape.close();
            arrowShape = null;
        }
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        if (noPlayer() || arrowPaint == null) {
            return;
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float centerX = event.width() / 2.0F;
        float centerY = event.height() / 2.0F;
        double selfX = Mth.lerp(partialTick, mc.player.xOld, mc.player.getX());
        double selfZ = Mth.lerp(partialTick, mc.player.zOld, mc.player.getZ());
        float yawRadians = (float) Math.toRadians(mc.player.getYRot(partialTick));
        Canvas canvas = event.canvas();

        for (Player target : mc.level.players()) {
            if (target == mc.player || !target.isAlive() || target.isSpectator()) {
                continue;
            }

            double distance = mc.player.distanceTo(target);
            if (distance > MAX_DISTANCE) {
                continue;
            }

            double targetX = Mth.lerp(partialTick, target.xOld, target.getX());
            double targetZ = Mth.lerp(partialTick, target.zOld, target.getZ());
            double angle = Math.atan2(targetZ - selfZ, targetX - selfX)
                    - yawRadians - Math.PI / 2.0;
            float x = centerX + (float) (Math.sin(angle) * RADIUS);
            float y = centerY - (float) (Math.cos(angle) * RADIUS);

            boolean closeEnemy = distance <= CLOSE_ENEMY_DISTANCE
                    && !FriendManager.INSTANCE.isFriend(target);
            arrowPaint.setColor(closeEnemy ? CLOSE_ENEMY_COLOR : COLOR);
            drawArrow(canvas, x, y, (float) angle);

            String distanceText = Math.round(distance) + "m";
            float textWidth = SkijaUi.textWidth(distanceText, TEXT_SIZE);
            SkijaUi.text(canvas, distanceText, x - textWidth / 2.0F,
                    y - ARROW_SIZE - TEXT_SIZE, TEXT_SIZE, COLOR, TEXT_SIZE);
        }
    }

    private void drawArrow(Canvas canvas, float centerX, float centerY, float angle) {
        if (arrowShape == null) return;
        int save = canvas.save();
        try {
            canvas.translate(centerX, centerY);
            canvas.rotate((float) Math.toDegrees(angle));
            canvas.drawPath(arrowShape, arrowPaint);
        } finally {
            canvas.restoreToCount(save);
        }
    }
}
