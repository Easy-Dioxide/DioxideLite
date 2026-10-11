package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.ChestStealer) and adapted.
 *  Drops the Krs BlinkUtil "blink" option (no client-side packet-blink utility) and the
 *  render-coupled Camera Move helper. ToolUtil armor helpers (isArmor / getArmorEquipmentSlot
 *  / isBetterArmor) are inlined via DataComponents.EQUIPPABLE and attribute comparison.
 *  RandomUtil delays use ThreadLocalRandom. Hooks Events.ROTATION (the client's proven
 *  container-operation tick, which fires while a chest screen is open) with a per-tick dedup. */
public class ChestStealer2 extends Feature {
   private final NumberSetting startDelay = new NumberSetting("Start Delay", this, 2, 0, 10, 1);
   private final NumberSetting maxDelay = new NumberSetting("Max Delay", this, 2, 0, 10, 1);
   private final NumberSetting minDelay = new NumberSetting("Min Delay", this, 0, 0, 10, 1);

   private int tickCounter = 0;
   private int startCounter = 0;
   private int closeCounter = 0;
   private int closeDelayTarget = -1;
   private long lastTick = Long.MIN_VALUE;
   private final ArrayList<Integer> slotIndices = new ArrayList<>(27);

   public ChestStealer2() {
      super("ChestStealer2", Category.PLAYER);
   }

   @Override
   public void onDisable() {
      this.tickCounter = 0;
      this.startCounter = 0;
      this.closeCounter = 0;
      this.closeDelayTarget = -1;
      this.lastTick = Long.MIN_VALUE;
      this.slotIndices.clear();
   }

   @Override
   public void onEnable() {
      onDisable();
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.ROTATION) return;
      var player = mc.player;
      var gameMode = mc.gameMode;
      if (player == null || gameMode == null) {
         onDisable();
         return;
      }
      if (this.lastTick == player.tickCount) return;
      this.lastTick = player.tickCount;

      if (this.tickCounter > 0) {
         this.tickCounter--;
         return;
      }

      if (player.containerMenu instanceof ChestMenu screenHandler) {
         if (this.startCounter < (int) this.startDelay.getValue()) {
            this.startCounter++;
            return;
         }

         int chestSize = screenHandler.getContainer().getContainerSize();
         this.slotIndices.clear();
         for (int i = 0; i < chestSize; i++) this.slotIndices.add(i);
         Collections.shuffle(this.slotIndices);

         boolean itemStolen = false;
         for (int i : this.slotIndices) {
            var slot = screenHandler.getSlot(i);
            if (slot.hasItem() && shouldSteal(slot.getItem(), player)) {
               gameMode.handleContainerInput(screenHandler.containerId, i, 0, ContainerInput.QUICK_MOVE, player);
               itemStolen = true;
               this.tickCounter = nextDelay();
               this.closeCounter = 0;
               this.closeDelayTarget = -1;
               break;
            }
         }

         if (!itemStolen) {
            if (this.closeDelayTarget < 0)
               this.closeDelayTarget = nextDelay();

            if (this.closeCounter < this.closeDelayTarget) {
               this.closeCounter++;
               return;
            }

            this.closeCounter = 0;
            this.closeDelayTarget = -1;
            player.closeContainer();
         }
      } else {
         this.closeCounter = 0;
         this.closeDelayTarget = -1;
         this.startCounter = 0;
      }
   }

   private boolean shouldSteal(ItemStack stack, net.minecraft.client.player.LocalPlayer player) {
      Item item = stack.getItem();
      if (item instanceof BlockItem
            || stack.has(DataComponents.POTION_CONTENTS)
            || item == Items.GOLDEN_APPLE
            || item == Items.ENCHANTED_GOLDEN_APPLE
            || item == Items.ENDER_PEARL
            || item == Items.ENDER_EYE)
         return true;

      EquipmentSlot armorSlot = armorSlot(stack);
      if (armorSlot != null) {
         ItemStack currentArmor = player.getItemBySlot(armorSlot);
         if (!currentArmor.isEmpty() && !isBetterArmor(stack, currentArmor, armorSlot))
            return false;
      }

      var inventory = player.getInventory();
      for (int i = 0; i < Math.min(36, inventory.getContainerSize()); i++) {
         ItemStack invStack = inventory.getItem(i);
         if (ItemStack.matches(invStack, stack) && invStack.getCount() >= stack.getCount())
            return false;
      }

      return true;
   }

   private int nextDelay() {
      int min = (int) this.minDelay.getValue();
      int max = (int) this.maxDelay.getValue();
      int lower = Math.min(min, max);
      int upper = Math.max(min, max);
      return ThreadLocalRandom.current().nextInt(lower, upper + 1);
   }

   private static EquipmentSlot armorSlot(ItemStack stack) {
      Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
      if (equippable == null) return null;
      EquipmentSlot slot = equippable.slot();
      return slot != EquipmentSlot.BODY && slot.isArmor() && armorAttribute(stack, slot) > 0 ? slot : null;
   }

   private static double armorAttribute(ItemStack stack, EquipmentSlot slot) {
      double[] value = {0};
      stack.forEachModifier(slot, (Holder<Attribute> attribute, AttributeModifier modifier) -> {
         if (attribute.equals(Attributes.ARMOR)) value[0] += modifier.amount();
      });
      return value[0];
   }

   private static int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> type) {
      for (var entry : stack.getEnchantments().entrySet())
         if (entry.getKey().is(type)) return entry.getIntValue();
      return 0;
   }

   private static boolean isBetterArmor(ItemStack candidate, ItemStack current, EquipmentSlot slot) {
      double candidateScore = armorAttribute(candidate, slot) + enchantmentLevel(candidate, Enchantments.PROTECTION) * 0.5
            + enchantmentLevel(candidate, Enchantments.UNBREAKING) * 0.25;
      double currentScore = armorAttribute(current, slot) + enchantmentLevel(current, Enchantments.PROTECTION) * 0.5
            + enchantmentLevel(current, Enchantments.UNBREAKING) * 0.25;
      return candidateScore > currentScore;
   }
}
