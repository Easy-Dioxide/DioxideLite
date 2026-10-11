package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.util.Mth;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.SpinBot) and adapted.
 *  Krs rotationManager.startRotation/stopRotation are replaced with the client
 *  Events.ROTATION yaw/pitch overrides. */
public class SpinBot extends Feature {
   private final NumberSetting spinSpeed = new NumberSetting("Spin Speed", this, 30.0, -40.0, 40.0, 1.0);
   private final NumberSetting pitch = new NumberSetting("Pitch", this, 90.0, -90.0, 90.0, 1.0);

   private float spinYaw = 0.0F;

   public SpinBot() {
      super("SpinBot", Category.PLAYER);
   }

   @Override
   public void onDisable() {
      if (mc.player == null) return;
      this.spinYaw = 0.0F;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.TICK) {
         if (mc.player == null) return;
         if (this.spinYaw >= 360.0F || this.spinYaw <= -360.0F) this.spinYaw = 0.0F;
         this.spinYaw += (float) this.spinSpeed.getValue();
      } else if (event == Events.ROTATION) {
         this.setSuffix(String.valueOf((int) this.spinSpeed.getValue()));
         if (mc.player == null) return;
         Events.ROTATION.setYaw(this.spinYaw);
         Events.ROTATION.setPitch(Mth.clamp((float) this.pitch.getValue(), -90.0F, 90.0F));
         Events.ROTATION.setMovementCorrection(true);
      }
   }
}
