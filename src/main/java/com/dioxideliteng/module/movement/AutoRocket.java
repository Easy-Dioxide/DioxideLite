package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.AutoRocket) and adapted. */
public class AutoRocket extends Feature {
   private static final String MODE_LABEL = "Mode";
   private static final String SPEED_LABEL = "Speed";
   private static final String DELAY_LABEL = "Delay";
   private static final String LOW_SPEED = "Low Speed";
   private static final String JUMP = "Jump";
   private static final String ALWAYS = "Always";

   private final ModeSetting mode = new ModeSetting(MODE_LABEL, this, LOW_SPEED, new String[]{LOW_SPEED, JUMP, ALWAYS});
   private final NumberSetting speed = new NumberSetting(SPEED_LABEL, this, 0.7, 0.1, 3.0, 0.1);
   private final NumberSetting delay = new NumberSetting(DELAY_LABEL, this, 1500.0, 100.0, 5000.0, 1.0);

   private long lastUseTime;

   public AutoRocket() {
      super("Auto Rocket", Category.MOVEMENT);
      this.speed.setVisible(() -> this.mode.is(LOW_SPEED));
   }

   @Override
   public void onEnable() {
      this.lastUseTime = System.currentTimeMillis();
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());
      var player = mc.player;
      var gameMode = mc.gameMode;
      if (player == null || gameMode == null || !player.isFallFlying() || mc.gui.screen() != null
            || !this.hasDelayPassed() || !this.shouldUse()) return;

      int slot = this.findRocketSlot();
      if (slot == -1) return;

      int oldSlot = player.getInventory().getSelectedSlot();
      if (slot != oldSlot) {
         player.getInventory().setSelectedSlot(slot);
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
      }

      gameMode.useItem(player, InteractionHand.MAIN_HAND);

      if (slot != oldSlot) {
         player.getInventory().setSelectedSlot(oldSlot);
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(oldSlot));
      }

      this.lastUseTime = System.currentTimeMillis();
   }

   private boolean shouldUse() {
      if (this.mode.is(JUMP)) return mc.options.keyJump.isDown();
      if (this.mode.is(ALWAYS)) return true;
      return this.getHorizontalSpeed() <= this.speed.getValue();
   }

   private boolean hasDelayPassed() {
      return System.currentTimeMillis() - this.lastUseTime >= (long) this.delay.getValue();
   }

   private int findRocketSlot() {
      var player = mc.player;
      if (player == null) return -1;
      for (int i = 0; i <= 8; i++) {
         if (player.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) return i;
      }
      return -1;
   }

   private double getHorizontalSpeed() {
      var dm = mc.player.getDeltaMovement();
      return Math.sqrt(dm.x * dm.x + dm.z * dm.z);
   }
}
