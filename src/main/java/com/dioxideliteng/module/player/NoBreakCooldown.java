package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import mixins.MultiPlayerGameModeAccessor;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.level.NoBreakCooldown).
 *  In Krs this is a flag module whose behavior lives in a mixin; here the behavior is
 *  implemented directly via the client's existing MultiPlayerGameModeAccessor, mirroring
 *  FastMine's "Remove Delay" semantics (destroyDelay = 0 each tick). */
public class NoBreakCooldown extends Feature {

   public NoBreakCooldown() {
      super("NoBreakCooldown", Category.PLAYER);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.ROTATION) return;
      if (mc.gameMode == null) return;
      ((MultiPlayerGameModeAccessor) mc.gameMode).setDestroyDelay(0);
   }
}
