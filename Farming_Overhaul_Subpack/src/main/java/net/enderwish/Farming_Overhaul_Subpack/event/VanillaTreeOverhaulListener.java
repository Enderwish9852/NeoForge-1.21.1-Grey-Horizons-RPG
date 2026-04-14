package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.util.BranchingMathCore;
import net.enderwish.Farming_Overhaul_Subpack.world.tree.TreeDNA;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;

@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID)
public class VanillaTreeOverhaulListener {

    @SubscribeEvent
    public static void onSaplingGrow(BlockGrowFeatureEvent event) {
        // 1. Ensure we are on the server
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        TreeDNA dna = null;

        // 2. Determine which DNA to use based on the sapling block
        if (state.is(Blocks.OAK_SAPLING)) {
            dna = TreeDNA.OAK;
        } else if (state.is(Blocks.BIRCH_SAPLING)) {
            dna = TreeDNA.BIRCH;
        } else if (state.is(Blocks.SPRUCE_SAPLING)) {
            dna = TreeDNA.SPRUCE;
        }

        // 3. Hijack
        if (dna != null) {
            // Cancel the vanilla tree feature from placing
            event.setCanceled(true);

            // Delete the sapling block immediately
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

            // Run our custom branching math
            BranchingMathCore.growTree(level, pos, dna.branchBlock());
        }
    }
}