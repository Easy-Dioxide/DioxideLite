package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.events.features.Render3DEvent;
import com.dioxideliteng.krs.events.features.TickEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.input.Keys;
import com.dioxideliteng.krs.utils.render.RenderUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * KRS movement trail.
 *
 * <p>Sampling and curve math are KRS's (one position per tick, Catmull-Rom spline, a short quadratic bridge to the
 * current position). The ribbon is drawn in world space with the vanilla gizmo collector - the same mechanism as
 * DioxideLiteNG's BedAura box - as two passes of gradient segments (wide and faint, thin and bright), instead of KRS's
 * screen-space NanoVG ribbon. That also removes the need for near-plane clipping.
 */
public class Trails extends Module {
    private static final int LENGTH = 20;
    private static final int DELAY = 1;
    private static final double LAG = 0.3;
    private static final int BRIDGE_STEPS = 6;
    private static final int CAPACITY = LENGTH + 8;
    private static final int SUBDIVISIONS = 6;
    private static final int MAX_SAMPLES = LENGTH * SUBDIVISIONS + BRIDGE_STEPS + 4;

    private static final float CORE_WIDTH = 2.5f;
    private static final float GLOW_WIDTH = 7.5f;
    private static final float CORE_ALPHA = 150f;
    private static final float GLOW_ALPHA = 45f;
    private static final Color HEAD_COLOUR = new Color(110, 190, 255);
    private static final Color TAIL_COLOUR = new Color(255, 90, 200);

    private final double[] xs = new double[CAPACITY];
    private final double[] ys = new double[CAPACITY];
    private final double[] zs = new double[CAPACITY];
    private final Vec3[] points = new Vec3[MAX_SAMPLES];
    private final float[] life = new float[MAX_SAMPLES];
    private final double[] curveHead = new double[3];
    private final double[] curveBefore = new double[3];
    private boolean hasSamples;
    private int firstTick;
    private int tickCounter;
    private int pointCount;

    public Trails() {
        super("Trails", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, false);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onWorld(WorldEvent event) {
        reset();
    }

    private void reset() {
        hasSamples = false;
        pointCount = 0;
    }

    @Override
    public void onTick(TickEvent event) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            reset();
            return;
        }

        tickCounter++;
        if (!hasSamples) {
            firstTick = tickCounter;
            hasSamples = true;
        }
        firstTick = Math.max(firstTick, tickCounter - CAPACITY + 1);

        int slot = Math.floorMod(tickCounter, CAPACITY);
        xs[slot] = player.getX();
        ys[slot] = player.getY();
        zs[slot] = player.getZ();
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        pointCount = 0;
        if (mc.player == null || mc.level == null || !hasSamples || !RenderUtil.shouldRenderWorldHudOverlays()) return;

        double now = tickCounter - 1 + event.partialTicks;
        double headTime = now - DELAY;
        double tailTime = Math.max(now - LENGTH, firstTick);
        double endTime = now - LAG;
        double bridge = endTime - headTime;
        double span = endTime - tailTime;
        if (headTime - tailTime < 1.0e-3) return;

        evaluate(tailTime, 0f);
        for (long step = (long) Math.floor(tailTime * SUBDIVISIONS) + 1; pointCount < MAX_SAMPLES - BRIDGE_STEPS - 2; step++) {
            double time = step / (double) SUBDIVISIONS;
            if (time >= headTime - 1.0e-4) break;

            evaluate(time, (float) ((time - tailTime) / span));
        }
        evaluate(headTime, (float) ((headTime - tailTime) / span));

        curve(headTime, curveHead);
        curve(headTime - 0.5, curveBefore);
        int endBase = (int) Math.floor(endTime);
        double endFraction = endTime - endBase;
        double endX = sample(xs, endBase) + (sample(xs, endBase + 1) - sample(xs, endBase)) * endFraction;
        double endY = sample(ys, endBase) + (sample(ys, endBase + 1) - sample(ys, endBase)) * endFraction;
        double endZ = sample(zs, endBase) + (sample(zs, endBase + 1) - sample(zs, endBase)) * endFraction;
        double controlX = curveHead[0] + (curveHead[0] - curveBefore[0]) * bridge;
        double controlY = curveHead[1] + (curveHead[1] - curveBefore[1]) * bridge;
        double controlZ = curveHead[2] + (curveHead[2] - curveBefore[2]) * bridge;
        for (int i = 1; i <= BRIDGE_STEPS; i++) {
            double u = i / (double) BRIDGE_STEPS;
            double a = (1.0 - u) * (1.0 - u);
            double b = 2.0 * (1.0 - u) * u;
            double c = u * u;
            add(a * curveHead[0] + b * controlX + c * endX,
                a * curveHead[1] + b * controlY + c * endY,
                a * curveHead[2] + b * controlZ + c * endZ,
                (float) ((headTime + u * bridge - tailTime) / span));
        }

        draw(GLOW_WIDTH, GLOW_ALPHA);
        draw(CORE_WIDTH, CORE_ALPHA);
    }

    private void evaluate(double time, float remaining) {
        curve(time, curveHead);
        add(curveHead[0], curveHead[1], curveHead[2], remaining);
    }

    private void add(double x, double y, double z, float remaining) {
        if (pointCount >= MAX_SAMPLES) return;
        points[pointCount] = new Vec3(x, y, z);
        life[pointCount] = Math.clamp(remaining, 0f, 1f);
        pointCount++;
    }

    private void curve(double time, double[] output) {
        int base = (int) Math.floor(time);
        float t = (float) (time - base);
        output[0] = catmullRom(sample(xs, base - 1), sample(xs, base), sample(xs, base + 1), sample(xs, base + 2), t);
        output[1] = catmullRom(sample(ys, base - 1), sample(ys, base), sample(ys, base + 1), sample(ys, base + 2), t);
        output[2] = catmullRom(sample(zs, base - 1), sample(zs, base), sample(zs, base + 1), sample(zs, base + 2), t);
    }

    private double sample(double[] values, int tick) {
        return values[Math.floorMod(Math.clamp(tick, firstTick, tickCounter), CAPACITY)];
    }

    /** One pass over the polyline; each segment takes the colour and fade of its midpoint, like KRS's per-chunk gradient. */
    private void draw(float width, float maxAlpha) {
        for (int i = 1; i < pointCount; i++) {
            float lifeMid = (life[i - 1] + life[i]) * 0.5f;
            int alpha = (int) (lifeMid * lifeMid * maxAlpha);
            if (alpha <= 0) continue;

            Color colour = mix(TAIL_COLOUR, HEAD_COLOUR, lifeMid);
            int argb = alpha << 24 | colour.getRed() << 16 | colour.getGreen() << 8 | colour.getBlue();
            Vec3 from = points[i - 1];
            Vec3 to = points[i];
            // A degenerate quad (a, b, b, a) strokes as the single segment a-b; see PathLines.
            Gizmos.rect(from, to, to, from, GizmoStyle.stroke(argb, width)).setAlwaysOnTop();
        }
    }

    private static double catmullRom(double p0, double p1, double p2, double p3, float t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * (2.0 * p1 + (p2 - p0) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (3.0 * p1 - p0 - 3.0 * p2 + p3) * t3);
    }

    private static Color mix(Color a, Color b, float t) {
        float inverse = 1f - t;
        return new Color(
            Math.round(a.getRed() * inverse + b.getRed() * t),
            Math.round(a.getGreen() * inverse + b.getGreen() * t),
            Math.round(a.getBlue() * inverse + b.getBlue() * t)
        );
    }
}
