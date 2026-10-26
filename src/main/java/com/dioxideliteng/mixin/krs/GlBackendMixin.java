package com.dioxideliteng.mixin.krs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.opengl.GlBackend;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * renderpearl 创建 OpenGL 辅助窗口时的两处修正。
 *
 * <p><b>1. 剥离冲突的窗口子系统标志。</b>{@code GlBackend.createWindow} 最终调用
 * {@code SDL_CreateWindow(title, w, h, 2L | flags)}。实测传入的 {@code flags} 是
 * {@code 0x80020008}，即 {@code SDL_WINDOW_HIDDEN | SDL_WINDOW_VULKAN | 0x80000000}，
 * 合并常量 {@code 2}（{@code SDL_WINDOW_OPENGL}）后得到 {@code 0x8002000A}。</p>
 *
 * <p>{@code SDL_WINDOW_VULKAN} 会让 SDL 把窗口按 Vulkan 通道创建，于是紧随其后的
 * {@code SDL_GL_CreateContext} 报 {@code Couldn't find matching GLX visual}——
 * 这句错误信息有误导性，真实原因是窗口根本不是 OpenGL 窗口。因此这里在位运算结果上
 * 剥掉 {@code VULKAN}；顺带清掉 {@code 0x80000000} 这个非公开状态位。</p>
 *
 * <p><b>2. 申请 8 位模板缓冲</b>（KRS 原版行为）。KRS 的 ESP / NameTags 等 2D 绘制
 * 依赖模板缓冲做遮罩。</p>
 */
@Mixin(GlBackend.class)
public abstract class GlBackendMixin {

    /** SDL_WINDOW_VULKAN，与 SDL_WINDOW_OPENGL 互斥。 */
    @Unique
    private static final long KRS_WINDOW_VULKAN = 0x200000L;
    /** 非公开的内部状态位，不应出现在创建标志里。 */
    @Unique
    private static final long KRS_WINDOW_INVALID_HIGH = 0x80000000L;

    @Inject(method = "createWindow", at = @At("HEAD"))
    private void krs$requestStencilBuffer(String title, int width, int height, long flags,
                                          CallbackInfoReturnable<Long> cir) {
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_STENCIL_SIZE, 8);
    }

    /** 拦截窗口创建，去掉与 OpenGL 互斥的 Vulkan 标志后再交给 SDL。 */
    @WrapOperation(
            method = "createWindow",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/sdl/SDLVideo;SDL_CreateWindow(Ljava/lang/CharSequence;IIJ)J")
    )
    private long krs$sanitizeWindowFlags(CharSequence windowTitle, int w, int h, long windowFlags,
                                         Operation<Long> original) {
        long sanitized = windowFlags & ~KRS_WINDOW_VULKAN & ~KRS_WINDOW_INVALID_HIGH;
        return original.call(windowTitle, w, h, sanitized);
    }
}
