package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.module.modules.combat.BedTracker;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.ui.UiTheme;
import com.dioxidelite.util.render.Render3DUtils;
import com.dioxidelite.util.render.WorldToScreen;
import com.dioxidelite.util.world.BlockUtils;
import io.github.humbleui.skija.Canvas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 移植自 来源客户端 {@code features/render/BedESP.java}：扫描周围床方块，画出床的
 * 描边/填充、床的光柱，以及围住床的防御方块（可选文字标签）。
 * <p>
 * 1:1 对应：Own bed / Enemy beds、Own color / Enemy color、Defense / Defense radius /
 * Defense opacity、Defense tags / Tag scale / Tag falloff / Tag blur、Beam / Beam height /
 * Beam width / Beam opacity / Beam color / Beam tint / Own bed beam / Own beam color /
 * Own beam tint、Fill、Through walls、Line width、Refresh。默认颜色与 来源的
 * 0xFF6BE38B（己方）、0xFFFF6A62（敌方）、0xFF7FD4FF（光柱）一致，床盒外扩 0.003、
 * 床高 0.5625、防御方块 bbox 外扩 0.003、光柱半径 = Beam width、双层（外 0.55×、
 * 内 0.35 半径）也与原实现对应。
 * <p>
 * 说明：来源 直接遍历已加载区块找床；Dioxide 端按 BedTracker 的做法，在玩家周围
 * 的方块盒内扫描（半径见 SCAN_RADIUS 常量）。来源的 BedTracker 能区分“自己的床”，
 * 本端口用 Dioxide 的 {@link BedTracker#trackedBed()}（最近的一张床）做等价近似。
 */
public final class BedESP extends Module {

    // PORT-NOTE: 需要 mixin（可切换深度测试的 3D 管线），本端口只实现了始终穿透墙壁的床/防御方块绘制（Through walls 开关因此不改变绘制方式）。
    // PORT-NOTE: 需要 RenderSupport_103 的模糊底钩子，本端口只实现了不透明底板 + 阴影文字的 Defense tags（Tag blur 只调整底板浓度）。

    public static final BedESP INSTANCE = new BedESP();

    /** 来源的床高常量（0.5625）。 */
    private static final double BED_TOP = 0.5625D;
    /** 来源的描边外扩常量（0.003）。 */
    private static final double EXPAND = 0.003D;
    /** 来源 扫描已加载区块；这里改成玩家周围的方块盒。 */
    private static final int SCAN_RADIUS = 32;
    private static final int SCAN_UP = 12;
    private static final int SCAN_DOWN = 12;
    /** 每张床的防御标签最多列几行（来源的 FeatureSupport_261 也是固定行数）。 */
    private static final int TAG_ROWS = 5;
    private static final int TAG_PLATE = 0x8C0A0F14;
    private static final int TAG_PLATE_BLUR = 0xD90A0F14;

    /** 来源的防御方块调色：黑曜石 / 玻璃 / 其它（来源 用材质贴图颜色，这里用等价近似）。 */
    private static final int DEFENSE_OBSIDIAN = 0xFF1B1226;
    private static final int DEFENSE_GLASS = 0xFFBFE9F5;
    private static final int DEFENSE_OTHER = 0xFFB0B0B0;

    // 设置项顺序与来源 构造函数一致。
    public final BooleanSetting ownBed = add(new BooleanSetting("Own bed", true));
    public final BooleanSetting enemyBeds = add(new BooleanSetting("Enemy beds", true));
    public final ColorSetting ownColor = add(new ColorSetting("Own color", new Color(107, 227, 139, 255)));
    public final ColorSetting enemyColor = add(new ColorSetting("Enemy color", new Color(255, 106, 98, 255)));
    public final BooleanSetting defense = add(new BooleanSetting("Defense", false));
    public final DoubleSetting defenseOpacity = add(new DoubleSetting("Defense opacity", 70.0, 10.0, 100.0, 5.0))
            .visibleWhen(defense::get);
    public final BooleanSetting defenseTags = add(new BooleanSetting("Defense tags", false));
    public final DoubleSetting defenseRadius = add(new DoubleSetting("Defense radius", 3.0, 1.0, 6.0, 1.0))
            .visibleWhen(() -> defense.get() || defenseTags.get());   // defenseTags 在下方声明，lambda 运行时求值
    public final DoubleSetting tagScale = add(new DoubleSetting("Tag scale", 1.0, 0.5, 2.0, 0.05))
            .visibleWhen(defenseTags::get);
    public final DoubleSetting tagFalloff = add(new DoubleSetting("Tag falloff", 1.0, 0.0, 2.0, 0.05))
            .visibleWhen(defenseTags::get);
    public final BooleanSetting tagBlur = add(new BooleanSetting("Tag blur", false))
            .visibleWhen(defenseTags::get);
    public final BooleanSetting beam = add(new BooleanSetting("Beam", false));
    public final DoubleSetting beamHeight = add(new DoubleSetting("Beam height", 24.0, 4.0, 128.0, 4.0))
            .visibleWhen(beam::get);
    public final DoubleSetting beamWidth = add(new DoubleSetting("Beam width", 0.5, 0.1, 2.0, 0.1))
            .visibleWhen(beam::get);
    public final DoubleSetting beamOpacity = add(new DoubleSetting("Beam opacity", 55.0, 10.0, 100.0, 5.0))
            .visibleWhen(beam::get);
    public final BooleanSetting beamColor = add(new BooleanSetting("Beam color", false))
            .visibleWhen(beam::get);
    public final ColorSetting beamTint = add(new ColorSetting("Beam tint", new Color(127, 212, 255, 255)))
            .visibleWhen(() -> beam.get() && beamColor.get());
    public final BooleanSetting ownBedBeam = add(new BooleanSetting("Own bed beam", true))
            .visibleWhen(beam::get);
    public final BooleanSetting ownBeamColor = add(new BooleanSetting("Own beam color", false))
            .visibleWhen(() -> beam.get() && ownBedBeam.get());
    public final ColorSetting ownBeamTint = add(new ColorSetting("Own beam tint", new Color(107, 227, 139, 255)))
            .visibleWhen(() -> beam.get() && ownBedBeam.get() && ownBeamColor.get());
    public final BooleanSetting fill = add(new BooleanSetting("Fill", false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", true));
    public final DoubleSetting lineWidth = add(new DoubleSetting("Line width", 1.5, 0.5, 5.0, 0.5));
    public final DoubleSetting refresh = add(new DoubleSetting("Refresh", 500.0, 100.0, 2000.0, 50.0));

    /** 来源的 FeatureSupport_271：一块暴露在外的防御方块（方块包围盒 + 颜色）。 */
    private record DefenseBlock(BlockPos pos, int color) {
    }

    /** 来源的 FeatureSupport_323：一张床的防御标签（床位置 + 己方/敌方 + 文字行）。 */
    private record DefenseTag(BlockPos bedPos, boolean own, List<String> rows) {
    }

    private final List<BlockPos> beds = new ArrayList<>();
    private final List<DefenseBlock> defenseBlocks = new ArrayList<>();
    private final List<DefenseTag> tags = new ArrayList<>();

    private long lastScanMs;
    private boolean scanned;
    private Object lastLevel;

    private BedESP() {
        super("BedESP", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        clearCache();
    }

    @Override
    protected void onDisable() {
        clearCache();
    }

    private void clearCache() {
        beds.clear();
        defenseBlocks.clear();
        tags.clear();
        scanned = false;
        lastScanMs = 0L;
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }
        // 来源的 WorldChangeEvent → F()：换世界就清空缓存。
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            clearCache();
        }
        long now = System.currentTimeMillis();
        if (!scanned || now - lastScanMs >= refresh.get().longValue()) {
            scanned = true;
            lastScanMs = now;
            rescan();
        }
        if (beds.isEmpty()) {
            return;
        }

        float thickness = lineWidth.get().floatValue();
        for (BlockPos bedPos : beds) {
            BlockState state = mc.level.getBlockState(bedPos);
            if (!(state.getBlock() instanceof BedBlock)) {
                continue;
            }
            BlockPos foot = bedPos.relative(BedBlock.getConnectedDirection(state));
            boolean own = isOwnBed(bedPos, foot);
            if (!isBedVisible(own)) {
                continue;
            }
            AABB box = bedBox(bedPos, foot);
            Color base = own ? ownColor.get() : enemyColor.get();

            // 来源：描边 alpha = 原色 alpha * 0.95，填充 = 原色 alpha * 0.16。
            Render3DUtils.drawOutlineBox(event.getPoseStack(), box, withAlpha(base, 0.95F), thickness);
            if (fill.get()) {
                Render3DUtils.drawFilledBox(box, withAlpha(base, 0.16F));
            }
            if (beam.get() && (!own || ownBedBeam.get())) {
                drawBeam(box, own);
            }
        }

        if (defense.get()) {
            // 来源：防御方块描边 alpha = 原色 alpha * Defense opacity，填充再 * 0.18。
            float opacity = defenseOpacity.get().floatValue() / 100.0F;
            for (DefenseBlock block : defenseBlocks) {
                AABB box = new AABB(block.pos()).inflate(EXPAND);
                Color base = new Color(block.color());
                Render3DUtils.drawOutlineBox(event.getPoseStack(), box, withAlpha(base, opacity), thickness);
                if (fill.get()) {
                    Render3DUtils.drawFilledBox(box, withAlpha(base, opacity * 0.18F));
                }
            }
        }
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        if (noPlayer() || tags.isEmpty()) {
            return;
        }
        Canvas canvas = event.canvas();
        float baseScale = tagScale.get().floatValue();
        float falloff = tagFalloff.get().floatValue();
        boolean blur = tagBlur.get();
        int plateColor = blur ? TAG_PLATE_BLUR : TAG_PLATE;

        for (DefenseTag tag : tags) {
            if (!isBedVisible(tag.own())) {
                continue;
            }
            // 来源 把标签锚在 (x + 0.5, y + 1.4, z + 0.5) 并随距离缩小（Tag falloff）。
            Vector3f screen = WorldToScreen.getWorldPositionToScreen(new Vec3(
                    tag.bedPos().getX() + 0.5D,
                    tag.bedPos().getY() + 1.4D,
                    tag.bedPos().getZ() + 0.5D));
            if (screen == null || screen.z < 0.0F || screen.z > 1.0F) {
                continue;
            }
            double distance = Math.sqrt(mc.player.distanceToSqr(
                    tag.bedPos().getX() + 0.5D,
                    tag.bedPos().getY() + 0.5D,
                    tag.bedPos().getZ() + 0.5D));
            float shrink = falloff <= 0.0F
                    ? 1.0F
                    : Mth.clamp(1.0F - falloff * (float) (distance / 48.0D), 0.35F, 1.0F);
            float scale = baseScale * shrink;
            if (scale <= 0.05F) {
                continue;
            }

            float lineHeight = 10.0F * scale;
            float fontSize = 8.0F * scale;
            float padding = 3.0F * scale;
            float width = 0.0F;
            for (String row : tag.rows()) {
                width = Math.max(width, SkijaUi.textWidth(row, fontSize));
            }
            float boxWidth = width + padding * 2.0F;
            float boxHeight = tag.rows().size() * lineHeight + padding * 2.0F;
            float left = screen.x - boxWidth / 2.0F;
            float top = screen.y - boxHeight - 4.0F * scale;

            SkijaUi.rounded(canvas, left, top, boxWidth, boxHeight, 2.0F * scale, plateColor);
            float textTop = top + padding;
            for (String row : tag.rows()) {
                SkijaUi.textShadow(canvas, row, left + padding, textTop, lineHeight, UiTheme.text(), fontSize);
                textTop += lineHeight;
            }
        }
    }

    /** 来源的 l()：扫描床方块（原实现遍历已加载区块，这里扫玩家周围的方块盒）。 */
    private void rescan() {
        beds.clear();
        defenseBlocks.clear();
        tags.clear();
        if (mc.level == null || mc.player == null) {
            return;
        }

        BlockPos origin = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-SCAN_RADIUS, -SCAN_DOWN, -SCAN_RADIUS),
                origin.offset(SCAN_RADIUS, SCAN_UP, SCAN_RADIUS))) {
            BlockState state = mc.level.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock)) {
                continue;
            }
            BlockPos foot = pos.relative(BedBlock.getConnectedDirection(state));
            // 来源 只收 HEAD 那一半；这里用“另一半在前”的规范判定达到同样效果。
            if (!isCanonicalHalf(pos, foot)) {
                continue;
            }
            beds.add(pos.immutable());
        }

        if ((defense.get() || defenseTags.get()) && !beds.isEmpty()) {
            buildDefense();
        }
    }

    /** 来源的 h()：统计每张床周围围着的方块，并记下暴露在外的那些。 */
    private void buildDefense() {
        if (mc.level == null) {
            return;
        }
        int radius = Mth.ceil(defenseRadius.get());
        for (BlockPos bedPos : beds) {
            BlockState bedState = mc.level.getBlockState(bedPos);
            if (!(bedState.getBlock() instanceof BedBlock)) {
                continue;
            }
            BlockPos foot = bedPos.relative(BedBlock.getConnectedDirection(bedState));
            boolean own = isOwnBed(bedPos, foot);

            int minX = Math.min(bedPos.getX(), foot.getX()) - radius;
            int maxX = Math.max(bedPos.getX(), foot.getX()) + radius;
            int minY = Math.min(bedPos.getY(), foot.getY());
            int maxY = Math.max(bedPos.getY(), foot.getY()) + radius;
            int minZ = Math.min(bedPos.getZ(), foot.getZ()) - radius;
            int maxZ = Math.max(bedPos.getZ(), foot.getZ()) + radius;

            Map<String, Integer> counts = new LinkedHashMap<>();
            int total = 0;
            for (BlockPos cursor : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
                BlockState state = mc.level.getBlockState(cursor);
                if (state.isAir() || state.getBlock() instanceof BedBlock) {
                    continue;
                }
                if (!BlockUtils.isSolidBlock(cursor)) {
                    continue;
                }
                String name = state.getBlock().getName().getString();
                counts.merge(name, 1, Integer::sum);
                total++;
                if (isExposed(cursor)) {
                    defenseBlocks.add(new DefenseBlock(cursor.immutable(), defenseColor(state)));
                }
            }
            if (defenseTags.get() && total > 0) {
                tags.add(new DefenseTag(bedPos.immutable(), own, tagRows(counts)));
            }
        }
    }

    /** 来源的 BedESP.L(BlockPos)：四个方向里只要有一侧是空的就算暴露。 */
    private boolean isExposed(BlockPos pos) {
        if (mc.level == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            if (mc.level.getBlockState(neighbour).isAir() || !BlockUtils.isSolidBlock(neighbour)) {
                return true;
            }
        }
        return false;
    }

    /** 来源的 BedESP.L(IBlockState)：黑曜石 / 玻璃 / 其它。 */
    private static int defenseColor(BlockState state) {
        if (state.getBlock() == Blocks.OBSIDIAN) {
            return DEFENSE_OBSIDIAN;
        }
        if (state.getBlock() == Blocks.GLASS) {
            return DEFENSE_GLASS;
        }
        return DEFENSE_OTHER;
    }

    /** 来源的标签列表：按方块种类计数，数量多的排前面。 */
    private static List<String> tagRows(Map<String, Integer> counts) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            if (rows.size() >= TAG_ROWS) {
                break;
            }
            rows.add(entry.getKey() + " x" + entry.getValue());
        }
        return rows;
    }

    /** 来源的 L(boolean)：己方光束色 / 统一光束色 / 床色。 */
    private Color beamColor(boolean own) {
        if (own && ownBeamColor.get()) {
            return ownBeamTint.get();
        }
        if (beamColor.get()) {
            return beamTint.get();
        }
        return own ? ownColor.get() : enemyColor.get();
    }

    /** 来源的 L(float)：床顶起、向上 Beam height、半径 Beam width，外层弱 + 内层亮的两层光柱。 */
    private void drawBeam(AABB bed, boolean own) {
        double baseY = bed.maxY;
        double height = beamHeight.get();
        double half = beamWidth.get();
        double centerX = (bed.minX + bed.maxX) / 2.0D;
        double centerZ = (bed.minZ + bed.maxZ) / 2.0D;
        Color base = beamColor(own);
        float alpha = (base.getAlpha() / 255.0F) * (beamOpacity.get().floatValue() / 100.0F);

        AABB outer = new AABB(
                centerX - half, baseY, centerZ - half,
                centerX + half, baseY + height, centerZ + half);
        AABB inner = new AABB(
                centerX - half * 0.35D, baseY, centerZ - half * 0.35D,
                centerX + half * 0.35D, baseY + height, centerZ + half * 0.35D);

        // 来源：外柱底 alpha = 0.55 * alpha，内柱底 alpha = alpha，顶端都渐变到 0。
        Render3DUtils.drawFilledFadeBox(outer, withAlphaArgb(base, alpha * 0.55F), withAlphaArgb(base, 0.0F));
        Render3DUtils.drawFilledFadeBox(inner, withAlphaArgb(base, alpha), withAlphaArgb(base, 0.0F));
    }

    /** 来源的 L(BlockPos)：两半拼起来的床盒（高 0.5625，外扩 0.003）。 */
    private static AABB bedBox(BlockPos bedPos, BlockPos foot) {
        double minX = Math.min(bedPos.getX(), foot.getX()) - EXPAND;
        double minY = Math.min(bedPos.getY(), foot.getY()) - EXPAND;
        double minZ = Math.min(bedPos.getZ(), foot.getZ()) - EXPAND;
        double maxX = Math.max(bedPos.getX(), foot.getX()) + 1.0D + EXPAND;
        double maxY = Math.max(bedPos.getY(), foot.getY()) + BED_TOP + EXPAND;
        double maxZ = Math.max(bedPos.getZ(), foot.getZ()) + 1.0D + EXPAND;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** 只保留两半床里靠“前”的那一半，避免同一张床画两遍。 */
    private static boolean isCanonicalHalf(BlockPos pos, BlockPos foot) {
        if (foot.getY() != pos.getY()) {
            return foot.getY() < pos.getY();
        }
        if (foot.getX() != pos.getX()) {
            return foot.getX() < pos.getX();
        }
        return foot.getZ() < pos.getZ();
    }

    private boolean isBedVisible(boolean own) {
        return own ? ownBed.get() : enemyBeds.get();
    }

    /**
     * 说明：来源的 BedTracker 知道哪张是“自己的床”，Dioxide 的 BedTracker 只暴露
     * 最近的一张床，这里用它做等价近似。
     */
    private static boolean isOwnBed(BlockPos bedPos, BlockPos foot) {
        BlockPos tracked = BedTracker.INSTANCE.trackedBed();
        if (tracked == null) {
            return false;
        }
        return tracked.equals(bedPos) || tracked.equals(foot);
    }

    /** 来源的 ThemeSupport_064.L(color, factor)：按系数缩放 alpha。 */
    private static Color withAlpha(Color color, float factor) {
        int alpha = Mth.clamp(Math.round(color.getAlpha() * factor), 0, 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static int withAlphaArgb(Color color, float factor) {
        int alpha = Mth.clamp(Math.round(color.getAlpha() * factor), 0, 255);
        return (alpha << 24)
                | (color.getRed() << 16)
                | (color.getGreen() << 8)
                | color.getBlue();
    }
}
