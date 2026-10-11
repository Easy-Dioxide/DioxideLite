package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventAttack;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.WTap) and adapted to use Events.MOVE_INPUT. */
public class WTap extends Feature {
   private final NumberSetting delay = new NumberSetting("Delay", this, 5.5, 0.0, 10.0, 0.1);
   private final NumberSetting duration = new NumberSetting("Duration", this, 1.5, 1.0, 5.0, 0.1);

   private boolean active;
   private boolean stopForward;
   private long lastAttackMs;
   private long phaseEndMs;

   public WTap() {
      super("WTap", Category.COMBAT);
   }

   @Override
   public void onEnable() {
      reset();
   }

   @Override
   public void onDisable() {
      reset();
   }

   private void reset() {
      this.active = false;
      this.stopForward = false;
      this.phaseEndMs = 0L;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ATTACK) {
         triggerAttack((EventAttack) event);
      } else if (event == Events.MOVE_INPUT) {
         handleInput();
      }
   }

   private void triggerAttack(EventAttack attackEvent) {
      var player = mc.player;
      if (player == null || attackEvent.isCancelled() || this.active) return;
      long now = System.currentTimeMillis();
      if (now - this.lastAttackMs < 500L) return;
      if (!player.isSprinting()) return;

      this.lastAttackMs = now;
      this.active = true;
      this.stopForward = false;
      this.phaseEndMs = now + (long) (this.delay.getValue() * 50.0);
   }

   private void handleInput() {
      var player = mc.player;
      if (player == null || !this.active) return;

      if (!this.stopForward && !canTrigger(player)) {
         reset();
         return;
      }

      long now = System.currentTimeMillis();
      if (now < this.phaseEndMs && !this.stopForward) {
         return;
      }

      if (!this.stopForward) {
         this.stopForward = true;
         this.phaseEndMs = now + (long) (this.duration.getValue() * 50.0);
      }

      if (now < this.phaseEndMs) {
         Events.MOVE_INPUT.setForward(false);
         Events.MOVE_INPUT.setBackward(false);
         Events.MOVE_INPUT.setSprint(false);
         if (player.isSprinting()) player.setSprinting(false);
      } else {
         reset();
      }
   }

   private boolean canTrigger(Player player) {
      return mc.options.keyUp.isDown()
            && !player.horizontalCollision
            && (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().flying)
            && (player.isSprinting()
                  || !player.isUsingItem() && !player.hasEffect(MobEffects.BLINDNESS) && mc.options.keySprint.isDown());
   }
}
