package net.enderwish.TerraForma_Subpack.core.worldgen.river;

import net.enderwish.TerraForma_Subpack.core.worldgen.noise.GHNoiseRouter;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RiverNetwork
 *
 * The rivers of the world, and the current that runs in them.
 *
 * A river here is not a noise that says "river" and hopes for the
 * best. It is traced: a source is picked on the high ground, and
 * from there the river follows the steepest descent, step by step,
 * until it runs into the sea. That is the rule a real river follows
 * -- it goes down, and it goes down until it reaches something bigger
 * than itself.
 *
 * Because the trace follows the ground down, the current in a river
 * always points at the sea. That is not an extra rule bolted on: it
 * is what tracing downhill means.
 *
 * The channel is cut into the terrain as it is traced, and it widens
 * as it goes, because the water gathering downhill has more to carry.
 * A narrow river in the highlands runs fast; a wide one at the mouth
 * runs slowly. That is not a setting either -- it is what happens
 * when the same amount of water has to get through a narrower gap.
 *
 * The traces are cached, and a spatial index of them is kept, so that
 * a column can be asked what the river is doing at it without
 * walking every path in the world.
 */
public final class RiverNetwork {

    private RiverNetwork() {}

    /** How far apart the river sources are. */
    public static final int SOURCE_SPACING = 384;
    /** The most a river is traced before it is given up on. */
    public static final int MAX_TRACE = 384;
    /** How far off the river looks for the broad slope, in blocks. */
    private static final int COARSE = 6;
    /** The narrowest a river is, at its source. */
    public static final float MIN_HALF_WIDTH = 1.6f;
    /** The widest it gets, at its mouth. */
    public static final float MAX_HALF_WIDTH = 6.5f;
    /** How deep the channel is cut, as a fraction of its width. */
    private static final float DEPTH_FACTOR = 0.8f;

    /** The index is a grid of cells this many blocks across. */
    private static final int INDEX_CELL = 16;

    /** Every traced river, by the source it came from. */
    private static final Map<Long, List<PathPoint>> PATHS = new ConcurrentHashMap<>();
    /** The same points, indexed by the cell they lie in. */
    private static final Map<Long, List<PathPoint>> INDEX = new ConcurrentHashMap<>();

    /**
     * A point on a river.
     *
     * @param x        the block x
     * @param z        the block z
     * @param bedY     the height of the bed the channel is cut to
     * @param waterY   the height the water in the channel stands at
     * @param halfWidth how far the channel reaches from the centre line
     * @param dirX     the way the water runs, x
     * @param dirZ     the way the water runs, z
     */
    public record PathPoint(int x, int z, int bedY, int waterY,
                            float halfWidth, float dirX, float dirZ) {}

    /**
     * What the river does to one column.
     *
     * @param bedY  the height the bed is cut down to
     * @param waterY the height the water in it stands at
     */
    public record RiverColumn(int bedY, int waterY) {}

    // ── The question the terrain asks ──────────────────────────────────────

