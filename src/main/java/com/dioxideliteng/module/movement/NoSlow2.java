package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.ToolUtil;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;

/**
 * Port of Krs NoSlow (com.instrumentalist.krs.hacks.features.movement.NoSlow) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event/module-state API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - Krs SendPacketEvent -> {@link EventPacketSend};
 *  - Krs MotionEvent (y mutation) -> {@link Events#PRE_MOTION} (setY), the analog of
 *    the Krs dispatch point (LocalPlayer#sendPosition HEAD);
 *  - Krs {@code tag()} -> {@link Feature#setSuffix(String)} refreshed on UPDATE;
 *  - {@code ModuleManager.getModuleState(NoSlow.class)} -> the resolved instance
 *    check (Scaffold2-style lookup; FeatureManager is owned outside this port).
 *
 * <p>The static entry points (noSlowHook / shouldNoSlowSneak / shouldNoSlowUseItem)
 * are preserved verbatim for the Krs ClientPlayerEntityMixin modifyInput hooks
 * (itemUseSpeedMultiplier / isMovingSlowly) and the canStartSprinting hook; the
 * injections themselves are not part of this port (see report).
 */
public class NoSlow2 extends Feature {

   private final ModeSetting mode = new ModeSetting("Mode", this, "Vanilla", new String[]{"Vanilla", "Hypixel NCP"});
   private final BooleanSetting sneak = new BooleanSetting("Sneak", this, false);

   private int lastSneakInputTick = Integer.MIN_VALUE;
   private boolean waitingPacket = false;

   public NoSlow2() {
      super("NoSlow2", Category.MOVEMENT);
      this.sneak.setVisible(() -> this.mode.is("Vanilla"));
   }

   @Override
   public void onDisable() {
      this.lastSneakInputTick = Integer.MIN_VALUE;
      this.waitingPacket = false;
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.UPDATE) {
         setSuffix(this.mode.getValue());
         onUpdate();
      } else if (event == Events.PRE_MOTION) {
         onMotion();
      } else if (event instanceof EventPacketSend sending) {
         onSendPacket(sending);
      }
   }

   private void onMotion() {
      var player = mc.player;
      if (player == null) return;

      if (this.mode.is("Hypixel NCP") && !ToolUtil.INSTANCE.isSword(player.getMainHandItem()) && player.isUsingItem() && player.onGround())
         Events.PRE_MOTION.setY(Events.PRE_MOTION.getY() + (player.tickCount % 2 == 0 ? 0.03 : 1E-5));
   }

   private void onSendPacket(EventPacketSend event) {
      var player = mc.player;
      if (player == null) return;

      Packet<?> packet = event.getPacket();
      ItemStack mainHandItem = player.getMainHandItem();

      if (this.mode.is("Hypixel NCP") && MovementUtil.fallTicks < 2 && isHypixelUseItem(mainHandItem)) {
         if (packet instanceof ServerboundUseItemPacket) {
            event.setCancelled(true);

            if (player.onGround()) {
               player.input.makeJump();
            }

            this.waitingPacket = true;
         }

         if (packet instanceof ServerboundUseItemOnPacket)
            event.setCancelled(true);
      }
   }

   private void onUpdate() {
      var player = mc.player;
      if (player == null) return;

      if (this.mode.is("Hypixel NCP") && this.waitingPacket && !player.isUsingItem())
         this.waitingPacket = false;

      if (this.mode.is("Hypixel NCP") && this.waitingPacket && MovementUtil.fallTicks >= 2 && isHypixelUseItem(player.getMainHandItem())) {
         PacketUtil.sendPacket(new ServerboundUseItemPacket(
                 InteractionHand.MAIN_HAND,
                 0,
                 player.getYRot(),
                 player.getXRot()
         ));
         this.waitingPacket = false;
      }
   }

   public static boolean shouldNoSlowSneak() {
      var player = mc.player;
      NoSlow2 module = inst();
      if (player == null
              || module == null || !module.isEnabled()
              || !module.mode.is("Vanilla")
              || !module.sneak.getValue())
         return false;

      if (isSneakInputActive()) {
         module.lastSneakInputTick = player.tickCount;
         return true;
      }

      int ticksSinceSneak = player.tickCount - module.lastSneakInputTick;
      return ticksSinceSneak >= 0 && ticksSinceSneak <= 1 && player.isMovingSlowly();
   }

   public static boolean noSlowHook() {
      NoSlow2 module = inst();
      if (module == null || !module.isEnabled())
         return false;

      switch (module.mode.getValue().toLowerCase(Locale.ROOT)) {
         case "vanilla":
            return shouldNoSlowSneak() || shouldNoSlowUseItem();
         case "hypixel ncp":
            return shouldNoSlowUseItem();
      }
      return false;
   }

   public static boolean shouldNoSlowUseItem() {
      var player = mc.player;
      NoSlow2 module = inst();
      if (player == null || module == null || !module.isEnabled() || !player.isUsingItem())
         return false;

      boolean fastUseItem = isFastUseAnimation(player.getUseItem().getUseAnimation());
      switch (module.mode.getValue().toLowerCase(Locale.ROOT)) {
         case "vanilla":
            return fastUseItem;
         case "hypixel ncp":
            return !ToolUtil.INSTANCE.isSword(player.getMainHandItem()) && !player.isShiftKeyDown() && fastUseItem;
      }

      return false;
   }

   private static boolean isSneakInputActive() {
      var player = mc.player;
      if (player == null)
         return false;

      if (player.isShiftKeyDown())
         return true;

      if (player.input == null)
         return false;

      Input input = player.input.keyPresses;
      return input != null && input.shift();
   }

   private static boolean isFastUseAnimation(ItemUseAnimation animation) {
      return animation == ItemUseAnimation.SPYGLASS
              || animation == ItemUseAnimation.TOOT_HORN
              || animation == ItemUseAnimation.BUNDLE
              || animation == ItemUseAnimation.DRINK
              || animation == ItemUseAnimation.BRUSH
              || animation == ItemUseAnimation.BLOCK
              || animation == ItemUseAnimation.EAT
              || animation == ItemUseAnimation.CROSSBOW
              || animation == ItemUseAnimation.SPEAR
              || animation == ItemUseAnimation.BOW
              || animation == ItemUseAnimation.TRIDENT;
   }

   private static boolean isHypixelUseItem(ItemStack stack) {
      var item = stack.getItem();
      return stack.has(DataComponents.FOOD) || item == Items.POTION || item instanceof BowItem || item == Items.MILK_BUCKET;
   }

   // Resolve the active NoSlow2 instance so static methods can read settings/state.
   // Public: Krs KillAura.finishAutoBlock gates spoofHypixelNoSlowSlot on
   // ModuleManager.getModuleState(NoSlow.class), which maps onto this ported module.
   public static NoSlow2 inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof NoSlow2 n) return n;
      }
      return null;
   }
}
