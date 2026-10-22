package com.dioxideliteng.module.movement;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.util.krs.InputUtil;
import com.dioxideliteng.util.krs.RotationManager;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/**
 * Port of Krs MovementFix (com.instrumentalist.krs.hacks.features.movement.MovementFix)
 * to DioxideLiteNG. The whole static API (shouldFixMovement / shouldFixSprintCondition /
 * getMovementYaw / getMovementPitch / getMovementLookVector / getJumpMovementYaw /
 * updateInput / getRawInput / getCorrectionDirectionError / hasForwardImpulseForSprint)
 * is preserved verbatim — these are the entry points the Player/LocalPlayer/KeyboardInput
 * mixin hooks call in Krs. Only the module-state and client references are adapted:
 *  - {@code ModuleManager.getModuleState(MovementFix.class)} -> the resolved instance
 *    check (Scaffold2-style lookup; FeatureManager is owned outside this port);
 *  - {@code Client.rotationManager} -> {@link RotationManager#INSTANCE};
 *  - {@code Client.mc} -> {@link Feature#mc};
 *  - {@code Sprint.*} (Krs) -> {@link Sprint2};
 *  - {@code KeyboardInput#calculateImpulse} is private in MC 26.3; the identical
 *    vanilla logic is inlined below (same approach as the ported InputUtil/WTap).
 *
 * <p>The swimming look-vector hook this module needs is appended to
 * {@code mixins/MixinPlayer} (dioxideliteng$movementFixSwimmingLookVector); since B11 the
 * remaining mixin injections are wired as well: {@code MixinEntity#moveRelative} (strafe yaw),
 * {@code MixinLivingEntity#jumpFromGround} (sprint-jump boost direction) and
 * {@code MixinLocalPlayer#preMotion} (fresh rotationManager read at packet time, Krs
 * {@code ModuleManager#onMotion}).
 *
 * <p>This module is a <b>manual switch</b> (default off), exactly like Krs: no rotation module
 * auto-enables it. Enable it whenever a rotation module (Scaffold2 / KillAura2) is active,
 * otherwise the client moves along its real yaw while the server simulates the transmitted
 * spoofed rotation — a per-tick position divergence flagged as simulation violations.
 */
public class MovementFix extends Feature {
   private static Input rawInput = Input.EMPTY;
   private static Input correctedInput = Input.EMPTY;
   private static Input sprintConditionInput = Input.EMPTY;
   private static boolean inputOverridden = false;
   private static boolean sprintConditionOverridden = false;

   public MovementFix() {
      super("Movement Fix", Category.MOVEMENT);
   }

   public static boolean shouldFixMovement() {
      return shouldUseRotationFix();
   }

   public static boolean shouldFixSprintCondition() {
      return isSprintCorrectionEnabled() && (shouldUseRotationFix() || sprintConditionOverridden);
   }

   // Module-state query mirroring Krs ModuleManager.getModuleState(MovementFix.class),
   // for call sites that need "is the module on" without the rotation precondition.
   public static boolean isModuleEnabled() {
      MovementFix module = inst();
      return module != null && module.isEnabled();
   }

   // Used by Scaffold2's auto-link (Krs users pair the two modules by hand; see
   // Scaffold2#onEnable). Returns false when the module instance cannot be
   // resolved so the caller can skip its bookkeeping.
   public static boolean setModuleEnabled(boolean enabled) {
      MovementFix module = inst();
      if (module == null || module.isEnabled() == enabled) {
         return false;
      }

      module.setEnabled(enabled);
      return true;
   }

   public static float getMovementYaw() {
      if (!shouldFixMovement())
         return mc.player != null ? mc.player.getYRot() : 0.0f;

      return RotationManager.INSTANCE.getRotationYaw();
   }

   public static float getMovementPitch() {
      if (!shouldFixMovement())
         return mc.player != null ? mc.player.getXRot() : 0.0f;

      return RotationManager.INSTANCE.getRotationPitch();
   }

   public static Vec3 getMovementLookVector() {
      return Vec3.directionFromRotation(getMovementPitch(), getMovementYaw());
   }

   public static float getJumpMovementYaw() {
      return getMovementYaw();
   }

   public static void updateInput(ClientInput input) {
      if (input == null) {
         resetInputState();
         return;
      }

      rawInput = input.keyPresses;
      Input movementInput = rawInput;
      sprintConditionInput = rawInput;
      sprintConditionOverridden = false;

      if (shouldUseRotationFix()) {
         float rotationYaw = RotationManager.INSTANCE.getRotationYaw();

         movementInput = fixStrafeInput(rawInput, rotationYaw);

         sprintConditionInput = fixStrafeInput(rawInput, rotationYaw);
         sprintConditionOverridden = !sprintConditionInput.equals(rawInput);
      }

      Input sprintInput = Sprint2.applyInputSprint(mc.player, sprintConditionInput);
      correctedInput = withSprint(movementInput, sprintInput.sprint());
      inputOverridden = !correctedInput.equals(rawInput);

      if (inputOverridden)
         InputUtil.applyInput(input, correctedInput);
   }

