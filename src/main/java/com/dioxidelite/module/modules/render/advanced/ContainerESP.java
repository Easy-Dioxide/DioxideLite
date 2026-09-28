package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.util.render.Render3DUtils;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 移植自 来源客户端 {@code features/render/ContainerESP.java}：高亮周围的箱子/末影箱/
 * 漏斗/熔炉等容器方块，盒子外扩 0.003，描边 alpha 0.95、填充 alpha 0.14。
 * 全部几何都在世界空间 3D 层（{@link Render3DEvent}）绘制。
 * <p>
 * 1:1 对应：Chests(true, 0xFFB327)、Ender chests(false, 0x9456FF)、Hoppers(false, 0x777A82)、
 * Furnaces(false, 0xD0D0D0)、Fill(true, alpha 0.14)、Through walls(true)、Line width(1.5 / 0.5~5)。
 * 颜色来自反编译常量 -19673 / -7055617 / -8947070 / -3092272，盒体外扩 0.003 与原实现一致。
 * <p>
 * 说明：来源 遍历世界已加载的 TileEntity；本端口按 Dioxide ESP 的做法在玩家周围
 * 非阻塞地扫描已加载区块（{@code getChunk(..., FULL, false)}，最多 8 个区块半径），
 * 并像 ESP 一样做 400ms 缓存重扫。"Through walls" 用两条等价渲染管线实现：
 * 开启时走 Render3DUtils（始终穿透），关闭时走本文件内基于
 * {@code RenderPipelines} 的深度测试管线（写法与项目现有 CircleESP 一致）。
 */
public final class ContainerESP extends Module {

    public static final ContainerESP INSTANCE = new ContainerESP();

    // 来源的四种容器颜色（低 24 位）。
    private static final int CHEST_COLOR = 0xFFB327;
    private static final int ENDER_CHEST_COLOR = 0x9456FF;
    private static final int HOPPER_COLOR = 0x777A82;
    private static final int FURNACE_COLOR = 0xD0D0D0;

    /** 来源的描边/填充浓度：0.95 与 0.14。 */
    private static final int OUTLINE_ALPHA = 242;
    private static final int FILL_ALPHA = 36;

    /** 来源的盒子外扩常量。 */
    private static final double BOX_EXPAND = 0.003D;

    private static final long RESCAN_INTERVAL_MS = 400L;
    private static final int MAX_SCAN_CHUNK_RADIUS = 8;

