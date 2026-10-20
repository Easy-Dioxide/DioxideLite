package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.movement.NoSlow2;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.ClientColors;
import com.dioxideliteng.util.MutableVector3d;
import com.dioxideliteng.util.WorldToScreenProjector;
import com.dioxideliteng.util.krs.BehaviorUtils;
import com.dioxideliteng.util.krs.BlinkUtil;
import com.dioxideliteng.util.krs.EntityExtension;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.PlayerUtil;
import com.dioxideliteng.util.krs.RandomUtil;
import com.dioxideliteng.util.krs.RotationManager;
import com.dioxideliteng.util.krs.ToolUtil;
import com.dioxideliteng.util.pathfinder.MainPathFinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.KillAura) and adapted
 * to the DioxideLiteNG client API. Notable adaptations vs the original:
 *  - Krs ModuleManager.getModuleState(NoSlow.class) replaced with the null-safe
 *    NoSlow2.inst() enabled check (NoSlow2 is the ported Krs NoSlow module; the
 *    legacy NoSlow speed module is a different feature and is intentionally not used).
 *  - Client.rotationManager replaced with the shared RotationManager.INSTANCE;
 *    rotations are consumed by the global rotation hook in MixinLocalPlayer, which
 *    writes them into Events.ROTATION right after the ROTATION dispatch (silent aim).
 *  - The Krs TextValue "Prefer Target ID" setting is not ported (no client equivalent).
 */
public class KillAura2 extends Feature {
    private static final int RANDOM_TP_ATTEMPTS = 32;
    private static final int[] RANDOM_TP_VERTICAL_OFFSETS = {0, 1, -1, 2, -2};
    private static final double RANDOM_TP_COLLISION_MARGIN = 0.05D;
    private static final double[] SMART_ROTATION_OFFSETS = {
            0.0D, 0.03125D, 0.0625D, 0.09375D,
            0.125D, 0.15625D, 0.1875D, 0.21875D,
            0.25D, 0.28125D, 0.3125D, 0.34375D,
            0.375D, 0.40625D, 0.4375D, 0.46875D,
            0.5D, 0.53125D, 0.5625D, 0.59375D,
            0.625D, 0.65625D, 0.6875D, 0.71875D,
            0.75D, 0.78125D, 0.8125D, 0.84375D,
            0.875D, 0.90625D, 0.9375D, 0.96875D,
            1.0D
    };

    private final ModeSetting targetMode = new ModeSetting("Target Mode", this, "Single",
            new String[]{"Single", "Multi"});
    private final NumberSetting maxTargets = new NumberSetting("Max Targets", this, 3.0, 2.0, 10.0, 1.0);
    private final NumberSetting targetRange = new NumberSetting("Target Range", this, 4.0, 0.0, 8.0, 0.1);
    // Krs parity: these three settings were public static in Krs KillAura and are read
    // cross-module (MaceExploit, Criticals2); kept public for identical access semantics.
    public final NumberSetting attackRange = new NumberSetting("Attack Range", this, 3.0, 0.0, 6.0, 0.1);
    private final ModeSetting apsMode = new ModeSetting("APS Mode", this, "Cooldown",
            new String[]{"Cooldown", "Randomized CPS", "Hurt Math", "No Delay"});
    private final BooleanSetting quickCooldown = new BooleanSetting("Quick Cooldown", this, true);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", this, 12.0, 1.0, 20.0, 1.0);
    private final NumberSetting minCps = new NumberSetting("Min CPS", this, 6.0, 1.0, 20.0, 1.0);
    private final ModeSetting swingOrderMode = new ModeSetting("Swing Order Mode", this, "1.9.x",
            new String[]{"1.8", "1.9.x", "No Order"});
    private final ModeSetting autoBlockMode = new ModeSetting("Auto Block Mode", this, "No Order",
            new String[]{"Vanilla", "Blink1", "Blink2", "Hypixel NCP", "Swap", "Legit", "Packet",
                    "Interact1", "Interact2", "Basic", "Spoof", "Delayed", "Post Attack", "No Order"});
    private final NumberSetting autoBlockCps = new NumberSetting("Auto Block CPS", this, 8.0, 1.0, 10.0, 0.5);
    private final NumberSetting autoBlockRange = new NumberSetting("Auto Block Range", this, 6.0, 3.0, 8.0, 0.1);
    private final BooleanSetting rotations = new BooleanSetting("Rotations", this, true);
    private final NumberSetting maxRotationSpeed = new NumberSetting("Max Rotation Speed", this, 60.0, 1.0, 180.0, 1.0);
    private final NumberSetting minRotationSpeed = new NumberSetting("Min Rotation Speed", this, 40.0, 1.0, 180.0, 1.0);
    private final BooleanSetting noHitCheck = new BooleanSetting("No Hit Check", this, false);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", this, true);
    private final BooleanSetting onlyPlayers = new BooleanSetting("Only Players", this, false);
    private final BooleanSetting visualSwing = new BooleanSetting("Visual Swing", this, false);
    public final BooleanSetting tpReach = new BooleanSetting("TP Reach", this, false);
    public final BooleanSetting randomTp = new BooleanSetting("Random TP", this, false);
    private final NumberSetting tpExtendedReach = new NumberSetting("TP Extended Reach", this, 40.0, 0.0, 200.0, 1.0);
    private final NumberSetting tpTicks = new NumberSetting("TP Ticks", this, 0.0, 0.0, 60.0, 1.0);
    private final BooleanSetting tpBack = new BooleanSetting("TP Back", this, true);
    private final BooleanSetting tpOnGroundPacket = new BooleanSetting("TP OnGround Packet", this, true);
    private final RotationManager rotationManager = RotationManager.INSTANCE;

    /** Legacy TargetESP visual (orbiting crystals + glow, or orbiting shard particles),
     *  ported from the client's historical TargetESP onto the 2D render pipeline. */
    public final TargetEsp targetEsp = new TargetEsp(this);

    public Entity closestEntity = null;
    public final List<Entity> multiTargets = new ArrayList<>();
    private final ArrayList<TargetCandidate> targetCandidateBuffer = new ArrayList<>();
    private final ArrayList<TargetCandidate> targetCandidatePool = new ArrayList<>();
    private int targetCandidatePoolCursor;
    private PendingTeleportSequence pendingTeleportSequence;
    private boolean sendingTeleportSequencePacket;

    private long lastAttackTime = 0;
    private long lastOpenBlockTime = 0;
    private int randomDelay = 0;
    private int abTick = 0;
    private int blockTick = 0;
    private boolean wasBlinking = false;
    private boolean wasPacketBlocking = false;
    private boolean wasBlocking = false;
    public boolean isBlocking = false;

    public KillAura2() {
        super("KillAura2", Category.COMBAT);
        this.maxTargets.setVisible(() -> this.targetMode.is("Multi"));
        this.quickCooldown.setVisible(() -> this.apsMode.is("Cooldown"));
        this.maxCps.setVisible(() -> this.apsMode.is("Randomized CPS"));
        this.minCps.setVisible(() -> this.apsMode.is("Randomized CPS"));
        this.autoBlockCps.setVisible(this::isAutoBlockModeActive);
        this.autoBlockRange.setVisible(this::isAutoBlockModeActive);
        this.maxRotationSpeed.setVisible(this.rotations::getValue);
        this.minRotationSpeed.setVisible(this.rotations::getValue);
        this.noHitCheck.setVisible(this.rotations::getValue);
        this.tpExtendedReach.setVisible(this.tpReach::getValue);
        this.tpTicks.setVisible(() -> this.tpReach.getValue() || this.randomTp.getValue());
        this.tpBack.setVisible(() -> this.tpReach.getValue() || this.randomTp.getValue());
        this.tpOnGroundPacket.setVisible(() -> this.tpReach.getValue() || this.randomTp.getValue());
    }

    private boolean isAutoBlockModeActive() {
        return isAutoBlockMode(this.autoBlockMode.getValue());
    }

    private static final class TargetCandidate {
        private Entity entity;
        private double squaredDistance;
        private boolean preferred;

        private TargetCandidate set(Entity entity, double squaredDistance, boolean preferred) {
            this.entity = entity;
            this.squaredDistance = squaredDistance;
            this.preferred = preferred;
            return this;
        }

        private Entity entity() { return entity; }
        private double squaredDistance() { return squaredDistance; }
        private boolean preferred() { return preferred; }
    }

    private record SmartRotationTarget(float yaw, float pitch) {
    }

    private interface TeleportStep {
    }

    private record TeleportMoveStep(Vec3 position) implements TeleportStep {
    }

    private record TeleportGoStep(Entity target, boolean preferRandom, boolean requiresPath,
                                 Vec3 plannedFrom, ArrayList<Vec3> plannedPath) implements TeleportStep {
    }

    private record TeleportReturnStep(int estimatedPackets) implements TeleportStep {
    }

    private record TeleportAttackStep(Entity target, boolean moveClientToArrival) implements TeleportStep {
    }

    private record ResolvedTeleportPath(Vec3 arrivalPosition, ArrayList<Vec3> positions) {
    }

    private static final class PendingTeleportSequence {
        private final ArrayList<TeleportStep> steps;
        private final ArrayList<Vec3> sentPositions = new ArrayList<>();
        private int totalMovePackets;
        private final int totalTicks;
        private final String swingMode;
        private final String autoBlockMode;
        private final float yaw;
        private final float pitch;
        private final Entity blockTarget;
        private final boolean canAutoBlock;
        private final boolean onGround;
        private final boolean horizontalCollision;
        private Vec3 lastSentPosition;
        private int nextStep;
        private int sentMovePackets;
        private int elapsedTicks;

        private PendingTeleportSequence(ArrayList<TeleportStep> steps, int totalMovePackets, int totalTicks,
                                        String swingMode, String autoBlockMode, float yaw, float pitch,
                                        Entity blockTarget, boolean canAutoBlock, boolean onGround,
                                        boolean horizontalCollision, Vec3 lastSentPosition) {
            this.steps = steps;
            this.totalMovePackets = totalMovePackets;
            this.totalTicks = totalTicks;
            this.swingMode = swingMode;
            this.autoBlockMode = autoBlockMode;
            this.yaw = yaw;
            this.pitch = pitch;
            this.blockTarget = blockTarget;
            this.canAutoBlock = canAutoBlock;
            this.onGround = onGround;
            this.horizontalCollision = horizontalCollision;
            this.lastSentPosition = lastSentPosition;
        }
    }

    private float getRealTargetReach() {
        float reach = (float) targetRange.getValue();
        if (isAutoBlockMode(autoBlockMode.getValue()))
            reach = Math.max(reach, (float) autoBlockRange.getValue());
        if (mc.player.isCreative() && reach <= 4.5f)
            reach += 1.5f;
        if (tpReach.getValue())
            reach += (float) tpExtendedReach.getValue();
        return reach;
    }

    private float getRealAttackReach() {
        float reach = (float) attackRange.getValue();
        if (mc.player.isCreative() && reach <= 4.5f)
            reach += 1.5f;
        if (tpReach.getValue())
            reach += (float) tpExtendedReach.getValue();
        return reach;
    }

