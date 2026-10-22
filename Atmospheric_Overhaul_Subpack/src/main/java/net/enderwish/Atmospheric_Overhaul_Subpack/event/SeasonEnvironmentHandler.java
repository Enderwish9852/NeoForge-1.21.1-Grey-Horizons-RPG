package net.enderwish.Atmospheric_Overhaul_Subpack.event;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonData;
import net.enderwish.Atmospheric_Overhaul_Subpack.entity.FloatingIceEntity;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonTemperature;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    /**
     * How likely one visit is to freeze the next layer of a water
     * column.
     *
     * It was far too eager: a large amount of ice appeared within
     * half a day, which is no time at all for a river to freeze over.
     * The ice now takes the better part of a season to spread and
     * thicken, which is the point of it -- a frozen river in mid
     * winter should be the result of weeks of cold, not one cold
     * afternoon.
     */
    private static final float ICE_CHANCE = 0.008f;

    /**
     * How likely the shallow ice at the edge of a sheet is to crack
     * and break up in the cold-rain band.
     *
     * It was three times too eager, and every column of ice was
     * breaking up, not just the edges, so a frozen river came apart
     * all at once.
     */
    private static final float CRACK_CHANCE = 0.0012f;

    /** How much colder a shaded spot is, for each level of sky light lost. */
    private static final float SHADE_PER_LEVEL = 0.018f;

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

        // The shade makes it colder. A hollow under a rock, or the
        // ground under a tree, keeps its ice long after the open
        // ground has thawed -- and the sky light is a cheap way to
        // know how shaded a spot is.
        int skyLight = level.getLightEngine()
                .getLayerListener(net.minecraft.world.level.LightLayer.SKY)
                .getLightValue(surface);
        temp -= (15 - skyLight) * SHADE_PER_LEVEL;

        // ── Ice ────────────────────────────────────────────────────────────
        // The ice's own job, and it is the one that has to happen
        // whether or not it is raining: it forms at the banks and
        // pushes out over the season.
        //
        // HOW THICK IT GETS depends on how deep the water is. A shallow
        // puddle gets a skin of ice, a deep river gets a sheet of it.
        // The ice fills the water column from the BED upwards, so it
        // is always supported and the surface of a frozen river stays
        // at the height the water was.
        int ice = 0;
        int waterDepth = 0;
        BlockPos lowestIce = null;
        BlockPos lowestWater = null;
        BlockPos p = ground;
        while (ice < 8 && level.getBlockState(p).is(Blocks.ICE)) {
            lowestIce = p;
            ice++;
            p = p.below();
        }
        while (waterDepth < 24 && level.getBlockState(p).is(Blocks.WATER)) {
            lowestWater = p;
            waterDepth++;
            p = p.below();
        }

        if (ice > 0) {
            // -- the ice is here already ----------------------------------
            if (temp > SeasonTemperature.MELT_THRESHOLD) {
                // It thins from the BOTTOM. The water under a sheet of
                // ice is warmer than the air above it, so that is
                // where it goes first.
                float speed = SeasonTemperature.getMeltSpeed(temp);
                if (rand.nextFloat() < MELT_CHANCE * speed) {
                    level.setBlockAndUpdate(lowestIce, Blocks.WATER.defaultBlockState());
                    // What comes off the edge of a melting sheet is a
                    // floe: the edge is where a sheet breaks up, and
                    // the middle of a river just gets thinner.
                    if (isIceEdge(level, lowestIce)) {
                        spawnFloatingIce(level, surface);
                        playCrack(level, surface);
                    }
                }
            } else if (temp > SeasonTemperature.NO_FORM_THRESHOLD && ice == 1
                    && isIceEdge(level, lowestIce)) {
                // -- the cracking ------------------------------------------
                // The shallow ice, in the cold-rain band of late
                // winter, starts to crack and break up. It goes from the
                // EDGE of the sheet inwards, the way a sheet of ice
                // really does, so the middle of a river stays solid
                // long after the edges have broken away.
                if (rand.nextFloat() < CRACK_CHANCE) {
                    level.setBlockAndUpdate(lowestIce, Blocks.WATER.defaultBlockState());
                    spawnFloatingIce(level, surface);
                    playCrack(level, surface);
                }
            }
            return;
        }

        if (lowestWater != null && level.canSeeSky(surface)
                && level.getBlockState(surface).is(Blocks.WATER)) {
            // -- the ice forms --------------------------------------------
            int iceSpread = SeasonTemperature.getIceSpread(temp);
            if (iceSpread > 0 && isNearEdge(level, surface, iceSpread)) {
                float formSpeed = SeasonTemperature.getFormationSpeed(temp);
                // the deeper the water, the thicker the ice it takes
                int target = iceLayersForDepth(ice + waterDepth);
                if (ice < target && rand.nextFloat() < formSpeed * ICE_CHANCE) {
                    // It grows DOWNWARD from the surface. The sheet
                    // always reaches the water and always touches the
                    // top, so it is always found again -- which is the
                    // whole of the melt.
                    //
                    // An earlier version put the ice at the bed of the
                    // water instead. A sheet down there was never seen
                    // again: the scan looks for ice from the surface
                    // down, so it neither melted nor got any thicker.
                    BlockPos place = (ice == 0) ? surface : lowestIce.below();
                    level.setBlockAndUpdate(place, Blocks.ICE.defaultBlockState());
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
                } else if (layers >= maxDepth && maxDepth >= 8
                        && temp <= SeasonTemperature.EXTREME_THRESHOLD
                        && rand.nextFloat() < SNOW_CHANCE * speed * 0.25f) {
                    // At the very bottom of the cold the drift itself
                    // turns to powder snow. It replaces the top layer
                    // rather than sitting on top of it, so the ground
                    // does not rise, and when it melts the layers come
                    // back and there is no hole.
                    level.setBlockAndUpdate(ground, Blocks.POWDER_SNOW.defaultBlockState());
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
            // the powder snow goes back to being a drift
            if (groundState.is(Blocks.POWDER_SNOW) && rand.nextFloat() < MELT_CHANCE * speed) {
                level.setBlockAndUpdate(ground,
                        Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 8));
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

    /**
     * How thick the ice should be for a water of this depth.
     *
     * A shallow puddle takes a skin, a deep river takes a sheet. Three
     * blocks is the most: past that it is a frozen lake rather than a
     * frozen river, and a thick sheet takes so long to thaw that it
     * would still be there in late spring.
     */
    private static int iceLayersForDepth(int depth) {
        if (depth <= 1) return 1;
        if (depth <= 2) return 1;
        if (depth <= 3) return 2;
        if (depth <= 4) return 2;
        return 3;
    }


    /**
     * Is this the edge of the ice sheet?
     *
     * A sheet breaks up from its edges inwards, so only the ice that
     * has open water beside it cracks. The middle of a frozen river
     * stays solid long after the edges have gone.
     */
    private static boolean isIceEdge(ServerLevel level, BlockPos icePos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (!level.getBlockState(icePos.offset(dx, 0, dz)).is(Blocks.ICE)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A clump of ice breaks off and starts drifting.
     *
     * There is one roll for it, and it decides what comes off: a small
     * piece most of the time, a medium one now and then, a large raft
     * rarely, and four times in ten nothing at all comes off and the
     * ice simply melts back to water.
     *
     * The bigger ones take a bite of the sheet with them as they go,
     * which is why a floe is often found floating in a hole it made
     * for itself.
     */
    private static void spawnFloatingIce(ServerLevel level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;

        float roll = server.getRandom().nextFloat();
        FloatingIceEntity.Kind kind;
        if (roll < 0.30f) {
            kind = FloatingIceEntity.Kind.SMALL;
        } else if (roll < 0.50f) {
            kind = FloatingIceEntity.Kind.MEDIUM;
        } else if (roll < 0.60f) {
            kind = FloatingIceEntity.Kind.LARGE;
        } else {
            // four times in ten, nothing comes off
            return;
        }

        // it takes a bite of the sheet with it
        breakIceAround(server, pos, kind);

        FloatingIceEntity.spawn(server, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, kind);
    }

    /**
     * The ice the floe takes with it as it comes off.
     *
     * A medium takes three or four blocks, a large one takes a dozen
     * and more, and a small one takes none at all.
     */
    private static void breakIceAround(ServerLevel level, BlockPos pos, FloatingIceEntity.Kind kind) {
        if (kind.iceTaken <= 0) return;
        int radius = kind == FloatingIceEntity.Kind.LARGE ? 2 : 1;
        int removed = 0;
        for (int dx = -radius; dx <= radius && removed < kind.iceTaken; dx++) {
            for (int dz = -radius; dz <= radius && removed < kind.iceTaken; dz++) {
                BlockPos p = pos.offset(dx, 0, dz);
                if (level.getBlockState(p).is(Blocks.ICE)) {
                    level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
                    removed++;
                }
            }
        }
    }

    /** The ice cracking. Vanilla has no ice crack, so it borrows glass. */
    private static void playCrack(ServerLevel level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        server.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.45f, 1.3f);
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
