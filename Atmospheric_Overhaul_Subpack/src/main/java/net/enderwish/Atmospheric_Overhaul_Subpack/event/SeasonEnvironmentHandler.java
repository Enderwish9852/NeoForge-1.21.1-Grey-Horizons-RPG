package net.enderwish.Atmospheric_Overhaul_Subpack.event;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonData;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonTemperature;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * SeasonEnvironmentHandler
 *
 * Uses SeasonTemperature.calculate() to drive all snow and ice behaviour.
 *
 * Snow formation — only during active precipitation (hasRain = true)
 *                  AND cold enough temperature
 * Ice formation  — continuous background process when cold enough,
 *                  regardless of current weather
 * Melting        — always active when temperature is above melt threshold
 *
 * TODO: constants will move to SeasonConfig for player customisation.
 */
public class SeasonEnvironmentHandler {

    // ── Constants ─────────────────────────────────────────────────────────────
    // TODO: these will become config values in the settings screen

    private static final int RADIUS        = 48;
    private static final int BASE_INTERVAL = 20;
    private static final int BASE_BLOCKS   = 8;

    private static int tickCounter = 0;

    // ── Tick ──────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        tickCounter++;
        if (tickCounter % BASE_INTERVAL != 0) return;

        SeasonData data = SeasonData.get(level);
        RandomSource rand = level.getRandom();

        // Check once per tick whether it is currently precipitating
        // Snow only falls when hasRain = true on the active weather
        boolean isPrecipitating = WeatherRegistry.INSTANCE
                .getByName(data.getActiveWeatherId())
                .hasRain();

        for (ServerPlayer player : level.players()) {
            BlockPos center = player.blockPosition();

            for (int i = 0; i < BASE_BLOCKS; i++) {
                int dx = rand.nextInt(RADIUS * 2) - RADIUS;
                int dz = rand.nextInt(RADIUS * 2) - RADIUS;
                BlockPos surfacePos = level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING,
                        center.offset(dx, 0, dz)
                );

                float temp = SeasonTemperature.calculate(level, surfacePos);
                processBlock(level, surfacePos, temp, rand, isPrecipitating);
            }
        }
    }

    // ── Block processing ──────────────────────────────────────────────────────

    private static void processBlock(ServerLevel level, BlockPos pos,
                                     float temp, RandomSource rand,
                                     boolean isPrecipitating) {
        BlockState state = level.getBlockState(pos);
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        // ── Melting ───────────────────────────────────────────────────────────
        if (SeasonTemperature.shouldMelt(temp)) {
            float meltSpeed = SeasonTemperature.getMeltSpeed(temp);

            // Melt snow layers
            if (state.is(Blocks.SNOW)) {
                int layers = state.getValue(SnowLayerBlock.LAYERS);
                if (rand.nextFloat() < meltSpeed * 0.5f) {
                    if (layers <= 1) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(pos,
                                state.setValue(SnowLayerBlock.LAYERS, layers - 1));
                    }
                }
            }
            if (belowState.is(Blocks.SNOW)) {
                int layers = belowState.getValue(SnowLayerBlock.LAYERS);
                if (rand.nextFloat() < meltSpeed * 0.5f) {
                    if (layers <= 1) {
                        level.setBlockAndUpdate(below, Blocks.AIR.defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(below,
                                belowState.setValue(SnowLayerBlock.LAYERS, layers - 1));
                    }
                }
            }

            // Melt ice — only if well above threshold
            if (state.is(Blocks.ICE) && temp > SeasonTemperature.MELT_THRESHOLD + 0.1f) {
                if (rand.nextFloat() < meltSpeed * 0.25f) {
                    level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                }
            }
            return;
        }

        // ── Formation ─────────────────────────────────────────────────────────
        if (SeasonTemperature.shouldForm(temp)) {
            float formSpeed = SeasonTemperature.getFormationSpeed(temp);
            int maxDepth    = SeasonTemperature.getMaxSnowDepth(temp);
            int iceSpread   = SeasonTemperature.getIceSpread(temp);

            // ── Snow — only during active precipitation ────────────────────────
            if (isPrecipitating) {
                if (state.isAir() && belowState.isFaceSturdy(level, below, Direction.UP)
                        && level.canSeeSky(pos)) {

                    if (belowState.is(Blocks.SNOW)) {
                        int currentLayers = belowState.getValue(SnowLayerBlock.LAYERS);
                        if (currentLayers < maxDepth
                                && rand.nextFloat() < formSpeed * 0.25f) {
                            level.setBlockAndUpdate(below,
                                    belowState.setValue(SnowLayerBlock.LAYERS, currentLayers + 1));
                        }
                    } else if (!belowState.is(Blocks.ICE)) {
                        if (rand.nextFloat() < formSpeed * 0.4f) {
                            level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
                        }
                    }
                }
            }

            // ── Ice — always in background, no precipitation needed ────────────
            if (state.is(Blocks.WATER) && level.canSeeSky(pos)
                    && iceSpread > 0 && isNearEdge(level, pos, iceSpread)) {
                if (rand.nextFloat() < formSpeed * 0.3f) {
                    level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isNearEdge(ServerLevel level, BlockPos pos, int spread) {
        for (int dx = -spread; dx <= spread; dx++) {
            for (int dz = -spread; dz <= spread; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockState checkState = level.getBlockState(pos.offset(dx, 0, dz));
                if (!checkState.is(Blocks.WATER) && !checkState.is(Blocks.ICE)) {
                    return true;
                }
            }
        }
        return false;
    }
}