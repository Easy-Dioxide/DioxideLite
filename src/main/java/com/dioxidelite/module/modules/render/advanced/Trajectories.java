package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.util.render.ColorUtils;
import com.dioxidelite.util.render.Render3DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Predicts where the held projectile (bow, ender pearl, snowball/egg, splash
 * potion) will land and draws the flight path plus an optional impact marker.
 * <p>
 * Ported from 来源客户端 {@code Trajectories} ({@code FeatureMode_252} speed /
 * gravity pairs, 0.99 drag, 0.16 spawn offset, 0.1 eye drop). The line is drawn
 * in the world-space 3D pass, fading from the configured colour to 35% alpha
 * along the path, exactly like 来源客户端's polyline gradient.
 */
public final class Trajectories extends Module {

    public static final Trajectories INSTANCE = new Trajectories();

    /** Per-tick air drag applied by the vanilla projectile simulation. */
    private static final double DRAG = 0.99;
    /** Forward spawn offset used by vanilla thrown-item spawning. */
    private static final double SPAWN_OFFSET = 0.16;
    /** Vertical spawn offset used by vanilla thrown-item spawning. */
    private static final double SPAWN_EYE_DROP = 0.1;
    /** Half-size of the impact marker box. */
    private static final double IMPACT_BOX = 0.12;

    private enum ProjectileType {
        ENDER_PEARL(1.5, 0.03),
        THROWABLE(1.5, 0.03),
        SPLASH_POTION(0.5, 0.03),
        BOW(3.0, 0.05);

        private final double speed;
        private final double gravity;

        ProjectileType(double speed, double gravity) {
            this.speed = speed;
            this.gravity = gravity;
        }
    }

    public final ColorSetting color = add(new ColorSetting("Color", new Color(120, 215, 255)));
    public final ColorSetting impactColor = add(new ColorSetting("Impact color", new Color(255, 106, 98)));
    public final DoubleSetting lineWidth = add(new DoubleSetting("Line width", 1.5, 0.5, 5.0, 0.5));
    public final BooleanSetting impactMarker = add(new BooleanSetting("Impact marker", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));
    public final DoubleSetting maxTicks = add(new DoubleSetting("Max ticks", 120.0, 20.0, 300.0, 10.0));
    public final BooleanSetting enderPearls = add(new BooleanSetting("Ender pearls", true));
    public final BooleanSetting throwables = add(new BooleanSetting("Snowballs & eggs", true));
    public final BooleanSetting splashPotions = add(new BooleanSetting("Splash potions", true));
    public final BooleanSetting bows = add(new BooleanSetting("Bows", true));
    public final BooleanSetting entityCollision = add(new BooleanSetting("Entity collision", true));

    private Trajectories() {
        super("Trajectories", Category.RENDER);
    }

