package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.util.render.Render3DUtils;
import com.dioxidelite.util.render.WorldToScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * 让不可见实体重新显现（移植自 来源客户端 {@code features/render/SeeInvisibles}，
 * 原描述 "Renders invisible entities"，原设置：Players / Mobs / Animals / Opacity）。
 *
 * <p>来源的 SeeInvisibles 自己什么都不画：它只提供两个判定 ——
 * {@code L(EntityLivingBase)}（这个不可见实体是否应该被渲染，按玩家 / 敌对怪物 / 被动动物分流）
 * 和 {@code L()}（Opacity 百分比换算出的 0~1 不透明度），由实体渲染 hook 调用。
 * 来源的 SkeletonESP 就调用前者来决定是否跳过不可见目标。</p>
 *
 * <p>本端口把两项判定 1:1 暴露为 {@link #shouldRender(LivingEntity)} 与
 * {@link #opacityFraction()}；在没有实体渲染 hook 的前提下，额外在世界空间
 * （Render3DEvent）给通过判定的不可见实体画一层按 Opacity 取透明度的白色占位框。</p>
 */
// PORT-NOTE: 需要实体渲染 hook（LivingEntityRenderer#isBodyVisible(LivingEntityRenderState)，或修改 render state 的 isInvisible / isInvisibleToPlayer；由 hook 读取本模块的 shouldRender(LivingEntity) 与 opacityFraction()）；本端口只实现了设置面、两项判定与世界空间（Render3DEvent）按 Opacity 绘制的白色占位框。
public final class SeeInvisibles extends Module {

    public static final SeeInvisibles INSTANCE = new SeeInvisibles();

    /** 来源: Players，默认开（"Show invisible players"，来源 默认 5 >> 2 != 0）。 */
    public final BooleanSetting players = add(new BooleanSetting("Players", true));

    /** 来源: Mobs，默认开（"Show invisible hostile mobs"）。 */
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));

    /** 来源: Animals，默认关（"Show invisible passive mobs"，来源 默认 0 != 0）。 */
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    /** 来源: Opacity，默认 60%，范围 10~100%，步进 5%（单位 %）。 */
    public final DoubleSetting opacity = add(new DoubleSetting("Opacity", 60.0, 10.0, 100.0, 5.0));

    /** 占位框描边厚度（来源 没有这个设置，仅用于本端口的替代绘制）。 */
    private static final float PLACEHOLDER_OUTLINE_THICKNESS = 1.5F;

    private SeeInvisibles() {
        super("SeeInvisibles", Category.RENDER);
    }

    /**
     * 对应 来源 {@code L()}：把 Opacity 百分比换算成 0~1 的不透明度，
     * 供实体渲染 hook（或本端口的占位框）使用。
     */
    public float opacityFraction() {
        return opacity.get().floatValue() / 100.0F;
    }

    /**
     * 对应 来源 {@code L(EntityLivingBase)}：模块启用、实体非 null 且确实不可见，
     * 并且该实体所属分类的开关打开时返回 true。玩家 / 敌对怪物 / 被动动物各走各的开关，
     * 其余 LivingEntity 回落到 Animals —— 与 来源的分支一致
     * （来源的 IMob 对应这里的 Monster）。
     */
    public boolean shouldRender(LivingEntity entity) {
        if (!isEnabled() || entity == null || !entity.isInvisible()) {
            return false;
        }
        if (entity instanceof Player) {
            return players.get();
        }
        if (entity instanceof Monster) {
            return mobs.get();
        }
        if (entity instanceof Animal) {
            return animals.get();
        }
        return animals.get();
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }

        PoseStack stack = event.getPoseStack();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Color placeholder = placeholderColor();

        for (Entity candidate : mc.level.entitiesForRendering()) {
            if (!(candidate instanceof LivingEntity entity) || !entity.isAlive() || !shouldRender(entity)) {
                continue;
            }
            if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) {
                continue;
            }

            Vec3 current = WorldToScreen.interpolate(entity, partialTick);
            double half = entity.getBbWidth() * 0.5;
            AABB box = new AABB(
                    current.x - half, current.y, current.z - half,
                    current.x + half, current.y + entity.getBbHeight(), current.z + half);

            Render3DUtils.drawFilledBox(box, placeholder);
            Render3DUtils.drawOutlineBox(stack, box, placeholder, PLACEHOLDER_OUTLINE_THICKNESS);
        }
    }

    /** 来源 没有颜色设置：占位框固定白色，透明度完全来自 Opacity。 */
    private Color placeholderColor() {
        int alpha = Math.round(255.0F * opacityFraction());
        return new Color(255, 255, 255, Math.max(0, Math.min(255, alpha)));
    }
}
