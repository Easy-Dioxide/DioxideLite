package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.TextSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.AutoBypass).
 * Krs parity: server-specific auto-requeue / auto-login driven by system chat
 * packets. Hypixel mode leaves limbo/AFK with /lobby and clicks the "play again"
 * RunCommand click event; Cubecraft runs "playagain now"; Purple Prison runs
 * "sell" on a full-inventory alert; Auth Me logs in / registers with the password.
 * Pending commands are flushed one per update tick, click commands unattended
 * against the current screen.
 * Notable adaptations vs the original:
 *  - Krs static ListValue/TextValue replaced with instance settings; the pending
 *    command state stays static like in Krs (mixin/hook parity not required, kept
 *    for a 1:1 module surface).
 *  - Client.notificationManager.addNotification replaced with
 *    DynamicIslandManager.notifyInfo.
 *  - Krs ReceivedPacketEvent replaced with Events dispatching of
 *    EventPacketReceive (event instanceof check, matching the Krs event mapping).
 */
public class AutoBypass2 extends Feature {
    private final ModeSetting mode = new ModeSetting("Mode", this, "Hypixel",
            new String[]{"Hypixel", "Cubecraft", "Purple Prison", "Auth Me"});
    private final TextSetting password = new TextSetting("Password", this, "aaaaaaaa");

    private String neededCommand = null;
    private String neededClickCommand = null;

    public AutoBypass2() {
        super("AutoBypass2", Category.MISC);
        this.password.setVisible(() -> this.mode.is("Auth Me"));
    }

    @Override
    public void onDisable() {
        this.neededCommand = null;
        this.neededClickCommand = null;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.UPDATE) {
            this.setSuffix(this.mode.getValue());
            if (mc.player == null) return;

            if (this.neededClickCommand != null) {
                mc.player.connection.sendUnattendedCommand(Commands.trimOptionalPrefix(this.neededClickCommand), mc.gui.screen());
                this.neededClickCommand = null;
            } else if (this.neededCommand != null) {
                mc.player.connection.sendCommand(this.neededCommand);
                this.neededCommand = null;
            }
        } else if (event instanceof EventPacketReceive packetEvent) {
            if (this.neededCommand != null || this.neededClickCommand != null) return;

            Packet<?> packet = packetEvent.getPacket();

            switch (this.mode.getValue().toLowerCase(Locale.ROOT)) {
                case "hypixel":
                    if (packet instanceof ClientboundSystemChatPacket chatPacket && !chatPacket.overlay()) {
                        if (chatPacket.content().getString().contains("You were spawned in Limbo.") || chatPacket.content().getString().contains("You are AFK. Move around to return from AFK.")) {
                            this.neededCommand = "lobby";
                            DynamicIslandManager.notifyInfo("Auto Chat", "Trying to bypass limbo...");
                        } else if (chatPacket.content().getString().contains("You won! Want to play again?") || chatPacket.content().getString().contains("You died! Want to play again?")) {
                            ClickEvent.RunCommand playAgain = findClickHereCommand(chatPacket.content());
                            if (playAgain != null) {
                                this.neededClickCommand = playAgain.command();
                                DynamicIslandManager.notifyInfo("Auto Join", "Joining to next game...");
                            } else {
                                DynamicIslandManager.notifyInfo("Auto Join", "Failed to track command");
                            }
                        }
                    }
                    break;

                case "cubecraft":
                    if (packet instanceof ClientboundSystemChatPacket chatPacket && !chatPacket.overlay() && chatPacket.content().getString().contains("Thank you for playing")) {
                        this.neededCommand = "playagain now";
                        DynamicIslandManager.notifyInfo("Auto Join", "Joining to next game...");
                    }
                    break;

                case "purple prison":
                    if (packet instanceof ClientboundSystemChatPacket chatPacket && !chatPacket.overlay() && chatPacket.content().getString().contains("ALERT! Your inventory is full (Use /sell)")) {
                        this.neededCommand = "sell";
                        DynamicIslandManager.notifyInfo("Auto Sell", "Sold every items");
                    }
                    break;

                case "auth me":
                    if (packet instanceof ClientboundSystemChatPacket chatPacket && !chatPacket.overlay()) {
                        String message = chatPacket.content().getString();
                        if (message.contains("login")) {
                            this.neededCommand = "login " + this.password.getValue();
                            DynamicIslandManager.notifyInfo("Auto Auth", "Logging in...");
                        } else if (message.contains("register")) {
                            this.neededCommand = "register " + this.password.getValue() + " " + this.password.getValue();
                            DynamicIslandManager.notifyInfo("Auto Auth", "Registering...");
                        }
                    }
                    break;
            }
        }
    }

    private static ClickEvent.RunCommand findClickHereCommand(Component message) {
        ClickEvent.RunCommand firstCommand = null;

        for (Component part : message.toFlatList()) {
            if (!(part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand command))
                continue;

            if (part.getString().toLowerCase(Locale.ROOT).contains("click here"))
                return command;

            if (firstCommand == null)
                firstCommand = command;
        }

        return firstCommand;
    }
}
