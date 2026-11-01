package com.dioxideliteng.krs;

import com.dioxideliteng.ClientBranding;
import com.dioxideliteng.krs.events.EventManager;
import com.dioxideliteng.krs.utils.IMinecraft;
import com.dioxideliteng.krs.utils.NotificationManager;
import com.dioxideliteng.krs.utils.rotation.RotationManager;

/** Static hub with the members ported KRS code reads from KRS's original {@code Client} class. */
public final class Client implements IMinecraft {
    public static String clientVersion = ClientBranding.DISPLAY_VERSION;

    public static volatile EventManager eventManager = null;
    public static RotationManager rotationManager = null;
    public static NotificationManager notificationManager = null;
    public static volatile boolean loaded = false;
    public static String configLocation = "Krs-cfg";

    private Client() {
    }

    public static void copyToClipboard(String string) {
        if (string == null) return;
        mc.keyboardHandler.setClipboard(string);
    }
}
