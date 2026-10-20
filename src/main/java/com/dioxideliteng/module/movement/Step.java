package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.RotationManager;
import com.dioxideliteng.util.krs.TimerUtil;
import java.util.Locale;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Port of Krs Step (com.instrumentalist.krs.hacks.features.movement.Step) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the module-state/event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - {@code ModuleManager.getModuleState(Step.class)} -> the resolved instance
 *    check (Scaffold2-style lookup; FeatureManager is owned outside this port);
 *  - {@code ModuleManager.getModuleState(FlyModule.class)} / {@code SpeedModule.class}
 *    -> the ported Fly2 / Speed2 feature lookups (same iteration pattern the ported
 *    SpeedModeManager uses for its Fly gate);
 *  - {@code Client.rotationManager} -> {@link RotationManager#INSTANCE}.
 *
 * <p>The static entry points (shouldStep / hookStepHeight / steppingFunctions) are
 * preserved verbatim for the Krs LivingEntity maxUpStep hook; the injection itself
 * is not part of this port (see report).
 */
public class Step extends Feature {

   private final ModeSetting mode = new ModeSetting("Mode", this, "Vanilla", new String[]{"Vanilla", "NCP", "Hypixel"});
   private final BooleanSetting disableWhenSpeed = new BooleanSetting("Disable when Speed", this, true);
   private final NumberSetting height = new NumberSetting("Height", this, 2, 1, 10, 0.1);
   private final NumberSetting delay = new NumberSetting("Delay", this, 1, 1, 10, 1);
   private final BooleanSetting customTimer = new BooleanSetting("Custom Timer", this, true);
   private final NumberSetting timerSpeed = new NumberSetting("Timer Speed", this, 0.6, 0.1, 10, 0.1);

   private static boolean canStep = true;
   private static boolean sentBypass = false;
   private static boolean calledModifiedStep = false;
   private static int stepDelay = 0;

   public Step() {
      super("Step", Category.MOVEMENT);
      this.height.setVisible(() -> !this.mode.is("Hypixel"));
      this.timerSpeed.setVisible(this.customTimer::getValue);
   }

   private static boolean shouldStep() {
      Step module = inst();
      return module != null && module.isEnabled()
            && mc.player != null && !mc.player.isShiftKeyDown() && mc.player.onGround() && canStep
            && !isFlyEnabled()
            && (!module.disableWhenSpeed.getValue() || !isSpeedEnabled());
   }

   public static float hookStepHeight(float original, LivingEntity entity) {
      if (entity instanceof LocalPlayer && mc.player != null) {
         Step module = inst();
         if (shouldStep()) {
            if (calledModifiedStep)
               afterStepFunctions();

            if (module.mode.is("Hypixel")) {
               BlockPos above = mc.player.blockPosition().above(3);
               if (!RotationManager.INSTANCE.isRotating()
                       && mc.level.getBlockState(above).isAir()
                       && mc.level.getBlockState(above.east()).isAir()
                       && mc.level.getBlockState(above.south()).isAir()
                       && mc.level.getBlockState(above.west()).isAir()
                       && mc.level.getBlockState(above.north()).isAir())
                  return 1f;
            } else return (float) module.height.getValue();
         }

         if (calledModifiedStep && !mc.player.onGround())
            afterStepFunctions();
      }

      return original;
   }

   public static void steppingFunctions() {
      Step module = inst();
      if (!shouldStep()) return;

      if (module.customTimer.getValue())
         TimerUtil.timerSpeed = (float) module.timerSpeed.getValue();

      if (!sentBypass) {
         Vec3 pos = mc.player.position();
         boolean horizontalCollision = mc.player.horizontalCollision;

         switch (module.mode.getValue().toLowerCase(Locale.ROOT)) {
            case "ncp":
               PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y + 0.41999998688698, pos.z, false, horizontalCollision));
               PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y + 0.7531999805212, pos.z, false, horizontalCollision));
               break;

            case "hypixel":
               sendStepPacket(pos, .41999998688698, horizontalCollision);
               sendStepPacket(pos, .7531999805212, horizontalCollision);
               sendStepPacket(pos, 1.001335997911214, horizontalCollision);
               sendStepPacket(pos, 1.16610926093821, horizontalCollision);
               sendStepPacket(pos, 1.24918707874468, horizontalCollision);
               sendStepPacket(pos, 1.093955074228084, horizontalCollision);
               break;
         }
         sentBypass = true;
      }

      calledModifiedStep = true;
   }

   private static void sendStepPacket(Vec3 pos, double yOffset, boolean horizontalCollision) {
      PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y + yOffset, pos.z, false, horizontalCollision));
   }

   private static void afterStepFunctions() {
      Step module = inst();
      if (module.customTimer.getValue())
         TimerUtil.reset();

      stepDelay = (int) module.delay.getValue();
      canStep = false;
      sentBypass = false;
      calledModifiedStep = false;
   }

   @Override
   public void onDisable() {
      Step module = inst();
      if (module != null && module.customTimer.getValue())
         TimerUtil.reset();

      canStep = true;
      stepDelay = 0;
      calledModifiedStep = false;
      sentBypass = false;
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;

      if (mc.player == null) return;

      if (!canStep) {
         if (stepDelay <= 0) {
            stepDelay = 0;
            canStep = true;
         } else stepDelay--;
      }
   }

   private static Step inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof Step s) return s;
      }
      return null;
   }

   private static boolean isFlyEnabled() {
      for (Feature feature : FeatureManager.getModules()) {
         if (feature instanceof Fly2 fly2)
            return fly2.isEnabled();
      }
      return false;
   }

   private static boolean isSpeedEnabled() {
      for (Feature feature : FeatureManager.getModules()) {
         if (feature instanceof Speed2 speed2)
            return speed2.isEnabled();
      }
      return false;
   }
}
