package net.enderwish.TerraForma_Subpack.core.current;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.GHNoiseRouter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CurrentField
 *
 * The water's own motion: what the current is doing at a place, and
 * how fast.
 *
 * There are two kinds of moving water in the world, and they are
 * quite different animals.
 *
 * A RIVER runs because the ground falls away under it. The direction
 * it runs is the direction the ground falls, and the ground falls all
 * the way to the sea, so the current in a river always points at the
 * sea. That is not a rule laid on top: it is what falling ground
 * means.
 *
 * The speed is the same water through a narrower gap. A river in the
 * highlands is a couple of blocks wide and runs like a torrent; the
 * same water spread out at the mouth is a mile across and barely
 * moves. So the current is fast where the water is narrow and slow
 * where it is wide.
 *
 * The SEA is a different thing altogether, and what happens in it
 * depends on the season.
 *
 * The great ocean currents are driven by two things: the wind, which
 * piles the water up on one side of an ocean, and the difference in
 * temperature between the equator and the poles, which makes cold
 * dense water sink and drag the warm surface water after it. The
 * result is the great wheels of circulation that turn anticlockwise
 * in the northern hemisphere and clockwise in the south.
 *
 * And they change with the seasons. In winter the contrast between
 * the warm southern water and the cold northern water is at its
 * greatest, so the whole circulation runs hardest, and the cold
 * currents push further towards the equator. In summer the warm
 * currents take over -- the water runs poleward along the west side
 * of the ocean -- and the cold water comes up along the eastern
 * coasts, which is where the summer fogs come from too.
 *
 * So the sea's current here is a slow wheel, whose strength follows
 * the season, with a drift along it that follows the temperature:
 * the warm water goes poleward and the cold water goes towards the
 * equator.
 *
 * LAKES have no current. Still water is still water.
 *
 * Nothing here is traced, and nothing here depends on the rivers
 * having been made. The current is worked out from the ground
 * itself, the same way on the client as on the server, so that the
 * water carries a player and the player in exactly the same
 * direction.
 */
public final class CurrentField {

    private CurrentField() {}

    /** How fast the water runs in a river of the ordinary width. */
    private static final float RIVER_BASE_SPEED = 3.5f;
    /** The width of river the base speed is for, in blocks. */
    private static final float REFERENCE_WIDTH = 6.0f;
    /** The fastest any current runs. */
    private static final float MAX_SPEED = 8.0f;
    /** How fast the sea's own wheel turns, in blocks a second. */
    private static final float SEA_BASE_SPEED = 0.32f;
    /** The most the temperature may add to the sea's drift. */
    private static final float MAX_POLAR_DRIFT = 0.75f;
    /** How far apart the two temperatures are compared, in blocks. */
    private static final int DRIFT_DISTANCE = 96;
    /** How far out the water is measured for its width. */
    private static final int WIDTH_STEPS = 16;

    /**
     * What the current is doing at a place.
     *
     * @param x     the way it runs, x
     * @param z     the way it runs, z
     * @param speed how fast, in blocks a second
     */
    private record Flow(float x, float z, float speed) {}

    /**
     * The answers, kept because they are expensive and they are
     * asked for every entity in the water on every tick.
     */
    private static final Map<Long, Flow> CACHE = new ConcurrentHashMap<>();

    /**
     * The current at a place, as a velocity in blocks a tick.
     *
     * Returns null where the water is still.
     */
    public static Vec3 currentAt(Level level, BlockPos pos) {
        if (level == null) return null;
        if (!isWet(level, pos)) return null;

        int x = pos.getX();
        int z = pos.getZ();

        // Below the sea level it is the sea, and the sea has its own
        // slow wheel of a current. Above it, it is a river -- or a
        // lake, and a lake is flat, so it has no current at all.
        if (pos.getY() <= GHNoiseRouter.SEA_LEVEL) {
            Vec3 sea = seaCurrent(level, x, z);
            if (sea != null) return sea;
        }

        Flow flow = flowAt(x, z);
        if (flow == null) return null;
        float s = flow.speed() / 20.0f;
        return new Vec3(flow.x() * s, 0.0, flow.z() * s);
    }

    /**
     * The current of a river at a place, as a velocity in blocks a
     * tick.
     *
     * Returns null where the ground is flat, which is where lakes
     * are.
     */
    public static Vec3 riverCurrent(int x, int z) {
        Flow flow = flowAt(x, z);
        if (flow == null) return null;
        float s = flow.speed() / 20.0f;
        return new Vec3(flow.x() * s, 0.0, flow.z() * s);
    }

    /**
     * The flow of a river at a place.
     *
     * The ground's slope gives the direction and the steepness; the
     * width of the water gives the speed.
     */
    private static Flow flowAt(int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        Flow cached = CACHE.get(key);
        if (cached != null) return cached;

        Flow flow = computeFlow(x, z);
        if (flow == null) return null;   // flat ground: still water
        if (CACHE.size() > 100_000) CACHE.clear();
        CACHE.put(key, flow);
        return flow;
    }

