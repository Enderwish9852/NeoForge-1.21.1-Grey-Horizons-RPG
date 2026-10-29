package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLeavesBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLogBlock;
import net.enderwish.Farming_Overhaul_Subpack.core.tree.TreeGrowthManager;
import net.enderwish.Farming_Overhaul_Subpack.core.tree.TreeRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.tree.TreeStage;
import net.enderwish.Farming_Overhaul_Subpack.entity.FallingTreeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Detection strategy:
 *   1. First check TreeGrowthManager for a tracked tree at the cut position.
 *      Tracked trees store every voxel (TreeRecord.voxels), so we know exactly
 *      which blocks belong to the tree — no forest-wide leaf chaining, no
 *      missed blocks, works for dense trees.
 *   2. Fall back to flood-fill for wild/untracked trees (worldgen trees that
 *      were never tracked).
 *
 * Behavior:
 *   - Blocks at/below the cut Y level become a stump (stay in world)
 *   - Logs above the cut + connected leaves become falling entities
 *   - Trunk = logs vertically connected above the cut → main falling entity
 *   - Branches = side logs → separate falling entities
 *   - Fall direction = downhill slope + weight distribution
 */
@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, bus = EventBusSubscriber.Bus.MOD)
public class TreeFallHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("GH_TreeFall");

    /** Guard against re-entrancy: removing blocks inside this handler must not re-trigger it. */
    private static boolean processing = false;

    /** Max blocks per falling tree — prevents absurdly large trees from lagging. */
    private static final int MAX_TREE_BLOCKS = 2000;

    private TreeFallHandler() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (processing) {
            LOGGER.debug("TreeFallHandler: already processing, skipping");
            return;
        }

        if (!net.enderwish.Frontier_Subpack.FrontierConfig.TREES_FALL.get()) {
            return;
        }

        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof Level level)) {
            return;
        }

        if (level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState(); // state BEFORE vanilla removed it

        // Only process tree log blocks
        boolean isTreeLog = state.getBlock() instanceof FruitTreeLogBlock
            || state.is(BlockTags.LOGS);
        if (!isTreeLog) {
            LOGGER.debug("TreeFallHandler: block at {} is not a tree log ({})", pos, state.getBlock().getDescriptionId());
            return;
        }

        LOGGER.info("TreeFallHandler: tree log cut at {} — starting tree fall", pos);

        processing = true;
        try {
            // ── Step 1: Try tracked tree data first ─────────────────────────────
            List<BlockPosAndState> treeBlocks = null;
            TreeGrowthManager mgr = TreeGrowthManager.get((net.minecraft.server.level.ServerLevel) level);
            BlockPos nearestOrigin = mgr.findNearest(pos);
            if (nearestOrigin != null) {
                TreeGrowthManager.TreeRecord rec = mgr.get(nearestOrigin);
                if (rec != null && isBlockInVoxels(rec, pos)) {
                    treeBlocks = trackedTreeToBlockList(level, rec, pos);
                    LOGGER.info("TreeFallHandler: used tracked tree data — {} blocks", treeBlocks.size());
                }
            }

            // ── Step 2: Fall back to flood-fill for untracked trees ─────────────
            if (treeBlocks == null) {
                treeBlocks = findConnectedTree(level, pos, state);
                LOGGER.info("TreeFallHandler: flood-fill found {} connected tree blocks", treeBlocks.size());
            }

            if (treeBlocks.isEmpty()) return;

            // Safety cap
            if (treeBlocks.size() > MAX_TREE_BLOCKS) {
                LOGGER.info("TreeFallHandler: tree too large ({} > {} blocks), skipping", treeBlocks.size(), MAX_TREE_BLOCKS);
                return;
            }

            // ── Stump: leave blocks at/below the cut Y level ─────────────────────
            int cutY = pos.getY();
            List<BlockPosAndState> fallingBlocks = new ArrayList<>();
            for (BlockPosAndState bps : treeBlocks) {
                if (bps.pos().getY() > cutY) {
                    fallingBlocks.add(bps);
                }
            }

            LOGGER.info("TreeFallHandler: {} falling blocks above cut height {}, {} stump blocks at/below",
                fallingBlocks.size(), cutY, treeBlocks.size() - fallingBlocks.size());

            if (fallingBlocks.isEmpty()) {
                LOGGER.info("TreeFallHandler: no blocks above cut height — nothing to fall, leaving stump");
                return;
            }

            // Remove only the blocks that will become the falling entity
            for (BlockPosAndState bps : fallingBlocks) {
                level.removeBlock(bps.pos(), false);
            }

            // ── Classify trunk vs branches vs leaves ────────────────────────────
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
                    if (dir.getAxis() != Direction.Axis.Y) continue;
                    BlockPos neighbor = current.relative(dir);
                    if (trunkLogPos.contains(neighbor)) continue;
                    BlockState ns = level.getBlockState(neighbor);
                    // Neighbor was removed already, check against original tree data
                    if (isLogInTreeBlocks(neighbor, treeBlocks)) {
                        trunkLogPos.add(neighbor);
                        trunkQueue.add(neighbor);
                    }
                }
            }

            for (BlockPosAndState bps : fallingBlocks) {
                if (trunkLogPos.contains(bps.pos())) {
                    trunkBlocks.add(bps);
                } else if (isLog(bps.state())) {
                    branchBlocks.add(bps);
                } else {
                    leafBlocks.add(bps);
                }
            }

            LOGGER.info("TreeFallHandler: split into {} trunk blocks, {} branch blocks, {} leaf blocks",
                trunkBlocks.size(), branchBlocks.size(), leafBlocks.size());

            // ── Calculate fall direction ────────────────────────────────────────
            Vec3 fallDir = calculateFallDirection(level, pos, trunkBlocks, branchBlocks);
            LOGGER.info("TreeFallHandler: fall direction = ({}, {}, {})", fallDir.x, fallDir.y, fallDir.z);

            // ── Create main falling entity (trunk + leaves) ─────────────────────
            if (!trunkBlocks.isEmpty()) {
                FallingTreeEntity entity = new FallingTreeEntity(
                        net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack.FALLING_TREE.get(), level);
                entity.setPos(pos.getX() + 0.5, pos.getY() + 0.5 + 1.0, pos.getZ() + 0.5);
                entity.setFallDirection(fallDir.x, fallDir.y, fallDir.z);
                entity.setTrunk(true);

                for (BlockPosAndState bps : trunkBlocks) {
                    entity.addBlock(bps.pos(), bps.state());
                }
                for (BlockPosAndState bps : leafBlocks) {
                    entity.addBlock(bps.pos(), bps.state());
                }

                level.addFreshEntity(entity);
                LOGGER.info("TreeFallHandler: spawned main falling entity with {} blocks", entity.blockCount());
            } else if (!leafBlocks.isEmpty()) {
                FallingTreeEntity entity = new FallingTreeEntity(
                        net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack.FALLING_TREE.get(), level);
                entity.setPos(pos.getX() + 0.5, pos.getY() + 0.5 + 1.0, pos.getZ() + 0.5);
                entity.setFallDirection(fallDir.x, fallDir.y, fallDir.z);
                entity.setTrunk(false);

                for (BlockPosAndState bps : leafBlocks) {
                    entity.addBlock(bps.pos(), bps.state());
                }

                level.addFreshEntity(entity);
                LOGGER.info("TreeFallHandler: spawned leaves-only falling entity with {} blocks", entity.blockCount());
            }

            // ── Create separate falling entities for branches ───────────────────
            if (!branchBlocks.isEmpty()) {
                List<List<BlockPosAndState>> branchGroups = groupConnectedBranches(branchBlocks);
                for (List<BlockPosAndState> group : branchGroups) {
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
                    entity.setFallDirection(fallDir.x, fallDir.y * 0.7, fallDir.z);
                    entity.setTrunk(false);

                    Set<BlockPos> branchPosSet = new HashSet<>();
                    for (BlockPosAndState bps : group) {
                        branchPosSet.add(bps.pos());
                        entity.addBlock(bps.pos(), bps.state());
                    }
                    for (BlockPosAndState bps : leafBlocks) {
                        if (isAdjacentTo(bps.pos(), branchPosSet)) {
                            entity.addBlock(bps.pos(), bps.state());
                        }
                    }

                    level.addFreshEntity(entity);
                    LOGGER.info("TreeFallHandler: spawned branch falling entity with {} blocks", entity.blockCount());
                }
            }
        } finally {
            processing = false;
        }
    }

    // ── Tracked tree data helpers ─────────────────────────────────────────────

    /** Check if a BlockPos is in a TreeRecord's voxel array. */
    private static boolean isBlockInVoxels(TreeGrowthManager.TreeRecord rec, BlockPos pos) {
        long target = pos.asLong();
        for (long v : rec.voxels()) {
            if (v == target) return true;
        }
        return false;
    }

    /** Convert a tracked TreeRecord's voxels to BlockPosAndState list, checking current world state. */
    private static List<BlockPosAndState> trackedTreeToBlockList(Level level, TreeGrowthManager.TreeRecord rec, BlockPos cutPos) {
        List<BlockPosAndState> result = new ArrayList<>();
        for (long v : rec.voxels()) {
            BlockPos p = BlockPos.of(v);
            BlockState st = level.getBlockState(p);
            // Only include blocks that still exist and are tree-related
            if (st.getBlock() instanceof FruitTreeLogBlock
                    || st.is(BlockTags.LOGS)
                    || st.getBlock() instanceof FruitTreeLeavesBlock
                    || st.is(BlockTags.LEAVES)) {
                result.add(new BlockPosAndState(p, st));
            }
        }
        return result;
    }

    /** Check if a BlockPos is a log in the treeBlocks list (block was removed but we know its type). */
    private static boolean isLogInTreeBlocks(BlockPos pos, List<BlockPosAndState> treeBlocks) {
        for (BlockPosAndState bps : treeBlocks) {
            if (bps.pos().equals(pos)) {
                return isLog(bps.state());
            }
        }
        return false;
    }

    private static boolean isLog(BlockState state) {
        return state.getBlock() instanceof FruitTreeLogBlock || state.is(BlockTags.LOGS);
    }

    // ── Flood-fill fallback ───────────────────────────────────────────────────

    /**
     * Flood-fill from the broken block to find all connected tree blocks.
     * Only same-log-type blocks and adjacent leaves are included.
     * Leaves only connect to logs (not leaf-to-leaf) to prevent forest-wide chaining.
     */
    private static List<BlockPosAndState> findConnectedTree(Level level, BlockPos start, BlockState startState) {
        List<BlockPosAndState> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);
        result.add(new BlockPosAndState(start, startState));

        while (!queue.isEmpty()) {
            if (result.size() >= MAX_TREE_BLOCKS) break;

            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) continue;

                BlockState neighborState = level.getBlockState(neighbor);
                Block neighborBlock = neighborState.getBlock();

                boolean neighborIsLog = neighborBlock instanceof FruitTreeLogBlock
                    || neighborState.is(BlockTags.LOGS);
                boolean neighborIsLeaf = neighborBlock instanceof FruitTreeLeavesBlock
                    || neighborState.is(BlockTags.LEAVES);

                if (neighborIsLog) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    result.add(new BlockPosAndState(neighbor, neighborState));
                } else if (neighborIsLeaf) {
                    // Leaves only connect to logs: current must be a log
                    boolean currentIsLog = current.equals(start) && isLog(startState)
                        || (level.getBlockState(current).getBlock() instanceof FruitTreeLogBlock
                        || level.getBlockState(current).is(BlockTags.LOGS));
                    if (currentIsLog) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                        result.add(new BlockPosAndState(neighbor, neighborState));
                    }
                }
            }
        }

        return result;
    }

    // ── Fall direction ────────────────────────────────────────────────────────

    private static Vec3 calculateFallDirection(
            Level level, BlockPos cutPos,
            List<BlockPosAndState> trunkBlocks,
            List<BlockPosAndState> branchBlocks) {

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
        double dx = 1.0, dz = 0.0;
        if (samples > 0) {
            double sh = Math.sqrt(sx * sx + sz * sz);
            if (sh > 0.001) {
                dx = sx / sh;
                dz = sz / sh;
            }
        }

        double wx = 0.0, wz = 0.0;
        for (BlockPosAndState bps : branchBlocks) {
            wx += bps.pos().getX() - (cutPos.getX() + 0.5);
            wz += bps.pos().getZ() - (cutPos.getZ() + 0.5);
        }
        double wh = Math.sqrt(wx * wx + wz * wz);
        if (wh > 0.001) {
            wx /= wh;
            wz /= wh;
            dx = dx * 0.7 + wx * 0.3;
            dz = dz * 0.7 + wz * 0.3;
            double h = Math.sqrt(dx * dx + dz * dz);
            if (h > 0.001) {
                dx /= h;
                dz /= h;
            }
        }

        double dy = -0.8;
        double vh = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return new Vec3(dx / vh, dy / vh, dz / vh);
    }

    // ── Branch grouping ───────────────────────────────────────────────────────

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
                    if (current.pos().distSqr(other.pos()) <= 2.0) {
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

    /** Record of a block position and its state in the falling tree. */
    public record BlockPosAndState(BlockPos pos, BlockState state) {}
}
