package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Handles global block changes based on seasons (Freezing/Thawing).
 * Updated with Instant methods for Cutscene transitions.
 */
@EventBusSubscriber(modid = "hud_visuals_subpack")
public class GlobalWeatherPhysicsHandler {

    /**
     * MASSIVE FREEZE: Called by SeasonManager during the cutscene transition to Winter.
     */
    public static void performInstantWinterFreeze(ServerLevel level, BlockPos center, int radius) {
        // Use betweenClosed instead of betweenClosedIterables
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -15, -radius),
                center.offset(radius, 15, radius)
        )) {
            if (!isExtremeHotBiome(level, pos)) {
                BlockState state = level.getBlockState(pos);
                // Freeze Water Sources
                if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                    level.setBlock(pos, Blocks.ICE.defaultBlockState(), 2);
                }
            }
        }
    }

    /**
     * MASSIVE THAW: Called by SeasonManager during the cutscene transition to Spring/Summer.
     */
    public static void performInstantMelt(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -15, -radius),
                center.offset(radius, 15, radius)
        )) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.ICE)) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
            } else if (state.is(Blocks.SNOW)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 20 == 0) {
            Season currentSeason = SeasonManager.getSeason(level);

            level.players().forEach(player -> {
                BlockPos pos = player.blockPosition();
                int radius = 24;

                // Process random blocks for "creeping" effects outside the instant radius
                for (int i = 0; i < 12; i++) {
                    BlockPos randomPos = pos.offset(
                            level.random.nextInt(radius * 2) - radius,
                            level.random.nextInt(10) - 5,
                            level.random.nextInt(radius * 2) - radius
                    );

                    if (!isExtremeHotBiome(level, randomPos)) {
                        handleSeasonalPhysics(level, randomPos, currentSeason);
                    }
                }
            });
        }
    }

    private static boolean isExtremeHotBiome(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.value().getBaseTemperature() >= 1.0f;
    }

    private static void handleSeasonalPhysics(ServerLevel level, BlockPos pos, Season season) {
        BlockState state = level.getBlockState(pos);

        if (season == Season.WINTER) {
            if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            }
        }
        else if (season == Season.SPRING) {
            if (state.is(Blocks.ICE)) {
                if (!isNearSolidBank(level, pos)) {
                    if (level.random.nextFloat() < 0.15f) {
                        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                    }
                }
            }
        }
        else {
            if (state.is(Blocks.ICE) || state.is(Blocks.SNOW)) {
                level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            }
        }
    }

    private static boolean isNearSolidBank(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.north()).isSolid() ||
                level.getBlockState(pos.south()).isSolid() ||
                level.getBlockState(pos.east()).isSolid() ||
                level.getBlockState(pos.west()).isSolid();
    }
}