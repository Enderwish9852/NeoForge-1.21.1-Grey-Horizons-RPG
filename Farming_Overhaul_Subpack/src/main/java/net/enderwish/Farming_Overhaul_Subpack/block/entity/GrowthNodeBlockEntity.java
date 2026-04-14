package net.enderwish.Farming_Overhaul_Subpack.block.entity;

import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.enderwish.Farming_Overhaul_Subpack.util.BranchingMathCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GrowthNodeBlockEntity extends BlockEntity {
    private int daysGrown = 0;

    public GrowthNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROWTH_NODE.get(), pos, state);
    }

    /**
     * Called by SeasonGrowthListener when the HUD pack signals a season change.
     */
    public void advanceGrowth() {
        this.daysGrown += 20;

        if (this.daysGrown >= 80) {
            if (this.level instanceof ServerLevel serverLevel) {
                // 1. Remove the sapling sitting on top of the node
                BlockPos saplingPos = this.worldPosition.above();
                serverLevel.setBlockAndUpdate(saplingPos, Blocks.AIR.defaultBlockState());

                // 2. Trigger the procedural branching math
                BranchingMathCore.growTree(serverLevel, this.worldPosition, ModBlocks.OAK_BRANCH.get());

                // 3. Transform the node back into regular dirt so the process finishes
                serverLevel.setBlockAndUpdate(this.worldPosition, Blocks.DIRT.defaultBlockState());
            }
        }

        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("DaysGrown", this.daysGrown);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.daysGrown = tag.getInt("DaysGrown");
    }
}