package com.dioxideliteng.util.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.sdl.SDLVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;

public final class NVGRenderer {

   private static final Logger LOGGER = LoggerFactory.getLogger("dioxideliteng-nvg");

   private static long VG = 0;

   private static final NanoVGStencilBuffer STENCIL = new NanoVGStencilBuffer();
   public static final NVGPaint NVG_PAINT = NVGPaint.create();
   public static final NVGColor NVG_COLOR_1 = NVGColor.create();
   public static final NVGColor NVG_COLOR_2 = NVGColor.create();

   private static NanoVGBackend gpuBackend;
   private static boolean frameStarted;
   private static boolean failed;
   private static boolean glStatePushed;
   private static volatile boolean environmentLogged;
   private static volatile String disabledReason;
   /** 连续多少次探测不到当前 GL 上下文后才永久降级，避免启动早期误判。 */
   private static int missingContextFrames;
   private static final int MISSING_CONTEXT_LIMIT = 3;
   public static float globalAlpha = 1;

   private static final Minecraft mc = Minecraft.getInstance();

   private static long getVG() {
      if (VG != 0) {
         return VG;
      }
      if (usesGpuBackend()) {
         gpuBackend = new NanoVGBackend(RenderSystem.getDevice());
         VG = gpuBackend.context();
      } else {
         // NanoVGGL3 会立刻调用原生 GL 函数。没有当前上下文时这会触发
         // LWJGL 的原生 abort（Java try/catch 拦不住），必须在调用前拦下。
         if (!hasCurrentOpenGlContext()) {
            throw new IllegalStateException("Refusing to create a NanoVG OpenGL context "
                  + "without a current OpenGL context on this thread");
         }
         VG = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
      }
      if (VG == 0) {
         throw new IllegalStateException("Failed to create NanoVG context");
      }
      return VG;
   }

   /**
    * 验证「当前线程真的有一个 make-current 的 OpenGL 上下文」。
    *
    * <p>{@code GL.getCapabilities()} 只是线程局部的缓存，即使上下文已被解绑仍可能非
    * null；只看它会误判，随后任何裸 GL 调用都会让 JVM 原生 abort。SDL 的
    * {@code SDL_GL_GetCurrentContext()} 才是权威依据。</p>
    */
   private static boolean hasCurrentOpenGlContext() {
      try {
         if (SDLVideo.SDL_GL_GetCurrentContext() == 0L) {
            return false;
         }
         return tryGetCapabilities() != null;
      } catch (Throwable ignored) {
         return false;
      }
   }

