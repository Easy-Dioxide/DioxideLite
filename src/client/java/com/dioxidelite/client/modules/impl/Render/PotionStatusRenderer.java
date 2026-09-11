package com.dioxidelite.client.modules.impl.Render;

import com.dioxidelite.Config;
import com.dioxidelite.client.render.nativeui.NativeGlassVisualSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.*;

/** Native potion/effect HUD with lightweight animation and no off-screen rendering. */
public final class PotionStatusRenderer {
    private static final PotionStatusRenderer INSTANCE=new PotionStatusRenderer(); private static final int WIDTH=134,ROW=28,GAP=5,PAD=7,ICON=18; private static final long HIDE=260;
    private final Map<String,Visual> visuals=new HashMap<>(); private long lastMs;
    public static PotionStatusRenderer getInstance(){return INSTANCE;}
    public void render(GuiGraphics g){Minecraft mc=Minecraft.getInstance();if(!Config.potionStatus){visuals.clear();return;}if(mc.player==null||mc.level==null||mc.options.hideGui)return;long now=System.currentTimeMillis();float dt=lastMs==0?.016f:Math.min(.05f,(now-lastMs)/1000f);lastMs=now;List<MobEffectInstance> effects=HudEditOverlay.getInstance().isActive()?preview():visible(mc);Map<String,Boolean> seen=new HashMap<>();int i=0;for(MobEffectInstance e:effects){String k=key(e);seen.put(k,true);Visual v=visuals.computeIfAbsent(k,z->{Visual nv=new Visual();nv.key=k;return nv;});v.e=e;v.targetY=PAD+i*(ROW+GAP);v.visible=true;v.alpha=Math.min(1,v.alpha+dt*9);v.y+=(v.targetY-v.y)*Math.min(1,dt*12);v.fill+=(targetFill(e)-v.fill)*Math.min(1,dt*8);i++;}for(Iterator<Map.Entry<String,Visual>> it=visuals.entrySet().iterator();it.hasNext();){Visual v=it.next().getValue();if(!seen.containsKey(v.key)){v.visible=false;v.alpha-=dt*8;if(v.alpha<=0)it.remove();}}
        if(visuals.isEmpty())return;float h=PAD+effects.size()*(ROW+GAP);float scale=Math.max(.5f,Config.potionStatusScale);int sw=Math.round(WIDTH*scale),sh=Math.round(h*scale);int x=Math.round(Math.max(0,Math.min(mc.getWindow().getGuiScaledWidth()-sw,8+Config.potionStatusX))),y=Math.round(Math.max(0,Math.min(mc.getWindow().getGuiScaledHeight()-sh,(mc.getWindow().getGuiScaledHeight()-sh)/2f+Config.potionStatusY)));NativeGlassVisualSystem.renderHudDock(g,x-4,y-4,sw+8,sh+8,.68f);g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);for(Visual v:visuals.values()){if(v.alpha<=.01)continue;int yy=Math.round(v.y);int ea=Math.round(255*v.alpha);int c=0xFF000000|(v.e==null?0x78CFFF:v.e.getEffect().value().getColor());g.fill(PAD,yy,PAD+WIDTH-2,yy+ROW,(Math.round(ea*.72f)<<24)|0x10161C);g.fill(PAD,yy,PAD+Math.round((WIDTH-2)*v.fill),yy+ROW,(Math.round(ea*.28f)<<24)|(c&0xFFFFFF));g.renderOutline(PAD,yy,WIDTH-2,ROW,(Math.round(ea*.20f)<<24)|0x78CFFF);g.fill(PAD+5,yy+5,PAD+5+ICON,yy+5+ICON,(ea<<24)|(c&0xFFFFFF));if(v.e!=null){String n=shortName(v.e);g.drawString(mc.font,n,PAD+ICON+10,yy+7,(ea<<24)|0xF0F6F8,false);if(Config.potionStatusCountdown){g.drawString(mc.font,duration(v.e),PAD+ICON+10,yy+18,(Math.round(ea*.72f)<<24)|0xB9C6CC,false);}}}g.pose().popMatrix();}
    public int getEditWidth(){return Math.round(WIDTH*Math.max(.5f,Config.potionStatusScale));}public int getEditHeight(){return Math.round((PAD+3*(ROW+GAP))*Math.max(.5f,Config.potionStatusScale));}public float getDefaultX(){return 8;}public float getDefaultY(int h){return (h-getEditHeight())/2f;}public float getRenderX(int w){return Math.max(0,Math.min(w-getEditWidth(),getDefaultX()+Config.potionStatusX));}public float getRenderY(int h){return Math.max(0,Math.min(h-getEditHeight(),getDefaultY(h)+Config.potionStatusY));}
    public boolean shouldHideVanillaEffects(){if(!Config.potionStatus||!Config.potionStatusHideVanilla)return false;Minecraft mc=Minecraft.getInstance();return mc.player!=null&&mc.level!=null&&!mc.options.hideGui&&!visible(mc).isEmpty();}
    private List<MobEffectInstance> visible(Minecraft mc){List<MobEffectInstance> l=new ArrayList<>();for(MobEffectInstance e:mc.player.getActiveEffects())if(e.isVisible())l.add(e);l.sort(Comparator.comparing(MobEffectInstance::getDescriptionId));return l;}
    private List<MobEffectInstance> preview(){return List.of(new MobEffectInstance(MobEffects.SPEED,3600,1),new MobEffectInstance(MobEffects.STRENGTH,1800,0),new MobEffectInstance(MobEffects.FIRE_RESISTANCE,9600,0));}
    private String key(MobEffectInstance e){return e.getDescriptionId()+"#"+e.getAmplifier();}private float targetFill(MobEffectInstance e){return e.isInfiniteDuration()?1f:Mth.clamp(e.getDuration()/3600f,0,1);}
    private String shortName(MobEffectInstance e){String s=net.minecraft.network.chat.Component.translatable(e.getDescriptionId()).getString();return s.length()>17?s.substring(0,17)+"…":s;}
    private String duration(MobEffectInstance e){if(e.isInfiniteDuration())return "∞";int sec=Math.max(0,e.getDuration()/20);return (sec/60)+":"+String.format(Locale.ROOT,"%02d",sec%60);}
    private static final class Visual{String key;MobEffectInstance e;float y,targetY,alpha,fill;boolean visible;Visual(){}}
}
