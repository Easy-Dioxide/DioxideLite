package com.dioxideliteng.krs.utils.render;

import com.dioxideliteng.krs.Client;
import com.dioxideliteng.krs.events.EventListener;
import com.dioxideliteng.krs.events.features.Render3DEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Path / trajectory lines shown by ported KRS modules (KillAura prediction, Backtrack, TargetStrafe...).
 *
 * <p>Same API and timing as KRS's {@code PathLines}: a line stays visible for a short hold time after the last
 * {@code show}, fading in and out. It is drawn with the vanilla gizmo collector, like DioxideLiteNG's own world overlays.
 * Each segment is drawn as a degenerate quad {@code a, b, b, a}, which renders as the stroked segment a-b and only uses
 * gizmo calls DioxideLiteNG already relies on.
 */
public final class PathLines implements EventListener {
    private static final PathLines INSTANCE = new PathLines();
    private static final int MAX_LINES = 16;
    private static final int MAX_POINTS = 512;
    private static final float FADE_IN_SECONDS = 0.12f;
    private static final float FADE_OUT_SECONDS = 0.35f;
    private static final long HOLD_NANOS = 1_500_000_000L;
    private static final double LIFT = 0.05;
    private static final float EDGE_ALPHA = 0.95f;
    private static final float EDGE_WIDTH = 2.0f;

    private final List<Line> lines = new ArrayList<>();
    private boolean registered;
    private long lastFrameNanos;

    private PathLines() {
    }

    public static void show(Object owner, Vec3 from, List<Vec3> path) {
        INSTANCE.put(owner, owner, from, path, RenderColors.DEFAULT);
    }

    public static void show(Object owner, Vec3 from, List<Vec3> path, Color color) {
        INSTANCE.put(owner, owner, from, path, color);
    }

    public static void show(Object owner, Object key, Vec3 from, List<Vec3> path) {
        INSTANCE.put(owner, key, from, path, RenderColors.DEFAULT);
    }

    public static void show(Object owner, Object key, Vec3 from, List<Vec3> path, Color color) {
        INSTANCE.put(owner, key, from, path, color);
    }

    public static void hide(Object owner, Object key) {
        long now = System.nanoTime();
        for (Line line : INSTANCE.lines) {
            if (line.owner == owner && line.key == key && line.holdUntilNanos - now > 0L) {
                line.holdUntilNanos = now;
            }
        }
    }

    private void put(Object owner, Object key, Vec3 from, List<Vec3> path, Color color) {
        if (owner == null || key == null || path == null || path.isEmpty()) return;

        List<Vec3> points = new ArrayList<>(Math.min(MAX_POINTS, path.size() + 1));
        if (from != null) points.add(from);
        for (int i = 0, n = path.size(); i < n && points.size() < MAX_POINTS; i++) {
            Vec3 point = path.get(i);
            if (point != null) points.add(point);
        }
        if (points.size() < 2) return;

        Line line = null;
        for (Line candidate : lines) {
            if (candidate.owner == owner && candidate.key == key) {
                line = candidate;
                break;
            }
        }

        if (line == null) {
            if (lines.size() >= MAX_LINES) {
                Line oldest = lines.get(0);
                for (Line candidate : lines) {
                    if (candidate.holdUntilNanos < oldest.holdUntilNanos) oldest = candidate;
                }
                lines.remove(oldest);
            }
            line = new Line(owner, key);
            lines.add(line);
        }

        line.points = points;
        line.color = color != null ? color : RenderColors.DEFAULT;
        line.holdUntilNanos = System.nanoTime() + HOLD_NANOS;
        setRegistered(true);
    }

    private void clear() {
        lines.clear();
        lastFrameNanos = 0L;
        setRegistered(false);
    }

    private void setRegistered(boolean state) {
        if (registered == state || Client.eventManager == null) return;

        registered = state;
        if (state) Client.eventManager.register(this);
        else Client.eventManager.unregister(this);
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

        for (int i = lines.size() - 1; i >= 0; i--) {
            Line line = lines.get(i);
            boolean visible = now - line.holdUntilNanos < 0L;
            if (visible) line.alpha = Math.min(1f, line.alpha + delta / FADE_IN_SECONDS);
            else line.alpha = Math.max(0f, line.alpha - delta / FADE_OUT_SECONDS);
            if (!visible && line.alpha <= 0f) lines.remove(i);
        }

        if (lines.isEmpty()) {
            clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        for (Line line : lines) {
            if (line.alpha <= 0f || line.points.size() < 2) continue;

            int alpha = Math.round(Math.clamp(line.color.getAlpha() * EDGE_ALPHA * line.alpha, 0f, 255f));
            int argb = alpha << 24 | line.color.getRed() << 16 | line.color.getGreen() << 8 | line.color.getBlue();
            GizmoStyle style = GizmoStyle.stroke(argb, EDGE_WIDTH);

            Vec3 previous = lift(line.points.get(0));
            for (int i = 1; i < line.points.size(); i++) {
                Vec3 next = lift(line.points.get(i));
                Gizmos.rect(previous, next, next, previous, style).setAlwaysOnTop();
                previous = next;
            }
        }
    }

    private static Vec3 lift(Vec3 point) {
        return new Vec3(point.x, point.y + LIFT, point.z);
    }

    private static final class Line {
        final Object owner;
        final Object key;
        List<Vec3> points = List.of();
        Color color = RenderColors.DEFAULT;
        float alpha;
        long holdUntilNanos;

        Line(Object owner, Object key) {
            this.owner = owner;
            this.key = key;
        }
    }
}
