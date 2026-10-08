package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.InventoryUtil;
import mixins.MinecraftAccessor;

public class FastPlace extends Feature {
   private static final String DELAY_LABEL = "Delay";
   private static final String BLOCKS_ONLY_LABEL = "Blocks Only";
   private static final String FAST_PLACE_LABEL = "FastPlace";

   private NumberSetting delay = new NumberSetting(DELAY_LABEL, this, 1.0, 0.0, 3.0, 1.0);
   private BooleanSetting blocksOnly;

   private int placementTicks;

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         if (this.blocksOnly.getValue() && !InventoryUtil.isHoldingPlaceableBlock()) {
            return;
         }

         if ((int)this.delay.getValue() == 0) {
            ((MinecraftAccessor)mc).setRightClickDelay(0);
         } else {
            if ((double)this.placementTicks >= this.delay.getValue()) {
               ((MinecraftAccessor)mc).setRightClickDelay(0);
               this.placementTicks = 0;
            }

            this.placementTicks++;
         }
      }
   }

   public FastPlace() {
      super(FAST_PLACE_LABEL, Category.PLAYER);
      this.blocksOnly = new BooleanSetting(BLOCKS_ONLY_LABEL, this, true);
   }
}
