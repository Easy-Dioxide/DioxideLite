package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.InputUtil;
import com.dioxideliteng.util.krs.MovementUtil;
import net.minecraft.client.player.ClientInput;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.Velocity).
 * Notable adaptations vs the original:
 *  - ModuleManager.getModuleState(FlyModule/SpeedModule) resolved through
 *    instanceof lookups over the client movement.Flight / movement.Speed
 *    modules (same pattern as Scaffold2 / TargetStrafe).
 *  - The static input hook is called from mixins.MixinKeyboardInput at tick TAIL
 *    (Krs parity: KeyboardInputMixin), after the input fixes run.
 *  - Krs continuous FloatValue/IntValue mapped to client NumberSetting steps
 *    (chance 0.1, tick/hit counters 1.0); reading via getValue() (double).
 * All mode logic (Standard cancel / Hypixel NCP velocity rewrite / Jump Reset
 * chance, cooldown and fall-damage tracking) is byte-exact Krs parity.
 */
public class Velocity2 extends Feature {
   private static Velocity2 instance;

   private final ModeSetting mode = new ModeSetting("Mode", this, "Standard",
           new String[]{"Standard", "Hypixel NCP", "Jump Reset"});

   private final NumberSetting jumpResetChance = new NumberSetting("Jump Reset Chance", this, 100.0, 0.0, 100.0, 0.1);

   private final ModeSetting jumpResetCooldown = new ModeSetting("Jump Reset Cooldown", this, "Delay",
           new String[]{"Delay", "Received Hits", "None"});

   private final NumberSetting jumpResetTicksUntilJump = new NumberSetting("Jump Reset Until Jump", this, 2.0, 0.0, 20.0, 1.0);

   private final NumberSetting jumpResetHitsUntilJump = new NumberSetting("Jump Reset Hits Until Jump", this, 2.0, 0.0, 10.0, 1.0);

   private int jumpResetLimitUntilJump = 0;
   private boolean jumpResetFallDamage = false;

   public Velocity2() {
      super("Velocity2", Category.COMBAT);
      instance = this;
      this.jumpResetChance.setVisible(this::isJumpResetMode);
      this.jumpResetCooldown.setVisible(this::isJumpResetMode);
      this.jumpResetTicksUntilJump.setVisible(() -> this.isJumpResetMode() && this.jumpResetCooldown.is("Delay"));
      this.jumpResetHitsUntilJump.setVisible(() -> this.isJumpResetMode() && this.jumpResetCooldown.is("Received Hits"));
   }

   @Override
   public void onDisable() {
      resetJumpReset();
   }

   @Override
   public void onEnable() {
      resetJumpReset();
   }

   public static void hookJumpResetInput(ClientInput input) {
      Velocity2 velocity = instance;
      if (velocity == null || !velocity.isEnabled())
         return;

      velocity.handleJumpResetInput(input);
   }

   private void handleJumpResetInput(ClientInput input) {
      var player = mc.player;
      if (input == null || player == null || player.input != input || !isJumpResetMode()) return;

      if (player.hurtTime != 9
            || !player.onGround()
            || !player.isSprinting()
            || jumpResetFallDamage
            || !isJumpResetCooldownOver()
            || !rollJumpResetChance()) {
         updateJumpResetLimit();
         return;
      }

      InputUtil.setJumping(input, true);
      jumpResetLimitUntilJump = 0;
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventPacketReceive receiving) {
         onReceivedPacket(receiving);
      }
   }

   private void onReceivedPacket(EventPacketReceive event) {
      var player = mc.player;
      if (player == null) return;

      var packet = event.getPacket();
      switch (mode.getValue().toLowerCase(Locale.ROOT)) {
         case "standard" -> {
            if (packet instanceof ClientboundSetEntityMotionPacket motionPacket && motionPacket.id() == player.getId())
               event.setCancelled(true);
         }
         case "hypixel ncp" -> {
            if (!isFlyEnabled() && packet instanceof ClientboundSetEntityMotionPacket motionPacket && motionPacket.id() == player.getId()) {
               if (!isSpeedEnabled() || MovementUtil.fallTicks >= 10 || player.onGround())
                  MovementUtil.setVelocityY(motionPacket.movement().y / 8000.0);
               event.setCancelled(true);
            }
         }
         case "jump reset" -> updateJumpResetFallDamage(packet, player.getId());
      }
   }

   private boolean isJumpResetMode() {
      return mode.is("Jump Reset");
   }

   private void resetJumpReset() {
      jumpResetLimitUntilJump = 0;
      jumpResetFallDamage = false;
   }

   private boolean rollJumpResetChance() {
      float chance = (float) jumpResetChance.getValue();
      return chance >= 100f || chance > 0f && ThreadLocalRandom.current().nextFloat() * 100.0f < chance;
   }

   private boolean isJumpResetCooldownOver() {
      if (jumpResetCooldown.is("Received Hits"))
         return jumpResetLimitUntilJump >= (int) jumpResetHitsUntilJump.getValue();

      if (jumpResetCooldown.is("Delay"))
         return jumpResetLimitUntilJump >= (int) jumpResetTicksUntilJump.getValue();

      return true;
   }

   private void updateJumpResetLimit() {
      var player = mc.player;
      if (player == null) return;

      if (jumpResetCooldown.is("Received Hits")) {
         if (player.hurtTime == 9)
            jumpResetLimitUntilJump++;
         return;
      }

      if (jumpResetCooldown.is("Delay"))
         jumpResetLimitUntilJump++;
   }

   private void updateJumpResetFallDamage(Object packet, int playerId) {
      if (packet instanceof ClientboundSetEntityMotionPacket motionPacket && motionPacket.id() == playerId) {
         var motion = motionPacket.movement();
         jumpResetFallDamage = motion.x == 0.0 && motion.z == 0.0 && motion.y < 0.0;
      }
   }

   private static boolean isFlyEnabled() {
      var modules = FeatureManager.getModules();
      if (modules == null) return false;
      for (var feature : modules) {
         if (feature instanceof com.dioxideliteng.module.movement.Flight)
            return feature.isEnabled();
      }
      return false;
   }

   private static boolean isSpeedEnabled() {
      var modules = FeatureManager.getModules();
      if (modules == null) return false;
      for (var feature : modules) {
         if (feature instanceof com.dioxideliteng.module.movement.Speed)
            return feature.isEnabled();
      }
      return false;
   }
}
