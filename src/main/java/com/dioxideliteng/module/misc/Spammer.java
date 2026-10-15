package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.setting.TextSetting;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.RandomUtil;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.Spammer).
 * Krs parity: every Delay milliseconds the configured message is sent — through
 * the party chat command ("pc ...") in Party Chat Mode, otherwise as a command
 * when the message starts with "/" and as plain chat otherwise. First Char is
 * prepended and a random 3-char suffix is appended when Random Suffix is on.
 * Notable adaptations vs the original:
 *  - The Krs ChatCommands branch (ModuleRenderable.executeCommand) is dropped:
 *    DioxideLite has neither a ChatCommands module nor the imguimc custom-window
 *    API; the "/" branch keeps its plain sendCommand behavior.
 *  - Krs ListValue/TextValue/IntValue/BooleanValue replaced with ModeSetting /
 *    TextSetting / NumberSetting / BooleanSetting.
 *  - Krs UpdateEvent replaced with Events.UPDATE; tag() replaced with setSuffix.
 */
public class Spammer extends Feature {
    private final ModeSetting mode = new ModeSetting("Mode", this, "Normal", new String[]{"Normal"});
    private final TextSetting message = new TextSetting("Message", this, "cocaine");
    private final NumberSetting delay = new NumberSetting("Delay", this, 1010.0, 0.0, 3000.0, 1.0);
    private final TextSetting firstChar = new TextSetting("First Char", this, "");
    private final BooleanSetting randomSuffix = new BooleanSetting("Random Suffix", this, true);
    private final BooleanSetting partyChatMode = new BooleanSetting("Party Chat Mode", this, false);

    private final MSTimer delayTimer = new MSTimer();

    public Spammer() {
        super("Spammer", Category.MISC);
        this.message.setVisible(() -> this.mode.is("Normal"));
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    private void doFunctions() {
        if (this.mode.is("Normal")) {
            sendChat(this.message.getValue());
        }
    }

    private void sendChat(String message) {
        if (message == null) return;

        var player = mc.player;
        if (player == null) return;
        var connection = player.connection;
        String prefix = this.firstChar.getValue();
        boolean randomize = this.randomSuffix.getValue();

        if (this.partyChatMode.getValue()) {
            if (randomize)
                connection.sendCommand("pc " + prefix + message + " " + RandomUtil.randomString(3));
            else connection.sendCommand("pc " + prefix + message);
        } else if (message.startsWith("/")) {
            String clearMessage = message.substring(1);
            if (randomize)
                connection.sendCommand(prefix + clearMessage + " " + RandomUtil.randomString(3));
            else connection.sendCommand(prefix + clearMessage);
        } else {
            if (randomize)
                connection.sendChat(prefix + message + " " + RandomUtil.randomString(3));
            else connection.sendChat(prefix + message);
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.UPDATE)
            return;
        if (mc.player == null) return;

        this.setSuffix(this.mode.getValue());

        if (this.delayTimer.hasTimePassed((long) this.delay.getValue())) {
            doFunctions();
            this.delayTimer.reset();
        }
    }
}
