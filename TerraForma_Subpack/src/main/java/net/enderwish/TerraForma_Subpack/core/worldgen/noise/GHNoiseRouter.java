package net.enderwish.TerraForma_Subpack.core.worldgen.noise;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * GHNoiseRouter
 *
 * BUGFIX 1 -- isOcean: previously `isOceanBasin(x,z) || continental
 * < -30`, a threshold DECOUPLED from the actual height formula. A
 * column could compute a real below-sea-level height without ever
 * crossing that -30 cutoff -- water fills it, but biome selection
 * (which calls isOcean) never finds out, so it keeps a LAND biome
 * name. Confirmed as the cause of the "massive sea labeled Ash
 * Plains/Temperate Forest" report -- both readings now come from the
 * SAME getSurfaceHeight formula, using the neutral profile so biome
 * selection (which needs this before it knows the biome) isn't
 * circular.
 *
 * BUGFIX 2 -- isCave: previously always checked against the
 * NEUTRAL-profile surface height for its "how close to the surface"
 * gate, while actual terrain was built with each column's REAL
 * per-biome profile. In high-hilliness biomes (Volcanic Lowlands
 * 2.4x, Glacial Peaks 2.2x) those two heights can differ by 100+
 * blocks, so the depth-based cave-density tightening (meant to keep
 * caves rare near a peak) was checking against the wrong depth
 * entirely. Confirmed as the cause of the chaotic "drilled-through"
 * underground, concentrated exactly where Volcanic Lowlands
 * generates. isCave now takes a BiomeTerrainProfile; a neutral
 * overload remains for callers without one handy.
 */
public class GHNoiseRouter {

    public static final GHNoiseRouter INSTANCE = new GHNoiseRouter();
    private GHNoiseRouter() {}

    public static final int SEA_LEVEL        = 63;
    public static final int MIN_HEIGHT       = -64;
    public static final int MAX_HEIGHT       = 320;
    public static final int BEDROCK_HEIGHT   = MIN_HEIGHT + 5;

    private static final int OCEAN_BASIN_DEPTH = 25;
    private static final double OCEAN_BASIN_THRESHOLD = -0.55;

    private long lastSeed = Long.MIN_VALUE;

    private NoiseLayer continental;
    private NoiseLayer erosion;
    private NoiseLayer peaksAndValleys;
    private NoiseLayer surfaceDetail;
    private NoiseLayer caveA;
    private NoiseLayer caveB;
    private NoiseLayer oceanBasin;

    public synchronized void setSeed(long seed) {
        if (seed == lastSeed) return;
        lastSeed = seed;

        RandomSource rand = RandomSource.create(seed);

        continental = new NoiseLayer("continental",
                RandomSource.create(rand.nextLong()), 7_000, 120, 4);

        erosion = new NoiseLayer("erosion",
                RandomSource.create(rand.nextLong()), 2_000, 1, 3);

        peaksAndValleys = new NoiseLayer("peaks_and_valleys",
                RandomSource.create(rand.nextLong()), 400, 80, 5);

        surfaceDetail = new NoiseLayer("surface_detail",
                RandomSource.create(rand.nextLong()), 100, 8, 3);

        caveA = new NoiseLayer("cave_a",
                RandomSource.create(rand.nextLong()), 80, 1, 2);

        caveB = new NoiseLayer("cave_b",
                RandomSource.create(rand.nextLong()), 80, 1, 2);

        oceanBasin = new NoiseLayer("ocean_basin",
                RandomSource.create(rand.nextLong()), 40_000, 1, 2);
    }

    private synchronized void ensureInitialized() {
        if (continental == null) {
            setSeed(12345L);
        }
    }

    public int getSurfaceHeight(int x, int z) {
        return getSurfaceHeight(x, z, BiomeTerrainProfile.NEUTRAL);
    }

