package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventPreMotion;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.disabler) and adapted.
 *  Mode dispatch inlines the Krs DisablerModeManager/DisablerEvent pattern. The HypixelSprint
 *  sub-feature is omitted (EventPreMotion exposes no yaw setter). Notifications/chat prints,
 *  the InvManager.getCleaning() hook, and silent-packet sends are dropped or inlined
 *  (ThreadLocalRandom / mc.getConnection().send with re-entry guards for recursive resend). */
public class Disabler2 extends Feature {
   private static final String HYPIXEL = "Hypixel";
   private static final String MINEPLEX = "Mineplex";
   private static final String CUBECRAFT_BEDROCK = "Cubecraft Bedrock";
   private static final String MINELAND = "Mineland";
   private static final String MINIBLOX = "Miniblox";
   private static final String VERUS = "Verus";
   private static final String VULCAN_SCAFFOLD = "Vulcan Scaffold";

   private final ModeSetting mode = new ModeSetting("Disabler Mode", this, HYPIXEL,
         new String[]{HYPIXEL, MINEPLEX, CUBECRAFT_BEDROCK, MINELAND, MINIBLOX, VERUS, VULCAN_SCAFFOLD});

   private final BooleanSetting hypixelInventoryMove = new BooleanSetting("Hypixel Inventory Move", this, true);
   private final BooleanSetting hypixelMotion = new BooleanSetting("Hypixel NCP Motion", this, false);

   // Hypixel state
   private boolean hypWatchdogDisabled;
   private boolean hypStartDisabler;
   private boolean hypStuckOnAir;
   private int hypAirStuckTicks;
   private int hypAirTicks;
   private boolean hypSentFirstOpen;
   private boolean hypCaughtClientStatus;
   private boolean hypCaughtCloseWindow;

   // Mineplex state
   private boolean mineplexDisabled;

   // Mineland state
   private long minelandRespawnTimes;

   // Re-entry guards for packet resends (replaces Krs silent-send semantics)
   private boolean minibloxResending;
   private boolean verusResending;

   private String activeMode = null;

   public Disabler2() {
      super("Disabler2", Category.MISC);
      this.hypixelInventoryMove.setVisible(() -> this.mode.is(HYPIXEL));
      this.hypixelMotion.setVisible(() -> this.mode.is(HYPIXEL));
   }

   @Override
   public void onEnable() {
      this.activeMode = this.mode.getValue();
      enableMode(this.activeMode);
   }

   @Override
   public void onDisable() {
      resetMode(this.activeMode);
      this.activeMode = null;
   }

   private void enableMode(String m) {
      if (HYPIXEL.equals(m)) {
         this.hypStartDisabler = true;
      }
   }

   private void resetMode(String m) {
      if (m == null) return;
      switch (m) {
         case HYPIXEL -> {
            this.hypWatchdogDisabled = false;
            this.hypStartDisabler = false;
            this.hypStuckOnAir = false;
            this.hypAirStuckTicks = 0;
            this.hypAirTicks = 0;
            this.hypSentFirstOpen = false;
            this.hypCaughtClientStatus = false;
            this.hypCaughtCloseWindow = false;
         }
         case MINEPLEX -> this.mineplexDisabled = false;
         default -> { }
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         onWorld(this.mode.getValue());
         return;
      }
      if (event == Events.TICK) {
         String current = this.mode.getValue();
         this.setSuffix(current);
         if (!current.equals(this.activeMode)) {
            resetMode(this.activeMode);
            this.activeMode = current;
            enableMode(current);
         }
         onUpdate(current);
         return;
      }
      if (event instanceof EventPreMotion preMotion) {
         onMotion(this.mode.getValue(), preMotion);
         return;
      }
      if (event instanceof EventPacketSend sending && !sending.isCancelled()) {
         onSendPacket(this.mode.getValue(), sending);
         return;
      }
      if (event instanceof EventPacketReceive receiving && !receiving.isCancelled()) {
         onReceivedPacket(this.mode.getValue(), receiving);
      }
   }

   private void onUpdate(String m) {
      if (HYPIXEL.equals(m)) hypixelOnUpdate();
   }

   private void onMotion(String m, EventPreMotion event) {
      if (HYPIXEL.equals(m)) hypixelOnMotion(event);
      else if (VULCAN_SCAFFOLD.equals(m)) vulcanOnMotion();
   }

