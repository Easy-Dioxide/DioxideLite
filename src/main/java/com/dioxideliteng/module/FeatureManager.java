package com.dioxideliteng.module;

import com.dioxideliteng.module.combat.AimAssist;
import com.dioxideliteng.module.combat.AntiBot;
import com.dioxideliteng.module.combat.AutoClicker;
import com.dioxideliteng.module.combat.AutoRod;
import com.dioxideliteng.module.combat.Criticals;
import com.dioxideliteng.module.combat.KillAura;
import com.dioxideliteng.module.combat.SprintReset;
import com.dioxideliteng.module.combat.TriggerBot;
import com.dioxideliteng.module.combat.Velocity;
import com.dioxideliteng.module.misc.Disabler;
import com.dioxideliteng.module.misc.Whitelist;
import com.dioxideliteng.module.misc.WindCharge;
import com.dioxideliteng.module.movement.AntiSwim;
import com.dioxideliteng.module.movement.AutoWalk;
import com.dioxideliteng.module.movement.Blink;
import com.dioxideliteng.module.movement.Flight;
import com.dioxideliteng.module.movement.InventoryMove;
import com.dioxideliteng.module.movement.KeepSprint;
import com.dioxideliteng.module.movement.LongJump;
import com.dioxideliteng.module.movement.MovementCorrection;
import com.dioxideliteng.module.movement.NoSlow;
import com.dioxideliteng.module.movement.Speed;
import com.dioxideliteng.module.movement.Sprint;
import com.dioxideliteng.module.movement.Stasis;
import com.dioxideliteng.module.movement.Timer;
import com.dioxideliteng.module.player.AutoHead;
import com.dioxideliteng.module.player.AutoTool;
import com.dioxideliteng.module.player.Backtrack;
import com.dioxideliteng.module.player.BedAura;
import com.dioxideliteng.module.player.ChestStealer;
import com.dioxideliteng.module.player.Eagle;
import com.dioxideliteng.module.player.FastMine;
import com.dioxideliteng.module.player.FastPlace;
import com.dioxideliteng.module.player.InventoryManager;
import com.dioxideliteng.module.player.LagRange;
import com.dioxideliteng.module.player.NoFall;
import com.dioxideliteng.module.player.NoJumpDelay;
import com.dioxideliteng.module.player.Scaffold;
import com.dioxideliteng.module.visual.Ambience;
import com.dioxideliteng.module.visual.Animations;
import com.dioxideliteng.module.visual.AntiFire;
import com.dioxideliteng.module.visual.BedPlates;
import com.dioxideliteng.module.visual.Cape;
import com.dioxideliteng.module.visual.ClickGui;
import com.dioxideliteng.module.visual.FullBright;
import com.dioxideliteng.module.visual.Hud;
import com.dioxideliteng.module.visual.NameTags;
import com.dioxideliteng.module.visual.NoHurtCamera;
import com.dioxideliteng.module.visual.PlayerEsp;
import com.dioxideliteng.module.visual.Scoreboard;
import com.dioxideliteng.module.visual.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class FeatureManager {
   public static Velocity velocity;
   public static AutoRod autoRod;
   public static com.dioxideliteng.module.combat.TargetSettings targets;
   public static Cape cape;
   public static Scaffold scaffold;
   public static Theme theme;
   public static AntiBot antiBot;
   public static KillAura killAura;
   public static Scoreboard scoreboard;
   public static AntiFire antiFire;
   public static BedAura bedAura;
   public static LongJump longJump;
   public static Animations animations;
   public static Hud hud;
   public static ClickGui clickGui;
   public static NameTags nameTags;
   public static ChestStealer chestStealer;
   public static Whitelist whitelist;
   public static KeepSprint keepSprint;
   public static AntiSwim antiSwim;
   public static Blink blink;
   public static Stasis stasis;

   private static List<Feature> modules;
   private static List<Feature> sortedModules;

   public static void loadEnabled() {
      // Modules ported from KRS (names that clash with the ones above get a "2" suffix). They are created here, not in
      // registerModules(): this runs once resources are loaded, so options, fonts and the window already exist, and it
      // is still before ConfigManager.loadState() restores module state.
      com.dioxideliteng.krs.KrsModules.registerAll(modules);
      sortedModules = new ArrayList<>(modules);
      sortModules();
   }

   public static void clientTick() {
      for (Feature feature : modules) feature.initializeWorldState();
      for (Feature feature : modules) feature.clientTick();
      resolveRestoredConflicts();
      // Picks up modules enabled by config restore or world join, which never pass through Events.refreshListeners().
      com.dioxideliteng.krs.KrsBridge.refresh();
   }

   public static void clientTickEnd() {
      if (modules == null) return;
      for (Feature feature : modules) feature.clientTickEnd();
   }

   public static void registerModules() {
      modules = new ArrayList<>();
      modules.add(targets = new com.dioxideliteng.module.combat.TargetSettings());
      modules.add(new AimAssist());
      modules.add(antiBot = new AntiBot());
      modules.add(new AutoClicker());
      modules.add(new Criticals());
      modules.add(killAura = new KillAura());
      modules.add(new SprintReset());
      modules.add(new TriggerBot());
      modules.add(velocity = new Velocity());
      modules.add(autoRod = new AutoRod());
      modules.add(antiSwim = new AntiSwim());
      modules.add(new AutoWalk());
      modules.add(new Flight());
      modules.add(new InventoryMove());
      modules.add(keepSprint = new KeepSprint());
      modules.add(longJump = new LongJump());
      modules.add(new MovementCorrection());
      modules.add(new NoSlow());
      modules.add(new Speed());
      modules.add(new Sprint());
      modules.add(stasis = new Stasis());
      modules.add(new Timer());
      modules.add(blink = new Blink());
      modules.add(new AutoTool());
      modules.add(new AutoHead());
      modules.add(new Backtrack());
      modules.add(bedAura = new BedAura());
      modules.add(chestStealer = new ChestStealer());
      modules.add(new Eagle());
      modules.add(new FastMine());
      modules.add(new FastPlace());
      modules.add(new InventoryManager());
      modules.add(new LagRange());
      modules.add(new NoFall());
      modules.add(new NoJumpDelay());
      modules.add(scaffold = new Scaffold());
      modules.add(new Ambience());
      modules.add(animations = new Animations());
      modules.add(antiFire = new AntiFire());
      modules.add(new BedPlates());
      modules.add(cape = new Cape());
      modules.add(clickGui = new ClickGui());
      modules.add(new FullBright());
      modules.add(hud = new Hud());
      modules.add(nameTags = new NameTags());
      modules.add(new NoHurtCamera());
      modules.add(new PlayerEsp());
      modules.add(scoreboard = new Scoreboard());
      modules.add(theme = new Theme());
      modules.add(new Disabler());
      modules.add(whitelist = new Whitelist());
      modules.add(new WindCharge());
   }

   /** Native modules whose ported KRS twin ("<name>2") must never run at the same time. */
   private static final String[] EXCLUSIVE_WITH_TWIN = {"KillAura", "Scaffold"};

   /** Returns the module that must be off while {@code feature} is on, or null when it has no twin. */
   private static Feature exclusiveTwin(Feature feature) {
      if (modules == null) return null;
      String name = feature.getName();
      for (String base : EXCLUSIVE_WITH_TWIN) {
         String twin = name.equals(base) ? base + "2" : name.equals(base + "2") ? base : null;
         if (twin == null) continue;
         for (Feature candidate : modules) {
            if (candidate != feature && candidate.getName().equals(twin)) return candidate;
         }
      }
      return null;
   }

   /** Called when {@code enabled} was just switched on: switches its twin off and says so. */
   static void resolveConflicts(Feature enabled) {
      Feature twin = exclusiveTwin(enabled);
      if (twin == null || !twin.isEnabled()) return;
      twin.setEnabled(false);
      com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager.notifyWarning(twin.getName() + " disabled",
         enabled.getName() + " cannot run together with it");
   }

   /**
    * Config restore enables modules without going through toggle(); if a saved config has both twins on, keep the
    * native one. Runs every tick but only compares two flags per pair.
    */
   private static void resolveRestoredConflicts() {
      for (String base : EXCLUSIVE_WITH_TWIN) {
         Feature native_ = null, twin = null;
         for (Feature candidate : modules) {
            if (candidate.getName().equals(base)) native_ = candidate;
            else if (candidate.getName().equals(base + "2")) twin = candidate;
         }
         if (native_ != null && twin != null && native_.isEnabled() && twin.isEnabled()) resolveConflicts(native_);
      }
   }

   /** True while DioxideLiteNG's Scaffold or the ported KRS Scaffold ("Scaffold2") is running. */
   public static boolean scaffoldActive() {
      return scaffold != null && scaffold.isEnabled() || com.dioxideliteng.krs.KrsBridge.scaffoldEnabled();
   }

   /** The entity the active KillAura - native or the ported KRS "KillAura2" - is attacking right now, or null. */
   public static net.minecraft.world.entity.Entity auraTarget() {
      net.minecraft.world.entity.Entity own = killAura != null && killAura.isEnabled() ? killAura.getTarget() : null;
      return own != null ? own : com.dioxideliteng.krs.KrsBridge.auraTarget();
   }

   public static List<Feature> getModulesByCategory(Category category) {
      ArrayList<Feature> matchingFeatures = new ArrayList<>();

      for (Feature feature : modules) {
         if (feature.getCategory() == category) {
            matchingFeatures.add(feature);
         }
      }

      return matchingFeatures;
   }

   public static void sortModules() {
      sortedModules.sort(
         (leftFeature, rightFeature) -> Integer.compare(Minecraft.getInstance().font.width(rightFeature.getDisplayName()), Minecraft.getInstance().font.width(leftFeature.getDisplayName()))
      );
   }

   public static List<Feature> getSortedModules() {
      return sortedModules;
   }

   public static List<Feature> getModules() {
      return modules;
   }
}