    public int getSurfaceHeight(int x, int z, BiomeTerrainProfile profile) {
        ensureInitialized();

        double cont = getContinentalValue(x, z);
        double eros = getErosionValue(x, z);
        double pv   = getPeaksAndValleysValue(x, z);
        double det  = surfaceDetail.sample(x, z);

        double effectiveEros = Mth.clamp(eros + profile.erosionBias(), 0.0, 1.0);

        double baseHeight = SEA_LEVEL
                + cont * (1.0 - effectiveEros)
                + pv * profile.hilliness() * (1.0 - effectiveEros);

        double finalHeight = baseHeight + det * profile.roughness();

        if (isOceanBasin(x, z)) {
            finalHeight = Math.min(finalHeight, SEA_LEVEL - OCEAN_BASIN_DEPTH);
        }

        return (int) Mth.clamp(finalHeight, MIN_HEIGHT, MAX_HEIGHT);
    }

    public boolean isOceanBasin(int x, int z) {
        ensureInitialized();
        return oceanBasin.sample(x, z) < OCEAN_BASIN_THRESHOLD;
    }

    /** True if this column's raw landform ends up below sea level -- see class doc. */
    public boolean isOcean(int x, int z) {
        ensureInitialized();
        return getSurfaceHeight(x, z, BiomeTerrainProfile.NEUTRAL) < SEA_LEVEL;
    }

    public FlowDirection getWaterFlowDirection(int x, int z) {
        ensureInitialized();
        double step = 8.0;
        double dx = oceanBasin.sample(x + step, z) - oceanBasin.sample(x - step, z);
        double dz = oceanBasin.sample(x, z + step) - oceanBasin.sample(x, z - step);
        return new FlowDirection((float) -dx, (float) -dz);
    }

    public record FlowDirection(float x, float z) {}

    public boolean isRiver(int x, int z) {
        ensureInitialized();
        double pv = getPeaksAndValleysValue(x, z);
        double eros = getErosionValue(x, z);
        return Math.abs(pv) < 5.0 && eros > 0.5 && !isOcean(x, z);
    }

    public boolean isMountain(int x, int z) {
        ensureInitialized();
        return getContinentalValue(x, z) > 40
                && getErosionValue(x, z) < 0.3
                && getPeaksAndValleysValue(x, z) > 20;
    }

    /** Neutral-profile overload -- prefer the 4-arg version below when a profile is already known. */
    public boolean isCave(int x, int y, int z) {
        return isCave(x, y, z, BiomeTerrainProfile.NEUTRAL);
    }

    public boolean isCave(int x, int y, int z, BiomeTerrainProfile profile) {
        ensureInitialized();
        if (y <= BEDROCK_HEIGHT) return false;
        if (y >= getSurfaceHeight(x, z, profile) - 5) return false;

        double a = caveA.sample3D(x, y, z);
        double b = caveB.sample3D(x, y, z);

        double depthFactor = 1.0 - ((double)(y - MIN_HEIGHT) / (SEA_LEVEL - MIN_HEIGHT));
        double threshold = 0.15 + depthFactor * 0.1;

        return Math.abs(a) < threshold && Math.abs(b) < threshold;
    }

    public boolean isLargeChamber(int x, int y, int z) {
        ensureInitialized();
        if (y <= BEDROCK_HEIGHT || y >= SEA_LEVEL - 10) return false;

        double a = caveA.sample3D(x, y, z);
        double b = caveB.sample3D(x, y, z);

        return Math.abs(a) < 0.05 && Math.abs(b) < 0.05;
    }

    public double getContinentalValue(int x, int z) {
        ensureInitialized();
        return continental.sample(x, z);
    }

    public double getErosionValue(int x, int z) {
        ensureInitialized();
        double raw = erosion.sample(x, z);
        return Mth.clamp((raw + 1.0) * 0.5, 0.0, 1.0);
    }

    public double getPeaksAndValleysValue(int x, int z) {
        ensureInitialized();
        return peaksAndValleys.sample(x, z);
    }

    public double getSurfaceDetailValue(int x, int z) {
        ensureInitialized();
        return surfaceDetail.sample(x, z);
    }
}
