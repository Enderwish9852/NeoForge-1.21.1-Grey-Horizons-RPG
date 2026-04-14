package net.enderwish.Farming_Overhaul_Subpack.world.tree.feature;

import com.mojang.serialization.Codec;
import net.enderwish.Farming_Overhaul_Subpack.util.BranchingMathCore;
import net.enderwish.Farming_Overhaul_Subpack.world.tree.TreeDNA;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BranchingTreeFeature extends Feature<NoneFeatureConfiguration> {
    private final TreeDNA dna;

    public BranchingTreeFeature(Codec<NoneFeatureConfiguration> codec, TreeDNA dna) {
        super(codec);
        this.dna = dna;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos pos = context.origin();
        // Check if there is space and dirt below
        if (context.level().getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.DIRT)) {
            BranchingMathCore.growTree(context.level(), pos, dna.branchBlock());
            return true;
        }
        return false;
    }
}
