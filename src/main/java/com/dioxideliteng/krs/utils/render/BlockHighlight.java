package com.dioxideliteng.krs.utils.render;

import com.dioxideliteng.krs.Client;
import com.dioxideliteng.krs.events.EventListener;
import com.dioxideliteng.krs.events.features.Render3DEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Fading block outline used by ported KRS modules (Scaffold, Breaker, Nuker...).
 *
 * <p>Same API and fade behavior as KRS's {@code BlockHighlight}, but drawn with the vanilla gizmo collector exactly like
 * DioxideLiteNG's BedAura target box, so ported modules look like native ones. Drawing happens from
 * {@link Render3DEvent}, which {@code mixins.krs.MixinKrsLevelExtractor} raises inside an open gizmo collection.
 */
public final class BlockHighlight implements EventListener {
    private static final int MAX_ENTRIES = 64;
    private static final float DEFAULT_FADE_IN_SECONDS = 0.12f;
    private static final float DEFAULT_FADE_OUT_SECONDS = 0.2f;
    private static final double EXPAND = 0.002;
    private static final float EDGE_ALPHA = 0.95f;
    private static final float FILL_ALPHA = 0.35f;
    private static final float EDGE_WIDTH = 1.5f;

    private final List<Entry> entries = new ArrayList<>();
    private final Color color;
    private final float fadeInSeconds;
    private final float fadeOutSeconds;
    private boolean registered;
    private long lastFrameNanos;

    public BlockHighlight() {
        this(DEFAULT_FADE_OUT_SECONDS);
    }

    public BlockHighlight(Color color) {
        this(DEFAULT_FADE_OUT_SECONDS, color);
    }

    public BlockHighlight(float fadeOutSeconds) {
        this(fadeOutSeconds, RenderColors.DEFAULT);
    }

    public BlockHighlight(float fadeOutSeconds, Color color) {
        this.color = color == null ? RenderColors.DEFAULT : color;
        this.fadeInSeconds = DEFAULT_FADE_IN_SECONDS;
        this.fadeOutSeconds = Math.max(0.01f, fadeOutSeconds);
    }

    public void setTarget(BlockPos pos) {
        setTarget(pos, null);
    }

    public void setTarget(BlockPos pos, BlockPos mergedPos) {
        releaseTargets();
        if (pos != null) acquire(pos, mergedPos, 0L);
    }

    public void setTargets(List<BlockPos> positions) {
        releaseTargets();
        for (int i = 0, n = Math.min(positions.size(), MAX_ENTRIES); i < n; i++) {
            acquire(positions.get(i), null, 0L);
        }
    }

    public void flash(BlockPos pos, long holdMillis) {
        if (pos != null) acquire(pos, null, System.nanoTime() + holdMillis * 1_000_000L);
    }

    /** Lets the current targets fade out. */
    public void release() {
        releaseTargets();
    }

    /** Removes everything immediately. */
    public void clear() {
        entries.clear();
        lastFrameNanos = 0L;
        setRegistered(false);
    }

    private void setRegistered(boolean state) {
        if (registered == state || Client.eventManager == null) return;

        registered = state;
        if (state) Client.eventManager.register(this);
        else Client.eventManager.unregister(this);
    }

    private void releaseTargets() {
        for (Entry entry : entries) {
            if (entry.holdUntilNanos == 0L) entry.target = false;
        }
    }

    private void acquire(BlockPos pos, BlockPos mergedPos, long holdUntilNanos) {
        BlockPos immutable = pos.immutable();
        for (Entry entry : entries) {
            if (entry.pos.equals(immutable)) {
                entry.target = true;
                entry.merged = mergedPos == null ? null : mergedPos.immutable();
                if (holdUntilNanos != 0L) entry.holdUntilNanos = holdUntilNanos;
                return;
            }
        }

        if (entries.size() >= MAX_ENTRIES) {
            entries.remove(0);
        }
        Entry entry = new Entry(immutable, mergedPos == null ? null : mergedPos.immutable());
        entry.holdUntilNanos = holdUntilNanos;
        entries.add(entry);
        setRegistered(true);
    }

    @Override
    public void onWorld(WorldEvent event) {
        clear();
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 0f : Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        Minecraft mc = Minecraft.getInstance();
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entry.holdUntilNanos != 0L && now - entry.holdUntilNanos >= 0L) {
                entry.target = false;
                entry.holdUntilNanos = 0L;
            }

            if (entry.target) entry.alpha = Math.min(1f, entry.alpha + delta / fadeInSeconds);
            else entry.alpha = Math.max(0f, entry.alpha - delta / fadeOutSeconds);

            if (!entry.target && entry.alpha <= 0f) entries.remove(i);
        }

        if (entries.isEmpty()) {
            clear();
            return;
        }
        if (mc.player == null || mc.level == null) return;

        for (Entry entry : entries) {
            if (entry.alpha <= 0f) continue;
            draw(mc, entry.pos, entry.alpha);
            if (entry.merged != null) draw(mc, entry.merged, entry.alpha);
        }
    }

    private void draw(Minecraft mc, BlockPos pos, float alpha) {
        BlockState state = mc.level.getBlockState(pos);
        VoxelShape shape = state.getShape(mc.level, pos);
        AABB box = (shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos)).inflate(EXPAND);
        Gizmos.cuboid(box, GizmoStyle.strokeAndFill(argb(EDGE_ALPHA * alpha), EDGE_WIDTH, argb(FILL_ALPHA * alpha)))
            .setAlwaysOnTop();
    }

    private int argb(float alphaScale) {
        int alpha = Math.round(Math.clamp(color.getAlpha() * alphaScale, 0f, 255f));
        return alpha << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
    }

    private static final class Entry {
        final BlockPos pos;
        BlockPos merged;
        float alpha;
        boolean target = true;
        long holdUntilNanos;

        Entry(BlockPos pos, BlockPos merged) {
            this.pos = pos;
            this.merged = merged;
        }
    }
}
