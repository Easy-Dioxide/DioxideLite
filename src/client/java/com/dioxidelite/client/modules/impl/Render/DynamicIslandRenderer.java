package com.dioxidelite.client.modules.impl.Render;

import com.dioxidelite.Config;
import com.dioxidelite.client.render.nativeui.NativeGlassVisualSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.GameType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Native Dynamic Island renderer. No off-screen surfaces or external rasterizer. */
public final class DynamicIslandRenderer {
    private static final DynamicIslandRenderer INSTANCE=new DynamicIslandRenderer();
    private static final int MIN_W=250, TOP=10, H=30;
    private long lastTabRequest;
    private float width=-1, height=-1;
    private long lastNs=System.nanoTime();
    public static DynamicIslandRenderer getInstance(){return INSTANCE;}
    public void requestTabListFrame(){lastTabRequest=System.currentTimeMillis();}
    public void render(GuiGraphics g){
        Minecraft mc=Minecraft.getInstance(); if(!Config.dynamicIsland||mc.player==null||mc.getWindow()==null||mc.screen instanceof AbstractContainerScreen<?>)return;
        boolean tab=isTabOpen(mc); List<PlayerInfo> players=tab?getPlayers(mc):List.of();
        float targetW=tab?measureTab(mc,players):measureCompact(mc); float targetH=tab?Math.min(20+Math.max(1,(players.size()+5)/6)*15,mc.getWindow().getGuiScaledHeight()-20):H;
        float dt=Math.min(.05f,(System.nanoTime()-lastNs)/1e9f);lastNs=System.nanoTime(); if(width<0){width=targetW;height=targetH;} else {width+=(targetW-width)*Math.min(1,dt*13);height+=(targetH-height)*Math.min(1,dt*13);}
        int x=Math.round((mc.getWindow().getGuiScaledWidth()-width)/2), y=TOP; int iw=Math.round(width), ih=Math.round(height);
        NativeGlassVisualSystem.renderHudDock(g,x,y,iw,ih,.82f);
        if(tab)drawTab(g,mc,players,x,y,iw,ih); else drawCompact(g,mc,x,y,iw);
    }
    private boolean isTabOpen(Minecraft mc){return (mc.options!=null&&mc.options.keyPlayerList.isDown())||System.currentTimeMillis()-lastTabRequest<120;}
    private float measureCompact(Minecraft mc){String server=location(mc);String s=(mc.getUser()==null?"Player":mc.getUser().getName())+"  ·  "+server+"  ·  FPS "+mc.getFps();return Math.max(MIN_W,mc.font.width(s)+36)*Config.dynamicIslandWidthScale;}
    private float measureTab(Minecraft mc,List<PlayerInfo> p){int cols=Math.max(1,Math.min(6,(p.size()+19)/20));int rows=Math.max(1,(p.size()+cols-1)/cols);return Math.min(mc.getWindow().getGuiScaledWidth()-34,Math.max(340,cols*150+(cols-1)*16+36));}
    private void drawCompact(GuiGraphics g,Minecraft mc,int x,int y,int w){String server=location(mc);String user=mc.getUser()==null?"Player":mc.getUser().getName();String left=user+"  ·  "+server;g.drawString(mc.font,left,x+18,y+10,0xEFFFFFFF,false);String fps="FPS "+mc.getFps();g.drawString(mc.font,fps,x+w-18-mc.font.width(fps),y+10,0xFF78CFFF,false);}
    private void drawTab(GuiGraphics g,Minecraft mc,List<PlayerInfo> p,int x,int y,int w,int h){int cols=Math.max(1,Math.min(6,(p.size()+19)/20));int rows=Math.max(1,(p.size()+cols-1)/cols);int colW=(w-36-(cols-1)*16)/cols;for(int i=0;i<p.size();i++){int c=i/rows,r=i%rows;PlayerInfo info=p.get(i);String n=info.getProfile().name();if(n==null)n="Player";if(n.length()>18)n=n.substring(0,18);String ping=info.getLatency()+"ms";int xx=x+18+c*(colW+16),yy=y+12+r*15;int color=info.getGameMode()==GameType.SPECTATOR?0xFF8E98A2:0xFFEAF2F4;g.drawString(mc.font,n,xx,yy,color,false);g.drawString(mc.font,ping,xx+colW-mc.font.width(ping),yy,0xFF6D7D86,false);}}
    private List<PlayerInfo> getPlayers(Minecraft mc){if(mc.getConnection()==null)return List.of();List<PlayerInfo> p=new ArrayList<>(mc.getConnection().getListedOnlinePlayers());p.sort(Comparator.comparingInt(PlayerInfo::getTabListOrder).thenComparing(i->i.getProfile().name().toLowerCase()));return p;}
    private String location(Minecraft mc){ServerData s=mc.getCurrentServer();return s==null?"Singleplayer":s.ip;}
}