    /** "Through walls" 关闭时的深度测试填充管线（对照 util/render/esp/CircleESP 的写法）。 */
    private static final RenderPipeline FILL_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("dioxidelite", "pipeline/custom_container_fill_depth"))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    private static final RenderType FILL_DEPTH = RenderType.create("dioxidelite_custom_container_fill_depth",
            RenderSetup.builder(FILL_DEPTH_PIPELINE).createRenderSetup());

    // ------------------------------------------------------------------ 设置（顺序与来源 构造函数一致）

    public final BooleanSetting chests = add(new BooleanSetting("Chests", true));
    public final BooleanSetting enderChests = add(new BooleanSetting("Ender Chests", false));
    public final BooleanSetting hoppers = add(new BooleanSetting("Hoppers", false));
    public final BooleanSetting furnaces = add(new BooleanSetting("Furnaces", false));
    public final BooleanSetting fill = add(new BooleanSetting("Fill", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", true));
    public final DoubleSetting lineWidth = add(new DoubleSetting("Line Width", 1.5, 0.5, 5.0, 0.5));

    // ------------------------------------------------------------------ 状态

    private final List<ContainerBox> containers = new ArrayList<>();
    private ClientLevel trackedLevel;
    private ChunkPos lastChunk;
    private long lastScanTime;

    private ContainerESP() {
        super("ContainerESP", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        containers.clear();
        trackedLevel = null;
        lastChunk = null;
        lastScanTime = 0L;
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }

        if (trackedLevel != mc.level) {
            trackedLevel = mc.level;
            containers.clear();
            lastChunk = null;
            lastScanTime = 0L;
        }

        ChunkPos playerChunk = mc.player.chunkPosition();
        long now = System.currentTimeMillis();
        if (lastChunk == null || !lastChunk.equals(playerChunk) || now - lastScanTime >= RESCAN_INTERVAL_MS) {
            rebuildCache(playerChunk);
            lastChunk = playerChunk;
            lastScanTime = now;
        }
        if (containers.isEmpty()) {
            return;
        }

        PoseStack stack = event.getPoseStack();
        float thickness = lineWidth.get().floatValue();
        boolean fillEnabled = fill.get();
        boolean depthTested = !throughWalls.get();

        for (ContainerBox container : containers) {
            if (fillEnabled) {
                renderFill(stack, container.box(), argb(container.color(), FILL_ALPHA), depthTested);
            }
            renderOutline(stack, container.box(), argb(container.color(), OUTLINE_ALPHA), thickness, depthTested);
        }
    }

    /** 来源 {@code ContainerESP.L(TileEntity)} 的类型 -> 颜色判定。 */
    private int colorFor(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity && chests.get()) {
            return CHEST_COLOR;
        }
        if (blockEntity instanceof EnderChestBlockEntity && enderChests.get()) {
            return ENDER_CHEST_COLOR;
        }
        if (blockEntity instanceof HopperBlockEntity && hoppers.get()) {
            return HOPPER_COLOR;
        }
        if (blockEntity instanceof AbstractFurnaceBlockEntity && furnaces.get()) {
            return FURNACE_COLOR;
        }
        return 0;
    }

    private void rebuildCache(ChunkPos playerChunk) {
        containers.clear();
        if (mc.level == null) {
            return;
        }

        int chunkRadius = Math.min(mc.options.getEffectiveRenderDistance(), MAX_SCAN_CHUNK_RADIUS);
        int chunkRadiusSq = chunkRadius * chunkRadius;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (dx * dx + dz * dz > chunkRadiusSq) {
                    continue;
                }
                ChunkAccess chunk = mc.level.getChunk(playerChunk.x() + dx, playerChunk.z() + dz,
                        ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk levelChunk)) {
                    continue;
                }
                for (BlockEntity blockEntity : levelChunk.getBlockEntities().values()) {
                    int color = colorFor(blockEntity);
                    if (color == 0) {
                        continue;
                    }
                    containers.add(new ContainerBox(new AABB(blockEntity.getBlockPos()).inflate(BOX_EXPAND), color));
                }
            }
        }
    }

    // ------------------------------------------------------------------ 绘制

    private void renderFill(PoseStack stack, AABB box, int color, boolean depthTested) {
        if (!depthTested) {
            Render3DUtils.drawFilledBox(box, color);
            return;
        }

        Vec3 camera = mc.getEntityRenderDispatcher().camera.position();
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        Matrix4f matrix = stack.last().pose();

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        quad(buffer, matrix, color, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ); // 下
        quad(buffer, matrix, color, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ); // 上
        quad(buffer, matrix, color, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ); // 北
        quad(buffer, matrix, color, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ); // 南
        quad(buffer, matrix, color, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ); // 西
        quad(buffer, matrix, color, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ); // 东
        FILL_DEPTH.draw(buffer.buildOrThrow());
    }

    private void renderOutline(PoseStack stack, AABB box, int color, float thickness, boolean depthTested) {
        if (!depthTested) {
            Render3DUtils.drawOutlineBox(stack, box, color, thickness);
            return;
        }

        Vec3 camera = mc.getEntityRenderDispatcher().camera.position();
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        PoseStack.Pose entry = stack.last();
        Matrix4f matrix = entry.pose();

        // 描边也不再走 GL_LINES（该管线在本版本上渲染异常）：用细四边形沿着 12 条棱拼出来。
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float half = Math.max(0.004F, thickness * 0.006F);
        edgeQuad(buffer, matrix, color, minX, minY, minZ, maxX, minY, minZ, half);
        edgeQuad(buffer, matrix, color, maxX, minY, minZ, maxX, minY, maxZ, half);
        edgeQuad(buffer, matrix, color, maxX, minY, maxZ, minX, minY, maxZ, half);
        edgeQuad(buffer, matrix, color, minX, minY, maxZ, minX, minY, minZ, half);
        edgeQuad(buffer, matrix, color, minX, maxY, minZ, maxX, maxY, minZ, half);
        edgeQuad(buffer, matrix, color, maxX, maxY, minZ, maxX, maxY, maxZ, half);
        edgeQuad(buffer, matrix, color, maxX, maxY, maxZ, minX, maxY, maxZ, half);
        edgeQuad(buffer, matrix, color, minX, maxY, maxZ, minX, maxY, minZ, half);
        edgeQuad(buffer, matrix, color, minX, minY, minZ, minX, maxY, minZ, half);
        edgeQuad(buffer, matrix, color, maxX, minY, minZ, maxX, maxY, minZ, half);
        edgeQuad(buffer, matrix, color, maxX, minY, maxZ, maxX, maxY, maxZ, half);
        edgeQuad(buffer, matrix, color, minX, minY, maxZ, minX, maxY, maxZ, half);
        FILL_DEPTH.draw(buffer.buildOrThrow());
    }

    /** 一条棱：沿两个互相正交的垂直方向各铺一个薄四边形（十字截面），保证任何角度看都有描边。 */
    private static void edgeQuad(BufferBuilder buffer, Matrix4f matrix, int color,
                                 float x1, float y1, float z1, float x2, float y2, float z2, float half) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float ax = 0.0F, ay = 0.0F, az = 0.0F;
        if (Math.abs(dx) <= Math.abs(dy) && Math.abs(dx) <= Math.abs(dz)) {
            ax = 1.0F;
        } else if (Math.abs(dy) <= Math.abs(dz)) {
            ay = 1.0F;
        } else {
            az = 1.0F;
        }
        float bx = dy * az - dz * ay;
        float by = dz * ax - dx * az;
        float bz = dx * ay - dy * ax;
        float bl = Mth.sqrt(bx * bx + by * by + bz * bz);
        if (bl < 1.0E-5F) {
            return;
        }
        bx /= bl;
        by /= bl;
        bz /= bl;
        quad(buffer, matrix, color, x1 + ax * half, y1 + ay * half, z1 + az * half,
                x2 + ax * half, y2 + ay * half, z2 + az * half,
                x2 - ax * half, y2 - ay * half, z2 - az * half,
                x1 - ax * half, y1 - ay * half, z1 - az * half);
        quad(buffer, matrix, color, x1 + bx * half, y1 + by * half, z1 + bz * half,
                x2 + bx * half, y2 + by * half, z2 + bz * half,
                x2 - bx * half, y2 - by * half, z2 - bz * half,
                x1 - bx * half, y1 - by * half, z1 - bz * half);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, int color,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(color);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color);
        buffer.addVertex(matrix, x3, y3, z3).setColor(color);
        buffer.addVertex(matrix, x4, y4, z4).setColor(color);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, PoseStack.Pose entry,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             int color, float thickness) {
        Vector3f normal = normal(x1, y1, z1, x2, y2, z2);
        buffer.addVertex(matrix, x1, y1, z1).setColor(color)
                .setNormal(entry, normal.x, normal.y, normal.z).setLineWidth(thickness);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color)
                .setNormal(entry, normal.x, normal.y, normal.z).setLineWidth(thickness);
    }

    private static Vector3f normal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float x = x2 - x1;
        float y = y2 - y1;
        float z = z2 - z1;
        float length = Mth.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-6f) {
            return new Vector3f(0.0f, 1.0f, 0.0f);
        }
        return new Vector3f(x / length, y / length, z / length);
    }

    private static int argb(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** 缓存下来的一个容器方块盒（屏幕无关，世界坐标）。 */
    private record ContainerBox(AABB box, int color) {
    }
}
