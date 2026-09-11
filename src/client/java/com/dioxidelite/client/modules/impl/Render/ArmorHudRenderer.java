package com.dioxidelite.client.modules.impl.Render;

import com.dioxidelite.Config; import com.dioxidelite.client.render.nativeui.NativeGlassVisualSystem; import net.minecraft.client.Minecraft; import net.minecraft.client.gui.GuiGraphics; import net.minecraft.world.entity.EquipmentSlot; import net.minecraft.world.item.ItemStack; import java.util.ArrayList; import java.util.List;

/** Native armor HUD. */
public final class ArmorHudRenderer {
    private static final ArmorHudRenderer INSTANCE=new ArmorHudRenderer(); public static ArmorHudRenderer getInstance(){return INSTANCE;}
    public void render(GuiGraphics g){Minecraft mc=Minecraft.getInstance();if(!Config.armorHud||mc.player==null)return;List<ItemStack> items=new ArrayList<>();items.add(mc.player.getItemBySlot(EquipmentSlot.FEET));items.add(mc.player.getItemBySlot(EquipmentSlot.LEGS));items.add(mc.player.getItemBySlot(EquipmentSlot.CHEST));items.add(mc.player.getItemBySlot(EquipmentSlot.HEAD));float s=Math.max(.5f,Config.armorHudScale);int w=items.size()*22+12,h=30;int x=Math.round(mc.getWindow().getGuiScaledWidth()/2f+Config.armorHudX-w*s/2f),y=Math.round(mc.getWindow().getGuiScaledHeight()/2f+Config.armorHudY);NativeGlassVisualSystem.renderHudDock(g,x,y,Math.round(w*s),Math.round(h*s),.72f);g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(s,s);for(int i=0;i<4;i++){ItemStack stack=items.get(i);int xx=6+i*22;g.renderFakeItem(stack,xx,6);if(!stack.isEmpty()&&Config.armorHudShowPercentage){int max=stack.getMaxDamage(),rem=stack.getDamageValue();if(max>0){int pct=Math.round(100f*(max-rem)/max);g.drawString(mc.font,String.valueOf(pct),xx+8,21,0xDFFFFFFF,false);}}}g.pose().popMatrix();}
    public float getEditWidth(){return 100*Math.max(.5f,Config.armorHudScale);}
    public float getEditHeight(){return 30*Math.max(.5f,Config.armorHudScale);}
    public float getRenderX(int w){return w*.5f+Config.armorHudX-getEditWidth()/2f;}
    public float getRenderY(int h){return h*.5f+Config.armorHudY;}
    public boolean isPositionLockedToAdaptiveLayout(){return false;}
}
