package net.enderwish.Farming_Overhaul_Subpack.core.tree;

import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLeavesBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TreeGrowthManager — persistent registry of player-planted, still-
 * growing trees, keyed by their base position.
 *
 * Stores every placed voxel of each tracked tree so a stage change can
 * cleanly remove the old shape and regrow the tree at the new stage
 * (same species, fresh growth — like real wood widening around old wood).
 *
 * Stage progression: one stage per season change (FruitTreeSeasonHandler
 * calls onSeasonChanged), so a planted tree takes a full year (4 seasons)
 * to reach MATURE. Commands (/treestage) can advance or regress freely.
 *
 * Trees are saved per-dimension via SavedData ("gh_farming_trees").
 */
public class TreeGrowthManager extends SavedData {

    private static final String DATA_NAME = "gh_farming_trees";
    /** Max squared distance when locating a tree from given coordinates. */
    private static final double MAX_LOCATE_DIST_SQ = 8.0 * 8.0 * 2.0;

    /** One tracked tree: species, current stage, and every placed voxel. */
    /** One tracked tree: species, current stage, its growth seed, and
     * every placed voxel. The seed fixes the tree's shape — every stage
     * regrow replays the same seed, so advancing then regressing returns
     * the exact earlier appearance. */
    public record TreeRecord(String species, TreeStage stage, long seed,
                             long[] voxels) {}

    private final Map<BlockPos, TreeRecord> trees = new HashMap<>();

    public TreeGrowthManager() {}

    // ── Access ────────────────────────────────────────────────────────────────

    public static TreeGrowthManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(TreeGrowthManager::new,
                        TreeGrowthManager::load, null),
                DATA_NAME);
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /** Registers a freshly grown (staged) tree so it can grow on. */
    public void track(ServerLevel level, BlockPos origin, String species,
                      TreeStage stage, long seed, List<BlockPos> placedVoxels) {
        long[] voxels = new long[placedVoxels.size()];
        for (int i = 0; i < placedVoxels.size(); i++)
            voxels[i] = placedVoxels.get(i).asLong();
        trees.put(origin.immutable(),
                new TreeRecord(species, stage, seed, voxels));
        this.setDirty();
    }

    public TreeRecord get(BlockPos origin) { return trees.get(origin); }

    /** Nearest tracked tree origin to the given position, or null. */
    public BlockPos findNearest(BlockPos around) {
        BlockPos best = null;
        double bestD = MAX_LOCATE_DIST_SQ;
        for (BlockPos p : trees.keySet()) {
            double d = p.distSqr(around);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    // ── Stage changes ─────────────────────────────────────────────────────────

    /**
     * Removes the old shape and regrows the tree at a new stage.
     * Returns false if there was no tracked tree or the regrow failed.
     */
    public boolean setStage(ServerLevel level, BlockPos origin, TreeStage newStage) {
        TreeRecord rec = trees.get(origin);
        if (rec == null || rec.stage() == newStage) return false;

        Optional<TreeDefinition> def =
                TreeRegistry.INSTANCE.getBySpecies(rec.species());
        if (def.isEmpty()) return false;

        removeTreeBlocks(level, rec);

        // replay the SAME seed at the new stage so the tree keeps its
        // identity across stage changes (advance/regress are consistent)
        List<BlockPos> placed = new ArrayList<>();
        boolean ok = TreeGenerator.generate(level, origin, def.get(),
                RandomSource.create(rec.seed()), true, newStage, placed);
        if (ok) {
            track(level, origin, rec.species(), newStage, rec.seed(), placed);
        }
        return ok;
    }

    /** Season tick: every not-yet-mature tracked tree grows one stage. */
    public void onSeasonChanged(ServerLevel level) {
        for (BlockPos origin : new ArrayList<>(trees.keySet())) {
            TreeRecord rec = trees.get(origin);
            if (rec != null && rec.stage() != TreeStage.MATURE) {
                setStage(level, origin, rec.stage().next());
            }
        }
    }

    /**
     * Instantly recolors every tracked tree's leaves to the current
     * season — no chunk scanning, just direct block updates over the
     * stored voxel lists (setBlockAndUpdate re-renders immediately).
     */
    public void updateAllLeafStages(ServerLevel level) {
        for (TreeRecord rec : trees.values()) {
            for (long v : rec.voxels()) {
                BlockPos p = BlockPos.of(v);
                BlockState st = level.getBlockState(p);
                if (st.getBlock() instanceof FruitTreeLeavesBlock) {
                    FruitTreeLeavesBlock.updateSeasonStage(level, p, st);
                }
            }
        }
    }

    /** Clears old tree blocks, but only ones we recognise as ours. */
    private void removeTreeBlocks(ServerLevel level, TreeRecord rec) {
        for (long v : rec.voxels()) {
            BlockPos p = BlockPos.of(v);
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof FruitTreeLogBlock
                    || st.getBlock() instanceof FruitTreeLeavesBlock
                    || st.getBlock() instanceof FruitBlock) {
                level.removeBlock(p, false);
            }
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, TreeRecord> e : trees.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putLong("origin", e.getKey().asLong());
            t.putString("species", e.getValue().species());
            t.putInt("stage", e.getValue().stage().id());
            t.putLong("seed", e.getValue().seed());
            t.putLongArray("voxels", e.getValue().voxels());
            list.add(t);
        }
        tag.put("trees", list);
        return tag;
    }

    private static TreeGrowthManager load(CompoundTag tag,
                                          HolderLookup.Provider provider) {
        TreeGrowthManager mgr = new TreeGrowthManager();
        ListTag list = tag.getList("trees", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            BlockPos origin = BlockPos.of(t.getLong("origin"));
            TreeRecord rec = new TreeRecord(
                    t.getString("species"),
                    TreeStage.byId(t.getInt("stage")),
                    t.getLong("seed"),
                    t.getLongArray("voxels"));
            mgr.trees.put(origin, rec);
        }
        return mgr;
    }
}
