package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.player.Player;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.Velocity) and adapted. */
public class Velocity2 extends Feature {
   private static final String STANDARD_LABEL = "Standard";
   private static final String HYPIXEL_NCP_LABEL = "Hypixel NCP";
   private static final String JUMP_RESET_LABEL = "Jump Reset";

   private final ModeSetting mode = new ModeSetting("Mode", this, STANDARD_LABEL,
         new String[]{STANDARD_LABEL, HYPIXEL_NCP_LABEL, JUMP_RESET_LABEL});
   private final NumberSetting jumpResetChance = new NumberSetting("Jump Reset Chance", this, 100.0, 0.0, 100.0, 1.0);
   private final ModeSetting jumpResetCooldown = new ModeSetting("Jump Reset Cooldown", this, "Delay",
         new String[]{"Delay", "Received Hits", "None"});
   private final NumberSetting jumpResetTicksUntilJump = new NumberSetting("Jump Reset Until Jump", this, 2.0, 0.0, 20.0, 1.0);
   private final NumberSetting jumpResetHitsUntilJump = new NumberSetting("Jump Reset Hits Until Jump", this, 2.0, 0.0, 10.0, 1.0);

   private int jumpResetLimit;
   private boolean jumpResetFallDamage;

   public Velocity2() {
      super("Velocity2", Category.COMBAT);
      this.jumpResetChance.setVisible(this::isJumpResetMode);
      this.jumpResetCooldown.setVisible(this::isJumpResetMode);
      this.jumpResetTicksUntilJump.setVisible(() -> isJumpResetMode() && this.jumpResetCooldown.is("Delay"));
      this.jumpResetHitsUntilJump.setVisible(() -> isJumpResetMode() && this.jumpResetCooldown.is("Received Hits"));
   }

   @Override
   public void onEnable() {
      resetJumpReset();
   }

   @Override
   public void onDisable() {
      resetJumpReset();
   }

   private boolean isJumpResetMode() {
      return this.mode.is(JUMP_RESET_LABEL);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         this.setSuffix(this.mode.getValue());
         return;
      }
      if (event instanceof EventPacketReceive receiving && !receiving.isCancelled()) {
         onPacket(receiving);
      } else if (event == Events.MOVE_INPUT) {
         handleJumpResetInput();
      }
   }

   private void onPacket(EventPacketReceive receiving) {
      var player = mc.player;
      if (player == null) return;
      var packet = receiving.getPacket();
      String current = this.mode.getValue().toLowerCase(Locale.ROOT);
      if (current.equals(STANDARD_LABEL.toLowerCase(Locale.ROOT))) {
         if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.id() == player.getId()) {
            receiving.setCancelled(true);
         }
      } else if (current.equals(HYPIXEL_NCP_LABEL.toLowerCase(Locale.ROOT))) {
         if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.id() == player.getId()) {
            double y = motion.movement().y;
            var dm = player.getDeltaMovement();
            player.setDeltaMovement(dm.x, y, dm.z);
            receiving.setCancelled(true);
         }
      } else if (current.equals(JUMP_RESET_LABEL.toLowerCase(Locale.ROOT))) {
         updateJumpResetFallDamage(packet, player.getId());
      }
   }

   private void handleJumpResetInput() {
      var player = mc.player;
      if (player == null || !isJumpResetMode()) return;
      if (player.hurtTime != 9 || !player.onGround() || !player.isSprinting()
            || this.jumpResetFallDamage || !isJumpResetCooldownOver() || !rollChance()) {
         updateJumpResetLimit(player);
         return;
      }
      Events.MOVE_INPUT.setJump(true);
      this.jumpResetLimit = 0;
   }

   private boolean rollChance() {
      double chance = this.jumpResetChance.getValue();
      return chance >= 100.0 || chance > 0.0 && ThreadLocalRandom.current().nextDouble() * 100.0 < chance;
   }

   private boolean isJumpResetCooldownOver() {
      String cooldown = this.jumpResetCooldown.getValue();
      if (cooldown.equalsIgnoreCase("Received Hits")) {
         return this.jumpResetLimit >= (int) this.jumpResetHitsUntilJump.getValue();
      }
      if (cooldown.equalsIgnoreCase("Delay")) {
         return this.jumpResetLimit >= (int) this.jumpResetTicksUntilJump.getValue();
      }
      return true;
   }

   private void updateJumpResetLimit(Player player) {
      String cooldown = this.jumpResetCooldown.getValue();
      if (cooldown.equalsIgnoreCase("Received Hits")) {
         if (player.hurtTime == 9) this.jumpResetLimit++;
         return;
      }
      if (cooldown.equalsIgnoreCase("Delay")) this.jumpResetLimit++;
   }

   private void updateJumpResetFallDamage(Object packet, int playerId) {
      if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.id() == playerId) {
         var movement = motion.movement();
         this.jumpResetFallDamage = movement.x == 0.0 && movement.z == 0.0 && movement.y < 0.0;
      }
   }

   private void resetJumpReset() {
      this.jumpResetLimit = 0;
      this.jumpResetFallDamage = false;
   }
}
