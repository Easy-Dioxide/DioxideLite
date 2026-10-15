package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import mixins.LivingEntityAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.AABB;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.AlwaysRiptide).
 * Krs parity: forces the riptide spin-attack pose and state on the local player
 * (and on the integrated-server copy when the world is published to LAN), so
 * riptide keeps spinning without being in water/rain and touching entities are
 * attacked with a small velocity knockback. The spin state is cleared on disable,
 * falling back to a pose that fits the current collision box.
 * Notable adaptations vs the original:
 *  - ModuleManager.getModuleState(AlwaysRiptide.class) replaced with a static
 *    active flag (same pattern Krs uses for Xray/CaveFinder).
 *  - Level.getEntities(Entity, AABB) (two-arg) no longer exists in 26.3; the
 *    three-arg overload with a pass-through predicate is used.
 *  - player.swing(InteractionHand.MAIN_HAND) replaced with the 26.3 three-arg
 *    swing(hand, SwingAnimation.DEFAULT, true).
 *  - Krs's accesswidener fields (autoSpinAttackDmg / autoSpinAttackItemStack) are
 *    reached through the mixins/LivingEntityAccessor @Accessor methods, and the
 *    krs$ prefixed invoker/accessor names drop their prefix in this client.
 *  - Static hooks (shouldForceSpinAttack / hookDesiredPose / hookDimensions /
 *    prepareAutoSpinAttack) keep their Krs surface for the render/pose mixins.
 */
public class AlwaysRiptide extends Feature {
    private static volatile boolean active;

    private AABB lastBox = null;

    public AlwaysRiptide() {
        super("Always Riptide", Category.MISC);
    }

    @Override
    public void onEnable() {
        active = true;
        var player = mc.player;
        if (player == null) return;

        applySpinPose(player);
    }

    @Override
    public void onDisable() {
        active = false;
        lastBox = null;

        var player = mc.player;
        if (player == null) return;

        syncPublishedServerSpinAttack(player, false);

        if (!player.hasPose(Pose.SPIN_ATTACK) || player.isAutoSpinAttack()) return;

        if (canFit(player, Pose.STANDING))
            player.setPose(Pose.STANDING);
        else if (canFit(player, Pose.CROUCHING))
            player.setPose(Pose.CROUCHING);
        else player.setPose(Pose.SWIMMING);

        player.refreshDimensions();
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.WORLD) {
            lastBox = null;
        } else if (event == Events.UPDATE) {
            var player = mc.player;
            var level = mc.level;
            if (player == null || level == null) return;

            applySpinPose(player);
            prepareAutoSpinAttack(player);
            syncPublishedServerSpinAttack(player, true);
            attackTouchedEntities(player, level);
        }
    }

    public static boolean shouldForceSpinAttack(LivingEntity entity) {
        return entity == mc.player && active;
    }

    public static Pose hookDesiredPose(Pose original, Entity entity) {
        if (shouldApplyTo(entity))
            return Pose.SPIN_ATTACK;

        return original;
    }

    public static EntityDimensions hookDimensions(EntityDimensions original, Entity entity, Pose pose) {
        if (pose != Pose.SPIN_ATTACK && shouldApplyTo(entity))
            return EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F);

        return original;
    }

    public static void prepareAutoSpinAttack(LivingEntity entity) {
        if (!shouldApplyTo(entity)) return;

        ItemStack weaponStack = entity.getMainHandItem();
        if (weaponStack.isEmpty())
            weaponStack = entity.getOffhandItem();

        ((LivingEntityAccessor) entity).setAutoSpinAttackDmg(8.0F);
        ((LivingEntityAccessor) entity).setAutoSpinAttackItemStack(weaponStack);
    }

    private static boolean shouldApplyTo(Entity entity) {
        var player = mc.player;
        return player != null && entity != null && active && entity.getUUID().equals(player.getUUID());
    }

    private void applySpinPose(LocalPlayer player) {
        if (!player.hasPose(Pose.SPIN_ATTACK))
            player.setPose(Pose.SPIN_ATTACK);

        if (player.getBoundingBox().getYsize() > 0.7)
            player.refreshDimensions();
    }

    private void syncPublishedServerSpinAttack(LocalPlayer player, boolean enabled) {
        var server = mc.getSingleplayerServer();
        if (server == null || !server.isPublished()) return;

        var uuid = player.getUUID();
        server.executeIfPossible(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(uuid);
            if (serverPlayer == null) return;

            if (enabled) {
                if (!active) return;

                applyServerSpinAttack(serverPlayer);
            } else {
                if (active) return;

                clearServerSpinAttack(serverPlayer);
            }
        });
    }

    private void applyServerSpinAttack(ServerPlayer player) {
        prepareAutoSpinAttack(player);
        ((LivingEntityAccessor) player).setAutoSpinAttackTicks(20);
        ((LivingEntityAccessor) player).invokeSetLivingEntityFlag(4, true);

        if (!player.hasPose(Pose.SPIN_ATTACK))
            player.setPose(Pose.SPIN_ATTACK);

        if (player.getBoundingBox().getYsize() > 0.7)
            player.refreshDimensions();
    }

    private void clearServerSpinAttack(ServerPlayer player) {
        ((LivingEntityAccessor) player).setAutoSpinAttackTicks(0);
        ((LivingEntityAccessor) player).setAutoSpinAttackDmg(0.0F);
        ((LivingEntityAccessor) player).setAutoSpinAttackItemStack(null);
        ((LivingEntityAccessor) player).invokeSetLivingEntityFlag(4, false);

        if (!player.hasPose(Pose.SPIN_ATTACK)) return;

        if (canFit(player, Pose.STANDING))
            player.setPose(Pose.STANDING);
        else if (canFit(player, Pose.CROUCHING))
            player.setPose(Pose.CROUCHING);
        else player.setPose(Pose.SWIMMING);

        player.refreshDimensions();
    }

    private void attackTouchedEntities(LocalPlayer player, ClientLevel level) {
        if (mc.gameMode == null) return;

        AABB currentBox = player.getBoundingBox();
        AABB scanBox = lastBox == null ? currentBox : lastBox.minmax(currentBox);
        lastBox = currentBox;
        boolean attacked = false;

        for (Entity entity : level.getEntities(player, scanBox, entity -> true)) {
            if (!(entity instanceof LivingEntity livingEntity)
                    || !livingEntity.isAlive()
                    || !entity.isAttackable()
                    || entity.skipAttackInteraction(player))
                continue;

            prepareAutoSpinAttack(player);
            mc.gameMode.attack(player, entity);
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            attacked = true;
        }

        if (attacked)
            player.setDeltaMovement(player.getDeltaMovement().scale(-0.2));
    }

    private boolean canFit(LivingEntity player, Pose pose) {
        var level = player.level();
        if (level == null) return false;

        return level.noCollision(player, player.getDimensions(pose).makeBoundingBox(player.position()).deflate(1.0E-7));
    }
}
