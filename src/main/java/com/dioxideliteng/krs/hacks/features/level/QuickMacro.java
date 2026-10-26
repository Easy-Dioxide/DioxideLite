package com.dioxideliteng.krs.hacks.features.level;



import com.dioxideliteng.krs.events.features.KeyboardEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.KeyBindValue;
import com.dioxideliteng.krs.utils.value.TextValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class QuickMacro extends Module {

    public QuickMacro() {
        super("Quick Macro", ModuleCategory.Level, Keys.KEY_UNKNOWN, false, true);
    }

    @Setting
    private final KeyBindValue macroKey = new KeyBindValue("Macro Key", Keys.KEY_X);

    @Setting
    private final TextValue messageOrCommand = new TextValue("Message / Command", "");

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onKey(KeyboardEvent event) {
        if (mc.player == null) return;

        if (event.action == Keys.PRESS && event.key == macroKey.get()) {
            boolean commandMode = messageOrCommand.get().startsWith("/");
            if (commandMode) {
                mc.player.connection.sendCommand(messageOrCommand.get().substring(1));
            } else {
                mc.player.connection.sendChat(messageOrCommand.get());
            }
        }
    }
}
