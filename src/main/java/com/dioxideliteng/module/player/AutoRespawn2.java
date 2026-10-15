package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.PacketUtil;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

import java.util.Locale;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.AutoRespawn).
 *
 * Adaptations:
 *  - ListValue/IntValue/BooleanValue mapped to ModeSetting/NumberSetting/BooleanSetting;
 *  - Krs onUpdate -> Events.UPDATE; tag() -> setSuffix.
 */
public class AutoRespawn2 extends Feature {
    private final ModeSetting mode = new ModeSetting("Mode", this, "Packet", new String[]{"Packet", "Client", "Both"});
    private final NumberSetting delay = new NumberSetting("Delay", this, 0, 0, 20, 1);
    private final BooleanSetting closeDeathScreen = new BooleanSetting("Close Death Screen", this, true);

    private int deathTicks = 0;
    private boolean requested = false;

    public AutoRespawn2() {
        super("AutoRespawn2", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.UPDATE) return;
        setSuffix(mode.getValue());

        var player = mc.player;
        if (player == null || !player.isDeadOrDying()) {
            reset();
            return;
        }

        if (deathTicks < (int) delay.getValue()) {
            deathTicks++;
            return;
        }

        if (requested)
            return;

        switch (mode.getValue().toLowerCase(Locale.ROOT)) {
            case "client" -> player.respawn();
            case "both" -> {
                PacketUtil.sendPacket(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                player.respawn();
            }
            default -> PacketUtil.sendPacket(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        }
        if (closeDeathScreen.getValue() && mc.gui.screen() instanceof DeathScreen)
            mc.gui.setScreen(null);
        requested = true;
    }

    private void reset() {
        deathTicks = 0;
        requested = false;
    }
}
