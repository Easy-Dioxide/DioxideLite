package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import mixins.MultiPlayerGameModeAccessor;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.FastBreak) and adapted.
 *  Krs exposes block-break speed via a static mixin hook (hookFastBreak); this port uses
 *  the client's MultiPlayerGameModeAccessor to multiply the destroy progress directly,
 *  mirroring the client's FastMine mechanism. */
public class FastBreak extends Feature {

   private final NumberSetting multiplier = new NumberSetting("Multiplier", this, 1.4, 0.1, 3.0, 0.1);

   public FastBreak() {
      super("FastBreak", Category.PLAYER);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.ROTATION) return;
      this.setSuffix("x" + this.multiplier.getValue());
      if (mc.gameMode == null) return;

      MultiPlayerGameModeAccessor accessor = (MultiPlayerGameModeAccessor) mc.gameMode;
      float destroyProgress = accessor.getDestroyProgress();
      if (destroyProgress > 0.0F && destroyProgress < 0.99F) {
         accessor.setDestroyProgress((float) (destroyProgress * this.multiplier.getValue()));
      }
   }
}
