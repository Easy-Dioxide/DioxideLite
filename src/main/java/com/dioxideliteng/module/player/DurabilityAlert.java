package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import java.util.EnumMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.DurabilityAlert) and adapted.
 *  Krs notificationManager/ChatUtil alerts fall back to a local chat message. */
public class DurabilityAlert extends Feature {
   private static final EquipmentSlot[] ARMOR_SLOTS = {
         EquipmentSlot.HEAD,
         EquipmentSlot.CHEST,
         EquipmentSlot.LEGS,
         EquipmentSlot.FEET
   };
   private static final EquipmentSlot[] HAND_SLOTS = {
         EquipmentSlot.MAINHAND,
         EquipmentSlot.OFFHAND
   };

   private final NumberSetting threshold = new NumberSetting("Threshold", this, 15.0, 1.0, 50.0, 1.0);
   private final NumberSetting repeatDelay = new NumberSetting("Repeat Delay", this, 30.0, 0.0, 300.0, 1.0);
   private final BooleanSetting armor = new BooleanSetting("Armor", this, true);
   private final BooleanSetting hands = new BooleanSetting("Hands", this, true);

   private final EnumMap<EquipmentSlot, SlotState> slotStates = new EnumMap<>(EquipmentSlot.class);
   private int checkTicks;

   public DurabilityAlert() {
      super("DurabilityAlert", Category.PLAYER);
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
      this.setSuffix((int) this.threshold.getValue() + "%");

      if (mc.player == null) {
         reset();
         return;
      }

      if (++this.checkTicks < 5) return;
      this.checkTicks = 0;

      if (this.armor.getValue()) {
         for (EquipmentSlot slot : ARMOR_SLOTS) checkSlot(slot);
      } else {
         for (EquipmentSlot slot : ARMOR_SLOTS) this.slotStates.remove(slot);
      }

      if (this.hands.getValue()) {
         for (EquipmentSlot slot : HAND_SLOTS) checkSlot(slot);
      } else {
         for (EquipmentSlot slot : HAND_SLOTS) this.slotStates.remove(slot);
      }
   }

   private void checkSlot(EquipmentSlot slot) {
      ItemStack stack = mc.player.getItemBySlot(slot);
      if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
         this.slotStates.remove(slot);
         return;
      }

      int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
      int remainingPercent = (int) Math.ceil(remaining * 100.0 / stack.getMaxDamage());
      SlotState state = this.slotStates.get(slot);
      if (state == null || state.item != stack.getItem() || state.maxDamage != stack.getMaxDamage()) {
         state = new SlotState(stack.getItem(), stack.getMaxDamage());
         this.slotStates.put(slot, state);
      }

      if (remainingPercent > Math.min(100, (int) this.threshold.getValue() + 5)) {
         state.warned = false;
         state.lastAlertNanos = 0L;
      }

      long now = System.nanoTime();
      long repeatNanos = (long) this.repeatDelay.getValue() * 1_000_000_000L;
      boolean repeatReady = this.repeatDelay.getValue() > 0
            && state.lastAlertNanos > 0L
            && now - state.lastAlertNanos >= repeatNanos;
      if (remainingPercent <= this.threshold.getValue() && (!state.warned || repeatReady)) {
         String message = slotName(slot) + " " + stack.getHoverName().getString()
               + ": " + remaining + " durability (" + remainingPercent + "%)";
         mc.player.sendSystemMessage(Component.literal(message));
         state.warned = true;
         state.lastAlertNanos = now;
      }
   }

   private String slotName(EquipmentSlot slot) {
      return switch (slot) {
         case HEAD -> "Helmet";
         case CHEST -> "Chest";
         case LEGS -> "Leggings";
         case FEET -> "Boots";
         case MAINHAND -> "Main hand";
         case OFFHAND -> "Offhand";
         default -> slot.getName();
      };
   }

   private void reset() {
      this.slotStates.clear();
      this.checkTicks = 0;
   }

   private static final class SlotState {
      private final Item item;
      private final int maxDamage;
      private boolean warned;
      private long lastAlertNanos;

      private SlotState(Item item, int maxDamage) {
         this.item = item;
         this.maxDamage = maxDamage;
      }
   }
}
