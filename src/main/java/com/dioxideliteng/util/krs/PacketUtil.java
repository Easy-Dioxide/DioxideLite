package com.dioxideliteng.util.krs;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import org.slf4j.Logger;

public class PacketUtil {
    public static final Minecraft mc = Minecraft.getInstance();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<Set<Packet<?>>> SILENT_PACKETS = new ThreadLocal<>();

    public static void sendPacket(Packet<?> packet) {
        ClientPacketListener networkHandler = mc.getConnection();
        if (networkHandler == null || packet == null) return;

        networkHandler.send(normalizeRotationPacket(packet));
    }

    public static void sendPacketAsSilent(Packet<?> packet) {
        ClientPacketListener networkHandler = mc.getConnection();
        if (networkHandler == null || packet == null) return;

        Packet<?> normalizedPacket = normalizeRotationPacket(packet);
        Set<Packet<?>> packetsForThread = SILENT_PACKETS.get();
        if (packetsForThread == null) {
            packetsForThread = Collections.newSetFromMap(new IdentityHashMap<>());
            SILENT_PACKETS.set(packetsForThread);
        }
        packetsForThread.add(normalizedPacket);
        try {
            networkHandler.send(normalizedPacket);
        } finally {
            // The send hook normally consumes this marker synchronously. Remove it
            // here as well so a failed or short-circuited send cannot leak markers.
            packetsForThread.remove(normalizedPacket);
            if (packetsForThread.isEmpty())
                SILENT_PACKETS.remove();
        }
    }

    public static boolean consumeSilentPacket(Packet<?> packet) {
        if (packet == null)
            return false;

        Set<Packet<?>> packetsForThread = SILENT_PACKETS.get();
        if (packetsForThread == null)
            return false;

        boolean removed = packetsForThread.remove(packet);
        if (packetsForThread.isEmpty())
            SILENT_PACKETS.remove();
        return removed;
    }

    /**
     * 调用全局 RotationManager.INSTANCE 归一化 outgoing packet 的旋转字段，
     * 让服务器看到的旋转与 RotationManager 维护的服务器旋转状态一致。
     * 这是 Krs 反作弊绕过的核心：所有携带旋转的包都会被自动归一化。
     */
    private static Packet<?> normalizeRotationPacket(Packet<?> packet) {
        return RotationManager.INSTANCE.normalizeOutgoingPacket(packet);
    }

    @SuppressWarnings("unchecked")
    public static void handlePacket(Packet<?> packet) {
        ClientGamePacketListener networkHandler = mc.getConnection();
        if (networkHandler == null || packet == null) return;

        try {
            Packet<ClientGamePacketListener> typedPacket = (Packet<ClientGamePacketListener>) packet;
            typedPacket.handle(networkHandler);
        } catch (Exception e) {
            LOGGER.error("Failed to handle clientbound packet {}", packet.getClass().getName(), e);
        }
    }
}
