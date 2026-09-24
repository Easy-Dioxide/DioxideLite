package com.dioxidelite.module.modules.combat;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.DoubleSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;

/** Lightweight BedTracker port: keeps the nearest bed position available to visual modules. */
public final class BedTracker extends Module {
    public static final BedTracker INSTANCE = new BedTracker();
    private final DoubleSetting range = add(new DoubleSetting("Range", 16.0, 4.0, 32.0, 1.0));
    private BlockPos trackedBed;
    private int scanCooldown;
    private BedTracker() { super("Bed Tracker", Category.COMBAT); }
    public BlockPos trackedBed() { return trackedBed; }
    @Listen private void onTick(PlayerTickEvent.Pre event) {
        if (noPlayer()) { trackedBed = null; return; }
        if (scanCooldown++ < 10) return;
        scanCooldown = 0;
        int r = (int)Math.ceil(range.get());
        BlockPos origin = mc.player.blockPosition();
        BlockPos best = null; double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-r,-4,-r), origin.offset(r,4,r))) {
            if (mc.level.getBlockState(p).getBlock() instanceof BedBlock) {
                double d = p.distSqr(origin);
                if (d < bestDist) { bestDist = d; best = p.immutable(); }
            }
        }
        trackedBed = best;
    }
}
