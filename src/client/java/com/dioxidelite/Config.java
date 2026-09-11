package com.dioxidelite;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class Config {
    public static boolean autoMode = false;
    public static boolean swordBlock = false;
    public static boolean useSwing = false;
    public static boolean noSneakAnimation = false;
    public static boolean noDoubleSneak = true;
    public static boolean isChinese = defaultChinese();
    public static boolean autoScreenshot = false;
    public static boolean hitMarker = false;
    public static boolean hitSound = false;
    public static boolean elytraAssist = false;
    public static boolean elytraAutoDeploy = true;
    public static boolean elytraAutoFirework = true;
    public static boolean lowHealthNotify = false;
    public static boolean targetHud = false;
    public static boolean diggingStatus = false;
    public static boolean fallDamagePredict = false;
    public static boolean victorySound = false;
    public static boolean gammaOverride = false;
    public static boolean autoSprint = false;
    public static boolean fishingRodAssist = false;
    public static boolean blockCountDisplay = false;
    public static BlockCountDisplayMode blockCountDisplayMode = BlockCountDisplayMode.NEW;
    public static boolean dynamicIsland = false;
    public static boolean itemPhysics = false;
    public static boolean item2DRender = false;
    public static float itemPhysicsRotationSpeed = 1.0f;
    public static HudTheme hudTheme = HudTheme.DARK;
    /** DioxideLite visual shell used by ClickGUI/HUD surfaces. */
    public enum VisualStyle { AURORA, LIQUID_GLASS, RISE_CLEAN, MINIMAL, SIGNATURE }
    public static VisualStyle visualStyle = VisualStyle.LIQUID_GLASS;
    public enum ClickGuiTheme { ORIGINAL, MINIMAL_POP, SIGNATURE, GLASS }
    public static ClickGuiTheme clickGuiTheme = ClickGuiTheme.ORIGINAL;
    // Presentation-only visual modules inspired by the reference client.
    public static boolean visualWatermark = false;
    public static boolean visualArrayList = false;
    public static boolean visualInfoHud = false;
    public static boolean visualCompass = false;
    public static boolean visualCrosshair = false;
    public static boolean visualParticles = false;
    public static boolean visualFps = false;
    public static boolean visualBps = false;
    public static boolean visualCoordinates = false;
    public static boolean visualSession = false;
    public static boolean visualKeybinds = false;
    public static boolean visualInventory = false;
    public static boolean visualScoreboard = false;
    /** One-click material switch for gameplay visuals/screens (never the main menu). */
    public static boolean liquidGlassAllVisuals = false;
    public static float glassOpacity = 0.62f;
    public static float glassBlur = 1.0f;
    public static float glassHighlight = 0.75f;
    public static float glassRadius = 18.0f;
    public static boolean glassRefraction = true;
    public static boolean performanceMode = false;

    // [v1.8 ADDITION] Signature ClickGUI brand mark (consumed by SignatureLogo).
    public static boolean signatureLogoEnabled = true;
    public static float signatureLogoOpacity = 0.90f;
    public static float signatureLogoGlow = 0.55f;

    // [v1.8 ADDITION] Rendering backend for the Skija GUI layer.
    // true  = SkiaGlBackend: Skija draws straight into the current render target (GPU).
    // false = Surface.makeRaster + peekPixels + writeToTexture (CPU raster, full-frame upload).
    public static boolean gpuFrameRendering = true;
    public static float dynamicIslandWidthScale = 1.0f;
    public static float dynamicIslandHeightScale = 1.0f;
    public static float dynamicIslandBlur = 0.85f;
    public static float dynamicIslandOpacity = 0.72f;
    public static boolean timeChange = false;
    public static boolean weatherChange = false;
    public static boolean armorHud = false;
    public static boolean armorHudShowPercentage = true;
    public static boolean armorHudShowBar = true;
    public static ArmorHudDisplayMode armorHudDisplayMode = ArmorHudDisplayMode.BOTH;
    public static boolean potionStatus = false;
    public static boolean potionStatusBackground = true;
    public static boolean potionStatusCountdown = true;
    public static boolean potionStatusHideVanilla = true;
    public static boolean autoChestDeposit = false;
    public static boolean autoChestDepositResourcesOnly = true;
    public static boolean keystrokes = false;
    public static boolean disableImeInGame = false;
    public static boolean hideSignText = false;
    public static boolean hideEnchantTableBook = false;
    public static boolean hideFireOverlay = false;
    public static boolean hideHurtShake = false;
    public static boolean hideTotemAnimation = false;
    public static boolean hideExplosionParticles = false;
    public static boolean hideRainParticles = false;
    public static boolean hideVignette = false;
    public static boolean hideFog = false;
    public static boolean attackEffectsCritParticles = false;
    public static boolean attackEffectsSharpnessParticles = false;
    public static boolean attackEffectsFlameParticles = false;
    public static boolean attackEffectsBloodParticles = false;
    public static boolean attackEffectsLightning = false;
    public static boolean noAttackCooldownAnimation = false;
    public static boolean customCape = false;
    public static boolean chatHudEditQuickEnable = true;
    public static boolean betterChat = false;
    public static boolean betterChatMessageAnimation = true;
    public static boolean betterChatInputAnimation = true;
    public static boolean betterChatAvatar = true;
    public static boolean smoothHotbarScrolling = false;
    public static float smoothHotbarAnimationSpeed = 0.55f;
    public static int betterChatMessageFadeTime = 170;
    public static int betterChatInputFadeTime = 170;
    public static int hotbarRollover = 0;
    public static boolean useMainUI = true;
    public static boolean mainUICustomBackground = true;
    public static boolean mainUIMouseEffect = false;
    public static boolean termsRead = false;
    public static boolean fullMode = false;
    public static String clientName = "DioxideLite";
    public static String mainUIBackgroundImage = "1.png";
    public static String customCapeImage = "default.png";
    public static HitSoundType hitSoundType = HitSoundType.NETHERITE;
    public static HitSoundCondition hitSoundCondition = HitSoundCondition.BOTH;
    public static TargetHudMode targetHudMode = TargetHudMode.NEW;
    public static KeystrokesMode keystrokesMode = KeystrokesMode.NEW;
    public static ArmorHudMode armorHudMode = ArmorHudMode.NEW;
    public static ArmorHudLayout armorHudLayout = ArmorHudLayout.SEPARATED;
    public static double range = 3.0;
    public static float animSpeed = 1.0f;
    public static float sneakDropScale = 0.5f;
    public static float sneakAnimationSpeed = 1.0f;
    public static float targetHudX = -300f;
    public static float targetHudY = -100f;
    public static float targetHudZ = 0f;
    public static float targetHudScale = 1.0f;
    public static float keystrokesX = -170f;
    public static float keystrokesY = 70f;
    public static float keystrokesScale = 1.0f;
    public static boolean nameTag = false;
    public static float nameTagScale = 1.0f;
    public static boolean nameTagDynamicScale = false;
    public static boolean nameTagOnlyPlayer = false;
    public static boolean dynamicMotionBlur = false;
    public static float dynamicMotionBlurStrength = 1.0f;
    public static boolean dynamicMotionBlurRefreshRateScaling = true;
    public static float attackEffectsCritMultiplier = 1.0f;
    public static float attackEffectsSharpnessMultiplier = 1.0f;
    public static float attackEffectsFlameMultiplier = 1.0f;
    public static float attackEffectsBloodMultiplier = 1.0f;
    public static int attackEffectsLightningCount = 1;
    public static float blockCountDisplayX = 0f;
    public static float blockCountDisplayY = 0f;
    public static float blockCountDisplayScale = 1.0f;
    public static float armorHudX = 0f;
    public static float armorHudY = 0f;
    public static float armorHudScale = 1.0f;
    public static float notificationX = Float.NaN;
    public static float notificationY = Float.NaN;
    public static float notificationScale = 1.0f;
    public static float potionStatusX = 0f;
    public static float potionStatusY = 0f;
    public static float potionStatusScale = 1.0f;
    public static float offsetX = 0f;
    public static float offsetY = 0f;
    public static float offsetZ = 0f;
    public static double gammaValue = 15.0;
    public static int fishingRodAssistUseDelay = 0;
    public static int autoChestDepositDepositDelay = 4;
    public static int autoChestDepositCloseDelay = 4;
    public static int clientTime = 6000;
    public static WeatherMode weatherMode = WeatherMode.CLEAR;

    public enum AnimMode { MODE_1_7, MODE_PUSH, MODE_1_7_PLUS, MODE_NEW }
    public enum HitSoundType { NETHERITE, EXPERIENCE }
    public enum HitSoundCondition { BOTH, MELEE, RANGED }
    public enum TargetHudMode { LITE, NEW, BLUR }
    public enum BlockCountDisplayMode { NEW, BLUR }
    public enum KeystrokesMode { LITE, NEW }
    public enum ArmorHudMode { LITE, NEW }
    public enum ArmorHudLayout { SEPARATED, VERTICAL, HORIZONTAL }
    public enum ArmorHudDisplayMode { PERCENTAGE, BAR, BOTH }
    public enum MotionBlurAlgorithm { VELOCITY_BASED, FRAME_BLENDING, HYBRID_BLENDING, ACCUMULATION_MAX, ACCUMULATION_MIX }
    public enum HudTheme { DARK, LIGHT }
    public enum WeatherMode { CLEAR, RAIN, SNOW, THUNDER }

    public static int hudPrimaryTextColor() {
        return hudTheme == HudTheme.LIGHT ? 0xFF111827 : 0xFFFFFFFF;
    }

    public static int hudSecondaryTextColor() {
        return hudTheme == HudTheme.LIGHT ? 0xAA111827 : 0xCCFFFFFF;
    }

    public static int hudMutedTextColor() {
        return hudTheme == HudTheme.LIGHT ? 0xAA5C5870 : 0xBFFFFFFF;
    }

    public static int hudBorderColor() {
        return hudTheme == HudTheme.LIGHT ? 0x55111827 : 0x55FFFFFF;
    }

    public static AnimMode animationMode = AnimMode.MODE_1_7;
    public static MotionBlurAlgorithm motionBlurAlgorithm = MotionBlurAlgorithm.VELOCITY_BASED;

    private static final Path CONFIG_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("DioxideLite");
    private static final Path CONFIG_FILE = CONFIG_DIRECTORY.resolve("Config.cfg");
    private static final Path LEGACY_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("dioxide_lite.properties");

    public static void load() {
        ensureConfigDirectory();
        migrateLegacyConfigIfNeeded();
        if (!Files.exists(CONFIG_FILE)) {
            applyGameLanguageDefault();
            save();
            return;
        }
        boolean needsVisualDefaultsSave = false;
        try (InputStream is = Files.newInputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.load(is);
            boolean visualDefaultsV2 = "2".equals(prop.getProperty("visualModuleDefaultsVersion"));
            autoMode = Boolean.parseBoolean(prop.getProperty("autoMode", "false"));
            swordBlock = Boolean.parseBoolean(prop.getProperty("swordBlock", "false"));
            useSwing = Boolean.parseBoolean(prop.getProperty("useSwing", "false"));
            noSneakAnimation = Boolean.parseBoolean(prop.getProperty("noSneakAnimation", "false"));
            noDoubleSneak = true;
            isChinese = Boolean.parseBoolean(prop.getProperty("isChinese", String.valueOf(defaultChinese())));
            autoScreenshot = Boolean.parseBoolean(prop.getProperty("autoScreenshot", "false"));
            hitMarker = Boolean.parseBoolean(prop.getProperty("hitMarker", "false"));
            hitSound = Boolean.parseBoolean(prop.getProperty("hitSound", "false"));
            elytraAssist = Boolean.parseBoolean(prop.getProperty("elytraAssist", "false"));
            elytraAutoDeploy = Boolean.parseBoolean(prop.getProperty("elytraAutoDeploy", "true"));
            elytraAutoFirework = Boolean.parseBoolean(prop.getProperty("elytraAutoFirework", "true"));
            lowHealthNotify = Boolean.parseBoolean(prop.getProperty("lowHealthNotify", "false"));
            targetHud = Boolean.parseBoolean(prop.getProperty("targetHud", "false"));
            diggingStatus = Boolean.parseBoolean(prop.getProperty("diggingStatus", "false"));
            fallDamagePredict = Boolean.parseBoolean(prop.getProperty("fallDamagePredict", "false"));
            victorySound = Boolean.parseBoolean(prop.getProperty("victorySound", "false"));
            gammaOverride = Boolean.parseBoolean(prop.getProperty("gammaOverride", "false"));
            autoSprint = Boolean.parseBoolean(prop.getProperty("autoSprint", "false"));
            fishingRodAssist = Boolean.parseBoolean(prop.getProperty("fishingRodAssist", "false"));
            blockCountDisplay = Boolean.parseBoolean(prop.getProperty("blockCountDisplay", "false"));
            blockCountDisplayMode = parseEnum(prop.getProperty("blockCountDisplayMode", "NEW"), BlockCountDisplayMode.NEW, BlockCountDisplayMode.class);
            dynamicIsland = Boolean.parseBoolean(prop.getProperty("dynamicIsland", "false"));
            itemPhysics = Boolean.parseBoolean(prop.getProperty("itemPhysics", "false"));
            item2DRender = Boolean.parseBoolean(prop.getProperty("item2DRender", "false"));
            itemPhysicsRotationSpeed = Float.parseFloat(prop.getProperty("itemPhysicsRotationSpeed", "1.0"));
            hudTheme = parseHudTheme(prop.getProperty("hudTheme", prop.getProperty("hudTheme", "DARK")));
            visualStyle = parseVisualStyle(prop.getProperty("visualStyle", "LIQUID_GLASS"));
            clickGuiTheme = parseEnum(prop.getProperty("clickGuiTheme", "ORIGINAL"), ClickGuiTheme.ORIGINAL, ClickGuiTheme.class);
            visualWatermark = Boolean.parseBoolean(prop.getProperty("visualWatermark", "false"));
            visualArrayList = Boolean.parseBoolean(prop.getProperty("visualArrayList", "false"));
            visualInfoHud = Boolean.parseBoolean(prop.getProperty("visualInfoHud", "false"));
            visualCompass = Boolean.parseBoolean(prop.getProperty("visualCompass", "false"));
            visualCrosshair = Boolean.parseBoolean(prop.getProperty("visualCrosshair", "false"));
            visualParticles = Boolean.parseBoolean(prop.getProperty("visualParticles", "false"));
            visualFps = Boolean.parseBoolean(prop.getProperty("visualFps", "false"));
            visualBps = Boolean.parseBoolean(prop.getProperty("visualBps", "false"));
            visualCoordinates = Boolean.parseBoolean(prop.getProperty("visualCoordinates", "false"));
            visualSession = Boolean.parseBoolean(prop.getProperty("visualSession", "false"));
            visualKeybinds = Boolean.parseBoolean(prop.getProperty("visualKeybinds", "false"));
            visualInventory = Boolean.parseBoolean(prop.getProperty("visualInventory", "false"));
            visualScoreboard = Boolean.parseBoolean(prop.getProperty("visualScoreboard", "false"));
            liquidGlassAllVisuals = Boolean.parseBoolean(prop.getProperty("liquidGlassAllVisuals", "false"));
            glassOpacity = clamp01(finiteOrDefault(Float.parseFloat(prop.getProperty("glassOpacity", "0.62")), 0.62f));
            glassBlur = clamp(Float.parseFloat(prop.getProperty("glassBlur", "1.0")), 0f, 2f, 1.0f);
            glassHighlight = clamp01(finiteOrDefault(Float.parseFloat(prop.getProperty("glassHighlight", "0.75")), 0.75f));
            glassRadius = clamp(Float.parseFloat(prop.getProperty("glassRadius", "18.0")), 8f, 32f, 18.0f);
            glassRefraction = Boolean.parseBoolean(prop.getProperty("glassRefraction", "true"));
            performanceMode = Boolean.parseBoolean(prop.getProperty("performanceMode", "false"));
            signatureLogoEnabled = Boolean.parseBoolean(prop.getProperty("signatureLogoEnabled", "true"));
            signatureLogoOpacity = clamp01(finiteOrDefault(Float.parseFloat(prop.getProperty("signatureLogoOpacity", "0.90")), 0.90f));
            signatureLogoGlow = clamp01(finiteOrDefault(Float.parseFloat(prop.getProperty("signatureLogoGlow", "0.55")), 0.55f));
            gpuFrameRendering = Boolean.parseBoolean(prop.getProperty("gpuFrameRendering", "true"));
            dynamicIslandWidthScale = clamp(Float.parseFloat(prop.getProperty("dynamicIslandWidthScale", "1.0")), 0.55f, 1.8f, 1.0f);
            dynamicIslandHeightScale = clamp(Float.parseFloat(prop.getProperty("dynamicIslandHeightScale", "1.0")), 0.65f, 1.6f, 1.0f);
            dynamicIslandBlur = clamp(Float.parseFloat(prop.getProperty("dynamicIslandBlur", "0.85")), 0f, 2f, 0.85f);
            dynamicIslandOpacity = clamp01(finiteOrDefault(Float.parseFloat(prop.getProperty("dynamicIslandOpacity", "0.72")), 0.72f));
            timeChange = Boolean.parseBoolean(prop.getProperty("timeChange", "false"));
            weatherChange = Boolean.parseBoolean(prop.getProperty("weatherChange", "false"));
            armorHud = Boolean.parseBoolean(prop.getProperty("armorHud", "false"));
            armorHudShowPercentage = Boolean.parseBoolean(prop.getProperty("armorHudShowPercentage", prop.getProperty("armorHudLitePercentage", "true")));
            armorHudShowBar = Boolean.parseBoolean(prop.getProperty("armorHudShowBar", prop.getProperty("armorHudLiteBar", "true")));
            armorHudDisplayMode = parseEnum(prop.getProperty("armorHudDisplayMode", "BOTH"), ArmorHudDisplayMode.BOTH, ArmorHudDisplayMode.class);
            potionStatus = Boolean.parseBoolean(prop.getProperty("potionStatus", "false"));
            potionStatusBackground = Boolean.parseBoolean(prop.getProperty("potionStatusBackground", "true"));
            potionStatusCountdown = Boolean.parseBoolean(prop.getProperty("potionStatusCountdown", "true"));
            potionStatusHideVanilla = Boolean.parseBoolean(prop.getProperty("potionStatusHideVanilla", "true"));
            autoChestDeposit = Boolean.parseBoolean(prop.getProperty("autoChestDeposit", "false"));
            autoChestDepositResourcesOnly = Boolean.parseBoolean(prop.getProperty("autoChestDepositResourcesOnly", "true"));
            keystrokes = Boolean.parseBoolean(prop.getProperty("keystrokes", "false"));
            disableImeInGame = Boolean.parseBoolean(prop.getProperty("disableImeInGame", "false"));
            hideSignText = Boolean.parseBoolean(prop.getProperty("hideSignText", "false"));
            hideEnchantTableBook = Boolean.parseBoolean(prop.getProperty("hideEnchantTableBook", "false"));
            hideFireOverlay = Boolean.parseBoolean(prop.getProperty("hideFireOverlay", "false"));
            hideHurtShake = Boolean.parseBoolean(prop.getProperty("hideHurtShake", "false"));
            hideTotemAnimation = Boolean.parseBoolean(prop.getProperty("hideTotemAnimation", "false"));
            hideExplosionParticles = Boolean.parseBoolean(prop.getProperty("hideExplosionParticles", "false"));
            hideRainParticles = Boolean.parseBoolean(prop.getProperty("hideRainParticles", "false"));
            hideVignette = Boolean.parseBoolean(prop.getProperty("hideVignette", "false"));
            hideFog = Boolean.parseBoolean(prop.getProperty("hideFog", "false"));
            attackEffectsCritParticles = Boolean.parseBoolean(prop.getProperty("attackEffectsCritParticles", "false"));
            attackEffectsSharpnessParticles = Boolean.parseBoolean(prop.getProperty("attackEffectsSharpnessParticles", "false"));
            attackEffectsFlameParticles = Boolean.parseBoolean(prop.getProperty("attackEffectsFlameParticles", "false"));
            attackEffectsBloodParticles = Boolean.parseBoolean(prop.getProperty("attackEffectsBloodParticles", "false"));
            attackEffectsLightning = Boolean.parseBoolean(prop.getProperty("attackEffectsLightning", "false"));
            noAttackCooldownAnimation = Boolean.parseBoolean(prop.getProperty("noAttackCooldownAnimation", "false"));
            customCape = Boolean.parseBoolean(prop.getProperty("customCape", "false"));
            chatHudEditQuickEnable = Boolean.parseBoolean(prop.getProperty("chatHudEditQuickEnable", "true"));
            betterChat = Boolean.parseBoolean(prop.getProperty("betterChat", "false"));
            betterChatMessageAnimation = Boolean.parseBoolean(prop.getProperty("betterChatMessageAnimation", "true"));
            betterChatInputAnimation = Boolean.parseBoolean(prop.getProperty("betterChatInputAnimation", "true"));
            betterChatAvatar = Boolean.parseBoolean(prop.getProperty("betterChatAvatar", "true"));
            smoothHotbarScrolling = Boolean.parseBoolean(prop.getProperty("smoothHotbarScrolling", "false"));
            smoothHotbarAnimationSpeed = Float.parseFloat(prop.getProperty("smoothHotbarAnimationSpeed", "0.55"));
            betterChatMessageFadeTime = Integer.parseInt(prop.getProperty("betterChatMessageFadeTime", "170"));
            betterChatInputFadeTime = Integer.parseInt(prop.getProperty("betterChatInputFadeTime", "170"));
            hotbarRollover = Integer.parseInt(prop.getProperty("hotbarRollover", "0"));
            useMainUI = Boolean.parseBoolean(prop.getProperty("useMainUI", "true"));
            mainUICustomBackground = Boolean.parseBoolean(prop.getProperty("mainUICustomBackground", "true"));
            mainUIMouseEffect = Boolean.parseBoolean(prop.getProperty("mainUIMouseEffect", "false"));
            termsRead = Boolean.parseBoolean(prop.getProperty("termsRead", "false"));
            fullMode = Boolean.parseBoolean(prop.getProperty("fullMode", "false"));
            clientName = prop.getProperty("clientName", "DioxideLite");
            mainUIBackgroundImage = prop.getProperty("mainUIBackgroundImage", "1.png");
            customCapeImage = prop.getProperty("customCapeImage", "default.png");
            hitSoundType = parseEnum(prop.getProperty("hitSoundType", "NETHERITE"), HitSoundType.NETHERITE, HitSoundType.class);
            hitSoundCondition = parseEnum(prop.getProperty("hitSoundCondition", "BOTH"), HitSoundCondition.BOTH, HitSoundCondition.class);
            targetHudMode = parseEnum(prop.getProperty("targetHudMode", "NEW"), TargetHudMode.NEW, TargetHudMode.class);
            keystrokesMode = parseEnum(prop.getProperty("keystrokesMode", "NEW"), KeystrokesMode.NEW, KeystrokesMode.class);
            armorHudMode = parseEnum(prop.getProperty("armorHudMode", "NEW"), ArmorHudMode.NEW, ArmorHudMode.class);
            armorHudLayout = parseEnum(prop.getProperty("armorHudLayout", "SEPARATED"), ArmorHudLayout.SEPARATED, ArmorHudLayout.class);
            range = Double.parseDouble(prop.getProperty("range", "3.0"));
            animSpeed = Float.parseFloat(prop.getProperty("animSpeed", "1.0"));
            sneakDropScale = Float.parseFloat(prop.getProperty("sneakDropScale", "0.5"));
            sneakAnimationSpeed = Float.parseFloat(prop.getProperty("sneakAnimationSpeed", "1.0"));
            animationMode = parseEnum(prop.getProperty("animationMode", "MODE_1_7"), AnimMode.MODE_1_7, AnimMode.class);
            motionBlurAlgorithm = parseEnum(prop.getProperty("motionBlurAlgorithm", "VELOCITY_BASED"), MotionBlurAlgorithm.VELOCITY_BASED, MotionBlurAlgorithm.class);
            weatherMode = parseEnum(prop.getProperty("weatherMode", "CLEAR"), WeatherMode.CLEAR, WeatherMode.class);
            offsetX = Float.parseFloat(prop.getProperty("offsetX", "0"));
            offsetY = Float.parseFloat(prop.getProperty("offsetY", "0"));
            offsetZ = Float.parseFloat(prop.getProperty("offsetZ", "0"));
            targetHudX = Float.parseFloat(prop.getProperty("targetHudX", "-300"));
            targetHudY = Float.parseFloat(prop.getProperty("targetHudY", "-100"));
            targetHudZ = Float.parseFloat(prop.getProperty("targetHudZ", "0"));
            targetHudScale = Float.parseFloat(prop.getProperty("targetHudScale", "1.0"));
            keystrokesX = Float.parseFloat(prop.getProperty("keystrokesX", "-170"));
            keystrokesY = Float.parseFloat(prop.getProperty("keystrokesY", "70"));
            keystrokesScale = Float.parseFloat(prop.getProperty("keystrokesScale", "1.0"));
            nameTag = Boolean.parseBoolean(prop.getProperty("nameTag", "false"));
            nameTagScale = Float.parseFloat(prop.getProperty("nameTagScale", "1.0"));
            nameTagDynamicScale = Boolean.parseBoolean(prop.getProperty("nameTagDynamicScale", "false"));
            nameTagOnlyPlayer = Boolean.parseBoolean(prop.getProperty("nameTagOnlyPlayer", "false"));
            dynamicMotionBlur = Boolean.parseBoolean(prop.getProperty("dynamicMotionBlur", "false"));
            dynamicMotionBlurStrength = Float.parseFloat(prop.getProperty("dynamicMotionBlurStrength", "1.0"));
            dynamicMotionBlurRefreshRateScaling = Boolean.parseBoolean(prop.getProperty("dynamicMotionBlurRefreshRateScaling", "true"));
            attackEffectsCritMultiplier = Float.parseFloat(prop.getProperty("attackEffectsCritMultiplier", "1.0"));
            attackEffectsSharpnessMultiplier = Float.parseFloat(prop.getProperty("attackEffectsSharpnessMultiplier", "1.0"));
            attackEffectsFlameMultiplier = Float.parseFloat(prop.getProperty("attackEffectsFlameMultiplier", "1.0"));
            attackEffectsBloodMultiplier = Float.parseFloat(prop.getProperty("attackEffectsBloodMultiplier", "1.0"));
            attackEffectsLightningCount = Integer.parseInt(prop.getProperty("attackEffectsLightningCount", "1"));
            blockCountDisplayX = Float.parseFloat(prop.getProperty("blockCountDisplayX", "0"));
            blockCountDisplayY = Float.parseFloat(prop.getProperty("blockCountDisplayY", "0"));
            blockCountDisplayScale = Float.parseFloat(prop.getProperty("blockCountDisplayScale", "1.0"));
            armorHudX = Float.parseFloat(prop.getProperty("armorHudX", "0"));
            armorHudY = Float.parseFloat(prop.getProperty("armorHudY", "0"));
            armorHudScale = Float.parseFloat(prop.getProperty("armorHudScale", "1.0"));
            notificationX = Float.parseFloat(prop.getProperty("notificationX", "NaN"));
            notificationY = Float.parseFloat(prop.getProperty("notificationY", "NaN"));
            notificationScale = Float.parseFloat(prop.getProperty("notificationScale", "1.0"));
            potionStatusX = Float.parseFloat(prop.getProperty("potionStatusX", "0"));
            potionStatusY = Float.parseFloat(prop.getProperty("potionStatusY", "0"));
            potionStatusScale = Float.parseFloat(prop.getProperty("potionStatusScale", "1.0"));
            gammaValue = Double.parseDouble(prop.getProperty("gammaValue", "15.0"));
            fishingRodAssistUseDelay = Integer.parseInt(prop.getProperty("fishingRodAssistUseDelay", "0"));
            autoChestDepositDepositDelay = Integer.parseInt(prop.getProperty("autoChestDepositDepositDelay", "4"));
            autoChestDepositCloseDelay = Integer.parseInt(prop.getProperty("autoChestDepositCloseDelay", "4"));
            clientTime = Integer.parseInt(prop.getProperty("clientTime", "6000"));
            // Older DioxideLite builds shipped several presentation overlays enabled by default.
            // Migrate existing configs once so the new visual modules are genuinely opt-in.
            if (!visualDefaultsV2) {
                visualWatermark = false;
                visualArrayList = false;
                visualInfoHud = false;
                visualCompass = false;
                visualCrosshair = false;
                visualParticles = false;
                prop.setProperty("visualModuleDefaultsVersion", "2");
                needsVisualDefaultsSave = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.err.println("[DioxideLite] Invalid configuration; keeping safe defaults for malformed entries: " + e.getMessage());
        }
        if (needsVisualDefaultsSave) save();
    }

    public static void save() {
        ensureConfigDirectory();
        try (OutputStream os = Files.newOutputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.setProperty("autoMode", String.valueOf(autoMode));
            prop.setProperty("swordBlock", String.valueOf(swordBlock));
            prop.setProperty("useSwing", String.valueOf(useSwing));
            prop.setProperty("noSneakAnimation", String.valueOf(noSneakAnimation));
            prop.setProperty("isChinese", String.valueOf(isChinese));
            prop.setProperty("autoScreenshot", String.valueOf(autoScreenshot));
            prop.setProperty("hitMarker", String.valueOf(hitMarker));
            prop.setProperty("hitSound", String.valueOf(hitSound));
            prop.setProperty("elytraAssist", String.valueOf(elytraAssist));
            prop.setProperty("elytraAutoDeploy", String.valueOf(elytraAutoDeploy));
            prop.setProperty("elytraAutoFirework", String.valueOf(elytraAutoFirework));
            prop.setProperty("lowHealthNotify", String.valueOf(lowHealthNotify));
            prop.setProperty("targetHud", String.valueOf(targetHud));
            prop.setProperty("diggingStatus", String.valueOf(diggingStatus));
            prop.setProperty("fallDamagePredict", String.valueOf(fallDamagePredict));
            prop.setProperty("victorySound", String.valueOf(victorySound));
            prop.setProperty("gammaOverride", String.valueOf(gammaOverride));
            prop.setProperty("autoSprint", String.valueOf(autoSprint));
            prop.setProperty("fishingRodAssist", String.valueOf(fishingRodAssist));
            prop.setProperty("blockCountDisplay", String.valueOf(blockCountDisplay));
            prop.setProperty("blockCountDisplayMode", blockCountDisplayMode.name());
            prop.setProperty("dynamicIsland", String.valueOf(dynamicIsland));
            prop.setProperty("itemPhysics", String.valueOf(itemPhysics));
            prop.setProperty("item2DRender", String.valueOf(item2DRender));
            prop.setProperty("itemPhysicsRotationSpeed", String.valueOf(itemPhysicsRotationSpeed));
            prop.setProperty("hudTheme", hudTheme.name());
            prop.setProperty("visualStyle", visualStyle.name());
            prop.setProperty("clickGuiTheme", clickGuiTheme.name());
            prop.setProperty("visualWatermark", String.valueOf(visualWatermark));
            prop.setProperty("visualArrayList", String.valueOf(visualArrayList));
            prop.setProperty("visualInfoHud", String.valueOf(visualInfoHud));
            prop.setProperty("visualCompass", String.valueOf(visualCompass));
            prop.setProperty("visualCrosshair", String.valueOf(visualCrosshair));
            prop.setProperty("visualParticles", String.valueOf(visualParticles));
            prop.setProperty("visualFps", String.valueOf(visualFps));
            prop.setProperty("visualBps", String.valueOf(visualBps));
            prop.setProperty("visualCoordinates", String.valueOf(visualCoordinates));
            prop.setProperty("visualSession", String.valueOf(visualSession));
            prop.setProperty("visualKeybinds", String.valueOf(visualKeybinds));
            prop.setProperty("visualInventory", String.valueOf(visualInventory));
            prop.setProperty("visualScoreboard", String.valueOf(visualScoreboard));
            prop.setProperty("liquidGlassAllVisuals", String.valueOf(liquidGlassAllVisuals));
            prop.setProperty("visualModuleDefaultsVersion", "2");
            prop.setProperty("glassOpacity", String.valueOf(glassOpacity));
            prop.setProperty("glassBlur", String.valueOf(glassBlur));
            prop.setProperty("glassHighlight", String.valueOf(glassHighlight));
            prop.setProperty("glassRadius", String.valueOf(glassRadius));
            prop.setProperty("glassRefraction", String.valueOf(glassRefraction));
            prop.setProperty("performanceMode", String.valueOf(performanceMode));
            prop.setProperty("signatureLogoEnabled", String.valueOf(signatureLogoEnabled));
            prop.setProperty("signatureLogoOpacity", String.valueOf(signatureLogoOpacity));
            prop.setProperty("signatureLogoGlow", String.valueOf(signatureLogoGlow));
            prop.setProperty("gpuFrameRendering", String.valueOf(gpuFrameRendering));
            prop.setProperty("dynamicIslandWidthScale", String.valueOf(dynamicIslandWidthScale));
            prop.setProperty("dynamicIslandHeightScale", String.valueOf(dynamicIslandHeightScale));
            prop.setProperty("dynamicIslandBlur", String.valueOf(dynamicIslandBlur));
            prop.setProperty("dynamicIslandOpacity", String.valueOf(dynamicIslandOpacity));
            prop.setProperty("timeChange", String.valueOf(timeChange));
            prop.setProperty("weatherChange", String.valueOf(weatherChange));
            prop.setProperty("armorHud", String.valueOf(armorHud));
            prop.setProperty("armorHudShowPercentage", String.valueOf(armorHudShowPercentage));
            prop.setProperty("armorHudShowBar", String.valueOf(armorHudShowBar));
            prop.setProperty("armorHudDisplayMode", armorHudDisplayMode.name());
            prop.setProperty("potionStatus", String.valueOf(potionStatus));
            prop.setProperty("potionStatusBackground", String.valueOf(potionStatusBackground));
            prop.setProperty("potionStatusCountdown", String.valueOf(potionStatusCountdown));
            prop.setProperty("potionStatusHideVanilla", String.valueOf(potionStatusHideVanilla));
            prop.setProperty("autoChestDeposit", String.valueOf(autoChestDeposit));
            prop.setProperty("autoChestDepositResourcesOnly", String.valueOf(autoChestDepositResourcesOnly));
            prop.setProperty("keystrokes", String.valueOf(keystrokes));
            prop.setProperty("disableImeInGame", String.valueOf(disableImeInGame));
            prop.setProperty("hideSignText", String.valueOf(hideSignText));
            prop.setProperty("hideEnchantTableBook", String.valueOf(hideEnchantTableBook));
            prop.setProperty("hideFireOverlay", String.valueOf(hideFireOverlay));
            prop.setProperty("hideHurtShake", String.valueOf(hideHurtShake));
            prop.setProperty("hideTotemAnimation", String.valueOf(hideTotemAnimation));
            prop.setProperty("hideExplosionParticles", String.valueOf(hideExplosionParticles));
            prop.setProperty("hideRainParticles", String.valueOf(hideRainParticles));
            prop.setProperty("hideVignette", String.valueOf(hideVignette));
            prop.setProperty("hideFog", String.valueOf(hideFog));
            prop.setProperty("attackEffectsCritParticles", String.valueOf(attackEffectsCritParticles));
            prop.setProperty("attackEffectsSharpnessParticles", String.valueOf(attackEffectsSharpnessParticles));
            prop.setProperty("attackEffectsFlameParticles", String.valueOf(attackEffectsFlameParticles));
            prop.setProperty("attackEffectsBloodParticles", String.valueOf(attackEffectsBloodParticles));
            prop.setProperty("attackEffectsLightning", String.valueOf(attackEffectsLightning));
            prop.setProperty("noAttackCooldownAnimation", String.valueOf(noAttackCooldownAnimation));
            prop.setProperty("customCape", String.valueOf(customCape));
            prop.setProperty("chatHudEditQuickEnable", String.valueOf(chatHudEditQuickEnable));
            prop.setProperty("betterChat", String.valueOf(betterChat));
            prop.setProperty("betterChatMessageAnimation", String.valueOf(betterChatMessageAnimation));
            prop.setProperty("betterChatInputAnimation", String.valueOf(betterChatInputAnimation));
            prop.setProperty("betterChatAvatar", String.valueOf(betterChatAvatar));
            prop.setProperty("smoothHotbarScrolling", String.valueOf(smoothHotbarScrolling));
            prop.setProperty("smoothHotbarAnimationSpeed", String.valueOf(smoothHotbarAnimationSpeed));
            prop.setProperty("betterChatMessageFadeTime", String.valueOf(betterChatMessageFadeTime));
            prop.setProperty("betterChatInputFadeTime", String.valueOf(betterChatInputFadeTime));
            prop.setProperty("hotbarRollover", String.valueOf(hotbarRollover));
            prop.setProperty("useMainUI", String.valueOf(useMainUI));
            prop.setProperty("mainUICustomBackground", String.valueOf(mainUICustomBackground));
            prop.setProperty("mainUIMouseEffect", String.valueOf(mainUIMouseEffect));
            prop.setProperty("termsRead", String.valueOf(termsRead));
            prop.setProperty("fullMode", String.valueOf(fullMode));
            prop.setProperty("clientName", clientName);
            prop.setProperty("mainUIBackgroundImage", mainUIBackgroundImage);
            prop.setProperty("customCapeImage", customCapeImage);
            prop.setProperty("hitSoundType", hitSoundType.name());
            prop.setProperty("hitSoundCondition", hitSoundCondition.name());
            prop.setProperty("targetHudMode", targetHudMode.name());
            prop.setProperty("keystrokesMode", keystrokesMode.name());
            prop.setProperty("armorHudMode", armorHudMode.name());
            prop.setProperty("armorHudLayout", armorHudLayout.name());
            prop.setProperty("range", String.valueOf(range));
            prop.setProperty("animSpeed", String.valueOf(animSpeed));
            prop.setProperty("sneakDropScale", String.valueOf(sneakDropScale));
            prop.setProperty("sneakAnimationSpeed", String.valueOf(sneakAnimationSpeed));
            prop.setProperty("animationMode", animationMode.name());
            prop.setProperty("motionBlurAlgorithm", motionBlurAlgorithm.name());
            prop.setProperty("weatherMode", weatherMode.name());
            prop.setProperty("offsetX", String.valueOf(offsetX));
            prop.setProperty("offsetY", String.valueOf(offsetY));
            prop.setProperty("offsetZ", String.valueOf(offsetZ));
            prop.setProperty("targetHudX", String.valueOf(targetHudX));
            prop.setProperty("targetHudY", String.valueOf(targetHudY));
            prop.setProperty("targetHudZ", String.valueOf(targetHudZ));
            prop.setProperty("targetHudScale", String.valueOf(targetHudScale));
            prop.setProperty("keystrokesX", String.valueOf(keystrokesX));
            prop.setProperty("keystrokesY", String.valueOf(keystrokesY));
            prop.setProperty("keystrokesScale", String.valueOf(keystrokesScale));
            prop.setProperty("nameTag", String.valueOf(nameTag));
            prop.setProperty("nameTagScale", String.valueOf(nameTagScale));
            prop.setProperty("nameTagDynamicScale", String.valueOf(nameTagDynamicScale));
            prop.setProperty("nameTagOnlyPlayer", String.valueOf(nameTagOnlyPlayer));
            prop.setProperty("dynamicMotionBlur", String.valueOf(dynamicMotionBlur));
            prop.setProperty("dynamicMotionBlurStrength", String.valueOf(dynamicMotionBlurStrength));
            prop.setProperty("dynamicMotionBlurRefreshRateScaling", String.valueOf(dynamicMotionBlurRefreshRateScaling));
            prop.setProperty("attackEffectsCritMultiplier", String.valueOf(attackEffectsCritMultiplier));
            prop.setProperty("attackEffectsSharpnessMultiplier", String.valueOf(attackEffectsSharpnessMultiplier));
            prop.setProperty("attackEffectsFlameMultiplier", String.valueOf(attackEffectsFlameMultiplier));
            prop.setProperty("attackEffectsBloodMultiplier", String.valueOf(attackEffectsBloodMultiplier));
            prop.setProperty("attackEffectsLightningCount", String.valueOf(attackEffectsLightningCount));
            prop.setProperty("blockCountDisplayX", String.valueOf(blockCountDisplayX));
            prop.setProperty("blockCountDisplayY", String.valueOf(blockCountDisplayY));
            prop.setProperty("blockCountDisplayScale", String.valueOf(blockCountDisplayScale));
            prop.setProperty("armorHudX", String.valueOf(armorHudX));
            prop.setProperty("armorHudY", String.valueOf(armorHudY));
            prop.setProperty("armorHudScale", String.valueOf(armorHudScale));
            prop.setProperty("notificationX", String.valueOf(notificationX));
            prop.setProperty("notificationY", String.valueOf(notificationY));
            prop.setProperty("notificationScale", String.valueOf(notificationScale));
            prop.setProperty("potionStatusX", String.valueOf(potionStatusX));
            prop.setProperty("potionStatusY", String.valueOf(potionStatusY));
            prop.setProperty("potionStatusScale", String.valueOf(potionStatusScale));
            prop.setProperty("gammaValue", String.valueOf(gammaValue));
            prop.setProperty("fishingRodAssistUseDelay", String.valueOf(fishingRodAssistUseDelay));
            prop.setProperty("autoChestDepositDepositDelay", String.valueOf(autoChestDepositDepositDelay));
            prop.setProperty("autoChestDepositCloseDelay", String.valueOf(autoChestDepositCloseDelay));
            prop.setProperty("clientTime", String.valueOf(clientTime));
            prop.store(os, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static float clamp01(float value) {
        return clamp(value, 0f, 1f, 0f);
    }

    private static float clamp(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    private static float finiteOrDefault(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static <E extends Enum<E>> E parseEnum(String value, E fallback, Class<E> type) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            System.err.println("[DioxideLite] Invalid config enum '" + value + "' for " + type.getSimpleName() + ", using " + fallback);
            return fallback;
        }
    }

    private static VisualStyle parseVisualStyle(String value) {
        return parseEnum(value, VisualStyle.LIQUID_GLASS, VisualStyle.class);
    }

    private static boolean defaultChinese() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.getLanguageManager() != null) {
                String selected = client.getLanguageManager().getSelected();
                return selected != null && selected.toLowerCase().startsWith("zh_");
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static HudTheme parseHudTheme(String value) {
        if ("LIGHT".equals(value)) {
            return HudTheme.LIGHT;
        }
        return HudTheme.DARK;
    }

    public static void applyGameLanguageDefault() {
        isChinese = defaultChinese();
    }

    public static void applyFirstUseLanguageDefault() {
        if (!termsRead) {
            applyGameLanguageDefault();
        }
    }

    private static void ensureConfigDirectory() {
        try {
            Files.createDirectories(CONFIG_DIRECTORY);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create config directory: " + CONFIG_DIRECTORY, e);
        }
    }

    private static void migrateLegacyConfigIfNeeded() {
        if (Files.exists(CONFIG_FILE) || !Files.exists(LEGACY_CONFIG_FILE)) {
            return;
        }
        try {
            Files.copy(LEGACY_CONFIG_FILE, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to migrate legacy config to: " + CONFIG_FILE, e);
        }
    }
}
