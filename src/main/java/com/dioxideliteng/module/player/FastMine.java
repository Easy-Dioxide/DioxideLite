package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import mixins.MultiPlayerGameModeAccessor;

public class FastMine extends Feature {
   private static final String REMOVE_DELAY_LABEL = "Remove Delay";
   private static final String FAST_MINE_LABEL = "FastMine";
   private static final String SPEED_LABEL = "Speed";

   private final NumberSetting speed;
   private final BooleanSetting removeDelay;

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         if (mc.gameMode == null) {
            return;
         }

         MultiPlayerGameModeAccessor gameModeAccessor = (MultiPlayerGameModeAccessor)mc.gameMode;
         if (this.removeDelay.getValue()) {
            gameModeAccessor.setDestroyDelay(0);
         }

         float destroyProgress = gameModeAccessor.getDestroyProgress();
         if (destroyProgress > 0.0F && destroyProgress < 0.99F) {
            gameModeAccessor.setDestroyProgress((float)((double)destroyProgress * this.speed.getValue()));
         }
      }
   }

   public FastMine() {
      super(FAST_MINE_LABEL, Category.PLAYER);
      this.speed = new NumberSetting(SPEED_LABEL, this, 1.5, 1.0, 5.0, 0.1);
      this.removeDelay = new BooleanSetting(REMOVE_DELAY_LABEL, this, true);
   }
}
