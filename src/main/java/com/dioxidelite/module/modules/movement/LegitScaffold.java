package com.dioxidelite.module.modules.movement;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.util.player.FindItemResult;
import com.dioxidelite.util.player.InvUtils;
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
 * 转头统一走 RotationManager（Vape PID 加速步进）。
 */
public final class LegitScaffold extends Module {

    public static final LegitScaffold INSTANCE = new LegitScaffold();

    private final BooleanSetting requireSneak = add(new BooleanSetting("Require Sneak", false));
    private final DoubleSetting sneakDelay = add(new DoubleSetting("Sneak Delay (ms)", 100.0, 0.0, 500.0, 10.0));
    private final DoubleSetting aimSpeed = add(new DoubleSetting("Aim Speed", 3.5, 1.0, 10.0, 0.1));
    private final BooleanSetting placeBlocks = add(new BooleanSetting("Place Blocks", true));
    private final BooleanSetting autoSprint = add(new BooleanSetting("Auto Sprint", true));
    private final BooleanSetting placeCheck = add(new BooleanSetting("Place Check", true));

    private boolean wasSneaking;
    private long edgeSneakTime = 0L;

    private LegitScaffold() {
        super("Legit Scaffold", Category.MOVEMENT);
    }

    @Override
    protected void onDisable() {
        setSneak(false);
        RotationManager.INSTANCE.releaseSilentRotation(this);
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

        if (!shouldSneak && System.currentTimeMillis() - edgeSneakTime < sneakDelay.get().longValue()) {
            shouldSneak = true;
        }

        if (mc.player.onGround()) {
            setSneak(shouldSneak);
            if (shouldSneak) edgeSneakTime = System.currentTimeMillis();
        }

        if (autoSprint.get() && mc.player.onGround() && !shouldSneak
                && mc.player.input.getMoveVector().y > 0 && !mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }

        if (placeBlocks.get() && atEdge && mc.player.onGround()) {
            placeUnderFeet();
        } else {
            RotationManager.INSTANCE.releaseSilentRotation(this);
        }
    }

    private boolean isAtEdge() {
        if (!mc.player.onGround()) return false;
        double mx = mc.player.getDeltaMovement().x;
        double mz = mc.player.getDeltaMovement().z;
        if (mx == 0 && mz == 0) return false;

        AABB box = mc.player.getBoundingBox();
        AABB checkBox = box.inflate(-0.2, 0.0, -0.2).move(mx, -1.0, mz);
        return !mc.level.getBlockCollisions(mc.player, checkBox).iterator().hasNext();
    }

    private void placeUnderFeet() {
        FindItemResult block = InvUtils.findInHotbar(s -> s.getItem() instanceof BlockItem);
        if (block == null || !block.found()) return;

        BlockPos below = mc.player.blockPosition().below();
        if (!BlockUtils.canPlaceAt(below)) return;

        BlockPlaceHelper.PlaceInfo info = BlockPlaceHelper.findPlaceInfo(below, false);
        if (info == null) return;

        Vec3 hit = info.hitPosition();
        Rot2f rot = RotationUtils.calculate(hit);
        RotationManager.INSTANCE.setRotations(rot, aimSpeed.get(), Priority.High);

        // 对准后再放置
        if (placeCheck.get()) {
            float yawDiff = Math.abs(wrapDegrees(RotationManager.INSTANCE.getYaw() - rot.getYaw()));
            float pitchDiff = Math.abs(RotationManager.INSTANCE.getPitch() - rot.getPitch());
            if (yawDiff > 8.0f || pitchDiff > 8.0f) return;
        }

        BlockPlaceHelper.place(info, block, true, true);
    }

    private static float wrapDegrees(float v) {
        v %= 360.0f;
        if (v >= 180.0f) v -= 360.0f;
        if (v < -180.0f) v += 360.0f;
        return v;
    }

    private void setSneak(boolean sneak) {
        if (sneak != wasSneaking) {
            wasSneaking = sneak;
            mc.player.setShiftKeyDown(sneak);
        }
    }
}
