package net.enderwish.Farming_Overhaul_Subpack.block.farmland;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * FertilizedFarmlandBlock
 *
 * Mirrors vanilla FarmBlock exactly:
 *   - MOISTURE property 0-7 (same as vanilla)
 *   - Water nearby (4x4 box, ±1 Y) → instantly jump to moisture 7
 *   - No water nearby → drain moisture by 1 per random tick
 *   - Moisture 0 + no crop above → turns to DIRT (fertilizer lost)
 *   - Moisture 0 + crop above   → stays fertilized (crop protects soil)
 *
 * Difference from vanilla:
 *   - Crops can ONLY be planted on THIS block (not vanilla farmland)
 *   - When a crop reaches stage 7 (mature/harvested) → reverts to
 *     NORMAL vanilla farmland (fertilizer consumed), see GHCropBlock
 *   - Trampling → DIRT (same as vanilla, fertilizer lost)
 *
 * Texture: moisture == 7 uses the "moist" top texture,
 *          moisture 0-6 uses the "dry" top texture (handled in blockstate JSON)
 */
public class FertilizedFarmlandBlock extends Block {

    public static final IntegerProperty MOISTURE = BlockStateProperties.MOISTURE; // 0-7

    private static final VoxelShape SHAPE =
            Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

    public FertilizedFarmlandBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(MOISTURE, 0));
    }

    // ── Block state definition ────────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOISTURE);
    }

    // ── Shape ─────────────────────────────────────────────────────────────────

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ── Random tick — vanilla moisture logic ──────────────────────────────────

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level,
                           BlockPos pos, RandomSource random) {
        int moisture = state.getValue(MOISTURE);

        if (!isNearWater(level, pos)) {
            // ── No water nearby ──────────────────────────────────────────────
            if (moisture > 0) {
                // Drain moisture by 1
                level.setBlockAndUpdate(pos, state.setValue(MOISTURE, moisture - 1));
            } else if (!hasCropAbove(level, pos)) {
                // Moisture is 0 and nothing planted — fertilizer exhausted
                turnToDirt(level, pos);
            }
            // moisture == 0 and crop above → stay as-is, crop protects soil
        } else {
            // ── Water nearby — instantly jump to moisture 7 ──────────────────
            if (moisture < 7) {
                level.setBlockAndUpdate(pos, state.setValue(MOISTURE, 7));
            }
        }
    }

    /**
     * Converts this block to plain dirt — fertilizer fully lost.
     */
    public static void turnToDirt(Level level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
    }

    /**
     * Returns true if the block directly above is a crop
     * (anything that isn't air — mirrors vanilla isUnderCrops check).
     */
    private static boolean hasCropAbove(LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return !above.isAir();
    }

    // ── Trampling — converts to dirt ──────────────────────────────────────────

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos,
                       Entity entity, float fallDistance) {
        if (!level.isClientSide()
                && level.random.nextFloat() < fallDistance - 0.5f
                && entity instanceof LivingEntity
                && (entity instanceof net.minecraft.world.entity.player.Player
                || level.getGameRules().getBoolean(
                net.minecraft.world.level.GameRules.RULE_MOBGRIEFING))) {
            // Trample to dirt — fertilizer is lost
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    // ── Neighbour changes — break if no solid block below ────────────────────

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return !above.isSolid() || above.getBlock() instanceof FarmBlock;
    }

    // ── Water check — exact vanilla FarmBlock logic ───────────────────────────

    /**
     * Checks a 4x4 horizontal box (±4 blocks), ±1 vertical, for water fluid.
     * Identical to vanilla FarmBlock.isNearWater().
     */
    public static boolean isNearWater(LevelReader level, BlockPos pos) {
        for (BlockPos nearby : BlockPos.betweenClosed(
                pos.offset(-4, 0, -4),
                pos.offset(4, 1, 4))) {
            if (level.getFluidState(nearby).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Returns true if this state is fertilized farmland (any moisture level).
     */
    public static boolean isFertilizedFarmland(BlockState state) {
        return state.getBlock() instanceof FertilizedFarmlandBlock;
    }

    /**
     * Returns true if this fertilized farmland is at max moisture (7).
     */
    public static boolean isMoist(BlockState state) {
        return isFertilizedFarmland(state) && state.getValue(MOISTURE) == 7;
    }
}
