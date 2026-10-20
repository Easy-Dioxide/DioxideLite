package com.dioxideliteng.util.krs;

import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.combat.AntiBot2;
import com.dioxideliteng.module.combat.KillAura2;
import com.dioxideliteng.module.combat.Teams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

public class BehaviorUtils {

    public static final Minecraft mc = Minecraft.getInstance();

    public static boolean noKillAura = false;

    // Krs parity: BehaviorUtils.isBot(entity) resolves ModuleManager.getModuleState(AntiBot.class)
    // itself and gates on it; the Krs AntiBot port in this client is AntiBot2 (inBotList lives
    // there). The legacy client "AntiBot" module is a separate pre-Krs feature and must not
    // be consulted here — its isBot() means "not in the tab list", which is not the Krs
    // bot-list semantic and would invert the KillAura target filter.
    public static boolean isBot(LivingEntity entity) {
        return isBot(entity, isAntiBotEnabled());
    }

    public static boolean isBot(LivingEntity entity, boolean antiBotEnabled) {
        if (!antiBotEnabled || !(entity instanceof Player) || entity instanceof LocalPlayer) return false;
        return AntiBot2.inBotList(entity);
    }

    private static boolean isAntiBotEnabled() {
        var modules = FeatureManager.getModules();
        if (modules == null) return false;
        for (var feature : modules) {
            if (feature instanceof AntiBot2 antiBot2) return antiBot2.isEnabled();
        }
        return false;
    }

    public static boolean isTeammate(LivingEntity entity) {
        if (entity == null) return false;
        Teams teams = findTeamsModule();
        if (teams == null || !teams.isEnabled()) return false;
        if (!(entity instanceof Player)) return false;
        return teams.isInTeam(entity);
    }

    public static List<Entity> getTargetList() {
        ArrayList<Entity> result = new ArrayList<>(4);
        fillTargetList(result);
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    // Krs parity: the target list is fed by the Krs KillAura port (KillAura2:
    // closestEntity + multiTargets), not the legacy client KillAura.
    public static void fillTargetList(List<Entity> result) {
        if (result == null) return;

        result.clear();

        KillAura2 killAura = FeatureManager.killAura2;
        boolean killAuraActive = killAura != null && killAura.isEnabled();
        List<Entity> multi = killAuraActive ? killAura.multiTargets : null;
        Entity closest = killAuraActive ? killAura.closestEntity : null;
        Entity targeted = (mc.crosshairPickEntity instanceof LivingEntity
                && !(mc.crosshairPickEntity instanceof ArmorStand))
                ? mc.crosshairPickEntity : null;

        if (multi != null && !multi.isEmpty()) {
            for (int i = 0, n = multi.size(); i < n; i++) {
                Entity e = multi.get(i);
                if (!result.contains(e)) result.add(e);
            }
        }
        if (closest != null && !result.contains(closest)) result.add(closest);
        if (targeted != null && !result.contains(targeted)) result.add(targeted);
    }

    public static boolean isTarget(Entity entity) {
        if (entity == null) return false;

        KillAura2 killAura = FeatureManager.killAura2;
        if (killAura != null && killAura.isEnabled()) {
            if (entity == killAura.closestEntity) return true;

            List<Entity> multi = killAura.multiTargets;
            for (int i = 0, n = multi.size(); i < n; i++) {
                if (entity == multi.get(i)) return true;
            }
        }

        return mc.crosshairPickEntity == entity
                && entity instanceof LivingEntity
                && !(entity instanceof ArmorStand);
    }

    private static Teams findTeamsModule() {
        var modules = FeatureManager.getModules();
        if (modules == null) return null;
        for (var feature : modules) {
            if (feature instanceof Teams teams) return teams;
        }
        return null;
    }
}
