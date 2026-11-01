package com.dioxideliteng.krs;

import com.dioxideliteng.krs.events.EventListener;
import com.dioxideliteng.krs.events.features.MotionEvent;
import com.dioxideliteng.krs.events.features.ReceivedPacketEvent;
import com.dioxideliteng.krs.events.features.SendPacketEvent;
import com.dioxideliteng.krs.events.features.TickEvent;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.combat.KillAura;
import com.dioxideliteng.krs.hacks.features.exploit.Blink;
import com.dioxideliteng.krs.hacks.features.exploit.TransactionConfirmBlinker;
import com.dioxideliteng.krs.hacks.features.level.Breaker;
import com.dioxideliteng.krs.hacks.features.movement.Stasis;
import com.dioxideliteng.krs.hacks.features.player.ChestStealer;
import com.dioxideliteng.krs.hacks.features.player.Freecam;
import com.dioxideliteng.krs.hacks.features.player.InvManager;
import com.dioxideliteng.krs.hacks.features.player.Scaffold;
import com.dioxideliteng.krs.utils.entity.PlayerUtil;
import com.dioxideliteng.krs.utils.math.BehaviorUtils;
import com.dioxideliteng.krs.utils.math.Tuple;
import com.dioxideliteng.krs.utils.packet.BlinkUtil;
import com.dioxideliteng.krs.utils.render.RenderUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;

import java.util.List;
import java.util.Queue;

/**
 * The always-on KRS listener: port of the behavior KRS's {@code ModuleManager} ran for every module
 * (rotation application, blink packet capture, disabling risky modules on world change).
 * It is registered first, so it runs before any module's listener, exactly like in KRS.
 */
final class KrsCore implements EventListener {
    static final KrsCore INSTANCE = new KrsCore();

    private KrsCore() {
    }

    @Override
    public void onWorld(WorldEvent event) {
        List<Module> snapshot;
        synchronized (ModuleManager.modules) {
            snapshot = List.copyOf(ModuleManager.modules);
        }
        for (Module module : snapshot) {
            if (module.tempEnabled && (module instanceof InvManager || module instanceof ChestStealer
                || module instanceof Scaffold || module instanceof KillAura || module instanceof Stasis)) {
                module.toggle();
                if (Client.notificationManager != null)
                    Client.notificationManager.addNotification("World Change", "Automatically disabled " + module.displayName());
            }
        }

        if (!ModuleManager.getModuleState(Breaker.class) && Breaker.resetAutoWhitelist(true) && Client.notificationManager != null)
            Client.notificationManager.addNotification("Auto Whitelist", "Reset bed whitelist");

        BehaviorUtils.noKillAura = false;
        PlayerUtil.INSTANCE.fullResetSpoofState();
    }

    @Override
    public void onSendPacket(SendPacketEvent event) {
        if (mc.player == null || mc.level == null) return;

        Packet<?> packet = event.packet;

        if (ModuleManager.gettingTransactions && packet instanceof ServerboundPongPacket pong) {
            ModuleManager.transactionCounter += 1;
            com.dioxideliteng.krs.utils.ChatUtil.printChat(pong.getId() + " (x" + ModuleManager.transactionCounter + ")");
            if (ModuleManager.transactionCounter >= 10) {
                com.dioxideliteng.krs.utils.ChatUtil.printChat("Logged 10x transactions!");
                ModuleManager.transactionCounter = 0;
                ModuleManager.gettingTransactions = false;
            }
        }

        if (BlinkUtil.INSTANCE.getBlinking() && Blink.shouldBlinkOutgoing() && !BlinkUtil.INSTANCE.getLimiter()
            && !ModuleManager.getModuleState(Freecam.class)) {
            if (KillAura.isSendingSpreadTeleportPacket())
                return;

            if (packet instanceof ServerboundMovePlayerPacket && KillAura.shouldSuppressMovementDuringSpreadTeleport()) {
                event.cancel();
                return;
            }

            if (packet instanceof ServerboundMovePlayerPacket
                || ModuleManager.getModuleState(KillAura.class) && KillAura.closestEntity != null
                && KillAura.shouldCancelUseItemOnWhileBlinking() && packet instanceof ServerboundUseItemOnPacket)
                event.cancel();

            if (packet instanceof ServerboundMovePlayerPacket || packet instanceof ServerboundPlayerCommandPacket
                || packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundPlayerActionPacket
                || packet instanceof ServerboundUseItemPacket
                || ModuleManager.getModuleState(TransactionConfirmBlinker.class) && packet instanceof ServerboundPongPacket) {
                event.cancel();
                BlinkUtil.INSTANCE.addPacket(packet);
            }
        }
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
        if (mc.player == null || mc.level == null) return;

        if (BlinkUtil.INSTANCE.getBlinking() && Blink.shouldQueueIncoming(event.packet)
            && !ModuleManager.getModuleState(Freecam.class)) {
            event.cancel();
            BlinkUtil.INSTANCE.addIncomingPacket(event.packet);
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.level == null) return;

        Queue<Tuple<Integer, Integer>> pending = RenderUtil.getPending();
        if (pending.isEmpty()) return;

        int count = Math.min(4, pending.size());
        for (int i = 0; i < count; i++) {
            Tuple<Integer, Integer> pos = pending.poll();
            if (pos == null) break;

            int cx = pos.getFirst();
            int cz = pos.getSecond();
            mc.level.setSectionRangeDirty(cx >> 4, -64 >> 4, cz >> 4, (cx + 15) >> 4, mc.level.getHeight() >> 4, (cz + 15) >> 4);
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || Client.rotationManager == null) return;
        Client.rotationManager.update();
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.player == null || Client.rotationManager == null) return;

        if (Client.rotationManager.isRotating()) {
            event.yaw = Client.rotationManager.getRotationYaw();
            event.pitch = Client.rotationManager.getRotationPitch();
            ModuleManager.rotTick++;
        } else if (ModuleManager.rotTick != 0) {
            ModuleManager.rotTick = 0;
        }
    }
}
