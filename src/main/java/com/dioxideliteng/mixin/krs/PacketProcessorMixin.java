package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.EventManager;
import com.dioxideliteng.krs.events.features.ReceivedPacketEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public abstract class PacketProcessorMixin {
    @Unique private static final Logger KRS_LOGGER = LogUtils.getLogger();
    @Unique private static final int KRS_MAX_BUNDLE_DEPTH = 32;
    @Unique private static final int KRS_MAX_BUNDLE_PACKETS = 4096;
    @Unique private static final long KRS_BUNDLE_WARNING_INTERVAL_NANOS = 10_000_000_000L;
    @Unique private static long krs$lastBundleWarningNanos;

    @Shadow @Final private PacketListener listener;
    @Shadow @Final private Packet<?> packet;

    @Inject(
            method = "handle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"
            ),
            cancellable = true
    )
    private void callReceivedPacketEvent(CallbackInfo ci) {
        if (!(listener instanceof ClientboundPacketListener) || packet == null)
            return;

        EventManager eventManager = KrsClient.eventManager;
        if (eventManager == null || !eventManager.hasListeners(ReceivedPacketEvent.class))
            return;

        if (packet instanceof ClientboundBundlePacket bundlePacket) {
            ci.cancel();
            int[] remainingPacketBudget = {KRS_MAX_BUNDLE_PACKETS};
            for (Packet<?> subPacket : bundlePacket.subPackets()) {
                if (!handlePacket(subPacket, listener, eventManager, 1, remainingPacketBudget))
                    break;
            }
            return;
        }

        ReceivedPacketEvent event = new ReceivedPacketEvent(packet);
        eventManager.call(event);
        if (event.isCancelled())
            ci.cancel();
    }

    @Unique
    private static boolean handlePacket(Packet<?> packet, PacketListener listener, EventManager eventManager,
                                        int depth, int[] remainingPacketBudget) {
        if (packet == null)
            return true;

        if (remainingPacketBudget[0]-- <= 0) {
            krs$warnInvalidBundle("packet count");
            return false;
        }

        if (packet instanceof ClientboundBundlePacket bundlePacket) {
            if (depth >= KRS_MAX_BUNDLE_DEPTH) {
                krs$warnInvalidBundle("nesting depth");
                return true;
            }

            for (Packet<?> subPacket : bundlePacket.subPackets()) {
                if (!handlePacket(subPacket, listener, eventManager, depth + 1, remainingPacketBudget))
                    return false;
            }
            return true;
        }

        ReceivedPacketEvent event = new ReceivedPacketEvent(packet);
        eventManager.call(event);
        if (!event.isCancelled())
            invokePacketHandler(packet, listener);
        return true;
    }

    @Unique
    private static void krs$warnInvalidBundle(String limit) {
        long now = System.nanoTime();
        if (krs$lastBundleWarningNanos != 0L && now - krs$lastBundleWarningNanos < KRS_BUNDLE_WARNING_INTERVAL_NANOS)
            return;

        krs$lastBundleWarningNanos = now;
        KRS_LOGGER.warn("Discarded part of an oversized clientbound packet bundle ({})", limit);
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void invokePacketHandler(Packet<?> packet, PacketListener listener) {
        ((Packet) packet).handle(listener);
    }
}
