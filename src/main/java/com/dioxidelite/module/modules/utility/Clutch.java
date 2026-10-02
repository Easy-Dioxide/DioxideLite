package com.dioxidelite.module.modules.utility;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.util.player.FindItemResult;
import com.dioxidelite.util.player.InvUtils;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import com.dioxidelite.util.world.BlockPlaceHelper;
import com.dioxidelite.util.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.Vec3;

/**
 * 移植自 Vape-v4 Clutch。掉落自救：
 * - 检测致命掉落 / 虚空掉落 / 超过 N 格掉落
 * - 自动切换到热栏方块，转头看向落点，放置方块垫脚
 * - 转头统一走 RotationManager（Vape PID 加速步进）
 */
public final class Clutch extends Module {

    public static final Clutch INSTANCE = new Clutch();

    private final BooleanSetting onVoid = add(new BooleanSetting("On Void", true));
    private final BooleanSetting onLethalFall = add(new BooleanSetting("On Lethal Fall", true));
    private final BooleanSetting onMoreThanX = add(new BooleanSetting("On More Than X Blocks", false));
    private final DoubleSetting blocksThreshold = add(new DoubleSetting("Blocks", 6.0, 3.0, 10.0, 1.0)
            .visibleWhen(onMoreThanX::get));
    private final DoubleSetting speed = add(new DoubleSetting("Speed", 3.5, 1.0, 10.0, 0.1));
    private final BooleanSetting resetAngle = add(new BooleanSetting("Reset Angle", true));
    private final IntSetting resetDelay = add(new IntSetting("Reset Delay (ticks)", 3, 1, 10, 1)
            .visibleWhen(resetAngle::get));
    private final BooleanSetting returnToLastSlot = add(new BooleanSetting("Return To Last Slot", true));
    private final IntSetting returnDelay = add(new IntSetting("Return Delay (ticks)", 3, 1, 10, 1)
            .visibleWhen(returnToLastSlot::get));
    private final BooleanSetting placeDelay = add(new BooleanSetting("Place Delay", true,
            "转头对准后再放置，避免 Grim 的 invalid place 检测"));

    private int resetAngleDelayTicks = 0;
    private int returnDelayTicks = 0;
    private int previousSlot = -1;
    private boolean clutched = false;
    private float savedYaw;
    private float savedPitch;

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

        if (!clutched) {
            savedYaw = mc.player.getYRot();
            savedPitch = mc.player.getXRot();
        }
        clutched = true;

        // 转头到放置点（Vape PID 步进）
        Vec3 hit = info.hitPosition();
        Rot2f rot = RotationUtils.calculate(hit);
        RotationManager.INSTANCE.setRotations(rot, speed.get(), Priority.Highest);

        // 放置前确认已对准，绕过 Grim/Matrix 的 invalid place 检测
        if (placeDelay.get()) {
            float yawDiff = Math.abs(MthWrap(RotationManager.INSTANCE.getYaw() - rot.getYaw()));
            float pitchDiff = Math.abs(RotationManager.INSTANCE.getPitch() - rot.getPitch());
            if (yawDiff > 6.0f || pitchDiff > 6.0f) return; // 未对准，等下 tick
        }

        if (previousSlot < 0 && returnToLastSlot.get()) {
            previousSlot = mc.player.getInventory().selected;
        }
        BlockPlaceHelper.place(info, block, true, true);

        if (returnToLastSlot.get() && previousSlot >= 0) {
            returnDelayTicks = returnDelay.get();
        }
    }

    private static float MthWrap(float v) {
        v %= 360.0f;
        if (v >= 180.0f) v -= 360.0f;
        if (v < -180.0f) v += 360.0f;
        return v;
    }

    private boolean shouldClutch() {
        if (!onVoid.get() && !onLethalFall.get() && !onMoreThanX.get()) return false;

        BlockPos landing = predictLanding(40);
        if (landing == null) {
            return onVoid.get();
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
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos candidate = playerPos.below(dy);
            if (BlockUtils.canPlaceAt(candidate)) {
                for (Direction d : Direction.values()) {
                    BlockPos support = candidate.relative(d);
                    if (!mc.level.getBlockState(support).canBeReplaced()) {
                        return candidate;
                    }
                }
            }
        }
        BlockPos landing = predictLanding(30);
        if (landing != null) {
            BlockPos candidate = landing.above();
            if (BlockUtils.canPlaceAt(candidate)) return candidate;
        }
        return null;
    }
}
