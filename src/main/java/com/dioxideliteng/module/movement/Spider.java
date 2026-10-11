package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.TimerController;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.Spider) and adapted. */
public class Spider extends Feature {
   private static final String MODE_LABEL = "Mode";
   private static final String SPEED_LABEL = "Speed";
   private static final String TIMER_SPEED_LABEL = "Timer Speed";
   private static final String ONLY_MOVING_LABEL = "Only Moving";
   private static final String VANILLA = "Vanilla";
   private static final String TIMER = "Timer";
   private static final String PULSE = "Pulse";

   private final ModeSetting mode = new ModeSetting(MODE_LABEL, this, VANILLA, new String[]{VANILLA, TIMER, PULSE});
   private final NumberSetting speed = new NumberSetting(SPEED_LABEL, this, 0.32, 0.1, 1.0, 0.01);
   private final NumberSetting timerSpeed = new NumberSetting(TIMER_SPEED_LABEL, this, 1.7, 1.1, 4.0, 0.1);
   private final BooleanSetting onlyMoving = new BooleanSetting(ONLY_MOVING_LABEL, this, true);

   private boolean timered;
   private int pulseTicks;

   public Spider() {
      super("Spider", Category.MOVEMENT);
      this.speed.setVisible(() -> !this.mode.is(TIMER));
      this.timerSpeed.setVisible(() -> this.mode.is(TIMER));
   }

   @Override
   public void onDisable() {
      this.resetTimer();
      this.pulseTicks = 0;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());
      var player = mc.player;
      if (player == null) {
         this.resetTimer();
         this.pulseTicks = 0;
         return;
      }

      boolean climbing = player.horizontalCollision && !player.onClimbable()
            && (!this.onlyMoving.getValue() || this.isMoving());
      if (!climbing) {
         this.resetTimer();
         this.pulseTicks = 0;
         return;
      }

      if (this.mode.is(TIMER)) {
         TimerController.setMultiplier((float) this.timerSpeed.getValue());
         this.timered = true;
         if (player.getDeltaMovement().y < 0.0)
            this.setVelocityY(0.0);
      } else if (this.mode.is(PULSE)) {
         this.pulseTicks++;
         if (this.pulseTicks >= 3) {
            this.setVelocityY(this.speed.getValue());
            this.pulseTicks = 0;
         } else if (player.getDeltaMovement().y < 0.0) {
            this.setVelocityY(0.0);
         }
      } else {
         this.setVelocityY(this.speed.getValue());
      }
   }

   private void resetTimer() {
      if (this.timered) {
         TimerController.setMultiplier(1.0F);
         this.timered = false;
      }
   }

   private void setVelocityY(double y) {
      var dm = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(dm.x, y, dm.z);
   }

   private boolean isMoving() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }
}
