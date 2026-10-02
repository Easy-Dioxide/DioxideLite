package com.dioxidelite.module.modules.utility;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.util.player.FindItemResult;
import com.dioxidelite.util.player.InvUtils;
import com.dioxidelite.util.rotation.AdaptiveRotationController;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import com.dioxidelite.util.world.BlockPlaceHelper;
import com.dioxidelite.util.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 移植自 Vape-v4 Clutch。掉落自救：
 * - 检测致命掉落 / 虚空掉落 / 超过 N 格掉落
 * - 自动切换到热栏方块，转头看向落点，放置方块垫脚
 * - 旋转框架可切换 Vape(PID) / DioxideLite
 * - 支持 void / lethal / moreThanX 三种触发条件
 */
public final class Clutch extends Module {

    public static final Clutch INSTANCE = new Clutch();

    public enum RotationMode { Vape, DioxideLite }

    private final BooleanSetting onVoid = add(new BooleanSetting("On Void", true));
    private final BooleanSetting onLethalFall = add(new BooleanSetting("On Lethal Fall", true));
    private final BooleanSetting onMoreThanX = add(new BooleanSetting("On More Than X Blocks", false));
    private final DoubleSetting blocksThreshold = add(new DoubleSetting("Blocks", 6.0, 3.0, 10.0, 1.0)
            .visibleWhen(onMoreThanX::get));
    private final DoubleSetting speed = add(new DoubleSetting("Speed", 3.5, 1.0, 10.0, 0.1));
    private final EnumSetting<RotationMode> rotationMode = add(new EnumSetting<>("Rotation Mode", RotationMode.Vape));
    private final BooleanSetting resetAngle = add(new BooleanSetting("Reset Angle", true));
    private final IntSetting resetDelay = add(new IntSetting("Reset Delay (ticks)", 3, 1, 10, 1)
            .visibleWhen(resetAngle::get));
    private final BooleanSetting returnToLastSlot = add(new BooleanSetting("Return To Last Slot", true));
    private final IntSetting returnDelay = add(new IntSetting("Return Delay (ticks)", 3, 1, 10, 1)
            .visibleWhen(returnToLastSlot::get));
    private final BooleanSetting silentAim = add(new BooleanSetting("Silent Aim", true));

    private int resetAngleDelayTicks = 0;
    private int returnDelayTicks = 0;
    private int previousSlot = -1;
    private boolean clutched = false;
    private float savedYaw;
    private float savedPitch;
    private final AdaptiveRotationController pidController = new AdaptiveRotationController();

    private Clutch() {
        super("Clutch", Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        resetState();
    }

    @Override
    protected void onDisable() {
        resetState();
        RotationManager.INSTANCE.releaseSilentRotation(this);
    }

    private void resetState() {
        clutched = false;
        resetAngleDelayTicks = 0;
        returnDelayTicks = 0;
        previousSlot = -1;
        pidController.clearTarget();
    }

    @Listen
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (mc.player.onGround()) {
            if (clutched) {
                clutched = false;
                if (resetAngle.get() && resetAngleDelayTicks == 0) {
                    resetAngleDelayTicks = resetDelay.get();
                }
            }
        }

        // reset angle phase
        if (resetAngleDelayTicks > 0) {
            resetAngleDelayTicks--;
            if (resetAngleDelayTicks == 0) {
                RotationManager.INSTANCE.setRotations(new Rot2f(savedYaw, savedPitch), 180.0, Priority.High);
                RotationManager.INSTANCE.releaseSilentRotation(this);
                pidController.clearTarget();
            }
            return;
        }

        // return slot phase
        if (returnDelayTicks > 0) {
            returnDelayTicks--;
            if (returnDelayTicks == 0 && previousSlot >= 0 && returnToLastSlot.get()) {
                InvUtils.swap(previousSlot, true);
                previousSlot = -1;
            }
        }

        if (mc.player.onGround() || mc.player.getDeltaMovement().y > -0.08) return;

        if (!shouldClutch()) return;

        FindItemResult block = findBlock();
        if (block == null || !block.found()) return;

        BlockPos placePos = findPlacementPosition();
        if (placePos == null) return;

        BlockPlaceHelper.PlaceInfo info = BlockPlaceHelper.findPlaceInfo(placePos, false);
        if (info == null) return;

        // save original rotation
        if (!clutched) {
            savedYaw = mc.player.getYRot();
            savedPitch = mc.player.getXRot();
        }
        clutched = true;

        // rotate
        Vec3 hit = info.hitPosition();
        if (rotationMode.get() == RotationMode.Vape) {
            pidController.setSpeed(speed.get().floatValue());
            pidController.setTarget(hit);
            pidController.update();
            RotationManager.INSTANCE.setRotations(
                    new Rot2f(pidController.getCurrentYaw(), pidController.getCurrentPitch()),
                    180.0, Priority.Highest);
        } else {
            Rot2f rot = RotationUtils.calculate(hit);
            RotationManager.INSTANCE.setRotations(rot, speed.get(), Priority.Highest);
        }

        // place
        if (previousSlot < 0 && returnToLastSlot.get()) {
            previousSlot = mc.player.getInventory().selected;
        }
        BlockPlaceHelper.place(info, block, true, true);

        // schedule return
        if (returnToLastSlot.get() && previousSlot >= 0) {
            returnDelayTicks = returnDelay.get();
        }
    }

    private boolean shouldClutch() {
        if (!onVoid.get() && !onLethalFall.get() && !onMoreThanX.get()) return false;

        // predict landing
        BlockPos landing = predictLanding(40);
        if (landing == null) {
            return onVoid.get(); // falling into void
        }

        double fallDist = mc.player.getY() - landing.getY() - 1;
        if (onLethalFall.get() && fallDist > mc.player.getMaxFallDistance() + 3) {
            return true;
        }
        if (onMoreThanX.get() && fallDist >= blocksThreshold.get()) {
            return true;
        }
        return false;
    }

    private BlockPos predictLanding(int maxTicks) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        double vy = mc.player.getDeltaMovement().y;
        int bottom = mc.level.dimensionType().minY();

        for (int t = 0; t < maxTicks; t++) {
            vy -= 0.08;
            vy *= 0.98;
            y += vy;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (!mc.level.getBlockState(pos).canBeReplaced()) {
                return pos.above();
            }
            if (y < bottom) return null;
        }
        return null;
    }

    private FindItemResult findBlock() {
        return InvUtils.findInHotbar(stack -> stack.getItem() instanceof BlockItem);
    }

    private BlockPos findPlacementPosition() {
        BlockPos playerPos = mc.player.blockPosition();
        // try directly below first
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos candidate = playerPos.below(dy);
            if (BlockUtils.canPlaceAt(candidate)) {
                // need a support adjacent
                for (Direction d : Direction.values()) {
                    BlockPos support = candidate.relative(d);
                    if (!mc.level.getBlockState(support).canBeReplaced()) {
                        return candidate;
                    }
                }
            }
        }
        // try the landing position
        BlockPos landing = predictLanding(30);
        if (landing != null) {
            BlockPos candidate = landing.above();
            if (BlockUtils.canPlaceAt(candidate)) return candidate;
        }
        return null;
    }
}
