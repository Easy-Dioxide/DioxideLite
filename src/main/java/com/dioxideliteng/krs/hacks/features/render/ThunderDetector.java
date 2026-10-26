package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.features.ReceivedPacketEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.ChatUtil;
import com.dioxideliteng.krs.utils.entity.EntityExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import com.dioxideliteng.krs.utils.input.Keys;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ThunderDetector extends Module {
    private static final int MAX_EXCLUDED_LIGHTNINGS = 256;
    private static final long EXCLUSION_NANOS = 3_000_000_000L;
    private static final Map<BlockPos, Long> excludedLightnings = new LinkedHashMap<>();

    public ThunderDetector() {
        super("Thunder Detector", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, false);
    }

    public static synchronized void excludeLightningAt(Vec3 position) {
        if (position == null)
            return;

        long now = System.nanoTime();
        removeExpiredExclusions(now);
        while (excludedLightnings.size() >= MAX_EXCLUDED_LIGHTNINGS) {
            Iterator<BlockPos> iterator = excludedLightnings.keySet().iterator();
            if (!iterator.hasNext())
                break;
            iterator.next();
            iterator.remove();
        }
        excludedLightnings.put(BlockPos.containing(position), now + EXCLUSION_NANOS);
    }

    @Override
    public void onDisable() {
        clearExclusions();
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onWorld(WorldEvent event) {
        clearExclusions();
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
        var player = mc.player;
        if (player == null || mc.level == null) return;

        var packet = event.packet;
        if (packet instanceof ClientboundAddEntityPacket addEntityPacket && addEntityPacket.getType() == EntityTypes.LIGHTNING_BOLT) {
            if (isExcludedLightning(addEntityPacket.getX(), addEntityPacket.getY(), addEntityPacket.getZ()))
                return;

            String showMessage = "Detected lightning at " + (int) addEntityPacket.getX() + " " + (int) addEntityPacket.getY() + " " + (int) addEntityPacket.getZ()
                    + " (" + (int) EntityExtension.squaredDistanceToWithoutY(player, addEntityPacket.getX(), addEntityPacket.getZ()) + " blocks away)";
            Component tpMessage = Component.literal("§e> §7(§eClick to Teleport§7) §e" + showMessage).withStyle(style ->
                    style.withClickEvent(new ClickEvent.SuggestCommand("/tp " + addEntityPacket.getX() + " " + addEntityPacket.getY() + " " + addEntityPacket.getZ()))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("§lClick to easily prepare for teleport to " + (int) addEntityPacket.getX() + ", " + (int) addEntityPacket.getY() + ", " + (int) addEntityPacket.getZ())))
            );
            ChatUtil.printModifiedChat(tpMessage);
            KrsClient.notificationManager.addNotification("Lightning detected!", showMessage);
        }
    }

    private static synchronized boolean isExcludedLightning(double x, double y, double z) {
        long now = System.nanoTime();
        BlockPos lightningPos = BlockPos.containing(x, y, z);
        boolean excluded = false;

        Iterator<Map.Entry<BlockPos, Long>> iterator = excludedLightnings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            if (entry.getValue() < now) {
                iterator.remove();
                continue;
            }

            if (entry.getKey().equals(lightningPos)) {
                excluded = true;
                iterator.remove();
                break;
            }
        }

        return excluded;
    }

    private static void removeExpiredExclusions(long now) {
        excludedLightnings.entrySet().removeIf(entry -> entry.getValue() - now <= 0L);
    }

    private static synchronized void clearExclusions() {
        excludedLightnings.clear();
    }
}
