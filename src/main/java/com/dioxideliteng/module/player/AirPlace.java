package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventMouseButton;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.AirPlace) and adapted.
 *  Krs intercepts the crosshair via a static mixin hook (createPlacementHit); without
 *  that hook, this port triggers placement directly on a right-click when the crosshair
 *  misses and the player holds a block. */
public class AirPlace extends Feature {

   public AirPlace() {
      super("AirPlace", Category.PLAYER);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.MOUSE_BUTTON) return;
      var mouse = (EventMouseButton) event;
      if (!mouse.isRightButton() || !mouse.isPressed()) return;
      if (mc.gui.screen() != null || mc.player == null || mc.gameMode == null) return;

      var hit = mc.hitResult;
      if (!(hit instanceof BlockHitResult miss) || miss.getType() != HitResult.Type.MISS) return;

      InteractionHand hand = null;
      if (mc.player.getMainHandItem().getItem() instanceof BlockItem) hand = InteractionHand.MAIN_HAND;
      else if (mc.player.getOffhandItem().getItem() instanceof BlockItem) hand = InteractionHand.OFF_HAND;
      if (hand == null) return;

      BlockPos target = BlockPos.containing(miss.getLocation());
      BlockHitResult placeHit = new BlockHitResult(miss.getLocation(), miss.getDirection(), target, false);
      if (mc.gameMode.useItemOn(mc.player, hand, placeHit).consumesAction())
         mc.player.swing(hand, SwingAnimation.DEFAULT, false);
      mouse.setCancelled(true);
   }
}
