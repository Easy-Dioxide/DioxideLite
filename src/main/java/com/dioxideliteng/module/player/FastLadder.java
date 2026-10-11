package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.TimerController;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.FastLadder) and adapted.
 *  Krs TimerUtil maps to the client's TimerController; MovementUtil.setVelocityY is
 *  inlined as a direct delta-movement override. */
public class FastLadder extends Feature {
   private static final String VANILLA = "Vanilla";
   private static final String TIMER = "Timer";

   private final ModeSetting mode = new ModeSetting("Mode", this, VANILLA, new String[]{VANILLA, TIMER});
   private final BooleanSetting resetY = new BooleanSetting("ResetY", this, true);
   private final NumberSetting speed = new NumberSetting("Speed", this, 0.4, 0.2, 1.0, 0.1);
   private final NumberSetting timerSpeed = new NumberSetting("Timer Speed", this, 2.0, 1.1, 3.0, 0.1);

   private boolean wasClimbing = false;

   public FastLadder() {
      super("FastLadder", Category.PLAYER);
      this.resetY.setVisible(() -> this.mode.is(VANILLA));
      this.speed.setVisible(() -> this.mode.is(VANILLA));
      this.timerSpeed.setVisible(() -> this.mode.is(TIMER));
   }

   @Override
   public void onDisable() {
      if (mc.player == null) return;
      this.wasClimbing = false;
      TimerController.reset();
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());
      var player = mc.player;
      if (player == null) return;

      boolean climbing = player.onClimbable() && (player.horizontalCollision || mc.options.keyJump.isDown());
      if (this.mode.is(VANILLA)) {
         if (climbing) {
            setVelocityY(this.speed.getValue());
            this.wasClimbing = true;
         } else if (this.wasClimbing) {
            if (this.resetY.getValue()) setVelocityY(-0.1);
            this.wasClimbing = false;
         }
      } else if (this.mode.is(TIMER)) {
         if (climbing) {
            TimerController.setMultiplier((float) this.timerSpeed.getValue());
            this.wasClimbing = true;
         } else if (this.wasClimbing) {
            TimerController.reset();
            this.wasClimbing = false;
         }
      }
   }

   private static void setVelocityY(double y) {
      var player = mc.player;
      if (player == null) return;
      var dm = player.getDeltaMovement();
      player.setDeltaMovement(dm.x, y, dm.z);
   }
}
