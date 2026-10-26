package com.dioxideliteng.krs.utils.pathfinder;

import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.world.phys.Vec3;

public class PathHub {
    private final Vec3 loc;
    private final PathHub parentPathHub;
    private final double sqDist;
    private final double currentCost;
    private final double maxCost;
    private final long sequence;

    public PathHub(final Vec3 loc, final PathHub parentPathHub, final double sqDist,
                   final double currentCost, final double maxCost, final long sequence) {
        this.loc = loc;
        this.parentPathHub = parentPathHub;
        this.sqDist = sqDist;
        this.currentCost = currentCost;
        this.maxCost = maxCost;
        this.sequence = sequence;
    }

    public Vec3 getLoc() {
        return this.loc;
    }

    public ArrayList<Vec3> getPathway() {
        ArrayList<Vec3> pathway = new ArrayList<>();
        for (PathHub hub = this; hub != null; hub = hub.parentPathHub) {
            pathway.add(hub.loc);
        }
        Collections.reverse(pathway);
        return pathway;
    }

    public double getSqDist() {
        return this.sqDist;
    }

    public double getCurrentCost() {
        return this.currentCost;
    }

    public double getMaxCost() {
        return this.maxCost;
    }

    public long getSequence() {
        return this.sequence;
    }
}
