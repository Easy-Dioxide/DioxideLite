package com.dioxidelite.module.modules.player;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/module/modules/player/GhostHand.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.StartUseItemEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.util.rotation.RotationUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class GhostHand extends Module {

    public static final GhostHand INSTANCE = new GhostHand();

    private final Set<BlockPos> visitedPositions = new ObjectOpenHashSet<>();

    private GhostHand() {
        super("Ghost Hand", Category.PLAYER);
    }

    @Listen
    private void onStartUseItem(StartUseItemEvent event) {
        if (!mc.options.keyUse.isDown()) return;

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        HitResult normalHit = mc.player.pick(mc.player.blockInteractionRange(), partialTick, false);
        if (mc.level.getBlockState(BlockPos.containing(normalHit.getLocation())).hasBlockEntity()) {
            return;
        }

        Vec3 direction = new Vec3(0, 0, 0.1)
                .xRot(-(float) Math.toRadians(mc.player.getXRot()))
                .yRot(-(float) Math.toRadians(mc.player.getYRot()));

        visitedPositions.clear();
        for (int i = 1; i < mc.player.blockInteractionRange() * 10; i++) {
            BlockPos pos = BlockPos.containing(mc.player.getEyePosition(partialTick).add(direction.scale(i)));
            if (!visitedPositions.add(pos)) continue;

            if (mc.level.getBlockState(pos).hasBlockEntity()) {
                for (InteractionHand hand : InteractionHand.values()) {
                    InteractionResult result = mc.gameMode.useItemOn(
                            mc.player,
                            hand,
                            new BlockHitResult(pos.getCenter(), RotationUtils.getDirection(pos), pos, true)
                    );
                    if (result instanceof InteractionResult.Success || result instanceof InteractionResult.Fail) {
                        mc.player.swing(hand);
                        event.cancel();
                        return;
                    }
                }
            }
        }
    }
}
