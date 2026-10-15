package com.dioxideliteng.util.pathfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public final class LinearPathFinder {
    public static final Minecraft mc = Minecraft.getInstance();
    public static final LinearPathFinder INSTANCE = new LinearPathFinder();

    private LinearPathFinder() {
    }

    public ArrayList<Vec3> getPaths(BlockPos startPos, BlockPos endPos, int blockPerStep, int maxSteps) {
        ArrayList<Vec3> path = new ArrayList<>();
        if (mc.player == null || startPos == null || endPos == null || blockPerStep <= 0 || maxSteps <= 0)
            return path;

        double dx = endPos.getX() - startPos.getX();
        double dy = endPos.getY() - startPos.getY();
        double dz = endPos.getZ() - startPos.getZ();
        double totalDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(totalDistance) || totalDistance == 0.0) return path;

        int totalSteps = Math.min((int) Math.ceil(totalDistance / blockPerStep), maxSteps);

        for (int i = 1; i <= totalSteps; i++) {
            double t = (double) i / totalSteps;
            double px = startPos.getX() + t * dx;
            double py = startPos.getY() + t * dy;
            double pz = startPos.getZ() + t * dz;
            path.add(new Vec3(px, py, pz));
        }

        return path;
    }
}
