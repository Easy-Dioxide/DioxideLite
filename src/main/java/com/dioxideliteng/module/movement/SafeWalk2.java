package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.util.krs.MovementUtil;

/**
 * Port of Krs SafeWalk (com.instrumentalist.krs.hacks.features.movement.SafeWalk) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs BlockEdgeEvent -> {@link Events#BLOCK_EDGE}; cancelling the event keeps the
 *    player on the block surface (dispatch hook already present in MixinPlayer).
 */
public class SafeWalk2 extends Feature {
   private final BooleanSetting onlyMoving = new BooleanSetting("Only Moving", this, false);
   private final BooleanSetting ignoreSneak = new BooleanSetting("Ignore Sneak", this, false);

   public SafeWalk2() {
      super("SafeWalk2", Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.BLOCK_EDGE) return;

      var player = mc.player;
      if (player == null || player.isFallFlying() || player.getAbilities().flying)
         return;

      if (this.onlyMoving.getValue() && !MovementUtil.isMoving())
         return;

      if (!this.ignoreSneak.getValue() && player.isShiftKeyDown())
         return;

      Events.BLOCK_EDGE.setCancelled(true);
   }
}
