package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import mixins.LivingEntityAccessor;

public class NoJumpDelay extends Feature {
   private static final String NO_JUMP_DELAY_LABEL = "NoJumpDelay";

   public NoJumpDelay() {
      super(NO_JUMP_DELAY_LABEL, Category.PLAYER);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.MOVE_INPUT) {
         ((LivingEntityAccessor)mc.player).setNoJumpDelay(0);
      }
   }
}
