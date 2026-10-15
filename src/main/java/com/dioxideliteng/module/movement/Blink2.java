package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.combat.KillAura2;
import com.dioxideliteng.module.misc.TPStealer;
import com.dioxideliteng.module.misc.TPSurround;
import com.dioxideliteng.module.misc.TransactionConfirmBlinker;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.util.krs.BlinkUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.Blink) and adapted to the
 * DioxideLiteNG event system. Krs parity notes:
 *  - The module keeps the Krs shape: a "Directions" mode (Both / Incoming / Outgoing,
 *    default Outgoing), doBlink on enable and on every update, and sync+stopBlink on disable.
 *  - In Krs the packet filtering itself lives in ModuleManager.onSendPacket /
 *    onReceivedPacket (the global packet hooks) and calls Blink.shouldBlinkOutgoing() /
 *    Blink.shouldQueueIncoming(). DioxideLiteNG has no global module-side packet hook, so the
 *    exact same filter runs inside this module's own EventPacketSend / EventPacketReceive
 *    handlers; the guard chain (BlinkUtil blinking + direction + !limiter + spread-teleport
 *    ownership + movement suppression) is reproduced line by line.
 *  - Krs ModuleManager.getModuleState(KillAura.class) maps onto the ported KillAura2
 *    (FeatureManager.killAura2, null-safe); Krs KillAura.isSendingSpreadTeleportPacket /
 *    shouldSuppressMovementDuringSpreadTeleport / shouldCancelUseItemOnWhileBlinking /
 *    closestEntity map onto the same-named KillAura2 members.
 *  - Krs TPStealer/TPSurround static hooks map onto their faithful ports in module.misc.
 *  - Krs ModuleManager.getModuleState(TransactionConfirmBlinker.class) maps onto the ported
 *    TransactionConfirmBlinker.shouldIncludeTransactions() (registered-instance check).
 *  - The Krs !getModuleState(Freecam.class) guard is dropped: no Freecam module exists in
 *    this client (it is outside the exploit porting scope and has no ported equivalent), so
 *    the guard would be a constant there. Everything else is unchanged.
 *  - The Krs tag() suffix maps onto setSuffix, refreshed on tick as in the other ports.
 */
public class Blink2 extends Feature {
   private static final String BOTH = "Both";
   private static final String INCOMING = "Incoming";
   private static final String OUTGOING = "Outgoing";

   private final ModeSetting directions = new ModeSetting("Directions", this, OUTGOING,
         new String[]{BOTH, INCOMING, OUTGOING});

   private static Blink2 instance;

   public Blink2() {
      super("Blink2", Category.MOVEMENT);
      instance = this;
   }

   // ===================== Krs Blink static API =====================

   public static boolean shouldBlinkIncoming() {
      // Krs parity: the lookup is ModuleManager.getModule(Blink.class) (registration, not
      // enablement); the module is registered for the whole session, so instance != null
      // is the faithful equivalent.
      return instance != null
            && (instance.directions.is(BOTH) || instance.directions.is(INCOMING));
   }

   public static boolean shouldQueueIncoming(Packet<?> packet) {
      return shouldBlinkIncoming() && !shouldHandleIncomingImmediately(packet);
   }

   private static boolean shouldHandleIncomingImmediately(Packet<?> packet) {
      return packet instanceof ClientboundDisconnectPacket
            || isSetbackPacket(packet)
            || isScoreboardOrPlayerListPacket(packet)
            || isChatPacket(packet)
            || isInventoryUpdatePacket(packet)
            || isEntityAwarenessPacket(packet);
   }

   private static boolean isSetbackPacket(Packet<?> packet) {
      return packet instanceof ClientboundPlayerPositionPacket
            || packet instanceof ClientboundPlayerRotationPacket
            || packet instanceof ClientboundMoveVehiclePacket;
   }

   private static boolean isScoreboardOrPlayerListPacket(Packet<?> packet) {
      return packet instanceof ClientboundSetObjectivePacket
            || packet instanceof ClientboundSetDisplayObjectivePacket
            || packet instanceof ClientboundSetScorePacket
            || packet instanceof ClientboundResetScorePacket
            || packet instanceof ClientboundSetPlayerTeamPacket
            || packet instanceof ClientboundPlayerInfoUpdatePacket
            || packet instanceof ClientboundPlayerInfoRemovePacket
            || packet instanceof ClientboundTabListPacket
            || packet instanceof ClientboundTrackedWaypointPacket;
   }

   private static boolean isChatPacket(Packet<?> packet) {
      return packet instanceof ClientboundPlayerChatPacket
            || packet instanceof ClientboundSystemChatPacket
            || packet instanceof ClientboundDisguisedChatPacket
            || packet instanceof ClientboundDeleteChatPacket;
   }

   private static boolean isInventoryUpdatePacket(Packet<?> packet) {
      if (packet instanceof ClientboundSetPlayerInventoryPacket)
         return true;

      if (mc.player == null)
         return false;

      int currentContainerId = mc.player.containerMenu.containerId;
      if (packet instanceof ClientboundContainerSetSlotPacket slotPacket)
         return slotPacket.getContainerId() == 0 || slotPacket.getContainerId() == currentContainerId;
      if (packet instanceof ClientboundContainerSetContentPacket contentPacket)
         return contentPacket.containerId() == 0 || contentPacket.containerId() == currentContainerId;

      return false;
   }

