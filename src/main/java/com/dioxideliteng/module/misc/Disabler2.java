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
import com.dioxideliteng.module.player.InvManager2;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.RandomUtil;
import com.dioxideliteng.util.krs.RotationManager;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.disabler.DisablerModule +
 * DisablerModeManager + DisablerEvent + the 7 disabler features) and adapted to the
 * DioxideLiteNG event system. The Krs DisablerModeManager/DisablerEvent dispatch pattern is
 * inlined as mode dispatch inside a single Feature, matching the b4 sample conventions:
 *  - Krs UpdateEvent -> Events.UPDATE; Krs MotionEvent -> EventPreMotion (the sendPosition-stage
 *    event, the exact analog of Krs MotionEvent: it is the only event exposing the outgoing
 *    packet's x/y/z, which the Hypixel NCP motion bypass mutates via event.z);
 *  - Krs TickEvent -> Events.TICK (mode-switch detection runs here, as in Krs
 *    DisablerModeManager.onTick); Krs SendPacketEvent/ReceivedPacketEvent -> EventPacketSend/
 *    EventPacketReceive; Krs WorldEvent -> Events.WORLD;
 *  - Krs ModuleManager.getModuleState(Scaffold.class) -> FeatureManager.scaffold2 (Scaffold2 is
 *    the faithful port of the Krs Scaffold the Vulcan Scaffold bypass was written against);
 *  - Krs Client.rotationManager -> RotationManager.INSTANCE; Client.mc -> mc;
 *  - PacketUtil.sendPacketAsSilent semantics are preserved: silent packets skip
 *    EventPacketSend dispatch (MixinConnection consumes the marker before the event), exactly
 *    like Krs where Connection.send(Packet, listener) is the WrapMethod boundary and the
 *    1-arg silent path never reaches SendPacketEvent;
 *  - Krs mutated ServerboundKeepAlivePacket#id in place; EventPacketSend exposes no mutable
 *    packet field and 26.3 keeps the id field private final, so the Verus bypass cancels the
 *    original and resends a modified copy silently (same server-visible effect; the silent
 *    send also prevents the resend from re-entering this hook);
 *  - The Krs "Hypixel NCP Sprint" sub-feature wrote MotionEvent#yaw; EventPreMotion has no yaw
 *    setter, but the outgoing packet yaw is read from Events.ROTATION after PRE_MOTION dispatch
 *    (MixinLocalPlayer#preMotion), so the spoofed direction is applied via
 *    Events.ROTATION.setYaw with the same !rotationManager.isRotating() guard as Krs;
 *  - Notifications/chat prints (Client.notificationManager / ChatUtil.printChat) are dropped
 *    (no client equivalent in this port), and the empty onAttack forward of every mode is
 *    omitted.
 */
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
   private final BooleanSetting hypixelSprint = new BooleanSetting("Hypixel NCP Sprint", this, false);

   // Hypixel state (Krs HypixelDisabler statics)
   private boolean hypWatchdogDisabled;
   private boolean hypStartDisabler;
   private boolean hypStuckOnAir;
   private int hypAirStickTicks;
   private int hypAirTicks;
   private boolean hypSentFirstOpen;
   private boolean hypCaughtClientStatus;
   private boolean hypCaughtCloseWindow;

   // Mineplex state (Krs MineplexDisabler.disabled)
   private boolean mineplexDisabled;

   // Mineland state (Krs MinelandDisabler.respawnTimes)
   private long minelandRespawnTimes;

   // Verus state (Krs VerusDisabler.prevTran / godTick)
   private int verusPrevTran;
   private int verusGodTick;

   private String activeMode = null;

   public Disabler2() {
      super("Disabler2", Category.MISC);
      this.hypixelInventoryMove.setVisible(() -> this.mode.is(HYPIXEL));
      this.hypixelMotion.setVisible(() -> this.mode.is(HYPIXEL));
      this.hypixelSprint.setVisible(() -> this.mode.is(HYPIXEL));
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

   // ===================== Krs DisablerModule.onEnableFunctions / onDisableFunctions =====================

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
            this.hypAirStickTicks = 0;
            this.hypAirTicks = 0;
            this.hypSentFirstOpen = false;
            this.hypCaughtClientStatus = false;
            this.hypCaughtCloseWindow = false;
         }
         case VERUS -> {
            this.verusPrevTran = 0;
            this.verusGodTick = 0;
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
         // Krs parity: DisablerModeManager.onTick detects a mode switch, resets the old
         // mode, rebuilds the current mode and re-runs the enable functions.
         String current = this.mode.getValue();
         this.setSuffix(current);
         if (!current.equals(this.activeMode)) {
            resetMode(this.activeMode);
            this.activeMode = current;
            enableMode(current);
         }
         return;
      }
      if (event == Events.UPDATE) {
         onUpdate(this.mode.getValue());
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
            this.hypAirStickTicks = 0;
            this.hypAirTicks = 0;
            this.hypSentFirstOpen = false;
            this.hypCaughtClientStatus = false;
            this.hypCaughtCloseWindow = false;
            this.hypStartDisabler = true;
         }
         case MINEPLEX -> this.mineplexDisabled = false;
         case MINELAND -> this.minelandRespawnTimes = 0;
         case VERUS -> {
            this.verusPrevTran = 0;
            this.verusGodTick = 0;
         }
         default -> { }
      }
   }

   // ===================== Hypixel (Krs HypixelDisabler) =====================

   private void hypixelOnUpdate() {
      var player = mc.player;
      if (player == null) return;

      if (this.hypixelMotion.getValue()) {
         if (!this.hypWatchdogDisabled) {
            if (player.onGround()) this.hypAirTicks = 0;
            else this.hypAirTicks++;

            if (this.hypStuckOnAir && this.hypAirTicks >= 9)
               MovementUtil.stopMoving();
         }
      }

      if (this.hypixelInventoryMove.getValue()) {
         if (isInventoryMovePausedForChest()) {
            resetHypixelInvMoveState();
            return;
         }

         this.hypCaughtClientStatus = false;
         this.hypCaughtCloseWindow = false;

         if (mc.gui.screen() instanceof InventoryScreen || isInvManagerCleaning()) {
            if (!this.hypSentFirstOpen) {
               PacketUtil.sendPacketAsSilent(new ServerboundContainerClosePacket(0));
               this.hypSentFirstOpen = true;
            }

            int modulus = player.hasEffect(MobEffects.SPEED) ? 3 : 4;
            if (player.tickCount % modulus == 0) {
               PacketUtil.sendPacketAsSilent(new ServerboundContainerClosePacket(0));
            } else if (player.tickCount % modulus == 1) {
               PacketUtil.sendPacket(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
            }
         } else this.hypSentFirstOpen = false;
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
               } else if (this.hypAirStickTicks > 1) {
                  this.hypStartDisabler = false;
               }
            }
            if (this.hypStuckOnAir && this.hypAirTicks >= 9) {
               if (this.hypAirTicks % 2 == 0) event.setZ(event.getZ() + 0.095);
               MovementUtil.setVelocityY(0.0);
            }
         }
      }

      if (this.hypixelSprint.getValue()
            && !RotationManager.INSTANCE.isRotating()
            && player.isSprinting()
            && MovementUtil.isMoving()
            && player.input.getMoveVector().y < 0f) {
         // Krs parity: MotionEvent#yaw = MovementUtil.getPlayerDirection(). The outgoing
         // packet yaw is read from Events.ROTATION after PRE_MOTION dispatch, so writing
         // the spoofed direction there transmits it to the server this tick.
         Events.ROTATION.setYaw(MovementUtil.getPlayerDirection());
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
            this.hypAirStickTicks++;
            if (this.hypAirStickTicks >= 25) {
               this.hypAirStickTicks = 0;
               this.hypAirTicks = 0;
               this.hypStuckOnAir = false;
               this.hypWatchdogDisabled = true;
            }
         }
      }
   }

   // ===================== Krs cross-module state accessors =====================

   /**
    * Krs parity: HypixelDisabler#onUpdate opens the inventory-move bypass window while
    * {@code ModuleManager.getModuleState(InvManager.class) && InvManager.getCleaning()}.
    * Resolves the ported InvManager2 instance the same way the other ports do.
    */
   private static boolean isInvManagerCleaning() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof InvManager2 manager)
            return manager.isEnabled() && InvManager2.getCleaning();
      }
      return false;
   }

   /** Krs parity: DisablerModule.hypixelMotion.get(). */
   public boolean isHypixelNcpMotionEnabled() {
      return this.hypixelMotion.getValue();
   }

   /** Krs parity: HypixelDisabler.watchDogDisabled. */
   public boolean isWatchdogDisabled() {
      return this.hypWatchdogDisabled;
   }

   public boolean isInventoryMovePausedForChest() {
      if (!this.mode.is(HYPIXEL)
            || !this.hypixelInventoryMove.getValue()
            || mc.player == null
            || mc.level == null)
         return false;

      HitResult hitResult = mc.player.pick(5.0D, 0.0F, false);
      if (hitResult.getType() != HitResult.Type.BLOCK)
         return false;

      BlockHitResult blockHitResult = (BlockHitResult) hitResult;
      return mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock() instanceof ChestBlock;
   }

   private void resetHypixelInvMoveState() {
      this.hypSentFirstOpen = false;
      this.hypCaughtClientStatus = false;
      this.hypCaughtCloseWindow = false;
   }

   // ===================== Mineplex (Krs MineplexDisabler) =====================

   private void mineplexOnSendPacket(EventPacketSend sending) {
      if (mc.player == null) return;
      Packet<?> packet = sending.getPacket();
      if (packet instanceof ServerboundPlayerAbilitiesPacket abilities && abilities.isFlying()) {
         sending.setCancelled(true);
         this.mineplexDisabled = true;
      }
   }

   // ===================== Cubecraft Bedrock (Krs CubecraftBedrockDisabler) =====================

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

   // ===================== Mineland (Krs MinelandDisabler) =====================

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

   // ===================== Miniblox (Krs MinibloxDisabler) =====================

   private void minibloxOnSendPacket(EventPacketSend sending) {
      if (mc.player == null) return;
      Packet<?> packet = sending.getPacket();
      if (packet instanceof ServerboundPongPacket pong) {
         // Krs parity: PacketUtil.sendPacketAsSilent(new ServerboundPongPacket(Math.abs(id))) -
         // the original pong is NOT cancelled, an extra modified copy goes out.
         PacketUtil.sendPacketAsSilent(new ServerboundPongPacket(Math.abs(pong.getId())));
      }
   }

   // ===================== Verus (Krs VerusDisabler) =====================

   private void verusOnSendPacket(EventPacketSend sending) {
      if (mc.player == null) return;
      Packet<?> packet = sending.getPacket();
      if (packet instanceof ServerboundKeepAlivePacket keepAlive) {
         // Krs mutated the final id field in place (id -= random). ServerboundKeepAlivePacket.id
         // is private final on 26.3 so the in-place mutation is impossible; the faithful
         // equivalent is to cancel the original and resend a modified copy silently, which also
         // avoids re-entering this hook (Krs would have looped on a fresh packet otherwise).
         long newId = keepAlive.getId() - RandomUtil.nextInt(1, 2147483647);
         sending.setCancelled(true);
         PacketUtil.sendPacketAsSilent(new ServerboundKeepAlivePacket(newId));
      }
      if (packet instanceof ServerboundMovePlayerPacket) {
         PacketUtil.sendPacket(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, false)));
      }
   }

   // ===================== Vulcan Scaffold (Krs VulcanScaffoldDisabler) =====================

   private void vulcanOnMotion() {
      var player = mc.player;
      if (player == null) return;
      if (FeatureManager.scaffold2 != null && FeatureManager.scaffold2.isEnabled()
            && player.tickCount % 15 == 0
            && !(player.isInWater() || player.isDeadOrDying() || player.onClimbable())) {
         PacketUtil.sendPacket(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
         PacketUtil.sendPacket(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
         PacketUtil.sendPacket(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, true, false)));
         PacketUtil.sendPacket(new ServerboundPlayerInputPacket(Input.EMPTY));
      }
   }
}
