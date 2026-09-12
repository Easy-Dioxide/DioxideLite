package com.dioxidelite.ui.dioxide;

import com.dioxidelite.render.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Compact Dynamic Island overlay; visual/QoL only. */
public final class DioxideDynamicIsland {
    private static final DioxideDynamicIsland INSTANCE = new DioxideDynamicIsland();
    private float width = 250, height = 30;
    private long lastNs = System.nanoTime();
    private DioxideDynamicIsland() {}
    public static DioxideDynamicIsland getInstance() { return INSTANCE; }
    public void render(Canvas canvas, float screenW, float screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.screen != null && mc.screen.getClass().getName().contains("Container")) return;
        List<PlayerInfo> players = tabPlayers(mc);
        boolean tab = mc.options != null && mc.options.keyPlayerList.isDown();
        float targetW = tab ? Math.min(screenW - 34, Math.max(340, 150 * Math.min(6, Math.max(1, (players.size()+19)/20)))) : Math.max(250, compactWidth(mc));
        float targetH = tab ? Math.min(screenH - 20, 44 + Math.max(0, (players.size()+5)/6) * 15) : 30;
        float dt = Math.min(.05f, (System.nanoTime()-lastNs)/1_000_000_000f); lastNs = System.nanoTime();
        width += (targetW-width) * Math.min(1, dt*13); height += (targetH-height) * Math.min(1, dt*13);
        float x=(screenW-width)/2f, y=10;
        int alpha = DioxideThemeController.current()==DioxideThemeController.Theme.MINIMAL ? 205 : 225;
        try (Paint p=new Paint().setAntiAlias(true).setColor(0x00000000)) {
            SkijaUi.rounded(canvas,x,y,width,height,16, (alpha<<24)|0x10151A);
            SkijaUi.outline(canvas,x+.5f,y+.5f,width-1,height-1,16,1.0f,0x6078CFFF);
        }
        if (tab) drawTab(canvas,mc,players,x,y,width); else drawCompact(canvas,mc,x,y,width);
    }
    private void drawCompact(Canvas c,Minecraft mc,float x,float y,float w){String user=mc.getUser()==null?"Player":mc.getUser().getName();String server=mc.getCurrentServer()==null?"Singleplayer":mc.getCurrentServer().ip;SkijaUi.text(c,user+"  ·  "+server,x+18,y+19,8.5f,0xEFFFFFFF);String fps="FPS "+mc.getFps();SkijaUi.text(c,fps,x+w-18-SkijaUi.textWidth(fps,8.5f),y+19,8.5f,0xFF78CFFF);}
    private void drawTab(Canvas c,Minecraft mc,List<PlayerInfo> p,float x,float y,float w){int cols=Math.max(1,Math.min(6,(p.size()+19)/20));int rows=Math.max(1,(p.size()+cols-1)/cols);int colW=(int)((w-36-(cols-1)*16)/cols);for(int i=0;i<p.size();i++){int col=i/rows,row=i%rows;PlayerInfo info=p.get(i);String n=info.getProfile().name();if(n==null)n="Player";if(n.length()>18)n=n.substring(0,18);String ping=info.getLatency()+"ms";float xx=x+18+col*(colW+16),yy=y+22+row*15;int color=info.getGameMode()==GameType.SPECTATOR?0xFF8E98A2:0xFFEAF2F4;SkijaUi.text(c,n,xx,yy,7.5f,color);SkijaUi.text(c,ping,xx+colW-SkijaUi.textWidth(ping,7.5f),yy,7.5f,0xFF6D7D86);}}
    private float compactWidth(Minecraft mc){String s=(mc.getUser()==null?"Player":mc.getUser().getName())+"  ·  "+(mc.getCurrentServer()==null?"Singleplayer":mc.getCurrentServer().ip)+"  ·  FPS "+mc.getFps();return SkijaUi.textWidth(s,8.5f)+36;}
    private List<PlayerInfo> tabPlayers(Minecraft mc){if(mc.getConnection()==null)return List.of();List<PlayerInfo> p=new ArrayList<>(mc.getConnection().getListedOnlinePlayers());p.sort(Comparator.comparingInt(PlayerInfo::getTabListOrder).thenComparing(i->i.getProfile().name().toLowerCase()));return p;}
}
