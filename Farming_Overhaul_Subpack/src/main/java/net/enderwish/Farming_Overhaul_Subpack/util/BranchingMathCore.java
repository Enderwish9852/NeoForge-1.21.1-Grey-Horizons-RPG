package net.enderwish.Farming_Overhaul_Subpack.util;

import net.enderwish.Farming_Overhaul_Subpack.block.AbstractBranchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BranchingMathCore {

    /**
     * Entry point for growing the tree.
     */
    public static void growTree(LevelAccessor level, BlockPos pos, Block branchBlock) {
        RandomSource random = level.getRandom();
        // Initial growth: Start at depth 5 (thicker base)
        executeBranch(level, pos, Direction.UP, 5, branchBlock, random);
    }

    private static void executeBranch(LevelAccessor level, BlockPos pos, Direction direction, int depth, Block branchBlock, RandomSource random) {
        // TERMINATION: If depth is 0, place leaves and stop.
        if (depth <= 0) {
            placeLeaves(level, pos);
            return;
        }

        // Segment length: 2 to 3 blocks per "joint"
        int segmentLength = 2 + random.nextInt(2);
        BlockPos currentPos = pos;

        for (int i = 0; i < segmentLength; i++) {
            currentPos = currentPos.relative(direction);

            if (!level.hasChunkAt(currentPos)) return;

            BlockState currentState = level.getBlockState(currentPos);
            // Only place if it's air or replaceable (like grass)
            if (currentState.isAir() || currentState.canBeReplaced()) {

                // THICKNESS: Calculated based on recursion depth
                int thickness = Math.max(1, depth);

                // This line is now fixed because 'Block' is properly imported
                BlockState branchState = branchBlock.defaultBlockState()
                        .setValue(AbstractBranchBlock.THICKNESS, thickness)
                        .setValue(AbstractBranchBlock.FACING, direction);

                level.setBlock(currentPos, branchState, 3);
            } else {
                return;
            }
        }

        // DECISION: Should we split or continue?
        float splitChance = 0.4f;
        if (random.nextFloat() < splitChance) {
            executeBranch(level, currentPos, getRandomSideDirection(random, direction), depth - 1, branchBlock, random);
            executeBranch(level, currentPos, getRandomSideDirection(random, direction), depth - 1, branchBlock, random);
        } else {
            Direction nextDir = random.nextFloat() < 0.2f ? getRandomSideDirection(random, direction) : direction;
            executeBranch(level, currentPos, nextDir, depth - 1, branchBlock, random);
        }
    }

    private static void placeLeaves(LevelAccessor level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos leafPos = pos.offset(x, y, z);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(leafPos, Blocks.OAK_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static Direction getRandomSideDirection(RandomSource random, Direction current) {
        Direction dir = Direction.getRandom(random);
        // Avoid growing down or growing exactly backwards
        if (dir == Direction.DOWN || dir == current.getOpposite()) {
            return Direction.UP;
        }
        return dir;
    }
}