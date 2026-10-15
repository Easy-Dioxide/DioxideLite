package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventAttack;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventRotation;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.util.krs.FakePlayerEntity;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Optional;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.Backtrack).
 * Notable adaptations vs the original:
 *  - onReceivedPacket/onAttack/onMotion/onWorld handlers mapped to the client
 *    EventPacketReceive / EventAttack / EventRotation / EventWorld dispatch.
 *  - MC 26.3 removed ClientboundMoveEntityPacket#getXa/getYa/getZa: the delta is
 *    decoded through packet.getPositionDelta().decode(codec) (VecDelta API),
 *    taking PositionPath#endPosition() as the resulting position.
 *  - MC 26.3 turned ClientboundEntityPositionSyncPacket into a record whose
 *    position() returns a PositionPath directly (the old values() ->
 *    PositionMoveRotation accessor no longer exists); yRot()/xRot() records are
 *    read straight off the packet.
 *  - Krs tag() replaced by Feature#setSuffix.
 * All freeze/release range math, packet storage and smooth-release logic are
 * byte-exact Krs parity.
 */
public class Backtrack2 extends Feature {
   private static final int MAX_STORED_PACKETS = 512;

   private final NumberSetting trackDelay = new NumberSetting("Track Delay", this, 200.0, 0.0, 2000.0, 1.0);

   private final ArrayDeque<Packet<?>> storagePackets = new ArrayDeque<>();
   private final ArrayDeque<EntityPacketLoc> storageEntityMove = new ArrayDeque<>();
   private final Object storageLock = new Object();
   private final VecDeltaCodec simulatedPositionCodec = new VecDeltaCodec();
   private final MSTimer timer = new MSTimer();

   private Entity lastAttackedEntity = null;
   private int lastAttackedEntityId = -1;
   private Vec3 simulatedPosition = null;
   private Vec3 simulatedDeltaMovement = Vec3.ZERO;
   private float simulatedYaw = 0.0F;
   private float simulatedPitch = 0.0F;
   private volatile boolean needFreeze = false;

   public Backtrack2() {
      super("Backtrack2", Category.COMBAT);
   }

   public boolean isBacktracking() {
      return isEnabled() && needFreeze;
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
      releasePackets();
      clearTracking();
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         synchronized (storageLock) {
            storagePackets.clear();
            storageEntityMove.clear();
         }
         clearTracking();
         return;
      }

      if (event instanceof EventAttack attack) {
         Entity target = attack.getEntity();
         if (target != null && target != mc.player && !(target instanceof FakePlayerEntity)) {
            setTarget(target);
         }
         return;
      }

      if (event instanceof EventPacketReceive receiving) {
         onReceivedPacket(receiving);
         return;
      }

