package com.dioxidelite.module.modules.combat;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.manager.FriendManager;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.util.player.ChatUtils;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * [v2.2.6 补全] Bed Defender 真实实现：护床防御。
 * <p>
 * 修复前该模块只有 4 个设置项（Targets Per Tick / Aim Speed / Bedwars Only / Debug Logs），
 * 没有任何执行逻辑，是空壳。本次补全：每 tick 扫描最近一张床，检测防御半径内靠近的敌人，
 * 旋转瞄准并真实攻击（每 tick 最多处理 targetsPerTick 个目标，默认 8 CPS）。
 * </p>
 * <p>Bedwars Only 开启时仅在床战服务器生效（Hypixel 计分板标题含 "BED WARS"，或服务器
 * IP 含 hypixel / bedwars / bed-wars）。目标过滤：排除自己、朋友、旁观者与不在射程内的实体。</p>
 */
public final class BedDefender extends Module {

    public static final BedDefender INSTANCE = new BedDefender();

    public final IntSetting targetsPerTick = add(new IntSetting("Targets Per Tick", 4, 1, 16, 1));
    public final IntSetting aimSpeed = add(new IntSetting("Aim Speed", 20, 1, 180, 1));
    public final BooleanSetting bedwarsOnly = add(new BooleanSetting("Bedwars Only", true));
    public final BooleanSetting debug = add(new BooleanSetting("Debug Logs", false));
    private final DoubleSetting attackRange = add(new DoubleSetting("Attack Range", 4.2, 3.0, 6.0, 0.1));
    private final DoubleSetting defendRadius = add(new DoubleSetting("Defend Radius", 5.0, 2.0, 12.0, 0.5));

    private long lastAttack;

    private BedDefender() {
        super("Bed Defender", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        RotationManager.INSTANCE.releaseSilentRotation(this);
    }

    @Listen
    private void onTick(PlayerTickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        if (bedwarsOnly.get() && !isBedWarsServer()) return;

        BlockPos bed = nearestBed();
        if (bed == null) {
            RotationManager.INSTANCE.releaseSilentRotation(this);
            return;
        }

        List<Player> threats = nearBedThreats(bed);
        if (threats.isEmpty()) {
            RotationManager.INSTANCE.releaseSilentRotation(this);
            return;
        }

        int perTick = Math.min(targetsPerTick.get(), threats.size());
        int attacked = 0;
        for (Player threat : threats) {
            if (attacked >= perTick) break;
            if (tryAttack(threat)) {
                attacked++;
            }
        }
        if (debug.get() && attacked > 0) {
            ChatUtils.addChatMessage("Bed Defender: attacked " + attacked + " threat(s) near bed");
        }
    }

    /** 收集床防御半径内、射程内的敌人，按离床距离升序排列。 */
    private List<Player> nearBedThreats(BlockPos bed) {
        List<Player> threats = new ArrayList<>();
        Vec3 bedCenter = Vec3.atCenterOf(bed);
        double rr = defendRadius.get();
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || p.isRemoved() || p.isSpectator()) continue;
            if (FriendManager.INSTANCE.isFriend(p)) continue;
            if (p.distanceToSqr(bedCenter.x, bedCenter.y, bedCenter.z) > rr * rr) continue;
            if (RotationUtils.getEyeDistanceToEntity(p) > attackRange.get()) continue;
            threats.add(p);
        }
        threats.sort(Comparator.comparingDouble(p -> p.distanceToSqr(bedCenter.x, bedCenter.y, bedCenter.z)));
        return threats;
    }

    /** 旋转瞄准并攻击单个目标；命中冷却（默认 8 CPS）与攻击判定成功后返回 true。 */
    private boolean tryAttack(Player target) {
        if (mc.gameMode == null) return false;
        float cooldown = mc.player.getAttackStrengthScale(0f);
        if (cooldown < 1.0f) return false;
        long interval = (long) (1000.0 / 8.0);
        if (System.currentTimeMillis() - lastAttack < interval) return false;
        if (!RotationUtils.isInFov(target, 8.0f)) return false;

        Rot2f rot = RotationUtils.getRotationsToEntity(target);
        RotationManager.INSTANCE.setRotations(rot, Math.max(0.5F, aimSpeed.get() / 10.0F), Priority.High);

        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastAttack = System.currentTimeMillis();
        return true;
    }

    /** 优先复用 BedTracker 缓存的最近床；否则本地扫描（半径 16 格内最近的一张床）。 */
    private BlockPos nearestBed() {
        BlockPos tracked = BedTracker.INSTANCE.trackedBed();
        if (tracked != null) return tracked;
        int r = 16;
        BlockPos origin = mc.player.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-r, -4, -r), origin.offset(r, 4, r))) {
            if (mc.level.getBlockState(p).getBlock() instanceof BedBlock) {
                double d = p.distSqr(origin);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    /** 床战服务器检测：IP 含 hypixel / bedwars / bed-wars，或计分板侧边栏标题含 "BED WARS"。 */
    private boolean isBedWarsServer() {
        ServerData server = mc.getCurrentServer();
        if (server != null && server.ip != null) {
            String ip = server.ip.toLowerCase(Locale.ROOT);
            if (ip.contains("hypixel") || ip.contains("bedwars") || ip.contains("bed-wars")) return true;
        }
        if (mc.level != null) {
            Objective sidebar = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
            if (sidebar != null && sidebar.getDisplayName().getString().toUpperCase(Locale.ROOT).contains("BED WARS")) {
                return true;
            }
        }
        return false;
    }
}
