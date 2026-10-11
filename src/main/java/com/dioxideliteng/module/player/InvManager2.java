package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.InvManager) and adapted.
 *  HypixelDisabler.isInventoryMovePausedForChest() is stubbed to false (no client Hypixel
 *  disabler). ToolUtil armor helpers (isArmor / getArmorEquipmentSlot / isBetterArmor /
 *  getItemSlotId) are inlined via DataComponents.EQUIPPABLE and attribute/enchantment
 *  comparison. RandomUtil delays use ThreadLocalRandom. The creative double-drop branch
 *  is simplified: creative players skip trash drops entirely. Hooks Events.ROTATION (the
 *  client's proven inventory-screen tick) with a per-tick dedup, mirroring onUpdate. */
public class InvManager2 extends Feature {
   private final NumberSetting startDelay = new NumberSetting("Start Delay", this, 2, 0, 10, 1);
   private final NumberSetting maxItemDelay = new NumberSetting("Max Item Delay", this, 2, 0, 10, 1);
   private final NumberSetting minItemDelay = new NumberSetting("Min Item Delay", this, 0, 0, 10, 1);
   private final NumberSetting maxArmorDelay = new NumberSetting("Max Armor Delay", this, 2, 0, 10, 1);
   private final NumberSetting minArmorDelay = new NumberSetting("Min Armor Delay", this, 0, 0, 10, 1);
   private final BooleanSetting cleaner = new BooleanSetting("Cleaner", this, true);
   private final BooleanSetting onlyInInventory = new BooleanSetting("Only in Inventory", this, false);

   private int tickCounter = 0;
   private int startCounter = 0;
   private long lastTick = Long.MIN_VALUE;
   private final ItemStack[] hotbarTargets = new ItemStack[9];
   private final ItemStack[] cleanerTargets = new ItemStack[9];
   private final EnumMap<EquipmentSlot, ArmorCandidate> armorCandidates = new EnumMap<>(EquipmentSlot.class);

   private final Map<Item, Integer> itemSlots = Map.ofEntries(
         Map.entry(Items.NETHERITE_SWORD, 0),
         Map.entry(Items.DIAMOND_SWORD, 0),
         Map.entry(Items.IRON_SWORD, 0),
         Map.entry(Items.GOLDEN_SWORD, 0),
         Map.entry(Items.STONE_SWORD, 0),
         Map.entry(Items.WOODEN_SWORD, 0),
         Map.entry(Items.NETHERITE_PICKAXE, 1),
         Map.entry(Items.DIAMOND_PICKAXE, 1),
         Map.entry(Items.IRON_PICKAXE, 1),
         Map.entry(Items.GOLDEN_PICKAXE, 1),
         Map.entry(Items.STONE_PICKAXE, 1),
         Map.entry(Items.WOODEN_PICKAXE, 1),
         Map.entry(Items.NETHERITE_AXE, 2),
         Map.entry(Items.DIAMOND_AXE, 2),
         Map.entry(Items.IRON_AXE, 2),
         Map.entry(Items.GOLDEN_AXE, 2),
         Map.entry(Items.STONE_AXE, 2),
         Map.entry(Items.WOODEN_AXE, 2),
         Map.entry(Items.NETHERITE_SHOVEL, 3),
         Map.entry(Items.DIAMOND_SHOVEL, 3),
         Map.entry(Items.IRON_SHOVEL, 3),
         Map.entry(Items.GOLDEN_SHOVEL, 3),
         Map.entry(Items.STONE_SHOVEL, 3),
         Map.entry(Items.WOODEN_SHOVEL, 3)
   );

   private final Map<Item, Integer> swordMaterialRank = Map.of(
         Items.NETHERITE_SWORD, 6,
         Items.DIAMOND_SWORD, 5,
         Items.IRON_SWORD, 4,
         Items.GOLDEN_SWORD, 3,
         Items.STONE_SWORD, 2,
         Items.WOODEN_SWORD, 1
   );

   private final Map<Item, Double> swordAttackDamageFallback = Map.of(
         Items.NETHERITE_SWORD, 8.0,
         Items.DIAMOND_SWORD, 7.0,
         Items.IRON_SWORD, 6.0,
         Items.STONE_SWORD, 5.0,
         Items.GOLDEN_SWORD, 4.0,
         Items.WOODEN_SWORD, 4.0
   );

   private final Map<Item, Integer> pickaxeMaterialRank = Map.of(
         Items.NETHERITE_PICKAXE, 6,
         Items.DIAMOND_PICKAXE, 5,
         Items.IRON_PICKAXE, 4,
         Items.GOLDEN_PICKAXE, 3,
         Items.STONE_PICKAXE, 2,
         Items.WOODEN_PICKAXE, 1
   );

   private final Map<Item, Integer> axeMaterialRank = Map.of(
         Items.NETHERITE_AXE, 6,
         Items.DIAMOND_AXE, 5,
         Items.IRON_AXE, 4,
         Items.GOLDEN_AXE, 3,
         Items.STONE_AXE, 2,
         Items.WOODEN_AXE, 1
   );

   private final Map<Item, Integer> shovelMaterialRank = Map.of(
         Items.NETHERITE_SHOVEL, 6,
         Items.DIAMOND_SHOVEL, 5,
         Items.IRON_SHOVEL, 4,
         Items.GOLDEN_SHOVEL, 3,
         Items.STONE_SHOVEL, 2,
         Items.WOODEN_SHOVEL, 1
   );

   public InvManager2() {
      super("InvManager2", Category.PLAYER);
   }

   @Override
   public void onDisable() {
      this.tickCounter = 0;
      this.startCounter = 0;
      this.lastTick = Long.MIN_VALUE;
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
      if (player == null || gameMode == null || gameMode.getPlayerMode() == GameType.SPECTATOR) return;

      var screen = mc.gui.screen();
      if ((this.onlyInInventory.getValue() && !(screen instanceof InventoryScreen))
            || screen instanceof ContainerScreen
            || screen instanceof FurnaceScreen
            || false /* HypixelDisabler.isInventoryMovePausedForChest() - no client equivalent */) {
         this.tickCounter = 0;
         this.startCounter = 0;
         return;
      }

      if (this.lastTick == player.tickCount) return;
      this.lastTick = player.tickCount;

      if (this.startCounter < (int) this.startDelay.getValue()) {
         this.startCounter++;
         return;
      }

      if (this.tickCounter > 0) {
         this.tickCounter--;
         return;
      }

      var inventory = player.getInventory();
      java.util.Arrays.fill(this.hotbarTargets, null);

      for (int i = 0; i < Math.min(36, inventory.getContainerSize()); i++) {
         ItemStack stack = inventory.getItem(i);
         Integer slot = this.itemSlots.get(stack.getItem());
         if (slot != null) {
            ItemStack targetStack = this.hotbarTargets[slot];
            if (targetStack == null || compareItems(stack, targetStack) > 0)
               this.hotbarTargets[slot] = stack;
         }
      }

      for (int slot = 0; slot < this.hotbarTargets.length; slot++) {
         ItemStack targetStack = this.hotbarTargets[slot];
         if (targetStack == null) continue;
         int hotbarIndex = findItemInHotbar(inventory, targetStack);

         if (hotbarIndex != slot) {
            int inventoryIndex = -1;
            for (int i = 0; i < Math.min(36, inventory.getContainerSize()); i++) {
               ItemStack stack = inventory.getItem(i);
               if (stack.getItem() == targetStack.getItem() && compareItems(stack, targetStack) == 0) {
                  inventoryIndex = i;
                  break;
               }
            }

            if (inventoryIndex != -1) {
               swapItems(player, gameMode, inventoryIndex, slot);
               this.tickCounter = nextItemDelay();
               return;
            }
         }
      }

      this.armorCandidates.clear();

      for (int i = 0; i <= 35; i++) {
         ItemStack stack = inventory.getItem(i);
         EquipmentSlot armorEquipSlot = armorSlot(stack);
         if (armorEquipSlot != null) {
            ArmorCandidate currentBest = this.armorCandidates.get(armorEquipSlot);
            if (currentBest == null) {
               this.armorCandidates.put(armorEquipSlot, new ArmorCandidate(stack, i));
            } else if (isBetterArmor(stack, currentBest.stack, armorEquipSlot)) {
               currentBest.stack = stack;
               currentBest.inventoryIndex = i;
            }
         }
      }

      for (Map.Entry<EquipmentSlot, ArmorCandidate> entry : this.armorCandidates.entrySet()) {
         EquipmentSlot slot = entry.getKey();
         ArmorCandidate candidate = entry.getValue();
         ItemStack bestArmor = candidate.stack;
         int inventoryIndex = candidate.inventoryIndex;
         ItemStack currentArmor = player.getItemBySlot(slot);

         if (currentArmor.isEmpty() || isBetterArmor(bestArmor, currentArmor, slot)) {
            if (!currentArmor.isEmpty()) {
               dropItem(player, gameMode, armorMenuSlot(slot), false);
               this.tickCounter = nextArmorDelay();
               return;
            }

            int slotIndex = inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
            gameMode.handleContainerInput(player.containerMenu.containerId, slotIndex, 0, ContainerInput.QUICK_MOVE, player);
            this.tickCounter = nextArmorDelay();
            return;
         }
      }

      if (this.cleaner.getValue()) {
         java.util.Arrays.fill(this.cleanerTargets, null);

         for (int i = 0; i <= 35; i++) {
            ItemStack stack = inventory.getItem(i);
            Integer slot = this.itemSlots.get(stack.getItem());
            if (slot != null) {
               ItemStack targetStack = this.cleanerTargets[slot];
               if (targetStack == null || compareItems(stack, targetStack) > 0)
                  this.cleanerTargets[slot] = stack;
            }
         }

         for (int i = 0; i <= 35; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()
                  && (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem) || stack.is(Items.CHEST))
                  && !stack.has(DataComponents.POTION_CONTENTS)
                  && !(stack.getItem() instanceof net.minecraft.world.item.SpawnEggItem)
                  && !isAllowedCleanerItem(stack.getItem())
                  && !containsCleanerTarget(stack)) {
               int slot = i < 9 ? i + 36 : i;
               dropItem(player, gameMode, slot, stack.getCount() > 1);
               this.tickCounter = nextItemDelay();
               return;
            }
         }
      }
   }

   private int findItemInHotbar(net.minecraft.world.entity.player.Inventory inventory, ItemStack targetStack) {
      for (int i = 0; i <= 8; i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.getItem() == targetStack.getItem() && compareItems(stack, targetStack) == 0)
            return i;
      }
      return -1;
   }

   private int compareItems(ItemStack stack1, ItemStack stack2) {
      Item item1 = stack1.getItem();
      Item item2 = stack2.getItem();

      int rank1 = 0;
      int rank2 = 0;

      if (this.swordMaterialRank.containsKey(item1) && this.swordMaterialRank.containsKey(item2)) {
         int attackDamageComparison = Double.compare(getSwordAttackDamage(stack1), getSwordAttackDamage(stack2));
         if (attackDamageComparison != 0)
            return attackDamageComparison;

         rank1 = this.swordMaterialRank.getOrDefault(item1, 0);
         rank2 = this.swordMaterialRank.getOrDefault(item2, 0);
      } else if (this.pickaxeMaterialRank.containsKey(item1) && this.pickaxeMaterialRank.containsKey(item2)) {
         rank1 = this.pickaxeMaterialRank.getOrDefault(item1, 0);
         rank2 = this.pickaxeMaterialRank.getOrDefault(item2, 0);
      } else if (this.axeMaterialRank.containsKey(item1) && this.axeMaterialRank.containsKey(item2)) {
         rank1 = this.axeMaterialRank.getOrDefault(item1, 0);
         rank2 = this.axeMaterialRank.getOrDefault(item2, 0);
      } else if (this.shovelMaterialRank.containsKey(item1) && this.shovelMaterialRank.containsKey(item2)) {
         rank1 = this.shovelMaterialRank.getOrDefault(item1, 0);
         rank2 = this.shovelMaterialRank.getOrDefault(item2, 0);
      }

      if (rank1 != rank2)
         return Integer.compare(rank1, rank2);

      if (item1 == item2) {
         int enchantments1 = getEnchantmentScore(stack1);
         int enchantments2 = getEnchantmentScore(stack2);
         if (enchantments1 != enchantments2)
            return Integer.compare(enchantments1, enchantments2);
      }

      return 0;
   }

   private int getEnchantmentScore(ItemStack stack) {
      int score = 0;
      for (var entry : stack.getEnchantments().entrySet())
         score += entry.getIntValue();
      return score;
   }

   private boolean containsCleanerTarget(ItemStack stack) {
      for (ItemStack target : this.cleanerTargets) {
         if (stack.equals(target))
            return true;
      }
      return false;
   }

   private static boolean isAllowedCleanerItem(Item item) {
      return item == Items.ENDER_PEARL
            || item == Items.ENDER_EYE
            || item == Items.TRIDENT
            || item == Items.MACE
            || item == Items.BOW
            || item == Items.CROSSBOW
            || item == Items.ARROW
            || item == Items.GOLDEN_APPLE
            || item == Items.ENCHANTED_GOLDEN_APPLE
            || item == Items.APPLE
            || item == Items.MUSHROOM_STEW
            || item == Items.BREAD
            || item == Items.PORKCHOP
            || item == Items.COOKED_PORKCHOP
            || item == Items.GOLDEN_CARROT
            || item == Items.CARROT
            || item == Items.POTATO
            || item == Items.BAKED_POTATO
            || item == Items.COOKED_BEEF
            || item == Items.BEEF
            || item == Items.COOKED_CHICKEN
            || item == Items.CHICKEN
            || item == Items.COOKED_MUTTON
            || item == Items.MUTTON
            || item == Items.COOKED_RABBIT
            || item == Items.RABBIT
            || item == Items.RABBIT_STEW
            || item == Items.BEETROOT
            || item == Items.BEETROOT_SOUP
            || item == Items.MELON_SLICE
            || item == Items.PUMPKIN_PIE
            || item == Items.COOKIE
            || item == Items.SWEET_BERRIES
            || item == Items.COD
            || item == Items.COOKED_COD
            || item == Items.SALMON
            || item == Items.COOKED_SALMON
            || item == Items.TROPICAL_FISH
            || item == Items.PUFFERFISH
            || item == Items.HONEY_BOTTLE
            || item == Items.GLOW_BERRIES
            || item == Items.DRIED_KELP
            || item == Items.ROTTEN_FLESH
            || item == Items.POISONOUS_POTATO
            || item == Items.COMPASS
            || item == Items.RECOVERY_COMPASS
            || item == Items.WATER_BUCKET
            || item == Items.ELYTRA
            || item == Items.IRON_INGOT
            || item == Items.DIAMOND
            || item == Items.EMERALD
            || item == Items.GOLD_INGOT
            || item == Items.NETHERITE_INGOT
            || item == Items.SHIELD
            || item == Items.BLAZE_POWDER
            || item == Items.PAPER
            || item == Items.FISHING_ROD
            || item == Items.BUCKET;
   }

   private double getSwordAttackDamage(ItemStack stack) {
      double baseAttackDamage = mc.player != null ? mc.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) : 1.0;
      double[] attackDamage = {baseAttackDamage};
      boolean[] hasAttackDamageModifier = {false};

      stack.forEachModifier(EquipmentSlot.MAINHAND, (Holder<Attribute> attribute, AttributeModifier modifier) -> {
         if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
            hasAttackDamageModifier[0] = true;
            attackDamage[0] += modifier.amount();
         }
      });

      if (!hasAttackDamageModifier[0])
         attackDamage[0] = this.swordAttackDamageFallback.getOrDefault(stack.getItem(), 0.0);

      return attackDamage[0] + getSharpnessDamageBonus(stack.getEnchantments());
   }

   private double getSharpnessDamageBonus(net.minecraft.world.item.enchantment.ItemEnchantments itemEnchantments) {
      int level = getEnchantmentLevel(itemEnchantments, Enchantments.SHARPNESS);
      if (level <= 0) return 0.0;

      return 1.0 + 0.5 * (level - 1);
   }

   private int getEnchantmentLevel(net.minecraft.world.item.enchantment.ItemEnchantments itemEnchantments, ResourceKey<Enchantment> enchantment) {
      for (var entry : itemEnchantments.entrySet()) {
         if (entry.getKey().is(enchantment)) return entry.getIntValue();
      }
      return 0;
   }

   private int nextItemDelay() {
      return nextDelay((int) this.minItemDelay.getValue(), (int) this.maxItemDelay.getValue());
   }

   private int nextArmorDelay() {
      return nextDelay((int) this.minArmorDelay.getValue(), (int) this.maxArmorDelay.getValue());
   }

   private int nextDelay(int min, int max) {
      int lower = Math.min(min, max);
      int upper = Math.max(min, max);
      return ThreadLocalRandom.current().nextInt(lower, upper + 1);
   }

   private void swapItems(LocalPlayer player, MultiPlayerGameMode gameMode, int from, int to) {
      int syncId = player.containerMenu.containerId;
      int fromSlot = from < 9 ? from + 36 : from;
      if (fromSlot != to + 36)
         gameMode.handleContainerInput(syncId, fromSlot, to, ContainerInput.SWAP, player);
   }

   private void dropItem(LocalPlayer player, MultiPlayerGameMode gameMode, int slot, boolean wholeStack) {
      if (player.getAbilities().instabuild) return;
      int button = wholeStack ? 1 : 0;
      gameMode.handleContainerInput(player.containerMenu.containerId, slot, button, ContainerInput.THROW, player);
   }

   private static EquipmentSlot armorSlot(ItemStack stack) {
      Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
      if (equippable == null) return null;
      EquipmentSlot slot = equippable.slot();
      return slot != EquipmentSlot.BODY && slot.isArmor() && armorAttribute(stack, slot) > 0 ? slot : null;
   }

   private static int armorMenuSlot(EquipmentSlot slot) {
      return switch (slot) {
         case HEAD -> 5;
         case CHEST -> 6;
         case LEGS -> 7;
         case FEET -> 8;
         default -> throw new IllegalArgumentException();
      };
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

   private static final class ArmorCandidate {
      private ItemStack stack;
      private int inventoryIndex;

      private ArmorCandidate(ItemStack stack, int inventoryIndex) {
         this.stack = stack;
         this.inventoryIndex = inventoryIndex;
      }
   }
}
