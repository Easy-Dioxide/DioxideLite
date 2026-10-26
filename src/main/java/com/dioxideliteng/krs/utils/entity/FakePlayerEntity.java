package com.dioxideliteng.krs.utils.entity;

import com.dioxideliteng.krs.utils.IMinecraft;
import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FakePlayerEntity extends RemotePlayer implements IMinecraft {
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

    private void copyInventory() {
        getInventory().replaceWith(player.getInventory());
    }

    private void copyPlayerModel(Entity from, Entity to) {
        SynchedEntityData fromTracker = from.getEntityData();
        SynchedEntityData toTracker = to.getEntityData();
        Byte playerModel = fromTracker.get(Player.DATA_PLAYER_MODE_CUSTOMISATION);
        toTracker.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, playerModel);
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
