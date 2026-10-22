package net.enderwish.Atmospheric_Overhaul_Subpack.event;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonData;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonTemperature;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Map;

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
 * ── Why this sweeps instead of sampling ───────────────────────────────
 * The first version picked EIGHT random blocks per second within 48
 * blocks of each player. The area to cover is around 36,000 surface
 * blocks, so a single pass of snow cover would have taken over an
 * hour, and in practice nothing ever appeared: the season had no
 * visible effect on the terrain at all.
 *
 * It now SWEEPS. Each tick it takes one chunk per player, out of a
 * 13x13 grid around them, and walks every column of it, so the whole
 * area comes round once every 169 ticks -- about eight and a half
 * seconds. Every column is then visited again and again, and the
 * probability per visit is what decides how fast the snow piles up.
 *
 * TODO: constants will move to SeasonConfig for player customisation.
 */
public class SeasonEnvironmentHandler {

    // ── Constants ─────────────────────────────────────────────────────────────
    // TODO: these will become config values in the settings screen

    /** How many chunks either side of the player the snow and ice reach. */
    private static final int RADIUS_CHUNKS = 6;
    /** How many chunks are swept per player per tick. */
    private static final int CHUNKS_PER_TICK = 1;

    /**
     * How likely one visit is to add a layer of snow, before the
     * temperature's own formation speed is applied. A column is visited
     * every 169 ticks, so at 0.008 a layer takes about a hundred
     * visits -- a couple of in-game hours, and a full cover over
     * days. The ground should not be white by lunchtime.
     */
    private static final float SNOW_CHANCE = 0.008f;

    /**
     * How likely one visit is to melt a layer, before the melt speed.
     * The spring thaw is meant to be quicker than the build-up.
     */
    private static final float MELT_CHANCE = 0.02f;

    /**
     * How likely the cold rain washes a layer away. This is the SLEET
     * band: cold enough to fall as rain rather than snow, but not warm
     * enough for a full melt, so the rain lands as ice and takes the
     * snow with it. It is the early thaw of late winter.
     */
    private static final float SLEET_CHANCE = 0.006f;

    /** The sweep cursor per dimension. */
    private static final Map<ResourceKey<Level>, Integer> CURSORS = new HashMap<>();

    // ── Tick ──────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        SeasonData data = SeasonData.get(level);
        RandomSource rand = level.getRandom();

        // Check once per tick whether it is currently precipitating
        // Snow only falls when hasRain = true on the active weather
        boolean isPrecipitating = WeatherRegistry.INSTANCE
                .getByName(data.getActiveWeatherId())
                .hasRain();

        int size = RADIUS_CHUNKS * 2 + 1;
        int total = size * size;
        int cursor = CURSORS.getOrDefault(level.dimension(), 0);

