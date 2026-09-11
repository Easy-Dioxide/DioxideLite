package com.dioxidelite.client.modules.impl.Render;

import com.dioxidelite.Config; import net.minecraft.client.Minecraft; import net.minecraft.client.gui.GuiGraphics; import org.lwjgl.glfw.GLFW;

/** Native keystrokes HUD. */
public final class KeystrokesRenderer {
    private static final KeystrokesRenderer INSTANCE=new KeystrokesRenderer(); private long lastL,lastR; private int leftCps,rightCps; private final RateCounter lc=new RateCounter(),rc=new RateCounter();
    public static KeystrokesRenderer getInstance(){return INSTANCE;}
    public void render(GuiGraphics g){Minecraft mc=Minecraft.getInstance();if(!Config.keystrokes||mc.player==null)return;boolean w=mc.options.keyUp.isDown(),a=mc.options.keyLeft.isDown(),s=mc.options.keyDown.isDown(),d=mc.options.keyRight.isDown(),l=GLFW.glfwGetMouseButton(mc.getWindow().handle(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS,r=GLFW.glfwGetMouseButton(mc.getWindow().handle(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)==GLFW.GLFW_PRESS;if(l)lc.record();if(r)rc.record();leftCps=lc.getCps();rightCps=rc.getCps();float scale=Math.max(.5f,Config.keystrokesScale);int x=Math.round(mc.getWindow().getGuiScaledWidth()*.5f+Config.keystrokesX),y=Math.round(mc.getWindow().getGuiScaledHeight()*.5f+Config.keystrokesY);g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);key(g,24,0,"W",w);key(g,0,27,"A",a);key(g,27,27,"S",s);key(g,54,27,"D",d);key(g,0,54,"LMB "+leftCps,l);key(g,54,54,"RMB "+rightCps,r);g.pose().popMatrix();}
    private void key(GuiGraphics g,int x,int y,String s,boolean active){int fill=active?0xDDF2F4F8:0x7A0E1117;g.fill(x,y,x+24+(s.length()>1?30:0),y+24,fill);g.renderOutline(x,y,24+(s.length()>1?30:0),24,active?0xFF78CFFF:0x40717B83);int tw=Minecraft.getInstance().font.width(s);g.drawString(Minecraft.getInstance().font,s,x+(24+(s.length()>1?30:0)-tw)/2,y+8,active?0xFF171724:0xFFEFF5F7,false);}
    public boolean needsCanvas(){return false;} public void renderFrameEnd(){}
    public float getScaledWidth(){return 81*Math.max(.5f,Config.keystrokesScale);}
    public float getScaledHeight(){return 78*Math.max(.5f,Config.keystrokesScale);}
    public float getRenderX(int w){return w*.5f+Config.keystrokesX;}
    public float getRenderY(int h){return h*.5f+Config.keystrokesY;}
    public float getEditWidth(){return getScaledWidth();}public float getEditHeight(){return getScaledHeight();}
    private static final class RateCounter{private final java.util.ArrayDeque<Long> q=new java.util.ArrayDeque<>();void record(){long n=System.currentTimeMillis();q.add(n);while(!q.isEmpty()&&n-q.peek()>1000)q.poll();}int getCps(){long n=System.currentTimeMillis();while(!q.isEmpty()&&n-q.peek()>1000)q.poll();return q.size();}}
}
