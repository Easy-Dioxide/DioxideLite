package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.manager.FriendManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.util.render.Render3DUtils;
import com.dioxidelite.util.render.WorldToScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * 实体染色（移植自 来源客户端 Chams：按 SELF / ENEMY / TEAM / MOBS 分组给实体上色，
 * 每组可配颜色、透明度、线宽）。
 *
 * PORT-NOTE: 需要实体模型渲染 hook（LivingEntityRenderer/getRenderType 或 RenderLayer）才能真正给模型本身着色；
 * 本端口只实现了按目标类型在世界空间（Render3DEvent）绘制的半透明色块与描边。
 */
public final class Chams extends Module {

    public static final Chams INSTANCE = new Chams();

    /** 来源客户端 TargetType 的四个分组。 */
    public enum TargetType {
        SELF,
        ENEMY,
        TEAM,
        MOBS
    }

    public final BooleanSetting fill = add(new BooleanSetting("Fill", true));
    public final BooleanSetting outline = add(new BooleanSetting("Outline", true));

    public final BooleanSetting self = add(new BooleanSetting("Self", true));
    public final ColorSetting selfColor = add(new ColorSetting("Self Color", new Color(0xFF6AA0FF, true))
            .visibleWhen(self::get));
    public final DoubleSetting selfOpacity = add(new DoubleSetting("Self Opacity", 45.0, 5.0, 100.0, 5.0)
            .visibleWhen(self::get));
    public final DoubleSetting selfLineWidth = add(new DoubleSetting("Self Line Width", 1.5, 0.5, 5.0, 0.5)
            .visibleWhen(self::get));

    public final BooleanSetting enemy = add(new BooleanSetting("Enemy", true));
    public final ColorSetting enemyColor = add(new ColorSetting("Enemy Color", new Color(0xFFFF5555, true))
            .visibleWhen(enemy::get));
    public final DoubleSetting enemyOpacity = add(new DoubleSetting("Enemy Opacity", 45.0, 5.0, 100.0, 5.0)
            .visibleWhen(enemy::get));
    public final DoubleSetting enemyLineWidth = add(new DoubleSetting("Enemy Line Width", 1.5, 0.5, 5.0, 0.5)
            .visibleWhen(enemy::get));

    public final BooleanSetting team = add(new BooleanSetting("Team", true));
    public final ColorSetting teamColor = add(new ColorSetting("Team Color", new Color(0xFF55FF55, true))
            .visibleWhen(team::get));
    public final DoubleSetting teamOpacity = add(new DoubleSetting("Team Opacity", 45.0, 5.0, 100.0, 5.0)
            .visibleWhen(team::get));
    public final DoubleSetting teamLineWidth = add(new DoubleSetting("Team Line Width", 1.5, 0.5, 5.0, 0.5)
            .visibleWhen(team::get));

    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final ColorSetting mobColor = add(new ColorSetting("Mob Color", new Color(0xFFFFCC44, true))
            .visibleWhen(mobs::get));
    public final DoubleSetting mobOpacity = add(new DoubleSetting("Mob Opacity", 45.0, 5.0, 100.0, 5.0)
            .visibleWhen(mobs::get));
    public final DoubleSetting mobLineWidth = add(new DoubleSetting("Mob Line Width", 1.5, 0.5, 5.0, 0.5)
            .visibleWhen(mobs::get));

    private Chams() {
        super("Chams", Category.RENDER);
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }
        PoseStack stack = event.getPoseStack();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living instanceof ArmorStand) {
                continue;
            }
            if (!living.isAlive()) {
                continue;
            }
            if (living == mc.player && mc.options.getCameraType().isFirstPerson()) {
                continue;
            }

            TargetType type = classify(living);
            if (type == null || !enabled(type)) {
                continue;
            }

            Vec3 current = WorldToScreen.interpolate(living, partialTick);
            double half = living.getBbWidth() * 0.5;
            AABB box = new AABB(
                    current.x - half, current.y, current.z - half,
                    current.x + half, current.y + living.getBbHeight(), current.z + half);

            Color base = color(type);
            if (fill.get()) {
                Render3DUtils.drawFilledBox(box, withOpacity(base, opacity(type)));
            }
            if (outline.get()) {
                Render3DUtils.drawOutlineBox(stack, box, base, lineWidth(type));
            }
        }
    }

    private TargetType classify(LivingEntity entity) {
        if (entity == mc.player) {
            return TargetType.SELF;
        }
        if (entity instanceof Player player) {
            return FriendManager.INSTANCE.isFriend(player) ? TargetType.TEAM : TargetType.ENEMY;
        }
        return TargetType.MOBS;
    }

    private boolean enabled(TargetType type) {
        return switch (type) {
            case SELF -> self.get();
            case ENEMY -> enemy.get();
            case TEAM -> team.get();
            case MOBS -> mobs.get();
        };
    }

    private Color color(TargetType type) {
        return switch (type) {
            case SELF -> selfColor.get();
            case ENEMY -> enemyColor.get();
            case TEAM -> teamColor.get();
            case MOBS -> mobColor.get();
        };
    }

    private double opacity(TargetType type) {
        return switch (type) {
            case SELF -> selfOpacity.get();
            case ENEMY -> enemyOpacity.get();
            case TEAM -> teamOpacity.get();
            case MOBS -> mobOpacity.get();
        };
    }

    private float lineWidth(TargetType type) {
        return switch (type) {
            case SELF -> selfLineWidth.get().floatValue();
            case ENEMY -> enemyLineWidth.get().floatValue();
            case TEAM -> teamLineWidth.get().floatValue();
            case MOBS -> mobLineWidth.get().floatValue();
        };
    }

    private static int withOpacity(Color color, double percent) {
        int alpha = (int) Math.round(color.getAlpha() * Mth.clamp(percent / 100.0, 0.0, 1.0));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Mth.clamp(alpha, 0, 255)).getRGB();
    }
}
