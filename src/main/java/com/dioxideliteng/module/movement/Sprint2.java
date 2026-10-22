package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.player.Scaffold2;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import mixins.LocalPlayerSprintAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;

/**
 * Port of Krs Sprint (com.instrumentalist.krs.hacks.features.movement.Sprint) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the module-state/event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - Krs SendPacketEvent -> {@link EventPacketSend};
 *  - Krs WorldEvent -> {@link EventWorld} (fresh instance: instanceof dispatch);
 *  - {@code ModuleManager.getModuleState(Sprint.class)} -> the resolved instance
 *    check (Scaffold2-style lookup; FeatureManager is owned outside this port);
 *  - {@code Scaffold.shouldSuppressTellyNormalJumpSprintHook()} ->
 *    {@link Scaffold2#shouldSuppressTellyNormalJumpSprintHook()};
 *  - {@code ModuleManager.getModuleState(Scaffold.class) && Scaffold.noSprint.get()}
 *    -> {@link Scaffold2#isNoSprintEnabled()};
 *  - {@code NoSlow.*} (Krs) -> {@link NoSlow2};
 *  - {@code MovementFix.*} (Krs) -> {@link MovementFix};
 *  - {@code LocalPlayerSprintAccessor.krs$invoke*} (Krs) ->
 *    {@link LocalPlayerSprintAccessor} (mixins package, registered in
 *    dioxideliteng.mixins.json);
 *  - {@code KeyboardInput#calculateImpulse} is private in MC 26.3; the identical
 *    vanilla logic is inlined below (same approach as the ported InputUtil/WTap).
 *
 * <p>Deviation from Krs: the "Silent" sprint behaviour (cancelling outbound
 * START/STOP_SPRINTING command packets) is intentionally disabled — on 26.3 it
 * desyncs the server-side/Grim sprint state and causes simulation flags
 * (see {@link #onSendPacket}). Everything else matches Krs verbatim.
 *
 * <p>The static entry points (shouldSprintOmnidirectional / getOmnidirectionalJumpYaw /
 * applyInputSprint / tryStartSprinting / keepSprintHook) are preserved verbatim for
 * the Krs ClientPlayerEntityMixin/PlayerEntityMixin/LivingEntityMixin hooks; the
 * injections themselves are not part of this port (see report).
 */
public class Sprint2 extends Feature {

   private final BooleanSetting autoSprint = new BooleanSetting("Auto Sprint", this, true);
   private final BooleanSetting multiDirection = new BooleanSetting("Multi Direction", this, true);
   private final BooleanSetting keepSprint = new BooleanSetting("Keep Sprint", this, true);
   // Legacy Krs "Silent" toggle, kept for config compatibility only. It no longer affects
   // packet flow: suppressing START/STOP_SPRINTING desyncs sprint state on 26.3 (see
   // onSendPacket), which produced per-tick simulation offsets on Grim.
   private final BooleanSetting silentSprint = new BooleanSetting("Silent", this, false);
   private final BooleanSetting wallCheck = new BooleanSetting("Wall Check", this, false);

