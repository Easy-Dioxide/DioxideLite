package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.PotionContents;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.AutoPot) and adapted to use Events.ROTATION. */
public class AutoPot extends Feature {
   private final List<Holder<MobEffect>> potions = List.of(
         MobEffects.SPEED, MobEffects.RESISTANCE, MobEffects.REGENERATION, MobEffects.FIRE_RESISTANCE,
         MobEffects.INVISIBILITY, MobEffects.WATER_BREATHING, MobEffects.STRENGTH, MobEffects.JUMP_BOOST,
         MobEffects.INSTANT_HEALTH);
   private final ArrayList<Holder<MobEffect>> potionOrder = new ArrayList<>(this.potions.size());
   private final ArrayList<Holder<MobEffect>> needPots = new ArrayList<>(this.potions.size());

   private boolean wasRotating;
   private boolean potted;
   private boolean resetEffect;
   private int oldSlot = -1;
   private int rotTicks;
   private int potTimer;

   public AutoPot() {
      super("Auto Pot", Category.COMBAT);
   }

   @Override
   public void onDisable() {
      cleanup();
   }

   private void cleanup() {
      if (this.wasRotating && this.oldSlot != -1 && mc.player != null) {
         mc.player.getInventory().setSelectedSlot(this.oldSlot);
      }
      this.oldSlot = -1;
      this.wasRotating = false;
      this.potted = false;
      this.resetEffect = false;
      this.rotTicks = 0;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         if (this.wasRotating && mc.player != null) {
            Events.ROTATION.setYaw(mc.player.getYRot());
            Events.ROTATION.setPitch(90.0F);
            this.rotTicks++;
         }
         return;
      }
      if (event != Events.TICK) return;
      var player = mc.player;
      if (player == null
            || !player.onGround() && !this.wasRotating
            || mc.gui.screen() instanceof ContainerScreen
            || this.resetEffect
            || this.potted
            || FeatureManager.scaffold != null && FeatureManager.scaffold.isEnabled()
            || player.isUsingItem()) {
         if (this.wasRotating) cleanup();
         else { this.potted = false; this.resetEffect = false; }
         return;
      }

      Inventory inventory = player.getInventory();
      this.needPots.clear();
      this.potionOrder.clear();
      this.potionOrder.addAll(this.potions);
      Collections.shuffle(this.potionOrder);
      for (Holder<MobEffect> effect : this.potionOrder) {
         if (!player.hasEffect(effect) && effect != MobEffects.INSTANT_HEALTH && effect != MobEffects.REGENERATION
               || player.getHealth() <= 10.0F && (effect == MobEffects.INSTANT_HEALTH || effect == MobEffects.REGENERATION)) {
            this.needPots.add(effect);
         }
      }

      if (this.needPots.isEmpty()) {
         if (this.wasRotating) cleanup();
         return;
      }

      if (this.potTimer < 10) {
         this.potTimer++;
         return;
      }

      ItemStack mainHandStack = player.getMainHandItem();
      if (!(mainHandStack.getItem() instanceof SplashPotionItem)) {
         int hotbarIndex = findPotInHotbar(inventory, this.needPots);
         if (hotbarIndex != -1) {
            this.oldSlot = inventory.getSelectedSlot();
            inventory.setSelectedSlot(hotbarIndex);
         }
         return;
      }

      PotionContents contents = mainHandStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      boolean containsEffect = false;
      for (var effectInstance : contents.getAllEffects()) {
         if (this.needPots.contains(effectInstance.getEffect())) {
            containsEffect = true;
            break;
         }
      }
      if (!containsEffect) {
         this.resetEffect = true;
         this.wasRotating = true;
         return;
      }

      if (!this.wasRotating) {
         this.wasRotating = true;
         this.rotTicks = 0;
      }
      if (this.rotTicks < 1) return;

      mc.getConnection().send(new ServerboundUseItemPacket(
            InteractionHand.MAIN_HAND, 0, Events.ROTATION.getYaw(), Events.ROTATION.getPitch()));
      mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);

      this.potTimer = 0;
      this.potted = true;
   }

   private int findPotInHotbar(Inventory inventory, List<Holder<MobEffect>> needed) {
      for (int i = 0; i <= 8; i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.getItem() == Items.SPLASH_POTION) {
            for (Holder<MobEffect> effect : needed) {
               if (hasEffect(stack, effect)) return i;
            }
         }
      }
      return -1;
   }

   private boolean hasEffect(ItemStack stack, Holder<MobEffect> effect) {
      PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      for (var effectInstance : contents.getAllEffects()) {
         if (effectInstance.getEffect() == effect) return true;
      }
      return false;
   }
}
