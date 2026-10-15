package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.TimerUtil;
import java.util.Set;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

/**
 * Port of Krs LongJump (com.instrumentalist.krs.hacks.features.movement.LongJump)
 * to DioxideLiteNG. Logic is a 1:1 copy; only the event/packet API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - Krs {@code tag()} -> {@link Feature#setSuffix(String)} refreshed on UPDATE;
 *  - Krs MotionEvent (pitch) -> {@link Events#ROTATION};
 *  - Krs SendPacketEvent/ReceivedPacketEvent -> {@link EventPacketSend}/{@link EventPacketReceive};
 *  - {@code setState(false)} -> {@link Feature#setEnabled(boolean)};
 *  - {@code tempEnabled} -> {@link Feature#isEnabled()};
 *  - 26.3: {@code ServerboundAcceptTeleportationPacket} takes the teleport id plus
 *    the acknowledged position/rotation (Krs 26.2 only had the id), same adaptation
 *    as the ported MatrixFly mode;
 *  - Krs {@code event.isMoving = true} (matrix branch of onMotion) has no equivalent
 *    in the ported sendPosition (no move-suppression logic there) and is dropped.
 */
public class LongJump2 extends Feature {
   private final ModeSetting longJumpMode = new ModeSetting("Long Jump Mode", this, "Matrix", new String[]{"Matrix", "Grim"});
   private final BooleanSetting airViewBobbing = new BooleanSetting("Air View Bobbing", this, true);

   private boolean start;
   private Vec3 matrix2StartCameraPosition = Vec3.ZERO;
   private boolean matrix2Started;
   private boolean matrix2Airborne;
   private int grimTicks;

   public LongJump2() {
      super("LongJump2", Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (this.longJumpMode.is("Grim")) {
         this.grimTicks = 0;

         if (mc.player != null && MovementUtil.fallTicks >= 2) {
            for (int i = 0; i < 20; i++)
               PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
         }
      }
   }

   @Override
   public void onDisable() {
      this.matrix2Started = false;
      this.matrix2Airborne = false;
      this.start = false;
      this.matrix2StartCameraPosition = Vec3.ZERO;
      this.grimTicks = 0;
      TimerUtil.reset();
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.UPDATE) {
         setSuffix(this.longJumpMode.getValue());
         onUpdate();
      } else if (event == Events.ROTATION) {
         onMotion();
      } else if (event instanceof EventPacketSend sending) {
         onSendPacket(sending);
      } else if (event instanceof EventPacketReceive receiving) {
         onReceivedPacket(receiving);
      }
   }

   private void onUpdate() {
      if (mc.player == null) {
         this.matrix2Started = false;
         this.matrix2Airborne = false;
         this.start = false;
         this.matrix2StartCameraPosition = Vec3.ZERO;
         this.grimTicks = 0;
         return;
      }

      switch (this.longJumpMode.getValue().toLowerCase()) {
         case "grim":
            this.grimTicks++;

            if (mc.player.onGround())
               mc.player.input.makeJump();

            int airTicks = MovementUtil.fallTicks;
            if (airTicks == 1) {
               PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
               PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
            } else if (airTicks == 2) {
               for (int i = 0; i < 10; i++)
                  PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
            } else if (this.grimTicks == 2) {
               for (int i = 0; i < 4; i++)
                  PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
            }
            break;

         case "matrix":
            if (!mc.player.isSprinting()) {
               this.matrix2Started = false;
               this.matrix2Airborne = false;
               this.start = false;
               this.matrix2StartCameraPosition = Vec3.ZERO;
               return;
            }

            if (!this.start) {
               this.matrix2Started = false;
               this.matrix2Airborne = false;

               if (mc.player != null)
                  this.matrix2StartCameraPosition = new Vec3(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
               else
                  this.matrix2StartCameraPosition = Vec3.ZERO;

               this.start = true;
               return;
            }

            if (this.matrix2Airborne && mc.player.onGround()) {
               setEnabled(false);
               return;
            }

            if (mc.player.onGround())
               mc.player.input.makeJump();

            this.matrix2Started = true;

            if (mc.player.tickCount > 1)
               MovementUtil.setVelocityY(mc.player.getDeltaMovement().y + 0.00348);

            if (this.matrix2Started && mc.player.fallDistance > 0.1F) {
               MovementUtil.setVelocityY(0.42);
               MovementUtil.strafe(1.97F);
            }

            this.matrix2Airborne = !mc.player.onGround();
            break;
      }
   }

   private void onMotion() {
      switch (this.longJumpMode.getValue().toLowerCase()) {
         case "grim":
            Events.ROTATION.setPitch(Events.ROTATION.getPitch() + (float) (Math.random() * 0.5));

            int airTicks = MovementUtil.fallTicks;
            if (this.grimTicks > 4)
               TimerUtil.timerSpeed = 1.0F;
            else if (this.grimTicks > 2)
               TimerUtil.timerSpeed = 1.0F;
            else if (airTicks == 1)
               TimerUtil.timerSpeed = 0.8F;
            else if (airTicks > 2)
               TimerUtil.timerSpeed = 2.0F;
            break;

         case "matrix":
            // Krs: event.isMoving = true (matrix) — no equivalent in the ported
            // sendPosition; dropped (see class javadoc).
            break;
      }
   }

   private void onSendPacket(EventPacketSend event) {
      if (mc.player == null) return;

      Packet<?> packet = event.getPacket();

      if (this.longJumpMode.is("Grim") && packet instanceof ServerboundMovePlayerPacket && this.grimTicks >= 2 && MovementUtil.fallTicks >= 1)
         event.setCancelled(true);
   }

   private void onReceivedPacket(EventPacketReceive event) {
      if (mc.player == null) return;

      Packet<?> packet = event.getPacket();

      switch (this.longJumpMode.getValue().toLowerCase()) {
         case "grim":
            if (packet instanceof ClientboundSetEntityMotionPacket(int id1, Vec3 movement) && id1 == mc.player.getId()) {
               if (movement.y / 8000.0 < 0.0)
                  setEnabled(false);
            }
            break;

         case "matrix":
            if (!(packet instanceof ClientboundPlayerPositionPacket(
                    int id, PositionMoveRotation change, Set<Relative> relatives
            )))
               return;

            event.setCancelled(true);

            PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(
                    PositionMoveRotation.of(mc.player),
                    change,
                    relatives
            );
            Vec3 position = absolute.position();

            PacketUtil.sendPacketAsSilent(new ServerboundAcceptTeleportationPacket(
                    id,
                    position.x,
                    position.y,
                    position.z,
                    mc.player.getYRot(),
                    mc.player.getXRot()
            ));
            PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.PosRot(
                    position.x,
                    position.y,
                    position.z,
                    mc.player.getYRot(),
                    mc.player.getXRot(),
                    false,
                    mc.player.horizontalCollision
            ));

            mc.player.setPos(position);
            mc.player.jumpFromGround();
            setEnabled(false);
            break;
      }
   }

   public boolean shouldUseMatrix2SilentCamera() {
      return isEnabled() && this.matrix2StartCameraPosition != Vec3.ZERO;
   }

   public Vec3 getMatrix2SilentCameraPosition() {
      return this.matrix2StartCameraPosition;
   }
}
