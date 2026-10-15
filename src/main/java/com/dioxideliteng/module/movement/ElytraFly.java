package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.NumberSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Port of Krs ElytraFly (com.instrumentalist.krs.hacks.features.movement.ElytraFly) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs TickEvent -> {@link Events#TICK};
 *  - {@code ModuleManager.getModuleState(ElytraFly.class)} -> the resolved instance check
 *    (Scaffold2-style lookup; FeatureManager is owned outside this port);
 *  - The static {@code hookFallFlyingMovement} entry point is preserved verbatim for the
 *    LivingEntity#updateFallFlyingMovement ModifyReturnValue hook (see report: the mixin
 *    injection itself is not part of this port).
 */
public class ElytraFly extends Feature {
   private final NumberSetting horizontalSpeed = new NumberSetting("Horizontal Speed", this, 1.8, 0.1, 4.0, 0.1);
   private final NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", this, 1.2, 0.1, 4.0, 0.1);

   public ElytraFly() {
      super("Elytra Fly", Category.MOVEMENT);
   }

   public static Vec3 hookFallFlyingMovement(Vec3 original, LivingEntity entity) {
      if (!(entity instanceof LocalPlayer player)) return original;

      ElytraFly module = inst();
      if (module == null || !module.isEnabled())
         return original;

      return module.getControlledMotion(original, player);
   }

   private Vec3 getControlledMotion(Vec3 original, LocalPlayer player) {
      if (!player.isFallFlying() || player.isPassenger() || !InventoryMove2.canMoveFreely())
         return original;

      double left = 0.0;
      double forward = 0.0;

      if (isKeyDown(mc.options.keyLeft)) left += 1.0;
      if (isKeyDown(mc.options.keyRight)) left -= 1.0;
      if (isKeyDown(mc.options.keyUp)) forward += 1.0;
      if (isKeyDown(mc.options.keyDown)) forward -= 1.0;

      double length = Math.sqrt(left * left + forward * forward);
      if (length > 1.0) {
         left /= length;
         forward /= length;
      }

      double yaw = Math.toRadians(player.getYRot());
      double x = left * Math.cos(yaw) - forward * Math.sin(yaw);
      double z = left * Math.sin(yaw) + forward * Math.cos(yaw);
      double y = 0.0;

      if (isKeyDown(mc.options.keyJump)) y += this.verticalSpeed.getValue();
      if (isKeyDown(mc.options.keyShift)) y -= this.verticalSpeed.getValue();

      return new Vec3(x * this.horizontalSpeed.getValue(), y, z * this.horizontalSpeed.getValue());
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;

      if (mc.player != null && mc.player.isFallFlying() && InventoryMove2.canMoveFreely())
         mc.options.keyShift.setDown(false);
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEnable() {
   }

   private static boolean isKeyDown(KeyMapping key) {
      return InputConstants.isKeyDown(InputConstants.getKey(key.saveString()).getValue()
      );
   }

   private static ElytraFly inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof ElytraFly e) return e;
      }
      return null;
   }
}
