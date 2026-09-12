/*
 * This file is part of DioxideLiteVia - https://github.com/ViaVersion/DioxideLiteVia
 * Copyright (C) 2021-2026 the original authors
 *                         - Florian Reuth <git@florianreuth.de>
 *                         - RK_01/RaphiMC
 * Copyright (C) 2023-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.viaversion.setsunavia.injection.mixin.features.block.shape;

import com.viaversion.setsunavia.injection.DioxideLiteViaMixinPlugin;
import com.viaversion.setsunavia.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilBlock.class)
public abstract class MixinAnvilBlock extends FallingBlock {

    @Unique
    private static final VoxelShape DioxideLiteVia$x_axis_shape_r1_12_2 = Block.box(0.0D, 0.0D, 2.0D, 16.0D, 16.0D, 14.0D);

    @Unique
    private static final VoxelShape DioxideLiteVia$z_axis_shape_r1_12_2 = Block.box(2.0D, 0.0D, 0.0D, 14.0D, 16.0D, 16.0D);

    @Shadow
    @Final
    public static EnumProperty<Direction> FACING;

    public MixinAnvilBlock(Properties settings) {
        super(settings);
    }

    @Unique
    private boolean DioxideLiteVia$requireOriginalShape;

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void changeOutlineShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (DioxideLiteViaMixinPlugin.MORE_CULLING_PRESENT && DioxideLiteVia$requireOriginalShape) {
            DioxideLiteVia$requireOriginalShape = false;
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            cir.setReturnValue(state.getValue(FACING).getAxis() == Direction.Axis.X ? DioxideLiteVia$x_axis_shape_r1_12_2 : DioxideLiteVia$z_axis_shape_r1_12_2);
        }
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        // Workaround for https://github.com/ViaVersion/DioxideLiteVia/issues/246
        // MoreCulling is caching the culling shape and doesn't reload it, so we have to force vanilla's shape here.
        DioxideLiteVia$requireOriginalShape = true;
        return super.getOcclusionShape(state);
    }

}
