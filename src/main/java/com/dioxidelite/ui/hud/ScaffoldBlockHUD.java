package com.dioxidelite.ui.hud;

import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.render.SkijaRenderer;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.ui.UiTheme;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.Color;
import java.util.Locale;

/** Visual-only scaffold placement HUD. It never places blocks or automates movement. */
public final class ScaffoldBlockHUD extends EpsilonHudModule {
    public static final ScaffoldBlockHUD INSTANCE = new ScaffoldBlockHUD();
    private static final Paint BLOCK_PAINT = new Paint().setAntiAlias(false);

    public final DoubleSetting scale = add(new DoubleSetting("Scale", 1.0, 0.65, 1.6, 0.05));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final ColorSetting backgroundColor = add(new ColorSetting("Background Color",
            new Color(UiTheme.withAlpha(UiTheme.SURFACE, 174), true), true)
            .visibleWhen(background::get));
    public final BooleanSetting border = add(new BooleanSetting("Border", true));
    public final ColorSetting borderColor = add(new ColorSetting("Border Color",
            new Color(UiTheme.INFO, true), true).visibleWhen(border::get));
    public final DoubleSetting borderRadius = add(new DoubleSetting("Border Radius", 5.0, 0.0, 16.0, 0.5));
    public final DoubleSetting holdTime = add(new DoubleSetting("Hold Time", 1.25, 0.25, 4.0, 0.05));
    public final BooleanSetting showBps = add(new BooleanSetting("Show BPS", true));
    public final IntSetting fadeOut = add(new IntSetting("Fade Out", 350, 0, 1200, 25));

    private long lastPlacementNanos;
    private ItemStack lastBlock = ItemStack.EMPTY;
    private int lastCount;
    private double lastX, lastZ;
    private int lastTick;
    private float bps;
    private boolean movementInitialized;

    private ScaffoldBlockHUD() { super("Scaffold HUD", 500, 760, 112.0F, 24.0F); }

    @Override protected void renderHud(Render2DEvent event) {
        if (noPlayer()) return;
        updateMovement();
        ItemStack held = mc.player.getMainHandItem();
        boolean blockHeld = held.getItem() instanceof BlockItem;
        boolean useHeld = mc.options.keyUse.isDown();
        if (blockHeld && useHeld) {
            lastPlacementNanos = System.nanoTime();
            lastBlock = held.copy();
            lastCount = countBlocks(held);
        }
        long elapsed = System.nanoTime() - lastPlacementNanos;
        long holdNanos = (long)(holdTime.get() * 1_000_000_000L);
        long fadeNanos = fadeOut.get() * 1_000_000L;
        if (lastPlacementNanos == 0L || elapsed > holdNanos + fadeNanos) return;
        float alpha = elapsed <= holdNanos ? 1.0F : 1.0F - (elapsed - holdNanos) / (float)Math.max(1L, fadeNanos);
        float s = scale.get().floatValue();
        String count = Integer.toString(Math.max(0, lastCount));
        String speed = String.format(Locale.ROOT, "%.1f", bps);
        float h = 24.0F * s;
        float width = (showBps.get() ? 56.0F : 39.0F) * s + SkijaUi.boldTextWidth(count, 8.5F*s);
        float x = renderX(event, width), y = renderY(event, h);
        updateBounds(width, h);
        int a = Math.round(255.0F * HudRenderUtil.clamp(alpha * opacityFactor(), 0.0F, 1.0F));
        int radius = Math.round(borderRadius.get().floatValue()*s);
        if (background.get()) HudRenderUtil.coloredSurface(event.canvas(), x, y, width, h, radius, applyOpacity(backgroundColor.argb()), HudFusionManager.Edges.NONE);
        if (border.get()) HudRenderUtil.border(event.canvas(), x, y, width, h, radius, 1.1F*s, 1.0F, HudRenderUtil.BorderMode.Single, applyOpacity(borderColor.argb()), borderColor.argb(), borderColor.argb(), a);
        drawBlockTexture(event.canvas(), lastBlock, x+4*s, y+4*s, 16*s, a);
        SkijaUi.boldText(event.canvas(), count, x+23*s, y+2*s, 12*s, UiTheme.withAlpha(UiTheme.TEXT,a), 8.5F*s);
        if (showBps.get()) SkijaUi.text(event.canvas(), speed+" BPS", x+23*s, y+12*s, 9*s, UiTheme.withAlpha(UiTheme.TEXT_FAINT,a), 6.5F*s);
    }

    private int countBlocks(ItemStack held) {
        if (!(held.getItem() instanceof BlockItem block)) return 0;
        int count = 0;
        for (int i=0;i<mc.player.getInventory().getContainerSize();i++) {
            ItemStack stack=mc.player.getInventory().getItem(i);
            if (stack.getItem()==block) count += stack.getCount();
        }
        return count;
    }
    private void updateMovement() {
        int tick=mc.player.tickCount; double x=mc.player.getX(), z=mc.player.getZ();
        if (!movementInitialized) { movementInitialized=true; lastX=x; lastZ=z; lastTick=tick; return; }
        int dt=Math.max(1,tick-lastTick);
        if (dt>20) { lastX=x; lastZ=z; lastTick=tick; return; }
        bps=(float)Math.min(60.0,Math.hypot(x-lastX,z-lastZ)*20.0/dt);
        lastX=x; lastZ=z; lastTick=tick;
    }
    private void drawBlockTexture(Canvas canvas, ItemStack stack,float x,float y,float size,int alpha) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem block)) return;
        BlockState state=block.getBlock().defaultBlockState();
        TextureAtlasSprite sprite=mc.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
        try (SkijaRenderer.BorrowedImage borrowed=SkijaRenderer.borrowTexture(sprite.atlasLocation())) {
            if (borrowed==null) return;
            float sx=sprite.getU0()*borrowed.image().getWidth(), sy=sprite.getV0()*borrowed.image().getHeight();
            float sw=(sprite.getU1()-sprite.getU0())*borrowed.image().getWidth(), sh=(sprite.getV1()-sprite.getV0())*borrowed.image().getHeight();
            BLOCK_PAINT.setAlpha(alpha); canvas.drawImageRect(borrowed.image(), Rect.makeXYWH(sx,sy,sw,sh), Rect.makeXYWH(x,y,size,size), SamplingMode.DEFAULT,BLOCK_PAINT,true);
        } finally { BLOCK_PAINT.setAlpha(255); }
    }
}
