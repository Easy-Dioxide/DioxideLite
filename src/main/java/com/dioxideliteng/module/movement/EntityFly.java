package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.Interpolation;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.PacketUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.lwjgl.glfw.GLFW;

/**
 * Port of Krs EntityFly (com.instrumentalist.krs.hacks.features.movement.EntityFly) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event/notification API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE}; Krs TickEvent -> {@link Events#TICK};
 *  - {@code Client.notificationManager.addNotification(...)} (NotificationManager is not
 *    ported) -> {@link DynamicIslandManager#notifySuccess(String, String)};
 *  - {@code ModuleManager.getModuleState(EntityFly.class)} -> the resolved instance check
 *    (Scaffold2-style lookup; FeatureManager is owned outside this port);
 *  - Krs settings are static fields; DioxideLiteNG registers settings per instance, so the
 *    statics resolve the single module instance (same pattern as Scaffold2's {@code inst()});
 *  - {@code description()} ("Dismount with left alt") has no Feature equivalent and is
 *    dropped; the static paddle/steering hook entry points are preserved verbatim for the
 *    AbstractBoat mixin hooks (see report: the mixin injections are not part of this port).
 */
public class EntityFly extends Feature {
   private final BooleanSetting stopBoatPaddles = new BooleanSetting("Stop Boat Paddles", this, true);
   private final BooleanSetting twoBjpBoost = new BooleanSetting("2b2tJP Boost", this, false);
   private final NumberSetting hSpeed = new NumberSetting("Horizontal Speed", this, 2.0, 0.1, 4.0, 0.1);
   private final NumberSetting vSpeed = new NumberSetting("Vertical Speed", this, 1.2, 0.1, 4.0, 0.1);

   public EntityFly() {
      super("Entity Fly", Category.MOVEMENT);
      this.hSpeed.setVisible(() -> !this.twoBjpBoost.getValue());
      this.vSpeed.setVisible(() -> !this.twoBjpBoost.getValue());
   }

   public static boolean hookUpdatePaddles(AbstractBoat boatEntity, boolean original) {
      EntityFly module = inst();
      if (module != null && shouldIgnoreBoatSteering(boatEntity) && module.stopBoatPaddles.getValue())
         return false;

      return original;
   }

   public static boolean shouldIgnoreBoatSteering(AbstractBoat boatEntity) {
      var player = mc.player;
      EntityFly module = inst();
      if (module != null && module.isEnabled() && player != null && player.isPassenger() && player.getVehicle() == boatEntity)
         return true;

      return false;
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.UPDATE) {
         var player = mc.player;
         if (player == null) return;

         if (player.isPassenger() && InputConstants.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT)) {
            player.stopRiding();
            PacketUtil.sendPacket(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, true, false)));
            DynamicIslandManager.notifySuccess("Entity Fly", "Dismounted");
            return;
         }

         var vehicle = player.getVehicle();
         if (!player.isPassenger() || vehicle == null) return;

         float yMotion = 0f;
         boolean boostMode = this.twoBjpBoost.getValue();
         float booster = boostMode ? 3f : (float) this.vSpeed.getValue();

         if (InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyJump.saveString()).getValue()))
            yMotion += booster;

         if (InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyShift.saveString()).getValue()))
            yMotion -= booster;

         double yaw = Math.toRadians(MovementUtil.getPlayerDirection());
         boolean moving = MovementUtil.isMoving();
         float horizontalSpeed = boostMode ? 17f : (float) this.hSpeed.getValue();
         double xMotion = moving ? -Math.sin(yaw) * horizontalSpeed : 0.0;
         double zMotion = moving ? Math.cos(yaw) * horizontalSpeed : 0.0;

         if (this.stopBoatPaddles.getValue()) {
            float lerpedYaw = Interpolation.INSTANCE.lerp(vehicle.getYRot(), player.getYRot(), 0.35f);
            vehicle.setYRot(lerpedYaw);
         }

         vehicle.setNoGravity(true);
         vehicle.setDeltaMovement(xMotion, yMotion, zMotion);
      } else if (event == Events.TICK) {
         var player = mc.player;
         if (player == null || !player.isPassenger() || player.getVehicle() == null) return;

         mc.options.keyShift.setDown(false);
      }
   }

   private static EntityFly inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof EntityFly e) return e;
      }
      return null;
   }
}
