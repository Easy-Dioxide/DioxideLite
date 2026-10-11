package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.WaterSpeed) and adapted. */
public class WaterSpeed extends Feature {
   private static final String MODE_LABEL = "Mode";
   private static final String MULTIPLIER_LABEL = "Multiplier";
   private static final String MAX_SPEED_LABEL = "Max Speed";
   private static final String LAVA_LABEL = "Lava";
   private static final String SCALE = "Scale";
   private static final String STRAFE = "Strafe";
   private static final String BOOST = "Boost";

   private final ModeSetting mode = new ModeSetting(MODE_LABEL, this, SCALE, new String[]{SCALE, STRAFE, BOOST});
   private final NumberSetting multiplier = new NumberSetting(MULTIPLIER_LABEL, this, 1.2, 1.0, 3.0, 0.1);
   private final NumberSetting maxSpeed = new NumberSetting(MAX_SPEED_LABEL, this, 0.5, 0.1, 2.0, 0.1);
   private final BooleanSetting lava = new BooleanSetting(LAVA_LABEL, this, false);

   public WaterSpeed() {
      super("Water Speed", Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());
      var player = mc.player;
      if (player == null || !this.isMoving()
            || !player.isInWater() && (!this.lava.getValue() || !player.isInLava())) return;

      if (this.mode.is(STRAFE)) {
         this.strafe(this.maxSpeed.getValue());
      } else if (this.mode.is(BOOST)) {
         this.boost((this.multiplier.getValue() - 1.0) * 0.08, this.maxSpeed.getValue());
      } else {
         this.scaleVelocityXZ(this.multiplier.getValue());
      }
      this.limitSpeed(this.maxSpeed.getValue());
   }

   private boolean isMoving() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }

   private double getSpeed() {
      var dm = mc.player.getDeltaMovement();
      return Math.sqrt(dm.x * dm.x + dm.z * dm.z);
   }

   private void strafe(double speed) {
      this.setSpeed(speed);
   }

   private void setSpeed(double target) {
      var dm = mc.player.getDeltaMovement();
      double current = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
      if (current > 1.0E-6) {
         double factor = target / current;
         mc.player.setDeltaMovement(dm.x * factor, dm.y, dm.z * factor);
      }
   }

   private void boost(double amount, double max) {
      this.setSpeed(Math.min(this.getSpeed() + amount, max));
   }

   private void scaleVelocityXZ(double multiplier) {
      var dm = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(dm.x * multiplier, dm.y, dm.z * multiplier);
   }

   private void limitSpeed(double max) {
      var dm = mc.player.getDeltaMovement();
      double current = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
      if (current > max) {
         double factor = max / current;
         mc.player.setDeltaMovement(dm.x * factor, dm.y, dm.z * factor);
      }
   }
}
