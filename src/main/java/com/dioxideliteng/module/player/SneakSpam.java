package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.mojang.blaze3d.platform.InputConstants;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.SneakSpam) and adapted. */
public class SneakSpam extends Feature {

   public SneakSpam() {
      super("SneakSpam", Category.PLAYER);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.PRE_MOTION) return;
      if (mc.player == null) return;
      if (InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyShift.saveString()).getValue())) return;

      mc.options.keyShift.setDown(!mc.options.keyShift.isDown());
   }
}
