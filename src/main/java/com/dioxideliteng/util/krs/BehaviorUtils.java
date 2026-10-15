package com.dioxideliteng.util.krs;

import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.combat.AntiBot;
import com.dioxideliteng.module.combat.KillAura;
import com.dioxideliteng.module.combat.Teams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

public class BehaviorUtils {

    public static final Minecraft mc = Minecraft.getInstance();

    public static boolean noKillAura = false;

    public static boolean isBot(LivingEntity entity) {
        AntiBot antiBot = FeatureManager.antiBot;
        return antiBot != null && antiBot.isBot(entity);
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

    public static void fillTargetList(List<Entity> result) {
        if (result == null) return;

        result.clear();

        KillAura killAura = FeatureManager.killAura;
        Entity closest = (killAura != null && killAura.isEnabled()) ? killAura.getTarget() : null;
        Entity targeted = (mc.crosshairPickEntity instanceof LivingEntity
                && !(mc.crosshairPickEntity instanceof ArmorStand))
                ? mc.crosshairPickEntity : null;

        if (closest != null && !result.contains(closest)) result.add(closest);
        if (targeted != null && !result.contains(targeted)) result.add(targeted);
    }

    public static boolean isTarget(Entity entity) {
        if (entity == null) return false;

        KillAura killAura = FeatureManager.killAura;
        if (killAura != null && killAura.isEnabled()) {
            if (entity == killAura.getTarget()) return true;
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
