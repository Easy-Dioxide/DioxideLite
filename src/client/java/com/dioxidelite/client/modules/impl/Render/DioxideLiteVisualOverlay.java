package com.dioxidelite.client.modules.impl.Render;

import com.dioxidelite.Config;
import com.dioxidelite.client.Version;
import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.LiquidGlassVisualSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Presentation-only HUD layer. It contains no combat automation, packet logic or targeting. */
public final class DioxideLiteVisualOverlay {
    private static final DioxideLiteVisualOverlay INSTANCE = new DioxideLiteVisualOverlay();
    private long lastMs;
    private float pulse;
    private DioxideLiteVisualOverlay() {}
    public static DioxideLiteVisualOverlay getInstance() { return INSTANCE; }

    public void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        long now = System.currentTimeMillis();
        float dt = lastMs == 0 ? .016f : Math.min(.05f, (now-lastMs)/1000f);
        lastMs = now;
        pulse += dt;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int accent = Config.visualStyle == Config.VisualStyle.SIGNATURE ? 0xFF3ED6B4 : 0xFF78CFFF;

        if (Config.visualWatermark) drawWatermark(g, mc, accent);
        if (Config.visualArrayList) drawFeatureList(g, mc, accent);
        if (Config.visualInfoHud) drawInfo(g, mc, accent);
        if (Config.visualFps) drawFps(g, mc, accent);
        if (Config.visualBps) drawBps(g, mc, accent);
        if (Config.visualCoordinates) drawCoordinates(g, mc, accent);
        if (Config.visualSession) drawSession(g, mc, accent);
        if (Config.visualKeybinds) drawKeybinds(g, mc, accent);
        if (Config.visualInventory) drawInventory(g, mc, accent);
        if (Config.visualScoreboard) drawScoreboard(g, mc, accent);
        if (Config.visualCompass) drawCompass(g, mc, w, accent);
        if (Config.visualCrosshair) drawCrosshair(g, mc, w, h, accent);
        if (Config.visualParticles && !Config.performanceMode) drawAmbientDots(g, w, h, accent);
    }

    private void panel(GuiGraphics g, float x, float y, float w, float h, float alpha) {
        if (Config.liquidGlassAllVisuals) LiquidGlassVisualSystem.renderHudDock(g, x, y, w, h, alpha);
        else g.fill(Math.round(x), Math.round(y), Math.round(x+w), Math.round(y+h), ((int)(alpha*255)<<24)|0x080D13);
    }

    private void drawWatermark(GuiGraphics g, Minecraft mc, int accent) {
        int x=12, y=10;
        panel(g, x-8,y-6,132,30,.72f);
        g.fill(x-8,y-6,x-6,y+24,accent);
        g.drawString(mc.font, "DioxideLite", x, y, 0xFFF4F8FF, true);
        g.drawString(mc.font, Version.displayName(), x+74, y, 0xFF9BA7B7, false);
    }

    private void drawFeatureList(GuiGraphics g, Minecraft mc, int accent) {
        List<String> enabled = new ArrayList<>();
        if (Config.visualWatermark) enabled.add("Watermark");
        if (Config.visualInfoHud) enabled.add("Info HUD");
        if (Config.visualFps) enabled.add("FPS");
        if (Config.visualBps) enabled.add("BPS");
        if (Config.visualCoordinates) enabled.add("Coordinates");
        if (Config.visualCompass) enabled.add("Compass");
        if (Config.visualKeybinds) enabled.add("Keybinds");
        if (Config.visualInventory) enabled.add("Inventory");
        if (Config.visualScoreboard) enabled.add("Scoreboard");
        if (Config.dynamicMotionBlur) enabled.add("Motion Blur");
        if (Config.dynamicIsland) enabled.add("Dynamic Island");
        if (Config.targetHud) enabled.add("Target HUD");
        if (Config.keystrokes) enabled.add("Keystrokes");
        if (Config.armorHud) enabled.add("Armor HUD");
        if (Config.potionStatus) enabled.add("Potion Status");
        if (Config.betterChat) enabled.add("Better Chat");
        int y=40;
        for (String name: enabled) {
            int tw=mc.font.width(name);
            int x=mc.getWindow().getGuiScaledWidth()-tw-18;
            panel(g, x-10,y-6,tw+20,24,.52f);
            g.fill(x-10,y-6,x-8,y+18,accent);
            g.drawString(mc.font, name, x, y, 0xFFE7EEF7, false);
            y += 22;
        }
    }

    private void drawInfo(GuiGraphics g, Minecraft mc, int accent) {
        Player p=mc.player;
        String xyz=String.format(Locale.ROOT,"XYZ %.1f  %.1f  %.1f",p.getX(),p.getY(),p.getZ());
        double speed=Math.sqrt(p.getDeltaMovement().x*p.getDeltaMovement().x+p.getDeltaMovement().z*p.getDeltaMovement().z)*20.0;
        String line2=String.format(Locale.ROOT,"SPD %.2f  FPS %d",speed,Math.max(0, Minecraft.getInstance().getFps()));
        int x=12, y=mc.getWindow().getGuiScaledHeight()-34;
        panel(g,x-8,y-6,176,34,.54f);
        g.fill(x-8,y-6,x-6,y+28,accent);
        g.drawString(mc.font, xyz, x, y, 0xFFD4DCE7, false);
        g.drawString(mc.font, line2, x, y+12, 0xFF8E9AAA, false);
    }

    private void drawFps(GuiGraphics g, Minecraft mc, int accent) {
        int x=12,y=48; panel(g,x-7,y-5,78,22,.52f);
        g.drawString(mc.font,"FPS "+Math.max(0,mc.getFps()),x,y,0xFFF0F5FA,false);
        g.fill(x-7,y-5,x-5,y+17,accent);
    }
    private void drawBps(GuiGraphics g, Minecraft mc, int accent) {
        Player p=mc.player; double bps=Math.sqrt(p.getDeltaMovement().x*p.getDeltaMovement().x+p.getDeltaMovement().z*p.getDeltaMovement().z)*20.0;
        int x=12,y=76; panel(g,x-7,y-5,92,22,.52f); g.drawString(mc.font,String.format(Locale.ROOT,"BPS %.2f",bps),x,y,0xFFF0F5FA,false); g.fill(x-7,y-5,x-5,y+17,accent);
    }
    private void drawCoordinates(GuiGraphics g, Minecraft mc, int accent) {
        Player p=mc.player; int x=12,y=104; panel(g,x-7,y-5,170,22,.52f); g.drawString(mc.font,String.format(Locale.ROOT,"X %.1f  Y %.1f  Z %.1f",p.getX(),p.getY(),p.getZ()),x,y,0xFFF0F5FA,false); g.fill(x-7,y-5,x-5,y+17,accent);
    }
    private void drawSession(GuiGraphics g, Minecraft mc, int accent) {
        String server=mc.getCurrentServer()==null?"Singleplayer":mc.getCurrentServer().ip;
        int x=12,y=132; panel(g,x-7,y-5,Math.min(260,mc.font.width(server)+64),22,.52f); g.drawString(mc.font,"SESSION  "+server,x,y,0xFFF0F5FA,false); g.fill(x-7,y-5,x-5,y+17,accent);
    }
    private void drawKeybinds(GuiGraphics g, Minecraft mc, int accent) {
        int x=mc.getWindow().getGuiScaledWidth()-118,y=mc.getWindow().getGuiScaledHeight()-104;
        panel(g,x,y,110,70,.52f); g.drawString(mc.font,"KEYBINDS",x+10,y+8,accent,false); g.drawString(mc.font,"RSHIFT  ClickGUI",x+10,y+25,0xFFE7EEF7,false); g.drawString(mc.font,"F3      Debug",x+10,y+40,0xFFB9C4D0,false); g.drawString(mc.font,"TAB     Players",x+10,y+55,0xFFB9C4D0,false);
    }
    private void drawInventory(GuiGraphics g, Minecraft mc, int accent) {
        int x=mc.getWindow().getGuiScaledWidth()-176,y=mc.getWindow().getGuiScaledHeight()-150;
        panel(g,x,y,168,34,.52f); g.drawString(mc.font,"INVENTORY",x+10,y+8,accent,false);
        g.drawString(mc.font,"Selected slot  "+(mc.player.getInventory().getSelectedSlot()+1),x+10,y+21,0xFFE7EEF7,false);
    }
    private void drawScoreboard(GuiGraphics g, Minecraft mc, int accent) {
        if (mc.level == null) return;
        int x=mc.getWindow().getGuiScaledWidth()-176,y=34; panel(g,x,y,168,34,.48f); g.drawString(mc.font,"SCOREBOARD",x+10,y+8,accent,false); g.drawString(mc.font,"DioxideLite",x+10,y+21,0xFFE7EEF7,false);
    }
    private void drawCompass(GuiGraphics g, Minecraft mc, int w, int accent) {
        String[] dirs={"N","NE","E","SE","S","SW","W","NW"}; float yaw=(mc.player.getYRot()%360+360)%360; int center=w/2;
        if (Config.liquidGlassAllVisuals) panel(g,center-94,8,188,24,.50f); else g.fill(center-90,12,center+90,28,0x52080D13);
        for(int i=0;i<dirs.length;i++){ float angle=(i*45f-yaw+3600f)%360f; if(angle>180f)angle-=360f; int xx=Math.round(center+angle*1.35f); if(xx<center-84||xx>center+84)continue; int tw=mc.font.width(dirs[i]); g.drawString(mc.font,dirs[i],xx-tw/2,16,i==0?accent:0xFFB6C0CE,false); }
        g.fill(center-1,10,center+1,30,accent);
    }
    private void drawCrosshair(GuiGraphics g, Minecraft mc, int w, int h, int accent) {
        int cx=w/2,cy=h/2,gap=4,len=5; g.fill(cx-gap-len,cy-1,cx-gap,cy+2,0xDDF4F8FF); g.fill(cx+gap,cy-1,cx+gap+len,cy+2,0xDDF4F8FF); g.fill(cx-1,cy-gap-len,cx+2,cy-gap,0xDDF4F8FF); g.fill(cx-1,cy+gap,cx+2,cy+gap+len,0xDDF4F8FF); g.fill(cx,cy,cx+1,cy+1,accent);
    }
    private void drawAmbientDots(GuiGraphics g,int w,int h,int accent){ for(int i=0;i<14;i++){double t=pulse*(.18+i*.013)+i*1.73;int x=(int)((Math.sin(t)*.5+.5)*(w-24))+12;int y=(int)((Math.cos(t*.77+i)*.5+.5)*(h-48))+24;int a=22+(int)(14*(Math.sin(t)+1));g.fill(x,y,x+1,y+1,(a<<24)|(accent&0xFFFFFF));} }
}
