package com.dioxideliteng.krs.utils;

import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;

/**
 * KRS notifications are shown through DioxideLiteNG's Dynamic Island so ported modules look identical to native ones.
 */
public final class NotificationManager implements IMinecraft {
    public void addNotification(String title, String message) {
        if (title == null && message == null) return;
        String heading = title == null ? "" : title;
        String detail = message == null ? "" : message;
        // The island lives on the render thread's state; hop onto the client thread like KRS's chat helper does.
        mc.execute(() -> DynamicIslandManager.notifyInfo(heading, detail));
    }

    public void clear() {
        // Island notices expire on their own; nothing to drop.
    }
}
