package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventAttack;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.InputUtil;
import com.dioxideliteng.util.krs.MSTimer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Input;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.WTap).
 * Notable adaptations vs the original:
 *  - MovementFix.getRawInput() has no client port yet; the Krs fallback path
 *    (input.keyPresses) is used directly, which is what Krs reads whenever
 *    MovementFix has not recorded a raw input for this tick.
 *  - KeyboardInput#calculateImpulse is private in MC 26.3, so the identical
 *    vanilla logic is inlined in forwardImpulse.
 *  - The static input hook is called from mixins.MixinKeyboardInput at tick TAIL
 *    (Krs parity: KeyboardInputMixin), after POST_MOVE_INPUT dispatch.
 */
public class WTap extends Feature {
   private static WTap instance;

   private final NumberSetting delay = new NumberSetting("Delay", this, 5.5, 0.0, 10.0, 0.1);
   private final NumberSetting duration = new NumberSetting("Duration", this, 1.5, 1.0, 5.0, 0.1);

   private final MSTimer attackTimer = new MSTimer();
   private boolean active = false;
   private boolean stopForward = false;
   private long delayTicks = 0L;
   private long durationTicks = 0L;

   public WTap() {
      super("WTap", Category.COMBAT);
      instance = this;
   }

   @Override
   public void onDisable() {
      resetActive();
   }

   @Override
   public void onEnable() {
      resetActive();
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.ATTACK) return;
      if (((EventAttack) event).isCancelled()) return;
      onAttack();
   }

   private void onAttack() {
      var player = mc.player;
      if (player == null || active || !attackTimer.hasTimePassed(500L) || !player.isSprinting())
         return;

      attackTimer.reset();
      active = true;
      stopForward = false;
      delayTicks += ticksToMillis(delay.getValue());
      durationTicks += ticksToMillis(duration.getValue());
   }

   public static void hookWTapInput(ClientInput input) {
      WTap wTap = instance;
      if (wTap == null || !wTap.isEnabled())
         return;

      wTap.handleInput(input);
   }

   private void handleInput(ClientInput input) {
      var player = mc.player;
      if (input == null || player == null || player.input != input) {
         resetActive();
         return;
      }

      if (!active)
         return;

      if (!stopForward && !canTrigger(input)) {
         resetActive();
         return;
      }

      if (delayTicks > 0L) {
         delayTicks -= 50L;
         return;
      }

      if (durationTicks > 0L) {
         durationTicks -= 50L;
         stopForward = true;
         stopForward(input);
      }

      if (durationTicks <= 0L)
         resetActive();
   }

   private boolean canTrigger(ClientInput input) {
      var player = mc.player;
      if (player == null)
         return false;

      Input movementInput = input.keyPresses == null ? Input.EMPTY : input.keyPresses;

      return forwardImpulse(movementInput) >= 0.8F
              && !player.horizontalCollision
              && (player.getFoodData().getFoodLevel() > 6.0F || player.getAbilities().flying)
              && (player.isSprinting()
              || !player.isUsingItem() && !player.hasEffect(MobEffects.BLINDNESS) && mc.options.keySprint.isDown());
   }

   private void stopForward(ClientInput input) {
      Input keyPresses = input.keyPresses == null ? Input.EMPTY : input.keyPresses;
      InputUtil.applyInput(input, new Input(
              false,
              false,
              keyPresses.left(),
              keyPresses.right(),
              keyPresses.jump(),
              keyPresses.shift(),
              false
      ));

      var player = mc.player;
      if (player != null && player.isSprinting())
         player.setSprinting(false);
   }

   private void resetActive() {
      active = false;
      stopForward = false;
      delayTicks = 0L;
      durationTicks = 0L;
   }

   private static long ticksToMillis(double ticks) {
      return (long) (50.0 * ticks);
   }

   private static float forwardImpulse(Input input) {
      if (input == null)
         return 0.0F;

      // MC 26.3 keeps KeyboardInput#calculateImpulse private; identical vanilla logic.
      boolean forward = input.forward();
      boolean backward = input.backward();
      return forward == backward ? 0.0F : (forward ? 1.0F : -1.0F);
   }
}
