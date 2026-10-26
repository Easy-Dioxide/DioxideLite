package com.dioxideliteng.krs.hacks.features.movement;

import com.dioxideliteng.krs.events.features.ReceivedPacketEvent;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffects;
import com.dioxideliteng.krs.utils.input.Keys;

public class AntiLevitation extends Module {

    public AntiLevitation() {
        super("Anti Levitation", ModuleCategory.Movement, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
        var player = mc.player;
        if (player == null || !(event.packet instanceof ClientboundUpdateMobEffectPacket packet))
            return;

        if (packet.getEntityId() == player.getId() && packet.getEffect() == MobEffects.LEVITATION)
            event.cancel();
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player != null && mc.player.hasEffect(MobEffects.LEVITATION))
            mc.player.removeEffect(MobEffects.LEVITATION);
    }
}
