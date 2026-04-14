package net.enderwish.Farming_Overhaul_Subpack.world.tree;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * TreeDNA defines the "biological rules" for how a specific species grows.
 * This record is used by the BranchingMathCore to determine the shape of the tree.
 */
public record TreeDNA(
        Block logBlock,          // The main base log
        Block branchBlock,       // The custom block that supports thickness
        Block leafBlock,         // The leaves to place at the ends
        int maxDepth,            // How many times the tree can branch out
        float branchProbability, // Chance (0.0 to 1.0) of a branch splitting
        float minAngle,          // Minimum spread angle
        float maxAngle,          // Maximum spread angle
        int averageLogLength     // How many blocks to grow before attempting a split
) {

    // --- VANILLA UPGRADE PRESETS ---

    public static final TreeDNA OAK = new TreeDNA(
            Blocks.OAK_LOG,
            Blocks.OAK_LOG,      // Placeholder: Swap to ModBlocks.OAK_BRANCH later
            Blocks.OAK_LEAVES,
            5,                   // Depth
            0.45f,               // 45% chance to split
            30.0f,               // Min Angle
            60.0f,               // Max Angle
            3                    // Avg Length
    );

    public static final TreeDNA BIRCH = new TreeDNA(
            Blocks.BIRCH_LOG,
            Blocks.BIRCH_LOG,    // Placeholder: Swap to ModBlocks.BIRCH_BRANCH later
            Blocks.BIRCH_LEAVES,
            4,                   // Depth
            0.15f,               // 15% chance to split (Birch stays thin)
            10.0f,               // Min Angle
            25.0f,               // Max Angle
            5                    // Avg Length
    );

    public static final TreeDNA SPRUCE = new TreeDNA(
            Blocks.SPRUCE_LOG,
            Blocks.SPRUCE_LOG,    // Placeholder: Swap to ModBlocks.SPRUCE_BRANCH later
            Blocks.SPRUCE_LEAVES,
            6,                   // Depth
            0.80f,               // 80% chance to split (Spruce is very bushy)
            45.0f,               // Min Angle
            90.0f,               // Max Angle
            1                    // Avg Length
    );
}
