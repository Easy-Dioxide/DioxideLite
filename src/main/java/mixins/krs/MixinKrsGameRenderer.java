package mixins.krs;

import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.player.Freecam;
import com.dioxideliteng.krs.hacks.features.render.FPSBobbing;
import com.dioxideliteng.krs.hacks.features.render.NoHurtCam;
import com.dioxideliteng.krs.hacks.features.render.Zoom;
import com.dioxideliteng.krs.utils.IMinecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Camera/view hooks of the ported render modules (Freecam, Camera, FPSBobbing, Zoom, NoHurtCam).
 * KRS's shader ESP capture, NanoVG queue and debug overlay plumbing in this class are not part of the port.
 */
@Mixin(GameRenderer.class)
public abstract class MixinKrsGameRenderer implements IMinecraft {

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Unique
    private boolean krs$cancelNextBobView;

    @Unique
    private float krs$currentCameraTickDelta;

    // Since 26.3 renderLevel() only renders the extracted state; the camera partial tick is part of that state.
    @Inject(method = "renderLevel()V", at = @At("HEAD"))
    private void krs$captureRenderTickDelta(CallbackInfo ci) {
        krs$currentCameraTickDelta = this.gameRenderState.levelRenderState.cameraRenderState.cameraEntityPartialTicks;
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 0), method = "renderLevel()V")
    private void krs$cameraBobTransformHook(CallbackInfo ci) {
        krs$cancelNextBobView = true;
    }

    @Inject(at = @At("HEAD"), method = "bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", cancellable = true)
    private void krs$onViewBobbing(CameraRenderState cameraRenderState, PoseStack matrices, CallbackInfo ci) {
        if (ModuleManager.getModuleState(Freecam.class) && Freecam.getCanFly()) {
            krs$cancelNextBobView = false;
            ci.cancel();
            return;
        }

        if (krs$cancelNextBobView) {
            if (com.dioxideliteng.krs.hacks.features.render.Camera.shouldDisableCameraBobbing())
                ci.cancel();

            krs$cancelNextBobView = false;
        }

        if (!ci.isCancelled() && ModuleManager.getModuleState(FPSBobbing.class)) {
            Entity cameraEntity = mc.getCameraEntity();
            if (cameraEntity instanceof AbstractClientPlayer player) {
                float tickDelta = krs$currentCameraTickDelta;
                float totalMovement = player.avatarState().getInterpolatedWalkDistance(tickDelta);
                float strideInterp = player.avatarState().getInterpolatedBob(tickDelta);

                float tiltStrength = 3.0f;
                float swingStrength = 5.0f;
                float bobStrength = 0.5f;

                float phase = totalMovement * (float) Math.PI;
                float phaseOffset = 0.2f;
                float verticalBob = -Math.abs(Mth.cos(phase) * strideInterp);
                float horizontalBob = Mth.sin(phase) * strideInterp;

                matrices.translate(horizontalBob * bobStrength, verticalBob * bobStrength, 0.0f);
                matrices.rotate(Axis.ZP.rotationDegrees(horizontalBob * tiltStrength));
                matrices.rotate(Axis.XP.rotationDegrees(Math.abs(Mth.cos(phase - phaseOffset) * strideInterp) * swingStrength));
            }
            ci.cancel();
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void krs$hideHand(CameraRenderState cameraRenderState, PlayerRenderState playerState, GpuTextureView depthTextureView, CallbackInfo ci) {
        krs$cancelNextBobView = false;

        if (Zoom.shouldZoom())
            ci.cancel();
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void krs$noHurtCam(CameraRenderState cameraRenderState, PoseStack matrixStack, CallbackInfo ci) {
        if (ModuleManager.getModuleState(NoHurtCam.class))
            ci.cancel();
    }
}
