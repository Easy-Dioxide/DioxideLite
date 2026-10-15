package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.util.krs.TickTimer;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.AntiBot).
 * Krs keeps the mode setting and the bot/suspect lists static; the client
 * requires per-instance settings, so the static inBotList entry point resolves
 * the registered module instance (same pattern as TPStealer/WTap static hooks).
 * Event handlers map to Events.UPDATE / Events.TICK / EventPacketReceive /
 * EventWorld.
 */
public class AntiBot2 extends Feature {
   private static AntiBot2 instance;

   private final ModeSetting mode = new ModeSetting("Mode", this, "Advanced",
           new String[]{"Advanced", "Hypixel", "Cubecraft", "Cubecraft Bedrock", "Shotbow"});

   private final HashSet<UUID> suspectList = new HashSet<>();
   private final HashSet<UUID> botList = new HashSet<>();

   private final TickTimer armorTimer = new TickTimer();
   private boolean armorChecker = false;
   private final HashMap<UUID, RotationState> rotationTracker = new HashMap<>();
   private final HashSet<UUID> damagedEntities = new HashSet<>();
   private final HashSet<UUID> listedPlayerProfiles = new HashSet<>();

   public AntiBot2() {
      super("AntiBot2", Category.COMBAT);
      instance = this;
   }