    private static Flow computeFlow(int x, int z) {
        // the slope of the ground: the way the water runs and how
        // steeply it is falling
        int here = GHNoiseRouter.INSTANCE.getSurfaceHeight(x, z);
        int east = GHNoiseRouter.INSTANCE.getSurfaceHeight(x + 1, z);
        int west = GHNoiseRouter.INSTANCE.getSurfaceHeight(x - 1, z);
        int south = GHNoiseRouter.INSTANCE.getSurfaceHeight(x, z + 1);
        int north = GHNoiseRouter.INSTANCE.getSurfaceHeight(x, z - 1);

        float dx = (east - west) / 2.0f;
        float dz = (south - north) / 2.0f;
        float slope = (float) Math.sqrt(dx * dx + dz * dz);
        if (slope < 0.0005f) return null;   // dead flat: no current

        // downhill
        float dirX = -dx / slope;
        float dirZ = -dz / slope;

        // the same water through a narrower gap runs faster
        float width = widthAt(x, z, dirX, dirZ, here);
        float narrowness = REFERENCE_WIDTH / Math.max(1.5f, width);
        float speed = RIVER_BASE_SPEED * narrowness;

        // and the steeper it falls, the faster it runs
        speed *= 1.0f + Math.min(2.0f, slope * 4.0f);

        if (speed > MAX_SPEED) speed = MAX_SPEED;
        if (speed < 0.05f) return null;

        return new Flow(dirX, dirZ, speed);
    }

    /**
     * How wide the water is here, measured across the flow.
     *
     * It walks out sideways from the column until the ground rises
     * above the water. The narrower the gap, the harder the water
     * is squeezed and the faster it runs.
     */
    private static float widthAt(int x, int z, float dirX, float dirZ, int here) {
        float px = -dirZ;
        float pz = dirX;
        int left = 0;
        int right = 0;
        for (int d = 1; d <= WIDTH_STEPS; d++) {
            if (left == 0 && isLand(x + Math.round(px * d), z + Math.round(pz * d), here)) {
                left = d;
            }
            if (right == 0 && isLand(x - Math.round(px * d), z - Math.round(pz * d), here)) {
                right = d;
            }
            if (left > 0 && right > 0) break;
        }
        if (left == 0) left = WIDTH_STEPS;
        if (right == 0) right = WIDTH_STEPS;
        return left + right;
    }

    /** Is the ground at this place above the water? */
    private static boolean isLand(int x, int z, int waterLevel) {
        return GHNoiseRouter.INSTANCE.getSurfaceHeight(x, z) > waterLevel;
    }

    /** Is there water here at all? */
    private static boolean isWet(Level level, BlockPos pos) {
        if (level.getFluidState(pos).is(Fluids.WATER)) return true;
        return level.getFluidState(pos.below()).is(Fluids.WATER);
    }

    /**
     * The sea's current at a place.
     *
     * The wheel of the great circulation, turned by the season, with
     * the warm and cold water running along it.
     */
    public static Vec3 seaCurrent(Level level, int x, int z) {
        GHNoiseRouter.FlowDirection wheel =
                GHNoiseRouter.INSTANCE.getWaterFlowDirection(x, z);
        float len = (float) Math.sqrt(wheel.x() * wheel.x() + wheel.z() * wheel.z());
        if (len < 1.0e-5f) return null;

        SeasonCalendar.Season season = seasonOf(level);
        float strength = seasonStrength(season);
        float speed = SEA_BASE_SPEED * strength;

        // The temperature decides which way the water runs along the
        // wheel. Where it gets colder going north, the water runs
        // north: the warm water goes poleward and the cold water
        // comes towards the equator.
        float warm = temperatureAt(level, x, z + DRIFT_DISTANCE)
                - temperatureAt(level, x, z - DRIFT_DISTANCE);
        float drift = clamp(warm * 6.0f * polewardStrength(season),
                -MAX_POLAR_DRIFT, MAX_POLAR_DRIFT);

        float dx = wheel.x() / len * speed;
        float dz = wheel.z() / len * speed;

        return new Vec3(dx, 0.0, dz + drift / 20.0f);
    }

    /**
     * How hard the whole circulation runs in a season.
     *
     * The contrast between the warm southern water and the cold
     * northern water is at its greatest in winter, and that is when
     * the wheel turns hardest.
     */
    private static float seasonStrength(SeasonCalendar.Season season) {
        return switch (season) {
            case WINTER -> 1.0f;
            case SUMMER -> 0.85f;
            case AUTUMN -> 0.7f;
            case SPRING -> 0.6f;
        };
    }

    /**
     * How strongly the warm water runs poleward in a season.
     *
     * In summer the warm currents take over and the water runs north
     * along the western side of the ocean. In winter the cold water
     * is the master of it and the warm water barely moves.
     */
    private static float polewardStrength(SeasonCalendar.Season season) {
        return switch (season) {
            case SUMMER -> 1.4f;
            case AUTUMN -> 1.0f;
            case SPRING -> 0.9f;
            case WINTER -> 0.4f;
        };
    }

    /** The temperature of the water at a place, on the server or the client. */
    private static float temperatureAt(Level level, int x, int z) {
        if (level instanceof ClientLevel) {
            return net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState
                    .temperatureAt(new BlockPos(x, GHNoiseRouter.SEA_LEVEL, z));
        }
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            return net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonTemperature
                    .calculate(server, new BlockPos(x, GHNoiseRouter.SEA_LEVEL, z));
        }
        return 0.0f;
    }

    /** Which season it is, on the server or the client. */
    private static SeasonCalendar.Season seasonOf(Level level) {
        if (level instanceof ClientLevel) {
            return net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState.getSeason();
        }
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            return net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI.getSeason(server);
        }
        return SeasonCalendar.Season.SUMMER;
    }

    private static float clamp(float v, float low, float high) {
        return v < low ? low : (v > high ? high : v);
    }
}
