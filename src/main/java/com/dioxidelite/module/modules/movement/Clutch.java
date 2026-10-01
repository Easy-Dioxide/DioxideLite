package com.dioxidelite.module.modules.movement;

// ---------------------------------------------------------------------------
// 移植来源：Vape-v4 gg/vape/module/blatant/Clutch.java
// 变更（1.7.10 -> 26.1.2 重写）：
//   - Vape Clutch 在 ModManager 中被 addMinecraft1710Constraint 约束为仅 1.7.10。
//     1.7.10 的 PlayerControllerMP.processRightClickBlock / ItemStack.tryPlaceItem /
//     Block.canPlaceBlockAt 签名在 26.x 全变（26.x 走 BlockPlaceContext/useOn），
//     因此本模块不是逐行移植而是按 Vape 语义重写。
//   - 落点预测改用 DioxideLite FallingPlayer（已存在，等价 Vape PlayerSimulationUtil）。
//   - 方块放置改用 DioxideLite BlockPlaceHelper.findPlaceInfo + place（26.x 原生 useItemOn）。
//   - 旋转改用 DioxideLite RotationManager + RotationUtils.calculate(BlockPos)。
//   - Value 系统改为 DioxideLite Setting。
//   - 事件 @EventHandler -> @Listen，EventPreTick -> PlayerTickEvent.Pre。
// 保留（Vape 核心语义）：
//   - 掉落检测：预测玩家落点，若会掉到虚空或低于阈值高度则触发。
//   - 自动放方块接住：在落点放方块。
//   - 旋转到放置点（可选 Silent）。
//   - 物品栏自动切换到可放置方块。
// ---------------------------------------------------------------------------

import com.dioxidelite.DioxideLite;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.util.player.FallingPlayer;
import com.dioxidelite.util.player.FindItemResult;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import com.dioxidelite.util.world.BlockPlaceHelper;
import com.dioxidelite.util.world.BlockPlaceHelper.PlaceInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * 掉落自动放方块模块，移植自 Vape-v4 Clutch 的核心语义。
 * <p>
 * Vape 原版是 1.7.10 专属（含梯子/方块/路径搜索等复杂子模块）；本 26.x 重写保留
 * "预测玩家将掉落 → 在落点自动放置方块接住" 的核心行为。落点预测走 DioxideLite
 * {@link FallingPlayer}，方块放置走 {@link BlockPlaceHelper}，旋转交给
 * {@link RotationManager}（移动修复与视觉转头由客户端自带的 MovementFix 接管）。
 */
public final class Clutch extends Module {

    public static final Clutch INSTANCE = new Clutch();

    private static final Minecraft mc = DioxideLite.mc();

    /** 移植自 Vape Clutch 的放置模式：Normal=脚下放方块、Ladder=梯子（26.x 简化）。 */
    private enum PlaceMode {
        Normal,
        Ladder
    }

    private final IntSetting previewTicks = add(new IntSetting("Preview Ticks", 8, 2, 40, 1));
    private final DoubleSetting rotationSpeed = add(new DoubleSetting("Rotation Speed", 120.0, 1.0, 360.0, 1.0));
    private final BooleanSetting silentRotation = add(new BooleanSetting("Silent Rotation", true));
    private final BooleanSetting silentSwap = add(new BooleanSetting("Silent Swap", true));
    private final BooleanSetting swing = add(new BooleanSetting("Swing", true));
    private final BooleanSetting onlyInAir = add(new BooleanSetting("Only In Air", true));
    private final BooleanSetting preferWaterBucket = add(new BooleanSetting("Prefer Water Bucket", false));
    private final EnumSetting<PlaceMode> placeMode = add(new EnumSetting<>("Place Mode", PlaceMode.Normal));

    private Clutch() {
        super("Clutch", Category.MOVEMENT);
    }

