package com.dioxidelite.module.modules.movement;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.util.player.FindItemResult;
import com.dioxidelite.util.player.InvUtils;
import com.dioxidelite.util.rotation.AdaptiveRotationController;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import com.dioxidelite.util.world.BlockPlaceHelper;
import com.dioxidelite.util.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 移植自 Vape-v4 LegitScaffoldMode。
 * 边缘潜行 + 自动垫脚：走到方块边缘时自动潜行，在脚下放置方块。
 * 旋转框架可切换 Vape(PID) / DioxideLite。
 */
public final class LegitScaffold extends Module {

    public static final LegitScaffold INSTANCE = new LegitScaffold();

    public enum RotationMode { Vape, DioxideLite }

    private final BooleanSetting requireSneak = add(new BooleanSetting("Require Sneak", false));
    private final DoubleSetting sneakDelay = add(new DoubleSetting("Sneak Delay (ms)", 100.0, 0.0, 500.0, 10.0));
    private final EnumSetting<RotationMode> rotationMode = add(new EnumSetting<>("Rotation Mode", RotationMode.Vape));
    private final DoubleSetting aimSpeed = add(new DoubleSetting("Aim Speed", 3.5, 1.0, 10.0, 0.1));
    private final BooleanSetting placeBlocks = add(new BooleanSetting("Place Blocks", true));
    private final BooleanSetting autoSprint = add(new BooleanSetting("Auto Sprint", true));

    private boolean wasSneaking;
    private long edgeSneakTime = 0L;
    private final AdaptiveRotationController pidController = new AdaptiveRotationController();

    private LegitScaffold() {
        super("Legit Scaffold", Category.MOVEMENT);
    }

    @Override
    protected void onDisable() {
        setSneak(false);
        RotationManager.INSTANCE.releaseSilentRotation(this);
        pidController.clearTarget();
    }

    @Listen
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        if (mc.player.isFallFlying()) return;

        if (requireSneak.get() && !mc.player.isShiftKeyDown()) {
            setSneak(false);
            return;
        }

        boolean atEdge = isAtEdge();
        boolean shouldSneak = atEdge;

        // keep sneaking for a short delay after leaving edge (human-like)
        if (!shouldSneak && System.currentTimeMillis() - edgeSneakTime < sneakDelay.get().longValue()) {
            shouldSneak = true;
        }

        if (mc.player.onGround()) {
            setSneak(shouldSneak);
            if (shouldSneak) {
                edgeSneakTime = System.currentTimeMillis();
            }
        }

        // sprint
        if (autoSprint.get() && mc.player.onGround() && !shouldSneak
                && mc.player.forwardImpulse > 0 && !mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }

        // place blocks at edge
        if (placeBlocks.get() && atEdge && mc.player.onGround()) {
            placeUnderFeet();
        } else {
            RotationManager.INSTANCE.releaseSilentRotation(this);
            pidController.clearTarget();
        }
    }

    private boolean isAtEdge() {
        if (!mc.player.onGround()) return false;
        double mx = mc.player.getDeltaMovement().x;
        double mz = mc.player.getDeltaMovement().z;
        if (mx == 0 && mz == 0) return false;

        AABB box = mc.player.getBoundingBox();
        AABB checkBox = box.inflate(-0.2, 0.0, -0.2).move(mx, -1.0, mz);
        return mc.level.getBlockCollisions(mc.player, checkBox).findAny().isEmpty();
    }

    private void placeUnderFeet() {
        FindItemResult block = InvUtils.findInHotbar(s -> s.getItem() instanceof BlockItem);
        if (block == null || !block.found()) return;

        BlockPos below = mc.player.blockPosition().below();
        if (!BlockUtils.canPlaceAt(below)) return;

        BlockPlaceHelper.PlaceInfo info = BlockPlaceHelper.findPlaceInfo(below, false);
        if (info == null) return;

        Vec3 hit = info.hitPosition();
        if (rotationMode.get() == RotationMode.Vape) {
            pidController.setSpeed(aimSpeed.get().floatValue());
            pidController.setTarget(hit);
            pidController.update();
            RotationManager.INSTANCE.setRotations(
                    new Rot2f(pidController.getCurrentYaw(), pidController.getCurrentPitch()),
                    180.0, Priority.High);
        } else {
            Rot2f rot = RotationUtils.calculate(hit);
            RotationManager.INSTANCE.setRotations(rot, aimSpeed.get(), Priority.High);
        }

        BlockPlaceHelper.place(info, block, true, true);
    }

    private void setSneak(boolean sneak) {
        if (sneak != wasSneaking) {
            wasSneaking = sneak;
            mc.player.setShiftKeyDown(sneak);
        }
    }
}
