package mixins.krs;

import com.dioxideliteng.krs.KrsBridge;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.events.features.HandleInputEvent;
import com.dioxideliteng.krs.events.features.TickEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import com.dioxideliteng.krs.hacks.features.level.NoPlaceCooldown;
import com.dioxideliteng.krs.hacks.features.player.AirPlace;
import com.dioxideliteng.krs.hacks.features.render.Camera;
import com.dioxideliteng.krs.hacks.features.render.ViewModel;
import com.dioxideliteng.krs.utils.entity.PlayerUtil;
import com.dioxideliteng.krs.utils.IMinecraft;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client tick / world change / input events and the small client hooks ported KRS modules need. */
@Mixin(Minecraft.class)
public abstract class MixinKrsMinecraft implements IMinecraft {

    @Shadow @Nullable public LocalPlayer player;
    @Shadow protected int missTime;
    @Shadow @Nullable public HitResult hitResult;
    @Shadow @Nullable public ClientLevel level;
    @Shadow @Nullable public MultiPlayerGameMode gameMode;
    @Shadow public Options options;
    @Shadow private int rightClickDelay;

    @Unique
    private HitResult krs$airPlaceOriginalHitResult;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0), method = "tick()V", cancellable = true)
    private void krs$handleInputEvent(CallbackInfo ci) {
        if (this.player == null || !KrsBridge.has(HandleInputEvent.class))
            return;

        if (KrsBridge.call(new HandleInputEvent()))
            ci.cancel();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void krs$handleBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (this.missTime <= 0 && breaking && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) this.hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();
            if (this.level != null && !this.level.getBlockState(blockPos).isAir()) {
                if (ModuleManager.getModuleState(ViewModel.class) && ViewModel.punchingABlockWhileDoingStuff.get()
                    && this.player.isUsingItem() && mc.player.getUseItem().getUseAnimation() != ItemUseAnimation.NONE) {
                    ci.cancel();
                    this.level.addBreakingBlockEffects(blockPos, blockHitResult.getDirection(), false);
                    PlayerUtil.INSTANCE.swingHandWithoutPacket(InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void krs$airPlaceBeforeItemUse(CallbackInfo ci) {
        HitResult placementHit = AirPlace.createPlacementHit(this.hitResult, this.player);
        if (placementHit != this.hitResult) {
            this.krs$airPlaceOriginalHitResult = this.hitResult;
            this.hitResult = placementHit;
        }
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void krs$airPlaceAfterItemUse(CallbackInfo ci) {
        if (this.krs$airPlaceOriginalHitResult != null) {
            this.hitResult = this.krs$airPlaceOriginalHitResult;
            this.krs$airPlaceOriginalHitResult = null;
        }
    }

    @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;cycle()Lnet/minecraft/client/CameraType;"))
    private CameraType krs$cameraCycle(CameraType original) {
        return Camera.nextCameraType(this.options.getCameraType(), original);
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private int krs$noPlaceCooldown(Minecraft minecraft) {
        if (ModuleManager.getModuleState(NoPlaceCooldown.class))
            return 0;

        return this.rightClickDelay;
    }

    @Inject(method = "setLevel", at = @At("HEAD"), cancellable = true)
    private void krs$worldEvent(ClientLevel world, CallbackInfo ci) {
        if (!KrsBridge.has(WorldEvent.class))
            return;

        if (KrsBridge.call(new WorldEvent(this.level, world)))
            ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void krs$tickEvent(CallbackInfo ci) {
        if (!KrsBridge.has(TickEvent.class))
            return;

        if (KrsBridge.call(new TickEvent()))
            ci.cancel();
    }
}
