package com.dioxidelite.onyx.engine;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Current-version movement backend based on OpenOnyx's movement feature boundaries. */
public final class OnyxMovementEngine {
    private final Minecraft mc;

    public OnyxMovementEngine(Minecraft mc) { this.mc = mc; }

    public boolean shouldSprint(boolean onlyForward) {
        if (mc.player == null) return false;
        return !onlyForward || mc.options.keyUp.isDown();
    }

    public boolean isSafeMove(double x, double z, boolean onlyGround) {
        if (mc.player == null || mc.level == null) return true;
        if (onlyGround && !mc.player.onGround()) return true;
        if (Math.abs(x) < 1.0E-7 && Math.abs(z) < 1.0E-7) return true;
        BlockPos below = BlockPos.containing(mc.player.getX() + x, mc.player.getY() - 0.08D, mc.player.getZ() + z);
        BlockState state = mc.level.getBlockState(below);
        return !state.isAir();
    }
}
