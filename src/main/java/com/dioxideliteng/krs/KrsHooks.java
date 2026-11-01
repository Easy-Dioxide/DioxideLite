package com.dioxideliteng.krs;

import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.combat.TargetStrafe;
import com.dioxideliteng.krs.hacks.features.movement.MovementFix;
import com.dioxideliteng.krs.hacks.features.movement.Sprint;
import com.dioxideliteng.krs.utils.move.MovementUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Entry points for vanilla hooks that DioxideLiteNG already patches itself.
 *
 * <p>KRS cancelled and re-implemented {@code Entity.moveRelative} and {@code LivingEntity.jumpFromGround}; DioxideLiteNG's
 * mixins already rewrite both, and two mixins cannot own one method. DioxideLiteNG's mixins therefore call these helpers
 * and keep a single code path. Each helper returns the incoming value untouched unless a ported module wants a change.
 */
public final class KrsHooks {
    private KrsHooks() {
    }

    /**
     * Yaw used to turn movement input into velocity in {@code Entity.moveRelative}.
     * Mirrors KRS: MovementFix wins, then TargetStrafe.
     */
    public static float moveRelativeYaw(Entity entity, float original) {
        if (entity != Minecraft.getInstance().player || !KrsBridge.ready()) return original;

        if (MovementFix.shouldFixMovement()) return MovementFix.getMovementYaw();
        if (TargetStrafe.targetStrafeHook()) return MovementUtil.getPlayerDirection();
        return original;
    }

    /**
     * Yaw used for the sprint-jump boost in {@code LivingEntity.jumpFromGround}.
     * Mirrors KRS: MovementFix wins, then TargetStrafe, then Sprint's omnidirectional override.
     */
    public static float jumpYaw(Entity entity, float original) {
        if (entity != Minecraft.getInstance().player || !KrsBridge.ready()) return original;

        float yaw = original;
        if (MovementFix.shouldFixMovement()) yaw = MovementFix.getJumpMovementYaw();
        else if (TargetStrafe.targetStrafeHook()) yaw = MovementUtil.getPlayerDirection();
        return Sprint.getOmnidirectionalJumpYaw(yaw);
    }

    public static boolean moduleEnabled(Class<? extends com.dioxideliteng.krs.hacks.Module> type) {
        return ModuleManager.getModuleState(type);
    }
}
