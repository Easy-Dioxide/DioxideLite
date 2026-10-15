package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.player.AutoTool;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.BehaviorUtils;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.PlayerUtil;
import com.dioxideliteng.util.krs.RotationManager;
import com.dioxideliteng.util.krs.ToolUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.CivBreak).
 * Krs parity: watches outgoing START_DESTROY_BLOCK packets to learn the block the
 * player starts mining, then keeps "civ-breaking" it — Instant mode spams
 * START/STOP_DESTROY_BLOCK packet pairs at Fast Speed, Wait One Break advances to
 * the packet stage after the first legit break, Legit mode only mines normally.
 * Rotations face the target, KillAura is suppressed via BehaviorUtils.noKillAura,
 * and AutoTool swaps (spoofed) while breaking.
 * Notable adaptations vs the original:
 *  - Client.rotationManager replaced with RotationManager.INSTANCE.
 *  - ModuleManager.getModuleState(Scaffold.class) replaced with the null-safe
 *    FeatureManager.scaffold2 check; ModuleManager.getModuleState(AutoTool.class)
 *    replaced with an AutoTool lookup (the native AutoTool has no FeatureManager
 *    field).
 *  - ServerboundSwingPacket no longer exists in 26.3; the swing is sent through
 *    player.swing(hand, SwingAnimation.DEFAULT, true).
 *  - Krs static settings became instance settings; wasBreaking stays static
 *    because getWasBreaking() is part of the static module surface.
 */
public class CivBreak extends Feature {
    private static boolean wasBreaking = false;

    private final ModeSetting mode = new ModeSetting("Mode", this, "Instant",
            new String[]{"Instant", "Wait One Break", "Legit"});
    private final NumberSetting range = new NumberSetting("Range", this, 3.0, 0.1, 6.0, 0.1);
    private final NumberSetting fastSpeed = new NumberSetting("Fast Speed", this, 10.0, 1.0, 10.0, 1.0);
    private final BooleanSetting rotations = new BooleanSetting("Rotations", this, true);

    private final MSTimer rotateTimer = new MSTimer();
    private int originalSlot = -1;
    private BlockPos pos = null;
    private Direction direction = null;
    private int stage = 0;

    private final RotationManager rotationManager = RotationManager.INSTANCE;

    public CivBreak() {
        super("Civ Break", Category.MISC);
        this.fastSpeed.setVisible(() -> this.mode.is("Instant") || this.mode.is("Wait One Break"));
    }

    @Override
    public void onDisable() {
        var player = mc.player;
        if (wasBreaking && player != null) {
            if (originalSlot != -1 && originalSlot != player.getInventory().getSelectedSlot()) {
                player.getInventory().setSelectedSlot(originalSlot);
                originalSlot = -1;
            }
            BehaviorUtils.noKillAura = false;
        }
        PlayerUtil.INSTANCE.stopSpoof();
        if (wasBreaking)
            this.rotationManager.stopRotation();
        if (wasBreaking)
            PlayerUtil.INSTANCE.stopDestroyBlock();
        wasBreaking = false;
        pos = null;
        direction = null;
        stage = 0;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.WORLD) {
            if (wasBreaking) {
                var player = mc.player;
                if (originalSlot != -1) {
                    if (player != null && originalSlot != player.getInventory().getSelectedSlot())
                        player.getInventory().setSelectedSlot(originalSlot);
                    originalSlot = -1;
                }
                PlayerUtil.INSTANCE.stopSpoof();
                BehaviorUtils.noKillAura = false;
            }
            if (wasBreaking)
                this.rotationManager.stopRotation();
            if (wasBreaking)
                PlayerUtil.INSTANCE.stopDestroyBlock();
            wasBreaking = false;
            pos = null;
            direction = null;
            stage = 0;
        } else if (event == Events.ROTATION) {
            onMotion();
        } else if (event instanceof EventPacketSend sendEvent) {
            var packet = sendEvent.getPacket();
            if (packet instanceof ServerboundPlayerActionPacket actionPacket && actionPacket.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                stage = 0;
                pos = actionPacket.getPos();
                direction = actionPacket.getDirection();
            }
        }
    }

    private void onMotion() {
        var player = mc.player;
        var level = mc.level;
        var gameMode = mc.gameMode;
        if (player == null || level == null || gameMode == null || gameMode.getPlayerMode() == GameType.ADVENTURE || gameMode.getPlayerMode() == GameType.SPECTATOR) return;
        if (FeatureManager.scaffold2 != null && FeatureManager.scaffold2.isEnabled()) return;

        BlockPos targetPos = pos;
        Direction targetDirection = direction;
        double rangeLimit = this.range.getValue() * this.range.getValue();
        if (targetPos == null
                || targetDirection == null
                || level.getBlockState(targetPos).isAir() && stage == 1
                || rangeLimit < player.distanceToSqr(Vec3.atCenterOf(targetPos))) {
            if (wasBreaking) {
                PlayerUtil.INSTANCE.stopDestroyBlock();
                if (this.rotations.getValue())
                    this.rotationManager.stopRotation();
                if (originalSlot != -1 && originalSlot != player.getInventory().getSelectedSlot()) {
                    player.getInventory().setSelectedSlot(originalSlot);
                    originalSlot = -1;
                }
                PlayerUtil.INSTANCE.stopSpoof();
                BehaviorUtils.noKillAura = false;
                wasBreaking = false;
            }
            return;
        }

        if (this.rotations.getValue())
            this.rotationManager.faceBlock(targetPos, 180f);

        BehaviorUtils.noKillAura = true;

        if (isAutoToolEnabled()) {
            int bestToolSlot = ToolUtil.INSTANCE.findBestTool(targetPos);
            if (bestToolSlot != -1 && bestToolSlot != player.getInventory().getSelectedSlot()) {
                if (originalSlot == -1)
                    originalSlot = player.getInventory().getSelectedSlot();
                PlayerUtil.INSTANCE.doSpoof(originalSlot);
                player.getInventory().setSelectedSlot(bestToolSlot);
            }
        }

        if (this.mode.is("Legit"))
            stage = 0;
        else if (this.mode.is("Instant"))
            stage = 1;

        wasBreaking = true;

        switch (stage) {
            case 0 -> PlayerUtil.INSTANCE.destroyBlock(targetPos);
            case 1 -> {
                if (this.rotateTimer.hasTimePassed(1000L / ((long) this.fastSpeed.getValue() + 2))) {
                    var localPlayer = mc.player;
                    if (localPlayer != null)
                        localPlayer.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
                    ServerboundPlayerActionPacket.Action action = localPlayer != null && localPlayer.isCreative() ? ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK : ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK;
                    for (int i = 0; i < 2; i++) {
                        PacketUtil.sendPacket(new ServerboundPlayerActionPacket(
                                action,
                                targetPos,
                                targetDirection
                        ));
                    }
                    this.rotateTimer.reset();
                }
            }
        }

        if (this.mode.is("Wait One Break") && level.getBlockState(targetPos).isAir() && stage == 0)
            stage++;
    }

    public static boolean getWasBreaking() {
        return wasBreaking;
    }

    private static boolean isAutoToolEnabled() {
        List<Feature> modules = FeatureManager.getModules();
        for (Feature module : modules) {
            if (module instanceof AutoTool)
                return module.isEnabled();
        }
        return false;
    }
}
