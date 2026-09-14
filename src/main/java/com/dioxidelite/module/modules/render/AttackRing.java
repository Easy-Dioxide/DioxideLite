package com.dioxidelite.module.modules.render;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.util.render.Render3DUtils;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;

/** Visual-only target/attack ring. It only reads nearby entities and never performs attacks. */
public final class AttackRing extends Module {
    public static final AttackRing INSTANCE = new AttackRing();
    public final DoubleSetting searchDistance = add(new DoubleSetting("Player Search Distance", 16.0, 2.0, 64.0, 0.5));
    public final DoubleSetting radius = add(new DoubleSetting("Radius", 0.75, 0.2, 2.5, 0.05));
    public final DoubleSetting height = add(new DoubleSetting("Height", 0.03, 0.01, 0.25, 0.01));
    public final BooleanSetting onlyWhileUsingItem = add(new BooleanSetting("Only While Attacking", false));
    public final IntSetting segments = add(new IntSetting("Segments", 40, 12, 96, 4));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(80, 210, 255, 220), true));
    private AttackRing() { super("Attack Ring", Category.RENDER); }

    @Listen private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (onlyWhileUsingItem.get() && !mc.options.keyAttack.isDown()) return;
        double max = searchDistance.get() * searchDistance.get();
        Player nearest = null; double best = max;
        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive() || player.isSpectator()) continue;
            double d = mc.player.distanceToSqr(player);
            if (d <= best) { best = d; nearest = player; }
        }
        if (nearest == null) return;
        int c = color.argb();
        Render3DUtils.drawCylinder(event.getPoseStack(), nearest.position().add(0, 0.03, 0),
                radius.get(), height.get(), c, 2.0F, segments.get());
    }
}
