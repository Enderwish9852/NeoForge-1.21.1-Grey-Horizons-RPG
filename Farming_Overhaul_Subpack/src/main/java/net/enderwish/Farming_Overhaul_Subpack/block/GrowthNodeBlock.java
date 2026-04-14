package net.enderwish.Farming_Overhaul_Subpack.block;

import com.mojang.serialization.MapCodec;
import net.enderwish.Farming_Overhaul_Subpack.block.entity.GrowthNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GrowthNodeBlock extends BaseEntityBlock {
    // This is the missing piece the error is asking for
    public static final MapCodec<GrowthNodeBlock> CODEC = simpleCodec(GrowthNodeBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public GrowthNodeBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrowthNodeBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}