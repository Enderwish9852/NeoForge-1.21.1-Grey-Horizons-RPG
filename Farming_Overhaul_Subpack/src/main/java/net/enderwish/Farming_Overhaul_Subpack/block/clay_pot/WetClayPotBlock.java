package net.enderwish.Farming_Overhaul_Subpack.block.clay_pot;

import com.mojang.serialization.MapCodec;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * WetClayPotBlock
 *
 * Placed in sunlight for 1 in-game day to dry into ClayPotBlock.
 * Tracks drying progress via DRY_TIME block state (0-80).
 * 80 = one in-game day worth of ticks at random tick rate.
 * Requires sky access to dry.
 */
public class WetClayPotBlock extends Block {

    public static final MapCodec<WetClayPotBlock> CODEC = simpleCodec(WetClayPotBlock::new);

    // 0 = freshly placed, 7 = fully dry (ready to convert)
    // Uses 8 stages to avoid too many block states
    public static final IntegerProperty DRY_STAGE = IntegerProperty.create("dry_stage", 0, 7);

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public WetClayPotBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DRY_STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRY_STAGE);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        // Keep ticking until stage 6 — conversion happens at the 7th tick
        return state.getValue(DRY_STAGE) < 7;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Only dry if sky is visible above
        if (!level.canSeeSky(pos.above())) return;

        // Only dry during daytime
        if (level.isNight()) return;

        // Stop drying if it is raining
        if (level.isRaining()) return;

        int stage = state.getValue(DRY_STAGE);

        if (stage < 6) {
            // Advance drying stage
            level.setBlockAndUpdate(pos, state.setValue(DRY_STAGE, stage + 1));
        } else {
            // Stage 6 → convert directly to ClayPotBlock
            level.setBlockAndUpdate(pos, ModBlocks.CLAY_POT.get().defaultBlockState());
        }
    }
}
