package com.dioxideliteng.module;

import com.dioxideliteng.module.combat.AimAssist;
import com.dioxideliteng.module.combat.AntiBot;
import com.dioxideliteng.module.combat.AutoClicker;
import com.dioxideliteng.module.combat.AutoRod;
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
import com.dioxideliteng.module.combat.TargetStrafe;
import com.dioxideliteng.module.combat.Teams;
import com.dioxideliteng.module.combat.Velocity2;
import com.dioxideliteng.module.combat.WTap;
import com.dioxideliteng.module.combat.Criticals;
import com.dioxideliteng.module.combat.KillAura;
import com.dioxideliteng.module.combat.KillAura2;
import com.dioxideliteng.module.combat.SprintReset;
import com.dioxideliteng.module.combat.TriggerBot;
import com.dioxideliteng.module.combat.Velocity;
import com.dioxideliteng.module.misc.Disabler;
import com.dioxideliteng.module.misc.PathFinder;
import com.dioxideliteng.module.misc.Whitelist;
import com.dioxideliteng.module.misc.WindCharge;
import com.dioxideliteng.module.misc.AlwaysRiptide;
import com.dioxideliteng.module.misc.AntiCheatDetector;
import com.dioxideliteng.module.misc.AutoBypass2;
import com.dioxideliteng.module.misc.BrandSpoofer;
import com.dioxideliteng.module.misc.Breaker;
import com.dioxideliteng.module.misc.CaveFinder;
import com.dioxideliteng.module.misc.CivBreak;
import com.dioxideliteng.module.misc.ConsoleSpammer;
import com.dioxideliteng.module.misc.Disabler2;
import com.dioxideliteng.module.misc.EntityDesync;
import com.dioxideliteng.module.misc.ExploitPatcher;
import com.dioxideliteng.module.misc.FakePinger;
import com.dioxideliteng.module.misc.ItemDropChanger;
import com.dioxideliteng.module.misc.LongThrow;
import com.dioxideliteng.module.misc.NoBreakCooldown2;
import com.dioxideliteng.module.misc.NoBreakReset;
import com.dioxideliteng.module.misc.NoteBot;
import com.dioxideliteng.module.misc.Nuker;
import com.dioxideliteng.module.misc.PacketDuper;
import com.dioxideliteng.module.misc.PackSpoofer;
import com.dioxideliteng.module.misc.PortalGodMode;
import com.dioxideliteng.module.misc.PortalScreen;
import com.dioxideliteng.module.misc.QuickMacro;
import com.dioxideliteng.module.misc.ServerCrasher;
import com.dioxideliteng.module.misc.Spammer;
import com.dioxideliteng.module.misc.Timer2;
import com.dioxideliteng.module.misc.TPStealer;
import com.dioxideliteng.module.misc.TPSurround;
import com.dioxideliteng.module.misc.TransactionConfirmBlinker;
import com.dioxideliteng.module.misc.XCarry;
import com.dioxideliteng.module.misc.Xray;
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
import com.dioxideliteng.module.movement.AntiLevitation2;
import com.dioxideliteng.module.movement.AntiVoid;
import com.dioxideliteng.module.movement.AutoRocket2;
import com.dioxideliteng.module.movement.AutoSneak2;
import com.dioxideliteng.module.movement.AutoWalk2;
import com.dioxideliteng.module.movement.Blink2;
import com.dioxideliteng.module.movement.ElytraFly;
import com.dioxideliteng.module.movement.EntityControl;
import com.dioxideliteng.module.movement.EntityFly;
import com.dioxideliteng.module.movement.FastFall2;
import com.dioxideliteng.module.movement.Fly2;
import com.dioxideliteng.module.movement.InventoryMove2;
import com.dioxideliteng.module.movement.Jesus2;
import com.dioxideliteng.module.movement.LongJump2;
import com.dioxideliteng.module.movement.MovementFix;
import com.dioxideliteng.module.movement.NoJumpCooldown2;
import com.dioxideliteng.module.movement.NoPush;
import com.dioxideliteng.module.movement.NoSlow2;
import com.dioxideliteng.module.movement.NoWeb2;
import com.dioxideliteng.module.movement.Parkour2;
import com.dioxideliteng.module.movement.PerfectHorseJump;
import com.dioxideliteng.module.movement.SafeWalk2;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.Spider2;
import com.dioxideliteng.module.movement.Sprint2;
import com.dioxideliteng.module.movement.Stasis2;
import com.dioxideliteng.module.movement.Step;
import com.dioxideliteng.module.movement.WaterSpeed2;
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
import com.dioxideliteng.module.player.ChestStealer2;
import com.dioxideliteng.module.player.Scaffold;
import com.dioxideliteng.module.player.Scaffold2;
import com.dioxideliteng.module.player.AirPlace2;
import com.dioxideliteng.module.player.AntiAFK2;
import com.dioxideliteng.module.player.AutoFish2;
import com.dioxideliteng.module.player.AutoLeave2;
import com.dioxideliteng.module.player.AutoRespawn2;
import com.dioxideliteng.module.player.AutoTool2;
import com.dioxideliteng.module.player.ChatCommands;
import com.dioxideliteng.module.player.DurabilityAlert2;
import com.dioxideliteng.module.player.FastBreak2;
import com.dioxideliteng.module.player.FastLadder2;
import com.dioxideliteng.module.player.Freecam;
import com.dioxideliteng.module.player.HackerDetector2;
import com.dioxideliteng.module.player.InvManager2;
import com.dioxideliteng.module.player.LookTP;
import com.dioxideliteng.module.player.MiddleClick2;
import com.dioxideliteng.module.player.MurdererDetector2;
import com.dioxideliteng.module.player.NoFall2;
import com.dioxideliteng.module.player.Phase;
import com.dioxideliteng.module.player.SneakSpam2;
import com.dioxideliteng.module.player.SpinBot2;
import com.dioxideliteng.module.player.Stalker;
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
import com.dioxideliteng.module.visual.Rotations;
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
   public static KillAura2 killAura2;
   public static Scaffold2 scaffold2;
   public static ChestStealer2 chestStealer2;
   public static PathFinder pathFinder;
   public static NoSlow noSlow;
   public static Freecam freecam;

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
      modules.add(killAura2 = new KillAura2());
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
      modules.add(new TargetStrafe());
      modules.add(new Teams());
      modules.add(new Velocity2());
      modules.add(new WTap());
      modules.add(antiSwim = new AntiSwim());
      modules.add(new AutoWalk());
      modules.add(new Flight());
      modules.add(new InventoryMove());
      modules.add(keepSprint = new KeepSprint());
      modules.add(longJump = new LongJump());
      modules.add(new MovementCorrection());
      modules.add(noSlow = new NoSlow());
      modules.add(new Speed());
      modules.add(new Sprint());
      modules.add(stasis = new Stasis());
      modules.add(new Timer());
      modules.add(blink = new Blink());
      modules.add(new AntiLevitation2());
      modules.add(new AntiVoid());
      modules.add(new AutoRocket2());
      modules.add(new AutoSneak2());
      modules.add(new AutoWalk2());
      modules.add(new Blink2());
      modules.add(new ElytraFly());
      modules.add(new EntityControl());
      modules.add(new EntityFly());
      modules.add(new FastFall2());
      modules.add(new Fly2());
      modules.add(new InventoryMove2());
      modules.add(new Jesus2());
      modules.add(new LongJump2());
      modules.add(new MovementFix());
      modules.add(new NoJumpCooldown2());
      modules.add(new NoPush());
      modules.add(new NoSlow2());
      modules.add(new NoWeb2());
      modules.add(new Parkour2());
      modules.add(new PerfectHorseJump());
      modules.add(new SafeWalk2());
      modules.add(new Speed2());
      modules.add(new Spider2());
      modules.add(new Sprint2());
      modules.add(new Stasis2());
      modules.add(new Step());
      modules.add(new WaterSpeed2());
      modules.add(new AutoTool());
      modules.add(new AutoHead());
      modules.add(new Backtrack());
      modules.add(bedAura = new BedAura());
      modules.add(chestStealer = new ChestStealer());
      modules.add(chestStealer2 = new ChestStealer2());
      modules.add(new Eagle());
      modules.add(new FastMine());
      modules.add(new FastPlace());
      modules.add(new InventoryManager());
      modules.add(new LagRange());
      modules.add(new NoFall());
      modules.add(new NoJumpDelay());
      modules.add(scaffold = new Scaffold());
      modules.add(scaffold2 = new Scaffold2());
      modules.add(new AirPlace2());
      modules.add(new AntiAFK2());
      modules.add(new AutoFish2());
      modules.add(new AutoLeave2());
      modules.add(new AutoRespawn2());
      modules.add(new AutoTool2());
      modules.add(new ChatCommands());
      modules.add(new DurabilityAlert2());
      modules.add(new FastBreak2());
      modules.add(new FastLadder2());
      modules.add(freecam = new Freecam());
      modules.add(new HackerDetector2());
      modules.add(new InvManager2());
      modules.add(new LookTP());
      modules.add(new MiddleClick2());
      modules.add(new MurdererDetector2());
      modules.add(new NoFall2());
      modules.add(new Phase());
      modules.add(new SneakSpam2());
      modules.add(new SpinBot2());
      modules.add(new Stalker());
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
      modules.add(new Rotations());
      modules.add(scoreboard = new Scoreboard());
      modules.add(theme = new Theme());
      modules.add(new Disabler());
      modules.add(pathFinder = new PathFinder());
      modules.add(whitelist = new Whitelist());
      modules.add(new WindCharge());
      modules.add(new AlwaysRiptide());
      modules.add(new AntiCheatDetector());
      modules.add(new AutoBypass2());
      modules.add(new BrandSpoofer());
      modules.add(new Breaker());
      modules.add(new CaveFinder());
      modules.add(new CivBreak());
      modules.add(new ConsoleSpammer());
      modules.add(new Disabler2());
      modules.add(new EntityDesync());
      modules.add(new ExploitPatcher());
      modules.add(new FakePinger());
      modules.add(new ItemDropChanger());
      modules.add(new LongThrow());
      modules.add(new NoBreakCooldown2());
      modules.add(new NoBreakReset());
      modules.add(new NoteBot());
      modules.add(new Nuker());
      modules.add(new PacketDuper());
      modules.add(new PackSpoofer());
      modules.add(new PortalGodMode());
      modules.add(new PortalScreen());
      modules.add(new QuickMacro());
      modules.add(new ServerCrasher());
      modules.add(new Spammer());
      modules.add(new Timer2());
      modules.add(new TPStealer());
      modules.add(new TPSurround());
      modules.add(new TransactionConfirmBlinker());
      modules.add(new XCarry());
      modules.add(new Xray());
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