   private void onSendPacket(String m, EventPacketSend sending) {
      switch (m) {
         case MINEPLEX -> mineplexOnSendPacket(sending);
         case CUBECRAFT_BEDROCK -> cubecraftOnSendPacket(sending);
         case MINELAND -> minelandOnSendPacket(sending);
         case MINIBLOX -> minibloxOnSendPacket(sending);
         case VERUS -> verusOnSendPacket(sending);
         default -> { }
      }
   }

   private void onReceivedPacket(String m, EventPacketReceive receiving) {
      switch (m) {
         case HYPIXEL -> hypixelOnReceivedPacket(receiving);
         case MINELAND -> minelandOnReceivedPacket(receiving);
         default -> { }
      }
   }

   private void onWorld(String m) {
      switch (m) {
         case HYPIXEL -> {
            this.hypWatchdogDisabled = false;
            this.hypStuckOnAir = false;
            this.hypAirStuckTicks = 0;
            this.hypAirTicks = 0;
            this.hypSentFirstOpen = false;
            this.hypCaughtClientStatus = false;
            this.hypCaughtCloseWindow = false;
            this.hypStartDisabler = true;
         }
         case MINEPLEX -> this.mineplexDisabled = false;
         case MINELAND -> this.minelandRespawnTimes = 0;
         default -> { }
      }
   }

   // ===================== Hypixel =====================

   private void hypixelOnUpdate() {
      var player = mc.player;
      if (player == null) return;

      if (this.hypixelMotion.getValue()) {
         if (!this.hypWatchdogDisabled) {
            if (player.onGround()) this.hypAirTicks = 0;
            else this.hypAirTicks++;
            if (this.hypStuckOnAir && this.hypAirTicks >= 9) {
               var dm = player.getDeltaMovement();
               player.setDeltaMovement(0, dm.y, 0);
            }
         }
      }

      if (this.hypixelInventoryMove.getValue()) {
         if (isInventoryMovePausedForChest()) {
            resetHypixelInvMoveState();
            return;
         }
         this.hypCaughtClientStatus = false;
         this.hypCaughtCloseWindow = false;
         if (mc.gui.screen() instanceof InventoryScreen) {
            if (!this.hypSentFirstOpen) {
               mc.getConnection().send(new ServerboundContainerClosePacket(0));
               this.hypSentFirstOpen = true;
            }
            int modulus = player.hasEffect(MobEffects.SPEED) ? 3 : 4;
            if (player.tickCount % modulus == 0) {
               mc.getConnection().send(new ServerboundContainerClosePacket(0));
            } else if (player.tickCount % modulus == 1) {
               mc.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
            }
         } else {
            this.hypSentFirstOpen = false;
         }
      }
   }

   private void hypixelOnMotion(EventPreMotion event) {
      var player = mc.player;
      if (player == null) return;

      if (this.hypixelMotion.getValue()) {
         if (this.hypStartDisabler && player.getInventory().getItem(8).getItem() == Items.NETHER_STAR) {
            this.hypWatchdogDisabled = false;
            this.hypStartDisabler = false;
            return;
         }
         if (!this.hypWatchdogDisabled) {
            if (this.hypStartDisabler) {
               if (player.onGround()) {
                  player.input.makeJump();
                  this.hypStuckOnAir = true;
               } else if (this.hypAirStuckTicks > 1) {
                  this.hypStartDisabler = false;
               }
            }
            if (this.hypStuckOnAir && this.hypAirTicks >= 9) {
               if (this.hypAirTicks % 2 == 0) event.setZ(event.getZ() + 0.095);
               var dm = player.getDeltaMovement();
               player.setDeltaMovement(dm.x, 0, dm.z);
            }
         }
      }
   }

   private void hypixelOnReceivedPacket(EventPacketReceive receiving) {
      var player = mc.player;
      if (player == null) return;
      Packet<?> packet = receiving.getPacket();

      if (this.hypixelInventoryMove.getValue() && !isInventoryMovePausedForChest()) {
         if (packet instanceof ServerboundClientCommandPacket) {
            if (this.hypCaughtClientStatus) receiving.setCancelled(true);
            this.hypCaughtClientStatus = true;
         }
         if (packet instanceof ServerboundContainerClosePacket) {
            if (this.hypCaughtCloseWindow) receiving.setCancelled(true);
            this.hypCaughtCloseWindow = true;
         }
      }

      if (this.hypixelMotion.getValue()) {
         if (!this.hypWatchdogDisabled && this.hypStuckOnAir && packet instanceof ClientboundPlayerPositionPacket) {
            this.hypAirStuckTicks++;
            if (this.hypAirStuckTicks >= 25) {
               this.hypAirStuckTicks = 0;
               this.hypAirTicks = 0;
               this.hypStuckOnAir = false;
               this.hypWatchdogDisabled = true;
            }
         }
      }
   }

