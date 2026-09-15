package com.dioxidelite.module.modules.render;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.manager.FriendManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.util.render.Render3DUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.awt.Color;

/** Functional player ESP. Rendering is intentionally isolated from combat/automation modules. */
public final class ESP extends Module {
    public static final ESP INSTANCE = new ESP();

    public final DoubleSetting range = add(new DoubleSetting("Range", 48.0, 1.0, 128.0, 1.0));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting showSelf = add(new BooleanSetting("Show Self", false));
    public final BooleanSetting friends = add(new BooleanSetting("Friends", true));
    public final BooleanSetting fill = add(new BooleanSetting("Fill", true));
    public final BooleanSetting outline = add(new BooleanSetting("Outline", true));
    public final DoubleSetting lineWidth = add(new DoubleSetting("Line Width", 1.5, 0.5, 5.0, 0.5));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(80, 180, 255, 55), true));
    public final ColorSetting friendColor = add(new ColorSetting("Friend Color", new Color(90, 255, 120, 65), true));

    private ESP() { super("ESP", Category.RENDER); }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer() || !players.get()) return;
        double maxSq = range.get() * range.get();
        for (Player player : mc.level.players()) {
            if (player == null || player.isRemoved() || player.isSpectator()) continue;
            if (player == mc.player && !showSelf.get()) continue;
            if (player.distanceToSqr(mc.player) > maxSq) continue;
            Color c = friends.get() && FriendManager.INSTANCE.isFriend(player) ? friendColor.get() : color.get();
            AABB box = player.getBoundingBox().inflate(0.035);
            if (fill.get()) Render3DUtils.drawFilledBox(box, c);
            if (outline.get()) Render3DUtils.drawOutlineBox(event.getPoseStack(), box, c, lineWidth.get().floatValue());
        }
    }
}