   /** LWJGL 在没有 capabilities 时是抛异常而不是返回 null，这里静默归一化。 */
   private static GLCapabilities tryGetCapabilities() {
      try {
         return GL.getCapabilities();
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static void logEnvironmentOnce() {
      if (!environmentLogged) {
         environmentLogged = true;
         RenderDiagnostics.reportEnvironment();
      }
   }

   private static void noteMissingContext() {
      missingContextFrames++;
      if (missingContextFrames == MISSING_CONTEXT_LIMIT) {
         disableGlPath("no current OpenGL context after " + MISSING_CONTEXT_LIMIT + " probes");
      } else if (missingContextFrames == 1) {
         RenderDiagnostics.report("nvgContext", "OpenGL context not current yet; "
               + "skipping the NanoVG frame while the renderer settles");
      }
   }

   private static void disableGlPath(String reason) {
      if (disabledReason == null) {
         disabledReason = reason;
         failed = true;
         RenderDiagnostics.report("nvgGlDisabled", reason);
         LOGGER.warn("DioxideLiteNG: OpenGL NanoVG UI disabled ({}); falling back to vanilla screens", reason);
      }
   }

   public static void ensureContext() {
      getVG();
   }

   /** Only called at shutdown, while the device and native context are still alive. */
   public static void close() {
      try {
         com.dioxideliteng.ui.mainmenu.launch.LaunchResources.close();
         HudBackdrop.close();
      } catch (Throwable error) {
         RenderDiagnostics.report("close.resources", error);
      }
      if (VG != 0) {
         if (frameStarted) {
            try { nvgCancelFrame(VG); }
            catch (Throwable error) { RenderDiagnostics.report("close.cancelFrame", error); }
         }
         if (gpuBackend != null) {
            try { gpuBackend.close(); }
            catch (Throwable error) { RenderDiagnostics.report("close.gpuBackend", error); }
            gpuBackend = null;
         } else if (hasCurrentOpenGlContext()) {
            try { STENCIL.close(); nvgDelete(VG); }
            catch (Throwable error) { RenderDiagnostics.report("close.gl", error); }
         } else {
            // b14 的 ImguiLoader.shutdown() 就是在这里无上下文调 glDeleteBuffers
            // 导致 JVM 原生 abort。宁可泄漏几个 GL 对象，也不能在关机时崩端。
            RenderDiagnostics.report("close", "skipping OpenGL resource release: no current context");
         }
         VG = 0;
      }
      frameStarted = false;
      glStatePushed = false;
      try { scissors.clear(); FontRepository.clear(); }
      catch (Throwable error) { RenderDiagnostics.report("close.state", error); }
   }

   public static boolean isAvailable() {
      logEnvironmentOnce();
      if (failed) return false;
      if (usesGpuBackend()) {
         try { getVG(); return true; }
         catch (RuntimeException error) { unavailable(error); return false; }
      }
      // OpenGL（或未知）后端：NanoVGGL3 会提交裸 GL，必须先确认上下文仍然有效。
      try {
         GLCapabilities caps = tryGetCapabilities();
         if (caps == null || !caps.OpenGL33) {
            // 设备还没初始化好或驱动不足 3.3：静默跳过这一帧，不永久降级。
            return false;
         }
         if (!hasCurrentOpenGlContext()) {
            noteMissingContext();
            return false;
         }
         missingContextFrames = 0;
         return true;
      } catch (Throwable error) {
         // 探测失败不是错误，只是这一帧不画自定义 UI，不能每帧刷诊断日志。
         return false;
      }
   }

   /** 任何 Vulkan 变体都走 renderpearl GPU 后端，避免后端名带后缀时误落裸 GL 分支。 */
   public static boolean usesGpuBackend() {
      // 诊断开关：强制走 NanoVGGL3 裸 GL 分支，用于在只有 Vulkan 的环境里
      // 验证「无当前 GL 上下文」时的降级逻辑。默认关闭，生产环境不受影响。
      if (Boolean.getBoolean("dioxideliteng.forceGlPath")) {
         return false;
      }
      try {
         var device = RenderSystem.tryGetDevice();
         if (device == null) return false;
         String name = device.getDeviceInfo().backendName();
         return name != null && name.toLowerCase(Locale.ROOT).contains("vulkan");
      } catch (Throwable error) {
         // 不能在这里写诊断日志：RenderDiagnostics 探测环境时会回调本方法，
         // 异常路径会形成无限递归。仅记录到普通日志。
         LOGGER.warn("DioxideLiteNG: graphics backend probe failed", error);
         return false;
      }
   }

   public static boolean isFailed() { return failed; }

   public static String getDisabledReason() { return disabledReason; }

   public static boolean beginFrame() {
      if (!isAvailable()) {
         return false;
      }
      try {
         long vg = getVG();
         if (frameStarted) {
            return false;
         }
         if (gpuBackend == null) {
            // GLUtility / NanoVGGL3 全是裸 GL。二次确认后再进入，
            // 否则 Windows OpenGL 后端会在 GL11.glGetIntegerv 处原生 abort。
            if (!hasCurrentOpenGlContext()) {
               noteMissingContext();
               return false;
            }
            GLUtility.setup();
            GLUtility.push();
            glStatePushed = true;
            GLUtility.prepareNanoVG();
         }
         // The framebuffer bridge uses native texture interop and is optional.
         // Keep the main menu independent from it; enabling it during startup
         // can trigger an access violation on the 26.3 SDL/OpenGL path.

         final int guiWidth = mc.getWindow().getGuiScaledWidth();
         final int guiHeight = mc.getWindow().getGuiScaledHeight();
         final float devicePixelRatio = (float)mc.getWindow().getWidth() / (float)guiWidth;
         nvgBeginFrame(vg, guiWidth, guiHeight, devicePixelRatio);
         if (!scissors.isEmpty()) {
            useCurrentScissors();
         }
         frameStarted = true;
         return true;
      } catch (Throwable error) {
         // 原生 abort 到这里已经拦不住，但 Java 层异常必须接住，
         // 否则会顺着 MixinGuiRenderer 冒泡成一次正常的「崩端」。
         failed = true;
         RenderDiagnostics.report("beginFrame", error);
         LOGGER.error("DioxideLiteNG NanoVG frame start failed; using vanilla screens", error);
         frameStarted = false;
         return false;
      }
   }

   public static void endFrame() {
      if (!frameStarted) {
         return;
      }
      final long vg;
      try {
         vg = getVG();
      } catch (Throwable error) {
         RenderDiagnostics.report("endFrame.getVG", error);
         frameStarted = false;
         failed = true;
         return;
      }
      final RenderTarget target = mc.gameRenderer.mainRenderTarget();
      try {
         if (target == null || target.getColorTextureView() == null) {
            nvgCancelFrame(vg);
            return;
         }
         if (gpuBackend != null) {
            try {
               nvgEndFrame(vg);
               gpuBackend.render(target.getColorTextureView(), target.width, target.height);
            } catch (RuntimeException error) { unavailable(error); }
            return;
         }
         // 裸 GL 分支：提交前最后确认一次当前上下文。拿不到就取消本帧，
         // 绝不带着无效的上下文去调 GL11.glViewport / glGenRenderbuffers。
         if (!hasCurrentOpenGlContext()) {
            RenderDiagnostics.report("endFrame", "skipped raw GL submission: no current OpenGL context");
            try { nvgCancelFrame(vg); } catch (Throwable ignored) { }
            noteMissingContext();
            return;
         }
         // GuiRenderer has closed its pass. Bind the main target again before
         // NanoVG submits raw GL commands, otherwise they land in a stale FBO.
         try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                 .createRenderPass(() -> "dioxideliteng/nvg", target.getColorTextureView(), java.util.Optional.empty())) {
            org.lwjgl.opengl.GL11.glViewport(0, 0, target.width, target.height);
            GLUtility.prepareNanoVG();
            STENCIL.render(target.width, target.height, () -> nvgEndFrame(vg));
         }
      } catch (Throwable error) {
         failed = true;
         RenderDiagnostics.report("endFrame", error);
         LOGGER.error("DioxideLiteNG NanoVG frame end failed; using vanilla screens", error);
      } finally {
         try {
            if (gpuBackend == null && glStatePushed) {
               GLUtility.pop();
            }
         } catch (Throwable error) {
            RenderDiagnostics.report("endFrame.pop", error);
         }
         glStatePushed = false;
         frameStarted = false;
      }
   }

   private static void unavailable(RuntimeException error) {
      failed = true;
      RenderDiagnostics.report("gpuBackendUnavailable", error);
      LOGGER.error("DioxideLiteNG GPU UI unavailable; using native screens", error);
   }

   public static void clearScissors() {
      scissors.clear();
   }

   public static void globalAlpha(float alpha) {
      globalAlpha = alpha;
      nvgGlobalAlpha(getVG(), alpha);
   }

   public static void rect(float x, float y, float width, float height, int color) {
      applyColor(color, NVG_COLOR_1);

      nvgBeginPath(getVG());
      nvgFillColor(getVG(), NVG_COLOR_1);
      nvgRect(getVG(), x, y, width, height);
      nvgFill(getVG());
      nvgClosePath(getVG());
   }

   public static void rect(float x, float y, float width, float height, NVGPaint nvgPaint) {
      nvgBeginPath(getVG());
      nvgFillPaint(getVG(), nvgPaint);
      nvgRect(getVG(), x, y, width, height);
      nvgFill(getVG());
      nvgClosePath(getVG());
   }

   public static void scale(float factor, float x, float y, float width, float height, Runnable content) {
      long vg = getVG();
      float translateX = x + width / 2F;
      float translateY = y + height / 2F;

      nvgSave(vg);
      nvgTranslate(vg, translateX, translateY);
      nvgScale(vg, factor, factor);
      nvgTranslate(vg, -translateX, -translateY);

      try {
         content.run();
      } finally {
         nvgRestore(vg);
      }
   }

   public static void rectStroke(float x, float y, float width, float height, float strokeThickness, int color, int strokeColor) {
      rect(x - strokeThickness, y - strokeThickness, width + strokeThickness * 2, height + strokeThickness * 2, strokeColor);
      rect(x, y, width, height, color);
   }

   public static void rotate(double degrees, float x, float y, float width, float height, Runnable content) {
      long vg = getVG();
      float translateX = x + width / 2f;
      float translateY = y + height / 2f;

      nvgSave(vg);
      nvgTranslate(vg, translateX, translateY);
      nvgRotate(vg, (float)Math.toRadians(degrees));

      try {
         content.run();
      } finally {
         nvgRestore(vg);
      }
   }

   public static void rectOutline(float x, float y, float width, float height, float thickness, int color) {
      applyColor(color, NVG_COLOR_1);

      nvgBeginPath(getVG());
      nvgFillColor(getVG(), NVG_COLOR_1);
      nvgRect(getVG(), x, y, width, thickness);
      nvgFill(getVG());
      nvgClosePath(getVG());

      nvgBeginPath(getVG());
      nvgFillColor(getVG(), NVG_COLOR_1);
      nvgRect(getVG(), x + width - thickness, y + thickness, thickness, height - thickness);
      nvgFill(getVG());
      nvgClosePath(getVG());

      nvgBeginPath(getVG());
      nvgFillColor(getVG(), NVG_COLOR_1);
      nvgRect(getVG(), x, y + height - thickness, width - thickness, thickness);
      nvgFill(getVG());
      nvgClosePath(getVG());

      nvgBeginPath(getVG());
      nvgFillColor(getVG(), NVG_COLOR_1);
      nvgRect(getVG(), x, y + thickness, thickness, height - thickness);
      nvgFill(getVG());
      nvgClosePath(getVG());
   }

   private static final List<ScreenPosition> scissors = new ArrayList<>();

   public static void scissor(float x, float y, float width, float height, Runnable content) {
      long vg = getVG();
      ScreenPosition scissor = new ScreenPosition(x, y, width, height);
      scissors.add(scissor);

      try {
         nvgIntersectScissor(vg, x, y, width, height);
         content.run();
      } finally {
         nvgResetScissor(vg);
         scissors.remove(scissor);
         useCurrentScissors();
      }
   }

   private static void useCurrentScissors() {
      for (ScreenPosition scissor : scissors) {
         nvgIntersectScissor(getVG(), scissor.getX(), scissor.getY(), scissor.getWidth(), scissor.getHeight());
      }
   }

   public static void roundedRect(float x, float y, float width, float height, float radius, int color) {
      applyColor(color, NVG_COLOR_1);

      nvgBeginPath(getVG());
      nvgFillColor(getVG(), NVG_COLOR_1);
      nvgRoundedRect(getVG(), x, y, width, height, radius);
      nvgFill(getVG());
      nvgClosePath(getVG());
   }

   public static void roundedRectGradient(float x, float y, float width, float height, float radius, int color1, int color2, float angleDegrees) {
      applyColor(color1, NVG_COLOR_1);
      applyColor(color2, NVG_COLOR_2);

      float angleRadians = (float)Math.toRadians(angleDegrees);
      float dx = (float)Math.cos(angleRadians);
      float dy = (float)Math.sin(angleRadians);

      nvgLinearGradient(
         getVG(),
         x + width * 0.5f - dx * width * 0.5f,
         y + height * 0.5f - dy * height * 0.5f,
         x + width * 0.5f + dx * width * 0.5f,
         y + height * 0.5f + dy * height * 0.5f,
         NVG_COLOR_1,
         NVG_COLOR_2,
         NVG_PAINT
      );

      nvgBeginPath(getVG());
      nvgFillPaint(getVG(), NVG_PAINT);
      nvgRoundedRect(getVG(), x, y, width, height, radius);
      nvgFill(getVG());
      nvgClosePath(getVG());
   }

   public static void roundedRectVarying(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int color) {
      applyColor(color, NVG_COLOR_1);

      nvgBeginPath(getVG());
      nvgFillColor(getVG(), NVG_COLOR_1);
      nvgRoundedRectVarying(getVG(), x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft);
      nvgFill(getVG());
      nvgClosePath(getVG());
   }

   public static void applyColor(int color, NVGColor nvgColor) {
      nvgRGBAf((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f,
         (color & 255) / 255f, (color >>> 24) / 255f, nvgColor);
   }

   public static Minecraft getMinecraft() {
      return mc;
   }

   public static long getContext() {
      return getVG();
   }
}