   public static Input getRawInput() {
      return rawInput;
   }

   public static float getCorrectionDirectionError(float movementYaw, float targetYaw) {
      int sector = correctionSector(movementYaw, targetYaw);
      float correctedMovementYaw = targetYaw + (sector - 4) * 45.0F;
      return Mth.wrapDegrees(correctedMovementYaw - movementYaw);
   }

   public static boolean hasForwardImpulseForSprint(ClientInput input) {
      Input keyPresses = input != null ? input.keyPresses : correctedInput;
      if (Sprint2.shouldSprintOmnidirectional())
         return isMoving(keyPresses);

      if (shouldFixSprintCondition())
         return hasForwardImpulse(sprintConditionInput);

      return input == null ? hasForwardImpulse(keyPresses) : input.hasForwardImpulse();
   }

   private static boolean shouldUseRotationFix() {
      MovementFix module = inst();
      return module != null && module.isEnabled() && RotationManager.INSTANCE.isRotating() && mc.player != null;
   }

   private static boolean isSprintCorrectionEnabled() {
      MovementFix module = inst();
      return module != null && module.isEnabled() && mc.player != null;
   }

   private static Input fixStrafeInput(Input input, float targetYaw) {
      if (input == null || !isMoving(input))
         return input == null ? Input.EMPTY : input;

      LocalPlayer player = mc.player;
      if (player == null)
         return input;

      float movementYaw = adjustYaw(player.getYRot(), forwardValue(input), leftValue(input));
      return sectorInput(input, correctionSector(movementYaw, targetYaw));
   }

   private static int correctionSector(float movementYaw, float targetYaw) {
      float angle = Mth.wrapDegrees(movementYaw - targetYaw + 22.5F);
      return (int) (angle + 180.0F) / 45 % 8;
   }

   private static boolean isMoving(Input input) {
      return input.forward() != input.backward() || input.left() != input.right();
   }

   private static boolean hasForwardImpulse(Input input) {
      return input != null && calculateImpulse(input.forward(), input.backward()) > 1.0E-5F;
   }

   // MC 26.3 keeps KeyboardInput#calculateImpulse private; identical vanilla logic.
   private static float calculateImpulse(boolean positive, boolean negative) {
      return positive == negative ? 0.0F : (positive ? 1.0F : -1.0F);
   }

   private static int forwardValue(Input input) {
      int value = 0;
      if (input.forward())
         value++;
      if (input.backward())
         value--;
      return value;
   }

   private static int leftValue(Input input) {
      int value = 0;
      if (input.left())
         value++;
      if (input.right())
         value--;
      return value;
   }

   private static float adjustYaw(float yaw, float forward, float strafe) {
      if (forward < 0.0F)
         yaw += 180.0F;

      if (strafe != 0.0F) {
         float multiplier = forward == 0.0F ? 1.0F : 0.5F * Math.signum(forward);
         yaw += -90.0F * multiplier * Math.signum(strafe);
      }

      return Mth.wrapDegrees(yaw);
   }

   private static Input sectorInput(Input source, int sector) {
      return switch (sector) {
         case 0 -> directionalInput(source, -1, 0);
         case 1 -> directionalInput(source, -1, 1);
         case 2 -> directionalInput(source, 0, 1);
         case 3 -> directionalInput(source, 1, 1);
         case 4 -> directionalInput(source, 1, 0);
         case 5 -> directionalInput(source, 1, -1);
         case 6 -> directionalInput(source, 0, -1);
         case 7 -> directionalInput(source, -1, -1);
         default -> source;
      };
   }

   private static Input directionalInput(Input source, int forward, int left) {
      return new Input(
              forward > 0,
              forward < 0,
              left > 0,
              left < 0,
              source.jump(),
              source.shift(),
              source.sprint()
      );
   }

   private static Input withSprint(Input source, boolean sprint) {
      if (source.sprint() == sprint)
         return source;

      return new Input(
              source.forward(),
              source.backward(),
              source.left(),
              source.right(),
              source.jump(),
              source.shift(),
              sprint
      );
   }

   private static void restoreInput() {
      LocalPlayer player = mc.player;
      if (player != null && inputOverridden)
         InputUtil.applyInput(player.input, rawInput);

      resetInputState();
   }

   private static void resetInputState() {
      rawInput = Input.EMPTY;
      correctedInput = Input.EMPTY;
      sprintConditionInput = Input.EMPTY;
      inputOverridden = false;
      sprintConditionOverridden = false;
   }

   @Override
   public void onDisable() {
      restoreInput();
   }

   @Override
   public void onEnable() {
      resetInputState();
   }

   // Resolve the active MovementFix instance so static methods can check module state.
   private static MovementFix inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof MovementFix m) return m;
      }
      return null;
   }
}
