package com.dioxidelite.ui.hud;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.event.events.StartUseItemEvent;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.ui.UiTheme;
import net.minecraft.world.item.BlockItem;

import java.awt.Color;

/** Functional placement HUD independent of the Scaffold automation module. */
public final class ScaffoldBlockHUD extends EpsilonHudModule {
    public static final ScaffoldBlockHUD INSTANCE = new ScaffoldBlockHUD();
    public final DoubleSetting scale = add(new DoubleSetting("Scale", 1.0, 0.6, 1.8, 0.05));
    public final DoubleSetting stayTime = add(new DoubleSetting("Stay Time", 900.0, 100.0, 3000.0, 50.0));
    public final BooleanSetting showBps = add(new BooleanSetting("Show BPS", true));
    public final ColorSetting background = add(new ColorSetting("Background", new Color(12,16,24,220), true));

    private long lastPlacement;
    private int count;
    private double lastX, lastZ;
    private long lastTick;
    private double bps;

    private ScaffoldBlockHUD() { super("Scaffold HUD", 8, 110, 130, 36); }

    @Listen
    private void onUse(StartUseItemEvent event) {
        if (noPlayer()) return;
        if (mc.player.getMainHandItem().getItem() instanceof BlockItem || mc.player.getOffhandItem().getItem() instanceof BlockItem) {
            count = countHeldBlocks();
            lastPlacement = System.currentTimeMillis();
        }
    }

    @Override protected void renderHud(Render2DEvent event) {
        if (noPlayer()) return;
        sampleBps();
        long age = System.currentTimeMillis() - lastPlacement;
        if (lastPlacement == 0 || age > stayTime.get().longValue()) return;
        float s = scale.get().floatValue();
        float width = 130f*s, height = 36f*s;
        float x = renderX(event,width), y = renderY(event,height);
        updateBounds(width,height);
        int alpha = Math.round(255f * Math.max(0f, 1f - age / ((Number) stayTime.get()).floatValue()) * opacityFactor());
        HudRenderUtil.coloredSurface(event.canvas(),x,y,width,height,8f*s,(alpha<<24)|(background.argb()&0x00FFFFFF),HudFusionManager.Edges.NONE);
        SkijaUi.boldText(event.canvas(), "SCAFFOLD", x+8*s,y+5*s,10*s,(alpha<<24)|(UiTheme.TEXT&0xFFFFFF),7*s);
        SkijaUi.text(event.canvas(), "Blocks  " + count, x+8*s,y+16*s,9*s,(alpha<<24)|(UiTheme.TEXT_FAINT&0xFFFFFF),6.5f*s);
        if (showBps.get()) {
            String speed = String.format(java.util.Locale.ROOT,"%.1f BPS",bps);
            float tw=SkijaUi.textWidth(speed,6.5f*s);
            SkijaUi.text(event.canvas(),speed,x+width-8*s-tw,y+16*s,9*s,(alpha<<24)|(UiTheme.TEXT&0xFFFFFF),6.5f*s);
        }
    }

    private int countHeldBlocks() {
        int total = 0;
        if (mc.player.getMainHandItem().getItem() instanceof BlockItem) total += mc.player.getMainHandItem().getCount();
        if (mc.player.getOffhandItem().getItem() instanceof BlockItem) total += mc.player.getOffhandItem().getCount();
        return total;
    }

    private void sampleBps() {
        long tick = mc.player.tickCount;
        if (lastTick == 0) { lastTick=tick; lastX=mc.player.getX(); lastZ=mc.player.getZ(); return; }
        long dt=Math.max(1,tick-lastTick);
        bps=Math.hypot(mc.player.getX()-lastX,mc.player.getZ()-lastZ)*20.0/dt;
        lastX=mc.player.getX(); lastZ=mc.player.getZ(); lastTick=tick;
    }

    @Override public int editorColor(){ return UiTheme.accent(); }
    @Override public boolean supportsHudFusion(){ return true; }
    @Override public boolean hudFusionBorderEnabled(){ return true; }
    @Override public boolean hudFusionBackgroundEnabled(){ return true; }
}
