package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.EntityDesync) and adapted to the client.
 * MovementUtil.hasMotion() and PacketUtil.sendPacketAsSilent() were inlined; the silent dismount uses a bypass
 * flag so this module does not cancel its own STOP_RIDING_JUMP packet.
 */
public class EntityDesync extends Feature {
   private Entity ridingEntity;
   private boolean desynced;
   private boolean bypass;

   public EntityDesync() {
      super("EntityDesync", Category.MISC);
   }

   @Override
   public void onEnable() {
      this.ridingEntity = null;
      this.desynced = false;
      this.bypass = false;
      var player = mc.player;
      if (player == null) return;
      Entity vehicle = player.getVehicle();
      if (vehicle != null) {
         this.ridingEntity = vehicle;
      } else {
         setEnabled(false);
      }
   }

   @Override
   public void onDisable() {
      Entity entity = this.ridingEntity;
      if (this.desynced && entity != null && mc.player != null && mc.getConnection() != null) {
         this.bypass = true;
         try {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(entity, ServerboundPlayerCommandPacket.Action.STOP_RIDING_JUMP));
         } finally {
            this.bypass = false;
         }
      }
      this.ridingEntity = null;
      this.desynced = false;
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         setEnabled(false);
         return;
      }
      if (event instanceof EventPacketSend sending && !this.bypass) {
         if (mc.player == null) return;
         if (sending.getPacket() instanceof ServerboundPlayerCommandPacket command
               && (command.getAction() == ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP
               || command.getAction() == ServerboundPlayerCommandPacket.Action.STOP_RIDING_JUMP)) {
            event.setCancelled(true);
         }
         return;
      }
      if (event != Events.TICK) return;

      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return;

      if (!this.desynced) {
         Entity entity = this.ridingEntity;
         if (entity != null) {
            try {
               player.removeVehicle();
               level.removeEntity(entity.getId(), Entity.RemovalReason.UNLOADED_TO_CHUNK);
            } catch (Exception ignored) {
            }
            this.desynced = true;
         }
      } else {
         var movement = player.getDeltaMovement();
         if (movement.x * movement.x + movement.z * movement.z > 1.0E-6) {
            Entity entity = this.ridingEntity;
            if (entity == null) return;
            entity.setPos(player.position());
            mc.getConnection().send(ServerboundMoveVehiclePacket.fromEntity(entity));
         }
      }
   }
}