   @Override
   public void onDisable() {
      reset();
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         onWorld();
         return;
      }
      if (event instanceof EventPacketReceive receiving) {
         onReceivedPacket(receiving.getPacket());
         return;
      }
      if (event == Events.TICK) {
         onTick();
         return;
      }
      if (event == Events.UPDATE) {
         onUpdate();
      }
   }

   private void onWorld() {
      reset();
   }

   private void reset() {
      botList.clear();
      suspectList.clear();
      armorTimer.reset();
      armorChecker = false;
      rotationTracker.clear();
      damagedEntities.clear();
      listedPlayerProfiles.clear();
   }

   private void onUpdate() {
      if (mc.player == null) return;
      var level = mc.level;
      var connection = mc.getConnection();
      if (level == null || connection == null) return;

      if (mode.getValue().equalsIgnoreCase("hypixel")) {
         listedPlayerProfiles.clear();

         for (var entry : connection.getListedOnlinePlayers()) {
            listedPlayerProfiles.add(entry.getProfile().id());
         }

         for (Player player : level.players()) {
            UUID profile = player.getGameProfile().id();
            if (profile == null) continue;
            if (!listedPlayerProfiles.contains(profile))
               botList.add(profile);
            else
               botList.remove(profile);
         }
      }
   }

   private void onTick() {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return;

      switch (mode.getValue().toLowerCase(Locale.ROOT)) {
         case "shotbow" -> {
            if (suspectList.isEmpty()) return;
            for (Player entity : level.players()) {
               if (!suspectList.contains(entity.getUUID()))
                  continue;

               Iterable<ItemStack> armor = null;
               if (!isFullyArmored(entity)) {
                  armor = armorSlots(entity);
                  armorChecker = true;
               }

               if (armorChecker) {
                  armorTimer.update();

                  if (armorTimer.hasTimePassed(2)) {
                     if ((isFullyArmored(entity) || updatesArmor(entity, armor)) && entity.getGameProfile().properties().isEmpty())
                        botList.add(entity.getUUID());

                     suspectList.remove(entity.getUUID());
                     armorTimer.reset();
                     armorChecker = false;
                  }
               }
            }
         }
         case "cubecraft bedrock" -> {
            for (Player entity : level.players()) {
               if (entity == player) continue;

               UUID uuid = entity.getUUID();
               if (entity.hurtTime > 0)
                  damagedEntities.add(uuid);

               if (damagedEntities.contains(uuid)) {
                  botList.remove(uuid);
                  rotationTracker.remove(uuid);
                  continue;
               }

               float currentYaw = entity.getYRot();
               RotationState rotationState = rotationTracker.computeIfAbsent(uuid, ignored -> new RotationState(currentYaw));

               if (Math.abs(currentYaw - rotationState.lastYaw) > 1f) {
                  rotationState.movingTimer.update();
                  rotationState.stillTimer.reset();
               } else {
                  rotationState.stillTimer.update();
               }

               rotationState.lastYaw = currentYaw;

               if (rotationState.stillTimer.hasTimePassed(20)) {
                  botList.remove(uuid);
                  rotationTracker.remove(uuid);
                  continue;
               }

               if (rotationState.movingTimer.hasTimePassed(150)) {
                  botList.add(uuid);
                  rotationTracker.remove(uuid);
               }
            }
         }
      }
   }

   private void onReceivedPacket(net.minecraft.network.protocol.Packet<?> packet) {
      if (mc.player == null || mc.getConnection() == null) return;
      if (packet == null) return;

      if (mode.getValue().equalsIgnoreCase("shotbow")) {
         if (packet instanceof ClientboundPlayerInfoUpdatePacket updatePacket) {
            for (var entry : updatePacket.newEntries()) {
               GameProfile profile = entry.profile();
               if (profile == null)
                  continue;

               if (entry.latency() < 2 || !profile.properties().isEmpty() || isGameProfileUnique(profile))
                  continue;

               if (isDuplicated(profile)) {
                  botList.add(entry.profileId());
                  continue;
               }

               suspectList.add(entry.profileId());
            }
         } else if (packet instanceof ClientboundPlayerInfoRemovePacket removePacket) {
            for (UUID uuid : removePacket.profileIds()) {
               suspectList.remove(uuid);
               botList.remove(uuid);
            }
         }
      }
   }

   private boolean isGameProfileUnique(GameProfile originalProfile) {
      int matches = 0;
      for (var entry : mc.getConnection().getListedOnlinePlayers()) {
         if (entry.getProfile().name().equals(originalProfile.name()) && entry.getProfile().id().equals(originalProfile.id())) {
            matches++;
            if (matches > 1)
               return false;
         }
      }
      return matches == 1;
   }

   private boolean isDuplicated(GameProfile originalProfile) {
      for (var entry : mc.getConnection().getListedOnlinePlayers()) {
         if (entry.getProfile().name().equals(originalProfile.name()) && !entry.getProfile().id().equals(originalProfile.id())) {
            return true;
         }
      }
      return false;
   }

   private boolean isFullyArmored(Player entity) {
      return isEnchantedArmor(entity.getItemBySlot(EquipmentSlot.HEAD))
              && isEnchantedArmor(entity.getItemBySlot(EquipmentSlot.CHEST))
              && isEnchantedArmor(entity.getItemBySlot(EquipmentSlot.LEGS))
              && isEnchantedArmor(entity.getItemBySlot(EquipmentSlot.FEET));
   }

   private boolean isEnchantedArmor(ItemStack stack) {
      return stack.has(DataComponents.EQUIPPABLE) && stack.isEnchanted();
   }

   private boolean updatesArmor(Player entity, Iterable<ItemStack> prevArmor) {
      if (prevArmor == null) return true;

      var previousIterator = prevArmor.iterator();
      if (isUpdatedArmorSlot(entity, previousIterator, EquipmentSlot.HEAD)) return true;
      if (isUpdatedArmorSlot(entity, previousIterator, EquipmentSlot.CHEST)) return true;
      if (isUpdatedArmorSlot(entity, previousIterator, EquipmentSlot.LEGS)) return true;
      if (isUpdatedArmorSlot(entity, previousIterator, EquipmentSlot.FEET)) return true;

      return previousIterator.hasNext();
   }

   private boolean isUpdatedArmorSlot(Player entity, Iterator<ItemStack> previousIterator, EquipmentSlot slot) {
      return !previousIterator.hasNext() || !previousIterator.next().equals(entity.getItemBySlot(slot));
   }

   private ArrayList<ItemStack> armorSlots(LivingEntity entity) {
      ArrayList<ItemStack> armor = new ArrayList<>(4);
      armor.add(entity.getItemBySlot(EquipmentSlot.HEAD));
      armor.add(entity.getItemBySlot(EquipmentSlot.CHEST));
      armor.add(entity.getItemBySlot(EquipmentSlot.LEGS));
      armor.add(entity.getItemBySlot(EquipmentSlot.FEET));
      return armor;
   }

   public static boolean inBotList(LivingEntity playerEntity) {
      AntiBot2 module = instance;
      if (module == null) return false;

      UUID uuid = playerEntity.getUUID();
      var connection = mc.getConnection();
      String currentMode = module.mode.getValue();
      return module.botList.contains(uuid)
              || currentMode.equalsIgnoreCase("advanced") && connection != null && !connection.getOnlinePlayerIds().contains(uuid)
              || currentMode.equalsIgnoreCase("cubecraft") && playerEntity.getAttributes().getValue(Attributes.SCALE) <= 0.4f;
   }

   private static final class RotationState {
      private float lastYaw;
      private final TickTimer movingTimer = new TickTimer();
      private final TickTimer stillTimer = new TickTimer();

      private RotationState(float lastYaw) {
         this.lastYaw = lastYaw;
      }
   }
}
