package com.dioxidelite.module.modules.misc;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/misc/MiddleClickFriend.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.DioxideLite;
import com.dioxidelite.config.ConfigManager;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.MouseButtonEvent;
import com.dioxidelite.manager.FriendManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.notification.NotificationManager;
import com.dioxidelite.notification.NotificationType;
import com.dioxidelite.util.player.ChatUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

/** Toggles the player under the crosshair in the friend list with middle click. */
public final class MiddleClickFriend extends Module {

    public static final MiddleClickFriend INSTANCE = new MiddleClickFriend();

    private MiddleClickFriend() {
        super("MiddleClickFriend", Category.MISC);
    }

    @Listen
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action() != GLFW.GLFW_PRESS
                || event.button() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                || mc.screen != null
                || noPlayer()
                || !(mc.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Player player)
                || player == mc.player) {
            return;
        }

        String name = player.getGameProfile().name();
        if (name == null || name.isBlank()) {
            return;
        }

        boolean added;
        if (FriendManager.INSTANCE.isFriend(name)) {
            FriendManager.INSTANCE.remove(name);
            added = false;
        } else {
            FriendManager.INSTANCE.add(name);
            added = true;
        }

        String message = (added ? "Added " : "Removed ") + name
                + (added ? " as a friend." : " from friends.");
        ChatUtils.addChatMessage(message);
        NotificationManager.INSTANCE.post(
                added ? NotificationType.SUCCESS : NotificationType.INFO,
                "MiddleClickFriend",
                message);
        try {
            ConfigManager.INSTANCE.saveChecked();
        } catch (IOException | RuntimeException error) {
            DioxideLite.LOGGER.error("Failed to save middle-click friend change", error);
        }
    }
}
