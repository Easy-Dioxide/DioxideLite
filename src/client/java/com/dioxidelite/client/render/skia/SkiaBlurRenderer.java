package com.dioxidelite.client.render.skia;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorFilter;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL45.*;

public final class SkiaBlurRenderer {
    private static final SkiaBlurRenderer INSTANCE = new SkiaBlurRenderer();
    private static final float GPU_CAPTURE_SCALE = 0.55f;
    private static final long CAPTURE_INTERVAL_NS = 1_000_000L;

    private final Paint blurPaint = new Paint().setAntiAlias(true);
    private final Paint frostPaint = new Paint().setAntiAlias(true);
    private final Paint tintPaint = new Paint().setAntiAlias(true);
    private final SkiaGlBackend framebufferBackend = new SkiaGlBackend();
    private int captureTextureId = 0;
    private int captureFboId = 0;
    private int captureW = 0;
    private int captureH = 0;
    private int sourceW = 0;
    private int sourceH = 0;
    private Image captureImage;
    private DirectContext captureContext;
    private long lastCaptureNs = 0L;
    private int lastSourceFramebuffer = -1;
    private boolean nativeLoaded = false;

    private SkiaBlurRenderer() {}

    public static SkiaBlurRenderer getInstance() {
        return INSTANCE;
    }

