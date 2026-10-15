package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.MovementUtil;
import net.minecraft.core.BlockPos;

/**
 * Port of Krs Parkour (com.instrumentalist.krs.hacks.features.movement.Parkour) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE}.
 */
public class Parkour2 extends Feature {
   private final NumberSetting jumpDelay = new NumberSetting("Jump Delay", this, 30, 0, 300, 1);
   private final BooleanSetting onlyForward = new BooleanSetting("Only Forward", this, false);

   private final MSTimer jumpTimer = new MSTimer();

   public Parkour2() {
      super("Parkour2", Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      jumpTimer.reset();
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;

      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null || !player.onGround() || player.isShiftKeyDown() || player.getAbilities().flying || player.isInWater() || player.isInLava())
         return;

      if (this.onlyForward.getValue() ? !MovementUtil.isMovingForward() : !MovementUtil.isMoving())
         return;

      if (!level.getBlockState(BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ())).isAir())
         return;

      if (!jumpTimer.hasTimePassed((long) this.jumpDelay.getValue()))
         return;

      player.input.makeJump();
      jumpTimer.reset();
   }
}
