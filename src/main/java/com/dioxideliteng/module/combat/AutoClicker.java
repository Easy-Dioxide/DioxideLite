package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.mojang.blaze3d.platform.InputConstants.Key;
import java.util.Random;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.phys.HitResult.Type;

public class AutoClicker extends Feature {
   private static final String MIN_CPS_LABEL = "Min CPS";
   private static final String MAX_CPS_LABEL = "Max CPS";
   private static final String AUTO_CLICKER_LABEL = "AutoClicker";
   private static final String RANDOMIZE_LABEL = "Randomize";
   private static final String BREAK_BLOCKS_LABEL = "Break Blocks";

   private final NumberSetting minCps = new NumberSetting(MIN_CPS_LABEL, this, 8.0, 1.0, 20.0, 1.0);
   private final NumberSetting maxCps;
   private final BooleanSetting randomize;
   private final BooleanSetting breakBlocks;

   private final Random random;

   private long lastClickTimeMillis;

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         if (mc.gui.screen() != null) {
            return;
         }

         if (!mc.mouseHandler.isLeftPressed()) {
            return;
         }

         if (this.breakBlocks.getValue() && mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
            Key attackKey = com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(0);
            KeyMapping.set(attackKey, true);
            return;
         }

         long clickDelayMillis = this.sampleClickDelayMillis();
         if (System.currentTimeMillis() - this.lastClickTimeMillis >= clickDelayMillis) {
            Key attackKey = com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(0);
            KeyMapping.set(attackKey, true);
            KeyMapping.click(attackKey);
            KeyMapping.set(attackKey, false);
            this.lastClickTimeMillis = System.currentTimeMillis();
         }
      }
   }

   @Override
   public void onEnable() {
      this.lastClickTimeMillis = System.currentTimeMillis();
   }

   private long sampleClickDelayMillis() {
      double clicksPerSecond;
      if (this.randomize.getValue()) {
         clicksPerSecond = this.minCps.getValue() + (this.maxCps.getValue() - this.minCps.getValue()) * this.random.nextDouble();
      } else {
         clicksPerSecond = this.minCps.getValue();
      }

      return (long)(1000.0 / clicksPerSecond);
   }

   public AutoClicker() {
      super(AUTO_CLICKER_LABEL, Category.COMBAT);
      this.maxCps = new NumberSetting(MAX_CPS_LABEL, this, 12.0, 1.0, 20.0, 1.0);
      this.randomize = new BooleanSetting(RANDOMIZE_LABEL, this, true);
      this.breakBlocks = new BooleanSetting(BREAK_BLOCKS_LABEL, this, false);
      this.random = new Random();
   }
}
