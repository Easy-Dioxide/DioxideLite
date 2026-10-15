package com.dioxideliteng.module.movement.speed.features;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.speed.SpeedEvent;
import com.dioxideliteng.util.krs.MovementUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

public class FlagBoostSpeed implements SpeedEvent {

    private final Speed2 speed2;

    public FlagBoostSpeed(Speed2 speed2) {
        this.speed2 = speed2;
    }

    public static int tick1;

    @Override
    public String getName() {
        return "Flag Boost";
    }

    @Override
    public void onUpdate(EventUpdate event) {
        if (tick1 >= 7 && tick1 != 9 && tick1 != 10 && tick1 < 12) {
            MovementUtil.stopXZ();
            tick1++;
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;

        if (tick1 >= 12) {
            tick1 = 0;
            return;
        }

        if (tick1 < 3) {
            tick1++;
        }

        switch (tick1) {
            case 3:
                if (mc.player.onGround()) {
                    event.y -= 1;
                } else {
                    event.y -= 3;
                }
                break;

            case 4, 6:
                tick1++;
                break;

            case 5:
                event.cancel();
                MovementUtil.strafe((float) this.speed2.vanillaSpeed.getValue());
                tick1++;
                break;

            case 9, 10:
                MovementUtil.strafe((float) this.speed2.vanillaSpeed.getValue());
                tick1++;
                break;
        }
    }

    @Override
    public void onTick(EventTick event) {
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
        if (mc.player == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundPlayerPositionPacket && tick1 == 3) {
            tick1 = 4;
        }
    }
}
