package com.dioxidelite.client.modules.impl.Render;

import com.dioxidelite.Config;
import com.dioxidelite.client.render.nativeui.NativeGlassVisualSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/** Native Target HUD. Avatar and text are drawn directly through Minecraft's GUI pipeline. */
public final class TargetHudRenderer {
    private static final TargetHudRenderer INSTANCE=new TargetHudRenderer(); private LivingEntity target; private long hitAt,appearAt; private float lastHealth=-1; private long lastNs;
    public static TargetHudRenderer getInstance(){return INSTANCE;}
    public void onHit(LivingEntity entity){if(!Config.targetHud||entity==null)return;long n=System.currentTimeMillis();if(target!=entity){appearAt=n;}target=entity;hitAt=n;}
    public void render(GuiGraphics g){Minecraft mc=Minecraft.getInstance();long n=System.currentTimeMillis();boolean edit=HudEditOverlay.getInstance().isActive()&&Config.targetHud;if(edit&&mc.player!=null){target=mc.player;appearAt=n;hitAt=n;}if(target==null)return;if(!target.isAlive()&&!edit&&n-hitAt>200)return;float in=Mth.clamp((n-appearAt)/200f,0,1);float out=1-Mth.clamp((n-hitAt-3000)/200f,0,1);float a=Math.min(in,out);if(a<=0&&!edit){target=null;return;}float scale=Math.max(.5f,Config.targetHudScale);int w=190,h=58;int x=Math.round(mc.getWindow().getGuiScaledWidth()*.5f+Config.targetHudX),y=Math.round(mc.getWindow().getGuiScaledHeight()*.5f+Config.targetHudY);x=Math.max(0,Math.min(mc.getWindow().getGuiScaledWidth()-Math.round(w*scale),x));y=Math.max(0,Math.min(mc.getWindow().getGuiScaledHeight()-Math.round(h*scale),y));int ai=Math.round(255*a);NativeGlassVisualSystem.renderHudDock(g,x,y,Math.round(w*scale),Math.round(h*scale),.82f);g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);String name=target.getDisplayName().getString();if(name.length()>18)name=name.substring(0,18)+"…";g.drawString(mc.font,name,54,11,(ai<<24)|0xF2F7F8,false);float hp=target.getHealth(),max=Math.max(1,target.getMaxHealth()),ratio=Mth.clamp(hp/max,0,1);g.fill(54,34,180,40,(ai<<24)|0x26333A);int hc=ratio>.5?0x58DDBE:(ratio>.25?0xF2C94C:0xFF6570);g.fill(54,34,54+Math.round(126*ratio),40,(ai<<24)|hc);String hs=String.format(java.util.Locale.ROOT,"%.1f / %.1f",hp,max);g.drawString(mc.font,hs,54,43,(Math.round(ai*.7f)<<24)|0xB8C5CA,false);drawAvatar(g,mc,8,10,38,a);if(mc.player!=null){String status=mc.player.getHealth()>hp?"W":"L";g.drawString(mc.font,status,171,11,(ai<<24)|(status.equals("W")?0x58DDBE:0xFF6570),false);}g.pose().popMatrix();lastHealth=hp;lastNs=n;}
    private void drawAvatar(GuiGraphics g,Minecraft mc,int x,int y,int size,float a){if(target instanceof Player p){try{PlayerSkin skin=mc.getSkinManager().createLookup(p.getGameProfile(),false).get();PlayerFaceRenderer.draw(g,skin,x,y,size);return;}catch(Exception ignored){}}SpawnEggItem egg=SpawnEggItem.byId(target.getType());if(egg!=null){g.renderFakeItem(new ItemStack(egg),x+11,y+11);return;}g.fill(x,y,x+size,y+size,(Math.round(255*a)<<24)|0x202830);}
    public int getEditWidth(){return Math.round(190*Math.max(.5f,Config.targetHudScale));}public int getEditHeight(){return Math.round(58*Math.max(.5f,Config.targetHudScale));}
}