   private static boolean isEntityAwarenessPacket(Packet<?> packet) {
      return packet instanceof ClientboundAddEntityPacket
            || packet instanceof ClientboundRemoveEntitiesPacket
            || packet instanceof ClientboundMoveEntityPacket
            || packet instanceof ClientboundMoveMinecartPacket
            || packet instanceof ClientboundEntityPositionSyncPacket
            || packet instanceof ClientboundTeleportEntityPacket
            || packet instanceof ClientboundRotateHeadPacket
            || packet instanceof ClientboundSetEntityDataPacket
            || packet instanceof ClientboundUpdateAttributesPacket
            || packet instanceof ClientboundSetEquipmentPacket
            || packet instanceof ClientboundAnimatePacket
            || packet instanceof ClientboundHurtAnimationPacket
            || packet instanceof ClientboundDamageEventPacket
            || packet instanceof ClientboundEntityEventPacket
            || packet instanceof ClientboundUpdateMobEffectPacket
            || packet instanceof ClientboundRemoveMobEffectPacket
            || packet instanceof ClientboundSetPassengersPacket
            || packet instanceof ClientboundSetEntityLinkPacket
            || packet instanceof ClientboundTakeItemEntityPacket
            || packet instanceof ClientboundProjectilePowerPacket
            || (packet instanceof ClientboundExplodePacket explosionPacket
                && explosionPacket.playerKnockback().isEmpty())
            || packet instanceof ClientboundSetHealthPacket
            || packet instanceof ClientboundPlayerCombatEnterPacket
            || packet instanceof ClientboundPlayerCombatEndPacket
            || packet instanceof ClientboundPlayerCombatKillPacket;
   }

   public static boolean shouldBlinkOutgoing() {
      return instance != null
            && (instance.directions.is(BOTH) || instance.directions.is(OUTGOING));
   }

   // ===================== Krs Blink lifecycle =====================

   @Override
   public void onEnable() {
      BlinkUtil.INSTANCE.doBlink();
   }

   @Override
   public void onDisable() {
      BlinkUtil.INSTANCE.sync(true, true);
      BlinkUtil.INSTANCE.stopBlink();
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.TICK) {
         // Krs parity: tag() returns the current direction; the suffix is refreshed on
         // tick like the other ported mode modules.
         this.setSuffix(this.directions.getValue());
         return;
      }
      if (event == Events.UPDATE) {
         BlinkUtil.INSTANCE.doBlink();
         return;
      }
      if (event instanceof EventPacketSend sending && !sending.isCancelled()) {
         onSendPacket(sending);
         return;
      }
      if (event instanceof EventPacketReceive receiving && !receiving.isCancelled()) {
         onReceivedPacket(receiving);
      }
   }

   // ===================== Krs ModuleManager packet filter (outgoing) =====================

   private void onSendPacket(EventPacketSend sending) {
      if (mc.player == null || mc.level == null)
         return;

      Packet<?> packet = sending.getPacket();

      if (BlinkUtil.INSTANCE.getBlinking() && shouldBlinkOutgoing() && !BlinkUtil.INSTANCE.getLimiter()) {
         KillAura2 killAura2 = FeatureManager.killAura2;

         if (killAura2 != null && killAura2.isSendingSpreadTeleportPacket())
            return;

         if (TPStealer.isSendingSpreadTeleportPacket())
            return;

         if (TPSurround.isSendingSpreadTeleportPacket())
            return;

         if (packet instanceof ServerboundMovePlayerPacket
               && ((killAura2 != null && killAura2.shouldSuppressMovementDuringSpreadTeleport())
               || TPStealer.shouldSuppressMovement()
               || TPSurround.shouldSuppressMovement())) {
            sending.setCancelled(true);
            return;
         }

         if (packet instanceof ServerboundMovePlayerPacket || killAura2 != null && killAura2.isEnabled() && killAura2.closestEntity != null && killAura2.shouldCancelUseItemOnWhileBlinking() && packet instanceof ServerboundUseItemOnPacket)
            sending.setCancelled(true);

         if (packet instanceof ServerboundMovePlayerPacket.Pos || packet instanceof ServerboundMovePlayerPacket.Rot || packet instanceof ServerboundMovePlayerPacket.PosRot || packet instanceof ServerboundMovePlayerPacket.StatusOnly || packet instanceof ServerboundPlayerCommandPacket || packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundPlayerActionPacket || packet instanceof ServerboundUseItemPacket || TransactionConfirmBlinker.shouldIncludeTransactions() && packet instanceof ServerboundPongPacket) {
            sending.setCancelled(true);
            BlinkUtil.INSTANCE.addPacket(packet);
         }
      }
   }

   // ===================== Krs ModuleManager packet filter (incoming) =====================

   private void onReceivedPacket(EventPacketReceive receiving) {
      if (mc.player == null || mc.level == null)
         return;

      if (BlinkUtil.INSTANCE.getBlinking() && shouldQueueIncoming(receiving.getPacket())) {
         receiving.setCancelled(true);
         BlinkUtil.INSTANCE.addIncomingPacket(receiving.getPacket());
      }
   }
}
