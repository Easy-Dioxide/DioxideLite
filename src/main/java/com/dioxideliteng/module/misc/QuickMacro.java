package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventKey;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.setting.TextSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.QuickMacro).
 * Krs parity: on a key press of the configured macro key the module sends the
 * configured text — as a command when it starts with "/", otherwise as chat.
 * Notable adaptations vs the original:
 *  - Krs KeyBindValue replaced with a NumberSetting over the GLFW key range
 *    (default GLFW_KEY_X = 88).
 *  - Krs TextValue replaced with the ported TextSetting.
 *  - Krs KeyboardEvent replaced with Events.KEY (EventKey), dispatched from
 *    MixinKeyboardHandler at keyPress HEAD for every key.
 */
public class QuickMacro extends Feature {
    private final NumberSetting macroKey = new NumberSetting("Macro Key", this, 88.0, 0.0, 348.0, 1.0);
    private final TextSetting messageOrCommand = new TextSetting("Message / Command", this, "");

    public QuickMacro() {
        super("Quick Macro", Category.MISC);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.KEY)
            return;
        if (mc.player == null)
            return;

        EventKey keyEvent = (EventKey) event;
        if (keyEvent.getAction() == GLFW.GLFW_PRESS && keyEvent.getKey() == (int) this.macroKey.getValue()) {
            String message = this.messageOrCommand.getValue();
            boolean commandMode = message.startsWith("/");
            if (commandMode) {
                mc.player.connection.sendCommand(message.substring(1));
            } else {
                mc.player.connection.sendChat(message);
            }
        }
    }
}
