package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.AutoFish) and adapted.
 *  Krs MSTimer inlined with System.currentTimeMillis; PacketUtil.sendPacket maps to
 *  player.swing (the 26.3 arm-swing path); Krs notificationManager toasts are dropped. */
public class AutoFish extends Feature {
   private static final String MAINHAND = "Mainhand";
   private static final String OFFHAND = "Offhand";

   private final ModeSetting handMode = new ModeSetting("Interaction Hand Mode", this, MAINHAND, new String[]{MAINHAND, OFFHAND});

   private long lastFishMs;

   public AutoFish() {
      super("AutoFish", Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.lastFishMs = 0L;
   }

   private static InteractionHand fishHand(String mode) {
      return mode.equalsIgnoreCase(OFFHAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.handMode.getValue());

      var player = mc.player;
      var gameMode = mc.gameMode;
      if (player == null || gameMode == null) return;

      String mode = this.handMode.getValue();
      InteractionHand hand = fishHand(mode);
      if ((hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem()).getItem() != Items.FISHING_ROD)
         return;

      if (System.currentTimeMillis() - this.lastFishMs < 500L) return;

      var hook = player.fishing;
      boolean canFish = hook == null;
      if (!canFish) {
         Vec3 delta = hook.getDeltaMovement();
         canFish = delta.x == 0.0 && -0.2 >= delta.y && delta.z == 0.0;
      }

      if (!canFish) return;

      if (gameMode.useItem(player, hand).consumesAction())
         player.swing(hand, SwingAnimation.DEFAULT, false);
      this.lastFishMs = System.currentTimeMillis();
   }
}
