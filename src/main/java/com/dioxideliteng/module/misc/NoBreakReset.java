package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.NoBreakReset).
 * Krs parity: onCancelBlockBreaking cancels the vanilla block-breaking stop.
 */
public class NoBreakReset extends Feature {

   public NoBreakReset() {
      super("No Break Reset", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.CANCEL_BLOCK_BREAKING) {
         event.setCancelled(true);
      }
   }
}
