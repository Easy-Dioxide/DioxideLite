package com.dioxidelite.mixin;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.module.modules.render.CameraClip;
import com.dioxidelite.module.modules.render.advanced.Freelook;
import com.dioxidelite.module.modules.render.advanced.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$cameraClipDistance(float cameraDistance, CallbackInfoReturnable<Float> cir) {
        CameraClip cameraClip = CameraClip.INSTANCE;
        if (cameraClip.isEnabled()) {
            cir.setReturnValue(cameraClip.distance.get().floatValue());
        }
    }

    /** Zoom：按缩放倍率改写基础 FOV。 */
    @Inject(method = "calculateFov", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$zoomFov(float baseFov, CallbackInfoReturnable<Float> cir) {
        Zoom zoom = Zoom.INSTANCE;
        if (zoom.isEnabled() && zoom.isZooming()) {
            cir.setReturnValue(zoom.applyFov(baseFov));
        }
    }

    /** Freelook / 平滑相机：改写相机朝向（第三视角分离相机）。 */
    @ModifyArgs(
            method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void DioxideLite$cameraRotation(Args args) {
        Freelook freelook = Freelook.INSTANCE;
        if (freelook.isEnabled() && freelook.isActive()) {
            args.set(0, freelook.cameraYaw());
            args.set(1, freelook.cameraPitch());
            return;
        }
        com.dioxidelite.module.modules.render.advanced.Camera camera =
                com.dioxidelite.module.modules.render.advanced.Camera.INSTANCE;
        if (camera.isEnabled() && camera.isRotationReady()) {
            args.set(0, camera.cameraYaw());
            args.set(1, camera.cameraPitch());
        }
    }

    @ModifyArgs(
            method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void DioxideLite$actionCameraPosition(Args args) {
        CameraClip cameraClip = CameraClip.INSTANCE;
        if (!cameraClip.isEnabled() || !cameraClip.action.get()) {
            return;
        }

        if (DioxideLite.mc().options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            cameraClip.resetCameraPos();
            return;
        }

        Vec3 targetPos = new Vec3(args.get(0), args.get(1), args.get(2));
        cameraClip.updateActionCamera(targetPos);
        Vec3 cameraPos = cameraClip.getCameraPos();
        if (cameraPos != null) {
            args.set(0, cameraPos.x);
            args.set(1, cameraPos.y);
            args.set(2, cameraPos.z);
        }
    }
}
