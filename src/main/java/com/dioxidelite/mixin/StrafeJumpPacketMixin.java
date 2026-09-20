package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/mixin/StrafeJumpPacketMixin.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.accessor.StrafeJumpPoseAccess;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.module.modules.movement.Speed;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ports StrafeJump's input, packet yaw, and third-person pose state without
 * leaving the virtual yaw on the local camera.
 */
@Mixin(LocalPlayer.class)
public class StrafeJumpPacketMixin implements StrafeJumpPoseAccess {

    @Unique private boolean dioxidelite$silentRotationClaimed;
    @Unique private float dioxidelite$lastVisualBodyOffset;
    @Unique private float dioxidelite$visualBodyOffset;
    @Unique private float dioxidelite$lastVisualHeadOffset;
    @Unique private float dioxidelite$visualHeadOffset;
    @Unique private boolean dioxidelite$synchronizedStrafeTick;
    @Unique private Input dioxidelite$physicalPlayerInput;

    @Inject(method = "applyInput", at = @At("TAIL"))
    private void dioxidelite$afterMovementInput(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        Speed speed = Speed.INSTANCE;
        boolean active = speed.shouldApplyFortyFive(self);

        float side = self.xxa;
        float forward = self.zza;
        float length = (float) Math.sqrt(side * side + forward * forward);
        Speed.StrafeMode strafeMode = speed.strafeMode.get();

        boolean synchronizedEligible = active
                && forward > 1.0E-4F
                && Math.abs(side) < 1.0E-4F;
        dioxidelite$synchronizedStrafeTick = strafeMode == Speed.StrafeMode.PREDICTION_SYNCHRONIZED
                && synchronizedEligible;

        if (!active || length < 1.0E-4F
                || (strafeMode == Speed.StrafeMode.PREDICTION_SYNCHRONIZED
                && !synchronizedEligible)) {
            return;
        }

        if (strafeMode == Speed.StrafeMode.PREDICTION_SYNCHRONIZED) {
            float scale = 0.70710677F / length;
            self.xxa = (side + forward) * scale;
            self.zza = (forward - side) * scale;

            dioxidelite$physicalPlayerInput = self.input.keyPresses;
            float virtualSide = side + forward;
            float virtualForward = forward - side;
            Input physical = dioxidelite$physicalPlayerInput;
            self.input.keyPresses = new Input(
                    virtualForward > 1.0E-4F,
                    virtualForward < -1.0E-4F,
                    virtualSide > 1.0E-4F,
                    virtualSide < -1.0E-4F,
                    physical.jump(),
                    physical.shift(),
                    physical.sprint());
        } else if (strafeMode == Speed.StrafeMode.PACKET_SIMULATION) {
            self.xxa = side + forward;
            self.zza = forward - side;
        } else {
            float targetLength = Math.min(1.0F, length / 0.98F);
            float scale = targetLength / length;
            self.xxa = side * scale;
            self.zza = forward * scale;
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
            shift = At.Shift.AFTER))
    private void dioxidelite$updatePoseAfterMovement(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        dioxidelite$updateVisualPose(self, dioxidelite$isPacketStrafeActive(self));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V"))
    private void dioxidelite$beforeMovementPacket(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!dioxidelite$isPacketStrafeActive(self)) {
            return;
        }

        dioxidelite$silentRotationClaimed = RotationManager.INSTANCE.claimSilentRotation(
                Speed.INSTANCE,
                new Rot2f(self.getYRot() + 45.0F, self.getXRot()),
                Priority.Lowest
        );
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V",
            shift = At.Shift.AFTER))
    private void dioxidelite$afterMovementPacket(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        dioxidelite$releaseSilentRotation();
        dioxidelite$restorePhysicalInput(self);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dioxidelite$finishMovementPacketTick(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        dioxidelite$releaseSilentRotation();
        dioxidelite$restorePhysicalInput(self);
    }

    @Unique
    private void dioxidelite$releaseSilentRotation() {
        if (!dioxidelite$silentRotationClaimed) {
            return;
        }
        RotationManager.INSTANCE.releaseSilentRotation(Speed.INSTANCE);
        dioxidelite$silentRotationClaimed = false;
    }

    @Unique
    private boolean dioxidelite$isPacketStrafeActive(LocalPlayer player) {
        Speed speed = Speed.INSTANCE;
        if (!speed.shouldApplyFortyFive(player)) {
            return false;
        }
        return !speed.strafeMode.is(Speed.StrafeMode.PREDICTION_SYNCHRONIZED)
                || dioxidelite$synchronizedStrafeTick;
    }

    @Unique
    private void dioxidelite$restorePhysicalInput(LocalPlayer player) {
        if (dioxidelite$physicalPlayerInput != null) {
            player.input.keyPresses = dioxidelite$physicalPlayerInput;
            dioxidelite$physicalPlayerInput = null;
        }
    }

    @Unique
    private void dioxidelite$updateVisualPose(LocalPlayer player, boolean active) {
        dioxidelite$lastVisualBodyOffset = dioxidelite$visualBodyOffset;
        dioxidelite$lastVisualHeadOffset = dioxidelite$visualHeadOffset;

        float targetHeadOffset = active ? 45.0F : 0.0F;
        float headDelta = Mth.wrapDegrees(targetHeadOffset - dioxidelite$visualHeadOffset);
        float turnSpeed = Speed.INSTANCE.strafeTurnSpeed.get().floatValue();
        dioxidelite$visualHeadOffset += Mth.clamp(headDelta, -turnSpeed, turnSpeed);

        float targetBodyOffset = active && player.attackAnim > 0.0F
                ? dioxidelite$visualHeadOffset : 0.0F;
        dioxidelite$visualBodyOffset += Mth.wrapDegrees(
                targetBodyOffset - dioxidelite$visualBodyOffset) * 0.3F;

        float relative = Mth.wrapDegrees(dioxidelite$visualHeadOffset - dioxidelite$visualBodyOffset);
        if (Math.abs(relative) > 50.0F) {
            dioxidelite$visualBodyOffset += relative - Math.copySign(50.0F, relative);
        }
    }

    @Override
    public float dioxidelite$getVisualBodyOffset(float partialTick) {
        return Mth.rotLerp(partialTick, dioxidelite$lastVisualBodyOffset, dioxidelite$visualBodyOffset);
    }

    @Override
    public float dioxidelite$getVisualHeadOffset(float partialTick) {
        return Mth.rotLerp(partialTick, dioxidelite$lastVisualHeadOffset, dioxidelite$visualHeadOffset);
    }

    @Override
    public boolean dioxidelite$isSynchronizedStrafeTick() {
        return dioxidelite$synchronizedStrafeTick;
    }
}
