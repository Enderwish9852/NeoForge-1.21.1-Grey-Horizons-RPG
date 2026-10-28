package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLeavesBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLogBlock;
import net.enderwish.Farming_Overhaul_Subpack.entity.FallingTreeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * TreeFallHandler
 *
 * Listens for block break events and, when a tree log is cut, converts the
 * entire connected tree (logs + leaves) into a falling entity.
 *
 * Only active when FrontierConfig.TREES_FALL is true. When false, vanilla
 * behavior (log drops as item) is preserved.
 */
@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TreeFallHandler {

    private TreeFallHandler() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!net.enderwish.Frontier_Subpack.FrontierConfig.TREES_FALL.get()) {
            return;
        }

        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof Level level)) {
            return;
        }

        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Only process tree log blocks
        if (!(state.getBlock() instanceof FruitTreeLogBlock)) {
            return;
        }

        // Don't process if it's already been removed
        if (state.isAir()) return;

        // Find all connected tree blocks (logs + leaves) starting from this block
        List<BlockPosAndState> treeBlocks = findConnectedTree(level, pos, state);
        if (treeBlocks.isEmpty()) return;

        // Remove all tree blocks from the world
        for (BlockPosAndState bps : treeBlocks) {
            level.removeBlock(bps.pos(), false);
        }

        // Create falling tree entity at the cut position
        FallingTreeEntity entity = new FallingTreeEntity(
                net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack.FALLING_TREE.get(), level);
        entity.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        // Add all blocks to the entity
        for (BlockPosAndState bps : treeBlocks) {
            entity.addBlock(bps.pos(), bps.state());
        }

        level.addFreshEntity(entity);
    }

    /**
     * Flood-fill from the broken block to find all connected tree blocks.
     * Only same-log-type blocks and adjacent leaves are included.
     */
    private static List<BlockPosAndState> findConnectedTree(Level level, BlockPos start, BlockState startState) {
        List<BlockPosAndState> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);

            // Only process if it's a tree log or leaf
            if (!(currentState.getBlock() instanceof FruitTreeLogBlock)
                    && !(currentState.getBlock() instanceof FruitTreeLeavesBlock)) {
                continue;
            }

            result.add(new BlockPosAndState(current, currentState));

            // Check all 6 neighbors
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = new BlockPos(
                        current.getX() + (dir.getAxis() == Direction.Axis.X ? (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1) : 0),
                        current.getY() + (dir.getAxis() == Direction.Axis.Y ? (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1) : 0),
                        current.getZ() + (dir.getAxis() == Direction.Axis.Z ? (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1) : 0)
                );
                if (visited.contains(neighbor)) continue;

                BlockState neighborState = level.getBlockState(neighbor);
                Block neighborBlock = neighborState.getBlock();

                // Include connected logs (same block type)
                if (neighborBlock instanceof FruitTreeLogBlock) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
                // Include leaves adjacent to any tree block
                else if (neighborBlock instanceof FruitTreeLeavesBlock) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
            }
        }

        return result;
    }

    /** Record of a block position and its state in the falling tree. */
    public record BlockPosAndState(BlockPos pos, BlockState state) {}
}
