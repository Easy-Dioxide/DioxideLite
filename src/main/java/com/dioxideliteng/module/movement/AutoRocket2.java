package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.PacketUtil;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Port of Krs AutoRocket (com.instrumentalist.krs.hacks.features.movement.AutoRocket) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event/packet API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - Krs {@code tag()} -> {@link Feature#setSuffix(String)} refreshed on UPDATE;
 *  - {@code ServerboundSwingPacket} was removed in 26.3, so the post-use swing uses
 *    {@code player.swing(InteractionHand.MAIN_HAND)} exactly as Scaffold2 does.
 */
public class AutoRocket2 extends Feature {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Low Speed", new String[]{"Low Speed", "Jump", "Always"});
   private final NumberSetting speed = new NumberSetting("Speed", this, 0.7, 0.1, 3.0, 0.1);
   private final NumberSetting delay = new NumberSetting("Delay", this, 1500, 100, 5000, 1);

   private final MSTimer rocketTimer = new MSTimer();

   public AutoRocket2() {
      super("AutoRocket2", Category.MOVEMENT);
      this.speed.setVisible(() -> this.mode.is("Low Speed"));
   }

   @Override
   public void onEnable() {
      rocketTimer.reset();
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;
      setSuffix(this.mode.getValue());

      var player = mc.player;
      var gameMode = mc.gameMode;
      if (player == null || gameMode == null || !player.isFallFlying() || mc.gui.screen() != null || !rocketTimer.hasTimePassed((long) this.delay.getValue()) || !shouldUse())
         return;

      int slot = findRocketSlot();
      if (slot == -1)
         return;

      int oldSlot = player.getInventory().getSelectedSlot();
      if (slot != oldSlot) {
         player.getInventory().setSelectedSlot(slot);
         PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(slot));
      }

      if (gameMode.useItem(player, InteractionHand.MAIN_HAND).consumesAction())
         player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

      if (slot != oldSlot) {
         player.getInventory().setSelectedSlot(oldSlot);
         PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(oldSlot));
      }

      rocketTimer.reset();
   }

   private boolean shouldUse() {
      String currentMode = this.mode.getValue().toLowerCase(Locale.ROOT);
      return switch (currentMode) {
         case "jump" -> mc.options.keyJump.isDown();
         case "always" -> true;
         default -> MovementUtil.getSpeed() <= this.speed.getValue();
      };
   }

   private int findRocketSlot() {
      var player = mc.player;
      if (player == null)
         return -1;

      for (int i = 0; i <= 8; i++) {
         if (player.getInventory().getItem(i).getItem() == Items.FIREWORK_ROCKET)
            return i;
      }
      return -1;
   }
}
