package com.dioxidelite.client.render.skia;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.BackendState;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceOrigin;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL30.GL_MAJOR_VERSION;
import static org.lwjgl.opengl.GL30.GL_MINOR_VERSION;
import static org.lwjgl.opengl.GL30.glGetIntegerv;

/**
 * Skija GPU backend: wraps an existing OpenGL framebuffer as a Skia render target so that
 * Skija draws straight into it, with no CPU raster and no texture upload.
 *
 * <p>v1.8 note: this class is now reachable from {@link SkiaRenderer} for the whole GUI layer,
 * not just from {@link SkiaBlurRenderer}. Two changes were needed for that:
 * <ul>
 *   <li>{@link #begin(int)} is always called with an explicit framebuffer id, because Minecraft
 *       does not necessarily render into framebuffer 0.</li>
 *   <li>an outstanding {@code SkiaGlState.push()} is now tracked, so it can never be left
 *       un-popped and strand the client's GL state. See {@link #resetCanvasState()}.</li>
 * </ul>
 *
 * <p>{@code SkiaGlState} keeps a single save slot, not a stack, so {@code push()} must never
 * be called twice without an intervening {@code pop()}. Each {@code SkiaGlBackend} instance
 * owns its own {@code SkiaGlState}, which is what makes the nested use safe:
 * SkiaRenderer's frame backend pushes first, SkiaBlurRenderer's framebuffer backend pushes
 * and pops inside that, and the frame backend pops last.
 */
public final class SkiaGlBackend {
    private static final BackendState[] RESET_STATES = {
            BackendState.GL_BLEND,
            BackendState.GL_VERTEX,
            BackendState.GL_PIXEL_STORE,
            BackendState.GL_TEXTURE_BINDING,
            BackendState.GL_MISC
    };

    private DirectContext context;
    private BackendRenderTarget renderTarget;
    private Surface surface;
    private Canvas canvas;
    private SkiaGlState state;
    private int width = -1;
    private int height = -1;
    private int framebufferId = -1;
    private boolean drawing = false;
    // [v1.8 ADDITION] true while a SkiaGlState.push() is outstanding and must be popped.
    private boolean statePushed = false;

    public Canvas begin() {
        return begin(0);
    }

    public Canvas begin(int targetFramebufferId) {
        if (drawing) return canvas;
        var window = Minecraft.getInstance().getWindow();
        int targetW = Math.max(1, window.getWidth());
        int targetH = Math.max(1, window.getHeight());
        ensureState();
        state.push();
        statePushed = true;
        try {
            ensureSurface(targetW, targetH, targetFramebufferId);
            if (surface == null || canvas == null) {
                state.pop();
                statePushed = false;
                return null;
            }

            glDisable(GL_CULL_FACE);
            glClearColor(0f, 0f, 0f, 0f);
            context.reset(RESET_STATES);

            canvas.restoreToCount(1);
            canvas.resetMatrix();
            canvas.save();
            canvas.scale((float) window.getGuiScale(), (float) window.getGuiScale());
            drawing = true;
            return canvas;
        } catch (RuntimeException e) {
            state.pop();
            statePushed = false;
            throw e;
        }
    }

    public void end() {
        if (!drawing || surface == null) return;
        try {
            canvas.restore();
            context.flushAndSubmit(surface);
        } finally {
            drawing = false;
            state.pop();
            statePushed = false;
        }
    }

    public boolean isDrawing() {
        return drawing;
    }

    public boolean hasSurface() {
        return surface != null;
    }

    public DirectContext getContext() {
        return context;
    }

    public void resetCanvasState() {
        drawing = false;
        // [v1.8 ADDITION] If a begin() never reached its matching end(), the saved GL state is
        // still sitting in the single save slot and the client would keep Skija's GL state.
        // Pop it here so the state can never be stranded. SkiaGlState is not a stack, so this
        // must happen before any later push().
        if (statePushed && state != null) {
            state.pop();
            statePushed = false;
        }
        if (canvas != null) {
            canvas.restoreToCount(1);
            canvas.resetMatrix();
        }
    }

    public void destroy() {
        resetCanvasState();
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (renderTarget != null) {
            renderTarget.close();
            renderTarget = null;
        }
        if (context != null) {
            context.close();
            context = null;
        }
        canvas = null;
        state = null;
        width = -1;
        height = -1;
        framebufferId = -1;
    }

    private void ensureSurface(int targetW, int targetH, int targetFramebufferId) {
        ensureContext();
        if (surface != null && targetW == width && targetH == height && targetFramebufferId == framebufferId) return;

        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (renderTarget != null) {
            renderTarget.close();
            renderTarget = null;
        }

        renderTarget = BackendRenderTarget.makeGL(targetW, targetH, 0, 8, targetFramebufferId, FramebufferFormat.GR_GL_RGBA8);
        surface = Surface.wrapBackendRenderTarget(
                context,
                renderTarget,
                SurfaceOrigin.BOTTOM_LEFT,
                ColorType.RGBA_8888,
                ColorSpace.getSRGB()
        );
        canvas = surface.getCanvas();
        width = targetW;
        height = targetH;
        framebufferId = targetFramebufferId;
    }

    private void ensureContext() {
        if (context != null) return;
        context = DirectContext.makeGL();
    }

    private void ensureState() {
        if (state != null) return;
        state = new SkiaGlState(readGlVersion());
    }

    private static int readGlVersion() {
        int[] major = new int[1];
        int[] minor = new int[1];
        glGetIntegerv(GL_MAJOR_VERSION, major);
        glGetIntegerv(GL_MINOR_VERSION, minor);
        return major[0] * 100 + minor[0] * 10;
    }
}