    @Listen
    private void onPlayerTick(PlayerTickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        if (mc.screen != null) {
            return;
        }

        // 只在空中且不在潜行挖方块时触发（移植自 Vape Clutch onGround 检测）
        if (onlyInAir.get() && mc.player.onGround()) {
            return;
        }

        // 预测落点（移植自 Vape PlayerSimulationUtil）。
        // findCollision 返回玩家将踩到的实心方块（Direction.UP）；返回 null 表示
        // 预测范围内无落点（会继续下落或掉虚空）——这正是 Clutch 需要救场的场景。
        FallingPlayer simulation = new FallingPlayer(mc.player);
        BlockPos landing = simulation.findCollision(previewTicks.get());

        // landing != null：玩家会被接住，无需 clutch。
        // landing == null：预测范围内无落点，触发放置救场。
        // （落差过大提前接住以减摔伤的更复杂路径规划留作后续扩展。）
        boolean willFallIntoVoid = landing == null;
        if (!willFallIntoVoid) {
            return;
        }

        // 优先水桶（移植自 Vape Clutch waterBucket 选项）
        if (preferWaterBucket.get() && tryWaterBucket()) {
            return;
        }

        // 找可放置方块（热栏）
        FindItemResult block = BlockPlaceHelper.findHotbar(usableBlockItems());
        if (!block.found()) {
            return;
        }

        // 找可放置位置（含支撑面 + hit 点）：
        // 从玩家脚下逐层往下扫描可替换空位，对每个调用 findPlaceInfo 寻找支撑面。
        // strictRaycast=false 放宽射线检测，提高掉虚空场景（脚下都是空气）的命中率。
        PlaceInfo info = findPlaceableTarget();
        if (info == null) {
            return;
        }

        // 旋转到放置点（可选 Silent）
        if (silentRotation.get()) {
            Rot2f toPlace = RotationUtils.calculate(info.support());
            RotationManager.INSTANCE.setRotations(toPlace, rotationSpeed.get(), Priority.High);
        }

        // 放置
        BlockPlaceHelper.place(info, block, silentSwap.get(), swing.get());
    }

    /**
     * 从玩家脚下逐层往下扫描可替换空位，对每个位置调用
     * {@link BlockPlaceHelper#findPlaceInfo} 寻找支撑面。返回第一个有支撑面
     * 且玩家可达（reach）的 {@link PlaceInfo}。strictRaycast=false 放宽射线检测，
     * 提高掉虚空场景（脚下都是空气）的命中率。
     */
    private PlaceInfo findPlaceableTarget() {
        BlockPos feet = mc.player.blockPosition();
        for (int i = 0; i <= 6; i++) {
            BlockPos candidate = feet.below(i);
            if (!mc.level.getBlockState(candidate).canBeReplaced()) continue;
            PlaceInfo info = BlockPlaceHelper.findPlaceInfo(candidate, false);
            if (info != null) return info;
        }
        return null;
    }

    /** 26.x 可放置方块清单（移植自 Vape Clutch 的方块优先级，简化）。 */
    private Item[] usableBlockItems() {
        if (placeMode.get() == PlaceMode.Ladder) {
            return new Item[]{Items.LADDER};
        }
        return new Item[]{
                Items.COBBLESTONE, Items.STONE, Items.DIRT, Items.NETHERRACK,
                Items.COBBLED_DEEPSLATE, Items.SANDSTONE, Items.OBSIDIAN
        };
    }

    /**
     * 水桶接住（移植自 Vape Clutch waterBucket：在脚下放水面减速）。
     * 26.x：用 mc.gameMode.useItem + 旋转到脚下。
     */
    private boolean tryWaterBucket() {
        FindItemResult bucket = BlockPlaceHelper.findHotbar(Items.WATER_BUCKET);
        if (!bucket.found() || bucket.getHand() == null) {
            return false;
        }
        // 旋转到脚下
        if (silentRotation.get()) {
            BlockPos feet = mc.player.blockPosition().below();
            Rot2f down = RotationUtils.calculate(feet);
            RotationManager.INSTANCE.setRotations(down, rotationSpeed.get(), Priority.High);
        }
        // 使用水桶
        mc.gameMode.useItem(mc.player, bucket.getHand());
        if (swing.get()) {
            mc.player.swing(bucket.getHand());
        }
        return true;
    }

    @Override
    protected void onDisable() {
        // Clutch 不持有持久旋转状态，旋转由 RotationManager 自然接管。
    }

    /** 供其他模块查询当前是否正在 clutch（移植自 Vape RescueModuleUtil 语义）。 */
    public boolean isClutching() {
        return isEnabled() && mc.player != null && !mc.player.onGround();
    }
}
