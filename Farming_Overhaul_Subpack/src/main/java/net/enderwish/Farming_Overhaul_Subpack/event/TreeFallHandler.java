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
import net.minecraft.world.phys.Vec3;
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
 * connected tree into falling entity/ies.
 *
 * Only active when FrontierConfig.TREES_FALL is true. When false, vanilla
 * behavior (log drops as item) is preserved.
 *
 * Behavior:
 *   - Blocks at/below the cut Y level become a stump (stay in world)
 *   - Logs above the cut + connected leaves become one or more falling entities
 *   - Fall direction = downhill slope + weight distribution (defaults away from +X)
 *   - Branches (side logs) split off as their own falling entities
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

        // ── Stump: leave blocks at/below the cut Y level ─────────────────────
        int cutY = pos.getY();
        List<BlockPosAndState> fallingBlocks = new ArrayList<>();
        for (BlockPosAndState bps : treeBlocks) {
            if (bps.pos().getY() > cutY) {
                fallingBlocks.add(bps);
            }
        }

        // Remove only the blocks that will become the falling entity
        for (BlockPosAndState bps : fallingBlocks) {
            level.removeBlock(bps.pos(), false);
        }

        // ── Split branches into separate falling entities ────────────────────
        // Main trunk = logs connected vertically from the cut block upward
        // Branches = logs connected sideways from the trunk
        List<BlockPosAndState> trunkBlocks = new ArrayList<>();
        List<BlockPosAndState> branchBlocks = new ArrayList<>();
        List<BlockPosAndState> leafBlocks = new ArrayList<>();

        Set<BlockPos> trunkLogPos = new HashSet<>();
        trunkLogPos.add(pos); // the cut block is the trunk base

        // BFS upward from cut block to find trunk logs (vertical connection only)
        Queue<BlockPos> trunkQueue = new ArrayDeque<>();
        trunkQueue.add(pos);
        while (!trunkQueue.isEmpty()) {
            BlockPos current = trunkQueue.poll();
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() != Direction.Axis.Y) continue; // only vertical for trunk
                BlockPos neighbor = current.relative(dir);
                if (trunkLogPos.contains(neighbor)) continue;
                BlockState ns = level.getBlockState(neighbor);
                if (ns.getBlock() instanceof FruitTreeLogBlock) {
                    trunkLogPos.add(neighbor);
                    trunkQueue.add(neighbor);
                }
            }
        }

        for (BlockPosAndState bps : fallingBlocks) {
            if (trunkLogPos.contains(bps.pos())) {
                trunkBlocks.add(bps);
            } else if (bps.state().getBlock() instanceof FruitTreeLogBlock) {
                branchBlocks.add(bps);
            } else {
                leafBlocks.add(bps);
            }
        }

        // ── Calculate fall direction ────────────────────────────────────────
        Vec3 fallDir = calculateFallDirection(level, pos, trunkBlocks, branchBlocks);

        // ── Create main falling entity (trunk + leaves) ─────────────────────
        if (!trunkBlocks.isEmpty()) {
            FallingTreeEntity entity = new FallingTreeEntity(
                    net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack.FALLING_TREE.get(), level);
            entity.setPos(pos.getX() + 0.5, pos.getY() + 0.5 + 1.0, pos.getZ() + 0.5);
            entity.setFallDirection(fallDir.x, fallDir.y, fallDir.z);

            // Add trunk blocks + leaves to main entity
            for (BlockPosAndState bps : trunkBlocks) {
                entity.addBlock(bps.pos(), bps.state());
            }
            for (BlockPosAndState bps : leafBlocks) {
                entity.addBlock(bps.pos(), bps.state());
            }

            level.addFreshEntity(entity);
        }

        // ── Create separate falling entities for branches ───────────────────
        if (!branchBlocks.isEmpty()) {
            // Group branch logs by connectivity
            List<List<BlockPosAndState>> branchGroups = groupConnectedBranches(branchBlocks);
            for (List<BlockPosAndState> group : branchGroups) {
                // Find center of this branch group
                double cx = 0, cy = 0, cz = 0;
                for (BlockPosAndState bps : group) {
                    cx += bps.pos().getX();
                    cy += bps.pos().getY();
                    cz += bps.pos().getZ();
                }
                int n = group.size();
                cx /= n; cy /= n; cz /= n;

                FallingTreeEntity entity = new FallingTreeEntity(
                        net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack.FALLING_TREE.get(), level);
                entity.setPos(cx + 0.5, cy + 0.5, cz + 0.5);
                entity.setFallDirection(fallDir.x, fallDir.y * 0.7, fallDir.z); // branches fall slightly slower

                // Add branch blocks + nearby leaves to this entity
                Set<BlockPos> branchPosSet = new HashSet<>();
                for (BlockPosAndState bps : group) {
                    branchPosSet.add(bps.pos());
                    entity.addBlock(bps.pos(), bps.state());
                }
                // Add leaves that are adjacent to this branch group
                for (BlockPosAndState bps : leafBlocks) {
                    if (isAdjacentTo(bps.pos(), branchPosSet)) {
                        entity.addBlock(bps.pos(), bps.state());
                    }
                }

                level.addFreshEntity(entity);
            }
        }
    }

    /**
     * Calculate fall direction from: downhill slope + weight distribution.
     * Returns a normalized (x, y, z) direction vector.
     */
    private static Vec3 calculateFallDirection(
            Level level, BlockPos cutPos,
            List<BlockPosAndState> trunkBlocks,
            List<BlockPosAndState> branchBlocks) {

        // 1. Downhill slope: check blocks below the tree for slope direction
        double sx = 0.0, sz = 0.0;
        int samples = 0;
        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                BlockPos below = cutPos.offset(ox, -1, oz);
                BlockState bs = level.getBlockState(below);
                if (!bs.isAir()) {
                    BlockPos neighbor = below.offset(ox, 0, oz);
                    BlockState ns = level.getBlockState(neighbor);
                    if (ns.isAir()) {
                        sx += ox;
                        sz += oz;
                        samples++;
                    }
                }
            }
        }
        double dx = 1.0, dz = 0.0; // default: fall in +X direction
        if (samples > 0) {
            double sh = Math.sqrt(sx * sx + sz * sz);
            if (sh > 0.001) {
                dx = sx / sh;
                dz = sz / sh;
            }
        }

        // 2. Weight distribution: more branch weight on one side pulls that way
        double wx = 0.0, wz = 0.0;
        for (BlockPosAndState bps : branchBlocks) {
            wx += bps.pos().getX() - (cutPos.getX() + 0.5);
            wz += bps.pos().getZ() - (cutPos.getZ() + 0.5);
        }
        double wh = Math.sqrt(wx * wx + wz * wz);
        if (wh > 0.001) {
            wx /= wh;
            wz /= wh;
            // Blend slope (70%) with weight (30%)
            dx = dx * 0.7 + wx * 0.3;
            dz = dz * 0.7 + wz * 0.3;
            double h = Math.sqrt(dx * dx + dz * dz);
            if (h > 0.001) {
                dx /= h;
                dz /= h;
            }
        }

        // Vertical component: mostly downward
        double dy = -0.8;

        // Normalize the full vector
        double vh = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return new Vec3(dx / vh, dy / vh, dz / vh);
    }

    /**
     * Group branch logs into connected groups (each group = one falling entity).
     */
    private static List<List<BlockPosAndState>> groupConnectedBranches(
            List<BlockPosAndState> branchBlocks) {

        List<List<BlockPosAndState>> groups = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        for (BlockPosAndState start : branchBlocks) {
            if (visited.contains(start.pos())) continue;

            List<BlockPosAndState> group = new ArrayList<>();
            Queue<BlockPosAndState> queue = new ArrayDeque<>();
            queue.add(start);
            visited.add(start.pos());

            while (!queue.isEmpty()) {
                BlockPosAndState current = queue.poll();
                group.add(current);

                for (BlockPosAndState other : branchBlocks) {
                    if (visited.contains(other.pos())) continue;
                    if (current.pos().distSqr(other.pos()) <= 2.0) { // face-adjacent
                        visited.add(other.pos());
                        queue.add(other);
                    }
                }
            }

            groups.add(group);
        }

        return groups;
    }

    private static boolean isAdjacentTo(BlockPos pos, Set<BlockPos> branchPosSet) {
        for (Direction dir : Direction.values()) {
            if (branchPosSet.contains(pos.relative(dir))) return true;
        }
        return false;
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
                BlockPos neighbor = current.relative(dir);
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
