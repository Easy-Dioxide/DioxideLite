package com.dioxideliteng.krs.utils;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

/**
 * Slim replacement for KRS's input blocker. KRS used it to keep game input away from its ImGui debug window and
 * NanoVG click GUI; DioxideLiteNG's GUIs are ordinary screens, so only the movement-key helpers are needed.
 */
public final class GuiInputBlocker implements IMinecraft {
    private GuiInputBlocker() {
    }

    public static boolean shouldAllowWorldHudOverlays() {
        return false;
    }

    public static boolean shouldBlockMinecraftMouse() {
        return false;
    }

    public static boolean shouldBlockMinecraftKeyboard() {
        return false;
    }

    public static boolean shouldBlockGameMovement() {
        return false;
    }

    public static boolean shouldBlockMovementKey(int keyCode) {
        return false;
    }

    public static int sanitizeMouseCoordinate(int coordinate) {
        return coordinate;
    }

    public static void releaseMovementKeys() {
        Options options = mc.options;
        if (options == null) return;

        release(options.keyUp);
        release(options.keyDown);
        release(options.keyLeft);
        release(options.keyRight);
        release(options.keyJump);
        release(options.keyShift);
        release(options.keySprint);
    }

    private static void release(KeyMapping keyMapping) {
        if (keyMapping != null) keyMapping.setDown(false);
    }
}
