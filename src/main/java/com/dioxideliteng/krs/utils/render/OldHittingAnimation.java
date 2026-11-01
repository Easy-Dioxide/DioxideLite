package com.dioxideliteng.krs.utils.render;

import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.render.OldHitting;
import com.dioxideliteng.krs.hacks.features.render.ViewModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;

import java.util.Locale;

/**
 * First-person sword-block animations of KRS's OldHitting, as pure pose-stack math.
 *
 * <p>KRS embedded this in a large mixin that rewrote the held-item renderer. Here it is a plain helper that
 * DioxideLiteNG's existing {@code submitArmWithItem} hook calls (see {@code MixinItemInHandRenderer}), so there is a
 * single owner of that method. The transforms are KRS's, including the optional ViewModel item scale/position.
 */
public final class OldHittingAnimation {
    private OldHittingAnimation() {
    }

    /** Applies the selected OldHitting mode to {@code matrices}; the caller then submits the main-hand item. */
    public static void transform(PoseStack matrices, HumanoidArm arm, float equipProgress, float swingProgress) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;

        if (OldHitting.mode.get().equalsIgnoreCase("vanilla")) {
            applyEquipOffset(matrices, arm, equipProgress);
            applySwingOffset(matrices, arm, swingProgress);
            slightlyTiltItemPosition(matrices, arm);
            applyVanillaBlockTransformation(matrices, arm);
        } else {
            matrices.translate(0.0F, 0.0F, -0.02F);
            matrices.rotate(Axis.ZP.rotationDegrees(direction * -1.0F));
            applySwordAnimation(matrices, arm, equipProgress, swingProgress);
            applyFirstPersonDisplayTransform(matrices, arm);
        }
    }

    private static void applySwordAnimation(PoseStack matrices, HumanoidArm arm, float equipProgress, float swingProgress) {
        float swing = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);

        switch (OldHitting.mode.get().toLowerCase(Locale.ROOT)) {
            case "push" -> {
                applyFirstPersonItem(matrices, arm, equipProgress / 2.0F, 0.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * 40.0F / 2.0F, swing / 2.0F, 1.0F, 4.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * 30.0F, 1.0F, swing / 3.0F, 0.0F);
                applyBlockTransformation(matrices, arm);
            }
            case "dash" -> {
                applyFirstPersonItem(matrices, arm, equipProgress / 1.7F, 0.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * 22.0F, swing / 2.0F, 0.0F, 9.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * 50.0F, 0.8F, swing / 2.0F, 0.0F);
                applyBlockTransformation(matrices, arm);
            }
            case "swang" -> {
                applyFirstPersonItem(matrices, arm, equipProgress / 1.7F, 0.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * 74.0F / 2.0F, swing / 2.0F, 1.0F, 4.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * 52.0F, 1.0F, swing / 3.0F, 0.0F);
                applyBlockTransformation(matrices, arm);
            }
            case "swank" -> {
                applyFirstPersonItem(matrices, arm, equipProgress / 2.0F, swingProgress);
                applyMirroredAxisRotation(matrices, arm, swing * 30.0F / 2.0F, -swing, 0.0F, 9.0F);
                applyMirroredAxisRotation(matrices, arm, swing * 40.0F, 1.0F, -swing / 2.0F, 0.0F);
                applyBlockTransformation(matrices, arm);
            }
            case "swonk" -> {
                applyFirstPersonItem(matrices, arm, equipProgress / 1.6F, 0.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * -30.0F / 2.0F, swing / 2.0F, 1.0F, 4.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * 7.5F, 1.0F, swing / 3.0F, 0.0F);
                applyBlockTransformation(matrices, arm);
            }
            case "ethr" -> {
                matrices.translate((arm == HumanoidArm.RIGHT ? 1 : -1) * -0.08F, 0.1F, 0.0F);
                applyFirstPersonItem(matrices, arm, equipProgress / 1.4F, 0.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * 65.0F / 2.0F, swing / 2.0F, 1.0F, 4.0F);
                applyMirroredAxisRotation(matrices, arm, -swing * 60.0F, 1.0F, swing / 3.0F, 0.0F);
                applyBlockTransformation(matrices, arm);
            }
            case "spin" -> {
                float spin = (System.nanoTime() / 1_000_000L % 1000L) / (float) 1000L * 360.0F;

                matrices.rotate(Axis.ZP.rotationDegrees(-spin));
                matrices.translate((arm == HumanoidArm.RIGHT ? 1 : -1) * (-0.05F + 0.15F * 0.85F), 0.2F - 0.15F * 0.53F, 0.0F);
                applyFirstPersonItem(matrices, arm, equipProgress / 1.8F, 0.0F);
                applyBlockTransformation(matrices, arm);
            }
            default -> {
            }
        }
    }

    private static void applyFirstPersonItem(PoseStack matrices, HumanoidArm arm, float equipProgress, float swingProgress) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float realEquipProgress = ViewModel.shouldSkipEquipAnimation() ? 0f : equipProgress;
        float sizeModifier = itemModelScale();

        matrices.translate(direction * 0.56F, -0.52F, -0.71999997F);
        matrices.translate(0.0F, realEquipProgress * -0.6F, 0.0F);
        applyCustomItemPosition(matrices, arm);
        if (sizeModifier != 1.0F)
            matrices.scale(sizeModifier, sizeModifier, sizeModifier);
        matrices.translate(direction * -0.1F, 0.08F, 0.0F);
        matrices.rotate(Axis.YP.rotationDegrees(direction * 45.0F));
        float swingYaw = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float swing = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.rotate(Axis.YP.rotationDegrees(direction * swingYaw * -20.0F));
        matrices.rotate(Axis.ZP.rotationDegrees(direction * swing * -20.0F));
        matrices.rotate(Axis.XP.rotationDegrees(swing * -80.0F));
        matrices.scale(0.4F, 0.4F, 0.4F);
    }

    private static void applyEquipOffset(PoseStack matrices, HumanoidArm arm, float equipProgress) {
        float realEquipProgress = ViewModel.shouldSkipEquipAnimation() ? 0f : equipProgress;
        float sizeModifier = itemModelScale();

        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + realEquipProgress * -0.6F, -0.72F);
        applyCustomItemPosition(matrices, arm);
        if (sizeModifier != 1.0F)
            matrices.scale(sizeModifier, sizeModifier, sizeModifier);
    }

    private static void applySwingOffset(PoseStack matrices, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.rotate(Axis.YP.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.rotate(Axis.ZP.rotationDegrees((float) i * g * -20.0F));
        matrices.rotate(Axis.XP.rotationDegrees(g * -80.0F));
        matrices.rotate(Axis.YP.rotationDegrees((float) i * -45.0F));
    }

    private static void slightlyTiltItemPosition(PoseStack matrices, HumanoidArm arm) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float scale = 0.7585F / 0.86F;
        matrices.scale(scale, scale, scale);
        matrices.translate(direction * -0.084F, 0.059F, 0.08F);
        matrices.rotate(Axis.YP.rotationDegrees(direction * 5.0F));
    }

    private static void applyVanillaBlockTransformation(PoseStack matrices, HumanoidArm arm) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate(direction * -0.15F, 0.16F, 0.15F);
        matrices.rotate(Axis.YP.rotationDegrees(direction * -18.0F));
        matrices.rotate(Axis.ZP.rotationDegrees(direction * 82.0F));
        matrices.rotate(Axis.YP.rotationDegrees(direction * 112.0F));
    }

    private static void applyBlockTransformation(PoseStack matrices, HumanoidArm arm) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate(direction * -0.5F, 0.2F, 0.0F);
        matrices.rotate(Axis.YP.rotationDegrees(direction * 30.0F));
        matrices.rotate(Axis.XP.rotationDegrees(-80.0F));
        matrices.rotate(Axis.YP.rotationDegrees(direction * 60.0F));
    }

    private static void applyFirstPersonDisplayTransform(PoseStack matrices, HumanoidArm arm) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate(0.0F, 4.0F / 16.0F, 2.0F / 16.0F);
        matrices.rotate(Axis.YP.rotationDegrees(direction * -135.0F));
        matrices.rotate(Axis.ZP.rotationDegrees(direction * 25.0F));
        matrices.scale(2.2F, 2.2F, 2.2F);
        matrices.rotate(new Quaternionf().rotationXYZ(
            0.0F,
            (float) Math.toRadians(direction * -90.0F),
            (float) Math.toRadians(direction * 25.0F)
        ).invert());
        matrices.translate(direction * -1.13F / 16.0F, -3.2F / 16.0F, -1.13F / 16.0F);
    }

    private static void applyMirroredAxisRotation(PoseStack matrices, HumanoidArm arm, float degrees, float x, float y, float z) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        applyAxisRotation(matrices, degrees, x, direction * y, direction * z);
    }

    private static void applyAxisRotation(PoseStack matrices, float degrees, float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0F)
            return;

        matrices.rotate(new Quaternionf().rotationAxis((float) Math.toRadians(degrees), x / length, y / length, z / length));
    }

    private static float itemModelScale() {
        if (!ModuleManager.getModuleState(ViewModel.class))
            return 1.0F;

        return Mth.clamp(1.0F + ViewModel.itemSize.get(), 0.1F, 3.0F);
    }

    private static void applyCustomItemPosition(PoseStack matrices, HumanoidArm arm) {
        if (!ModuleManager.getModuleState(ViewModel.class))
            return;

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float positionX = ViewModel.itemPositionX.get();
        float positionY = ViewModel.itemPositionY.get();
        float positionZ = ViewModel.itemPositionZ.get();
        if (positionX == 0.0F && positionY == 0.0F && positionZ == 0.0F)
            return;

        matrices.translate(direction * positionX, positionY, positionZ);
    }
}
