package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MovementUtil;

/**
 * Port of Krs FastFall (com.instrumentalist.krs.hacks.features.movement.FastFall) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - Krs {@code tag()} -> {@link Feature#setSuffix(String)} refreshed on UPDATE.
 */
public class FastFall2 extends Feature {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Shift", new String[]{"Shift", "Always"});
   private final NumberSetting motion = new NumberSetting("Motion", this, 0.8, 0.1, 4.0, 0.1);

   public FastFall2() {
      super("FastFall2", Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;
      setSuffix(this.mode.getValue());

      var player = mc.player;
      if (player == null || player.onGround() || player.isFallFlying() || player.isInWater() || player.isInLava())
         return;

      if (this.mode.is("Shift") && !mc.options.keyShift.isDown())
         return;

      if (player.getDeltaMovement().y < 0.0)
         MovementUtil.setVelocityY(-this.motion.getValue());
   }
}
