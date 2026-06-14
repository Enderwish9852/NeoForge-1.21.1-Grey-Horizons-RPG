package net.enderwish.Farming_Overhaul_Subpack.block.crop;

import net.enderwish.Farming_Overhaul_Subpack.block.farmland.FertilizedFarmlandBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GHCropBlock
 *
 * Generic crop block used by all Grey Horizons crops.
 * Behaviour is driven by CropRegistry JSON data (future).
 *
 * AGE 0-6 = growing stages
 * AGE 7   = fully grown / harvestable (mature)
 * AGE 8   = wilted / dead — converts to dead bush + coarse dirt below
 *
 * Harvest types (future, from CropRegistry):
 *   ROOT — break to harvest (garlic, onion, sweet potato)
 *   VINE — right click to harvest, plant resets to stage 5
 *
 * Fertilized farmland integration:
 *   - Can only be planted on fertilized farmland (dry or moist)
 *   - At stage 7 (mature, awaiting harvest) — farmland stays fertilized
 *   - At stage 8 (wilted) — farmland below becomes coarse dirt,
 *     crop becomes dead bush — nutrients fully exhausted
 */
public class GHCropBlock extends Block {

    // ── Block state ───────────────────────────────────────────────────────────

    public static final int MAX_AGE     = 8; // 0-8 = 9 stages
    public static final int MATURE_AGE  = 7; // fully grown, harvestable
    public static final int WILT_AGE    = 8; // wilted, dead

    public static final IntegerProperty AGE =
            IntegerProperty.create("age", 0, MAX_AGE);

    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Block.box(0, 0, 0, 16, 2,  16), // stage 0
            Block.box(0, 0, 0, 16, 4,  16), // stage 1
            Block.box(0, 0, 0, 16, 6,  16), // stage 2
            Block.box(0, 0, 0, 16, 8,  16), // stage 3
            Block.box(0, 0, 0, 16, 10, 16), // stage 4
            Block.box(0, 0, 0, 16, 12, 16), // stage 5
            Block.box(0, 0, 0, 16, 14, 16), // stage 6
            Block.box(0, 0, 0, 16, 16, 16), // stage 7 — mature
            Block.box(0, 0, 0, 16, 16, 16), // stage 8 — wilted
    };

    // ── Crop ID — used to look up CropRegistry ────────────────────────────────
    private final String cropId;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GHCropBlock(String cropId, BlockBehaviour.Properties properties) {
        super(properties);
        this.cropId = cropId;
        this.registerDefaultState(
                this.stateDefinition.any().setValue(AGE, 0));
    }

    // ── Block state definition ────────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    // ── Shape ─────────────────────────────────────────────────────────────────

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    // ── Random tick — growth, mature hold, and wilt ──────────────────────────

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true; // always tick — needed for mature hold and wilt
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level,
                           BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);

        // ── Growing stages 0-6 → advance normally ────────────────────────────
        if (age < MATURE_AGE) {
            level.setBlockAndUpdate(pos, state.setValue(AGE, age + 1));
            return;
        }

        // ── Stage 7 (mature) — TODO: harvest window timer ────────────────────
        // For now stays at stage 7 indefinitely until harvested or
        // the wilt timer system is implemented.
        // When wilt timer expires → advance to stage 8
        if (age == MATURE_AGE) {
            // TODO: wilt timer logic goes here
            // level.setBlockAndUpdate(pos, state.setValue(AGE, WILT_AGE));
            return;
        }

        // ── Stage 8 (wilted) — convert to dead bush + coarse dirt ────────────
        if (age == WILT_AGE) {
            wilt(level, pos);
        }
    }

    // ── Wilt — replace with dead bush, farmland below → coarse dirt ─────────

    public static void wilt(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        // Fertilized farmland (dry or moist) OR normal farmland
        // → coarse dirt (nutrients fully exhausted)
        if (FertilizedFarmlandBlock.isFertilizedFarmland(belowState)
                || belowState.is(Blocks.FARMLAND)) {
            level.setBlockAndUpdate(below, Blocks.COARSE_DIRT.defaultBlockState());
        }

        // Replace crop with dead bush
        level.setBlockAndUpdate(pos, Blocks.DEAD_BUSH.defaultBlockState());
    }

    // ── Right click — vine crop harvest ──────────────────────────────────────

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state,
                                              Level level, BlockPos pos,
                                              Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        int age = state.getValue(AGE);

        // Only allow right-click harvest at mature stage
        if (age != MATURE_AGE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // TODO: Check if this crop is a vine type from CropRegistry
        // For now all crops support right-click harvest at stage 7
        if (!level.isClientSide()) {
            // Drop the crop item
            // TODO: drop correct item from CropRegistry

            // Mature → consume fertilizer, farmland reverts to normal
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (FertilizedFarmlandBlock.isFertilizedFarmland(belowState)) {
                level.setBlockAndUpdate(below, Blocks.FARMLAND.defaultBlockState());
            }

            // Reset to stage 5 for second cycle (vine crops only — TODO)
            level.setBlockAndUpdate(pos, state.setValue(AGE, 5));
        }

        return ItemInteractionResult.SUCCESS;
    }

    // ── Placement validation — must be on FERTILIZED farmland ────────────────

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return FertilizedFarmlandBlock.isFertilizedFarmland(belowState);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getCropId() { return cropId; }

    public boolean isMature(BlockState state) {
        return state.getValue(AGE) == MATURE_AGE;
    }

    public boolean isWilted(BlockState state) {
        return state.getValue(AGE) == WILT_AGE;
    }
}
