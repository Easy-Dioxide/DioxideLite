package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.StringSetting;
import net.minecraft.network.chat.Component;

/**
 * 移植自 OpenOpal StreamerModeModule。
 * 直播模式：隐藏服务器 ID、替换玩家用户名，避免直播时泄露隐私。
 */
public final class StreamerMode extends Module {

    public static final StreamerMode INSTANCE = new StreamerMode();

    public final BooleanSetting hideServerId = add(new BooleanSetting("Hide Server ID", true));
    public final BooleanSetting hideUsername = add(new BooleanSetting("Hide Username", true));
    public final StringSetting customUsername = add(new StringSetting("Custom Username", "You")
            .visibleWhen(hideUsername::get));

    private StreamerMode() {
        super("Streamer Mode", Category.RENDER);
    }

    /** 由 ChatComponentMixin 调用：对聊天内容进行隐私过滤。 */
    public Component filter(Component message) {
        if (!isEnabled() || message == null) return message;
        String text = message.getString();
        if (text == null || text.isEmpty()) return message;

        String result = text;
        if (hideServerId.get() && result.contains("Sending you to ")) {
            result = result.replaceAll("Sending you to [^!]+!", "Sending you to §k[HIDDEN]§r!");
        }
        if (hideUsername.get() && mc.player != null) {
            String username = mc.player.getGameProfile().name();
            String custom = customUsername.get() == null ? "You" : customUsername.get().trim();
            if (username != null && !username.isEmpty() && !custom.isEmpty()) {
                result = result.replace(username, custom);
            }
        }

        if (result.equals(text)) return message;
        return Component.literal(result);
    }
}
