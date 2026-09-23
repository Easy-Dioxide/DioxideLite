package com.dioxidelite.script;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/script/LuaWorldApi.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.DioxideLite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Read-only block, entity, dimension and time queries exposed to Lua. */
final class LuaWorldApi {

    private static final int MAX_SEARCH_RADIUS = 16;
    private static final int MAX_RESULTS = 512;

    private LuaWorldApi() {
    }

    static LuaTable create() {
        LuaTable world = new LuaTable();
        world.set("block", LuaApiSupport.method(world, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            BlockPos pos = position(args.arg(first), args.arg(first + 1), args.arg(first + 2));
            BlockState state = state(pos);
            return state == null ? LuaValue.NIL : block(pos, state);
        }));
        world.set("block_id", LuaApiSupport.method(world, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            BlockState state = state(position(args.arg(first), args.arg(first + 1), args.arg(first + 2)));
            return state == null ? LuaValue.NIL : LuaValue.valueOf(blockId(state));
        }));
        world.set("is_air", LuaApiSupport.method(world, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            BlockState state = state(position(args.arg(first), args.arg(first + 1), args.arg(first + 2)));
            return state == null ? LuaValue.NIL : LuaValue.valueOf(state.isAir());
        }));
        world.set("entity", LuaApiSupport.method(world, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            return LuaEntityApi.snapshot(LuaEntityApi.find(args.arg(first).checkint()));
        }));
        world.set("entities", LuaApiSupport.method(world, (args, first) -> {
            LuaExecutionGuard.consumeExpensiveQuery();
            double radius = LuaApiSupport.boundedDouble(args.arg(first), "radius", 0.0, 256.0);
            String filter = args.arg(first + 1).optjstring("all");
            int limit = boundedLimit(args.arg(first + 2).optint(128));
            return entities(radius, filter, limit);
        }));
        world.set("find_blocks", LuaApiSupport.method(world, (args, first) -> {
            LuaExecutionGuard.consumeExpensiveQuery();
            Set<String> ids = blockIds(args.arg(first));
            int radius = Math.max(0, Math.min(MAX_SEARCH_RADIUS, args.arg(first + 1).optint(8)));
            int limit = boundedLimit(args.arg(first + 2).optint(128));
            return findBlocks(ids, radius, limit);
        }));
        world.set("dimension", LuaApiSupport.method(world, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            if (DioxideLite.mc().level == null) return LuaValue.NIL;
            return LuaValue.valueOf(DioxideLite.mc().level.dimension().identifier().toString());
        }));
        world.set("time", LuaApiSupport.method(world, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            return DioxideLite.mc().level == null ? LuaValue.NIL
                    : LuaValue.valueOf(DioxideLite.mc().level.getOverworldClockTime());
        }));
        return world;
    }

    private static LuaTable entities(double radius, String filter, int limit) {
        LuaTable result = new LuaTable();
        if (DioxideLite.mc().level == null || DioxideLite.mc().player == null) return result;
        double radiusSquared = radius * radius;
        int index = 1;
        for (Entity entity : DioxideLite.mc().level.entitiesForRendering()) {
            if (entity == DioxideLite.mc().player || !LuaEntityApi.matches(entity, filter)
                    || DioxideLite.mc().player.distanceToSqr(entity) > radiusSquared) {
                continue;
            }
            result.set(index++, LuaEntityApi.snapshot(entity));
            if (index > limit) break;
        }
        return result;
    }

    private static LuaTable findBlocks(Set<String> ids, int radius, int limit) {
        LuaTable result = new LuaTable();
        if (DioxideLite.mc().level == null || DioxideLite.mc().player == null) return result;
        BlockPos center = DioxideLite.mc().player.blockPosition();
        int index = 1;
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = state(pos);
                    if (state == null || !ids.contains(blockId(state))) continue;
                    result.set(index++, block(pos, state));
                    if (index > limit) return result;
                }
            }
        }
        return result;
    }

    private static BlockState state(BlockPos pos) {
        if (DioxideLite.mc().level == null || !DioxideLite.mc().level.hasChunkAt(pos)) return null;
        return DioxideLite.mc().level.getBlockState(pos);
    }

    private static LuaTable block(BlockPos pos, BlockState state) {
        LuaTable result = new LuaTable();
        result.set("x", pos.getX());
        result.set("y", pos.getY());
        result.set("z", pos.getZ());
        result.set("id", blockId(state));
        result.set("air", LuaValue.valueOf(state.isAir()));
        result.set("solid", LuaValue.valueOf(state.isSolidRender()));
        result.set("replaceable", LuaValue.valueOf(state.canBeReplaced()));
        result.set("fluid", LuaValue.valueOf(!state.getFluidState().isEmpty()));
        return result;
    }

    private static String blockId(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id == null ? "unknown" : id.toString().toLowerCase(Locale.ROOT);
    }

    private static Set<String> blockIds(LuaValue value) {
        Set<String> ids = new HashSet<>();
        if (value.istable()) {
            LuaTable table = value.checktable();
            for (int index = 1; !table.get(index).isnil(); index++) {
                ids.add(normalizeId(table.get(index).checkjstring()));
                if (ids.size() > 64) throw new LuaError("At most 64 block ids may be searched at once");
            }
        } else {
            ids.add(normalizeId(value.checkjstring()));
        }
        if (ids.isEmpty()) throw new LuaError("At least one block id is required");
        return ids;
    }

    private static String normalizeId(String value) {
        String id = value.trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty() || id.length() > 128 || id.chars().anyMatch(Character::isWhitespace)) {
            throw new LuaError("Invalid block id: " + value);
        }
        return id.indexOf(':') < 0 ? "minecraft:" + id : id;
    }

    private static BlockPos position(LuaValue x, LuaValue y, LuaValue z) {
        int blockX = x.checkint();
        int blockY = y.checkint();
        int blockZ = z.checkint();
        if (Math.abs((long) blockX) > 30_000_000L || Math.abs((long) blockZ) > 30_000_000L
                || blockY < -4096 || blockY > 4096) {
            throw new LuaError("Block position is outside the supported world range");
        }
        return new BlockPos(blockX, blockY, blockZ);
    }

    private static int boundedLimit(int requested) {
        return Math.max(1, Math.min(MAX_RESULTS, requested));
    }
}
