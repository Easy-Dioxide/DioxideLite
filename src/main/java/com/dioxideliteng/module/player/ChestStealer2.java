package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.ModTextures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
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
 *  container-operation tick, which fires while a chest screen is open) with a per-tick dedup.
 *  The dynamic-island looting visual (shell, item grid, transfer feedback) mirrors the
 *  native ChestStealer via the shared ChestIslandSource contract. */
public class ChestStealer2 extends Feature implements ChestIslandSource {
   private final NumberSetting startDelay = new NumberSetting("Start Delay", this, 2, 0, 10, 1);
   private final NumberSetting maxDelay = new NumberSetting("Max Delay", this, 2, 0, 10, 1);
   private final NumberSetting minDelay = new NumberSetting("Min Delay", this, 0, 0, 10, 1);

   private int tickCounter = 0;
   private int startCounter = 0;
   private int closeCounter = 0;
   private int closeDelayTarget = -1;
   private long lastTick = Long.MIN_VALUE;
   private final ArrayList<Integer> slotIndices = new ArrayList<>(27);
   // Dynamic island state: the menu currently being looted and whether the island panel
   // should stay up (stealing, or inside the close delay before the chest closes).
   private ChestMenu currentMenu;
   private boolean working;
   private final ChestStealer.LootAnimation animation = new ChestStealer.LootAnimation();

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
      this.currentMenu = null;
      this.working = false;
      this.animation.clear();
   }

   @Override
   public void onEnable() {
      onDisable();
   }

   private ChestMenu workingMenu(Screen screen) {
      if (!isEnabled() || mc.player == null || mc.level == null || mc.gameMode == null
            || mc.player.isSpectator()
            || !(screen instanceof ContainerScreen container)
            || !(mc.player.containerMenu instanceof ChestMenu menu) || container.getMenu() != menu)
         return null;
      return menu;
   }

   /** True while this module owns {@code screen}: a chest menu with loot left, or the
    *  close-delay window that preserves the final island frame before the chest closes. */
   @Override
   public boolean replacesContainer(Screen screen) {
      ChestMenu menu = workingMenu(screen);
      if (menu == null || !menu.getCarried().isEmpty()) return false;
      if (this.currentMenu == menu) return this.working;
      return hasStealable(menu);
   }

   private boolean hasStealable(ChestMenu menu) {
      int chestSize = menu.getContainer().getContainerSize();
      for (int i = 0; i < chestSize; i++) {
         var slot = menu.getSlot(i);
         if (slot.hasItem() && shouldSteal(slot.getItem(), mc.player)) return true;
      }
      return false;
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

      ChestMenu menu = workingMenu(mc.gui.screen());
      if (menu == null) {
         this.closeCounter = 0;
         this.closeDelayTarget = -1;
         this.startCounter = 0;
         this.currentMenu = null;
         this.working = false;
         this.animation.clear();
         return;
      }

      if (this.currentMenu != menu) {
         this.currentMenu = menu;
         this.startCounter = 0;
         this.closeCounter = 0;
         this.closeDelayTarget = -1;
      }
      this.animation.attach(menu, menu.getRowCount());

      if (this.startCounter < (int) this.startDelay.getValue()) {
         this.startCounter++;
         return;
      }

      int chestSize = menu.getContainer().getContainerSize();
      this.slotIndices.clear();
      for (int i = 0; i < chestSize; i++) this.slotIndices.add(i);
      Collections.shuffle(this.slotIndices);

      boolean itemStolen = false;
      for (int i : this.slotIndices) {
         var slot = menu.getSlot(i);
         if (slot.hasItem() && shouldSteal(slot.getItem(), player)) {
            ItemStack before = slot.getItem().copy();
            gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, player);
            ItemStack after = slot.getItem();
            if (after.getCount() < before.getCount()) {
               this.animation.transferred(i, before.getCount(), after.getCount(), now());
            }
            itemStolen = true;
            this.working = true;
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
            // Preserve the final island frame through the close delay; the vanilla chest
            // must not flash in between.
            this.working = true;
            return;
         }

         this.closeCounter = 0;
         this.closeDelayTarget = -1;
         this.working = false;
         player.closeContainer();
      }
   }

   @Override
   public ChestStealer.IslandView islandView(Screen screen, long now, boolean reducedMotion) {
      if (!replacesContainer(screen)) {
         this.animation.clear();
         return null;
      }
      ChestMenu menu = workingMenu(screen);
      this.animation.attach(menu, menu.getRowCount());
      List<ItemStack> items = new ArrayList<>(menu.getContainer().getContainerSize());
      for (int slot = 0; slot < menu.getContainer().getContainerSize(); slot++) {
         items.add(menu.getSlot(slot).getItem().copy());
      }
      return new ChestStealer.IslandView(menu.getRowCount(), List.copyOf(items),
         reducedMotion ? List.of() : this.animation.feedback(now));
   }

   @Override
   public void extractIslandItems(GuiGraphicsExtractor graphics, ChestStealer.IslandView view, ChestStealer.IslandGeometry geometry) {
      var frame = geometry.frame();
      if (frame.width() <= 2 || frame.height() <= 2) return;
      float left = geometry.left() * geometry.scale(), top = frame.top();
      graphics.pose().pushMatrix();
      try {
         graphics.pose().translate(left, top);
         graphics.pose().scale(geometry.scale(), geometry.scale());
         extractIslandShell(graphics, frame.width(), frame.height());
      } finally {
         graphics.pose().popMatrix();
      }
      graphics.enableScissor((int)Math.ceil(left + geometry.scale()), (int)Math.ceil(top + geometry.scale()),
         (int)Math.floor(left + (frame.width() - 1) * geometry.scale()),
         (int)Math.floor(top + (frame.height() - 1) * geometry.scale()));
      graphics.pose().pushMatrix();
      try {
         graphics.pose().translate(geometry.contentLeft() * geometry.scale(), top);
         graphics.pose().scale(geometry.scale(), geometry.scale());
         graphics.nextStratum();
         for (int slot = 0; slot < view.items().size(); slot++) {
            ItemStack stack = view.items().get(slot);
            if (stack.isEmpty()) continue;
            int x = ChestStealer.IslandGeometry.slotX(slot), y = ChestStealer.IslandGeometry.slotY(slot);
            graphics.item(stack, x, y);
            graphics.itemDecorations(mc.font, stack, x, y);
         }
      } finally {
         graphics.pose().popMatrix();
         graphics.disableScissor();
      }
   }

   private static void extractIslandShell(GuiGraphicsExtractor graphics, float width, float height) {
      var texture = ModTextures.register("textures/hud/chest-island.png");
      // Nine slices preserve corner/shadow radii while the spring changes both dimensions.
      for (ChestStealer.ShellSlice slice : ChestStealer.IslandGeometry.shellSlices(width, height)) {
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(slice.x(), slice.y());
            graphics.pose().scale(slice.width() / slice.sourceWidth(), slice.height() / slice.sourceHeight());
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0,
               slice.sourceX() * 4, slice.sourceY() * 4, slice.sourceWidth(), slice.sourceHeight(),
               slice.sourceWidth() * 4, slice.sourceHeight() * 4, 224, 224);
         } finally {
            graphics.pose().popMatrix();
         }
      }
   }

   private static long now() { return System.nanoTime() / 1_000_000L; }

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
