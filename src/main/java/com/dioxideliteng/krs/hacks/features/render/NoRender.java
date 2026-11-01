package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.input.Keys;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.ExplodeParticle;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.HugeExplosionSeedParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.TotemParticle;

public class NoRender extends Module {

    @Setting
    public static final BooleanValue allParticles = new BooleanValue("All Particles", false);

    @Setting
    public static final BooleanValue explosions = new BooleanValue("Explosions", true, () -> !allParticles.get());

    @Setting
    public static final BooleanValue blockParticles = new BooleanValue("Block Particles", false, () -> !allParticles.get());

    @Setting
    public static final BooleanValue totemParticles = new BooleanValue("Totem Particles", true, () -> !allParticles.get());

    @Setting
    public static final BooleanValue fireworks = new BooleanValue("Fireworks", false, () -> !allParticles.get());

    @Setting
    public static final BooleanValue campfireSmoke = new BooleanValue("Campfire Smoke", true, () -> !allParticles.get());

    @Setting
    public static final BooleanValue totemAnimation = new BooleanValue("Totem Animation", true);

    @Setting
    public static final BooleanValue fog = new BooleanValue("Fog", false);

    @Setting
    public static final BooleanValue vignette = new BooleanValue("Vignette", true);

    @Setting
    public static final BooleanValue portalOverlay = new BooleanValue("Portal Overlay", true);

    public NoRender() {
        super("No Render", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, false);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public static boolean active(BooleanValue option) {
        return ModuleManager.getModuleState(NoRender.class) && option.get();
    }

    public static boolean shouldHideParticle(Particle particle) {
        if (!ModuleManager.getModuleState(NoRender.class))
            return false;
        if (allParticles.get())
            return true;

        if (explosions.get() && (particle instanceof ExplodeParticle || particle instanceof HugeExplosionParticle || particle instanceof HugeExplosionSeedParticle))
            return true;
        if (blockParticles.get() && particle instanceof TerrainParticle)
            return true;
        if (totemParticles.get() && particle instanceof TotemParticle)
            return true;
        if (fireworks.get() && particle.getClass().getEnclosingClass() == FireworkParticles.class)
            return true;
        return campfireSmoke.get() && particle instanceof CampfireSmokeParticle;
    }
}