      if (event instanceof EventRotation) {
         onMotion();
      }
   }

   private void onReceivedPacket(EventPacketReceive event) {
      if (event == null || event.getPacket() == null || mc.player == null || mc.level == null)
         return;

      try {
         if (!validateTarget())
            return;

         Packet<?> packet = event.getPacket();
         if (packet instanceof ClientboundMoveEntityPacket movePacket) {
            handleEntityMovePacket(event, movePacket);
         } else if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
            handleTeleportPacket(event, teleportPacket);
         } else if (packet instanceof ClientboundEntityPositionSyncPacket positionSyncPacket) {
            handlePositionSyncPacket(event, positionSyncPacket);
         }
      } catch (Exception ignored) {
         releasePackets();
      }
   }

   private void onMotion() {
      if (mc.player == null || mc.level == null || !needFreeze)
         return;

      this.setSuffix((int) trackDelay.getValue() + "ms");

      try {
         if (!validateTarget())
            return;

         doSmoothRelease();
         if (!needFreeze)
            return;

         Entity target = lastAttackedEntity;
         Vec3 position = getTrackedPosition(target);
         if (target == null || position == null) {
            releasePackets();
            return;
         }

         if (isServerPositionInReleaseRange(position)) {
            releasePackets();
            return;
         }

         if (timer.currentTime() < Math.clamp((int) trackDelay.getValue(), 0, 2000))
            return;

         AABB targetBox = makeBoundingBox(target, position);
         double range = getLookingTargetRange(targetBox);
         if (range == Double.MAX_VALUE) {
            range = Math.sqrt(targetBox.distanceToSqr(mc.player.getEyePosition())) + 0.075D;
         }

         if (range <= 1.5D || timer.hasTimePassed(100L) && range >= 1.5D) {
            releasePackets();
         }
      } catch (Exception ignored) {
         releasePackets();
      }
   }

   private void handleEntityMovePacket(EventPacketReceive event, ClientboundMoveEntityPacket packet) {
      Entity entity = packet.getEntity(mc.level);
      if (!(entity instanceof Player) || !isTarget(entity))
         return;

      ensureSimulationInitialized(entity);

      AABB beforeBox = entity.getBoundingBox();
      Vec3 afterPosition = simulateMovePacket(entity, packet);
      AABB afterBox = makeBoundingBox(entity, afterPosition);
      double beforeRange = Math.sqrt(beforeBox.distanceToSqr(mc.player.getEyePosition()));
      double afterRange = Math.sqrt(afterBox.distanceToSqr(mc.player.getEyePosition()));

      if (!needFreeze && packet.hasPosition() && beforeRange <= 8.0D
            && afterRange >= 1.5D
            && afterRange <= 7.5D
            && afterRange > beforeRange + 0.02D
            && (!(entity instanceof Player player) || player.hurtTime <= getCalculatedMaxHurtTime())) {
         startFreeze();
         storePacket(event, packet, entity, afterPosition);
         return;
      }

      if (needFreeze && beforeRange > 8.0D && afterRange <= beforeRange) {
         releasePackets();
         return;
      }

      if (needFreeze) {
         storePacket(event, packet, entity, afterPosition);
      }
   }

   private void handleTeleportPacket(EventPacketReceive event, ClientboundTeleportEntityPacket packet) {
      Entity entity = mc.level.getEntity(packet.id());
      if (!(entity instanceof Player) || !isTarget(entity))
         return;

      ensureSimulationInitialized(entity);
      PositionMoveRotation current = new PositionMoveRotation(simulatedPosition, simulatedDeltaMovement, simulatedYaw, simulatedPitch);
      PositionMoveRotation next = PositionMoveRotation.calculateAbsolute(current, packet.change(), packet.relatives());
      updateSimulation(next.position(), next.deltaMovement(), next.yRot(), next.xRot());

      if (needFreeze) {
         storePacket(event, packet, entity, next.position());
      }
   }

   private void handlePositionSyncPacket(EventPacketReceive event, ClientboundEntityPositionSyncPacket packet) {
      Entity entity = mc.level.getEntity(packet.id());
      if (!(entity instanceof Player) || !isTarget(entity))
         return;

      // 26.3: packet.position() returns PositionPath directly (Krs read packet.values()).
      Vec3 position = packet.position().endPosition();
      updateSimulation(position, Vec3.ZERO, packet.yRot(), packet.xRot());

      if (needFreeze) {
         storePacket(event, packet, entity, position);
      }
   }

   private Vec3 simulateMovePacket(Entity entity, ClientboundMoveEntityPacket packet) {
      Vec3 position = simulatedPosition != null ? simulatedPosition : entity.position();

      if (packet.hasPosition()) {
         // 26.3: packet.getPositionDelta().decode(codec) replaces the old
         // codec.decode(packet.getXa(), packet.getYa(), packet.getZa()) triple.
         position = packet.getPositionDelta().decode(simulatedPositionCodec).endPosition();
         simulatedPositionCodec.setBase(position);
         simulatedPosition = position;
      }

      if (packet.hasRotation()) {
         simulatedYaw = packet.getYRot();
         simulatedPitch = packet.getXRot();
      }

      return position;
   }

   private void storePacket(EventPacketReceive event, Packet<?> packet, Entity entity, Vec3 position) {
      boolean overflow;
      synchronized (storageLock) {
         if (!needFreeze)
            return;
         overflow = storagePackets.size() >= MAX_STORED_PACKETS;

         if (!overflow) {
            event.setCancelled(true);
            storagePackets.add(packet);
            storageEntityMove.add(new EntityPacketLoc(entity, position));
         }
      }

      if (overflow) {
         releasePackets();
         return;
      }

      if (needFreeze && isServerPositionInReleaseRange(position))
         releasePackets();
   }

   private void startFreeze() {
      timer.reset();
      needFreeze = true;
   }

   private void releasePackets() {
      ArrayDeque<Packet<?>> pendingPackets;
      synchronized (storageLock) {
         pendingPackets = new ArrayDeque<>(storagePackets);
         storagePackets.clear();
         storageEntityMove.clear();
         needFreeze = false;
      }

      while (!pendingPackets.isEmpty())
         PacketUtil.handlePacket(pendingPackets.removeFirst());
   }

   private void clearTracking() {
      synchronized (storageLock) {
         storageEntityMove.clear();
      }
      resetTarget();
      simulatedPosition = null;
      simulatedDeltaMovement = Vec3.ZERO;
      simulatedYaw = 0.0F;
      simulatedPitch = 0.0F;
      needFreeze = false;
   }

   private void setTarget(Entity target) {
      if (lastAttackedEntityId != -1 && lastAttackedEntityId != target.getId()) {
         releasePackets();
      }

      lastAttackedEntity = target;
      lastAttackedEntityId = target.getId();
      initializeSimulation(target);
   }

   private void resetTarget() {
      lastAttackedEntity = null;
      lastAttackedEntityId = -1;
   }

   private boolean isTarget(Entity entity) {
      return entity != null && entity.getId() == lastAttackedEntityId && entity == lastAttackedEntity;
   }

   private boolean validateTarget() {
      if (lastAttackedEntity == null || mc.player == null || mc.level == null) {
         if (needFreeze)
            releasePackets();
         resetTarget();
         return false;
      }

      Entity worldEntity = mc.level.getEntity(lastAttackedEntityId);
      if (worldEntity != lastAttackedEntity
            || lastAttackedEntity.isRemoved()
            || !lastAttackedEntity.isAlive()
            || mc.player.distanceTo(lastAttackedEntity) > 10.0F) {
         if (needFreeze)
            releasePackets();
         resetTarget();
         return false;
      }

      return true;
   }

   private void doSmoothRelease() {
      Entity target = lastAttackedEntity;
      if (target == null) {
         releasePackets();
         return;
      }

      boolean found = false;
      synchronized (storageLock) {
         for (EntityPacketLoc loc : storageEntityMove) {
            if (target == loc.entity) {
               found = true;
               break;
            }
         }
      }

      if (!found)
         releasePackets();
   }

   private void ensureSimulationInitialized(Entity entity) {
      if (simulatedPosition == null)
         initializeSimulation(entity);
   }

   private void initializeSimulation(Entity entity) {
      Vec3 base = entity.getPositionCodec().getBase();
      if (base == null)
         base = entity.position();

      simulatedPositionCodec.setBase(base);
      simulatedPosition = entity.position();
      simulatedDeltaMovement = entity.getDeltaMovement();
      simulatedYaw = entity.getYRot();
      simulatedPitch = entity.getXRot();
   }

   private void updateSimulation(Vec3 position, Vec3 deltaMovement, float yaw, float pitch) {
      simulatedPosition = position;
      simulatedDeltaMovement = deltaMovement;
      simulatedYaw = yaw;
      simulatedPitch = pitch;
      simulatedPositionCodec.setBase(position);
   }

   private Vec3 getTrackedPosition(Entity entity) {
      if (entity == null)
         return null;

      synchronized (storageLock) {
         Iterator<EntityPacketLoc> iterator = storageEntityMove.descendingIterator();
         while (iterator.hasNext()) {
            EntityPacketLoc loc = iterator.next();
            if (loc.entity == entity)
               return loc.position;
         }
      }

      return simulatedPosition;
   }

   private boolean isServerPositionInReleaseRange(Vec3 position) {
      double releaseRange = 3.5D;
      return mc.player != null && position != null
            && position.distanceToSqr(mc.player.position()) <= releaseRange * releaseRange;
   }

   private AABB makeBoundingBox(Entity entity, Vec3 position) {
      double halfWidth = entity.getBbWidth() / 2.0D;
      double minY = position.y - 0.1D;
      double maxY = position.y + entity.getBbHeight() + 0.1D;
      return new AABB(
            position.x - halfWidth,
            minY,
            position.z - halfWidth,
            position.x + halfWidth,
            maxY,
            position.z + halfWidth
      );
   }

   private double getLookingTargetRange(AABB box) {
      Vec3 eyePos = mc.player.getEyePosition();
      Vec3 reachVec = eyePos.add(mc.player.getViewVector(1.0F).scale(8.0D));
      Optional<Vec3> hit = box.clip(eyePos, reachVec);
      return hit.map(eyePos::distanceTo).orElse(Double.MAX_VALUE);
   }

   private int getCalculatedMaxHurtTime() {
      return 6;
   }

   private static class EntityPacketLoc {
      final Entity entity;
      final Vec3 position;

      EntityPacketLoc(Entity entity, Vec3 position) {
         this.entity = entity;
         this.position = position;
      }
   }
}
