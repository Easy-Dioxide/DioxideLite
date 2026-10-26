package com.dioxideliteng.krs.hacks.features.movement;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.features.ReceivedPacketEvent;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.entity.EntityExtension;
import com.dioxideliteng.krs.utils.move.MovementUtil;
import com.dioxideliteng.krs.utils.packet.BlinkUtil;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.value.IntValue;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import com.dioxideliteng.krs.utils.input.Keys;

public class AntiVoid extends Module {
    @Setting
    private final BooleanValue stopXZ = new BooleanValue("StopXZ", true);

    @Setting
    private final IntValue distance = new IntValue("Distance", 6, 0, 10, "m");

    private int canTick = 0;
    private Integer unSafeY = null;

    public AntiVoid() {
        super("Anti Void", ModuleCategory.Movement, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        BlinkUtil.INSTANCE.sync(true, false);
        BlinkUtil.INSTANCE.stopBlink();
        canTick = 0;
        unSafeY = null;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onWorld(WorldEvent event) {
        canTick = 0;
        if (unSafeY != null) {
            BlinkUtil.INSTANCE.sync(true, false);
            BlinkUtil.INSTANCE.stopBlink();
            unSafeY = null;
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        var player = mc.player;
        if (player == null || canTick > 0 || !EntityExtension.isFallingToVoid(player) || player.tickCount <= 50 || player.getAbilities().flying || player.isShiftKeyDown() || player.isSpectator() || player.isInWater()) {
            if (unSafeY != null) {
                BlinkUtil.INSTANCE.sync(true, false);
                BlinkUtil.INSTANCE.stopBlink();
                unSafeY = null;
            }
            if (canTick > 0)
                canTick--;
            return;
        }

        if (unSafeY == null)
            unSafeY = player.blockPosition().getY() - distance.get();

        BlinkUtil.INSTANCE.doBlink();
        if (player.blockPosition().getY() <= unSafeY) {
            BlinkUtil.INSTANCE.sync(false, true);
            if (stopXZ.get())
                MovementUtil.stopMoving();
        }
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
        var player = mc.player;
        if (player == null || player.tickCount <= 50) return;

        if (event.packet instanceof ClientboundPlayerPositionPacket && BlinkUtil.INSTANCE.getBlinking() && unSafeY != null) {
            canTick = 80;
            KrsClient.notificationManager.addNotification("Temporary disabled", "AntiVoid flagged (wait for " + canTick + "s to re-activate)");
            BlinkUtil.INSTANCE.sync(true);
            BlinkUtil.INSTANCE.stopBlink();
            if (stopXZ.get())
                MovementUtil.stopMoving();
            unSafeY = null;
        }
    }
}