    // PORT-NOTE: 需要 depth-tested 的线段/填充管线（mixin/vanilla hook）才能把 "Through walls" 真正关掉；
    //            本端口只实现了 Render3DUtils 的常开穿墙线框与填充盒。

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }

        ProjectileType type = resolveType(mc.player.getMainHandItem());
        if (type == null) {
            return;
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        double speed = initialSpeed(type, partialTick);
        if (speed <= 0.0) {
            return;
        }

        Result result = simulate(type, speed);
        if (result.path().size() < 2) {
            return;
        }

        PoseStack stack = event.getPoseStack();
        float thickness = lineWidth.get().floatValue();
        Color base = color.get();
        Color tail = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                Math.round(base.getAlpha() * 0.35F));

        List<Vec3> path = result.path();
        for (int i = 0; i < path.size() - 1; i++) {
            float fraction = (float) i / (path.size() - 1);
            Color segment = ColorUtils.interpolate(base, tail, fraction);
            Render3DUtils.drawLine(stack, path.get(i), path.get(i + 1), segment.getRGB(), thickness);
        }

        Vec3 impact = result.impact();
        if (!impactMarker.get() || impact == null) {
            return;
        }

        AABB box = new AABB(
                impact.x - IMPACT_BOX, impact.y - IMPACT_BOX, impact.z - IMPACT_BOX,
                impact.x + IMPACT_BOX, impact.y + IMPACT_BOX, impact.z + IMPACT_BOX);
        Color marker = impactColor.get();
        Render3DUtils.drawFilledBox(box, marker);
        Render3DUtils.drawOutlineBox(stack, box, new Color(marker.getRed(), marker.getGreen(),
                marker.getBlue(), Math.round(marker.getAlpha() * 0.25F)), thickness);
    }

    private ProjectileType resolveType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == Items.ENDER_PEARL) {
            return enderPearls.get() ? ProjectileType.ENDER_PEARL : null;
        }
        if (item == Items.SNOWBALL || item == Items.EGG) {
            return throwables.get() ? ProjectileType.THROWABLE : null;
        }
        if (item == Items.SPLASH_POTION) {
            return splashPotions.get() ? ProjectileType.SPLASH_POTION : null;
        }
        if (item == Items.BOW) {
            return bows.get() ? ProjectileType.BOW : null;
        }
        return null;
    }

    /** Initial speed in blocks/tick; bows are charged from the use duration like vanilla. */
    private double initialSpeed(ProjectileType type, float partialTick) {
        if (type != ProjectileType.BOW) {
            return type.speed;
        }
        if (!mc.player.isUsingItem()) {
            return 0.0;
        }
        float charge = mc.player.getTicksUsingItem(partialTick) / 20.0F;
        float power = Math.min(1.0F, (charge * charge + charge * 2.0F) / 3.0F);
        if (power < 0.1F) {
            return 0.0;
        }
        return power * type.speed;
    }

    private Result simulate(ProjectileType type, double speed) {
        float yaw = (float) Math.toRadians(mc.player.getYRot());
        float pitch = (float) Math.toRadians(mc.player.getXRot());

        Vec3 eye = mc.player.getEyePosition();
        double x = mc.player.getX() - Math.cos(yaw) * SPAWN_OFFSET;
        double y = eye.y - SPAWN_EYE_DROP;
        double z = mc.player.getZ() - Math.sin(yaw) * SPAWN_OFFSET;

        double motionX = -Math.sin(yaw) * Math.cos(pitch);
        double motionY = -Math.sin(pitch);
        double motionZ = Math.cos(yaw) * Math.cos(pitch);
        double length = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (length < 1.0E-6) {
            return new Result(List.of(), null);
        }
        motionX = motionX / length * speed;
        motionY = motionY / length * speed;
        motionZ = motionZ / length * speed;
        double gravity = type.gravity;

        int ticks = maxTicks.get().intValue();
        List<Vec3> path = new ArrayList<>();
        path.add(new Vec3(x, y, z));
        Vec3 impact = null;

        for (int tick = 0; tick < ticks; tick++) {
            Vec3 from = new Vec3(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            Vec3 to = new Vec3(x, y, z);

            BlockHitResult blockHit = mc.level.clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (blockHit.getType() != HitResult.Type.MISS) {
                impact = blockHit.getLocation();
                path.add(impact);
                break;
            }

            Vec3 entityHit = entityCollision.get() ? rayTraceEntities(from, to) : null;
            if (entityHit != null) {
                impact = entityHit;
                path.add(impact);
                break;
            }

            path.add(to);
            if (y < 0.0) {
                break;
            }
            motionX *= DRAG;
            motionY *= DRAG;
            motionZ *= DRAG;
            motionY -= gravity;
        }

        return new Result(path, impact);
    }

    /** Closest entity hit along one simulation step, with a 0.3 block inflation. */
    private Vec3 rayTraceEntities(Vec3 from, Vec3 to) {
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isPickable()) {
                continue;
            }
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3).clip(from, to);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = from.distanceToSqr(hit.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = hit.get();
            }
        }
        return best;
    }

    private record Result(List<Vec3> path, Vec3 impact) {
    }
}
