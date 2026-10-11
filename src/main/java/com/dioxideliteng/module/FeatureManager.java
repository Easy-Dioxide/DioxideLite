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
import com.dioxideliteng.module.combat.AntiBot2;
import com.dioxideliteng.module.combat.AutoPot;
import com.dioxideliteng.module.combat.AutoTotem;
import com.dioxideliteng.module.combat.Backtrack2;
import com.dioxideliteng.module.combat.Criticals2;
import com.dioxideliteng.module.combat.CrossbowExploit;
import com.dioxideliteng.module.combat.FastBow;
import com.dioxideliteng.module.combat.FastEat;
import com.dioxideliteng.module.combat.MaceExploit;
import com.dioxideliteng.module.combat.NoCombatDelay;
import com.dioxideliteng.module.combat.Reach;
import com.dioxideliteng.module.combat.Teams;
import com.dioxideliteng.module.combat.Velocity2;
import com.dioxideliteng.module.combat.WTap;
import com.dioxideliteng.module.misc.Disabler;
import com.dioxideliteng.module.misc.Disabler2;
import com.dioxideliteng.module.misc.Whitelist;
import com.dioxideliteng.module.misc.WindCharge;
import com.dioxideliteng.module.misc.BrandSpoofer;
import com.dioxideliteng.module.misc.ConsoleSpammer;
import com.dioxideliteng.module.misc.EntityDesync;
import com.dioxideliteng.module.misc.ExploitPatcher;
import com.dioxideliteng.module.misc.FakePinger;
import com.dioxideliteng.module.misc.LongThrow;
import com.dioxideliteng.module.misc.NoBreakReset;
import com.dioxideliteng.module.misc.PackSpoofer;
import com.dioxideliteng.module.misc.PacketDuper;
import com.dioxideliteng.module.misc.PluginsDetector;
import com.dioxideliteng.module.misc.PortalGodMode;
import com.dioxideliteng.module.misc.ServerCrasher;
import com.dioxideliteng.module.misc.XCarry;
import com.dioxideliteng.module.movement.AntiLevitation;
import com.dioxideliteng.module.movement.AntiSwim;
import com.dioxideliteng.module.movement.AutoRocket;
import com.dioxideliteng.module.movement.AutoSneak;
import com.dioxideliteng.module.movement.AutoWalk;
import com.dioxideliteng.module.movement.AutoWalk2;
import com.dioxideliteng.module.movement.Blink;
import com.dioxideliteng.module.movement.FastFall;
import com.dioxideliteng.module.movement.Flight;
import com.dioxideliteng.module.movement.InventoryMove;
import com.dioxideliteng.module.movement.Jesus;
import com.dioxideliteng.module.movement.KeepSprint;
import com.dioxideliteng.module.movement.LongJump;
import com.dioxideliteng.module.movement.MovementCorrection;
import com.dioxideliteng.module.movement.NoJumpCooldown;
import com.dioxideliteng.module.movement.NoSlow;
import com.dioxideliteng.module.movement.NoWeb;
import com.dioxideliteng.module.movement.Parkour;
import com.dioxideliteng.module.movement.SafeWalk;
import com.dioxideliteng.module.movement.Speed;
import com.dioxideliteng.module.movement.Spider;
import com.dioxideliteng.module.movement.Sprint;
import com.dioxideliteng.module.movement.Stasis;
import com.dioxideliteng.module.movement.Timer;
import com.dioxideliteng.module.movement.WaterSpeed;
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
import com.dioxideliteng.module.player.AirPlace;
import com.dioxideliteng.module.player.AntiAFK;
import com.dioxideliteng.module.player.AutoBypass;
import com.dioxideliteng.module.player.AutoFish;
import com.dioxideliteng.module.player.AutoLeave;
import com.dioxideliteng.module.player.AutoRespawn;
import com.dioxideliteng.module.player.AutoTool2;
import com.dioxideliteng.module.player.ChestStealer2;
import com.dioxideliteng.module.player.DurabilityAlert;
import com.dioxideliteng.module.player.FastBreak;
import com.dioxideliteng.module.player.FastLadder;
import com.dioxideliteng.module.player.HackerDetector;
import com.dioxideliteng.module.player.InvManager2;
import com.dioxideliteng.module.player.MiddleClick;
import com.dioxideliteng.module.player.MurdererDetector;
import com.dioxideliteng.module.player.NoBreakCooldown;
import com.dioxideliteng.module.player.NoFall2;
import com.dioxideliteng.module.player.SneakSpam;
import com.dioxideliteng.module.player.SpinBot;
import com.dioxideliteng.module.player.Timer2;
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
      sortedModules = new ArrayList<>(modules);
      sortModules();
   }

   public static void clientTick() {
      for (Feature feature : modules) feature.initializeWorldState();
      for (Feature feature : modules) feature.clientTick();
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
      modules.add(new AntiBot2());
      modules.add(new AutoPot());
      modules.add(new AutoTotem());
      modules.add(new Backtrack2());
      modules.add(new Criticals2());
      modules.add(new CrossbowExploit());
      modules.add(new FastBow());
      modules.add(new FastEat());
      modules.add(new MaceExploit());
      modules.add(new NoCombatDelay());
      modules.add(new Reach());
      modules.add(new Teams());
      modules.add(new Velocity2());
      modules.add(new WTap());
      modules.add(antiSwim = new AntiSwim());
      modules.add(new AutoWalk());
      modules.add(new AutoWalk2());
      modules.add(new FastFall());
      modules.add(new Flight());
      modules.add(new AntiLevitation());
      modules.add(new AutoRocket());
      modules.add(new AutoSneak());
      modules.add(new Jesus());
      modules.add(new NoJumpCooldown());
      modules.add(new NoWeb());
      modules.add(new Parkour());
      modules.add(new SafeWalk());
      modules.add(new Spider());
      modules.add(new WaterSpeed());
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
      modules.add(new AirPlace());
      modules.add(new AntiAFK());
      modules.add(new AutoBypass());
      modules.add(new AutoFish());
      modules.add(new AutoLeave());
      modules.add(new AutoRespawn());
      modules.add(new AutoTool2());
      modules.add(new ChestStealer2());
      modules.add(new DurabilityAlert());
      modules.add(new FastBreak());
      modules.add(new FastLadder());
      modules.add(new HackerDetector());
      modules.add(new InvManager2());
      modules.add(new MiddleClick());
      modules.add(new MurdererDetector());
      modules.add(new NoBreakCooldown());
      modules.add(new NoFall2());
      modules.add(new SneakSpam());
      modules.add(new SpinBot());
      modules.add(new Timer2());
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
      modules.add(new Disabler2());
      modules.add(whitelist = new Whitelist());
      modules.add(new WindCharge());
      modules.add(new BrandSpoofer());
      modules.add(new ConsoleSpammer());
      modules.add(new EntityDesync());
      modules.add(new ExploitPatcher());
      modules.add(new FakePinger());
      modules.add(new LongThrow());
      modules.add(new NoBreakReset());
      modules.add(new PackSpoofer());
      modules.add(new PacketDuper());
      modules.add(new PluginsDetector());
      modules.add(new PortalGodMode());
      modules.add(new ServerCrasher());
      modules.add(new XCarry());
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
