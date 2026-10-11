package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.AntiAFK) and adapted.
 *  Krs rotationManager.normalizeRotation is dropped (turn applied directly);
 *  Krs notificationManager toasts fall back to a chat message when enabled. */
public class AntiAFK extends Feature {
   private static final String SWING = "Swing";
   private static final String ROTATE = "Rotate";
   private static final String JUMP = "Jump";
   private static final String CYCLE = "Cycle";

   private final ModeSetting action = new ModeSetting("Action", this, CYCLE, new String[]{SWING, ROTATE, JUMP, CYCLE});
   private final NumberSetting interval = new NumberSetting("Interval", this, 5.0, 1.0, 300.0, 1.0);
   private final BooleanSetting pauseWhileHurt = new BooleanSetting("Pause While Hurt", this, true);
   private final BooleanSetting notifications = new BooleanSetting("Notifications", this, false);

   private int idleTicks;
   private int cycleIndex;
   private float lastYaw;
   private float lastPitch;
   private boolean initialized;
   private boolean rotateRight;

   public AntiAFK() {
      super("AntiAFK", Category.PLAYER);
   }

   @Override
   public void onDisable() {
      reset();
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.WORLD) {
         reset();
         return;
      }
      if (event != Events.TICK) return;

      this.setSuffix(this.action.getValue());

      var player = mc.player;
      if (player == null || player.isDeadOrDying()) {
         reset();
         return;
      }

      float yaw = player.getYRot();
      float pitch = player.getXRot();
      if (!this.initialized) {
         this.lastYaw = yaw;
         this.lastPitch = pitch;
         this.initialized = true;
         return;
      }

      boolean cameraMoved = Math.abs(Mth.wrapDegrees(yaw - this.lastYaw)) > 0.05f
            || Math.abs(pitch - this.lastPitch) > 0.05f;
      this.lastYaw = yaw;
      this.lastPitch = pitch;

      if (hasManualInput() || cameraMoved || this.pauseWhileHurt.getValue() && player.hurtTime > 0) {
         this.idleTicks = 0;
         return;
      }

      if (++this.idleTicks < (int) (this.interval.getValue() * 20)) return;

      performConfiguredAction();
      this.idleTicks = 0;
   }

   private boolean hasManualInput() {
      return mc.options.keyUp.isDown()
            || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown()
            || mc.options.keyRight.isDown()
            || mc.options.keyJump.isDown()
            || mc.options.keyShift.isDown()
            || mc.options.keySprint.isDown()
            || mc.options.keyAttack.isDown()
            || mc.options.keyUse.isDown();
   }

   private void performConfiguredAction() {
      String selectedAction = this.action.getValue().toLowerCase(Locale.ROOT);
      if (selectedAction.equals(CYCLE.toLowerCase(Locale.ROOT))) {
         selectedAction = switch (this.cycleIndex++ % 3) {
            case 1 -> ROTATE.toLowerCase(Locale.ROOT);
            case 2 -> JUMP.toLowerCase(Locale.ROOT);
            default -> SWING.toLowerCase(Locale.ROOT);
         };
      }

      switch (selectedAction) {
         case "rotate" -> rotate();
         case "jump" -> jump();
         default -> swing();
      }

      if (this.notifications.getValue() && mc.player != null)
         mc.player.sendSystemMessage(Component.literal("Anti AFK: Performed " + selectedAction));
   }

   private void swing() {
      if (mc.player != null)
         mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
   }

   private void rotate() {
      var player = mc.player;
      if (player == null) return;

      float offset = this.rotateRight ? 3.0f : -3.0f;
      this.rotateRight = !this.rotateRight;
      float currentYaw = player.getYRot();
      float targetYaw = currentYaw + offset;

      player.turn(Mth.wrapDegrees(targetYaw - currentYaw) / 0.15D, 0.0D);
   }

   private void jump() {
      if (mc.player != null)
         mc.player.input.makeJump();
   }

   private void reset() {
      this.idleTicks = 0;
      this.cycleIndex = 0;
      this.lastYaw = 0.0f;
      this.lastPitch = 0.0f;
      this.initialized = false;
      this.rotateRight = false;
   }
}
