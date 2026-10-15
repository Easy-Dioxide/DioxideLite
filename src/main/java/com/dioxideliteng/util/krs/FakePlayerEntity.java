package com.dioxideliteng.util.krs;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from Krs (com.instrumentalist.krs.utils.entity.FakePlayerEntity).
 * Notable adaptations vs the original:
 *  - The Krs IMinecraft mixin interface (mc field) is replaced by a static
 *    Minecraft.getInstance() holder, matching the client's other krs utils.
 *  - MC 26.3 removed Player#DATA_PLAYER_MODE_CUSTOMISATION; the skin/model copy
 *    now overrides getSkin() with the local player's skin instead of copying
 *    the entity data byte.
 */
public class FakePlayerEntity extends RemotePlayer {
   public static final net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

   private final LocalPlayer player;
   private final ClientLevel world;
   private PlayerInfo playerListEntry;

   public Vec3 fakePos;
   public Float fakeYaw;
   public Float fakePitch;
   public Boolean fakeGround;

   public FakePlayerEntity(ClientLevel clientWorld, GameProfile gameProfile) {
      super(clientWorld, gameProfile);

      this.player = mc.player;
      this.world = mc.level;

      if (this.player == null) return;

      setUUID(UUID.randomUUID());
      copyPosition(this.player);
      this.setOnGround(this.player.onGround());

      fakePos = this.player.position();
      fakeYaw = this.player.getYRot();
      fakePitch = this.player.getXRot();
      fakeGround = this.player.onGround();

      copyInventory();
      copyPlayerModel(this.player, this);
      copyRotation();
      resetCapeMovement();

      spawn();
   }

   @Override
   protected @Nullable PlayerInfo getPlayerInfo() {
      if (playerListEntry == null && mc.getConnection() != null)
         playerListEntry = mc.getConnection().getPlayerInfo(getGameProfile().id());

      return playerListEntry;
   }

   @Override
   public PlayerSkin getSkin() {
      // Krs parity: FakePlayerEntity copies the local player's model; MC 26.3
      // carries that in the PlayerSkin resolved from the player list entry.
      if (player != null)
         return player.getSkin();

      return super.getSkin();
   }

   private void copyInventory() {
      getInventory().replaceWith(player.getInventory());
   }

   private void copyPlayerModel(Entity from, Entity to) {
      // MC 26.3 removed Player#DATA_PLAYER_MODE_CUSTOMISATION; the model copy is
      // handled through getSkin() (see override above).
   }

   private void copyRotation() {
      yHeadRot = player.yHeadRot;
      yBodyRot = player.yBodyRot;
   }

   private void resetCapeMovement() {
      avatarState().tick(position(), Vec3.ZERO);
   }

   private void spawn() {
      world.addEntity(this);
   }

   public void deSpawn() {
      discard();
   }

   public void resetPlayerPosition() {
      player.setPos(getX(), getY(), getZ());
      player.setYRot(getYRot());
      player.setXRot(getXRot());
      yHeadRot = player.yHeadRot;
   }

   public void refreshFakePlayerPosition() {
      if (fakePos == null)
         fakePos = this.player.position();
      if (fakeYaw == null)
         fakeYaw = this.player.getYRot();
      if (fakePitch == null)
         fakePitch = this.player.getXRot();
      if (fakeGround == null)
         fakeGround = this.player.onGround();

      setPos(fakePos.x, fakePos.y, fakePos.z);
      setYRot(fakeYaw);
      setXRot(fakePitch);
      yHeadRot = fakeYaw;

      setOnGround(fakeGround);
   }
}