    public static int currentDrawFramebufferId() {
        int[] framebuffer = new int[1];
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, framebuffer);
        return framebuffer[0];
    }

    public boolean render(Minecraft client, float x, float y, float width, float height, float radius, int tintColor, float strength) {
        if (client == null || client.getWindow() == null || client.getMainRenderTarget() == null) return false;
        int framebufferId = mainFramebufferId(client);
        Canvas canvas = framebufferBackend.begin(framebufferId);
        DirectContext context = framebufferBackend.getContext();
        if (canvas == null || context == null) {
            framebufferBackend.end();
            return false;
        }
        try {
            return render(canvas, context, client, framebufferId, x, y, width, height, radius, tintColor, strength);
        } finally {
            framebufferBackend.end();
        }
    }

    public boolean render(Canvas canvas, DirectContext context, Minecraft client, int sourceFramebufferId,
                          float x, float y, float width, float height, float radius, int tintColor, float strength) {
        if (canvas == null || context == null || client == null || client.getWindow() == null) return false;
        ensureNativeLoaded();

        float scale = (float) client.getWindow().getGuiScale();
        float blurSigma = blurSigma(strength);
        if (!ensureFrameCapture(client, context, sourceFramebufferId)) return false;

        Image image = captureImage;
        if (image == null) return false;
        ImageFilter linearize = null;
        ImageFilter blur = null;
        ImageFilter encode = null;
        try {
            canvas.save();
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, radius), true);
            linearize = ImageFilter.makeColorFilter(ColorFilter.getSRGBToLinearGamma(), null);
            blur = ImageFilter.makeBlur(blurSigma, blurSigma, FilterTileMode.CLAMP, linearize, (Rect) null);
            encode = ImageFilter.makeColorFilter(ColorFilter.getLinearToSRGBGamma(), blur);
            blurPaint.setImageFilter(encode);

            float srcX = x * GPU_CAPTURE_SCALE * scale;
            float srcY = (sourceH - (y + height) * scale) * GPU_CAPTURE_SCALE;
            float srcW = width * GPU_CAPTURE_SCALE * scale;
            float srcH = height * GPU_CAPTURE_SCALE * scale;
            srcX = Math.max(0f, Math.min(captureW - 1f, srcX));
            srcY = Math.max(0f, Math.min(captureH - 1f, srcY));
            srcW = Math.max(1f, Math.min(captureW - srcX, srcW));
            srcH = Math.max(1f, Math.min(captureH - srcY, srcH));

            canvas.drawImageRect(image,
                    Rect.makeXYWH(srcX, srcY, srcW, srcH),
                    Rect.makeXYWH(x, y, width, height),
                    SamplingMode.LINEAR, blurPaint, true);
            blurPaint.setImageFilter(null);

            frostPaint.setColor(0x10000000);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), frostPaint);
            tintPaint.setColor(tintColor);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), tintPaint);
            canvas.restore();
            return true;
        } finally {
            blurPaint.setImageFilter(null);
            if (encode != null) encode.close();
            if (blur != null) blur.close();
            if (linearize != null) linearize.close();
        }
    }

    /**
     * One persistent GPU copy is shared by every glass surface in the current
     * frame. The old implementation allocated an FBO/texture for every panel,
     * which caused a large amount of driver work and synchronization.
     */
    private boolean ensureFrameCapture(Minecraft client, DirectContext context, int sourceFramebufferId) {
        int framebufferW = client.getWindow().getWidth();
        int framebufferH = client.getWindow().getHeight();
        if (framebufferW <= 0 || framebufferH <= 0) return false;

        long now = System.nanoTime();
        boolean sizeChanged = framebufferW != sourceW || framebufferH != sourceH ||
                captureContext != context || captureImage == null || captureTextureId == 0;
        boolean frameExpired = now - lastCaptureNs >= CAPTURE_INTERVAL_NS || sourceFramebufferId != lastSourceFramebuffer;
        if (!sizeChanged && !frameExpired) return true;

        ensureCaptureResources(context, framebufferW, framebufferH);

        int[] oldReadFramebuffer = new int[1];
        int[] oldDrawFramebuffer = new int[1];
        int[] oldReadBuffer = new int[1];
        int[] oldDrawBuffer = new int[1];
        int[] oldViewport = new int[4];
        glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, oldReadFramebuffer);
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, oldDrawFramebuffer);
        glGetIntegerv(GL_READ_BUFFER, oldReadBuffer);
        glGetIntegerv(GL_DRAW_BUFFER, oldDrawBuffer);
        glGetIntegerv(GL_VIEWPORT, oldViewport);
        try {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, sourceFramebufferId);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, captureFboId);
            glReadBuffer(sourceFramebufferId == 0 ? GL_BACK : GL_COLOR_ATTACHMENT0);
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            glBlitFramebuffer(
                    0, 0, framebufferW, framebufferH,
                    0, 0, captureW, captureH,
                    GL_COLOR_BUFFER_BIT, GL_LINEAR
            );
            lastCaptureNs = now;
            lastSourceFramebuffer = sourceFramebufferId;
            return true;
        } finally {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, oldReadFramebuffer[0]);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDrawFramebuffer[0]);
            restoreReadBuffer(oldReadFramebuffer[0], oldReadBuffer[0]);
            restoreDrawBuffer(oldDrawFramebuffer[0], oldDrawBuffer[0]);
            glViewport(oldViewport[0], oldViewport[1], oldViewport[2], oldViewport[3]);
        }
    }

    private static void restoreReadBuffer(int framebufferId, int buffer) {
        glReadBuffer(framebufferId == 0 ? GL_BACK : buffer);
    }

    private static void restoreDrawBuffer(int framebufferId, int buffer) {
        glDrawBuffer(framebufferId == 0 ? GL_BACK : buffer);
    }

    private void ensureCaptureResources(DirectContext context, int framebufferW, int framebufferH) {
        int desiredW = Math.max(1, Math.round(framebufferW * GPU_CAPTURE_SCALE));
        int desiredH = Math.max(1, Math.round(framebufferH * GPU_CAPTURE_SCALE));
        if (captureTextureId != 0 && desiredW == captureW && desiredH == captureH && captureImage != null && captureContext == context) {
            sourceW = framebufferW;
            sourceH = framebufferH;
            return;
        }

        destroyCaptureResources();
        captureW = desiredW;
        captureH = desiredH;
        sourceW = framebufferW;
        sourceH = framebufferH;
        captureContext = context;

        captureTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, captureTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, captureW, captureH, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);

        captureFboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, captureFboId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, captureTextureId, 0);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            destroyCaptureResources();
            return;
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        captureImage = Image.adoptGLTextureFrom(context, captureTextureId, GL_TEXTURE_2D, captureW, captureH,
                GL_RGBA8, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGBA_8888);
        // Skia now owns the GL texture lifetime. Do not delete captureTextureId
        // while captureImage is alive.
        lastCaptureNs = 0L;
        lastSourceFramebuffer = -1;
    }

    private void destroyCaptureResources() {
        if (captureImage != null) {
            captureImage.close();
            captureImage = null;
        }
        // If Image adoption succeeded, closing the image releases the texture.
        captureTextureId = 0;
        if (captureFboId != 0) {
            glDeleteFramebuffers(captureFboId);
            captureFboId = 0;
        }
        captureW = captureH = sourceW = sourceH = 0;
        captureContext = null;
        lastCaptureNs = 0L;
        lastSourceFramebuffer = -1;
    }

    private int mainFramebufferId(Minecraft client) {
        if (client.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), client.getMainRenderTarget().getDepthTexture());
        }
        return currentDrawFramebufferId();
    }

    private float blurSigma(float strength) {
        float clamped = Math.max(0f, Math.min(2f, strength));
        return Math.max(0.1f, 3f + clamped * 9f);
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

}
