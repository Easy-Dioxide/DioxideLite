package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MovementUtil;

/**
 * Port of Krs WaterSpeed (com.instrumentalist.krs.hacks.features.movement.WaterSpeed) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - Krs {@code tag()} -> {@link Feature#setSuffix(String)} refreshed on UPDATE.
 */
public class WaterSpeed2 extends Feature {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Scale", new String[]{"Scale", "Strafe", "Boost"});
   private final NumberSetting multiplier = new NumberSetting("Multiplier", this, 1.2, 1.0, 3.0, 0.1);
   private final NumberSetting maxSpeed = new NumberSetting("Max Speed", this, 0.5, 0.1, 2.0, 0.1);
   private final BooleanSetting lava = new BooleanSetting("Lava", this, false);

   public WaterSpeed2() {
      super("WaterSpeed2", Category.MOVEMENT);
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
      if (player == null || !MovementUtil.isMoving() || !player.isInWater() && (!this.lava.getValue() || !player.isInLava()))
         return;

      if (this.mode.is("Strafe"))
         MovementUtil.strafe((float) this.maxSpeed.getValue());
      else if (this.mode.is("Boost"))
         MovementUtil.boost((this.multiplier.getValue() - 1.0f) * 0.08f, this.maxSpeed.getValue());
      else
         MovementUtil.scaleVelocityXZ(this.multiplier.getValue());
      MovementUtil.limitSpeed(this.maxSpeed.getValue());
   }
}
