package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.PacketUtil;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.FastEat).
 */
public class FastEat extends Feature {

   private final NumberSetting packets = new NumberSetting("Packets", this, 32.0, 1.0, 32.0, 1.0);
   private final NumberSetting chargeTicks = new NumberSetting("Charge Ticks", this, 1.0, 1.0, 20.0, 1.0);

   public FastEat() {
      super("Fast Eat", Category.COMBAT);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;
      onUpdate();
   }

   private void onUpdate() {
      var player = mc.player;
      var gameMode = mc.gameMode;
      if (player == null || gameMode == null || !player.isUsingItem()) return;

      ItemStack useItem = player.getMainHandItem();
      if (!isFoodOrDrink(useItem)
              || player.getTicksUsingItem() < chargeTicks.getValue())
         return;

      boolean onGround = player.onGround();
      boolean horizontalCollision = player.horizontalCollision;
      int packetCount = (int) packets.getValue();
      for (int i = 0; i < packetCount; i++) {
         PacketUtil.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision));
      }

      player.stopUsingItem();
      if (mc.options.keyUse.isDown() && isFoodOrDrink(player.getMainHandItem()))
         gameMode.useItem(player, InteractionHand.MAIN_HAND);
   }

   private static boolean isFoodOrDrink(ItemStack stack) {
      ItemUseAnimation animation = stack.getUseAnimation();
      return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK;
   }
}
