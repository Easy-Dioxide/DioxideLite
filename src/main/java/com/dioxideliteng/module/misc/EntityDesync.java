package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.PacketUtil;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.EntityDesync).
 * Krs parity: on enable the ridden entity is captured; on the first update the player is
 * removed from the vehicle client-side and the entity is unloaded from the level, so the
 * server still sees the player riding it while the client moves freely. Jump packets are
 * cancelled to keep the desync, and disable silently dismounts. World changes toggle the
 * module off.
 */
public class EntityDesync extends Feature {
   private Entity ridingEntity = null;
   private boolean desynced = false;

   public EntityDesync() {
      super("Entity Desync", Category.MISC);
   }

   @Override
   public void onDisable() {
      if (mc.player == null) return;
      Entity entity = ridingEntity;
      if (desynced && entity != null) {
         PacketUtil.sendPacketAsSilent(new ServerboundPlayerCommandPacket(
               entity,
               ServerboundPlayerCommandPacket.Action.STOP_RIDING_JUMP
         ));
         DynamicIslandManager.notifyInfo("Entity Desync", "Dismounted desynced entity");
      }
      ridingEntity = null;
      desynced = false;
   }

   @Override
   public void onEnable() {
      var player = mc.player;
      if (player == null) return;

      Entity vehicle = player.getVehicle();
      if (vehicle != null) {
         ridingEntity = vehicle;
      } else {
         DynamicIslandManager.notifyInfo("Entity Desync", "You must be riding to entity");
         toggle();
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         toggle();
         return;
      }
      if (event instanceof EventPacketSend sending && !sending.isCancelled()) {
         if (mc.player == null) return;

         if (sending.getPacket() instanceof ServerboundPlayerCommandPacket commandPacket
               && (commandPacket.getAction() == ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP
               || commandPacket.getAction() == ServerboundPlayerCommandPacket.Action.STOP_RIDING_JUMP))
            sending.setCancelled(true);
         return;
      }
      if (event == Events.UPDATE) {
         onUpdate();
      }
   }

   private void onUpdate() {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return;

      if (!desynced) {
         Entity entity = ridingEntity;
         if (entity != null) {
            try {
               player.removeVehicle();
               level.removeEntity(entity.getId(), Entity.RemovalReason.UNLOADED_TO_CHUNK);
               DynamicIslandManager.notifyInfo("Entity Desync", "Desynced riding entity");
            } catch (Exception ignored) {
            }
            desynced = true;
         }
      } else if (MovementUtil.hasMotion()) {
         Entity entity = ridingEntity;
         if (entity == null) return;
         entity.setPos(player.position());
         PacketUtil.sendPacket(ServerboundMoveVehiclePacket.fromEntity(entity));
      }
   }
}