    /**
     * Is there a river at this column, and what does it do to it?
     *
     * Returns null where there is no river.
     */
    public static RiverColumn columnAt(int x, int z) {
        ensureTracedAround(x, z);

        List<PathPoint> points = INDEX.get(cellKey(x / INDEX_CELL, z / INDEX_CELL));
        if (points == null || points.isEmpty()) return null;

        RiverColumn best = null;
        float bestRatio = Float.MAX_VALUE;
        for (PathPoint p : points) {
            float dx = x - p.x();
            float dz = z - p.z();
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist > p.halfWidth()) continue;
            // the closest point to the centre line wins, so the channel
            // has a spine rather than a ragged edge
            float ratio = dist / p.halfWidth();
            if (ratio < bestRatio) {
                bestRatio = ratio;
                best = new RiverColumn(p.bedY(), p.waterY());
            }
        }
        return best;
    }


    // ── Tracing ────────────────────────────────────────────────────────────

    /**
     * Makes sure every source whose river could reach this column has
     * been traced.
     *
     * The trace is a pure function of the world seed, so it is the
     * same wherever it is computed -- which is what makes a river
     * run the same way on both sides of a chunk border.
     */
    private static void ensureTracedAround(int x, int z) {
        int gx = Math.floorDiv(x, SOURCE_SPACING);
        int gz = Math.floorDiv(z, SOURCE_SPACING);
        // the reach of a trace is MAX_TRACE, so a source further away
        // than that cannot have a river running through this column
        int span = (int) ((MAX_TRACE + MAX_HALF_WIDTH) / SOURCE_SPACING) + 1;
        for (int ix = gx - span; ix <= gx + span; ix++) {
            for (int iz = gz - span; iz <= gz + span; iz++) {
                traceSource(ix, iz);
            }
        }
    }

    /**
     * The traced river from a source, for looking at.
     *
     * The grid cell of the source, not a position.
     */
    public static List<PathPoint> pathOf(int gx, int gz) {
        traceSource(gx, gz);
        return PATHS.getOrDefault(sourceKey(gx, gz), List.of());
    }

    /** Traces the river from one source, if it has not been traced yet. */
    private static void traceSource(int gx, int gz) {
        long key = sourceKey(gx, gz);
        if (PATHS.containsKey(key)) return;

        // the source sits in its cell, moved about a little so the
        // rivers do not all start in a tidy row
        int jitterX = hash(gx * 31 + gz) % (SOURCE_SPACING / 2) - SOURCE_SPACING / 4;
        int jitterZ = hash(gx * 7919 + gz * 13) % (SOURCE_SPACING / 2) - SOURCE_SPACING / 4;
        int sx = gx * SOURCE_SPACING + SOURCE_SPACING / 2 + jitterX;
        int sz = gz * SOURCE_SPACING + SOURCE_SPACING / 2 + jitterZ;

        List<PathPoint> path = trace(sx, sz);
        PATHS.put(key, path);
        for (PathPoint p : path) {
            register(p);
        }
    }

    /** Follows the ground down from a source and records the way. */
    private static List<PathPoint> trace(int sx, int sz) {
        List<int[]> raw = new ArrayList<>();
        List<float[]> dirs = new ArrayList<>();

        int x = sx;
        int z = sz;
        int y = GHNoiseRouter.INSTANCE.getSurfaceHeight(x, z);
        // a source in the sea has no river to give
        if (y <= GHNoiseRouter.SEA_LEVEL + 1) return List.of();

        int coarseSteps = MAX_TRACE / COARSE;
        int lastStepX = 0;
        int lastStepZ = 0;
        for (int step = 0; step < coarseSteps; step++) {
            // The ground here falls by less than a block in a
            // hundred, and it is bumpy on top of that, so a river
            // that simply walked downhill would stop at every
            // shallow dip and never get anywhere. It looks at the
            // broad slope instead: the lowest of the eight
            // directions, a fair way off.
            int bx = x;
            int bz = z;
            int bestY = y;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    int nx = x + dx * COARSE;
                    int nz = z + dz * COARSE;
                    int ny = GHNoiseRouter.INSTANCE.getSurfaceHeight(nx, nz);
                    if (ny < bestY) {
                        bestY = ny;
                        bx = nx;
                        bz = nz;
                    }
                }
            }
            if (bx == x && bz == z) {
                // The ground has gone flat. A river does not stop
                // where the land flattens out -- it carries on along
                // its own bearing until the ground picks up again.
                // Without this, every river in the world would end
                // within a few hundred blocks of where it started.
                if (lastStepX == 0 && lastStepZ == 0) break;
                bx = x + lastStepX * COARSE;
                bz = z + lastStepZ * COARSE;
                bestY = GHNoiseRouter.INSTANCE.getSurfaceHeight(bx, bz);
            }
            lastStepX = Integer.signum(bx - x);
            lastStepZ = Integer.signum(bz - z);
            if (lastStepX == 0 && lastStepZ == 0) break;

            float len = (float) Math.sqrt((bx - x) * (bx - x) + (bz - z) * (bz - z));
            float dirX = (bx - x) / len;
            float dirZ = (bz - z) / len;

            // The points in between are filled in, so the channel it
            // cuts is unbroken rather than a line of stepping stones.
            for (int k = 0; k < COARSE; k++) {
                int px = x + (bx - x) * k / COARSE;
                int pz = z + (bz - z) * k / COARSE;
                int py = y + (bestY - y) * k / COARSE;
                raw.add(new int[]{px, pz, py});
                dirs.add(new float[]{dirX, dirZ});
            }

            x = bx;
            z = bz;
            y = bestY;
            if (y <= GHNoiseRouter.SEA_LEVEL) {
                // it has reached the sea
                break;
            }
        }

        if (raw.isEmpty()) return List.of();

        // The water's surface is smoothed along the river, or every
        // step of the ground would put a weir in it. It is kept close
        // to the ground it runs over, so the river never stands above
        // the land beside it and spills out.
        int n = raw.size();
        List<PathPoint> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int[] p = raw.get(i);
            float[] d = dirs.get(i);

            int sum = 0;
            int count = 0;
            for (int j = Math.max(0, i - 4); j <= Math.min(n - 1, i + 4); j++) {
                sum += raw.get(j)[2];
                count++;
            }
            int smoothed = sum / count;
            int ground = p[2];
            // The water stands at least a block below the land
            // beside it, or it would spread out over the banks the
            // moment it was made.
            int waterY = Mth.clamp(smoothed, ground - 3, ground - 1);

            // the channel widens as it goes downstream
            float t = (float) i / (float) Math.max(1, n - 1);
            float halfWidth = MIN_HALF_WIDTH + (MAX_HALF_WIDTH - MIN_HALF_WIDTH) * t;
            int depth = Math.max(2, (int) Math.round(halfWidth * 2.0f * DEPTH_FACTOR));
            int bedY = waterY - depth;

            out.add(new PathPoint(p[0], p[1], bedY, waterY, halfWidth, d[0], d[1]));
        }
        return out;
    }

    // ── The index ──────────────────────────────────────────────────────────

    /** Puts a point into every cell it can touch. */
    private static void register(PathPoint p) {
        int reach = (int) Math.ceil(p.halfWidth());
        int x0 = (p.x() - reach) / INDEX_CELL;
        int x1 = (p.x() + reach) / INDEX_CELL;
        int z0 = (p.z() - reach) / INDEX_CELL;
        int z1 = (p.z() + reach) / INDEX_CELL;
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                INDEX.computeIfAbsent(cellKey(cx, cz), k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(p);
            }
        }
    }

    private static long cellKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    private static long sourceKey(int gx, int gz) {
        return ((long) gx << 32) ^ (gz & 0xFFFFFFFFL) ^ 0x5DEECE66DL;
    }

    /** A cheap deterministic hash, so the same world always has the same rivers. */
    private static int hash(int v) {
        int h = v * 0x27D4EB2D;
        h ^= (h >>> 15);
        h *= 0x85EBCA6B;
        h ^= (h >>> 13);
        return h & 0x7FFFFFFF;
    }
}
