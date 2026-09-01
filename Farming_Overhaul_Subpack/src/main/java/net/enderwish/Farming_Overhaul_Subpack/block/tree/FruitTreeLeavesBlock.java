package net.enderwish.Farming_Overhaul_Subpack.block.tree;

import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.Farming_Overhaul_Subpack.core.tree.TreeDefinition;
import net.enderwish.Farming_Overhaul_Subpack.core.tree.TreeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * FruitTreeLeavesBlock
 *
 * Universal leaf block for all tree species.
 * Species identified by which block is registered (oak_leaves etc.)
 *
 * Block state:
 *   SEASON_STAGE — controls which texture/model is used
 *
 * Season stages:
 *   BARE     → winter for TEMPERATE trees (no leaves, just bare twigs visible)
 *   BUDDING  → early spring (small buds, sparse leaves returning)
 *   BLOSSOM  → mid spring TEMPERATE only (full pink/white flowers)
 *   FULL     → summer (complete dense canopy)
 *   COLOURED → autumn TEMPERATE only (orange/red/yellow by species)
 *
 * MEDITERRANEAN trees: always FULL (evergreen, never BARE)
 * TROPICAL trees:      always FULL (year-round canopy)
 * TEMPERATE trees:     cycle through all 5 stages
 *
 * Season updates driven by FruitTreeSeasonHandler
 * which listens to Atmospheric season change events.
 *
 * Leaves are non-solid, allow light through, and decay
 * when disconnected from logs (same as vanilla leaf decay logic).
 */
public class FruitTreeLeavesBlock extends Block {

    // ── Season stage enum ─────────────────────────────────────────────────────

    public enum SeasonStage implements StringRepresentable {
        BARE     ("bare"),
        BUDDING  ("budding"),
        BLOSSOM  ("blossom"),
        FULL     ("full"),
        COLOURED ("coloured");

        private final String name;

        SeasonStage(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }
    }

    public static final EnumProperty<SeasonStage> SEASON_STAGE =
            EnumProperty.create("season_stage", SeasonStage.class);

    // ── No collision — leaves are passable ────────────────────────────────────
    private static final VoxelShape SHAPE = Shapes.empty();

    // ── Species ID ────────────────────────────────────────────────────────────
    private final String species;

    // ── Constructor ───────────────────────────────────────────────────────────

    public FruitTreeLeavesBlock(String species,
                                BlockBehaviour.Properties properties) {
        super(properties);
        this.species = species;
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(SEASON_STAGE, SeasonStage.FULL));
    }

    // ── Block state ───────────────────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEASON_STAGE);
    }

    // ── Shape — leaves have no collision ──────────────────────────────────────

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos,
                                        CollisionContext context) {
        return SHAPE;
    }

    // ── Light — leaves don't block light fully ────────────────────────────────

    @Override
    public boolean propagatesSkylightDown(BlockState state,
                                          BlockGetter level, BlockPos pos) {
        return state.getValue(SEASON_STAGE) == SeasonStage.BARE;
    }

    // ── Random tick — leaf decay when disconnected from logs ──────────────────

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level,
                           BlockPos pos, RandomSource random) {
        // Check if disconnected from any log block within 6 blocks
        if (isDisconnected(level, pos)) {
            level.removeBlock(pos, false);
            // Drop a leaf item (optional — for future crafting use)
        }
    }

    /**
     * Returns true if no log block exists within 6 blocks Manhattan distance.
     * Simplified vanilla leaf decay logic.
     */
    private boolean isDisconnected(ServerLevel level, BlockPos pos) {
        for (BlockPos nearby : BlockPos.betweenClosed(
                pos.offset(-6, -6, -6),
                pos.offset(6, 6, 6))) {
            if (level.getBlockState(nearby).getBlock()
                    instanceof FruitTreeLogBlock) {
                return false;
            }
        }
        return true;
    }

    // ── Season update — called by FruitTreeSeasonHandler ─────────────────────

    /**
     * Updates the leaf stage based on current season.
     * Called externally by FruitTreeSeasonHandler on season change.
     */
    public static void updateSeasonStage(ServerLevel level, BlockPos pos,
                                         BlockState state) {
        if (!(state.getBlock() instanceof FruitTreeLeavesBlock leaves))
            return;

        TreeRegistry.INSTANCE.getBySpecies(leaves.species).ifPresent(def -> {
            SeasonStage newStage = calculateStage(level, def);
            if (state.getValue(SEASON_STAGE) != newStage) {
                level.setBlockAndUpdate(pos,
                        state.setValue(SEASON_STAGE, newStage));
            }
        });
    }

    /**
     * Calculates what season stage leaves should be in
     * based on the tree's SeasonalType and current season.
     */
    private static SeasonStage calculateStage(
            ServerLevel level, TreeDefinition def) {

        // Evergreen trees always stay FULL
        if (def.isEvergreen()) return SeasonStage.FULL;

        SeasonCalendar.Season season = SeasonsAPI.getSeason(level);
        SeasonCalendar.Phase  phase  = SeasonsAPI.getPhase(level);

        return switch (season) {
            case WINTER -> SeasonStage.BARE;
            case SPRING -> switch (phase) {
                case EARLY -> SeasonStage.BUDDING;
                case MID   -> def.hasBlossoms()
                        ? SeasonStage.BLOSSOM
                        : SeasonStage.BUDDING;
                case LATE  -> SeasonStage.FULL;
            };
            case SUMMER -> SeasonStage.FULL;
            case AUTUMN -> switch (phase) {
                case EARLY -> SeasonStage.FULL;
                case MID   -> SeasonStage.COLOURED;
                case LATE  -> SeasonStage.COLOURED;
            };
        };
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getSpecies() { return species; }
}
