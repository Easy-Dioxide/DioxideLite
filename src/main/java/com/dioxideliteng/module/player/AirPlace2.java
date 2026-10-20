package com.dioxideliteng.module.player;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.AirPlace).
 *
 * Adaptations:
 *  - {@code ModuleManager.getModuleState(AirPlace.class)} replaced with a FeatureManager scan;
 *  - the hit-result hook itself lives in a Krs mixin (crosshair pick); no such mixin exists in
 *    this client, so the static helper is ported verbatim for the hook site to call.
 */
public class AirPlace2 extends Feature {

    public AirPlace2() {
        super("AirPlace2", Category.PLAYER);
    }

    public static HitResult createPlacementHit(HitResult original, LocalPlayer player) {
        if (!isEnabledStatic()
                || original == null
                || original.getType() != HitResult.Type.MISS
                || !(original instanceof BlockHitResult miss)
                || player == null
                || !(player.getMainHandItem().getItem() instanceof BlockItem)
                && !(player.getOffhandItem().getItem() instanceof BlockItem)) {
            return original;
        }

        BlockPos target = BlockPos.containing(miss.getLocation());
        return new BlockHitResult(miss.getLocation(), miss.getDirection(), target, false);
    }

    private static boolean isEnabledStatic() {
        for (var feature : FeatureManager.getModules()) {
            if (feature instanceof AirPlace2 module && module.isEnabled()) return true;
        }
        return false;
    }
}
