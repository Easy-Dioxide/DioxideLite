package com.dioxidelite.onyx.engine;

import com.dioxidelite.manager.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Modern 26.1.2 adaptation of OpenOnyx's TargetScanner.
 *
 * The old scanner was tied to the 1.8 entity API.  This version keeps the
 * same conceptual boundary (relation checks + entity classes + range) while
 * delegating the actual world query to the current DioxideLite engine.
 */
public final class OnyxTargetScanner {
    public enum Type { PLAYERS, TEAMMATES, MOBS, ANIMALS }

    private static final Minecraft MC = Minecraft.getInstance();
    private static final OnyxCombatEngine COMBAT = new OnyxCombatEngine();

    private OnyxTargetScanner() {}

    public static Set<Type> defaults() {
        return EnumSet.of(Type.PLAYERS, Type.MOBS, Type.ANIMALS);
    }

    public static List<LivingEntity> scan(double range, Set<Type> types, boolean invisible, int limit) {
        if (types == null || types.isEmpty()) return List.of();
        boolean players = types.contains(Type.PLAYERS) || types.contains(Type.TEAMMATES);
        boolean mobs = types.contains(Type.MOBS);
        boolean animals = types.contains(Type.ANIMALS);
        return COMBAT.acquireTargets(range, players, mobs, animals, false, invisible,
                entity -> relationAllowed(entity, types), Math.max(1, limit));
    }

    private static boolean relationAllowed(LivingEntity entity, Set<Type> types) {
        if (entity instanceof Player player) {
            if (FriendManager.INSTANCE.isFriend(player)) {
                return types.contains(Type.TEAMMATES);
            }
            return types.contains(Type.PLAYERS);
        }
        if (entity instanceof Animal) return types.contains(Type.ANIMALS);
        if (entity instanceof Monster) return types.contains(Type.MOBS);
        return false;
    }
}