   public Sprint2() {
      super("Sprint2", Category.MOVEMENT);
      this.multiDirection.setVisible(this.autoSprint::getValue);
      this.wallCheck.setVisible(this.autoSprint::getValue);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.UPDATE) {
         onUpdate();
      } else if (event instanceof EventPacketSend sending) {
         onSendPacket(sending);
      }
   }

   private void onUpdate() {
      var player = mc.player;
      if (player == null) return;

      if (shouldAutoSprint(player, player.input.keyPresses) && !player.isSprinting())
         player.setSprinting(true);
   }

   private void onSendPacket(EventPacketSend event) {
      // 26.3 sprint synchronization chain (verified against vanilla bytecode):
      //   ServerboundPlayerInputPacket -> ServerGamePacketListenerImpl#handlePlayerInput
      //     mirrors ONLY input.shift(); the sprint flag of the input packet is ignored by
      //     the vanilla server. The sole path that tells the server "the player is
      //     sprinting" is ServerboundPlayerCommandPacket (START/STOP_SPRINTING) ->
      //     ServerPlayer#setSprinting.
      //   Grim mirrors this exactly: PacketEntityAction is the ONLY writer of
      //   player.isSprinting (PacketPlayerSteer merely stores input.isSprinting() into
      //   knownInput, which the prediction engine never consumes — Grim's own
      //   "1.21.5+ sprint desync" comment), and PredictionEngine /
      //   MovementCheckRunner read player.isSprinting for sprint multipliers and the
      //   sprinting attribute (Grim issue #838: suppressed sprint packets -> the server
      //   never sees sprinting -> hundreds of simulation flags per match).
      // Therefore sprint command packets must NEVER be cancelled here: the client sprints
      // locally (sprint speed / 1.3 multiplier / sprint FOV) while the server would
      // predict non-sprint movement, producing a per-tick offset -> simulated VL.
      // Intentionally left as a no-op; the "Silent" setting is legacy (see field).
   }

   public static boolean shouldSprintOmnidirectional() {
      Sprint2 module = inst();
      return module != null && module.isEnabled() && module.autoSprint.getValue() && module.multiDirection.getValue();
   }

   public static float getOmnidirectionalJumpYaw(float fallbackYaw) {
      var player = mc.player;
      if (!shouldSprintOmnidirectional() || player == null || player.input == null)
         return fallbackYaw;

      Input input = player.input.keyPresses == null ? Input.EMPTY : player.input.keyPresses;
      if (!isMoving(input))
         return fallbackYaw;

      if (MovementFix.shouldFixMovement())
         return getMovementDirection(MovementFix.getMovementYaw(), input);

      return MovementUtil.getPlayerDirection();
   }

   public static Input applyInputSprint(LocalPlayer player, Input input) {
      if (input == null || input.sprint() || !shouldAutoSprint(player, input))
         return input;

      return new Input(
              input.forward(),
              input.backward(),
              input.left(),
              input.right(),
              input.jump(),
              input.shift(),
              true
      );
   }

   public static boolean tryStartSprinting(LocalPlayer player, Input input, boolean ignoreScaffoldNoSprint) {
      if (player == null || input == null || player.isSprinting())
         return false;

      if (Scaffold2.shouldSuppressTellyNormalJumpSprintHook())
         return false;

      if (ignoreScaffoldNoSprint) {
         Sprint2 module = inst();
         if (!canStartSprintingLikeVanilla(player, input) || module != null && module.wallCheck.getValue() && player.horizontalCollision)
            return false;

         player.setSprinting(true);
         return true;
      }

      if (!shouldAutoSprint(player, input, ignoreScaffoldNoSprint) && !canStartPhysicalSprint(player, input))
         return false;

      player.setSprinting(true);
      return true;
   }

   private static boolean shouldAutoSprint(LocalPlayer player, Input input) {
      return shouldAutoSprint(player, input, false);
   }

   private static boolean shouldAutoSprint(LocalPlayer player, Input input, boolean ignoreScaffoldNoSprint) {
      Sprint2 module = inst();
      return module != null
              && module.isEnabled()
              && module.autoSprint.getValue()
              && player != null
              && input != null
              && !Scaffold2.shouldSuppressTellyNormalJumpSprintHook()
              && canStartSprintingLikeVanilla(player, input)
              && (ignoreScaffoldNoSprint || !Scaffold2.isNoSprintEnabled())
              && (!module.wallCheck.getValue() || !player.horizontalCollision);
   }

   private static boolean canStartPhysicalSprint(LocalPlayer player, Input input) {
      Sprint2 module = inst();
      return input.sprint()
              && canStartSprintingLikeVanilla(player, input)
              && (module == null || !module.wallCheck.getValue() || !player.horizontalCollision);
   }

   private static boolean canStartSprintingLikeVanilla(LocalPlayer player, Input input) {
      if (player == null || input == null)
         return false;

      LocalPlayerSprintAccessor sprintAccessor = (LocalPlayerSprintAccessor) player;
      boolean noSlowActive = NoSlow2.noSlowHook();

      if (player.isSprinting()
              || !hasSprintImpulse(input)
              || !sprintAccessor.invokeIsSprintingPossible(player.getAbilities().flying)
              || sprintAccessor.invokeIsSlowDueToUsingItem() && !noSlowActive)
         return false;

      if (player.isFallFlying() && !player.isUnderWater())
         return false;

      return !player.isMovingSlowly() || player.isUnderWater() || noSlowActive && NoSlow2.shouldNoSlowSneak();
   }

   private static boolean hasSprintImpulse(Input input) {
      Sprint2 module = inst();
      return module != null && module.multiDirection.getValue() ? isMoving(input) : hasForwardImpulse(input);
   }

   private static boolean isMoving(Input input) {
      return input.forward() != input.backward() || input.left() != input.right();
   }

   private static boolean hasForwardImpulse(Input input) {
      return calculateImpulse(input.forward(), input.backward()) > 1.0E-5F;
   }

   // MC 26.3 keeps KeyboardInput#calculateImpulse private; identical vanilla logic.
   private static float calculateImpulse(boolean positive, boolean negative) {
      return positive == negative ? 0.0F : (positive ? 1.0F : -1.0F);
   }

   private static float getMovementDirection(float facingYaw, Input input) {
      if (input == null)
         return facingYaw;

      float yaw = facingYaw;
      float forwardMultiplier;

      if (input.backward() && !input.forward()) {
         yaw += 180f;
         forwardMultiplier = -0.5f;
      } else if (input.forward() && !input.backward()) {
         forwardMultiplier = 0.5f;
      } else {
         forwardMultiplier = 1f;
      }

      if (input.left() && !input.right())
         yaw -= 90f * forwardMultiplier;
      if (input.right() && !input.left())
         yaw += 90f * forwardMultiplier;

      return (yaw % 360f + 360f) % 360f;
   }

   public static boolean keepSprintHook(Player entity) {
      Sprint2 module = inst();
      return !(module != null && module.isEnabled() && module.keepSprint.getValue() && entity instanceof LocalPlayer);
   }

   // Resolve the active Sprint2 instance so static methods can read settings/state.
   private static Sprint2 inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof Sprint2 s) return s;
      }
      return null;
   }
}