    private float getRotationSpeed() {
        float min = (float) minRotationSpeed.getValue();
        float max = (float) maxRotationSpeed.getValue();
        return RandomUtil.nextFloat(Math.min(min, max), Math.max(min, max));
    }

    @Override
    public void onEnable() {
        targetEsp.reset();
        DynamicIslandManager.notifyInfo("KillAura2", "已开启 KillAura2");
    }

    @Override
    public void onDisable() {
        targetEsp.reset();
        cancelPendingTeleportSequence();
        if (mc.player != null)
            resetPacketUnblocking();
        wasPacketBlocking = false;
        reset();
        DynamicIslandManager.notifyInfo("KillAura2", "已关闭 KillAura2");
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.HANDLE_INPUT) {
            onHandleInput();
        } else if (event instanceof EventPacketSend sending) {
            onSendPacket(sending);
        } else if (event == Events.TICK) {
            // Keep suffix in sync with the APS mode.
            if (apsMode.is("Randomized CPS"))
                setSuffix((int) minCps.getValue() + "-" + (int) maxCps.getValue());
            else
                setSuffix(apsMode.getValue());
        } else if (event == Events.RENDER_2D) {
            targetEsp.render(Events.RENDER_2D.getGraphics(), Events.RENDER_2D.getPartialTick());
        }
    }

    private void onSendPacket(EventPacketSend event) {
        if (pendingTeleportSequence != null
                && !sendingTeleportSequencePacket
                && event.getPacket() instanceof ServerboundMovePlayerPacket)
            event.setCancelled(true);
    }

    private void onHandleInput() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            pendingTeleportSequence = null;
            sendingTeleportSequencePacket = false;
            return;
        }

        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            reset();
            return;
        }

        if (BehaviorUtils.noKillAura || mc.player.isSpectator()) {
            reset();
            return;
        }

        if (pendingTeleportSequence != null && !tpReach.getValue() && !randomTp.getValue()) {
            reset();
            return;
        }

        boolean spreadingTeleport = pendingTeleportSequence != null;

        float realTargetReach = getRealTargetReach();
        float realAttackReach = getRealAttackReach();
        double realTargetReachSquared = square(realTargetReach);
        double realAttackReachSquared = square(realAttackReach);
        double baseAttackReachSquared = square(attackRange.getValue());
        boolean multiMode = targetMode.is("Multi");
        boolean canAttackThroughWalls = throughWalls.getValue();
        int multiTargetLimit = multiMode ? (int) maxTargets.getValue() : 0;
        ArrayList<TargetCandidate> candidates = multiMode ? targetCandidateBuffer : null;
        if (candidates != null)
            candidates.clear();
        targetCandidatePoolCursor = 0;
        TargetCandidate closest = null;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity)
                    || livingEntity.isDeadOrDying()
                    || entity instanceof ArmorStand
                    || entity instanceof LocalPlayer
                    || BehaviorUtils.isBot(livingEntity)
                    || BehaviorUtils.isTeammate(livingEntity)
                    || onlyPlayers.getValue() && !(entity instanceof Player)) continue;

            double squaredDistance = EntityExtension.squaredBoxedDistanceTo(entity, mc.player);
            if (squaredDistance > realTargetReachSquared) continue;

            TargetCandidate candidate = acquireTargetCandidate(entity, squaredDistance, false);

            if (isBetterTarget(candidate, closest))
                closest = candidate;
            if (multiMode)
                insertBoundedTarget(candidates, candidate, multiTargetLimit);
        }

        if (closest == null) {
            if (spreadingTeleport) {
                advancePendingTeleportSequence();
                return;
            }

            if (autoBlockMode.is("Vanilla"))
                resetPacketUnblocking();

            reset();
            return;
        }

        closestEntity = closest.entity();

        if (multiMode) {
            multiTargets.clear();
            int targetLimit = candidates.size();
            for (int i = 0; i < targetLimit; i++) {
                TargetCandidate candidate = candidates.get(i);
                Entity target = candidate.entity();
                if (candidate.squaredDistance() <= realAttackReachSquared && (canAttackThroughWalls || mc.player.hasLineOfSight(target)))
                    multiTargets.add(target);
            }
        } else {
            multiTargets.clear();
        }

        boolean canHitTargetAtRotation = false;
        if (rotations.getValue())
            canHitTargetAtRotation = faceSmartRotation(closestEntity, getRotationSpeed(), realTargetReach, canAttackThroughWalls);

        if (spreadingTeleport) {
            advancePendingTeleportSequence();
            return;
        }

        String currentAutoBlockMode = autoBlockMode.getValue().toLowerCase(Locale.ROOT);
        boolean autoBlock = isAutoBlockMode(currentAutoBlockMode);
        float yaw = rotations.getValue() ? rotationManager.getRotationYaw() : mc.player.getYRot();
        float pitch = rotations.getValue() ? rotationManager.getRotationPitch() : mc.player.getXRot();
        boolean hasTargetVisibility = mc.player.hasLineOfSight(closestEntity)
                || canHitTargetAtRotation
                || rotationManager.canHitEntityAtRotation(closestEntity, yaw, pitch, realTargetReach, false);
        boolean canAutoBlock = canAutoBlock(currentAutoBlockMode, closest.squaredDistance(), canAttackThroughWalls || hasTargetVisibility);

        if (!autoBlock && wasBlocking)
            resetAutoBlock();

        if (autoBlock) {
            isBlocking = canAutoBlock || wasBlocking;

            if (!canAutoBlock) {
                resetAutoBlock();
            } else if (wasBlocking) {
                if (shouldFinishAutoBlock(currentAutoBlockMode)) {
                    finishAutoBlock(currentAutoBlockMode, yaw, pitch);
                } else {
                    blockTick++;
                }
                return;
            }
        } else {
            isBlocking = true;
        }

        boolean canHitVisibleAtRotation = rotationManager.canHitEntityAtRotation(
                closestEntity,
                yaw,
                pitch,
                realAttackReach,
                false
        );
        boolean canAttackTarget = closest.squaredDistance() <= realAttackReachSquared
                && (canAttackThroughWalls || hasTargetVisibility || canHitVisibleAtRotation);

        if (!canAttackTarget) {
            switch (currentAutoBlockMode) {
                case "vanilla":
                    resetPacketUnblocking();
                    break;

                case "blink1":
                    if (wasBlinking) {
                        BlinkUtil.INSTANCE.sync(true, true);
                        BlinkUtil.INSTANCE.stopBlink();
                        wasBlinking = false;
                    }
                    break;

                case "hypixel ncp":
                case "blink2":
                case "swap":
                case "legit":
                case "packet":
                case "interact1":
                case "basic":
                case "interact2":
                case "spoof":
                case "delayed":
                    if (canAutoBlock)
                        startAutoBlock(currentAutoBlockMode, yaw, pitch, closestEntity, false);
                    break;
            }

            return;
        }

        boolean attackReady = attackCooldownMath(closestEntity);
        boolean canHitAtRotation = !rotations.getValue() || noHitCheck.getValue() || tpReach.getValue() || randomTp.getValue();
        if (!canHitAtRotation) {
            canHitAtRotation = canAttackThroughWalls
                    ? rotationManager.canHitEntityAtRotation(closestEntity, yaw, pitch, realAttackReach, true)
                    : canHitVisibleAtRotation;
        }

        if (autoBlock && canAutoBlock && (!attackReady || !canHitAtRotation)) {
            startAutoBlock(currentAutoBlockMode, yaw, pitch, closestEntity, false);
            return;
        }

        if (canHitAtRotation) {
            if (autoBlock && canAutoBlock && !canStartAutoBlockCycle())
                return;

            if (attackReady) {
                abTick++;

                switch (currentAutoBlockMode) {
                    case "vanilla":
                        if (mc.player.getOffhandItem().getItem() instanceof ShieldItem)
                            PacketUtil.sendPacket(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, yaw, pitch));
                        else if (ToolUtil.INSTANCE.isSword(mc.player.getMainHandItem()) && !wasPacketBlocking)
                            PacketUtil.sendPacket(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, yaw, pitch));
                        break;

                    case "blink1":
                        if (ToolUtil.INSTANCE.isSword(mc.player.getMainHandItem()) && abTick >= 1) {
                            BlinkUtil.INSTANCE.doBlink();
                            PacketUtil.sendPacket(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, yaw, pitch));
                            wasBlinking = true;
                        }
                        break;
                }

                ArrayList<Entity> attackTargets = new ArrayList<>(multiMode ? multiTargets.size() : 1);
                if (multiMode)
                    attackTargets.addAll(multiTargets);
                else
                    attackTargets.add(closestEntity);

                if (startSpreadTeleportSequence(
                        attackTargets,
                        baseAttackReachSquared,
                        currentAutoBlockMode,
                        yaw,
                        pitch,
                        closestEntity,
                        canAutoBlock
                )) return;

                for (Entity target : attackTargets) {
                    double squaredDistance = EntityExtension.squaredBoxedDistanceTo(target, mc.player);
                    boolean needsExtendedTeleport = squaredDistance >= baseAttackReachSquared;
                    attackToEntity(
                            target,
                            randomTp.getValue() || (tpReach.getValue() && needsExtendedTeleport),
                            needsExtendedTeleport
                    );
                }

                DynamicIslandManager.notifySuccess("KillAura2", "攻击目标: " + closestEntity.getName().getString());
                finishAttackCycle(currentAutoBlockMode, yaw, pitch, closestEntity, canAutoBlock);
            } else if (visualSwing.getValue()) {
                PlayerUtil.INSTANCE.swingHandWithoutPacket(InteractionHand.MAIN_HAND);
                renderParticles(closestEntity);
            }
        }
    }

    private void finishAttackCycle(String autoBlockMode, float yaw, float pitch, Entity blockTarget, boolean canAutoBlock) {
        switch (autoBlockMode) {
            case "vanilla":
                if (mc.player.getOffhandItem().getItem() instanceof ShieldItem)
                    PacketUtil.sendPacket(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, yaw, pitch));
                else if (ToolUtil.INSTANCE.isSword(mc.player.getMainHandItem()) && wasPacketBlocking)
                    PacketUtil.sendPacket(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, yaw, pitch));
                wasPacketBlocking = true;
                break;

            case "blink1":
                if (ToolUtil.INSTANCE.isSword(mc.player.getMainHandItem()) && abTick >= 1) {
                    BlinkUtil.INSTANCE.sync(true, true);
                    PacketUtil.sendPacket(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, yaw, pitch));
                    BlinkUtil.INSTANCE.stopBlink();
                    abTick = 0;
                }
                break;

            case "hypixel ncp":
            case "blink2":
            case "swap":
            case "legit":
            case "packet":
            case "interact1":
            case "basic":
            case "interact2":
            case "spoof":
            case "delayed":
            case "post attack":
                if (canAutoBlock)
                    startAutoBlock(autoBlockMode, yaw, pitch, blockTarget, true);
                break;
        }
    }

    private static int compareTargets(TargetCandidate first, TargetCandidate second) {
        if (first.preferred() != second.preferred())
            return first.preferred() ? -1 : 1;
        return Double.compare(first.squaredDistance(), second.squaredDistance());
    }

    public static double square(double value) {
        return value * value;
    }

    private static boolean isBetterTarget(TargetCandidate candidate, TargetCandidate currentBest) {
        return currentBest == null || compareTargets(candidate, currentBest) < 0;
    }

    private TargetCandidate acquireTargetCandidate(Entity entity, double squaredDistance, boolean preferred) {
        while (targetCandidatePool.size() <= targetCandidatePoolCursor)
            targetCandidatePool.add(new TargetCandidate());
        return targetCandidatePool.get(targetCandidatePoolCursor++).set(entity, squaredDistance, preferred);
    }

    private static void insertBoundedTarget(ArrayList<TargetCandidate> candidates, TargetCandidate candidate, int limit) {
        if (limit <= 0)
            return;

        int insertIndex = 0;
        int size = candidates.size();
        while (insertIndex < size && compareTargets(candidates.get(insertIndex), candidate) <= 0) {
            insertIndex++;
        }

        if (insertIndex >= limit)
            return;

        candidates.add(insertIndex, candidate);
        if (candidates.size() > limit)
            candidates.remove(candidates.size() - 1);
    }

    private boolean faceSmartRotation(Entity target, float rotationSpeed, double maxRange, boolean ignoreBlocks) {
        SmartRotationTarget rotationTarget = findSmartRotationTarget(target, maxRange, ignoreBlocks);
        if (rotationTarget == null) {
            rotationManager.faceEntity(target, rotationSpeed, false, false, maxRange);
            return rotationManager.canHitEntityAtRotation(
                    target,
                    rotationManager.getRotationYaw(),
                    rotationManager.getRotationPitch(),
                    maxRange,
                    ignoreBlocks
            );
        }

        rotationManager.startRotation(rotationTarget.yaw(), rotationTarget.pitch(), rotationSpeed);
        return rotationManager.canHitEntityAtRotation(
                target,
                rotationManager.getRotationYaw(),
                rotationManager.getRotationPitch(),
                maxRange,
                ignoreBlocks
        );
    }

    private SmartRotationTarget findSmartRotationTarget(Entity target, double maxRange, boolean ignoreBlocks) {
        var player = mc.player;
        if (player == null || target == null)
            return null;

        float currentYaw = rotationManager.getRotationYaw();
        float currentPitch = rotationManager.getRotationPitch();
        Vec3 eyesPos = player.getEyePosition();
        double eyeX = eyesPos.x;
        double eyeY = eyesPos.y;
        double eyeZ = eyesPos.z;
        AABB box = target.getBoundingBox().inflate(0.1D, 0.1D, 0.1D);
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerY = (box.minY + box.maxY) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        float centerYaw = rotationYawTo(centerX, centerZ, eyeX, eyeZ);
        float centerPitch = rotationPitchTo(centerX, centerY, centerZ, eyeX, eyeY, eyeZ);
        boolean currentCanHit = rotationManager.canHitEntityAtRotation(target, currentYaw, currentPitch, maxRange, ignoreBlocks);
        if (currentCanHit && rotationScore(centerYaw, centerPitch, currentYaw, currentPitch) <= 6.0D)
            return new SmartRotationTarget(currentYaw, currentPitch);

        if (rotationManager.canHitEntityAtRotation(target, centerYaw, centerPitch, maxRange, ignoreBlocks))
            return new SmartRotationTarget(centerYaw, centerPitch);

        float bestYaw = 0.0f;
        float bestPitch = 0.0f;
        double bestScore = Double.MAX_VALUE;
        boolean foundTarget = false;

        for (int fixedAxis = 0; fixedAxis < 3; fixedAxis++) {
            for (int fixedSide = 0; fixedSide < 2; fixedSide++) {
                double x = fixedAxis == 0 ? (fixedSide == 0 ? box.minX : box.maxX) : centerX;
                double y = fixedAxis == 1 ? (fixedSide == 0 ? box.minY : box.maxY) : centerY;
                double z = fixedAxis == 2 ? (fixedSide == 0 ? box.minZ : box.maxZ) : centerZ;
                float yaw = rotationYawTo(x, z, eyeX, eyeZ);
                float pitch = rotationPitchTo(x, y, z, eyeX, eyeY, eyeZ);
                double score = smartRotationScore(
                        x, y, z,
                        yaw, pitch,
                        currentYaw, currentPitch,
                        eyeX, eyeY, eyeZ,
                        box, centerX, centerY, centerZ,
                        currentCanHit
                );

                if (score < bestScore && rotationManager.canHitEntityAtRotation(target, yaw, pitch, maxRange, ignoreBlocks)) {
                    bestScore = score;
                    bestYaw = yaw;
                    bestPitch = pitch;
                    foundTarget = true;
                }
            }
        }

        for (int fixedAxis = 0; fixedAxis < 3; fixedAxis++) {
            for (int fixedSide = 0; fixedSide < 2; fixedSide++) {
                double fixedOffset = fixedSide;
                for (double offsetA : SMART_ROTATION_OFFSETS) {
                    for (double offsetB : SMART_ROTATION_OFFSETS) {
                        double x = fixedAxis == 0 ? lerp(box.minX, box.maxX, fixedOffset) : lerp(box.minX, box.maxX, offsetA);
                        double y = fixedAxis == 1 ? lerp(box.minY, box.maxY, fixedOffset) : lerp(box.minY, box.maxY, fixedAxis == 0 ? offsetA : offsetB);
                        double z = fixedAxis == 2 ? lerp(box.minZ, box.maxZ, fixedOffset) : lerp(box.minZ, box.maxZ, offsetB);
                        float yaw = rotationYawTo(x, z, eyeX, eyeZ);
                        float pitch = rotationPitchTo(x, y, z, eyeX, eyeY, eyeZ);
                        double score = smartRotationScore(
                                x, y, z,
                                yaw, pitch,
                                currentYaw, currentPitch,
                                eyeX, eyeY, eyeZ,
                                box, centerX, centerY, centerZ,
                                currentCanHit
                        );

                        if (score >= bestScore
                                || !rotationManager.canHitEntityAtRotation(target, yaw, pitch, maxRange, ignoreBlocks))
                            continue;

                        bestScore = score;
                        bestYaw = yaw;
                        bestPitch = pitch;
                        foundTarget = true;
                    }
                }
            }
        }

        return foundTarget ? new SmartRotationTarget(bestYaw, bestPitch) : null;
    }

    private static double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    private static double rotationScore(float yaw, float pitch, float currentYaw, float currentPitch) {
        return Math.abs(Mth.wrapDegrees(yaw - currentYaw)) + Math.abs(pitch - currentPitch);
    }

    private static double normalizedBoxCenterDistanceSqr(double hitX, double hitY, double hitZ,
                                                         AABB box, double centerX, double centerY, double centerZ) {
        double halfX = Math.max(1.0E-3D, (box.maxX - box.minX) * 0.5D);
        double halfY = Math.max(1.0E-3D, (box.maxY - box.minY) * 0.5D);
        double halfZ = Math.max(1.0E-3D, (box.maxZ - box.minZ) * 0.5D);
        double x = (hitX - centerX) / halfX;
        double y = (hitY - centerY) / halfY;
        double z = (hitZ - centerZ) / halfZ;
        return x * x + y * y + z * z;
    }

    private static double smartRotationScore(double hitX, double hitY, double hitZ,
                                             float yaw, float pitch, float currentYaw, float currentPitch,
                                             double eyeX, double eyeY, double eyeZ,
                                             AABB box, double centerX, double centerY, double centerZ,
                                             boolean currentCanHit) {
        double dx = hitX - eyeX;
        double dy = hitY - eyeY;
        double dz = hitZ - eyeZ;
        double distanceSqr = dx * dx + dy * dy + dz * dz;
        double centerScore = normalizedBoxCenterDistanceSqr(hitX, hitY, hitZ, box, centerX, centerY, centerZ);
        double score = rotationScore(yaw, pitch, currentYaw, currentPitch)
                + distanceSqr * 0.001D
                + centerScore * 0.02D;
        return currentCanHit ? score + centerScore * 1.5D : score;
    }

    private static float rotationYawTo(double hitX, double hitZ, double eyeX, double eyeZ) {
        return Mth.wrapDegrees((float) (Mth.atan2(-(hitX - eyeX), hitZ - eyeZ) * (180.0D / Math.PI)));
    }

    private static float rotationPitchTo(double hitX, double hitY, double hitZ,
                                         double eyeX, double eyeY, double eyeZ) {
        double diffX = hitX - eyeX;
        double diffY = hitY - eyeY;
        double diffZ = hitZ - eyeZ;
        double xzDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float pitch = (float) (-(Mth.atan2(diffY, xzDist) * (180.0D / Math.PI)));
        return Mth.clamp(pitch, -90.0f, 90.0f);
    }

    private void renderParticles(Entity target) {
        mc.player.magicCrit(target);
        if (mc.player.getDeltaMovement().y < -0.08)
            mc.player.crit(target);
    }

    private boolean attackCooldownMath(Entity target) {
        switch (apsMode.getValue().toLowerCase(Locale.ROOT)) {
            case "cooldown":
                return mc.player.getAttackStrengthScale(0f) >= (quickCooldown.getValue() ? 0.8f : 1f);

            case "randomized cps":
                long currentTime = System.currentTimeMillis();
                if (lastAttackTime == 0 || currentTime - lastAttackTime >= randomDelay) {
                    lastAttackTime = currentTime;
                    randomDelay = nextRandomizedCpsDelay();
                    return true;
                }
                break;

            case "no delay":
                return true;

            case "hurt math":
                return ((LivingEntity) target).hurtTime <= 5;
        }
        return false;
    }

    private int nextRandomizedCpsDelay() {
        int min = (int) minCps.getValue();
        int max = (int) maxCps.getValue();
        int lower = Math.min(min, max);
        int upper = Math.max(min, max);
        int cps = RandomUtil.nextInt(lower, upper + 1);
        return 1000 / cps;
    }

    private static boolean isAutoBlockMode(String mode) {
        if (mode == null) return false;
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "hypixel ncp", "blink2", "swap", "legit", "packet", "interact1", "interact2", "basic", "spoof", "delayed", "post attack" -> true;
            default -> false;
        };
    }

    public boolean shouldCancelUseItemOnWhileBlinking() {
        String mode = autoBlockMode.getValue();
        return mode.equalsIgnoreCase("vanilla")
                || mode.equalsIgnoreCase("blink1")
                || usesBlink(mode);
    }

    public boolean isSendingSpreadTeleportPacket() {
        return pendingTeleportSequence != null && sendingTeleportSequencePacket;
    }

    public boolean shouldSuppressMovementDuringSpreadTeleport() {
        return pendingTeleportSequence != null && !sendingTeleportSequencePacket;
    }

    private boolean canAutoBlock(String mode, double squaredDistance, boolean targetVisible) {
        return mc.player != null
                && targetVisible
                && squaredDistance <= square(autoBlockRange.getValue())
                && (usesItemInteractionMode(mode)
                ? findBlockableHand() != null
                : ToolUtil.INSTANCE.isSword(mc.player.getMainHandItem()));
    }

    private boolean canStartAutoBlockCycle() {
        long now = System.currentTimeMillis();
        long interval = Math.max(50L, (long) (1000.0f / autoBlockCps.getValue()));
        return lastOpenBlockTime == 0L || now - lastOpenBlockTime >= interval;
    }

    private boolean startAutoBlock(String mode, float yaw, float pitch, Entity target, boolean attacked) {
        if (mc.player == null || wasBlocking || !canStartAutoBlockCycle())
            return false;

        InteractionHand blockHand = InteractionHand.MAIN_HAND;
        if (usesItemInteractionMode(mode)) {
            if (mc.player.isUsingItem())
                return false;
            blockHand = findBlockableHand();
            if (blockHand == null)
                return false;
        } else if (!ToolUtil.INSTANCE.isSword(mc.player.getMainHandItem())) {
            return false;
        }

        if ("swap".equals(mode) && findSwordSlot(mc.player.getInventory().getSelectedSlot()) == -1)
            return false;

        if ("post attack".equals(mode) && !attacked)
            return false;

        switch (mode) {
            case "packet":
                sendUseItemPacketAutoBlock(yaw, pitch);
                break;

            case "interact1":
                sendInteractAutoBlock(target, yaw, pitch);
                break;

            case "basic":
                useItemStrictAutoBlock(blockHand);
                break;

            case "interact2":
                interactWithFacingAutoBlock(yaw, pitch, blockHand);
                break;

            default:
                sendUseItemAutoBlock(yaw, pitch);
                break;
        }

        if ("spoof".equals(mode))
            spoofAutoBlockSlot();

        wasPacketBlocking = true;
        wasBlocking = true;
        blockTick = 1;
        lastOpenBlockTime = System.currentTimeMillis();

        if (usesBlink(mode)) {
            BlinkUtil.INSTANCE.doBlink();
            wasBlinking = true;
        }
        return true;
    }

    private boolean shouldFinishAutoBlock(String mode) {
        return switch (mode) {
            case "delayed" -> blockTick >= 2;
            default -> blockTick >= 1;
        };
    }

    private void finishAutoBlock(String mode, float yaw, float pitch) {
        if (!wasBlocking) {
            blockTick = 0;
            return;
        }

        switch (mode) {
            case "hypixel ncp":
                NoSlow2 noSlow = NoSlow2.inst();
                if (noSlow != null && noSlow.isEnabled())
                    spoofHypixelNoSlowSlot();
                releaseAutoBlock();
                break;

            case "blink2":
            case "legit":
            case "packet":
            case "interact1":
            case "basic":
            case "interact2":
            case "spoof":
            case "delayed":
            case "post attack":
                releaseAutoBlock();
                break;

            case "swap":
                swapToAlternateSwordAndBlock(yaw, pitch);
                clearAutoBlockState();
                break;

            default:
                resetAutoBlock();
        }
    }

    private static boolean usesBlink(String mode) {
        if (mode == null) return false;
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "hypixel ncp", "blink2" -> true;
            default -> false;
        };
    }

    private static boolean usesItemInteractionMode(String mode) {
        if (mode == null) return false;
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "basic", "interact2" -> true;
            default -> false;
        };
    }

    private void sendUseItemAutoBlock(float yaw, float pitch) {
        syncSelectedSlot();
        sendSwordBlockPackets(yaw, pitch);
    }

    private void sendUseItemPacketAutoBlock(float yaw, float pitch) {
        syncSelectedSlot();
        PacketUtil.sendPacket(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, yaw, pitch));
    }

    private void sendInteractAutoBlock(Entity target, float yaw, float pitch) {
        syncSelectedSlot();
        if (target == null) {
            sendSwordBlockPackets(yaw, pitch);
            return;
        }

        PacketUtil.sendPacket(new ServerboundInteractPacket(
                target.getId(),
                InteractionHand.MAIN_HAND,
                interactionLocation(target, yaw, pitch),
                false
        ));
        sendSwordBlockPackets(yaw, pitch);
    }

    private void sendSwordBlockPackets(float yaw, float pitch) {
        PacketUtil.sendPacket(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                BlockHitResult.miss(new Vec3(-1.0D, -1.0D, -1.0D), Direction.DOWN, new BlockPos(-1, -1, -1)),
                0
        ));
        PacketUtil.sendPacket(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, yaw, pitch));
    }

    private boolean interactWithFacingAutoBlock(float yaw, float pitch, InteractionHand blockHand) {
        double reach = autoBlockRange.getValue();
        EntityHitResult entityHit = rayTraceEntity(yaw, pitch, reach);
        if (entityHit != null && interactEntityLikeUseItem(entityHit, blockHand))
            return true;

        if (entityHit != null)
            return useItemStrictAutoBlock(blockHand);

        BlockHitResult blockHit = rotationManager.rayTraceBlocks(yaw, pitch, reach);
        if (blockHit.getType() != HitResult.Type.BLOCK)
            return useItemStrictAutoBlock(blockHand);

        return interactBlockLikeUseItem(blockHit, blockHand) || useItemStrictAutoBlock(blockHand);
    }

    private boolean interactEntityLikeUseItem(EntityHitResult hitResult, InteractionHand blockHand) {
        if (mc.player == null || mc.gameMode == null)
            return false;

        Entity entity = hitResult.getEntity();
        Boolean mainHandResult = interactEntityLikeUseItem(entity, hitResult, InteractionHand.MAIN_HAND, blockHand);
        if (mainHandResult != null) return mainHandResult;
        Boolean offHandResult = interactEntityLikeUseItem(entity, hitResult, InteractionHand.OFF_HAND, blockHand);
        return offHandResult != null && offHandResult;
    }

    private Boolean interactEntityLikeUseItem(Entity entity, EntityHitResult hitResult, InteractionHand hand, InteractionHand blockHand) {
        InteractionResult interactResult = mc.gameMode.interact(mc.player, entity, hitResult, hand);
        if (interactResult.consumesAction())
            return false;
        return useItemAutoBlock(hand) ? hand == blockHand : null;
    }

    private boolean interactBlockLikeUseItem(BlockHitResult hitResult, InteractionHand blockHand) {
        if (mc.player == null || mc.gameMode == null)
            return false;

        Boolean mainHandResult = interactBlockLikeUseItem(hitResult, InteractionHand.MAIN_HAND, blockHand);
        if (mainHandResult != null) return mainHandResult;
        Boolean offHandResult = interactBlockLikeUseItem(hitResult, InteractionHand.OFF_HAND, blockHand);
        return offHandResult != null && offHandResult;
    }

    private Boolean interactBlockLikeUseItem(BlockHitResult hitResult, InteractionHand hand, InteractionHand blockHand) {
        InteractionResult interactResult = mc.gameMode.useItemOn(mc.player, hand, hitResult);
        if (interactResult.consumesAction() || interactResult == InteractionResult.FAIL)
            return false;
        return useItemAutoBlock(hand) ? hand == blockHand : null;
    }

    private boolean useItemStrictAutoBlock(InteractionHand blockHand) {
        if (useItemAutoBlock(InteractionHand.MAIN_HAND))
            return blockHand == InteractionHand.MAIN_HAND;
        return useItemAutoBlock(InteractionHand.OFF_HAND) && blockHand == InteractionHand.OFF_HAND;
    }

    private boolean useItemAutoBlock(InteractionHand hand) {
        if (mc.player == null || mc.gameMode == null)
            return false;
        return mc.gameMode.useItem(mc.player, hand).consumesAction();
    }

    private InteractionHand findBlockableHand() {
        if (mc.player == null)
            return null;
        if (isBlockableStack(itemInHand(InteractionHand.MAIN_HAND)))
            return InteractionHand.MAIN_HAND;
        return isBlockableStack(itemInHand(InteractionHand.OFF_HAND)) ? InteractionHand.OFF_HAND : null;
    }

    private ItemStack itemInHand(InteractionHand hand) {
        if (mc.player == null)
            return ItemStack.EMPTY;
        return hand == InteractionHand.MAIN_HAND ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
    }

    private boolean isBlockableStack(ItemStack stack) {
        return mc.player != null
                && mc.level != null
                && !stack.isEmpty()
                && (stack.getUseAnimation() == ItemUseAnimation.BLOCK
                || ToolUtil.INSTANCE.isSword(stack)
                || stack.getItem() instanceof ShieldItem)
                && stack.isItemEnabled(mc.level.enabledFeatures())
                && !mc.player.getCooldowns().isOnCooldown(stack);
    }

    private EntityHitResult rayTraceEntity(float yaw, float pitch, double reach) {
        if (mc.player == null || mc.level == null || reach <= 0.0D || !Double.isFinite(reach))
            return null;

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = getLookVecFromRotations(yaw, pitch);
        Vec3 endPos = eyePos.add(lookVec.scale(reach));
        AABB searchBox = mc.player.getBoundingBox().expandTowards(lookVec.scale(reach)).inflate(1.0D);
        Entity closestEntity = null;
        Vec3 closestHit = null;
        double closestDistance = reach * reach;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isPickable() || entity.isSpectator())
                continue;

            AABB box = entity.getBoundingBox().inflate(Math.max(0.0D, entity.getPickRadius()));
            if (!searchBox.intersects(box))
                continue;

            Vec3 hit = box.contains(eyePos)
                    ? eyePos
                    : box.clip(eyePos, endPos).orElse(null);
            if (hit == null)
                continue;

            double distance = eyePos.distanceToSqr(hit);
            if (distance <= closestDistance) {
                closestEntity = entity;
                closestHit = hit;
                closestDistance = distance;
            }
        }

        return closestEntity != null ? new EntityHitResult(closestEntity, closestHit) : null;
    }

    private void syncSelectedSlot() {
        if (mc.player == null) return;
        PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
    }

    private Vec3 interactionLocation(Entity target, float yaw, float pitch) {
        Vec3 fallback = target.getBoundingBox().getCenter().subtract(target.position());
        if (mc.player == null) return fallback;

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = getLookVecFromRotations(yaw, pitch);
        return target.getBoundingBox()
                .inflate(0.1D)
                .clip(eyePos, eyePos.add(lookVec.scale(128.0D)))
                .map(vec3 -> vec3.subtract(target.position()))
                .orElse(fallback);
    }

    private Vec3 getLookVecFromRotations(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float x = -Mth.cos(pitchRad) * Mth.sin(yawRad);
        float y = -Mth.sin(pitchRad);
        float z = Mth.cos(pitchRad) * Mth.cos(yawRad);
        return new Vec3(x, y, z).normalize();
    }

    private void releaseAutoBlock() {
        releaseUsingItem();
        clearAutoBlockState();
    }

    private void clearAutoBlockState() {
        wasPacketBlocking = false;
        wasBlocking = false;
        blockTick = 0;

        if (wasBlinking) {
            BlinkUtil.INSTANCE.sync(true, true);
            BlinkUtil.INSTANCE.stopBlink();
            wasBlinking = false;
        }
    }

    private void resetAutoBlock() {
        if (wasBlocking)
            releaseUsingItem();

        wasPacketBlocking = false;
        wasBlocking = false;
        blockTick = 0;
        lastOpenBlockTime = 0;

        if (wasBlinking) {
            BlinkUtil.INSTANCE.sync(true, true);
            BlinkUtil.INSTANCE.stopBlink();
            wasBlinking = false;
        }
    }

    private void releaseUsingItem() {
        if (mc.player != null && mc.gameMode != null && mc.player.isUsingItem()) {
            mc.gameMode.releaseUsingItem(mc.player);
            return;
        }

        PacketUtil.sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                Direction.DOWN
        ));
    }

    private void spoofAutoBlockSlot() {
        if (mc.player == null) return;
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        int spoofSlot = findEmptySlot(oldSlot);
        PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(spoofSlot));
        PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(oldSlot));
    }

    private void spoofHypixelNoSlowSlot() {
        if (mc.player == null) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        int spoofSlot = RandomUtil.nextInt(0, 9);
        if (spoofSlot == oldSlot)
            spoofSlot = (oldSlot + 1) % 9;

        PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(spoofSlot));
        PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(oldSlot));
    }

    private void swapToAlternateSwordAndBlock(float yaw, float pitch) {
        if (mc.player == null) return;
        int swordSlot = findSwordSlot(mc.player.getInventory().getSelectedSlot());
        if (swordSlot == -1) return;
        PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(swordSlot));
        sendSwordBlockPackets(yaw, pitch);
    }

    private int findEmptySlot(int currentSlot) {
        if (mc.player == null) return Math.floorMod(currentSlot - 1, 9);
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && mc.player.getInventory().getItem(i).isEmpty())
                return i;
        }
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot)
                return i;
        }
        return Math.floorMod(currentSlot - 1, 9);
    }

    private int findSwordSlot(int currentSlot) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && ToolUtil.INSTANCE.isSword(mc.player.getInventory().getItem(i)))
                return i;
        }
        return -1;
    }

    private void resetPacketUnblocking() {
        if (wasBlocking) {
            resetAutoBlock();
            return;
        }

        if (wasPacketBlocking) {
            int oldSlot = mc.player.getInventory().getSelectedSlot();
            if (oldSlot == 0) {
                PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(oldSlot + 1));
                PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(oldSlot));
            } else {
                PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(oldSlot - 1));
                PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(oldSlot));
            }
            wasPacketBlocking = false;
        }
    }

    private boolean startSpreadTeleportSequence(List<Entity> attackTargets, double baseAttackReachSquared,
                                                String autoBlockMode, float yaw, float pitch,
                                                Entity blockTarget, boolean canAutoBlock) {
        int spreadTicks = (int) Math.clamp(tpTicks.getValue(), 0.0, 60.0);
        if (spreadTicks == 0 || attackTargets.isEmpty())
            return false;
        int totalTicks = spreadTicks + 1;

        boolean returnAfterAttack = tpBack.getValue();
        ArrayList<TeleportStep> steps = new ArrayList<>();
        Vec3 simulatedPosition = mc.player.position();
        Vec3 simulatedEyePosition = mc.player.getEyePosition();
        double eyeHeight = mc.player.getEyeHeight();
        int totalMovePackets = 0;

        for (Entity target : attackTargets) {
            boolean needsExtendedTeleport = EntityExtension.squaredBoxedDistanceTo(target, simulatedEyePosition)
                    >= baseAttackReachSquared;
            boolean teleport = randomTp.getValue() || (tpReach.getValue() && needsExtendedTeleport);
            if (!teleport) {
                steps.add(new TeleportAttackStep(target, false));
                continue;
            }

            ResolvedTeleportPath resolvedPath = resolveTeleportPath(simulatedPosition, target, randomTp.getValue());
            if (resolvedPath == null && randomTp.getValue() && needsExtendedTeleport)
                resolvedPath = resolveTeleportPath(simulatedPosition, target, false);

            if (resolvedPath == null) {
                if (!needsExtendedTeleport)
                    steps.add(new TeleportAttackStep(target, false));
                continue;
            }

            ArrayList<Vec3> paths = resolvedPath.positions();
            Vec3 arrivalPosition = resolvedPath.arrivalPosition();
            steps.add(new TeleportGoStep(target, randomTp.getValue(), needsExtendedTeleport, simulatedPosition, paths));
            totalMovePackets += paths.size();
            steps.add(new TeleportAttackStep(target, !returnAfterAttack));

            if (returnAfterAttack) {
                steps.add(new TeleportReturnStep(paths.size()));
                totalMovePackets += paths.size();
            } else {
                simulatedPosition = arrivalPosition;
                simulatedEyePosition = arrivalPosition.add(0.0D, eyeHeight, 0.0D);
            }
        }

        if (totalMovePackets == 0)
            return false;

        if (wasBlinking)
            BlinkUtil.INSTANCE.sync(true, true);

        pendingTeleportSequence = new PendingTeleportSequence(
                steps,
                totalMovePackets,
                totalTicks,
                swingOrderMode.getValue().toLowerCase(Locale.ROOT),
                autoBlockMode,
                yaw,
                pitch,
                blockTarget,
                canAutoBlock,
                tpOnGroundPacket.getValue(),
                mc.player.horizontalCollision,
                mc.player.position()
        );
        advancePendingTeleportSequence();
        return true;
    }

    private void advancePendingTeleportSequence() {
        PendingTeleportSequence sequence = pendingTeleportSequence;
        if (sequence == null || mc.player == null || mc.gameMode == null)
            return;

        sequence.elapsedTicks = Math.min(sequence.elapsedTicks + 1, sequence.totalTicks);
        int totalMovePackets = Math.max(sequence.totalMovePackets, sequence.sentMovePackets);
        int movePacketTarget = (int) ((long) totalMovePackets
                * sequence.elapsedTicks / sequence.totalTicks);

        while (sequence.nextStep < sequence.steps.size()) {
            TeleportStep step = sequence.steps.get(sequence.nextStep);
            if (step instanceof TeleportGoStep goStep) {
                expandGoStep(sequence, goStep);
                totalMovePackets = Math.max(sequence.totalMovePackets, sequence.sentMovePackets);
                movePacketTarget = (int) ((long) totalMovePackets
                        * sequence.elapsedTicks / sequence.totalTicks);
                continue;
            }
            if (step instanceof TeleportReturnStep) {
                if (!advanceLiveReturn(sequence, movePacketTarget))
                    break;
                totalMovePackets = Math.max(sequence.totalMovePackets, sequence.sentMovePackets);
                movePacketTarget = (int) ((long) totalMovePackets
                        * sequence.elapsedTicks / sequence.totalTicks);
                continue;
            }
            if (step instanceof TeleportMoveStep moveStep) {
                if (sequence.sentMovePackets >= movePacketTarget)
                    break;

                sendTeleportMovePacket(moveStep.position(), sequence.onGround, sequence.horizontalCollision);
                sequence.lastSentPosition = moveStep.position();
                sequence.sentPositions.add(moveStep.position());
                sequence.sentMovePackets++;
            } else if (step instanceof TeleportAttackStep attackStep) {
                if (attackStep.moveClientToArrival() && sequence.lastSentPosition != null) {
                    mc.player.setPos(sequence.lastSentPosition);
                    sequence.sentPositions.clear();
                }
                performSpreadTeleportAttack(attackStep.target(), sequence.swingMode);
            }
            sequence.nextStep++;
        }

        if (sequence.nextStep < sequence.steps.size())
            return;

        if (tpBack.getValue())
            syncTeleportToCurrentPosition(sequence);

        pendingTeleportSequence = null;
        finishAttackCycle(
                sequence.autoBlockMode,
                sequence.yaw,
                sequence.pitch,
                sequence.blockTarget,
                sequence.canAutoBlock
        );
    }

    private void cancelPendingTeleportSequence() {
        PendingTeleportSequence sequence = pendingTeleportSequence;
        if (sequence == null)
            return;

        try {
            if (mc.player == null || sequence.sentPositions.isEmpty())
                return;

            for (int i = sequence.sentPositions.size() - 1; i >= 0; i--)
                sendTeleportMovePacket(sequence.sentPositions.get(i), sequence.onGround, sequence.horizontalCollision);

            sendTeleportMovePacket(mc.player.position(), sequence.onGround, mc.player.horizontalCollision);
        } finally {
            pendingTeleportSequence = null;
            sendingTeleportSequencePacket = false;
        }
    }

    private void expandGoStep(PendingTeleportSequence sequence, TeleportGoStep goStep) {
        Vec3 from = sequence.lastSentPosition != null ? sequence.lastSentPosition : mc.player.position();
        ArrayList<Vec3> path = goStep.plannedPath();
        int plannedCount = path != null ? path.size() : 0;

        if (!isSameExactPosition(from, goStep.plannedFrom())) {
            ResolvedTeleportPath resolvedPath = resolveTeleportPath(from, goStep.target(), goStep.preferRandom());
            if (resolvedPath == null && goStep.preferRandom() && goStep.requiresPath())
                resolvedPath = resolveTeleportPath(from, goStep.target(), false);
            path = resolvedPath != null ? resolvedPath.positions() : null;
        }

        sequence.steps.remove(sequence.nextStep);
        if (path == null || path.isEmpty()) {
            sequence.totalMovePackets = Math.max(0, sequence.totalMovePackets - plannedCount);
            if (goStep.requiresPath())
                skipFollowingAttackAndReturn(sequence);
            else
                keepLocalAttackWithoutReturn(sequence);
            return;
        }

        sequence.totalMovePackets += path.size() - plannedCount;
        for (int i = 0; i < path.size(); i++)
            sequence.steps.add(sequence.nextStep + i, new TeleportMoveStep(path.get(i)));
    }

    private boolean advanceLiveReturn(PendingTeleportSequence sequence, int movePacketTarget) {
        Vec3 current = mc.player.position();
        boolean lastTick = sequence.elapsedTicks >= sequence.totalTicks;

        if (isSameExactPosition(sequence.lastSentPosition, current)) {
            sequence.nextStep++;
            return true;
        }

        if (!lastTick && sequence.sentMovePackets >= movePacketTarget)
            return false;

        ArrayList<Vec3> path = resolveReturnPath(sequence.lastSentPosition, current);
        if (path.isEmpty()) {
            sequence.nextStep++;
            return true;
        }

        for (Vec3 position : path) {
            if (!lastTick && sequence.sentMovePackets >= movePacketTarget)
                break;

            sendTeleportMovePacket(position, sequence.onGround, sequence.horizontalCollision);
            sequence.lastSentPosition = position;
            sequence.sentPositions.add(position);
            sequence.sentMovePackets++;
            sequence.totalMovePackets = Math.max(sequence.totalMovePackets, sequence.sentMovePackets);
        }

        if (lastTick && !isSameExactPosition(sequence.lastSentPosition, current)) {
            sendTeleportMovePacket(current, sequence.onGround, sequence.horizontalCollision);
            sequence.lastSentPosition = current;
            sequence.sentPositions.add(current);
            sequence.sentMovePackets++;
            sequence.totalMovePackets = Math.max(sequence.totalMovePackets, sequence.sentMovePackets);
        }

        if (lastTick || isSameExactPosition(sequence.lastSentPosition, current)) {
            sequence.nextStep++;
            return true;
        }

        return false;
    }

    private void skipFollowingAttackAndReturn(PendingTeleportSequence sequence) {
        if (sequence.nextStep < sequence.steps.size()
                && sequence.steps.get(sequence.nextStep) instanceof TeleportAttackStep)
            sequence.steps.remove(sequence.nextStep);

        if (sequence.nextStep < sequence.steps.size()
                && sequence.steps.get(sequence.nextStep) instanceof TeleportReturnStep returnStep) {
            sequence.totalMovePackets = Math.max(0, sequence.totalMovePackets - returnStep.estimatedPackets());
            sequence.steps.remove(sequence.nextStep);
        }
    }

    private void keepLocalAttackWithoutReturn(PendingTeleportSequence sequence) {
        if (sequence.nextStep < sequence.steps.size()
                && sequence.steps.get(sequence.nextStep) instanceof TeleportAttackStep attackStep
                && attackStep.moveClientToArrival()) {
            sequence.steps.set(sequence.nextStep, new TeleportAttackStep(attackStep.target(), false));
        }

        int returnIndex = sequence.nextStep + 1;
        if (returnIndex < sequence.steps.size()
                && sequence.steps.get(returnIndex) instanceof TeleportReturnStep returnStep) {
            sequence.totalMovePackets = Math.max(0, sequence.totalMovePackets - returnStep.estimatedPackets());
            sequence.steps.remove(returnIndex);
        }
    }

    private ArrayList<Vec3> resolveReturnPath(Vec3 from, Vec3 to) {
        ArrayList<Vec3> path = new ArrayList<>();
        if (to == null)
            return path;

        if (from != null && !isSameExactPosition(from, to)) {
            ArrayList<Vec3> computed = MainPathFinder.computePath(from, to);
            if (computed != null)
                path.addAll(computed);
        }

        if (path.isEmpty() || !isSameExactPosition(path.getLast(), to))
            path.add(to);
        return path;
    }

    private void syncTeleportToCurrentPosition(PendingTeleportSequence sequence) {
        if (mc.player == null)
            return;

        Vec3 current = mc.player.position();
        if (isSameExactPosition(sequence.lastSentPosition, current))
            return;

        sendTeleportMovePacket(current, sequence.onGround, sequence.horizontalCollision);
    }

    private static boolean isSameExactPosition(Vec3 first, Vec3 second) {
        return first != null
                && second != null
                && first.distanceToSqr(second) <= 1.0E-6D;
    }

    private void sendTeleportMovePacket(Vec3 position, boolean onGround, boolean horizontalCollision) {
        boolean wasSendingTeleportSequencePacket = sendingTeleportSequencePacket;
        sendingTeleportSequencePacket = true;
        try {
            PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(
                    position.x,
                    position.y,
                    position.z,
                    onGround,
                    horizontalCollision
            ));
        } finally {
            sendingTeleportSequencePacket = wasSendingTeleportSequencePacket;
        }
    }

    private void performSpreadTeleportAttack(Entity target, String currentSwingMode) {
        boolean wasSendingTeleportSequencePacket = sendingTeleportSequencePacket;
        sendingTeleportSequencePacket = true;
        try {
            performAttack(target, currentSwingMode);
        } finally {
            sendingTeleportSequencePacket = wasSendingTeleportSequencePacket;
        }
    }

    private ResolvedTeleportPath resolveTeleportPath(Vec3 from, Entity target, boolean randomize) {
        if (!randomize) {
            ArrayList<Vec3> paths = MainPathFinder.computePath(from, target.position());
            if (paths == null || paths.isEmpty())
                return null;

            return new ResolvedTeleportPath(paths.getLast(), paths);
        }

        double attackReach = attackRange.getValue();
        if (mc.player == null || mc.level == null || !Double.isFinite(attackReach) || attackReach <= 0.0D)
            return null;

        double minimumRadius = (target.getBbWidth() + mc.player.getBbWidth()) * 0.5D
                + RANDOM_TP_COLLISION_MARGIN;
        double maximumRadius = attackReach + target.getBbWidth() * 0.5D
                - RANDOM_TP_COLLISION_MARGIN;
        if (maximumRadius < minimumRadius)
            return null;

        double minimumRadiusSquared = square(minimumRadius);
        double maximumRadiusSquared = square(maximumRadius);
        double attackReachSquared = square(attackReach);
        int baseY = Mth.floor(target.getBoundingBox().minY);

        for (int attempt = 0; attempt < RANDOM_TP_ATTEMPTS; attempt++) {
            double angle = RandomUtil.nextDouble(0.0D, Math.PI * 2.0D);
            double radius = Math.sqrt(RandomUtil.nextDouble(minimumRadiusSquared, maximumRadiusSquared));
            int blockX = Mth.floor(target.getX() + Math.cos(angle) * radius);
            int blockZ = Mth.floor(target.getZ() + Math.sin(angle) * radius);
            int verticalOffsetStart = RandomUtil.nextInt(0, RANDOM_TP_VERTICAL_OFFSETS.length);

            for (int index = 0; index < RANDOM_TP_VERTICAL_OFFSETS.length; index++) {
                int verticalOffset = RANDOM_TP_VERTICAL_OFFSETS[
                        (verticalOffsetStart + index) % RANDOM_TP_VERTICAL_OFFSETS.length
                ];
                Vec3 candidate = Vec3.atBottomCenterOf(new BlockPos(blockX, baseY + verticalOffset, blockZ));
                if (isSameBlockPosition(from, candidate)
                        || !isValidRandomTeleportPosition(target, candidate, attackReachSquared))
                    continue;

                ArrayList<Vec3> paths = MainPathFinder.computePath(from, candidate);
                if (paths == null || paths.isEmpty())
                    continue;

                Vec3 arrivalPosition = paths.getLast();
                if (isSameBlockPosition(from, arrivalPosition)
                        || !isValidRandomTeleportPosition(target, arrivalPosition, attackReachSquared))
                    continue;

                return new ResolvedTeleportPath(arrivalPosition, paths);
            }
        }

        return null;
    }

    private static boolean isSameBlockPosition(Vec3 first, Vec3 second) {
        return Mth.floor(first.x) == Mth.floor(second.x)
                && Mth.floor(first.y) == Mth.floor(second.y)
                && Mth.floor(first.z) == Mth.floor(second.z);
    }

    private boolean isValidRandomTeleportPosition(Entity target, Vec3 position, double attackReachSquared) {
        if (position == null
                || !Double.isFinite(position.x)
                || !Double.isFinite(position.y)
                || !Double.isFinite(position.z))
            return false;

        int blockX = Mth.floor(position.x);
        int blockY = Mth.floor(position.y);
        int blockZ = Mth.floor(position.z);
        if (!MainPathFinder.isValid(blockX, blockY, blockZ, false))
            return false;

        AABB playerBox = mc.player.getBoundingBox().move(
                position.x - mc.player.getX(),
                position.y - mc.player.getY(),
                position.z - mc.player.getZ()
        ).deflate(1.0E-7D);
        if (playerBox.intersects(target.getBoundingBox()) || !mc.level.noCollision(mc.player, playerBox))
            return false;

        Vec3 eyePosition = position.add(0.0D, mc.player.getEyeHeight(), 0.0D);
        return EntityExtension.squaredBoxedDistanceTo(target, eyePosition) <= attackReachSquared;
    }

    private void attackToEntity(Entity target, boolean tpMode, boolean needsExtendedTeleport) {
        ResolvedTeleportPath resolvedPath = null;

        if (tpMode) {
            resolvedPath = resolveTeleportPath(mc.player.position(), target, randomTp.getValue());
            if (resolvedPath == null && randomTp.getValue() && needsExtendedTeleport)
                resolvedPath = resolveTeleportPath(mc.player.position(), target, false);

            if (resolvedPath == null && needsExtendedTeleport)
                return;

            if (resolvedPath != null) {
                for (Vec3 path : resolvedPath.positions()) {
                    PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(path.x, path.y, path.z, tpOnGroundPacket.getValue(), mc.player.horizontalCollision));
                }

                if (!tpBack.getValue())
                    mc.player.setPos(resolvedPath.arrivalPosition());
            }
        }

        performAttack(target, swingOrderMode.getValue().toLowerCase(Locale.ROOT));

        if (resolvedPath != null && tpBack.getValue()) {
            Vec3 lastSent = resolvedPath.positions().isEmpty()
                    ? resolvedPath.arrivalPosition()
                    : resolvedPath.positions().get(resolvedPath.positions().size() - 1);
            ArrayList<Vec3> returnPath = resolveReturnPath(lastSent, mc.player.position());
            for (Vec3 path : returnPath) {
                PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(
                        path.x,
                        path.y,
                        path.z,
                        tpOnGroundPacket.getValue(),
                        mc.player.horizontalCollision
                ));
            }
        }
    }

    private void performAttack(Entity target, String currentSwingMode) {
        targetEsp.noteImpact(target);
        switch (currentSwingMode) {
            case "1.8":
                mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
                mc.gameMode.attack(mc.player, target);
                mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
                break;

            case "1.9.x":
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
                mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
                break;

            case "no order":
                mc.gameMode.attack(mc.player, target);
                PlayerUtil.INSTANCE.swingHandWithoutPacket(InteractionHand.MAIN_HAND);
                break;
        }

        renderParticles(target);
    }

    private void reset() {
        cancelPendingTeleportSequence();

        if (closestEntity != null)
            rotationManager.stopRotation();

        if (wasBlocking) {
            resetAutoBlock();
        } else if (autoBlockMode.is("Blink1") && wasBlinking) {
            BlinkUtil.INSTANCE.sync(true, true);
            BlinkUtil.INSTANCE.stopBlink();
        }

        lastAttackTime = 0;
        lastOpenBlockTime = 0;
        randomDelay = 0;
        abTick = 0;
        blockTick = 0;

        closestEntity = null;
        multiTargets.clear();
        targetCandidateBuffer.clear();
        for (TargetCandidate candidate : targetCandidatePool)
            candidate.set(null, 0.0D, false);
        targetCandidatePoolCursor = 0;
        wasBlinking = false;
        wasBlocking = false;

        isBlocking = false;
    }

    // =====================================================================
    // TargetESP — ported from the client's historical TargetESP
    // (com.dioxidelite.module.modules.render.advanced.TargetESP) onto the 2D
    // render pipeline: all world-space geometry is projected to screen space
    // via WorldToScreenProjector and drawn with GuiGraphicsExtractor primitives
    // (rotated-rect segments stand in for the old Render3DUtils lines/cylinders).
    // Constants preserved from the historical implementation: 18 orbiting
    // crystals at i * 20 degrees, 3 halo rings at 1.0 / 1.3 / 1.6 radius,
    // 260 ms strike animation, shard spawn (3 + rand(4)) * speed per second,
    // gravity -0.0022, damping 0.96, life 34 + rand(28) ticks, 400 shard cap.
    // =====================================================================
    public static final class TargetEsp {
        private static final int CRYSTAL_SHARDS = 18;
        private static final int MAX_SHARDS = 400;
        private static final long STRIKE_TIME_MS = 260L;
        private static final long FRAME_CAP_MS = 100L;
        private static final int RING_SEGMENTS = 28;
        private static final int IMPACT_RGB = 0xFF4030;

        private final KillAura2 owner;

        public final BooleanSetting enabled;
        public final ModeSetting style;
        public final NumberSetting colorOffset;

        // Crystal style
        public final BooleanSetting strike;
        public final NumberSetting glow;
        public final NumberSetting glowSize;
        public final NumberSetting crystalOpacity;
        public final NumberSetting strikeIntensity;
        public final NumberSetting strikeReach;

        // Shards style
        public final NumberSetting shardSize;
        public final NumberSetting shardSpeed;
        public final BooleanSetting gradient;
        public final BooleanSetting trail;
        public final NumberSetting trailLength;
        public final NumberSetting trailOpacity;
        public final BooleanSetting particles;
        public final NumberSetting particleHeight;
        public final NumberSetting particleLife;
        public final BooleanSetting redOnImpact;
        public final NumberSetting impactIntensity;

        private final List<Shard> shards = new ArrayList<>();
        private final java.util.Random random = new java.util.Random();
        private long lastFrameNanos;
        private long lastImpactMs;
        private double spawnBudget;
        private final MutableVector3d scratchA = new MutableVector3d();
        private final MutableVector3d scratchB = new MutableVector3d();

        TargetEsp(KillAura2 owner) {
            this.owner = owner;
            this.enabled = new BooleanSetting("Target ESP", owner, false);
            this.style = new ModeSetting("ESP Style", owner, "Crystal",
                    new String[]{"Crystal", "Shards"});
            this.colorOffset = new NumberSetting("Color Offset", owner, 0.0, 0.0, 360.0, 5.0);
            this.strike = new BooleanSetting("Strike", owner, true);
            this.glow = new NumberSetting("Glow", owner, 75.0, 0.0, 300.0, 5.0);
            this.glowSize = new NumberSetting("Glow Size", owner, 70.0, 25.0, 300.0, 5.0);
            this.crystalOpacity = new NumberSetting("Crystal Opacity", owner, 100.0, 0.0, 100.0, 5.0);
            this.strikeIntensity = new NumberSetting("Strike Intensity", owner, 100.0, 0.0, 100.0, 5.0);
            this.strikeReach = new NumberSetting("Strike Reach", owner, 100.0, 0.0, 300.0, 5.0);
            this.shardSize = new NumberSetting("Shard Size", owner, 1.0, 0.2, 4.0, 0.1);
            this.shardSpeed = new NumberSetting("Shard Speed", owner, 1.0, 0.1, 8.0, 0.1);
            this.gradient = new BooleanSetting("Gradient", owner, true);
            this.trail = new BooleanSetting("Trail", owner, true);
            this.trailLength = new NumberSetting("Trail Length", owner, 1.0, 0.1, 4.0, 0.1);
            this.trailOpacity = new NumberSetting("Trail Opacity", owner, 70.0, 0.0, 100.0, 5.0);
            this.particles = new BooleanSetting("Particles", owner, true);
            this.particleHeight = new NumberSetting("Particle Height", owner, 1.0, 0.0, 3.0, 0.05);
            this.particleLife = new NumberSetting("Particle Life", owner, 1.0, 0.2, 4.0, 0.1);
            this.redOnImpact = new BooleanSetting("Red On Impact", owner, false);
            this.impactIntensity = new NumberSetting("Impact Intensity", owner, 1.5, 0.0, 4.0, 0.1);
            strike.setVisible(() -> style.is("Crystal"));
            glow.setVisible(() -> style.is("Crystal"));
            glowSize.setVisible(() -> style.is("Crystal"));
            crystalOpacity.setVisible(() -> style.is("Crystal"));
            strikeIntensity.setVisible(() -> style.is("Crystal") && strike.getValue());
            strikeReach.setVisible(() -> style.is("Crystal") && strike.getValue());
            shardSize.setVisible(() -> style.is("Shards"));
            shardSpeed.setVisible(() -> style.is("Shards"));
            gradient.setVisible(() -> style.is("Shards"));
            trail.setVisible(() -> style.is("Shards"));
            trailLength.setVisible(() -> style.is("Shards") && trail.getValue());
            trailOpacity.setVisible(() -> style.is("Shards") && trail.getValue());
            particles.setVisible(() -> style.is("Shards"));
            particleHeight.setVisible(() -> style.is("Shards"));
            particleLife.setVisible(() -> style.is("Shards"));
            redOnImpact.setVisible(() -> style.is("Shards"));
            impactIntensity.setVisible(() -> style.is("Shards"));
        }

        public void reset() {
            shards.clear();
            lastFrameNanos = 0L;
            lastImpactMs = 0L;
            spawnBudget = 0.0;
        }

        /** Refreshes the strike animation; called from KillAura2#performAttack. */
        public void noteImpact(Entity target) {
            if (target != null)
                lastImpactMs = System.currentTimeMillis();
        }

        private List<Entity> collectTargets() {
            List<Entity> targets = new ArrayList<>();
            Entity closest = owner.closestEntity;
            if (closest != null && closest.isAlive())
                targets.add(closest);
            for (Entity entity : owner.multiTargets) {
                if (entity != null && entity.isAlive() && !targets.contains(entity))
                    targets.add(entity);
            }
            return targets;
        }

        public void render(GuiGraphicsExtractor graphics, float partialTick) {
            if (!enabled.getValue())
                return;

            List<Entity> targets = collectTargets();
            if (mc.player == null || mc.level == null) {
                reset();
                return;
            }

            long nowNanos = System.nanoTime();
            float deltaMs = lastFrameNanos == 0L
                    ? 0.0f
                    : Math.min((nowNanos - lastFrameNanos) / 1_000_000.0f, FRAME_CAP_MS);
            lastFrameNanos = nowNanos;
            long nowMs = System.currentTimeMillis();

            if (!style.is("Crystal")) {
                updateShards(deltaMs);
                for (Entity target : targets)
                    if (target instanceof LivingEntity living)
                        spawnShards(living, deltaMs);
                renderShards(graphics, nowMs);
                return;
            }

            shards.clear();
            for (Entity target : targets) {
                if (!(target instanceof LivingEntity living))
                    continue;
                double x = living.xOld + (living.getX() - living.xOld) * (double) partialTick;
                double y = living.yOld + (living.getY() - living.yOld) * (double) partialTick;
                double z = living.zOld + (living.getZ() - living.zOld) * (double) partialTick;
                renderCrystal(graphics, living, new Vec3(x, y, z), nowMs);
            }
        }

        // ---------------------------------------------------------------- crystal

        private void renderCrystal(GuiGraphicsExtractor graphics, LivingEntity target, Vec3 foot, long nowMs) {
            double height = Math.max(0.2, target.getBbHeight());
            double baseRadius = Math.max(0.3, target.getBbWidth() * 0.86 + 0.18);
            double sizeScale = Mth.clamp(glowSize.getValue() / 100.0, 0.25, 3.0);
            double ringRadius = baseRadius * (0.55 + 0.75 * sizeScale);
            float glowFactor = (float) Mth.clamp(glow.getValue() / 100.0, 0.0, 3.0);
            float opacity = (float) Mth.clamp(crystalOpacity.getValue() / 100.0, 0.0, 1.0);

            int halo = glowColor();
            int haloAlpha = Math.round(alphaOf(halo) * glowFactor * opacity);
            int base = baseColor();
            int crystalArgb = withAlpha(base, Math.round(alphaOf(base) * opacity));
            int edgeArgb = withAlpha(halo, Math.min(255, haloAlpha + 48));

            // Three outward halo rings (historical RenderSupport_154 bloom).
            for (int ring = 0; ring < 3 && haloAlpha > 4; ring++) {
                double radius = ringRadius * (1.0 + ring * 0.30);
                int alpha = Math.round(haloAlpha / (float) (ring + 1));
                if (alpha <= 4)
                    continue;
                drawWorldCircle(graphics, foot.x, foot.y + height * 0.5, foot.z, radius, 1.6f, withAlpha(halo, alpha));
            }

            float scale = pixelScale(foot.x, foot.y + height * 0.5, foot.z);
            if (scale <= 0.0f)
                return;

            // 18 orbiting crystals: angle = i * 20 degrees + time phase (RenderSupport_158).
            double spinDegrees = (nowMs % 360_000L) / 2.5 * 0.3;
            for (int i = 0; i < CRYSTAL_SHARDS; i++) {
                double angle = Math.toRadians(spinDegrees + i * 20.0);
                double radius = ringRadius * (0.88 + 0.22 * Math.sin(Math.toRadians(spinDegrees * 2.0 + i * 20.0)));
                double vertical = (Math.sin(Math.toRadians(spinDegrees * 1.3 + i * 47.0)) + 1.0) * 0.5;
                double y = foot.y + height * (0.12 + 0.76 * vertical);
                double centerX = foot.x + Math.cos(angle) * radius;
                double centerZ = foot.z + Math.sin(angle) * radius;
                double half = (0.045 + 0.035 * (0.5 + 0.5 * Math.sin(Math.toRadians(spinDegrees * 3.0 + i * 40.0)))) * sizeScale;
                if (WorldToScreenProjector.project(centerX, y, centerZ, scratchA) == null)
                    continue;
                float size = Math.max(1.5f, (float) (half * 2.0 * scale));
                int x0 = (int) Math.round(scratchA.x - size / 2.0f);
                int y0 = (int) Math.round(scratchA.y - size / 2.0f);
                int x1 = x0 + Math.max(1, Math.round(size));
                int y1 = y0 + Math.max(1, Math.round(size));
                graphics.fill(x0, y0, x1, y1, crystalArgb);
                graphics.outline(x0, y0, x1, y1, edgeArgb);
            }

            renderStrike(graphics, foot, height, ringRadius, nowMs);
        }

        private void renderStrike(GuiGraphicsExtractor graphics, Vec3 foot, double height, double ringRadius, long nowMs) {
            if (!strike.getValue() || lastImpactMs == 0L)
                return;
            long elapsed = nowMs - lastImpactMs;
            if (elapsed < 0L || elapsed > STRIKE_TIME_MS)
                return;

            float progress = elapsed / (float) STRIKE_TIME_MS;
            double reach = Mth.clamp(strikeReach.getValue() / 100.0, 0.0, 3.0);
            double radius = ringRadius * (0.7 + 1.5 * progress) * reach;
            float strength = (float) Mth.clamp(strikeIntensity.getValue() / 100.0, 0.0, 1.0);
            int base = baseColor();
            int alpha = Math.round(alphaOf(base) * strength * (1.0f - progress));
            if (alpha <= 4)
                return;
            drawWorldCircle(graphics, foot.x, foot.y + height * 0.35, foot.z, radius, 2.0f, withAlpha(base, alpha));
        }

        // ---------------------------------------------------------------- shards

        private void spawnShards(LivingEntity target, float deltaMs) {
            double perSecond = 20.0 * Math.max(0.15, shardSpeed.getValue());
            spawnBudget += Math.max(0.0f, deltaMs) / 1000.0 * perSecond;
            int guard = 0;
            while (spawnBudget >= 1.0 && shards.size() < MAX_SHARDS && guard++ < 64) {
                spawnBudget -= 1.0;
                int count = 3 + random.nextInt(4);
                for (int i = 0; i < count && shards.size() < MAX_SHARDS; i++) {
                    shards.add(createShard(target));
                }
            }
        }

        /** Historical RenderSupport_151.L(EntityLivingBase, float, RenderSupport_156) equivalent. */
        private Shard createShard(LivingEntity target) {
            double height = Math.max(0.2, target.getBbHeight());
            double minY = target.getY() + 0.03;
            double maxY = target.getY() + height - 0.03;
            double heightSpread = Mth.clamp(particleHeight.getValue(), 0.0, 3.0);
            double phaseClock = (System.currentTimeMillis() % 3_600_000L) * 0.36;
            double centrePhase = (Math.sin(Math.toRadians(0.0 + phaseClock)) + 1.0) * 0.5;
            double vertical = heightSpread >= 0.98
                    ? random.nextDouble()
                    : Mth.clamp(centrePhase + (random.nextDouble() - 0.5) * heightSpread, 0.0, 1.0);
            double y = minY + (maxY - minY) * vertical;

            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = (target.getBbWidth() * 0.86 + 0.18)
                    * Math.max(0.1, shardSize.getValue())
                    * (0.88 + random.nextDouble() * 0.22);
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;

            Shard shard = new Shard();
            shard.x = x;
            shard.y = y;
            shard.z = z;
            shard.prevX = x;
            shard.prevY = y;
            shard.prevZ = z;
            double tangent = (random.nextDouble() - 0.5) * 0.01;
            shard.motionX = Math.cos(angle) * tangent;
            shard.motionZ = Math.sin(angle) * tangent;
            shard.motionY = -8.0E-4 - random.nextDouble() * 0.006;
            shard.floorY = target.getY() - 0.08;
            shard.age = 0.0;
            shard.life = Math.max(1, (int) Math.round((34.0 + random.nextInt(28)) * Math.max(0.1, particleLife.getValue())));
            shard.halfSize = (float) ((0.0065 + random.nextFloat() * 0.014) * Math.max(0.1, shardSize.getValue()));
            shard.hot = redOnImpact.getValue() && target.hurtTime > 0;
            return shard;
        }

        /** Historical RenderSupport_151.L(): damping 0.96, gravity -0.0022, life/floor expiry. */
        private void updateShards(float deltaMs) {
            double ticks = Math.max(0.0f, deltaMs) / 50.0;
            java.util.Iterator<Shard> iterator = shards.iterator();
            while (iterator.hasNext()) {
                Shard shard = iterator.next();
                shard.prevX = shard.x;
                shard.prevY = shard.y;
                shard.prevZ = shard.z;
                shard.x += shard.motionX;
                shard.y += shard.motionY;
                shard.z += shard.motionZ;
                shard.motionX *= 0.96;
                shard.motionZ *= 0.96;
                shard.motionY -= 0.0022;
                shard.age += ticks;
                if (shard.age >= shard.life || shard.y < shard.floorY) {
                    iterator.remove();
                }
            }
        }

        private void renderShards(GuiGraphicsExtractor graphics, long nowMs) {
            if (shards.isEmpty())
                return;

            float impact = impactEnvelope(nowMs);
            float glowFactor = (float) Mth.clamp(glow.getValue() / 100.0, 0.0, 3.0);
            float trailAlpha = (float) Mth.clamp(trailOpacity.getValue() / 100.0, 0.0, 1.0);
            double trailStretch = Math.max(1.0, trailLength.getValue());
            boolean drawTrail = trail.getValue();
            boolean drawParticles = particles.getValue();
            List<Entity> targets = collectTargets();
            float scale = 0.0f;
            if (!targets.isEmpty()) {
                Entity primary = targets.get(0);
                double x = primary.xOld + (primary.getX() - primary.xOld);
                scale = pixelScale(x, primary.getY() + primary.getBbHeight() * 0.5, primary.getZ());
            }
            if (scale <= 0.0f)
                scale = 120.0f;

            for (Shard shard : shards) {
                float progress = Mth.clamp((float) (shard.age / shard.life), 0.0f, 1.0f);
                float fade = 1.0f - progress;
                int base = shard.hot ? IMPACT_RGB : baseColor();
                int tinted = gradient.getValue() ? lerpColor(base, glowColor(), progress) : base;
                float brightness = fade * Math.min(1.0f, Math.max(0.05f, glowFactor))
                        * (1.0f + (float) (impactIntensity.getValue() - 1.0) * impact);
                int alpha = Math.round(alphaOf(tinted) * brightness);
                if (alpha <= 2)
                    continue;

                if (drawTrail) {
                    if (WorldToScreenProjector.project(shard.prevX, shard.prevY, shard.prevZ, scratchA) != null
                            && WorldToScreenProjector.project(shard.x, shard.y, shard.z, scratchB) != null) {
                        double fromX = scratchA.x, fromY = scratchA.y;
                        double toX = scratchB.x, toY = scratchB.y;
                        if (trailStretch > 1.0) {
                            fromX += (fromX - toX) * (trailStretch - 1.0);
                            fromY += (fromY - toY) * (trailStretch - 1.0);
                        }
                        drawScreenSegment(graphics, fromX, fromY, toX, toY, 1.5f,
                                withAlpha(tinted, Math.round(alpha * trailAlpha)));
                    }
                }

                if (drawParticles) {
                    if (WorldToScreenProjector.project(shard.x, shard.y, shard.z, scratchA) != null) {
                        double half = Math.max(0.004, shard.halfSize * (1.0 + 0.6 * impact));
                        float size = Math.max(1.0f, (float) (half * 2.0 * scale));
                        int x0 = (int) Math.round(scratchA.x - size / 2.0f);
                        int y0 = (int) Math.round(scratchA.y - size / 2.0f);
                        graphics.fill(x0, y0, x0 + Math.max(1, Math.round(size)), y0 + Math.max(1, Math.round(size)),
                                withAlpha(tinted, alpha));
                    }
                }
            }
        }

        /** Historical RenderSupport_152 impact envelope: fade in, then fade out. */
        private float impactEnvelope(long nowMs) {
            if (lastImpactMs == 0L)
                return 0.0f;
            long elapsed = nowMs - lastImpactMs;
            if (elapsed < 0L || elapsed > STRIKE_TIME_MS)
                return 0.0f;

            float progress = elapsed / (float) STRIKE_TIME_MS;
            double fadeIn = 0.1;
            if (progress < fadeIn) {
                return (float) (progress / Math.max(fadeIn, 1.0e-4));
            }
            double fadeOut = Math.max(1.0e-4, 0.6);
            return (float) Mth.clamp(1.0 - (progress - fadeIn) / fadeOut, 0.0, 1.0);
        }

        // ---------------------------------------------------------------- drawing

        /** Projects a horizontal world-space circle and strokes it as screen-space segments. */
        private void drawWorldCircle(GuiGraphicsExtractor graphics, double centerX, double centerY, double centerZ,
                                     double radius, float lineWidth, int argb) {
            double prevX = 0.0, prevY = 0.0;
            boolean hasPrev = false;
            for (int i = 0; i <= RING_SEGMENTS; i++) {
                double angle = Math.PI * 2.0 * i / RING_SEGMENTS;
                double wx = centerX + Math.cos(angle) * radius;
                double wz = centerZ + Math.sin(angle) * radius;
                if (WorldToScreenProjector.project(wx, centerY, wz, scratchA) == null) {
                    hasPrev = false;
                    continue;
                }
                if (hasPrev) {
                    drawScreenSegment(graphics, prevX, prevY, scratchA.x, scratchA.y, lineWidth, argb);
                }
                prevX = scratchA.x;
                prevY = scratchA.y;
                hasPrev = true;
            }
        }

        /** Strokes a screen-space segment as a rotated filled rectangle (stand-in for 3D lines). */
        private void drawScreenSegment(GuiGraphicsExtractor graphics, double x1, double y1, double x2, double y2,
                                       float lineWidth, int argb) {
            double dx = x2 - x1, dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 0.5)
                return;
            float halfWidth = Math.max(0.5f, lineWidth * 0.5f);
            int halfLen = Math.max(1, (int) Math.round(len / 2.0));
            int halfW = Math.max(1, (int) Math.round(halfWidth));
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) ((x1 + x2) * 0.5), (float) ((y1 + y2) * 0.5));
            graphics.pose().rotate((float) Math.atan2(dy, dx));
            graphics.fill(-halfLen, -halfW, halfLen, halfW, argb);
            graphics.pose().popMatrix();
        }

        /** Pixels per world unit at a point (vertical projection), 0 when behind the camera. */
        private float pixelScale(double x, double y, double z) {
            if (WorldToScreenProjector.project(x, y, z, scratchA) == null)
                return 0.0f;
            if (WorldToScreenProjector.project(x, y + 1.0, z, scratchB) == null)
                return 0.0f;
            return (float) Math.abs(scratchB.y - scratchA.y);
        }

        // ---------------------------------------------------------------- colors

        private int baseColor() {
            return ClientColors.colorAtOffset((int) colorOffset.getValue());
        }

        private int glowColor() {
            return ClientColors.colorAtOffset((int) colorOffset.getValue() + 60);
        }

        private static int alphaOf(int argb) {
            return argb >>> 24;
        }

        private static int withAlpha(int argb, int alpha) {
            return (Mth.clamp(alpha, 0, 255) << 24) | (argb & 0xFFFFFF);
        }

        private static int lerpColor(int from, int to, float progress) {
            float t = Mth.clamp(progress, 0.0f, 1.0f);
            int a = Math.round(alphaOf(from) + (alphaOf(to) - alphaOf(from)) * t);
            int r = Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
            int g = Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
            int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
            return Mth.clamp(a, 0, 255) << 24 | Mth.clamp(r, 0, 255) << 16 | Mth.clamp(g, 0, 255) << 8 | Mth.clamp(b, 0, 255);
        }

        private static final class Shard {
            private double x;
            private double y;
            private double z;
            private double prevX;
            private double prevY;
            private double prevZ;
            private double motionX;
            private double motionY;
            private double motionZ;
            private double floorY;
            private double age;
            private int life;
            private float halfSize;
            private boolean hot;
        }
    }
}
