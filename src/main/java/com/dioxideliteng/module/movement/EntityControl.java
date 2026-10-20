package com.dioxideliteng.module.movement;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;

/**
 * Port of Krs EntityControl (com.instrumentalist.krs.hacks.features.movement.EntityControl)
 * to DioxideLiteNG. Logic is a 1:1 copy; only the module-state API is adapted:
 *  - {@code ModuleManager.getModuleState(EntityControl.class)} -> the resolved instance
 *    check (Scaffold2-style lookup; FeatureManager is owned outside this port);
 *  - The static hook entry points ({@code hookControllingPassenger},
 *    {@code shouldTreatAsSaddled}, {@code shouldTreatAsTamed}) are preserved verbatim for
 *    the Entity/Mob/AbstractHorse/Pig/TamableAnimal mixin hooks (see report: the mixin
 *    injections themselves are not part of this port).
 */
public class EntityControl extends Feature {

   public EntityControl() {
      super("Entity Control", Category.MOVEMENT);
   }

   public static LivingEntity hookControllingPassenger(LivingEntity original, Entity entity) {
      if (original != null || !shouldControl(entity))
         return original;

      return mc.player;
   }

   public static boolean shouldTreatAsSaddled(Mob entity) {
      return shouldControl(entity);
   }

   public static boolean shouldTreatAsTamed(Entity entity) {
      return shouldControl(entity);
   }

   private static boolean shouldControl(Entity entity) {
      var player = mc.player;
      EntityControl module = inst();
      return module != null && module.isEnabled()
            && player != null
            && entity != null
            && entity.isAlive()
            && player.isPassenger()
            && player.getVehicle() == entity
            && isSupported(entity);
   }

   private static boolean isSupported(Entity entity) {
      return entity instanceof PlayerRideable
            || entity instanceof ItemSteerable
            || entity instanceof HappyGhast;
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEnable() {
   }

   private static EntityControl inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof EntityControl e) return e;
      }
      return null;
   }
}
