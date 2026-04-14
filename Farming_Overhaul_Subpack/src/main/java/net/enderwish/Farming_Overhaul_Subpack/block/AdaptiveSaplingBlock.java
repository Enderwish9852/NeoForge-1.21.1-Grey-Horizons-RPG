package net.enderwish.Farming_Overhaul_Subpack.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import org.jetbrains.annotations.Nullable;

public class AdaptiveSaplingBlock extends BushBlock {
    // This defines the Codec that 1.21.1 is asking for in your screenshot
    public static final MapCodec<AdaptiveSaplingBlock> CODEC = simpleCodec(AdaptiveSaplingBlock::new);

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    public AdaptiveSaplingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide) {
            BlockPos below = pos.below();
            BlockState stateBelow = level.getBlockState(below);

            // Checks if the block below is dirt/grass to swap it for the Growth Node
            if (stateBelow.is(BlockTags.DIRT)) {
                level.setBlockAndUpdate(below, ModBlocks.GROWTH_NODE.get().defaultBlockState());
            }
        }
    }
}
