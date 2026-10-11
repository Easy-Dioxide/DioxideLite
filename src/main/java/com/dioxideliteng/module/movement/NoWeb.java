package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.NoWeb) and adapted. */
public class NoWeb extends Feature {
   private static final String SPEED_LABEL = "Speed";
   private static final String VERTICAL_LABEL = "Vertical";

   private final NumberSetting speed = new NumberSetting(SPEED_LABEL, this, 0.35, 0.05, 1.0, 0.01);
   private final BooleanSetting vertical = new BooleanSetting(VERTICAL_LABEL, this, true);

   public NoWeb() {
      super("No Web", Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      var player = mc.player;
      if (player == null || !this.isInWeb()) return;

      if (this.isMoving())
         this.setSpeed(Math.max(this.getSpeed(), this.speed.getValue()));

      if (this.vertical.getValue()) {
         if (mc.options.keyJump.isDown())
            this.setVelocityY(this.speed.getValue());
         else if (mc.options.keyShift.isDown())
            this.setVelocityY(-this.speed.getValue());
      }
   }

   private boolean isMoving() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }

   private double getSpeed() {
      var dm = mc.player.getDeltaMovement();
      return Math.sqrt(dm.x * dm.x + dm.z * dm.z);
   }

   private void setSpeed(double target) {
      var dm = mc.player.getDeltaMovement();
      double current = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
      if (current > 1.0E-6) {
         double factor = target / current;
         mc.player.setDeltaMovement(dm.x * factor, dm.y, dm.z * factor);
      }
   }

   private void setVelocityY(double y) {
      var dm = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(dm.x, y, dm.z);
   }

   private boolean isInWeb() {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return false;
      AABB box = player.getBoundingBox();
      for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
         for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
            for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
               if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.COBWEB)) return true;
            }
         }
      }
      return false;
   }
}
