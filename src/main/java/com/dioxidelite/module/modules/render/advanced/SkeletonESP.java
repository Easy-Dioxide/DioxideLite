package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.util.render.Render3DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * Draws the bones of nearby players (and optionally mobs) as a stick figure.
 * <p>
 * Ported from 来源客户端 {@code SkeletonESP} + the pose math of
 * {@code RenderSupport_169}/{@code RenderSupport_168}: the skeleton joints are
 * reconstructed in player-model space (head/neck/hip/shoulders/hands/feet) from
 * the entity's walk animation, body yaw, head yaw and pitch, then projected into
 * world space. Geometry is emitted in the world-space 3D pass.
 */
public final class SkeletonESP extends Module {

    public static final SkeletonESP INSTANCE = new SkeletonESP();

    /** Model units -> world blocks (1/17.066..., the factor used by 来源客户端). */
    private static final double MODEL_SCALE = 0.05859375;
    /** Y coordinate of the model origin inside the entity, in model units. */
    private static final double MODEL_HEIGHT = 24.0;

    public final ColorSetting color = add(new ColorSetting("Color", new Color(120, 215, 255)));
    public final BooleanSetting healthColor = add(new BooleanSetting("Health color", false));
    public final DoubleSetting lineWidth = add(new DoubleSetting("Line width", 1.5, 0.5, 5.0, 0.5));
    public final BooleanSetting joints = add(new BooleanSetting("Joints", true));
    public final DoubleSetting jointSize = add(new DoubleSetting("Joint size", 3.0, 1.0, 8.0, 0.5)
            .visibleWhen(joints::get));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", true));
    public final DoubleSetting range = add(new DoubleSetting("Range", 128.0, 8.0, 256.0, 8.0));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));

    private SkeletonESP() {
        super("SkeletonESP", Category.RENDER);
    }

    // PORT-NOTE: 需要 depth-tested 的线段管线（mixin/vanilla hook）才能把 "Through walls" 真正关掉；
    //            本端口只实现了 Render3DUtils 的常开穿墙线框。GL 点精灵在 Render3DUtils 中不存在，
    //            "Joints" 改用每个关节处一枚极小的填充立方体等价实现。

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }

        PoseStack stack = event.getPoseStack();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        double maxRangeSq = range.get() * range.get();
        float thickness = lineWidth.get().floatValue();
        boolean includeMobs = mobs.get();

        for (Entity candidate : mc.level.entitiesForRendering()) {
            if (!(candidate instanceof LivingEntity target) || target == mc.player) {
                continue;
            }
            if (!target.isAlive() || target.isDeadOrDying()) {
                continue;
            }
            if (!(target instanceof Player) && !includeMobs) {
                continue;
            }
            // 来源客户端 keeps invisible targets only when its see-invisible hook
            // approves them; that integration does not exist here.
            if (target.isInvisible()) {
                continue;
            }
            if (mc.player.distanceToSqr(target) > maxRangeSq) {
                continue;
            }

            Bones bones = buildBones(target, partialTick);
            int argb = boneColor(target);
            drawBones(stack, bones, argb, thickness);
            if (joints.get()) {
                drawJoints(stack, bones, argb, jointSize.get());
            }
        }
    }

    /** Bone colour, optionally fading green -> red with the target's health. */
    private int boneColor(LivingEntity target) {
        int base = color.argb();
        if (!healthColor.get()) {
            return base;
        }
        float maxHealth = target.getMaxHealth();
        float ratio = maxHealth <= 0.0F ? 1.0F : Mth.clamp(target.getHealth() / maxHealth, 0.0F, 1.0F);
        int red = Math.round(255.0F - 100.0F * ratio);
        int green = Math.round(90.0F + 137.0F * ratio);
        return new Color(red, green, 98, (base >>> 24) & 0xFF).getRGB();
    }

    private void drawBones(PoseStack stack, Bones b, int argb, float thickness) {
        Render3DUtils.drawLine(stack, b.head(), b.neck(), argb, thickness);
        Render3DUtils.drawLine(stack, b.neck(), b.hip(), argb, thickness);
        Render3DUtils.drawLine(stack, b.neck(), b.shoulderLeft(), argb, thickness);
        Render3DUtils.drawLine(stack, b.neck(), b.shoulderRight(), argb, thickness);
        Render3DUtils.drawLine(stack, b.shoulderLeft(), b.handLeft(), argb, thickness);
        Render3DUtils.drawLine(stack, b.shoulderRight(), b.handRight(), argb, thickness);
        Render3DUtils.drawLine(stack, b.hip(), b.legTopLeft(), argb, thickness);
        Render3DUtils.drawLine(stack, b.hip(), b.legTopRight(), argb, thickness);
        Render3DUtils.drawLine(stack, b.legTopLeft(), b.footLeft(), argb, thickness);
        Render3DUtils.drawLine(stack, b.legTopRight(), b.footRight(), argb, thickness);
    }

    /** 来源客户端 draws GL point sprites here; a tiny cube is the closest util equivalent. */
    private void drawJoints(PoseStack stack, Bones b, int argb, double sizePx) {
        double half = Math.max(0.01, sizePx * 0.01);
        drawJoint(stack, b.head(), argb, half);
        drawJoint(stack, b.neck(), argb, half);
        drawJoint(stack, b.hip(), argb, half);
        drawJoint(stack, b.shoulderLeft(), argb, half);
        drawJoint(stack, b.shoulderRight(), argb, half);
        drawJoint(stack, b.handLeft(), argb, half);
        drawJoint(stack, b.handRight(), argb, half);
        drawJoint(stack, b.footLeft(), argb, half);
        drawJoint(stack, b.footRight(), argb, half);
    }

    private void drawJoint(PoseStack stack, Vec3 center, int argb, double half) {
        Render3DUtils.drawFilledBox(new AABB(
                center.x - half, center.y - half, center.z - half,
                center.x + half, center.y + half, center.z + half), argb);
    }

    // --- pose reconstruction (来源客户端 RenderSupport_169 / RenderSupport_168) ---

    private record Bones(Vec3 head, Vec3 neck, Vec3 hip,
                         Vec3 shoulderLeft, Vec3 handLeft,
                         Vec3 shoulderRight, Vec3 handRight,
                         Vec3 legTopLeft, Vec3 footLeft,
                         Vec3 legTopRight, Vec3 footRight) {
    }

    private static Bones buildBones(LivingEntity entity, float partialTick) {
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        float limbSwing = entity.walkAnimation.position(partialTick);
        float bodyYaw = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.lerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float headPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        double headYawRad = Mth.wrapDegrees(headYaw - bodyYaw) * (Math.PI / 180.0);
        double headPitchRad = headPitch * (Math.PI / 180.0);

        boolean sneaking = entity.isCrouching();
        float armRightSwing = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        float armLeftSwing = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        float legRightSwing = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        float legLeftSwing = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

        float bodyTilt = 0.0F;
        double bodyLift = 0.0;
        double legTopY = 12.0;
        double legZ = 0.1;
        if (sneaking) {
            bodyTilt = 0.5F;
            armRightSwing += 0.4F;
            armLeftSwing += 0.4F;
            bodyLift = 1.0;
            legTopY = 9.0;
            legZ = 4.0;
        }

        double bodyYawRad = bodyYaw * (Math.PI / 180.0);
        double sinBodyYaw = Math.sin(bodyYawRad);
        double cosBodyYaw = Math.cos(bodyYawRad);

        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        double[] bodyOffset = {0.0, bodyLift, 0.0};
        double[] head = add(bodyOffset, rotate(new double[]{0.0, -4.0, 0.0}, headPitchRad, headYawRad));
        double[] hip = rotate(new double[]{0.0, 12.0, 0.0}, bodyTilt, 0.0);

        double[] shoulderLeft = {-5.0, 2.0, 0.0};
        double[] handLeft = add(shoulderLeft, rotate(new double[]{0.0, 12.0, 0.0}, armRightSwing, 0.0));
        double[] shoulderRight = {5.0, 2.0, 0.0};
        double[] handRight = add(shoulderRight, rotate(new double[]{0.0, 12.0, 0.0}, armLeftSwing, 0.0));

        double[] legTopLeft = {-1.9, legTopY, legZ};
        double[] footLeft = add(legTopLeft, rotate(new double[]{0.0, 10.5, 0.0}, legRightSwing, 0.0));
        double[] legTopRight = {1.9, legTopY, legZ};
        double[] footRight = add(legTopRight, rotate(new double[]{0.0, 10.5, 0.0}, legLeftSwing, 0.0));

        return new Bones(
                toWorld(head, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(bodyOffset, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(hip, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(shoulderLeft, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(handLeft, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(shoulderRight, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(handRight, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(legTopLeft, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(footLeft, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(legTopRight, x, y, z, sinBodyYaw, cosBodyYaw),
                toWorld(footRight, x, y, z, sinBodyYaw, cosBodyYaw));
    }

    /** Rotates a model vector around X by {@code pitch}, then around Y by {@code yaw}. */
    private static double[] rotate(double[] vector, double pitch, double yaw) {
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double y = vector[1] * cosPitch - vector[2] * sinPitch;
        double z = vector[1] * sinPitch + vector[2] * cosPitch;
        double x = vector[0];
        if (yaw != 0.0) {
            double cosYaw = Math.cos(yaw);
            double sinYaw = Math.sin(yaw);
            double rotatedX = x * cosYaw + z * sinYaw;
            z = -x * sinYaw + z * cosYaw;
            x = rotatedX;
        }
        return new double[]{x, y, z};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    /** Model space -> absolute world space, mirroring 来源客户端's final transform. */
    private static Vec3 toWorld(double[] model, double x, double y, double z, double sinYaw, double cosYaw) {
        return new Vec3(
                x + (model[0] * cosYaw + model[2] * sinYaw) * MODEL_SCALE,
                y + (MODEL_HEIGHT - model[1]) * MODEL_SCALE,
                z + (model[0] * sinYaw - model[2] * cosYaw) * MODEL_SCALE);
    }
}
