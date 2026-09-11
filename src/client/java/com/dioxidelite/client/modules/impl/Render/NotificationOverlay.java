package com.dioxidelite.client.modules.impl.Render;

import com.dioxidelite.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class NotificationOverlay {
    private static final NotificationOverlay INSTANCE=new NotificationOverlay();
    private static final int HEIGHT=55, BAR=5, PAD=80; private static final long IN=700,STAY=2000,OUT=700;
    private boolean active,persistent,preview; private long start,stop=-1,previewStart,previewStop=-1; private String message=""; private int color=0xFFFFFF; private ItemStack icon=ItemStack.EMPTY; private String symbol=""; private int symbolColor=0xFFFFFF;
    public static NotificationOverlay getInstance(){return INSTANCE;}
    public void show(String text,int c){message=text;color=c;icon=ItemStack.EMPTY;symbol="";active=true;persistent=false;start=System.currentTimeMillis();stop=-1;preview=false;}
    public void show(String text){show(text,0xFFFFFF);}
    public void show(String text,int c,ItemStack stack){show(text,c);icon=stack==null?ItemStack.EMPTY:stack;}
    public void showPersistent(String text,int c,ItemStack stack){show(text,c,stack);persistent=true;}
    public void showPersistentSymbol(String text,int c,String s,int sc){message=text;color=c;icon=ItemStack.EMPTY;symbol=s==null?"":s;symbolColor=sc;active=true;persistent=true;start=System.currentTimeMillis();stop=-1;preview=false;}
    public void stopPersistent(){stopPersistent(0);}
    public void stopPersistent(long delay){if(persistent){persistent=false;stop=System.currentTimeMillis()+delay;}}
    public void startEditPreview(){if(active)return;preview=true;previewStart=System.currentTimeMillis();previewStop=-1;}
    public void stopEditPreview(){if(preview&&!active)previewStop=System.currentTimeMillis();}
    public void render(GuiGraphics g){if(!active&&!preview)return;Minecraft mc=Minecraft.getInstance();long now=System.currentTimeMillis();boolean pv=preview&&!active;long e=now-(pv?previewStart:start);float progress=Math.min(1,e/(float)IN);float out=0;boolean visible=false;if(pv&&previewStop>=0){out=Math.min(1,(now-previewStop)/(float)OUT);visible=out<1;}else if(e<IN){visible=true;}else if(persistent||stop<0||now<stop){visible=true;}else{out=Math.min(1,(now-stop)/(float)OUT);visible=out<1;}if(!visible){active=false;preview=false;return;}float ease=progress<1?1-(float)Math.pow(1-progress,3):1-out;String text=pv?"Notification":message;int tw=mc.font.width(text);int scale=Math.max(1,Math.round(Config.notificationScale*100))/100;int w=Math.max(192,tw+PAD+BAR), h=HEIGHT;int sw=Math.round(w*Config.notificationScale),sh=Math.round(h*Config.notificationScale);int x=getRenderX(mc.getWindow().getGuiScaledWidth(),sw),y=getRenderY(mc.getWindow().getGuiScaledHeight());int alpha=Math.round(255*ease);g.fill(x,y,x+sw,y+sh,(alpha<<24)|0x0A1015);g.fill(x,y,x+Math.min(sw,Math.max(BAR,Math.round(sw*ease))),y+sh,(alpha<<24)|0x58DDBE);g.renderOutline(x,y,sw,sh,(Math.round(alpha*.32f)<<24)|0x78CFFF);if(ease>.75f){int cx=x+12; if(!icon.isEmpty()){g.renderFakeItem(icon,cx,y+19);cx+=24;} else if(!symbol.isEmpty()){g.drawString(mc.font,Component.literal(symbol),cx,y+21,(Math.round(alpha*.9f)<<24)|(symbolColor&0xFFFFFF),false);cx+=24;}g.drawString(mc.font,Component.literal(text),cx,y+23,(alpha<<24)|(color&0xFFFFFF),false);}}
    public int getEditWidth(){return Math.round(192*Math.max(.5f,Config.notificationScale));} public int getEditHeight(){return Math.round(HEIGHT*Math.max(.5f,Config.notificationScale));}
    public int getRenderX(int screenWidth,int width){return getRenderRight(screenWidth)-width;} public int getRenderY(int screenHeight){return Float.isNaN(Config.notificationY)?(int)(screenHeight*.12):Math.round(screenHeight*.5f+Config.notificationY);} public int getRenderRight(int screenWidth){return Float.isNaN(Config.notificationX)?screenWidth:Math.round(screenWidth*.5f+Config.notificationX);}
    public boolean needsCanvas(){return active||preview||HudEditOverlay.getInstance().isActive();} public boolean needsStandaloneCanvas(){return false;} public int[] getCanvasBounds(int w,int h){return null;}
}
