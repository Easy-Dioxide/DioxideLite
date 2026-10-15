package com.dioxideliteng.util.krs;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Ported from Krs (com.instrumentalist.krs.utils.ChatUtil). */
public final class ChatUtil {
    public static final Minecraft mc = Minecraft.getInstance();

    private ChatUtil() {
    }

    public static void showLog(String string) {
        LogUtils.getLogger().info(string);
    }

    public static void printChat(String string) {
        printModifiedChat(Component.literal("> " + string));
    }

    public static void printModifiedChat(MutableComponent mutableText) {
        printModifiedChat((Component) mutableText);
    }

    public static void printModifiedChat(Component component) {
        if (component == null)
            return;

        mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(component));
    }
}
