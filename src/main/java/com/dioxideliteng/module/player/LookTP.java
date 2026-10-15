package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.pathfinder.MainPathFinder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.LookTP).
 *
 * Adaptations:
 *  - Krs onMotion (MotionEvent) -> Events.PRE_MOTION (the sendPosition-stage event; the path
 *    coordinates are written through its x/y/z/onGround setters, which the outgoing packet
 *    reads). MotionEvent#isMoving has no equivalent field (the substituted coordinates are
 *    always transmitted), so it is dropped;
 *  - Krs onReceivedPacket -> {@code event instanceof EventPacketReceive};
 *  - Krs onUpdate -> Events.UPDATE;
 *  - {@code Module#tempEnabled} (the enabled flag) -> {@link Feature#isEnabled()};
 *  - {@code Client.notificationManager.addNotification} replaced with DynamicIslandManager.notifyInfo.
 */
public class LookTP extends Feature {

    private final BooleanSetting tpOnGroundPacket = new BooleanSetting("TP On Ground Packet", this, true);
    private final BooleanSetting clientsideTeleport = new BooleanSetting("Clientside Teleport", this, false);

    private final MSTimer tpTimer = new MSTimer();
    private ArrayList<Vec3> clientsidePath = null;
    private int clientsidePathIndex = 0;
    private int clientsidePathSize = 0;
    private int clientsideProgress = 0;
    private Vec3 clientsideLockPosition = null;
    private Vec3 clientsideTargetPosition = null;
    private boolean waitingForClientsideTeleport = false;

    public LookTP() {
        super("LookTP", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        resetClientsideTeleport();
    }

    @Override
    public void onEnable() {
        resetClientsideTeleport();
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.PRE_MOTION) {
            onMotion();
        } else if (event == Events.UPDATE) {
            onUpdate();
        } else if (event instanceof EventPacketReceive receiving) {
            onReceivedPacket(receiving.getPacket());
        }
    }

    private void onMotion() {
        if (mc.player == null || !isMovingServerSide()) return;

        if (clientsideLockPosition != null) {
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.setPos(clientsideLockPosition.x, clientsideLockPosition.y, clientsideLockPosition.z);
        }

        Vec3 path = clientsidePath.get(clientsidePathIndex);
        Events.PRE_MOTION.setX(path.x);
        Events.PRE_MOTION.setY(path.y);
        Events.PRE_MOTION.setZ(path.z);
        Events.PRE_MOTION.setOnGround(tpOnGroundPacket.getValue());

        clientsidePathIndex++;
        clientsideProgress = clientsidePathSize <= 0 ? 0 : Math.min(100, Math.round((float) clientsidePathIndex / clientsidePathSize * 100f));
        if (clientsidePathIndex >= clientsidePath.size()) {
            clientsidePath = null;
            clientsidePathIndex = 0;
            waitingForClientsideTeleport = true;
        }
    }

    private void onUpdate() {
        if (mc.player == null || mc.level == null) return;

        if (waitingForClientsideTeleport) {
            if (clientsideTargetPosition != null) {
                mc.player.setDeltaMovement(Vec3.ZERO);
                mc.player.setPos(clientsideTargetPosition.x, clientsideTargetPosition.y, clientsideTargetPosition.z);
            }

            resetClientsideTeleport();
            DynamicIslandManager.notifyInfo("Success", "Teleported!");
            tpTimer.reset();
            return;
        }

        if (isMovingServerSide()) {
            if (clientsideLockPosition != null) {
                mc.player.setDeltaMovement(Vec3.ZERO);
                mc.player.setPos(clientsideLockPosition.x, clientsideLockPosition.y, clientsideLockPosition.z);
            }
            return;
        }

        if (tpTimer.hasTimePassed(200L) && InputConstants.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) && mc.options.keyUse.isDown()) {
            Vec3 eyePosition = mc.player.getEyePosition(0.1f);
            Vec3 lookVector = mc.player.getViewVector(0.1f);
            Vec3 targetPosition = eyePosition.add(lookVector.scale(10000.0));
            ClipContext context = new ClipContext(eyePosition, targetPosition, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player);

            BlockHitResult posResult = mc.level.clip(context);
            if (mc.level.getBlockState(posResult.getBlockPos()).isAir()) return;

            ArrayList<Vec3> paths;
            paths = MainPathFinder.computePath(mc.player.position(), posResult.getLocation());

            if (paths.isEmpty()) {
                DynamicIslandManager.notifyInfo("Look TP", "Failed to teleport");
                tpTimer.reset();
                return;
            }

            if (clientsideTeleport.getValue()) {
                clientsidePath = new ArrayList<>(paths);
                clientsidePathIndex = 0;
                clientsidePathSize = clientsidePath.size();
                clientsideProgress = 0;
                clientsideLockPosition = mc.player.position();
                clientsideTargetPosition = posResult.getLocation();
                waitingForClientsideTeleport = false;
                mc.player.setDeltaMovement(Vec3.ZERO);
                DynamicIslandManager.notifyInfo("Look TP", "Teleporting...");
                tpTimer.reset();
                return;
            }

            for (Vec3 path : paths) {
                PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(path.x, path.y, path.z, tpOnGroundPacket.getValue(), mc.player.horizontalCollision));
            }

            mc.player.setPos(posResult.getLocation().x, posResult.getLocation().y, posResult.getLocation().z);

            DynamicIslandManager.notifyInfo("Success", "Teleported!");
            tpTimer.reset();
        }
    }

    private void onReceivedPacket(Packet<?> packet) {
        if (mc.player == null) return;

        if ((isMovingServerSide() || waitingForClientsideTeleport) && packet instanceof ClientboundPlayerPositionPacket) {
            resetClientsideTeleport();
            DynamicIslandManager.notifyInfo("Look TP", "Failed to teleport");
            tpTimer.reset();
        }
    }

    public boolean shouldStopClientInput() {
        return isEnabled() && (isMovingServerSide() || waitingForClientsideTeleport);
    }

    public boolean isClientsideTeleporting() {
        return isMovingServerSide() || waitingForClientsideTeleport;
    }

    public int getTeleportProgressPercent() {
        return isClientsideTeleporting() ? clientsideProgress : 0;
    }

    private boolean isMovingServerSide() {
        return clientsidePath != null && clientsidePathIndex < clientsidePath.size();
    }

    private void resetClientsideTeleport() {
        clientsidePath = null;
        clientsidePathIndex = 0;
        clientsidePathSize = 0;
        clientsideProgress = 0;
        clientsideLockPosition = null;
        clientsideTargetPosition = null;
        waitingForClientsideTeleport = false;
    }
}
