package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.player.AirPlace;
import com.dioxideliteng.krs.events.features.HandleInputEvent;
import com.dioxideliteng.krs.events.features.TickEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import com.dioxideliteng.krs.hacks.features.render.ViewModel;
import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.screen.CustomTitleScreen;
import com.dioxideliteng.krs.utils.IMinecraft;
import com.dioxideliteng.krs.utils.entity.PlayerUtil;
import com.dioxideliteng.krs.utils.render.GraphicsApiCompatibility;
import com.dioxideliteng.krs.nvgu.NVGU;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * KRS 客户端生命周期与核心事件分发。
 *
 * <p>这个 Mixin 承担四件移植体系无可替代的事：</p>
 * <ol>
 *   <li><b>兼容渲染层初始化</b>——{@code GraphicsApiCompatibility.initialize()} 必须在
 *       {@code RenderSystem.initRenderer} 之后立刻执行。缺少它会让 Vulkan 后端下
 *       {@code usesCompatibilityRenderer()} 恒为 false，ImGui / NanoVG 直接对
 *       Vulkan 上下文发 GL 调用，触发 {@code No context is current} 并让 JVM abort。</li>
 *   <li><b>核心事件分发</b>——{@code TickEvent} / {@code WorldEvent} / {@code HandleInputEvent}
 *       的唯一派发点，所有依赖它们的模块（移动、战斗、自动化）都靠这里才能运行。</li>
 *   <li><b>AirPlace</b>——临时替换 {@code hitResult} 实现空放。</li>
 *   <li><b>ViewModel 的挖掘抑制</b>——边用物品边挖方块时不触发挖掘动作。</li>
 * </ol>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin implements IMinecraft {

    @Shadow @Nullable public LocalPlayer player;

    @Shadow protected int missTime;

    @Shadow @Nullable public HitResult hitResult;

    @Shadow @Nullable public ClientLevel level;

    @Shadow @Nullable public MultiPlayerGameMode gameMode;

    @Shadow @Final public ParticleEngine particleEngine;

    @Shadow public Options options;

    @Shadow public abstract Window getWindow();

    @Unique
    private Boolean krs$lastAllowCursorChanges;

    @Unique
    private HitResult krs$airPlaceOriginalHitResult;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(Lcom/mojang/renderpearl/api/device/GpuDevice;)V", shift = At.Shift.AFTER))
    private void krs$initNanoVG(CallbackInfo ci) {
        GraphicsApiCompatibility.initialize();
        GraphicsApiCompatibility.runWithOpenGlContext(NVGU.INSTANCE::create);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void krs$syncAllowCursorChanges(CallbackInfo callback) {
        krs$updateAllowCursorChanges();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void krs$stopMainMenuMusic(CallbackInfo ci) {
        CustomTitleScreen.stopMainMenuMusic();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0), method = "tick()V", cancellable = true)
    private void krs$handleInputEvent(CallbackInfo ci) {
        if (this.player == null)
            return;
        if (KrsClient.eventManager == null || !KrsClient.eventManager.hasListeners(HandleInputEvent.class))
            return;

        HandleInputEvent event = new HandleInputEvent();
        KrsClient.eventManager.call(event);
        if (event.isCancelled())
            ci.cancel();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void krs$handleBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (this.missTime <= 0) {
            if (breaking && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) this.hitResult;
                BlockPos blockPos = blockHitResult.getBlockPos();
                if (this.level != null && !this.level.getBlockState(blockPos).isAir()) {
                    if (ModuleManager.getModuleState(ViewModel.class)
                            && ViewModel.punchingABlockWhileDoingStuff.get()
                            && this.player.isUsingItem()
                            && mc.player.getUseItem().getUseAnimation() != ItemUseAnimation.NONE) {
                        ci.cancel();
                        this.level.addBreakingBlockEffects(blockPos, blockHitResult.getDirection(), false);
                        PlayerUtil.INSTANCE.swingHandWithoutPacket(InteractionHand.MAIN_HAND);
                    }
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

    @Inject(method = "setLevel", at = @At("HEAD"), cancellable = true)
    private void krs$worldEvent(ClientLevel world, CallbackInfo ci) {
        if (KrsClient.eventManager == null || !KrsClient.eventManager.hasListeners(WorldEvent.class))
            return;

        WorldEvent event = new WorldEvent(this.level, world);
        KrsClient.eventManager.call(event);
        if (event.isCancelled())
            ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void krs$tickEvent(CallbackInfo ci) {
        krs$updateAllowCursorChanges();

        if (KrsClient.eventManager == null || !KrsClient.eventManager.hasListeners(TickEvent.class))
            return;

        TickEvent event = new TickEvent();
        KrsClient.eventManager.call(event);
        if (event.isCancelled())
            ci.cancel();
    }

    @Unique
    private void krs$updateAllowCursorChanges() {
        if (this.options == null)
            return;

        Window window = this.getWindow();
        if (window == null)
            return;

        Boolean allowCursorChanges = (Boolean) this.options.allowCursorChanges().get();
        if (!Objects.equals(krs$lastAllowCursorChanges, allowCursorChanges)) {
            window.setAllowCursorChanges(Boolean.TRUE.equals(allowCursorChanges));
            krs$lastAllowCursorChanges = allowCursorChanges;
        }
    }
}
