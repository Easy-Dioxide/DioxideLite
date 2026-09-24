package com.dioxidelite.onyx.engine;

import com.dioxidelite.manager.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * DioxideLite's current-Minecraft port of OpenOnyx's combat engine concepts.
 *
 * The original OpenOnyx feature code targets an older Minecraft mapping and
 * cannot be copied verbatim into 26.1.2. This class preserves the important
 * engine boundary: target filtering, relation checks, range ordering and
 * deterministic selection are kept in one backend used by combat modules.
 */
public final class OnyxCombatEngine {
    private static final Minecraft MC = Minecraft.getInstance();

    public List<LivingEntity> acquireTargets(double range, boolean players, boolean mobs,
                                             boolean animals, boolean villagers, boolean invisible,
                                             Predicate<LivingEntity> extraFilter, int limit) {
        if (MC.level == null || MC.player == null) return List.of();
        double maxSq = range * range;
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : MC.level.getEntitiesOfClass(LivingEntity.class, MC.player.getBoundingBox().inflate(range))) {
            if (entity == MC.player || !entity.isAlive()) continue;
            if (!invisible && entity.isInvisible()) continue;
            if (entity instanceof Player target) {
                if (!players || FriendManager.INSTANCE.isFriend(target)) continue;
            } else if (entity instanceof Animal) {
                if (!animals) continue;
            } else if (entity instanceof Monster) {
                if (!mobs) continue;
            } else {
                if (!mobs && !villagers) continue;
            }
            if (MC.player.distanceToSqr(entity) > maxSq) continue;
            if (extraFilter != null && !extraFilter.test(entity)) continue;
            result.add(entity);
            if (result.size() >= limit) break;
        }
        result.sort(Comparator.comparingDouble(MC.player::distanceToSqr));
        return result;
    }

    /** OpenOnyx-compatible scanner entry point. */
    public List<LivingEntity> scan(double range, java.util.Set<OnyxTargetScanner.Type> types,
                                   boolean invisible, int limit) {
        return OnyxTargetScanner.scan(range, types, invisible, limit);
    }

    public boolean isPotentialBot(Player player) {
        if (player == null || MC.player == null) return true;
        if (player == MC.player || !player.isAlive() || player.isSpectator()) return true;
        return FriendManager.INSTANCE.isFriend(player);
    }
}