        for (ServerPlayer player : level.players()) {
            int playerChunkX = player.blockPosition().getX() >> 4;
            int playerChunkZ = player.blockPosition().getZ() >> 4;
            for (int i = 0; i < CHUNKS_PER_TICK; i++) {
                int idx = Math.floorMod(cursor, total);
                int ox = idx % size - RADIUS_CHUNKS;
                int oz = idx / size - RADIUS_CHUNKS;
                sweepChunk(level, playerChunkX + ox, playerChunkZ + oz, rand, isPrecipitating);
                cursor++;
            }
        }
        CURSORS.put(level.dimension(), cursor);
    }

    // ── The sweep ────────────────────────────────────────────────────────────

    /**
     * Walks every column of one chunk.
     *
     * The surface is found with the MOTION_BLOCKING_NO_LEAVES
     * heightmap, so a tree's canopy is not mistaken for the ground:
     * the snow lands on the ground under the tree, which is also where
     * the shade makes it colder.
     */
    private static void sweepChunk(ServerLevel level, int chunkX, int chunkZ,
                                    RandomSource rand, boolean isPrecipitating) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                BlockPos probe = BlockPos.containing(baseX + lx, 0, baseZ + lz);
                BlockPos surface = level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
                processColumn(level, surface, rand, isPrecipitating);
            }
        }
    }

    // ── Block processing ──────────────────────────────────────────────────────

    private static void processColumn(ServerLevel level, BlockPos surface,
                                     RandomSource rand, boolean isPrecipitating) {
        BlockPos ground = surface.below();
        BlockState groundState = level.getBlockState(ground);
        BlockState airState = level.getBlockState(surface);

        boolean snowOnGround = groundState.is(Blocks.SNOW);
        boolean snowAbove = airState.is(Blocks.SNOW);

        float temp = SeasonTemperature.calculate(level, surface);

        // ── Ice ────────────────────────────────────────────────────────────
        // The ice's own job, and it is the one that has to happen
        // whether or not it is raining: it forms at the banks and
        // pushes out over the season.
        if (airState.is(Blocks.WATER) && level.canSeeSky(surface)) {
            int iceSpread = SeasonTemperature.getIceSpread(temp);
            if (iceSpread > 0 && isNearEdge(level, surface, iceSpread)) {
                float formSpeed = SeasonTemperature.getFormationSpeed(temp);
                if (rand.nextFloat() < formSpeed * 0.05f) {
                    level.setBlockAndUpdate(surface, Blocks.ICE.defaultBlockState());
                    return;
                }
            }
        }

        // ── The snow ───────────────────────────────────────────────────────
        // The water's surface is the ice's business, not the snow's.
        if (airState.is(Blocks.WATER) || groundState.is(Blocks.WATER)) return;

        if (temp <= SeasonTemperature.NO_FORM_THRESHOLD) {
            // ── forming ─────────────────────────────────────────────────────
            if (snowOnGround) {
                int layers = groundState.getValue(SnowLayerBlock.LAYERS);
                int maxDepth = SeasonTemperature.getMaxSnowDepth(temp);
                float speed = SeasonTemperature.getFormationSpeed(temp);
                if (layers < maxDepth && rand.nextFloat() < SNOW_CHANCE * speed) {
                    level.setBlockAndUpdate(ground,
                            groundState.setValue(SnowLayerBlock.LAYERS, layers + 1));
                }
            } else if (!snowAbove && isPrecipitating) {
                if (groundState.isFaceSturdy(level, ground, Direction.UP)
                        && level.canSeeSky(surface)) {
                    float speed = SeasonTemperature.getFormationSpeed(temp);
                    if (rand.nextFloat() < SNOW_CHANCE * speed) {
                        level.setBlockAndUpdate(surface, Blocks.SNOW.defaultBlockState());
                    }
                }
            }
        } else if (temp > SeasonTemperature.MELT_THRESHOLD) {
            // ── melting ─────────────────────────────────────────────────────
            float speed = SeasonTemperature.getMeltSpeed(temp);
            if (snowOnGround && rand.nextFloat() < MELT_CHANCE * speed) {
                meltSnow(level, ground, groundState);
            }
            if (snowAbove && rand.nextFloat() < MELT_CHANCE * speed) {
                meltSnow(level, surface, airState);
            }
        } else if (isPrecipitating) {
            // ── the sleet band ─────────────────────────────────────────────
            // Cold rain, just above freezing: it lands as ice and
            // washes the snow away. This is the early thaw of late
            // winter, and it is the gap between the snow's threshold
            // and the melt's.
            if (snowOnGround && rand.nextFloat() < SLEET_CHANCE) {
                meltSnow(level, ground, groundState);
            }
            if (snowAbove && rand.nextFloat() < SLEET_CHANCE) {
                meltSnow(level, surface, airState);
            }
        }
    }

    /** Takes one layer off a snow block, removing it entirely at one. */
    private static void meltSnow(ServerLevel level, BlockPos pos, BlockState state) {
        int layers = state.getValue(SnowLayerBlock.LAYERS);
        if (layers <= 1) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        } else {
            level.setBlockAndUpdate(pos, state.setValue(SnowLayerBlock.LAYERS, layers - 1));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Is this water next to a bank? The ice forms at the banks and
     * pushes out over the season, and the spread grows as it gets
     * colder.
     *
     * This scans the neighbourhood, so it is only worth doing for the
     * columns that are actually water.
     */
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