   private boolean isInventoryMovePausedForChest() {
      if (!this.mode.is(HYPIXEL) || !this.hypixelInventoryMove.getValue() || mc.player == null || mc.level == null)
         return false;
      HitResult hitResult = mc.player.pick(5.0D, 0.0F, false);
      if (hitResult.getType() != HitResult.Type.BLOCK) return false;
      BlockHitResult blockHitResult = (BlockHitResult) hitResult;
      return mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock() instanceof ChestBlock;
   }

   private void resetHypixelInvMoveState() {
      this.hypSentFirstOpen = false;
      this.hypCaughtClientStatus = false;
      this.hypCaughtCloseWindow = false;
   }

   // ===================== Mineplex =====================

   private void mineplexOnSendPacket(EventPacketSend sending) {
      if (mc.player == null) return;
      Packet<?> packet = sending.getPacket();
      if (packet instanceof ServerboundPlayerAbilitiesPacket abilities && abilities.isFlying()) {
         sending.setCancelled(true);
         this.mineplexDisabled = true;
      }
   }

   // ===================== Cubecraft Bedrock =====================

   private void cubecraftOnSendPacket(EventPacketSend sending) {
      if (mc.player == null) return;
      Packet<?> packet = sending.getPacket();
      if (packet instanceof ServerboundKeepAlivePacket keepAlive) {
         if (keepAlive.getId() == 999) {
            sending.setCancelled(true);
            mc.getConnection().send(new ServerboundPongPacket(-1));
         }
      }
   }

   // ===================== Mineland =====================

   private void minelandOnSendPacket(EventPacketSend sending) {
      if (mc.player == null) return;
      if (this.minelandRespawnTimes == 3) {
         this.minelandRespawnTimes++;
      }
      Packet<?> packet = sending.getPacket();
      if (this.minelandRespawnTimes == 4 && packet instanceof ServerboundPongPacket) {
         sending.setCancelled(true);
      }
   }

   private void minelandOnReceivedPacket(EventPacketReceive receiving) {
      var player = mc.player;
      if (player == null || player.tickCount < 50) return;
      Packet<?> packet = receiving.getPacket();
      if (packet instanceof ClientboundRespawnPacket && this.minelandRespawnTimes > 3) {
         this.minelandRespawnTimes++;
      }
   }

   // ===================== Miniblox =====================

   private void minibloxOnSendPacket(EventPacketSend sending) {
      if (mc.player == null) return;
      Packet<?> packet = sending.getPacket();
      if (packet instanceof ServerboundPongPacket pong) {
         if (!this.minibloxResending) {
            this.minibloxResending = true;
            mc.getConnection().send(new ServerboundPongPacket(Math.abs(pong.getId())));
            this.minibloxResending = false;
         }
      }
   }

   // ===================== Verus =====================

   private void verusOnSendPacket(EventPacketSend sending) {
      if (mc.player == null) return;
      Packet<?> packet = sending.getPacket();
      if (packet instanceof ServerboundKeepAlivePacket keepAlive) {
         // Krs mutated the final id field in place; here we cancel and resend a
         // modified copy, guarding against the re-entrant EventPacketSend dispatch.
         if (!this.verusResending) {
            long newId = keepAlive.getId() - ThreadLocalRandom.current().nextInt(1, 2147483647);
            this.verusResending = true;
            sending.setCancelled(true);
            mc.getConnection().send(new ServerboundKeepAlivePacket(newId));
            this.verusResending = false;
         }
      }
      if (packet instanceof ServerboundMovePlayerPacket) {
         mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, false)));
      }
   }

   // ===================== Vulcan Scaffold =====================

   private void vulcanOnMotion() {
      var player = mc.player;
      if (player == null) return;
      if (FeatureManager.scaffold != null && FeatureManager.scaffold.isEnabled()
            && player.tickCount % 15 == 0
            && !(player.isInWater() || player.isDeadOrDying() || player.onClimbable())) {
         mc.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
         mc.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
         mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, true, false)));
         mc.getConnection().send(new ServerboundPlayerInputPacket(Input.EMPTY));
      }
   }
}
