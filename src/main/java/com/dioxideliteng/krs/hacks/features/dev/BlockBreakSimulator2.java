package com.dioxideliteng.krs.hacks.features.dev;

import com.dioxideliteng.krs.utils.entity.PlayerUtil;


import com.dioxideliteng.krs.events.features.MotionEvent;
import com.dioxideliteng.krs.events.features.TickEvent;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.dioxideliteng.krs.utils.input.Keys;

public class BlockBreakSimulator2 extends Module {

    public BlockBreakSimulator2() {
        super("Block Break Simulator 2", ModuleCategory.Dev, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onUpdate(UpdateEvent event) {
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.level == null) return;

        HitResult ray = mc.player.pick(5.0D, 0.0F, false);
        if (ray.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) ray;
        BlockPos pos = blockHit.getBlockPos();
        Direction side = blockHit.getDirection();

        MultiPlayerGameMode interactionManager = mc.gameMode;
        if (interactionManager == null) return;

        interactionManager.continueDestroyBlock(pos, side);
        PlayerUtil.INSTANCE.swingHand(InteractionHand.MAIN_HAND);
    }
}
