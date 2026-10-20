package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.HackerDetector).
 *
 * Adaptations:
 *  - {@code Client.notificationManager.addNotification} replaced with DynamicIslandManager.notifyInfo;
 *  - Krs onUpdate -> Events.UPDATE; Krs onWorld -> Events.WORLD.
 */
public class HackerDetector2 extends Feature {

    private final Map<UUID, PlayerState> playerStates = new HashMap<>();
    private final Set<UUID> seenPlayers = new HashSet<>();

    private static class PlayerState {
        double lastX;
        double lastY;
        double lastZ;
        double lastYMotion;
        long lastAlertTime;
        long flyingTime;
        long lastUpdateTime;
        final long spawnTime;
        boolean hasPosition;

        PlayerState(long spawnTime) {
            this.spawnTime = spawnTime;
            this.lastUpdateTime = spawnTime;
        }
    }

    public HackerDetector2() {
        super("HackerDetector2", Category.PLAYER);
    }

    private void reset() {
        playerStates.clear();
        seenPlayers.clear();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.UPDATE) {
            onUpdate();
        } else if (event == Events.WORLD) {
            reset();
        }
    }

    private void onUpdate() {
        if (mc.level == null) return;

        long currentTime = System.currentTimeMillis();
        seenPlayers.clear();

        for (Player player : mc.level.players()) {
            if (player instanceof LocalPlayer) continue;

            UUID playerId = player.getUUID();
            seenPlayers.add(playerId);
            PlayerState state = playerStates.computeIfAbsent(playerId, id -> new PlayerState(currentTime));

            if (player.isDeadOrDying() || player.isCreative() || player.hurtTime != 0) continue;
            if (currentTime - state.spawnTime < 2000) continue;

            double currentX = player.getX();
            double currentY = player.getY();
            double currentZ = player.getZ();
            boolean isOnGround = player.onGround() || !mc.level.getBlockState(player.blockPosition().below()).isAir();

            if (state.hasPosition) {
                double dx = currentX - state.lastX;
                double dz = currentZ - state.lastZ;
                double distanceXZSqr = dx * dx + dz * dz;
                double motionY = currentY - state.lastY;

                double baseSpeed = 0.62;
                var speedEffect = player.getEffect(MobEffects.SPEED);
                if (speedEffect != null) {
                    int amplifier = speedEffect.getAmplifier();
                    baseSpeed *= 1.2 + (amplifier * 0.01);
                }
                double maxSpeed = isOnGround ? baseSpeed : baseSpeed * 1.5;

                if (distanceXZSqr > maxSpeed * maxSpeed && (currentTime - state.lastAlertTime) >= 5000) {
                    DynamicIslandManager.notifyInfo("Hacker Detected", player.getName().getString() + " is using speed hack!");
                    state.lastAlertTime = currentTime;
                }

                boolean isFlying = !isOnGround &&
                        !player.isSpectator() &&
                        !player.getAbilities().flying &&
                        !player.getAbilities().mayfly &&
                        !player.hasEffect(MobEffects.LEVITATION) &&
                        !player.isInWater() &&
                        !player.onClimbable() &&
                        !player.isInLava() &&
                        !player.isUnderWater();

                long deltaTime = currentTime - state.lastUpdateTime;
                long flyingDuration = state.flyingTime;

                if (isFlying && Math.abs(motionY - state.lastYMotion) < 0.01) {
                    flyingDuration += deltaTime;

                    if (flyingDuration > 2000 && (currentTime - state.lastAlertTime) >= 3000) {
                        DynamicIslandManager.notifyInfo("Hacker Detected", player.getName().getString() + " is using fly hack!");
                        state.lastAlertTime = currentTime;
                        flyingDuration = 0;
                    }
                } else {
                    flyingDuration = 0;
                }

                state.lastYMotion = motionY;
                state.flyingTime = flyingDuration;
                state.lastUpdateTime = currentTime;
            }

            state.lastX = currentX;
            state.lastY = currentY;
            state.lastZ = currentZ;
            state.hasPosition = true;
        }

        Iterator<Map.Entry<UUID, PlayerState>> iterator = playerStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerState> entry = iterator.next();
            if (!seenPlayers.contains(entry.getKey()))
                iterator.remove();
        }
    }
}
