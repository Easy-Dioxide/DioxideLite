package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.core.BlockPos;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.Parkour) and adapted. */
public class Parkour extends Feature {
   private static final String JUMP_DELAY_LABEL = "Jump Delay";
   private static final String ONLY_FORWARD_LABEL = "Only Forward";

   private final NumberSetting jumpDelay = new NumberSetting(JUMP_DELAY_LABEL, this, 30.0, 0.0, 300.0, 1.0);
   private final BooleanSetting onlyForward = new BooleanSetting(ONLY_FORWARD_LABEL, this, false);

   private long lastJumpTime;

   public Parkour() {
      super("Parkour", Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.lastJumpTime = System.currentTimeMillis();
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null || !player.onGround() || player.isShiftKeyDown()
            || player.getAbilities().flying || player.isInWater() || player.isInLava()) return;

      if (this.onlyForward.getValue() ? !this.isMovingForward() : !this.isMoving()) return;

      if (!level.getBlockState(BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ())).isAir()) return;

      if (!this.hasDelayPassed()) return;

      player.input.makeJump();
      this.lastJumpTime = System.currentTimeMillis();
   }

   private boolean isMoving() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }

   private boolean isMovingForward() {
      return mc.options.keyUp.isDown();
   }

   private boolean hasDelayPassed() {
      return System.currentTimeMillis() - this.lastJumpTime >= (long) this.jumpDelay.getValue();
   }
}
