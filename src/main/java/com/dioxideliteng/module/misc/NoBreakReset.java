package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventCancelBlockBreaking;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.NoBreakReset) and adapted to the client. */
public class NoBreakReset extends Feature {
   public NoBreakReset() {
      super("NoBreakReset", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventCancelBlockBreaking) {
         event.setCancelled(true);
      }
   }
}
